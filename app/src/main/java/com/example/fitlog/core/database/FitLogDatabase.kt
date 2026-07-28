package com.example.fitlog.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.fitlog.core.database.converter.Converters
import com.example.fitlog.core.database.dao.ExerciseCategoryDao
import com.example.fitlog.core.database.dao.ExerciseDao
import com.example.fitlog.core.database.dao.WorkoutScheduleDao
import com.example.fitlog.core.database.dao.WorkoutTemplateDao
import com.example.fitlog.core.database.entity.ExerciseCategoryEntity
import com.example.fitlog.core.database.entity.ExerciseEntity
import com.example.fitlog.core.database.entity.WorkoutScheduleEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateExerciseEntity

@Database(
    entities = [
        ExerciseCategoryEntity::class,
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutTemplateExerciseEntity::class,
        WorkoutScheduleEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FitLogDatabase : RoomDatabase() {

    abstract fun exerciseCategoryDao(): ExerciseCategoryDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun workoutScheduleDao(): WorkoutScheduleDao
}
