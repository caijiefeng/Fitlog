package com.example.fitlog.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["template_id", "exercise_id"]),
        Index(value = ["template_id", "sort_order"]),
        Index(value = ["exercise_id"]),
    ],
)
data class WorkoutTemplateExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "template_id")
    val templateId: Long,
    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long,
    @ColumnInfo(name = "target_sets")
    val targetSets: Int = 3,
    @ColumnInfo(name = "target_reps_min")
    val targetRepsMin: Int? = null,
    @ColumnInfo(name = "target_reps_max")
    val targetRepsMax: Int? = null,
    @ColumnInfo(name = "target_weight_kg")
    val targetWeightKg: Double? = null,
    @ColumnInfo(name = "target_rpe")
    val targetRpe: Double? = null,
    @ColumnInfo(name = "target_rir")
    val targetRir: Int? = null,
    @ColumnInfo(name = "rest_seconds")
    val restSeconds: Int = 90,
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
)
