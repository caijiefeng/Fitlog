package com.example.fitlog.domain.reminder

data class Reminder(
    val id: Long,
    val scheduleId: Long?,
    val label: String,
    val timeOfDayMinutes: Int,
    val daysOfWeekMask: Int,
    val zoneId: String,
    val isEnabled: Boolean,
)
