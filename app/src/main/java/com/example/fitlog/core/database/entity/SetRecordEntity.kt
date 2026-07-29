package com.example.fitlog.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_records",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["exercise_session_id", "set_number"], unique = true),
    ],
)
data class SetRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "exercise_session_id")
    val exerciseSessionId: Long,
    @ColumnInfo(name = "set_number")
    val setNumber: Int,
    @ColumnInfo(name = "set_type")
    val setType: String = "WORKING",     // WARMUP, WORKING, DROP, FAILURE
    @ColumnInfo(name = "reps")
    val reps: Int? = null,
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double? = null,
    @ColumnInfo(name = "rpe")
    val rpe: Double? = null,
    @ColumnInfo(name = "rir")
    val rir: Int? = null,
    @ColumnInfo(name = "rest_seconds")
    val restSeconds: Int? = null,
    @ColumnInfo(name = "completed")
    val completed: Boolean = false,
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
