package com.example.fitlog.domain.calendar

import com.example.fitlog.core.database.entity.WorkoutPlanOverrideEntity
import com.example.fitlog.core.database.entity.WorkoutScheduleEntity
import com.example.fitlog.core.database.entity.WorkoutSessionEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure Kotlin resolver that turns pre-loaded calendar data into a flat list
 * of [CalendarDay] with resolved occurrences. Accepts collections from the
 * repository so no N+1 database queries are performed.
 */
@Singleton
class CalendarOccurrenceResolver @Inject constructor() {

    /**
     * Resolves a date range [startEpochDay] to [endEpochDay] (inclusive) into
     * a chronologically ordered list of [CalendarDay].
     */
    fun resolveRange(
        startEpochDay: Long,
        endEpochDay: Long,
        schedules: List<WorkoutScheduleEntity>,
        overrides: List<WorkoutPlanOverrideEntity>,
        sessions: List<WorkoutSessionEntity>,
        templates: Map<Long, WorkoutTemplateEntity>,
    ): List<CalendarDay> {
        val today = LocalDate.now()
        val overrideIndex: Map<String, WorkoutPlanOverrideEntity> = overrides.associateBy {
            overrideKey(it.scheduleId, it.occurrenceDate)
        }
        val sessionIndex: Map<String, WorkoutSessionEntity> = sessions.associateBy { session ->
            if (session.scheduleId != null) {
                overrideKey(session.scheduleId, session.occurrenceDate ?: session.date)
            } else {
                "adhoc_${session.id}"
            }
        }

        // Pre-compute schedule look-ups
        val scheduleByDayOfWeek: Map<Int, List<WorkoutScheduleEntity>> =
            schedules.filter { it.isActive }.groupBy { it.dayOfWeek }

        val result = mutableListOf<CalendarDay>()

        // Iterate day-by-day over the range
        var current = startEpochDay
        while (current <= endEpochDay) {
            val date = LocalDate.ofEpochDay(current)
            val dayOfWeekIso = date.dayOfWeek.value // 1 (Mon) … 7 (Sun)
            val dayOfMonth = date.dayOfMonth
            val isToday = date == today

            val dayOccurrences = mutableListOf<CalendarWorkoutOccurrence>()

            // 1. Scheduled occurrences
            val daySchedules = scheduleByDayOfWeek[dayOfWeekIso] ?: emptyList()
            for (schedule in daySchedules) {
                val occKey = overrideKey(schedule.id, current)
                val override = overrideIndex[occKey]
                val session = sessionIndex[occKey]

                val isSkipped = override?.action == "SKIPPED"
                val isRescheduled = override?.action == "RESCHEDULED"
                val plannedDate = override?.plannedDate ?: current

                val template = templates[schedule.templateId]
                val templateName = template?.name ?: "未知训练"

                // If there is a session on this date for this schedule, use its status
                val status = if (session != null) {
                    mapSessionStatus(session.status, isSkipped, isRescheduled)
                } else if (isSkipped) {
                    CalendarWorkoutStatus.SKIPPED
                } else if (isRescheduled) {
                    CalendarWorkoutStatus.RESCHEDULED
                } else {
                    CalendarWorkoutStatus.SCHEDULED
                }

                dayOccurrences.add(
                    CalendarWorkoutOccurrence(
                        key = occKey,
                        scheduleId = schedule.id,
                        templateId = schedule.templateId,
                        templateName = templateName,
                        occurrenceDate = LocalDate.ofEpochDay(current),
                        plannedDate = LocalDate.ofEpochDay(plannedDate),
                        sessionId = session?.id,
                        status = status,
                        isQuickWorkout = false,
                    )
                )
            }

            // 2. Overrides rescheduled TO this date (plannedDate matches)
            val rescheduledToToday = overrides.filter { ov ->
                ov.action == "RESCHEDULED" && ov.plannedDate == current &&
                    ov.occurrenceDate != current
            }
            for (override in rescheduledToToday) {
                val occKey = overrideKey(override.scheduleId, override.occurrenceDate)
                // If we already added this via schedule, skip (already handled above)
                if (dayOccurrences.any { it.key == occKey }) continue

                val session = sessionIndex[occKey]
                val template = templates[override.templateId]
                val templateName = template?.name ?: "未知训练"

                val status = if (session != null) {
                    mapSessionStatus(session.status, isSkipped = false, isRescheduled = true)
                } else {
                    CalendarWorkoutStatus.RESCHEDULED
                }

                dayOccurrences.add(
                    CalendarWorkoutOccurrence(
                        key = occKey,
                        scheduleId = override.scheduleId,
                        templateId = override.templateId,
                        templateName = templateName,
                        occurrenceDate = LocalDate.ofEpochDay(override.occurrenceDate),
                        plannedDate = date,
                        sessionId = session?.id,
                        status = status,
                        isQuickWorkout = false,
                    )
                )
            }

            // 3. Quick workouts (no scheduleId) on this date
            val quickSessions = sessions.filter { s ->
                s.scheduleId == null && s.date == current
            }
            for (session in quickSessions) {
                val occKey = "adhoc_${session.id}"
                val templateName = session.templateNameSnapshot ?: "快速训练"
                dayOccurrences.add(
                    CalendarWorkoutOccurrence(
                        key = occKey,
                        scheduleId = null,
                        templateId = session.templateId,
                        templateName = templateName,
                        occurrenceDate = date,
                        plannedDate = date,
                        sessionId = session.id,
                        status = mapSessionStatus(
                            session.status,
                            isSkipped = false,
                            isRescheduled = false,
                        ),
                        isQuickWorkout = true,
                    )
                )
            }

            result.add(
                CalendarDay(
                    epochDay = current,
                    date = date,
                    dayOfMonth = dayOfMonth,
                    dayOfWeek = dayOfWeekIso,
                    occurrences = dayOccurrences,
                    hasCheckIn = false, // resolved by repository if needed
                    isToday = isToday,
                )
            )

            current++
        }

        return result
    }

    /**
     * Resolves a single [YearMonth] into [CalendarDay] entries for every day
     * in that month (1st to last day), delegating to [resolveRange].
     */
    fun resolveMonth(
        yearMonth: YearMonth,
        schedules: List<WorkoutScheduleEntity>,
        overrides: List<WorkoutPlanOverrideEntity>,
        sessions: List<WorkoutSessionEntity>,
        templates: Map<Long, WorkoutTemplateEntity>,
    ): List<CalendarDay> {
        val start = yearMonth.atDay(1).toEpochDay()
        val end = yearMonth.atEndOfMonth().toEpochDay()
        return resolveRange(start, end, schedules, overrides, sessions, templates)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun overrideKey(scheduleId: Long, occurrenceDate: Long): String =
        "${scheduleId}_${occurrenceDate}"

    private fun mapSessionStatus(
        sessionStatus: String,
        isSkipped: Boolean,
        isRescheduled: Boolean,
    ): CalendarWorkoutStatus = when {
        isSkipped -> CalendarWorkoutStatus.SKIPPED
        isRescheduled -> CalendarWorkoutStatus.RESCHEDULED
        sessionStatus == "IN_PROGRESS" -> CalendarWorkoutStatus.IN_PROGRESS
        sessionStatus == "COMPLETED" -> CalendarWorkoutStatus.COMPLETED
        sessionStatus == "PARTIALLY_COMPLETED" -> CalendarWorkoutStatus.PARTIALLY_COMPLETED
        sessionStatus == "CANCELLED" -> CalendarWorkoutStatus.CANCELLED
        sessionStatus == "SKIPPED" -> CalendarWorkoutStatus.SKIPPED
        else -> CalendarWorkoutStatus.SCHEDULED
    }
}
