package com.example.fitlog.core.database.relation

import androidx.room.ColumnInfo

data class ScheduleWithTemplate(
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "template_id")
    val templateId: Long,
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int,
    @ColumnInfo(name = "start_date")
    val startDate: Long?,
    @ColumnInfo(name = "end_date")
    val endDate: Long?,
    @ColumnInfo(name = "repeat_interval_weeks")
    val repeatIntervalWeeks: Int,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "template_name")
    val templateName: String,
    @ColumnInfo(name = "template_notes")
    val templateNotes: String?,
    @ColumnInfo(name = "exercise_count")
    val exerciseCount: Int,
)
