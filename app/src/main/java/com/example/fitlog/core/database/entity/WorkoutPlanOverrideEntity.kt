package com.example.fitlog.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_plan_overrides",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["schedule_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["schedule_id", "occurrence_date"], unique = true),
        Index(value = ["planned_date"]),
        Index(value = ["action"]),
    ],
)
data class WorkoutPlanOverrideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "schedule_id") val scheduleId: Long,
    @ColumnInfo(name = "template_id") val templateId: Long,
    @ColumnInfo(name = "occurrence_date") val occurrenceDate: Long,  // epochDay
    @ColumnInfo(name = "planned_date") val plannedDate: Long? = null, // epochDay, null if SKIPPED
    @ColumnInfo(name = "action") val action: String,  // RESCHEDULED or SKIPPED
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
