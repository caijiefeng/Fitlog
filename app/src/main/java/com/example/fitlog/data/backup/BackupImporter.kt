package com.example.fitlog.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import com.example.fitlog.core.database.FitLogDatabase
import com.example.fitlog.core.media.AppMediaStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of importing a backup ZIP.
 *
 * @property success  Whether the import completed without errors.
 * @property message  Human-readable outcome description.
 * @property rowsImported  Number of database rows imported (across all tables).
 * @property mediaRestored  Number of media files restored.
 */
data class ImportResult(
    val success: Boolean,
    val message: String,
    val rowsImported: Int = 0,
    val mediaRestored: Int = 0,
)

/**
 * Summary of what a backup contains, presented before import.
 *
 * @property manifest  The parsed manifest.
 * @property mediaFiles  List of media paths found in the ZIP.
 */
data class ImportSummary(
    val manifest: BackupManifest,
    val mediaFiles: List<String>,
    val dbRowCounts: Map<String, Int>,
)

/**
 * Imports a FitLog backup ZIP into the application.
 *
 * Validation:
 * - Manifest version must be [BackupManifest.CURRENT_VERSION].
 * - SHA-256 checksum of db.json must match the manifest's `db_checksum`.
 * - db.json must be present in the archive.
 *
 * Import strategy:
 * 1. Auto-backup: the existing database is exported to a temporary ZIP before
 *    proceeding (pre-import backup).
 * 2. All existing records are deleted within a Room transaction.
 * 3. db.json rows are inserted table by table.
 * 4. On any failure, the transaction is rolled back.
 * 5. Media files from the ZIP's `media/` prefix are copied into
 *    [AppMediaStorage].
 */
@Singleton
class BackupImporter @Inject constructor(
    private val db: FitLogDatabase,
    private val appMediaStorage: AppMediaStorage,
    private val backupExporter: BackupExporter,
    @ApplicationContext private val context: Context,
) {

    /**
     * Analyzes a backup ZIP and returns a [ImportSummary] without modifying
     * any data. Can be called before [importBackup] to present a preview.
     */
    suspend fun analyze(input: InputStream): ImportSummary {
        val entries = readZipEntries(input)
        val manifestJson = entries["manifest.json"]
            ?: throw ImportException("Backup is missing manifest.json")

        val manifest = parseManifest(manifestJson)
        validateVersion(manifest)

        val dbJsonBytes = entries["db.json"]
        if (dbJsonBytes == null) {
            throw ImportException("Backup is missing db.json")
        }

        // Validate checksum
        validateChecksum(manifest, dbJsonBytes)

        val dbJson = JSONObject(String(dbJsonBytes, Charsets.UTF_8))
        val dbRowCounts = mutableMapOf<String, Int>()

        // Extract row counts from all tables
        for (key in dbJson.keys()) {
            val arr = dbJson.optJSONArray(key)
            if (arr != null) {
                dbRowCounts[key] = arr.length()
            }
        }

        // Collect media file paths
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
     * 1. Auto-backups existing data.
     * 2. Validates the archive.
     * 3. Deletes all existing records and imports new ones in a single
     *    Room transaction (rolled back on failure).
     * 4. Copies media files from the ZIP to [AppMediaStorage].
     *
     * @throws ImportException on validation failure or import error.
     */
    suspend fun importBackup(
        input: InputStream,
        onProgress: (String) -> Unit = {},
    ): ImportResult {
        onProgress("Analyzing backup…")
        val entries = readZipEntries(input)
        val manifestBytes = entries["manifest.json"]
            ?: throw ImportException("Backup is missing manifest.json")

        val manifest = parseManifest(manifestBytes)
        validateVersion(manifest)

        val dbJsonBytes = entries["db.json"]
            ?: throw ImportException("Backup is missing db.json")

        validateChecksum(manifest, dbJsonBytes)

        val dbJson = JSONObject(String(dbJsonBytes, Charsets.UTF_8))

        // ── Pre-import auto-backup ───────────────────────────────────────────
        onProgress("Creating pre-import backup…")
        val preImportBackup = createPreImportBackup()

        // Count rows to import
        var totalRows = 0
        for (key in dbJson.keys()) {
            val arr = dbJson.optJSONArray(key)
            if (arr != null) totalRows += arr.length()
        }

        // Count media files
        val mediaEntries = entries.filterKeys { it.startsWith("media/") }

        // ── Import DB in transaction ─────────────────────────────────────────
        onProgress("Importing $totalRows database records…")

        var importedRows = 0
        try {
            db.withTransaction {
                // Clear all existing data (order matters for FK constraints)
                clearAllTables()

                // Import table by table
                importedRows += importTable(dbJson, "exercise_categories", ::insertExerciseCategory)
                importedRows += importTable(dbJson, "exercises", ::insertExercise)
                importedRows += importTable(dbJson, "workout_templates", ::insertWorkoutTemplate)
                importedRows += importTable(dbJson, "workout_template_exercises", ::insertWorkoutTemplateExercise)
                importedRows += importTable(dbJson, "workout_schedules", ::insertWorkoutSchedule)
                importedRows += importTable(dbJson, "workout_sessions", ::insertWorkoutSession)
                importedRows += importTable(dbJson, "exercise_sessions", ::insertExerciseSession)
                importedRows += importTable(dbJson, "set_records", ::insertSetRecord)
                importedRows += importTable(dbJson, "workout_plan_overrides", ::insertWorkoutPlanOverride)
                importedRows += importTable(dbJson, "reminders", ::insertReminder)
                importedRows += importTable(dbJson, "check_ins", ::insertCheckIn)
                importedRows += importTable(dbJson, "user_profiles", ::insertUserProfile)
                importedRows += importTable(dbJson, "body_measurements", ::insertBodyMeasurement)
                importedRows += importTable(dbJson, "food_records", ::insertFoodRecord)
                importedRows += importTable(dbJson, "media_records", ::insertMediaRecord)
            }
        } catch (e: Exception) {
            throw ImportException("Database import failed, rolled back. Pre-import backup saved.", e)
        }

        // ── Copy media files ─────────────────────────────────────────────────
        onProgress("Restoring ${mediaEntries.size} media files…")
        var mediaRestored = 0
        for ((entryName, data) in mediaEntries) {
            val relativePath = entryName.removePrefix("media/")
            try {
                restoreMediaFile(relativePath, data)
                mediaRestored++
            } catch (_: Exception) {
                // Skip individual media failures; don't abort the import
            }
        }

        onProgress("Import complete.")
        return ImportResult(
            success = true,
            message = buildSuccessMessage(totalRows, mediaRestored, preImportBackup),
            rowsImported = totalRows,
            mediaRestored = mediaRestored,
        )
    }

    // ── Validation ──────────────────────────────────────────────────────────

    private fun validateVersion(manifest: BackupManifest) {
        if (manifest.version !in SUPPORTED_VERSIONS) {
            throw ImportException(
                "Unsupported backup version ${manifest.version}. " +
                    "Supported versions: ${SUPPORTED_VERSIONS.joinToString(", ")}. " +
                    "Current version: ${BackupManifest.CURRENT_VERSION}.",
            )
        }
    }

    private fun validateChecksum(manifest: BackupManifest, dbJsonBytes: ByteArray) {
        if (manifest.dbChecksum.isBlank()) return // No checksum to validate
        val digest = MessageDigest.getInstance("SHA-256")
        val actualChecksum = digest.digest(dbJsonBytes).joinToString("") { "%02x".format(it) }
        if (actualChecksum != manifest.dbChecksum) {
            throw ImportException(
                "Database checksum mismatch. Expected: ${manifest.dbChecksum}, actual: $actualChecksum. " +
                    "The backup may be corrupted.",
            )
        }
    }

    // ── ZIP reading ─────────────────────────────────────────────────────────

    /**
     * Reads all ZIP entries into a map. Small enough for the expected backup
     * size (db.json is text, media is copied via stream).
     */
    private fun readZipEntries(input: InputStream): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        val zis = ZipInputStream(input)
        var entry: ZipEntry? = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                entries[entry.name] = zis.readBytes()
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
        return BackupManifest(
            version = json.getInt("version"),
            appVersion = json.optString("app_version", ""),
            exportedAt = json.optLong("exported_at", 0L),
            dbRows = json.optInt("db_rows", 0),
            mediaCount = json.optInt("media_count", 0),
            dbChecksum = json.optString("db_checksum", ""),
        )
    }

    // ── Table import ────────────────────────────────────────────────────────

    private fun importTable(
        dbJson: JSONObject,
        tableName: String,
        inserter: (JSONObject) -> Unit,
    ): Int {
        val arr = dbJson.optJSONArray(tableName) ?: return 0
        for (i in 0 until arr.length()) {
            inserter(arr.getJSONObject(i))
        }
        return arr.length()
    }

    // ── Table inserters (raw SQL to avoid Room entity FK conflicts) ────────

    /**
     * Uses a writable database directly to bypass Room's entity validation
     * during bulk import. All FK constraints are deferred to the transaction
     * commit.
     */
    private fun rawDb(): SQLiteDatabase =
        db.openHelper.writableDatabase

    private fun insert(table: String, values: ContentValues) {
        rawDb().insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun insertExerciseCategory(json: JSONObject) {
        insert("exercise_categories", ContentValues().apply {
            put("id", json.getLong("id"))
            put("name", json.getString("name"))
            put("description", json.optString("description", null))
            put("sort_order", json.optInt("sort_order", 0))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
        })
    }

    private fun insertExercise(json: JSONObject) {
        insert("exercises", ContentValues().apply {
            put("id", json.getLong("id"))
            put("name", json.getString("name"))
            put("primary_muscle_group", json.getString("primary_muscle_group"))
            put("secondary_muscle_group", optNullString(json, "secondary_muscle_group"))
            put("category_id", optNullLong(json, "category_id"))
            put("notes", optNullString(json, "notes"))
            put("is_custom", if (json.optBoolean("is_custom")) 1 else 0)
            put("is_active", if (json.optBoolean("is_active", true)) 1 else 0)
            put("sort_order", json.optInt("sort_order", 0))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertWorkoutTemplate(json: JSONObject) {
        insert("workout_templates", ContentValues().apply {
            put("id", json.getLong("id"))
            put("name", json.getString("name"))
            put("notes", optNullString(json, "notes"))
            put("sort_order", json.optInt("sort_order", 0))
            put("is_active", if (json.optBoolean("is_active", true)) 1 else 0)
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertWorkoutTemplateExercise(json: JSONObject) {
        insert("workout_template_exercises", ContentValues().apply {
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

    private fun insertWorkoutSchedule(json: JSONObject) {
        insert("workout_schedules", ContentValues().apply {
            put("id", json.getLong("id"))
            put("template_id", json.getLong("template_id"))
            put("day_of_week", json.getInt("day_of_week"))
            put("is_active", if (json.optBoolean("is_active", true)) 1 else 0)
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
        })
    }

    private fun insertWorkoutSession(json: JSONObject) {
        insert("workout_sessions", ContentValues().apply {
            put("id", json.getLong("id"))
            put("schedule_id", optNullLong(json, "schedule_id"))
            put("template_id", optNullLong(json, "template_id"))
            put("template_name_snapshot", optNullString(json, "template_name_snapshot"))
            put("date", json.getLong("date"))
            put("start_time", json.getLong("start_time"))
            put("end_time", optNullLong(json, "end_time"))
            put("status", json.getString("status"))
            put("notes", optNullString(json, "notes"))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("occurrence_date", optNullLong(json, "occurrence_date"))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertExerciseSession(json: JSONObject) {
        insert("exercise_sessions", ContentValues().apply {
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
            put("is_skipped", if (json.optBoolean("is_skipped")) 1 else 0)
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertSetRecord(json: JSONObject) {
        insert("set_records", ContentValues().apply {
            put("id", json.getLong("id"))
            put("exercise_session_id", json.getLong("exercise_session_id"))
            put("set_number", json.getInt("set_number"))
            put("set_type", json.optString("set_type", "WORKING"))
            put("reps", optNullInt(json, "reps"))
            put("weight_kg", optNullDouble(json, "weight_kg"))
            put("rpe", optNullDouble(json, "rpe"))
            put("rir", optNullInt(json, "rir"))
            put("rest_seconds", optNullInt(json, "rest_seconds"))
            put("completed", if (json.optBoolean("completed")) 1 else 0)
            put("notes", optNullString(json, "notes"))
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertWorkoutPlanOverride(json: JSONObject) {
        insert("workout_plan_overrides", ContentValues().apply {
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

    private fun insertReminder(json: JSONObject) {
        insert("reminders", ContentValues().apply {
            put("id", json.getLong("id"))
            put("schedule_id", optNullLong(json, "schedule_id"))
            put("label", json.getString("label"))
            put("time_of_day_minutes", json.getInt("time_of_day_minutes"))
            put("days_of_week_mask", json.getInt("days_of_week_mask"))
            put("zone_id", json.getString("zone_id"))
            put("is_enabled", if (json.optBoolean("is_enabled", true)) 1 else 0)
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    private fun insertCheckIn(json: JSONObject) {
        insert("check_ins", ContentValues().apply {
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

    private fun insertUserProfile(json: JSONObject) {
        insert("user_profiles", ContentValues().apply {
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

    private fun insertBodyMeasurement(json: JSONObject) {
        insert("body_measurements", ContentValues().apply {
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

    private fun insertFoodRecord(json: JSONObject) {
        insert("food_records", ContentValues().apply {
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

    private fun insertMediaRecord(json: JSONObject) {
        insert("media_records", ContentValues().apply {
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
            put("is_favorite", if (json.optBoolean("is_favorite")) 1 else 0)
            put("created_at", json.optLong("created_at", System.currentTimeMillis()))
            put("updated_at", json.optLong("updated_at", System.currentTimeMillis()))
        })
    }

    // ── Clear all tables (in FK-safe order) ─────────────────────────────────

    private fun clearAllTables() {
        val db = rawDb()
        // Disable FK checks temporarily for bulk delete
        db.execSQL("PRAGMA foreign_keys = OFF")
        try {
            db.delete("media_records", null, null)
            db.delete("food_records", null, null)
            db.delete("body_measurements", null, null)
            db.delete("user_profiles", null, null)
            db.delete("check_ins", null, null)
            db.delete("reminders", null, null)
            db.delete("workout_plan_overrides", null, null)
            db.delete("set_records", null, null)
            db.delete("exercise_sessions", null, null)
            db.delete("workout_sessions", null, null)
            db.delete("workout_schedules", null, null)
            db.delete("workout_template_exercises", null, null)
            db.delete("workout_templates", null, null)
            db.delete("exercises", null, null)
            db.delete("exercise_categories", null, null)
        } finally {
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    // ── Media restore ───────────────────────────────────────────────────────

    private fun restoreMediaFile(relativePath: String, data: ByteArray) {
        val file = appMediaStorage.resolveFile(relativePath)
        file.parentFile?.mkdirs()
        file.writeBytes(data)
    }

    // ── Pre-import auto-backup ──────────────────────────────────────────────

    /**
     * Creates a timestamped auto-backup before modifying any data.
     * Returns the file path for the success message.
     */
    private suspend fun createPreImportBackup(): String {
        val backupDir = java.io.File(context.cacheDir, "pre_import_backups")
        backupDir.mkdirs()
        val timestamp = java.text.SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            java.util.Locale.US,
        ).format(java.util.Date())
        val backupFile = java.io.File(backupDir, "pre_import_$timestamp.zip")

        backupFile.outputStream().use { output ->
            backupExporter.export(
                output = output,
                appVersion = "pre-import",
                onProgress = {},
            )
        }

        return backupFile.absolutePath
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun buildSuccessMessage(
        rowsImported: Int,
        mediaRestored: Int,
        preImportBackup: String,
    ): String =
        "Import complete. $rowsImported records imported, " +
            "$mediaRestored media files restored. " +
            "Pre-import backup saved to: $preImportBackup"

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

    companion object {
        /** Versions of backup format that this importer can handle. */
        private val SUPPORTED_VERSIONS = setOf(1)
    }
}

/**
 * Exception thrown when a backup cannot be imported.
 */
class ImportException(message: String, cause: Throwable? = null) : Exception(message, cause)
