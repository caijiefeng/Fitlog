package com.example.fitlog.domain.calendar

import java.time.LocalDate

// ── Calendar Domain Models ─────────────────────────────────────────────────

enum class CalendarWorkoutStatus {
    SCHEDULED,
    RESCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    PARTIALLY_COMPLETED,
    SKIPPED,
    CANCELLED,
}

data class CalendarWorkoutOccurrence(
    val key: String,
    val scheduleId: Long?,
    val templateId: Long?,
    val templateName: String,
    val occurrenceDate: LocalDate?,
    val plannedDate: LocalDate,
    val sessionId: Long?,
    val status: CalendarWorkoutStatus,
    val isQuickWorkout: Boolean,
)

data class CalendarDay(
    val epochDay: Long,
    val date: LocalDate,
    val dayOfMonth: Int,
    val dayOfWeek: Int,
    val occurrences: List<CalendarWorkoutOccurrence> = emptyList(),
    val hasCheckIn: Boolean = false,
    val isToday: Boolean = false,
)

// ── Override Domain Models ─────────────────────────────────────────────────

enum class OverrideAction {
    RESCHEDULED,
    SKIPPED,
}

data class WorkoutPlanOverride(
    val scheduleId: Long,
    val templateId: Long,
    val occurrenceDate: LocalDate,
    val plannedDate: LocalDate?,
    val action: OverrideAction,
)
