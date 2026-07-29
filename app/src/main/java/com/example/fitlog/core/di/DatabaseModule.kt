package com.example.fitlog.core.di

import android.content.Context
import androidx.room.Room
import com.example.fitlog.core.database.FitLogDatabase
import com.example.fitlog.core.database.dao.ExerciseCategoryDao
import com.example.fitlog.core.database.dao.ExerciseDao
import com.example.fitlog.core.database.dao.ExerciseSessionDao
import com.example.fitlog.core.database.dao.SetRecordDao
import com.example.fitlog.core.database.dao.WorkoutScheduleDao
import com.example.fitlog.core.database.dao.WorkoutSessionDao
import com.example.fitlog.core.database.dao.WorkoutTemplateDao
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
            .addMigrations(Migrations.MIGRATION_1_2)
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
}
