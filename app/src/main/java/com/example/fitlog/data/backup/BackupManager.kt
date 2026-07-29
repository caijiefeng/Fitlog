package com.example.fitlog.data.backup

import android.content.Context
import android.net.Uri
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
import com.example.fitlog.core.database.entity.BodyMeasurementEntity
import com.example.fitlog.core.database.entity.CheckInEntity
import com.example.fitlog.core.database.entity.ExerciseCategoryEntity
import com.example.fitlog.core.database.entity.ExerciseEntity
import com.example.fitlog.core.database.entity.ExerciseSessionEntity
import com.example.fitlog.core.database.entity.FoodRecordEntity
import com.example.fitlog.core.database.entity.MediaRecordEntity
import com.example.fitlog.core.database.entity.ReminderEntity
import com.example.fitlog.core.database.entity.SetRecordEntity
import com.example.fitlog.core.database.entity.UserProfileEntity
import com.example.fitlog.core.database.entity.WorkoutPlanOverrideEntity
import com.example.fitlog.core.database.entity.WorkoutScheduleEntity
import com.example.fitlog.core.database.entity.WorkoutSessionEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateExerciseEntity
import com.example.fitlog.core.media.AppMediaStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates a full database and media backup as a ZIP archive.
 *
 * ZIP structure:
 * ```
 * backup_v{N}.zip/
 *   manifest.json   — BackupManifest (version, exportedAt, dbChecksum, dbRows, mediaCount)
 *   db.json         — All database records as a structured JSON object
 *   media/          — Copies of media files, preserving relative path structure
 * ```
 *
 * Uses `org.json` (built-in Android) for JSON and `java.security.MessageDigest` for SHA-256.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val checkInDao: CheckInDao,
    private val exerciseCategoryDao: ExerciseCategoryDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseSessionDao: ExerciseSessionDao,
    private val foodRecordDao: FoodRecordDao,
    private val mediaRecordDao: MediaRecordDao,
    private val reminderDao: ReminderDao,
    private val setRecordDao: SetRecordDao,
    private val userProfileDao: UserProfileDao,
    private val workoutPlanOverrideDao: WorkoutPlanOverrideDao,
    private val workoutScheduleDao: WorkoutScheduleDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val mediaStorage: AppMediaStorage,
) {

    data class BackupResult(
        val uri: Uri,
        val manifest: BackupManifest,
    )

    companion object {
        private const val DB_JSON = "db.json"
        private const val MANIFEST_JSON = "manifest.json"
        private const val MEDIA_PREFIX = "media/"
        private const val BUFFER_SIZE = 8192
        private const val APP_VERSION = "0.1.0"
    }

    /**
     * Writes a complete backup ZIP to [uri], typically obtained via
     * [android.content.Intent.ACTION_CREATE_DOCUMENT].
     */
    suspend fun createBackup(uri: Uri): BackupResult {
        val allData = collectAllData()
        val dbJson = buildDbJson(allData)
        val dbJsonBytes = dbJson.toString(2).toByteArray(Charsets.UTF_8)

        val checksum = sha256Hex(dbJsonBytes)
        val totalRows = allData.totalCount()
        val mediaCount = allData.mediaRecords.size

        val manifest = BackupManifest(
            version = BackupManifest.CURRENT_VERSION,
            appVersion = APP_VERSION,
            dbRows = totalRows,
            mediaCount = mediaCount,
            dbChecksum = checksum,
        )

        writeZipToUri(uri, dbJsonBytes, manifest, allData.mediaRecords)
        return BackupResult(uri = uri, manifest = manifest)
    }

    // ── Data collection ──────────────────────────────────────────────────────

    private data class AllData(
        val exerciseCategories: List<ExerciseCategoryEntity>,
        val exercises: List<ExerciseEntity>,
        val workoutTemplates: List<WorkoutTemplateEntity>,
        val workoutTemplateExercises: List<WorkoutTemplateExerciseEntity>,
        val workoutSchedules: List<WorkoutScheduleEntity>,
        val workoutSessions: List<WorkoutSessionEntity>,
        val exerciseSessions: List<ExerciseSessionEntity>,
        val setRecords: List<SetRecordEntity>,
        val workoutPlanOverrides: List<WorkoutPlanOverrideEntity>,
        val reminders: List<ReminderEntity>,
        val checkIns: List<CheckInEntity>,
        val userProfiles: List<UserProfileEntity>,
        val bodyMeasurements: List<BodyMeasurementEntity>,
        val foodRecords: List<FoodRecordEntity>,
        val mediaRecords: List<MediaRecordEntity>,
    ) {
        fun totalCount(): Int =
            exerciseCategories.size + exercises.size + workoutTemplates.size +
            workoutTemplateExercises.size + workoutSchedules.size + workoutSessions.size +
            exerciseSessions.size + setRecords.size + workoutPlanOverrides.size +
            reminders.size + checkIns.size + userProfiles.size +
            bodyMeasurements.size + foodRecords.size + mediaRecords.size
    }

    private suspend fun collectAllData(): AllData = AllData(
        exerciseCategories = exerciseCategoryDao.getAll().first(),
        exercises = exerciseDao.getAll(),
        workoutTemplates = workoutTemplateDao.getAll(),
        workoutTemplateExercises = workoutTemplateDao.getAllTemplateExercises(),
        workoutSchedules = workoutScheduleDao.getAll(),
        workoutSessions = workoutSessionDao.getAll(),
        exerciseSessions = exerciseSessionDao.getAll(),
        setRecords = setRecordDao.getAll(),
        workoutPlanOverrides = workoutPlanOverrideDao.getAll(),
        reminders = reminderDao.getAll(),
        checkIns = checkInDao.getAll(),
        userProfiles = userProfileDao.getAll(),
        bodyMeasurements = bodyMeasurementDao.getAll(),
        foodRecords = foodRecordDao.getAll(),
        mediaRecords = mediaRecordDao.getAllRecords(),
    )

    // ── JSON builder ─────────────────────────────────────────────────────────

    private fun buildDbJson(data: AllData): JSONObject = JSONObject().apply {
        put("exercise_categories", toJsonArray(data.exerciseCategories))
        put("exercises", toJsonArray(data.exercises))
        put("workout_templates", toJsonArray(data.workoutTemplates))
        put("workout_template_exercises", toJsonArray(data.workoutTemplateExercises))
        put("workout_schedules", toJsonArray(data.workoutSchedules))
        put("workout_sessions", toJsonArray(data.workoutSessions))
        put("exercise_sessions", toJsonArray(data.exerciseSessions))
        put("set_records", toJsonArray(data.setRecords))
        put("workout_plan_overrides", toJsonArray(data.workoutPlanOverrides))
        put("reminders", toJsonArray(data.reminders))
        put("check_ins", toJsonArray(data.checkIns))
        put("user_profiles", toJsonArray(data.userProfiles))
        put("body_measurements", toJsonArray(data.bodyMeasurements))
        put("food_records", toJsonArray(data.foodRecords))
        put("media_records", toJsonArray(data.mediaRecords))
    }

    // ── ZIP writer ───────────────────────────────────────────────────────────

    private fun writeZipToUri(
        uri: Uri,
        dbJsonBytes: ByteArray,
        manifest: BackupManifest,
        mediaRecords: List<MediaRecordEntity>,
    ) {
        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: throw java.io.IOException("Cannot open $uri for writing")

        ZipOutputStream(BufferedOutputStream(outputStream, BUFFER_SIZE)).use { zos ->
            // manifest.json
            zos.putNextEntry(ZipEntry(MANIFEST_JSON))
            zos.write(manifest.toJson().toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // db.json
            zos.putNextEntry(ZipEntry(DB_JSON))
            zos.write(dbJsonBytes)
            zos.closeEntry()

            // media files — skip any that can't be resolved or read
            val buffer = ByteArray(BUFFER_SIZE)
            for (record in mediaRecords) {
                val file: File = try {
                    mediaStorage.resolveFile(record.relativePath)
                } catch (_: Exception) {
                    continue
                }
                if (!file.isFile) continue

                zos.putNextEntry(ZipEntry("$MEDIA_PREFIX${record.relativePath}"))
                BufferedInputStream(FileInputStream(file), BUFFER_SIZE).use { fis ->
                    var read: Int
                    while (fis.read(buffer).also { read = it } != -1) {
                        zos.write(buffer, 0, read)
                    }
                }
                zos.closeEntry()
            }
        }
    }

    // ── SHA-256 ──────────────────────────────────────────────────────────────

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }
}

// ── Manifest JSON serialization ─────────────────────────────────────────────

internal fun BackupManifest.toJson(): JSONObject = JSONObject().apply {
    put("version", version)
    put("appVersion", appVersion)
    put("exportedAt", exportedAt)
    put("dbRows", dbRows)
    put("mediaCount", mediaCount)
    put("dbChecksum", dbChecksum)
}

// ── Generic JSON serialization ───────────────────────────────────────────────

/**
 * Converts a list of Room entities to a [JSONArray].  Each entity is
 * converted by reflecting on its Kotlin properties.
 */
internal fun <T> toJsonArray(list: List<T>): JSONArray =
    JSONArray().apply {
        for (item in list) {
            put(toJson(item))
        }
    }

/**
 * Converts any value to a JSON-compatible type:
 * - null -> JSONObject.NULL
 * - Number, Boolean, String -> as-is
 * - Enum -> its name
 * - List -> JSONArray via [toJsonArray]
 * - else -> JSONObject built from the object's Kotlin properties
 */
internal fun toJson(value: Any?): Any? = when (value) {
    null -> JSONObject.NULL
    is Number, is Boolean, is String -> value
    is Enum<*> -> value.name
    is List<*> -> toJsonArray(value)
    else -> {
        val obj = JSONObject()
        for (prop in value::class.members) {
            if (prop is kotlin.reflect.KProperty<*>) {
                try {
                    obj.put(prop.name, toJson(prop.call(value)))
                } catch (_: Exception) { /* skip inaccessible props */ }
            }
        }
        obj
    }
}
