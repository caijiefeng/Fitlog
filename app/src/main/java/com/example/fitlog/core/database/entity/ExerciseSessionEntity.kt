package com.example.fitlog.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["session_id", "sort_order"]),
    ],
)
data class ExerciseSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    @ColumnInfo(name = "exercise_id")
    val exerciseId: Long? = null,            // nullable for ad-hoc exercises
    @ColumnInfo(name = "exercise_name_snapshot")
    val exerciseNameSnapshot: String,
    @ColumnInfo(name = "primary_muscle_group_snapshot")
    val primaryMuscleGroupSnapshot: String,
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
    @ColumnInfo(name = "planned_rest_seconds")
    val plannedRestSeconds: Int = 90,
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "is_skipped")
    val isSkipped: Boolean = false,
    @ColumnInfo(name = "is_completed", defaultValue = "0")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
