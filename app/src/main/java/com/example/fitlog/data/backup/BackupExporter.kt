package com.example.fitlog.data.backup

import com.example.fitlog.core.database.entity.BodyMeasurementEntity
import com.example.fitlog.core.database.entity.CheckInEntity
import com.example.fitlog.core.database.entity.ExerciseCategoryEntity
import com.example.fitlog.core.database.entity.ExerciseEntity
import com.example.fitlog.core.database.entity.ExerciseSessionEntity
import com.example.fitlog.core.database.entity.FoodRecordEntity
import com.example.fitlog.core.database.entity.MediaRecordEntity
import com.example.fitlog.core.database.entity.PlannedWorkoutEntity
import com.example.fitlog.core.database.entity.ReminderEntity
import com.example.fitlog.core.database.entity.SetRecordEntity
import com.example.fitlog.core.database.entity.UserProfileEntity
import com.example.fitlog.core.database.entity.WorkoutPlanOverrideEntity
import com.example.fitlog.core.database.entity.WorkoutScheduleEntity
import com.example.fitlog.core.database.entity.WorkoutSessionEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateExerciseEntity
import com.example.fitlog.core.media.AppMediaStorage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Result of a successful backup export.
 *
 * @property backupFile The temporary file containing the ZIP archive.
 * @property manifest   Metadata about the backup contents.
 */
data class ExportResult(
    val backupFile: File,
    val manifest: BackupManifest,
)

/**
 * Creates a backup ZIP archive in a temporary file.
 *
 * The ZIP contains:
 * - `manifest.json` — version, app version, db version, exportedAt, row counts,
 *   media count, SHA-256 checksum of db.json
 * - `db.json` — all Room tables serialised as JSON arrays via `org.json`
 * - `media/<relative-path>` — every media file stored under its original
 *   relative path from [AppMediaStorage]
 *
 * This is a pure data-exporter; it does not depend on Room DAOs — the caller
 * must provide all entity lists.  This keeps it testable and lets the caller
 * control the transaction boundary.
 */
object BackupExporter {

    /**
     * Builds a backup ZIP at a temporary file.
     *
     * @param appVersion  Human-readable app version string.
     * @param dbVersion   Room database schema version.
     * @param allEntities All entity lists keyed by table name (see [EntityLists]).
     * @param mediaRecords Media records whose `relativePath` entries will be
     *                     packed into the `media/` prefix.
     * @param appMediaStorage Used to resolve relative paths to absolute files.
     * @param outputDir   Directory where the temp ZIP file is created.
     *                    Defaults to the system temp directory.
     * @return [ExportResult] containing the ZIP [File] and parsed [BackupManifest].
     */
    fun export(
        appVersion: String,
        dbVersion: Int,
        allEntities: EntityLists,
        mediaRecords: List<MediaRecordEntity>,
        appMediaStorage: AppMediaStorage,
        outputDir: File = File(System.getProperty("java.io.tmpdir")),
    ): ExportResult {
        // ── Build db.json ────────────────────────────────────────────────────
        val dbJson = JSONObject()

        fun <T> List<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray {
            val arr = JSONArray()
            forEach { arr.put(transform(it)) }
            return arr
        }

        dbJson.put("exercise_categories", allEntities.exerciseCategories.toJsonArray { it.toJson() })
        dbJson.put("exercises", allEntities.exercises.toJsonArray { it.toJson() })
        dbJson.put("workout_templates", allEntities.workoutTemplates.toJsonArray { it.toJson() })
        dbJson.put("workout_template_exercises", allEntities.workoutTemplateExercises.toJsonArray { it.toJson() })
        dbJson.put("workout_schedules", allEntities.workoutSchedules.toJsonArray { it.toJson() })
        dbJson.put("workout_sessions", allEntities.workoutSessions.toJsonArray { it.toJson() })
        dbJson.put("exercise_sessions", allEntities.exerciseSessions.toJsonArray { it.toJson() })
        dbJson.put("set_records", allEntities.setRecords.toJsonArray { it.toJson() })
        dbJson.put("workout_plan_overrides", allEntities.workoutPlanOverrides.toJsonArray { it.toJson() })
        dbJson.put("reminders", allEntities.reminders.toJsonArray { it.toJson() })
        dbJson.put("check_ins", allEntities.checkIns.toJsonArray { it.toJson() })
        dbJson.put("user_profiles", allEntities.userProfiles.toJsonArray { it.toJson() })
        dbJson.put("body_measurements", allEntities.bodyMeasurements.toJsonArray { it.toJson() })
        dbJson.put("food_records", allEntities.foodRecords.toJsonArray { it.toJson() })
        dbJson.put("planned_workouts", allEntities.plannedWorkouts.toJsonArray { it.toJson() })
        dbJson.put("media_records", mediaRecords.toJsonArray { it.toJson() })

        val dbJsonBytes = dbJson.toString(2).toByteArray(Charsets.UTF_8)

        // SHA-256 checksum of raw db.json bytes
        val digest = MessageDigest.getInstance("SHA-256")
        val dbChecksum = digest.digest(dbJsonBytes).joinToString("") { "%02x".format(it) }

        // ── Build row counts map ─────────────────────────────────────────────
        val rowCounts = linkedMapOf(
            "exercise_categories" to allEntities.exerciseCategories.size,
            "exercises" to allEntities.exercises.size,
            "workout_templates" to allEntities.workoutTemplates.size,
            "workout_template_exercises" to allEntities.workoutTemplateExercises.size,
            "workout_schedules" to allEntities.workoutSchedules.size,
            "workout_sessions" to allEntities.workoutSessions.size,
            "exercise_sessions" to allEntities.exerciseSessions.size,
            "set_records" to allEntities.setRecords.size,
            "workout_plan_overrides" to allEntities.workoutPlanOverrides.size,
            "reminders" to allEntities.reminders.size,
            "check_ins" to allEntities.checkIns.size,
            "user_profiles" to allEntities.userProfiles.size,
            "body_measurements" to allEntities.bodyMeasurements.size,
            "food_records" to allEntities.foodRecords.size,
            "planned_workouts" to allEntities.plannedWorkouts.size,
            "media_records" to mediaRecords.size,
        )
        val totalRows = rowCounts.values.sum()

        // ── Build manifest ───────────────────────────────────────────────────
        val manifest = BackupManifest(
            version = BackupManifest.CURRENT_VERSION,
            appVersion = appVersion,
            dbVersion = dbVersion,
            totalRows = totalRows,
            rowCounts = rowCounts,
            mediaCount = mediaRecords.size,
            dbChecksum = dbChecksum,
        )

        val manifestJson = JSONObject().apply {
            put("backupVersion", manifest.version)
            put("appVersion", manifest.appVersion)
            put("dbVersion", manifest.dbVersion)
            put("exportedAt", manifest.exportedAt)
            put("totalRows", manifest.totalRows)
            put("rowCounts", JSONObject(manifest.rowCounts))
            put("mediaCount", manifest.mediaCount)
            put("dbChecksum", manifest.dbChecksum)
        }
        val manifestBytes = manifestJson.toString(2).toByteArray(Charsets.UTF_8)

        // ── Write ZIP ───────────────────────────────────────────────────────
        val backupFile = File(outputDir, "fitlog_backup_${System.currentTimeMillis()}.zip")

        FileOutputStream(backupFile).use { fos ->
            ZipOutputStream(fos).use { zip ->
                // manifest.json
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifestBytes)
                zip.closeEntry()

                // db.json
                zip.putNextEntry(ZipEntry("db.json"))
                zip.write(dbJsonBytes)
                zip.closeEntry()

                // media/ directory entries
                for (media in mediaRecords) {
                    val file = try {
                        appMediaStorage.resolveFile(media.relativePath)
                    } catch (_: Exception) {
                        null
                    }
                    if (file != null && file.isFile) {
                        val entryName = "media/${media.relativePath}"
                        zip.putNextEntry(ZipEntry(entryName))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }

        return ExportResult(backupFile = backupFile, manifest = manifest)
    }
}

// ── Data class grouping all entity lists ────────────────────────────────────

/**
 * Convenience container for all entity lists that are serialised into `db.json`.
 *
 * Create an instance by passing every table's data.  If a table is empty,
 * pass an empty list — not null.
 */
data class EntityLists(
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
    val plannedWorkouts: List<PlannedWorkoutEntity> = emptyList(),
)

// ── Entity → JSONObject extensions ──────────────────────────────────────────

internal fun ExerciseCategoryEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("description", description ?: JSONObject.NULL)
    put("sort_order", sortOrder)
    put("created_at", createdAt)
}

internal fun ExerciseEntity.toJson() = JSONObject().apply {
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

internal fun WorkoutTemplateEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("notes", notes ?: JSONObject.NULL)
    put("sort_order", sortOrder)
    put("is_active", isActive)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

internal fun WorkoutTemplateExerciseEntity.toJson() = JSONObject().apply {
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

internal fun WorkoutScheduleEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("template_id", templateId)
    put("day_of_week", dayOfWeek)
    put("start_date", startDate ?: JSONObject.NULL)
    put("end_date", endDate ?: JSONObject.NULL)
    put("repeat_interval_weeks", repeatIntervalWeeks)
    put("is_active", isActive)
    put("created_at", createdAt)
}

internal fun PlannedWorkoutEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("template_id", templateId)
    put("planned_date", plannedDate)
    put("note", note ?: JSONObject.NULL)
    put("created_at", createdAt)
}

internal fun WorkoutSessionEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("schedule_id", scheduleId ?: JSONObject.NULL)
    put("template_id", templateId ?: JSONObject.NULL)
    put("template_name_snapshot", templateNameSnapshot ?: JSONObject.NULL)
    put("date", date)
    put("start_time", startTime)
    put("end_time", endTime ?: JSONObject.NULL)
    put("status", status)
    put("notes", notes ?: JSONObject.NULL)
    put("active_rest_started_at", activeRestStartedAt ?: JSONObject.NULL)
    put("active_rest_duration_seconds", activeRestDurationSeconds ?: JSONObject.NULL)
    put("active_rest_set_record_id", activeRestSetRecordId ?: JSONObject.NULL)
    put("created_at", createdAt)
    put("occurrence_date", occurrenceDate ?: JSONObject.NULL)
    put("updated_at", updatedAt)
}

internal fun ExerciseSessionEntity.toJson() = JSONObject().apply {
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

internal fun SetRecordEntity.toJson() = JSONObject().apply {
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

internal fun WorkoutPlanOverrideEntity.toJson() = JSONObject().apply {
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

internal fun ReminderEntity.toJson() = JSONObject().apply {
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

internal fun CheckInEntity.toJson() = JSONObject().apply {
    put("id", id)
    put("date", date)
    put("session_id", sessionId ?: JSONObject.NULL)
    put("mood", mood ?: JSONObject.NULL)
    put("energy_level", energyLevel ?: JSONObject.NULL)
    put("notes", notes ?: JSONObject.NULL)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

internal fun UserProfileEntity.toJson() = JSONObject().apply {
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

internal fun BodyMeasurementEntity.toJson() = JSONObject().apply {
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

internal fun FoodRecordEntity.toJson() = JSONObject().apply {
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

internal fun MediaRecordEntity.toJson() = JSONObject().apply {
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
