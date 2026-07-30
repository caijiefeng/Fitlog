package com.example.fitlog.core.di

import android.content.Context
import androidx.room.Room
import com.example.fitlog.core.database.FitLogDatabase
import com.example.fitlog.core.database.dao.BodyMeasurementDao
import com.example.fitlog.core.database.dao.CheckInDao
import com.example.fitlog.core.database.dao.ExerciseCategoryDao
import com.example.fitlog.core.database.dao.FoodRecordDao
import com.example.fitlog.core.database.dao.ExerciseDao
import com.example.fitlog.core.database.dao.ExerciseSessionDao
import com.example.fitlog.core.database.dao.ReminderDao
import com.example.fitlog.core.database.dao.SetRecordDao
import com.example.fitlog.core.database.dao.UserProfileDao
import com.example.fitlog.core.database.dao.WorkoutPlanOverrideDao
import com.example.fitlog.core.database.dao.WorkoutScheduleDao
import com.example.fitlog.core.database.dao.WorkoutSessionDao
import com.example.fitlog.core.database.dao.WorkoutTemplateDao
import com.example.fitlog.core.database.dao.MediaRecordDao
import com.example.fitlog.core.database.migration.Migrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFitLogDatabase(
        @ApplicationContext context: Context,
    ): FitLogDatabase {
        return Room.databaseBuilder(
            context,
            FitLogDatabase::class.java,
            "fitlog.db",
        )
            .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3, Migrations.MIGRATION_3_4, Migrations.MIGRATION_4_5, Migrations.MIGRATION_5_6, Migrations.MIGRATION_6_7, Migrations.MIGRATION_7_8, Migrations.MIGRATION_8_9)
            .addCallback(DatabaseCallback())
            .build()
    }

    @Provides
    fun provideExerciseDao(db: FitLogDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideExerciseCategoryDao(db: FitLogDatabase): ExerciseCategoryDao = db.exerciseCategoryDao()

    @Provides
    fun provideWorkoutTemplateDao(db: FitLogDatabase): WorkoutTemplateDao = db.workoutTemplateDao()

    @Provides
    fun provideWorkoutScheduleDao(db: FitLogDatabase): WorkoutScheduleDao = db.workoutScheduleDao()

    @Provides
    fun provideWorkoutSessionDao(db: FitLogDatabase): WorkoutSessionDao = db.workoutSessionDao()

    @Provides
    fun provideExerciseSessionDao(db: FitLogDatabase): ExerciseSessionDao = db.exerciseSessionDao()

    @Provides
    fun provideSetRecordDao(db: FitLogDatabase): SetRecordDao = db.setRecordDao()

    @Provides
    fun provideWorkoutPlanOverrideDao(db: FitLogDatabase): WorkoutPlanOverrideDao = db.workoutPlanOverrideDao()

    @Provides
    fun provideReminderDao(db: FitLogDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideCheckInDao(db: FitLogDatabase): CheckInDao = db.checkInDao()

    @Provides
    fun provideUserProfileDao(db: FitLogDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideBodyMeasurementDao(db: FitLogDatabase): BodyMeasurementDao = db.bodyMeasurementDao()

    @Provides
    fun provideFoodRecordDao(db: FitLogDatabase): FoodRecordDao = db.foodRecordDao()

    @Provides
    fun provideMediaRecordDao(db: FitLogDatabase): MediaRecordDao = db.mediaRecordDao()
}
