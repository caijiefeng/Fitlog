package com.example.fitlog.data.backup

import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fitlog.core.database.FitLogDatabase
import com.example.fitlog.core.media.AppMediaStorage
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Result of importing a backup ZIP.
 */
sealed interface ImportResult {
    /** The import completed successfully. */
    data class Success(
        val message: String,
        val rowsImported: Int,
        val mediaRestored: Int,
    ) : ImportResult

    /** The import failed with an error. */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : ImportResult
}

/**
 * Summary of a backup's contents, presented before import.
 *
 * @property manifest     The parsed manifest.
 * @property mediaFiles   List of media relative paths found in the ZIP.
 * @property dbRowCounts  Per-table row counts keyed by table name.
 */
data class ImportSummary(
    val manifest: BackupManifest,
    val mediaFiles: List<String>,
    val dbRowCounts: Map<String, Int>,
)

/**
 * Imports a FitLog backup ZIP into the application.
 *
 * Uses Room's [SupportSQLiteDatabase] (via [Room.databaseBuilder]) for raw SQL
 * operations so the import bypasses Room entity validation during bulk insert.
 *
 * ## Validation
 * - Backup format version must be [BackupManifest.CURRENT_VERSION].
 * - SHA-256 checksum of `db.json` must match the manifest's `dbChecksum`.
 * - `db.json` must be present in the archive.
 * - **ZIP Slip protection**: every entry name is canonicalised and verified
 *   to resolve within a temp directory.
 * - Max 1000 ZIP entries overall.
 * - Max 10 MiB per entry.
 * - Max 500 MiB total uncompressed size.
 *
 * ## Import strategy (replace)
 * 1. All existing records are deleted within a Room transaction (FK-safe order).
 * 2. `db.json` rows are inserted table by table using raw SQL (`INSERT OR REPLACE`)
 *    through [SupportSQLiteDatabase.insert].
 * 3. On any failure the transaction is rolled back — no partial import.
 * 4. Media files from the ZIP's `media/` prefix are copied to
 *    [AppMediaStorage] after the DB transaction succeeds.
 */
class BackupImporter(
    private val context: Context,
    private val db: FitLogDatabase,
    private val appMediaStorage: AppMediaStorage,
) {

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Analyzes a backup ZIP and returns [ImportSummary] without modifying
     * any data.
     *
     * @throws ImportException on validation failure.
     */
    fun analyze(input: InputStream): ImportSummary {
        val entries = readZipEntries(input)

        val manifestBytes = entries["manifest.json"]
            ?: throw ImportException("Backup is missing manifest.json")
        val manifest = parseManifest(manifestBytes)
        validateVersion(manifest)

        val dbJsonBytes = entries["db.json"]
            ?: throw ImportException("Backup is missing db.json")
        validateChecksum(manifest, dbJsonBytes)

        val dbJson = JSONObject(String(dbJsonBytes, Charsets.UTF_8))
        val dbRowCounts = mutableMapOf<String, Int>()
        for (key in dbJson.keys()) {
            val arr = dbJson.optJSONArray(key)
            if (arr != null) {
                dbRowCounts[key] = arr.length()
            }
        }

        val mediaFiles = entries.keys
            .filter { it.startsWith("media/") }
            .map { it.removePrefix("media/") }

        return ImportSummary(
            manifest = manifest,
            mediaFiles = mediaFiles,
            dbRowCounts = dbRowCounts,
        )
    }

    /**
     * Imports the backup ZIP from [input].
     *
     * 1. Validates the archive.
     * 2. Opens a [SupportSQLiteDatabase] via Room's [Room.databaseBuilder].
     * 3. Deletes all existing records and inserts new ones in a single
     *    transaction (rolled back on failure).
     * 4. Copies media files from the ZIP to [AppMediaStorage].
     */
    fun importBackup(input: InputStream): ImportResult {
        return try {
            val entries = readZipEntries(input)

            val manifestBytes = entries["manifest.json"]
                ?: throw ImportException("Backup is missing manifest.json")
            val manifest = parseManifest(manifestBytes)
            validateVersion(manifest)

            val dbJsonBytes = entries["db.json"]
                ?: throw ImportException("Backup is missing db.json")
            validateChecksum(manifest, dbJsonBytes)

            val dbJson = JSONObject(String(dbJsonBytes, Charsets.UTF_8))

            // Count total rows to import
            var totalRows = 0
            for (key in dbJson.keys()) {
                val arr = dbJson.optJSONArray(key)
                if (arr != null) totalRows += arr.length()
            }

            val mediaEntries = entries.filterKeys { it.startsWith("media/") }

            // ── Open a SupportSQLiteDatabase through Room ─────────────────
            // Use Room.databaseBuilder to open the database; the writable
            // database handle is a SupportSQLiteDatabase, not a raw
            // android.database.sqlite.SQLiteDatabase.  We build a second
            // instance here so that import is self-contained and does not
            // depend on the app's singleton Room instance.
            val importDb = Room.databaseBuilder(
                context,
                FitLogDatabase::class.java,
                "fitlog.db",
            ).build()

            try {
                val writableDb: SupportSQLiteDatabase = importDb.openHelper.writableDatabase

                // ── Import DB in transaction ─────────────────────────
                writableDb.beginTransaction()
                try {
                    clearAllTables(writableDb)
                    importAllTables(writableDb, dbJson)
                    writableDb.setTransactionSuccessful()
                } catch (e: Exception) {
                    throw ImportException("Database import failed and was rolled back.", e)
                } finally {
                    writableDb.endTransaction()
                }
            } finally {
                importDb.close()
            }

            // ── Restore media files ─────────────────────────────────────
            var mediaRestored = 0
            for ((entryName, data) in mediaEntries) {
                val relativePath = entryName.removePrefix("media/")
                try {
                    restoreMediaFile(relativePath, data)
                    mediaRestored++
                } catch (_: Exception) {
                    // Skip individual media failures; do not abort the import
                }
            }

            ImportResult.Success(
                message = "Import complete. $totalRows records imported, $mediaRestored media files restored.",
                rowsImported = totalRows,
                mediaRestored = mediaRestored,
            )
        } catch (e: ImportException) {
            ImportResult.Error(message = e.message ?: "Import failed", cause = e)
        } catch (e: Exception) {
            ImportResult.Error(message = e.message ?: "Unexpected error during import", cause = e)
        }
    }

    // ── Validation ──────────────────────────────────────────────────────────

    private fun validateVersion(manifest: BackupManifest) {
        val supported = listOf(1)
        if (manifest.version !in supported) {
            throw ImportException(
                "Unsupported backup version ${manifest.version}. " +
                    "Supported versions: ${supported.joinToString(", ")}.",
            )
        }
    }

    private fun validateChecksum(manifest: BackupManifest, dbJsonBytes: ByteArray) {
        if (manifest.dbChecksum.isBlank()) return
        val digest = MessageDigest.getInstance("SHA-256")
        val actual = digest.digest(dbJsonBytes).joinToString("") { "%02x".format(it) }
        if (actual != manifest.dbChecksum) {
            throw ImportException(
                "Database checksum mismatch. Expected: ${manifest.dbChecksum}, actual: $actual.",
            )
        }
    }

    // ── ZIP reading with safety limits ───────────────────────────────────────

    companion object {
        /** Maximum number of ZIP entries allowed. */
        const val MAX_ENTRIES = 1000
        /** Maximum uncompressed size per entry (10 MiB). */
        const val MAX_ENTRY_SIZE = 10L * 1024 * 1024
        /** Maximum total uncompressed size (500 MiB). */
        const val MAX_TOTAL_SIZE = 500L * 1024 * 1024
    }

    /**
     * Reads all ZIP entries into a map, enforcing safety limits.
     *
     * **ZIP Slip protection**: every entry name is resolved against a temp
     * directory and verified via canonical path so that paths with `..` or
     * absolute paths cannot escape the intended directory.
     *
     * @throws ImportException if any limit is exceeded or path traversal is detected.
     */
    private fun readZipEntries(input: InputStream): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        val zis = ZipInputStream(input)
        var entry: ZipEntry? = zis.nextEntry
        var entryCount = 0
        var totalSize = 0L

        // Dummy base directory for ZIP Slip canonical-path check.
        val baseDir = File(
            System.getProperty("java.io.tmpdir"),
            "fitlog_import_validate",
        ).canonicalFile

        while (entry != null) {
            entryCount++
            if (entryCount > MAX_ENTRIES) {
                throw ImportException(
                    "Backup contains more than $MAX_ENTRIES entries.",
                )
            }

            if (!entry.isDirectory) {
                // ZIP Slip protection: reject absolute paths and path traversal
                val entryFile = File(baseDir, entry.name).canonicalFile
                if (!entryFile.startsWith(baseDir)) {
                    throw ImportException(
                        "ZIP Slip detected: entry \"${entry.name}\" would resolve " +
                            "outside the extraction directory.",
                    )
                }

                val size = entry.size
                if (size >= 0 && size > MAX_ENTRY_SIZE) {
                    throw ImportException(
                        "Entry \"${entry.name}\" is $size bytes, exceeds maximum of $MAX_ENTRY_SIZE.",
                    )
                }

                val data = zis.readBytes()
                if (data.size > MAX_ENTRY_SIZE) {
                    throw ImportException(
                        "Entry \"${entry.name}\" is ${data.size} bytes, exceeds maximum of $MAX_ENTRY_SIZE.",
                    )
                }

                totalSize += data.size
                if (totalSize > MAX_TOTAL_SIZE) {
                    throw ImportException(
                        "Backup total uncompressed size exceeds $MAX_TOTAL_SIZE bytes.",
                    )
                }

                entries[entry.name] = data
            }

            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()
        return entries
    }

    // ── Manifest parsing ────────────────────────────────────────────────────

    private fun parseManifest(bytes: ByteArray): BackupManifest {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        val rowCountsJson = json.optJSONObject("rowCounts")
        val rowCounts = if (rowCountsJson != null) {
            val map = mutableMapOf<String, Int>()
            for (key in rowCountsJson.keys()) {
                map[key] = rowCountsJson.optInt(key, 0)
            }
            map
        } else emptyMap()

        return BackupManifest(
            version = json.optInt("backupVersion", 1),
            appVersion = json.optString("appVersion", ""),
            dbVersion = json.optInt("dbVersion", 0),
            exportedAt = json.optLong("exportedAt", 0L),
            totalRows = json.optInt("totalRows", 0),
            rowCounts = rowCounts,
            mediaCount = json.optInt("mediaCount", 0),
            dbChecksum = json.optString("dbChecksum", ""),
        )
    }

    // ── Table import ────────────────────────────────────────────────────────

    private fun importAllTables(writableDb: SupportSQLiteDatabase, dbJson: JSONObject) {
        importTable(writableDb, dbJson, "exercise_categories", ::insertExerciseCategory)
        importTable(writableDb, dbJson, "exercises", ::insertExercise)
        importTable(writableDb, dbJson, "workout_templates", ::insertWorkoutTemplate)
        importTable(writableDb, dbJson, "workout_template_exercises", ::insertWorkoutTemplateExercise)
        importTable(writableDb, dbJson, "workout_schedules", ::insertWorkoutSchedule)
        importTable(writableDb, dbJson, "workout_sessions", ::insertWorkoutSession)
        importTable(writableDb, dbJson, "exercise_sessions", ::insertExerciseSession)
        importTable(writableDb, dbJson, "set_records", ::insertSetRecord)
        importTable(writableDb, dbJson, "workout_plan_overrides", ::insertWorkoutPlanOverride)
        importTable(writableDb, dbJson, "reminders", ::insertReminder)
        importTable(writableDb, dbJson, "check_ins", ::insertCheckIn)
        importTable(writableDb, dbJson, "user_profiles", ::insertUserProfile)
        importTable(writableDb, dbJson, "body_measurements", ::insertBodyMeasurement)
        importTable(writableDb, dbJson, "food_records", ::insertFoodRecord)
        importTable(writableDb, dbJson, "media_records", ::insertMediaRecord)
    }

    private fun importTable(
        writableDb: SupportSQLiteDatabase,
        dbJson: JSONObject,
        tableName: String,
        inserter: (SupportSQLiteDatabase, JSONObject) -> Unit,
    ): Int {
        val arr = dbJson.optJSONArray(tableName) ?: return 0
        for (i in 0 until arr.length()) {
            inserter(writableDb, arr.getJSONObject(i))
        }
        return arr.length()
    }

    // ── Raw SQL helpers ─────────────────────────────────────────────────────

    private fun insert(table: String, writableDb: SupportSQLiteDatabase, values: ContentValues) {
        writableDb.insert(
            table,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
            values,
        )
    }

    private fun insertExerciseCategory(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("exercise_categories", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("name", json.getString("name"))
            put("description", optNullString(json, "description"))
            put("sort_order", json.optInt("sort_order", 0))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
        })
    }

    private fun insertExercise(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("exercises", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("name", json.getString("name"))
            put("primary_muscle_group", json.getString("primary_muscle_group"))
            put("secondary_muscle_group", optNullString(json, "secondary_muscle_group"))
            put("category_id", optNullLong(json, "category_id"))
            put("notes", optNullString(json, "notes"))
            put("is_custom", boolToInt(json, "is_custom"))
            put("is_active", boolToInt(json, "is_active", true))
            put("sort_order", json.optInt("sort_order", 0))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertWorkoutTemplate(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("workout_templates", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("name", json.getString("name"))
            put("notes", optNullString(json, "notes"))
            put("sort_order", json.optInt("sort_order", 0))
            put("is_active", boolToInt(json, "is_active", true))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertWorkoutTemplateExercise(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("workout_template_exercises", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("template_id", json.getLong("template_id"))
            put("exercise_id", json.getLong("exercise_id"))
            put("target_sets", json.optInt("target_sets", 3))
            put("target_reps_min", optNullInt(json, "target_reps_min"))
            put("target_reps_max", optNullInt(json, "target_reps_max"))
            put("target_weight_kg", optNullDouble(json, "target_weight_kg"))
            put("target_rpe", optNullDouble(json, "target_rpe"))
            put("target_rir", optNullInt(json, "target_rir"))
            put("rest_seconds", json.optInt("rest_seconds", 90))
            put("notes", optNullString(json, "notes"))
            put("sort_order", json.optInt("sort_order", 0))
        })
    }

    private fun insertWorkoutSchedule(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("workout_schedules", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("template_id", json.getLong("template_id"))
            put("day_of_week", json.getInt("day_of_week"))
            put("is_active", boolToInt(json, "is_active", true))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
        })
    }

    private fun insertWorkoutSession(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("workout_sessions", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("schedule_id", optNullLong(json, "schedule_id"))
            put("template_id", optNullLong(json, "template_id"))
            put("template_name_snapshot", optNullString(json, "template_name_snapshot"))
            put("date", json.getLong("date"))
            put("start_time", json.getLong("start_time"))
            put("end_time", optNullLong(json, "end_time"))
            put("status", json.getString("status"))
            put("notes", optNullString(json, "notes"))
            put("active_rest_started_at", optNullLong(json, "active_rest_started_at"))
            put("active_rest_duration_seconds", optNullInt(json, "active_rest_duration_seconds"))
            put("active_rest_set_record_id", optNullLong(json, "active_rest_set_record_id"))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("occurrence_date", optNullLong(json, "occurrence_date"))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertExerciseSession(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("exercise_sessions", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("session_id", json.getLong("session_id"))
            put("exercise_id", optNullLong(json, "exercise_id"))
            put("exercise_name_snapshot", json.getString("exercise_name_snapshot"))
            put("primary_muscle_group_snapshot", json.getString("primary_muscle_group_snapshot"))
            put("target_sets", json.optInt("target_sets", 3))
            put("target_reps_min", optNullInt(json, "target_reps_min"))
            put("target_reps_max", optNullInt(json, "target_reps_max"))
            put("target_weight_kg", optNullDouble(json, "target_weight_kg"))
            put("target_rpe", optNullDouble(json, "target_rpe"))
            put("target_rir", optNullInt(json, "target_rir"))
            put("planned_rest_seconds", json.optInt("planned_rest_seconds", 90))
            put("notes", optNullString(json, "notes"))
            put("sort_order", json.optInt("sort_order", 0))
            put("is_skipped", boolToInt(json, "is_skipped"))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertSetRecord(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("set_records", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("exercise_session_id", json.getLong("exercise_session_id"))
            put("set_number", json.getInt("set_number"))
            put("set_type", json.optString("set_type", "WORKING"))
            put("reps", optNullInt(json, "reps"))
            put("weight_kg", optNullDouble(json, "weight_kg"))
            put("rpe", optNullDouble(json, "rpe"))
            put("rir", optNullInt(json, "rir"))
            put("rest_seconds", optNullInt(json, "rest_seconds"))
            put("completed", boolToInt(json, "completed"))
            put("notes", optNullString(json, "notes"))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertWorkoutPlanOverride(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("workout_plan_overrides", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("schedule_id", json.getLong("schedule_id"))
            put("template_id", json.getLong("template_id"))
            put("occurrence_date", json.getLong("occurrence_date"))
            put("planned_date", optNullLong(json, "planned_date"))
            put("action", json.getString("action"))
            put("notes", optNullString(json, "notes"))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertReminder(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("reminders", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("schedule_id", optNullLong(json, "schedule_id"))
            put("label", json.getString("label"))
            put("time_of_day_minutes", json.getInt("time_of_day_minutes"))
            put("days_of_week_mask", json.getInt("days_of_week_mask"))
            put("zone_id", json.getString("zone_id"))
            put("is_enabled", boolToInt(json, "is_enabled", true))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertCheckIn(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("check_ins", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("date", json.getLong("date"))
            put("session_id", optNullLong(json, "session_id"))
            put("mood", optNullInt(json, "mood"))
            put("energy_level", optNullInt(json, "energy_level"))
            put("notes", optNullString(json, "notes"))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertUserProfile(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("user_profiles", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("gender", json.getString("gender"))
            put("birthday", json.getLong("birthday"))
            put("height_cm", optNullDouble(json, "height_cm"))
            put("activity_level", json.getString("activity_level"))
            put("goal_type", json.getString("goal_type"))
            put("target_body_fat", optNullDouble(json, "target_body_fat"))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertBodyMeasurement(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("body_measurements", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("date", json.getLong("date"))
            put("weight_kg", optNullDouble(json, "weight_kg"))
            put("body_fat_percent", optNullDouble(json, "body_fat_percent"))
            put("muscle_kg", optNullDouble(json, "muscle_kg"))
            put("waist_cm", optNullDouble(json, "waist_cm"))
            put("note", optNullString(json, "note"))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertFoodRecord(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("food_records", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("date", json.getLong("date"))
            put("meal_type", json.getString("meal_type"))
            put("food_name", json.getString("food_name"))
            put("calories", optNullDouble(json, "calories"))
            put("protein_grams", optNullDouble(json, "protein_grams"))
            put("carbs_grams", optNullDouble(json, "carbs_grams"))
            put("fat_grams", optNullDouble(json, "fat_grams"))
            put("amount", optNullString(json, "amount"))
            put("note", optNullString(json, "note"))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertMediaRecord(writableDb: SupportSQLiteDatabase, json: JSONObject) {
        insert("media_records", writableDb, ContentValues().apply {
            put("id", json.getLong("id"))
            put("media_type", json.getString("media_type"))
            put("relative_path", json.getString("relative_path"))
            put("mime_type", json.getString("mime_type"))
            put("captured_at", json.getLong("captured_at"))
            put("date", json.getLong("date"))
            put("width", optNullInt(json, "width"))
            put("height", optNullInt(json, "height"))
            put("duration_millis", optNullLong(json, "duration_millis"))
            put("size_bytes", json.getLong("size_bytes"))
            put("workout_session_id", optNullLong(json, "workout_session_id"))
            put("body_measurement_id", optNullLong(json, "body_measurement_id"))
            put("check_in_id", optNullLong(json, "check_in_id"))
            put("exercise_session_id", optNullLong(json, "exercise_session_id"))
            put("food_record_id", optNullLong(json, "food_record_id"))
            put("category", json.getString("category"))
            put("pose_tag", optNullString(json, "pose_tag"))
            put("note", optNullString(json, "note"))
            put("is_favorite", boolToInt(json, "is_favorite"))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    // ── Clear all tables (FK-safe order) ────────────────────────────────────

    private fun clearAllTables(writableDb: SupportSQLiteDatabase) {
        writableDb.execSQL("PRAGMA foreign_keys = OFF")
        try {
            writableDb.delete("media_records", null, null)
            writableDb.delete("food_records", null, null)
            writableDb.delete("body_measurements", null, null)
            writableDb.delete("user_profiles", null, null)
            writableDb.delete("check_ins", null, null)
            writableDb.delete("reminders", null, null)
            writableDb.delete("workout_plan_overrides", null, null)
            writableDb.delete("set_records", null, null)
            writableDb.delete("exercise_sessions", null, null)
            writableDb.delete("workout_sessions", null, null)
            writableDb.delete("workout_schedules", null, null)
            writableDb.delete("workout_template_exercises", null, null)
            writableDb.delete("workout_templates", null, null)
            writableDb.delete("exercises", null, null)
            writableDb.delete("exercise_categories", null, null)
        } finally {
            writableDb.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    // ── Media restore ───────────────────────────────────────────────────────

    private fun restoreMediaFile(relativePath: String, data: ByteArray) {
        val file = appMediaStorage.resolveFile(relativePath)
        file.parentFile?.mkdirs()
        file.writeBytes(data)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun optNullString(json: JSONObject, key: String): String? {
        if (!json.has(key) || json.isNull(key)) return null
        return json.optString(key, null)
    }

    private fun optNullLong(json: JSONObject, key: String): Long? {
        if (!json.has(key) || json.isNull(key)) return null
        return json.optLong(key)
    }

    private fun optNullInt(json: JSONObject, key: String): Int? {
        if (!json.has(key) || json.isNull(key)) return null
        return json.optInt(key)
    }

    private fun optNullDouble(json: JSONObject, key: String): Double? {
        if (!json.has(key) || json.isNull(key)) return null
        return json.optDouble(key)
    }

    private fun boolToInt(json: JSONObject, key: String, default: Boolean = false): Int {
        return if (json.has(key) && !json.isNull(key)) {
            if (json.optBoolean(key, default)) 1 else 0
        } else {
            if (default) 1 else 0
        }
    }

}

/**
 * Exception thrown when a backup cannot be imported.
 */
class ImportException(message: String, cause: Throwable? = null) : Exception(message, cause)
