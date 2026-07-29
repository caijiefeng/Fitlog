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
    /** Human-readable display identifier: "scheduleId:occurrenceEpochDay" or "session:sessionId" */
    val displayKey: String = key,
    val scheduleId: Long?,
    val templateId: Long?,
    val templateName: String,
    val occurrenceDate: LocalDate?,
    val plannedDate: LocalDate,
    /** The effective date shown to the user (original or rescheduled target) */
    val displayDate: LocalDate = plannedDate,
    val sessionId: Long?,
    val status: CalendarWorkoutStatus,
    val isQuickWorkout: Boolean,
    /** True when this marker represents a rescheduled occurrence displayed on its original date */
    val isOriginalDateMarker: Boolean = false,
    /** True when the user can start this workout (SCHEDULED and not overridden) */
    val canStart: Boolean = status == CalendarWorkoutStatus.SCHEDULED,
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
