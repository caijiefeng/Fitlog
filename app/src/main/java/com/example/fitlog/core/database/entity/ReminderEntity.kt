package com.example.fitlog.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["schedule_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["is_enabled"]),
    ],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "schedule_id") val scheduleId: Long? = null,
    @ColumnInfo(name = "label") val label: String = "",
    @ColumnInfo(name = "time_of_day_minutes") val timeOfDayMinutes: Int = 480, // 0-1439
    @ColumnInfo(name = "days_of_week_mask") val daysOfWeekMask: Int = 0, // 7-bit bitmask: bit0=Mon...bit6=Sun
    @ColumnInfo(name = "zone_id") val zoneId: String = "",
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
