package com.example.fitlog.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.fitlog.core.database.converter.Converters
import com.example.fitlog.core.database.dao.CheckInDao
import com.example.fitlog.core.database.dao.BodyMeasurementDao
import com.example.fitlog.core.database.dao.FoodRecordDao
import com.example.fitlog.core.database.dao.ExerciseCategoryDao
import com.example.fitlog.core.database.dao.ExerciseDao
import com.example.fitlog.core.database.dao.MediaRecordDao
import com.example.fitlog.core.database.dao.ExerciseSessionDao
import com.example.fitlog.core.database.dao.PlannedWorkoutDao
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
import com.example.fitlog.core.database.entity.PlannedWorkoutEntity
import com.example.fitlog.core.database.entity.ReminderEntity
import com.example.fitlog.core.database.entity.SetRecordEntity
import com.example.fitlog.core.database.entity.UserProfileEntity
import com.example.fitlog.core.database.entity.WorkoutPlanOverrideEntity
import com.example.fitlog.core.database.entity.WorkoutScheduleEntity
import com.example.fitlog.core.database.entity.WorkoutSessionEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateExerciseEntity
import com.example.fitlog.core.database.entity.FoodRecordEntity
import com.example.fitlog.core.database.entity.MediaRecordEntity
import com.example.fitlog.core.database.migration.Migrations

@Database(
    entities = [
        ExerciseCategoryEntity::class,
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutTemplateExerciseEntity::class,
        WorkoutScheduleEntity::class,
        WorkoutSessionEntity::class,
        ExerciseSessionEntity::class,
        SetRecordEntity::class,
        WorkoutPlanOverrideEntity::class,
        PlannedWorkoutEntity::class,
        ReminderEntity::class,
        CheckInEntity::class,
        UserProfileEntity::class,
        BodyMeasurementEntity::class,
        FoodRecordEntity::class,
        MediaRecordEntity::class,
    ],
    version = 13,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FitLogDatabase : RoomDatabase() {

    abstract fun exerciseCategoryDao(): ExerciseCategoryDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun workoutScheduleDao(): WorkoutScheduleDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun exerciseSessionDao(): ExerciseSessionDao
    abstract fun setRecordDao(): SetRecordDao
    abstract fun workoutPlanOverrideDao(): WorkoutPlanOverrideDao
    abstract fun plannedWorkoutDao(): PlannedWorkoutDao
    abstract fun reminderDao(): ReminderDao
    abstract fun checkInDao(): CheckInDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun foodRecordDao(): FoodRecordDao
    abstract fun mediaRecordDao(): MediaRecordDao
}
