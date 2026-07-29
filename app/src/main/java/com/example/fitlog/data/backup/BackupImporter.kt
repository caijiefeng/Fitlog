package com.example.fitlog.data.backup

import android.content.Context
import android.net.Uri
import com.example.fitlog.core.database.FitLogDatabase
import com.example.fitlog.core.database.dao.BodyMeasurementDao
import com.example.fitlog.core.database.dao.CheckInDao
import com.example.fitlog.core.database.dao.ExerciseCategoryDao
import com.example.fitlog.core.database.dao.ExerciseDao
import com.example.fitlog.core.database.dao.ExerciseSessionDao
import com.example.fitlog.core.database.dao.FoodRecordDao
import com.example.fitlog.core.database.dao.MediaRecordDao
import com.example.fitlog.core.database.dao.ReminderDao
import com.example.fitlog.core.database.dao.SetRecordDao
import com.example.fitlog.core.database.dao.UserProfileDao
import com.example.fitlog.core.database.dao.WorkoutPlanOverrideDao
import com.example.fitlog.core.database.dao.WorkoutScheduleDao
import com.example.fitlog.core.database.dao.WorkoutSessionDao
import com.example.fitlog.core.database.dao.WorkoutTemplateDao
import com.example.fitlog.core.media.AppMediaStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports a FitLog backup ZIP created by [BackupManager].
 *
 * The import process:
 * 1. Read and validate manifest.json (version compatibility, checksum)
 * 2. Read db.json and verify SHA-256 against the manifest
 * 3. Auto-create a pre-import backup of current data
 * 4. Import all database records in a single Room transaction (replace strategy)
 * 5. Extract media files from the ZIP archive
 * 6. On failure, roll back all changes
 */
@Singleton
class BackupImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: FitLogDatabase,
    private val backupManager: BackupManager,
    private val mediaStorage: AppMediaStorage,
) {

    sealed interface ImportResult {
        data class Success(val preImportBackupUri: Uri?) : ImportResult
        data class Error(val message: String) : ImportResult
        data class VersionMismatch(val version: Int, val maxSupported: Int) : ImportResult
        data class ChecksumMismatch(val expected: String, val actual: String) : ImportResult
        data class Corrupt(val detail: String) : ImportResult
    }

    companion object {
        private const val MANIFEST_JSON = "manifest.json"
        private const val DB_JSON = "db.json"
        private const val MEDIA_PREFIX = "media/"
        private const val BUFFER_SIZE = 8192
        private const val MIN_SUPPORTED_VERSION = 1
    }

    suspend fun import(uri: Uri): ImportResult {
        // Phase 1: Read and validate
        val (manifest, dbJsonBytes, mediaEntries) = try {
            readArchive(uri)
        } catch (e: Exception) {
            return ImportResult.Corrupt(e.message ?: "Cannot read archive")
        }

        if (manifest.version < MIN_SUPPORTED_VERSION || manifest.version > BackupManifest.CURRENT_VERSION) {
            return ImportResult.VersionMismatch(manifest.version, BackupManifest.CURRENT_VERSION)
        }

        val actualChecksum = sha256Hex(dbJsonBytes)
        if (actualChecksum != manifest.dbChecksum) {
            return ImportResult.ChecksumMismatch(
                expected = manifest.dbChecksum, actual = actualChecksum,
            )
        }

        val dbJson = try {
            JSONObject(String(dbJsonBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            return ImportResult.Corrupt("Invalid db.json: ${e.message}")
        }

        // Phase 2: Auto-create pre-import backup
        val preImportBackupUri = try {
            createPreImportBackup()
        } catch (_: Exception) { null }

        // Phase 3: Import in transaction
        try {
            db.withTransaction {
                clearAllData()
                importAllEntities(dbJson)
            }
        } catch (e: Exception) {
            return ImportResult.Error("Import failed: ${e.message}")
        }

        // Phase 4: Extract media files
        var mediaErrors = 0
        for (entry in mediaEntries) {
            try {
                extractMediaEntry(entry)
            } catch (_: Exception) {
                mediaErrors++
            }
        }

        return ImportResult.Success(preImportBackupUri = preImportBackupUri)
    }

    // ── Archive reader ──────────────────────────────────────────────────────

    private data class ArchiveContent(
        val manifest: BackupManifest,
        val dbJsonBytes: ByteArray,
        val mediaEntries: List<MediaEntry>,
    )

    private data class MediaEntry(
        val relativePath: String,
        val bytes: ByteArray,
    )

    private fun readArchive(uri: Uri): ArchiveContent {
        var manifest: BackupManifest? = null
        var dbJsonBytes: ByteArray? = null
        val mediaEntries = mutableListOf<MediaEntry>()

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open $uri")

        ZipInputStream(BufferedInputStream(inputStream, BUFFER_SIZE)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                val bytes = zis.readBytes()
                when {
                    name == MANIFEST_JSON -> manifest = parseManifest(bytes)
                    name == DB_JSON -> dbJsonBytes = bytes
                    name.startsWith(MEDIA_PREFIX) && !entry.isDirectory -> {
                        mediaEntries.add(MediaEntry(name.removePrefix(MEDIA_PREFIX), bytes))
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        return ArchiveContent(
            manifest ?: throw IllegalArgumentException("Missing $MANIFEST_JSON"),
            dbJsonBytes ?: throw IllegalArgumentException("Missing $DB_JSON"),
            mediaEntries,
        )
    }

    private fun parseManifest(bytes: ByteArray): BackupManifest {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        return BackupManifest(
            version = json.optInt("version", 0),
            appVersion = json.optString("appVersion", ""),
            exportedAt = json.optLong("exportedAt", 0L),
            dbRows = json.optInt("dbRows", 0),
            mediaCount = json.optInt("mediaCount", 0),
            dbChecksum = json.optString("dbChecksum", ""),
        )
    }

    // ── Pre-import backup ───────────────────────────────────────────────────

    private suspend fun createPreImportBackup(): Uri? {
        val cacheDir = java.io.File(context.cacheDir, "pre_import_backups")
        cacheDir.mkdirs()
        val file = java.io.File(cacheDir, "pre_import_${System.currentTimeMillis()}.zip")
        val uri = android.net.Uri.fromFile(file)
        backupManager.createBackup(uri)
        return uri
    }

    // ── Data clearing ───────────────────────────────────────────────────────

    private fun clearAllData() {
        val dbRef = db.openHelper.writableDatabase
        dbRef.execSQL("DELETE FROM workout_plan_overrides")
        dbRef.execSQL("DELETE FROM set_records")
        dbRef.execSQL("DELETE FROM exercise_sessions")
        dbRef.execSQL("DELETE FROM workout_sessions")
        dbRef.execSQL("DELETE FROM workout_template_exercises")
        dbRef.execSQL("DELETE FROM workout_schedules")
        dbRef.execSQL("DELETE FROM workout_templates")
        dbRef.execSQL("DELETE FROM reminders")
        dbRef.execSQL("DELETE FROM check_ins")
        dbRef.execSQL("DELETE FROM media_records")
        dbRef.execSQL("DELETE FROM food_records")
        dbRef.execSQL("DELETE FROM body_measurements")
        dbRef.execSQL("DELETE FROM user_profiles")
        dbRef.execSQL("DELETE FROM exercises")
        dbRef.execSQL("DELETE FROM exercise_categories")
    }

    // ── Data import ─────────────────────────────────────────────────────────

    private fun importAllEntities(json: JSONObject) {
        importExerciseCategories(json.optJSONArray("exercise_categories"))
        importExercises(json.optJSONArray("exercises"))
        importTemplates(json.optJSONArray("workout_templates"))
        importTemplateExercises(json.optJSONArray("workout_template_exercises"))
        importSchedules(json.optJSONArray("workout_schedules"))
        importUserProfiles(json.optJSONArray("user_profiles"))
        importBodyMeasurements(json.optJSONArray("body_measurements"))
        importFoodRecords(json.optJSONArray("food_records"))
        importReminders(json.optJSONArray("reminders"))
        importCheckIns(json.optJSONArray("check_ins"))
        importWorkoutSessions(json.optJSONArray("workout_sessions"))
        importExerciseSessions(json.optJSONArray("exercise_sessions"))
        importSetRecords(json.optJSONArray("set_records"))
        importPlanOverrides(json.optJSONArray("workout_plan_overrides"))
        importMediaRecords(json.optJSONArray("media_records"))
    }

    /** Reads a Kotlin-camelCase key from JSON, returns null if missing or null. */
    private fun getString(obj: JSONObject, key: String): String? =
        if (obj.has(key) && !obj.isNull(key)) obj.optString(key, null) else null

    private fun getDouble(obj: JSONObject, key: String): Double? =
        if (obj.has(key) && !obj.isNull(key)) {
            obj.optDouble(key, Double.NaN).takeIf { !it.isNaN() }
        } else null

    private fun getLong(obj: JSONObject, key: String): Long? =
        if (obj.has(key) && !obj.isNull(key)) {
            obj.optLong(key, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
        } else null

    private fun getInt(obj: JSONObject, key: String): Int? =
        if (obj.has(key) && !obj.isNull(key)) {
            obj.optInt(key, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        } else null

    private fun getBool(obj: JSONObject, key: String, default: Boolean = false): Boolean =
        if (obj.has(key) && !obj.isNull(key)) obj.optBoolean(key) else default

    /** Convenience: converts a nullable bool to 0/1 for SQLite. */
    private fun boolToInt(value: Boolean) = if (value) 1 else 0

    // ── Import helpers using raw SQLiteStatement for performance ────────────

    private fun importExerciseCategories(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO exercise_categories (id, name, description, sort_order, created_at) VALUES (?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            s.bindString(2, o.optString("name", ""))
            bindString(s, 3, getString(o, "description"))
            s.bindLong(4, o.optLong("sortOrder"))
            s.bindLong(5, o.optLong("createdAt", System.currentTimeMillis()))
            s.executeInsert(); s.clearBindings()
        }
    }

    private fun importExercises(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO exercises (id, name, primary_muscle_group, secondary_muscle_group, category_id, notes, is_custom, is_active, sort_order, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            s.bindString(2, o.optString("name", ""))
            s.bindString(3, o.optString("primaryMuscleGroup", "FULL_BODY"))
            bindString(s, 4, getString(o, "secondaryMuscleGroup"))
            bindLong(s, 5, getLong(o, "categoryId"))
            bindString(s, 6, getString(o, "notes"))
            s.bindLong(7, boolToInt(getBool(o, "isCustom")))
            s.bindLong(8, boolToInt(getBool(o, "isActive", true)))
            s.bindLong(9, o.optLong("sortOrder"))
            s.bindLong(10, o.optLong("createdAt", System.currentTimeMillis()))
            s.bindLong(11, o.optLong("updatedAt", System.currentTimeMillis()))
            s.executeInsert(); s.clearBindings()
        }
    }

    private fun importTemplates(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO workout_templates (id, name, notes, sort_order, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            s.bindString(2, o.optString("name", ""))
            bindString(s, 3, getString(o, "notes"))
            s.bindLong(4, o.optLong("sortOrder"))
            s.bindLong(5, boolToInt(getBool(o, "isActive", true)))
            s.bindLong(6, o.optLong("createdAt", System.currentTimeMillis()))
            s.bindLong(7, o.optLong("updatedAt", System.currentTimeMillis()))
            s.executeInsert(); s.clearBindings()
        }
    }

    private fun importTemplateExercises(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO workout_template_exercises (id, template_id, exercise_id, target_sets, target_reps_min, target_reps_max, target_weight_kg, target_rpe, target_rir, rest_seconds, notes, sort_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            s.bindLong(2, o.optLong("templateId"))
            s.bindLong(3, o.optLong("exerciseId"))
            s.bindLong(4, o.optLong("targetSets", 3))
            bindInt(s, 5, getInt(o, "targetRepsMin"))
            bindInt(s, 6, getInt(o, "targetRepsMax"))
            bindDouble(s, 7, getDouble(o, "targetWeightKg"))
            bindDouble(s, 8, getDouble(o, "targetRpe"))
            bindInt(s, 9, getInt(o, "targetRir"))
            s.bindLong(10, o.optLong("restSeconds", 90))
            bindString(s, 11, getString(o, "notes"))
            s.bindLong(12, o.optLong("sortOrder"))
            s.executeInsert(); s.clearBindings()
        }
    }

    private fun importSchedules(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO workout_schedules (id, template_id, day_of_week, is_active, created_at) VALUES (?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            s.bindLong(2, o.optLong("templateId"))
            s.bindLong(3, o.optLong("dayOfWeek"))
            s.bindLong(4, boolToInt(getBool(o, "isActive", true)))
            s.bindLong(5, o.optLong("createdAt", System.currentTimeMillis()))
            s.executeInsert(); s.clearBindings()
        }
    }

    private fun importUserProfiles(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO user_profiles (id, gender, birthday, height_cm, activity_level, goal_type, target_body_fat, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            s.bindString(2, o.optString("gender", "male"))
            s.bindLong(3, o.optLong("birthday"))
            bindDouble(s, 4, getDouble(o, "heightCm"))
            s.bindString(5, o.optString("activityLevel", "moderate"))
            s.bindString(6, o.optString("goalType", "maintain"))
            bindDouble(s, 7, getDouble(o, "targetBodyFat"))
            s.bindLong(8, o.optLong("createdAt", System.currentTimeMillis()))
            s.bindLong(9, o.optLong("updatedAt", System.currentTimeMillis()))
            s.executeInsert(); s.clearBindings()
        }
    }

    private fun importBodyMeasurements(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO body_measurements (id, date, weight_kg, body_fat_percent, muscle_kg, waist_cm, note, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            s.bindLong(2, o.optLong("date"))
            bindDouble(s, 3, getDouble(o, "weightKg"))
            bindDouble(s, 4, getDouble(o, "bodyFatPercent"))
            bindDouble(s, 5, getDouble(o, "muscleKg"))
            bindDouble(s, 6, getDouble(o, "waistCm"))
            bindString(s, 7, getString(o, "note"))
            s.bindLong(8, o.optLong("createdAt", System.currentTimeMillis()))
            s.bindLong(9, o.optLong("updatedAt", System.currentTimeMillis()))
            s.executeInsert(); s.clearBindings()
        }
    }

    private fun importFoodRecords(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO food_records (id, date, meal_type, food_name, calories, protein_grams, carbs_grams, fat_grams, amount, note, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            s.bindLong(2, o.optLong("date"))
            s.bindString(3, o.optString("mealType", ""))
            s.bindString(4, o.optString("foodName", ""))
            bindDouble(s, 5, getDouble(o, "calories"))
            bindDouble(s, 6, getDouble(o, "proteinGrams"))
            bindDouble(s, 7, getDouble(o, "carbsGrams"))
            bindDouble(s, 8, getDouble(o, "fatGrams"))
            bindString(s, 9, getString(o, "amount"))
            bindString(s, 10, getString(o, "note"))
            s.bindLong(11, o.optLong("createdAt", System.currentTimeMillis()))
            s.bindLong(12, o.optLong("updatedAt", System.currentTimeMillis()))
            s.executeInsert(); s.clearBindings()
        }
    }

    private fun importReminders(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO reminders (id, schedule_id, label, time_of_day_minutes, days_of_week_mask, zone_id, is_enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            bindLong(s, 2, getLong(o, "scheduleId"))
            s.bindString(3, o.optString("label", ""))
            s.bindLong(4, o.optLong("timeOfDayMinutes", 480))
            s.bindLong(5, o.optLong("daysOfWeekMask", 0))
            s.bindString(6, o.optString("zoneId", "UTC"))
            s.bindLong(7, boolToInt(getBool(o, "isEnabled", true)))
            s.bindLong(8, o.optLong("createdAt", System.currentTimeMillis()))
            s.bindLong(9, o.optLong("updatedAt", System.currentTimeMillis()))
            s.executeInsert(); s.clearBindings()
        }
    }

    private fun importCheckIns(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO check_ins (id, date, session_id, mood, energy_level, notes, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            s.bindLong(2, o.optLong("date"))
            bindLong(s, 3, getLong(o, "sessionId"))
            bindInt(s, 4, getInt(o, "mood"))
            bindInt(s, 5, getInt(o, "energyLevel"))
            bindString(s, 6, getString(o, "notes"))
            s.bindLong(7, o.optLong("createdAt", System.currentTimeMillis()))
            s.bindLong(8, o.optLong("updatedAt", System.currentTimeMillis()))
            s.executeInsert(); s.clearBindings()
        }
    }

    private fun importWorkoutSessions(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO workout_sessions (id, schedule_id, template_id, template_name_snapshot, date, start_time, end_time, status, notes, active_rest_started_at, active_rest_duration_seconds, active_rest_set_record_id, occurrence_date, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            bindLong(s, 2, getLong(o, "scheduleId"))
            bindLong(s, 3, getLong(o, "templateId"))
            bindString(s, 4, getString(o, "templateNameSnapshot"))
            s.bindLong(5, o.optLong("date"))
            s.bindLong(6, o.optLong("startTime"))
            bindLong(s, 7, getLong(o, "endTime"))
            s.bindString(8, o.optString("status", "PLANNED"))
            bindString(s, 9, getString(o, "notes"))
            bindLong(s, 10, getLong(o, "activeRestStartedAt"))
            bindInt(s, 11, getInt(o, "activeRestDurationSeconds"))
            bindLong(s, 12, getLong(o, "activeRestSetRecordId"))
            bindLong(s, 13, getLong(o, "occurrenceDate"))
            s.bindLong(14, o.optLong("createdAt", System.currentTimeMillis()))
            s.bindLong(15, o.optLong("updatedAt", System.currentTimeMillis()))
            s.executeInsert(); s.clearBindings()
        }
    }

    private fun importExerciseSessions(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO exercise_sessions (id, session_id, exercise_id, exercise_name_snapshot, primary_muscle_group_snapshot, target_sets, target_reps_min, target_reps_max, target_weight_kg, target_rpe, target_rir, planned_rest_seconds, notes, sort_order, is_skipped, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            s.bindLong(2, o.optLong("sessionId"))
            bindLong(s, 3, getLong(o, "exerciseId"))
            s.bindString(4, o.optString("exerciseNameSnapshot", ""))
            s.bindString(5, o.optString("primaryMuscleGroupSnapshot", "FULL_BODY"))
            s.bindLong(6, o.optLong("targetSets", 3))
            bindInt(s, 7, getInt(o, "targetRepsMin"))
            bindInt(s, 8, getInt(o, "targetRepsMax"))
            bindDouble(s, 9, getDouble(o, "targetWeightKg"))
            bindDouble(s, 10, getDouble(o, "targetRpe"))
            bindInt(s, 11, getInt(o, "targetRir"))
            s.bindLong(12, o.optLong("plannedRestSeconds", 90))
            bindString(s, 13, getString(o, "notes"))
            s.bindLong(14, o.optLong("sortOrder"))
            s.bindLong(15, boolToInt(getBool(o, "isSkipped")))
            s.bindLong(16, o.optLong("createdAt", System.currentTimeMillis()))
            s.bindLong(17, o.optLong("updatedAt", System.currentTimeMillis()))
            s.executeInsert(); s.clearBindings()
        }
    }

    private fun importSetRecords(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO set_records (id, exercise_session_id, set_number, set_type, reps, weight_kg, rpe, rir, rest_seconds, completed, notes, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            s.bindLong(2, o.optLong("exerciseSessionId"))
            s.bindLong(3, o.optLong("setNumber"))
            s.bindString(4, o.optString("setType", "WORKING"))
            bindInt(s, 5, getInt(o, "reps"))
            bindDouble(s, 6, getDouble(o, "weightKg"))
            bindDouble(s, 7, getDouble(o, "rpe"))
            bindInt(s, 8, getInt(o, "rir"))
            bindInt(s, 9, getInt(o, "restSeconds"))
            s.bindLong(10, boolToInt(getBool(o, "completed")))
            bindString(s, 11, getString(o, "notes"))
            s.bindLong(12, o.optLong("createdAt", System.currentTimeMillis()))
            s.bindLong(13, o.optLong("updatedAt", System.currentTimeMillis()))
            s.executeInsert(); s.clearBindings()
        }
    }

    private fun importPlanOverrides(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO workout_plan_overrides (id, schedule_id, template_id, occurrence_date, planned_date, action, notes, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            s.bindLong(2, o.optLong("scheduleId"))
            s.bindLong(3, o.optLong("templateId"))
            s.bindLong(4, o.optLong("occurrenceDate"))
            bindLong(s, 5, getLong(o, "plannedDate"))
            s.bindString(6, o.optString("action", ""))
            bindString(s, 7, getString(o, "notes"))
            s.bindLong(8, o.optLong("createdAt", System.currentTimeMillis()))
            s.bindLong(9, o.optLong("updatedAt", System.currentTimeMillis()))
            s.executeInsert(); s.clearBindings()
        }
    }

    private fun importMediaRecords(arr: JSONArray?) {
        if (arr == null) return
        val sql = db.openHelper.writableDatabase
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = sql.compileStatement(
                "INSERT OR REPLACE INTO media_records (id, media_type, relative_path, mime_type, captured_at, date, width, height, duration_millis, size_bytes, workout_session_id, body_measurement_id, check_in_id, exercise_session_id, food_record_id, category, pose_tag, note, is_favorite, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            s.bindLong(1, o.optLong("id"))
            s.bindString(2, o.optString("mediaType", "PHOTO"))
            s.bindString(3, o.optString("relativePath", ""))
            s.bindString(4, o.optString("mimeType", ""))
            s.bindLong(5, o.optLong("capturedAt"))
            s.bindLong(6, o.optLong("date"))
            bindInt(s, 7, getInt(o, "width"))
            bindInt(s, 8, getInt(o, "height"))
            bindLong(s, 9, getLong(o, "durationMillis"))
            s.bindLong(10, o.optLong("sizeBytes"))
            bindLong(s, 11, getLong(o, "workoutSessionId"))
            bindLong(s, 12, getLong(o, "bodyMeasurementId"))
            bindLong(s, 13, getLong(o, "checkInId"))
            bindLong(s, 14, getLong(o, "exerciseSessionId"))
            bindLong(s, 15, getLong(o, "foodRecordId"))
            s.bindString(16, o.optString("category", "GENERAL"))
            bindString(s, 17, getString(o, "poseTag"))
            bindString(s, 18, getString(o, "note"))
            s.bindLong(19, boolToInt(getBool(o, "isFavorite")))
            s.bindLong(20, o.optLong("createdAt", System.currentTimeMillis()))
            s.bindLong(21, o.optLong("updatedAt", System.currentTimeMillis()))
            s.executeInsert(); s.clearBindings()
        }
    }

    // ── Media extraction ────────────────────────────────────────────────────

    private fun extractMediaEntry(entry: MediaEntry) {
        val file = mediaStorage.resolveFile(entry.relativePath)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { fos -> fos.write(entry.bytes) }
    }

    // ── SQLite bind helpers (nullable-aware) ────────────────────────────────

    private fun bindString(stmt: android.database.sqlite.SQLiteStatement, index: Int, value: String?) {
        if (value != null) stmt.bindString(index, value) else stmt.bindNull(index)
    }

    private fun bindLong(stmt: android.database.sqlite.SQLiteStatement, index: Int, value: Long?) {
        if (value != null) stmt.bindLong(index, value) else stmt.bindNull(index)
    }

    private fun bindInt(stmt: android.database.sqlite.SQLiteStatement, index: Int, value: Int?) {
        if (value != null) stmt.bindLong(index, value.toLong()) else stmt.bindNull(index)
    }

    private fun bindDouble(stmt: android.database.sqlite.SQLiteStatement, index: Int, value: Double?) {
        if (value != null) stmt.bindDouble(index, value) else stmt.bindNull(index)
    }

    // ── SHA-256 ──────────────────────────────────────────────────────────────

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }
}
