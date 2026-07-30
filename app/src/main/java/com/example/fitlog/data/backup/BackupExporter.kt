package com.example.fitlog.data.backup

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
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Progress update emitted during backup creation.
 *
 * @property phase  Current phase: "reading_db", "writing_json", "packing_media", "finalizing".
 * @property progress  0.0–1.0 fraction complete within the current phase.
 * @property message  Human-readable description.
 */
data class BackupProgress(
    val phase: String,
    val progress: Float,
    val message: String,
)

/**
 * Exports all FitLog data and media files into a single ZIP archive.
 *
 * The ZIP contains:
 * - `manifest.json` — version, app version, exportedAt, SHA-256 checksum of db.json, row counts
 * - `db.json` — all Room tables serialised as structured JSON via org.json
 * - `media/` — every media file stored under its original relative path
 */
@Singleton
class BackupExporter @Inject constructor(
    private val workoutSessionDao: WorkoutSessionDao,
    private val exerciseSessionDao: ExerciseSessionDao,
    private val setRecordDao: SetRecordDao,
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val checkInDao: CheckInDao,
    private val foodRecordDao: FoodRecordDao,
    private val userProfileDao: UserProfileDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseCategoryDao: ExerciseCategoryDao,
    private val workoutScheduleDao: WorkoutScheduleDao,
    private val reminderDao: ReminderDao,
    private val workoutPlanOverrideDao: WorkoutPlanOverrideDao,
    private val mediaRecordDao: MediaRecordDao,
    private val appMediaStorage: AppMediaStorage,
) {

    /**
     * Writes a complete backup ZIP to [output].
     *
     * @param output  The stream to write the ZIP into.
     * @param appVersion  A version string for the app (e.g. BuildConfig.VERSION_NAME).
     * @param onProgress  Optional callback invoked with progress updates.
     */
    suspend fun export(
        output: OutputStream,
        appVersion: String,
        onProgress: (BackupProgress) -> Unit = {},
    ) {
        onProgress(BackupProgress("reading_db", 0f, "Reading database…"))

        // ── Read all tables ─────────────────────────────────────────────────
        onProgress(BackupProgress("reading_db", 0.05f, "Reading exercise categories…"))
        val exerciseCategories = exerciseCategoryDao.getAll().first()

        onProgress(BackupProgress("reading_db", 0.1f, "Reading exercises…"))
        val exercises = exerciseDao.getAll()

        onProgress(BackupProgress("reading_db", 0.15f, "Reading templates…"))
        val templates = workoutTemplateDao.getAll()
        val templateExercises = workoutTemplateDao.getAllTemplateExercises()

        onProgress(BackupProgress("reading_db", 0.2f, "Reading schedules…"))
        val schedules = workoutScheduleDao.getAll()

        onProgress(BackupProgress("reading_db", 0.25f, "Reading workout sessions…"))
        val sessions = workoutSessionDao.getAll()

        onProgress(BackupProgress("reading_db", 0.3f, "Reading exercise sessions…"))
        val exerciseSessions = exerciseSessionDao.getAll()

        onProgress(BackupProgress("reading_db", 0.35f, "Reading set records…"))
        val setRecords = setRecordDao.getAll()

        onProgress(BackupProgress("reading_db", 0.4f, "Reading overrides…"))
        val overrides = workoutPlanOverrideDao.getAll()

        onProgress(BackupProgress("reading_db", 0.45f, "Reading reminders…"))
        val reminders = reminderDao.getAll()

        onProgress(BackupProgress("reading_db", 0.5f, "Reading check-ins…"))
        val checkIns = checkInDao.getAll()

        onProgress(BackupProgress("reading_db", 0.55f, "Reading profiles…"))
        val profiles = userProfileDao.getAll()

        onProgress(BackupProgress("reading_db", 0.6f, "Reading body measurements…"))
        val bodyMeasurements = bodyMeasurementDao.getAll()

        onProgress(BackupProgress("reading_db", 0.65f, "Reading food records…"))
        val foodRecords = foodRecordDao.getAll()

        onProgress(BackupProgress("reading_db", 0.7f, "Reading media records…"))
        val mediaRecords = mediaRecordDao.getAll()

        // ── Build db.json ────────────────────────────────────────────────────
        onProgress(BackupProgress("writing_json", 0f, "Writing database JSON…"))

        val dbJson = JSONObject()
        dbJson.put("exercise_categories", exerciseCategories.toJsonArray { it.toJson() })
        dbJson.put("exercises", exercises.toJsonArray { it.toJson() })
        dbJson.put("workout_templates", templates.toJsonArray { it.toJson() })
        dbJson.put("workout_template_exercises", templateExercises.toJsonArray { it.toJson() })
        dbJson.put("workout_schedules", schedules.toJsonArray { it.toJson() })
        dbJson.put("workout_sessions", sessions.toJsonArray { it.toJson() })
        dbJson.put("exercise_sessions", exerciseSessions.toJsonArray { it.toJson() })
        dbJson.put("set_records", setRecords.toJsonArray { it.toJson() })
        dbJson.put("workout_plan_overrides", overrides.toJsonArray { it.toJson() })
        dbJson.put("reminders", reminders.toJsonArray { it.toJson() })
        dbJson.put("check_ins", checkIns.toJsonArray { it.toJson() })
        dbJson.put("user_profiles", profiles.toJsonArray { it.toJson() })
        dbJson.put("body_measurements", bodyMeasurements.toJsonArray { it.toJson() })
        dbJson.put("food_records", foodRecords.toJsonArray { it.toJson() })
        dbJson.put("media_records", mediaRecords.toJsonArray { it.toJson() })

        val dbJsonBytes = dbJson.toString(2).toByteArray(Charsets.UTF_8)

        // SHA-256 checksum of db.json content
        val digest = MessageDigest.getInstance("SHA-256")
        val dbChecksum = digest.digest(dbJsonBytes).joinToString("") { "%02x".format(it) }

        // ── Write manifest.json ──────────────────────────────────────────────
        onProgress(BackupProgress("writing_json", 0.5f, "Writing manifest…"))

        val manifest = BackupManifest(
            version = BackupManifest.CURRENT_VERSION,
            appVersion = appVersion,
            dbRows = listOf(
                exerciseCategories.size,
                exercises.size,
                templates.size,
                templateExercises.size,
                schedules.size,
                sessions.size,
                exerciseSessions.size,
                setRecords.size,
                overrides.size,
                reminders.size,
                checkIns.size,
                profiles.size,
                bodyMeasurements.size,
                foodRecords.size,
                mediaRecords.size,
            ).sum(),
            mediaCount = mediaRecords.size,
            dbChecksum = dbChecksum,
        )

        val manifestJson = buildManifestJson(manifest, BuildRowCounts(
            exerciseCategories = exerciseCategories.size,
            exercises = exercises.size,
            workoutTemplates = templates.size,
            workoutTemplateExercises = templateExercises.size,
            workoutSchedules = schedules.size,
            workoutSessions = sessions.size,
            exerciseSessions = exerciseSessions.size,
            setRecords = setRecords.size,
            workoutPlanOverrides = overrides.size,
            reminders = reminders.size,
            checkIns = checkIns.size,
            userProfiles = profiles.size,
            bodyMeasurements = bodyMeasurements.size,
            foodRecords = foodRecords.size,
            mediaRecords = mediaRecords.size,
        ))
        val manifestBytes = manifestJson.toByteArray(Charsets.UTF_8)

        val zip = ZipOutputStream(output)
        val crc = CRC32()

        val manifestEntry = ZipEntry("manifest.json").apply {
            size = manifestBytes.size.toLong()
            crc.reset(); crc.update(manifestBytes); this.crc = crc.value
        }
        zip.putNextEntry(manifestEntry)
        zip.write(manifestBytes)
        zip.closeEntry()

        // ── Write db.json ────────────────────────────────────────────────────
        val dbEntry = ZipEntry("db.json").apply {
            size = dbJsonBytes.size.toLong()
            crc.reset(); crc.update(dbJsonBytes); this.crc = crc.value
        }
        zip.putNextEntry(dbEntry)
        zip.write(dbJsonBytes)
        zip.closeEntry()

        // ── Pack media files ─────────────────────────────────────────────────
        val totalMedia = mediaRecords.size
        if (totalMedia > 0) {
            onProgress(BackupProgress("packing_media", 0f, "Packing 0 of $totalMedia media files…"))
        }

        mediaRecords.forEachIndexed { index, media ->
            val file = try {
                appMediaStorage.resolveFile(media.relativePath)
            } catch (_: Exception) {
                null
            }
            if (file != null && file.isFile) {
                val entryName = "media/${media.relativePath}"
                val mediaEntry = ZipEntry(entryName).apply {
                    size = file.length()
                    crc.reset()
                }
                zip.putNextEntry(mediaEntry)
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            if (totalMedia > 0) {
                val progress = (index + 1).toFloat() / totalMedia
                onProgress(
                    BackupProgress(
                        "packing_media",
                        progress,
                        "Packing ${index + 1} of $totalMedia media files…",
                    ),
                )
            }
        }

        // ── Finalize ─────────────────────────────────────────────────────────
        onProgress(BackupProgress("finalizing", 1f, "Finalizing backup archive…"))
        zip.finish()
        zip.close()
        onProgress(BackupProgress("finalizing", 1f, "Backup complete."))
    }

    // ── Manifest JSON builder ───────────────────────────────────────────────

    private fun buildManifestJson(
        manifest: BackupManifest,
        counts: BuildRowCounts,
    ): String = JSONObject().apply {
        put("version", manifest.version)
        put("app_version", manifest.appVersion)
        put("exported_at", manifest.exportedAt)
        put("db_rows", manifest.dbRows)
        put("media_count", manifest.mediaCount)
        put("db_checksum", manifest.dbChecksum)
        put("row_counts", JSONObject().apply {
            put("exercise_categories", counts.exerciseCategories)
            put("exercises", counts.exercises)
            put("workout_templates", counts.workoutTemplates)
            put("workout_template_exercises", counts.workoutTemplateExercises)
            put("workout_schedules", counts.workoutSchedules)
            put("workout_sessions", counts.workoutSessions)
            put("exercise_sessions", counts.exerciseSessions)
            put("set_records", counts.setRecords)
            put("workout_plan_overrides", counts.workoutPlanOverrides)
            put("reminders", counts.reminders)
            put("check_ins", counts.checkIns)
            put("user_profiles", counts.userProfiles)
            put("body_measurements", counts.bodyMeasurements)
            put("food_records", counts.foodRecords)
            put("media_records", counts.mediaRecords)
        })
    }.toString(2)

    // ── Data class for manifest row counts ─────────────────────────────────

    private data class BuildRowCounts(
        val exerciseCategories: Int,
        val exercises: Int,
        val workoutTemplates: Int,
        val workoutTemplateExercises: Int,
        val workoutSchedules: Int,
        val workoutSessions: Int,
        val exerciseSessions: Int,
        val setRecords: Int,
        val workoutPlanOverrides: Int,
        val reminders: Int,
        val checkIns: Int,
        val userProfiles: Int,
        val bodyMeasurements: Int,
        val foodRecords: Int,
        val mediaRecords: Int,
    )
}

// ── Extension: List → JSONArray ─────────────────────────────────────────────

private fun <T> List<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray {
    val arr = JSONArray()
    forEach { arr.put(transform(it)) }
    return arr
}

// ── Entity → JSONObject extensions ──────────────────────────────────────────

private fun ExerciseCategoryEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("description", description ?: JSONObject.NULL)
    put("sort_order", sortOrder)
    put("created_at", createdAt)
}

private fun ExerciseEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("primary_muscle_group", primaryMuscleGroup)
    put("secondary_muscle_group", secondaryMuscleGroup ?: JSONObject.NULL)
    put("category_id", categoryId ?: JSONObject.NULL)
    put("notes", notes ?: JSONObject.NULL)
    put("is_custom", isCustom)
    put("is_active", isActive)
    put("sort_order", sortOrder)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

private fun WorkoutTemplateEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("notes", notes ?: JSONObject.NULL)
    put("sort_order", sortOrder)
    put("is_active", isActive)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

private fun WorkoutTemplateExerciseEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("template_id", templateId)
    put("exercise_id", exerciseId)
    put("target_sets", targetSets)
    put("target_reps_min", targetRepsMin ?: JSONObject.NULL)
    put("target_reps_max", targetRepsMax ?: JSONObject.NULL)
    put("target_weight_kg", targetWeightKg ?: JSONObject.NULL)
    put("target_rpe", targetRpe ?: JSONObject.NULL)
    put("target_rir", targetRir ?: JSONObject.NULL)
    put("rest_seconds", restSeconds)
    put("notes", notes ?: JSONObject.NULL)
    put("sort_order", sortOrder)
}

private fun WorkoutScheduleEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("template_id", templateId)
    put("day_of_week", dayOfWeek)
    put("is_active", isActive)
    put("created_at", createdAt)
}

private fun WorkoutSessionEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("schedule_id", scheduleId ?: JSONObject.NULL)
    put("template_id", templateId ?: JSONObject.NULL)
    put("template_name_snapshot", templateNameSnapshot ?: JSONObject.NULL)
    put("date", date)
    put("start_time", startTime)
    put("end_time", endTime ?: JSONObject.NULL)
    put("status", status)
    put("notes", notes ?: JSONObject.NULL)
    put("created_at", createdAt)
    put("occurrence_date", occurrenceDate ?: JSONObject.NULL)
    put("updated_at", updatedAt)
}

private fun ExerciseSessionEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("session_id", sessionId)
    put("exercise_id", exerciseId ?: JSONObject.NULL)
    put("exercise_name_snapshot", exerciseNameSnapshot)
    put("primary_muscle_group_snapshot", primaryMuscleGroupSnapshot)
    put("target_sets", targetSets)
    put("target_reps_min", targetRepsMin ?: JSONObject.NULL)
    put("target_reps_max", targetRepsMax ?: JSONObject.NULL)
    put("target_weight_kg", targetWeightKg ?: JSONObject.NULL)
    put("target_rpe", targetRpe ?: JSONObject.NULL)
    put("target_rir", targetRir ?: JSONObject.NULL)
    put("planned_rest_seconds", plannedRestSeconds)
    put("notes", notes ?: JSONObject.NULL)
    put("sort_order", sortOrder)
    put("is_skipped", isSkipped)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

private fun SetRecordEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("exercise_session_id", exerciseSessionId)
    put("set_number", setNumber)
    put("set_type", setType)
    put("reps", reps ?: JSONObject.NULL)
    put("weight_kg", weightKg ?: JSONObject.NULL)
    put("rpe", rpe ?: JSONObject.NULL)
    put("rir", rir ?: JSONObject.NULL)
    put("rest_seconds", restSeconds ?: JSONObject.NULL)
    put("completed", completed)
    put("notes", notes ?: JSONObject.NULL)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

private fun WorkoutPlanOverrideEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("schedule_id", scheduleId)
    put("template_id", templateId)
    put("occurrence_date", occurrenceDate)
    put("planned_date", plannedDate ?: JSONObject.NULL)
    put("action", action)
    put("notes", notes ?: JSONObject.NULL)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

private fun ReminderEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("schedule_id", scheduleId ?: JSONObject.NULL)
    put("label", label)
    put("time_of_day_minutes", timeOfDayMinutes)
    put("days_of_week_mask", daysOfWeekMask)
    put("zone_id", zoneId)
    put("is_enabled", isEnabled)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

private fun CheckInEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("date", date)
    put("session_id", sessionId ?: JSONObject.NULL)
    put("mood", mood ?: JSONObject.NULL)
    put("energy_level", energyLevel ?: JSONObject.NULL)
    put("notes", notes ?: JSONObject.NULL)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

private fun UserProfileEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("gender", gender)
    put("birthday", birthday)
    put("height_cm", heightCm ?: JSONObject.NULL)
    put("activity_level", activityLevel)
    put("goal_type", goalType)
    put("target_body_fat", targetBodyFat ?: JSONObject.NULL)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

private fun BodyMeasurementEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("date", date)
    put("weight_kg", weightKg ?: JSONObject.NULL)
    put("body_fat_percent", bodyFatPercent ?: JSONObject.NULL)
    put("muscle_kg", muscleKg ?: JSONObject.NULL)
    put("waist_cm", waistCm ?: JSONObject.NULL)
    put("note", note ?: JSONObject.NULL)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

private fun FoodRecordEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("date", date)
    put("meal_type", mealType)
    put("food_name", foodName)
    put("calories", calories ?: JSONObject.NULL)
    put("protein_grams", proteinGrams ?: JSONObject.NULL)
    put("carbs_grams", carbsGrams ?: JSONObject.NULL)
    put("fat_grams", fatGrams ?: JSONObject.NULL)
    put("amount", amount ?: JSONObject.NULL)
    put("note", note ?: JSONObject.NULL)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

private fun MediaRecordEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("media_type", mediaType.name)
    put("relative_path", relativePath)
    put("mime_type", mimeType)
    put("captured_at", capturedAt)
    put("date", date)
    put("width", width ?: JSONObject.NULL)
    put("height", height ?: JSONObject.NULL)
    put("duration_millis", durationMillis ?: JSONObject.NULL)
    put("size_bytes", sizeBytes)
    put("workout_session_id", workoutSessionId ?: JSONObject.NULL)
    put("body_measurement_id", bodyMeasurementId ?: JSONObject.NULL)
    put("check_in_id", checkInId ?: JSONObject.NULL)
    put("exercise_session_id", exerciseSessionId ?: JSONObject.NULL)
    put("food_record_id", foodRecordId ?: JSONObject.NULL)
    put("category", category.name)
    put("pose_tag", poseTag?.name ?: JSONObject.NULL)
    put("note", note ?: JSONObject.NULL)
    put("is_favorite", isFavorite)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}
