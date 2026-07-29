package com.example.fitlog.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["status"]),
        Index(value = ["template_id"]),
    ],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "schedule_id")
    val scheduleId: Long? = null,
    @ColumnInfo(name = "template_id")
    val templateId: Long? = null,
    @ColumnInfo(name = "template_name_snapshot")
    val templateNameSnapshot: String? = null,
    @ColumnInfo(name = "date")
    val date: Long,                    // epochDay
    @ColumnInfo(name = "start_time")
    val startTime: Long,               // epochMillis
    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,         // epochMillis
    @ColumnInfo(name = "status")
    val status: String,                // WorkoutStatus enum name
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    @ColumnInfo(name = "active_rest_started_at")
    val activeRestStartedAt: Long? = null,      // epochMillis
    @ColumnInfo(name = "active_rest_duration_seconds")
    val activeRestDurationSeconds: Int? = null,
    @ColumnInfo(name = "active_rest_set_record_id")
    val activeRestSetRecordId: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
