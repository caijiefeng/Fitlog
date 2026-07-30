package com.example.fitlog.domain.calendar

import com.example.fitlog.core.database.entity.PlannedWorkoutEntity
import com.example.fitlog.core.database.entity.WorkoutPlanOverrideEntity
import com.example.fitlog.core.database.entity.WorkoutScheduleEntity
import com.example.fitlog.core.database.entity.WorkoutSessionEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import com.example.fitlog.core.time.CurrentDateProvider
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure Kotlin resolver that turns pre-loaded calendar data into a flat list
 * of [CalendarDay] with resolved occurrences. Accepts collections from the
 * repository so no N+1 database queries are performed.
 *
 * Supports:
 * - Weekly recurring schedules (day-of-week based)
 * - Date-bounded schedules (start_date / end_date)
 * - Multi-week intervals (repeat_interval_weeks)
 * - Plan overrides (skip / reschedule)
 * - One-time planned workouts
 * - Quick workout sessions (no scheduleId)
 */
@Singleton
class CalendarOccurrenceResolver @Inject constructor(
    private val dateProvider: CurrentDateProvider,
) {

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
        plannedWorkouts: List<PlannedWorkoutEntity> = emptyList(),
    ): List<CalendarDay> {
        val today = dateProvider.today()
        val overrideIndex: Map<String, WorkoutPlanOverrideEntity> = overrides.associateBy {
            overrideKey(it.scheduleId, it.occurrenceDate)
        }
        val sessionIndex: Map<String, WorkoutSessionEntity> = sessions.associateBy { session ->
            if (session.scheduleId != null) {
                overrideKey(session.scheduleId, session.occurrenceDate ?: session.date)
            } else {
                "session:${session.id}"
            }
        }

        // Index planned workouts by date
        val plannedByDate: Map<Long, List<PlannedWorkoutEntity>> =
            plannedWorkouts.groupBy { it.plannedDate }

        // Pre-compute schedule look-ups by day-of-week
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

            // 1. Scheduled occurrences from recurring schedules
            val daySchedules = scheduleByDayOfWeek[dayOfWeekIso] ?: emptyList()
            for (schedule in daySchedules) {
                // Check if schedule is active on this date (date-bounded + interval)
                if (!isScheduleActiveOnDate(schedule, current)) continue

                val occKey = overrideKey(schedule.id, current)
                val override = overrideIndex[occKey]
                val session = sessionIndex[occKey]

                val isSkipped = override?.action == OverrideAction.SKIPPED.name
                val isRescheduled = override?.action == OverrideAction.RESCHEDULED.name
                val plannedDate = override?.plannedDate ?: current
                val isOriginalDateMarker = isRescheduled && plannedDate != current

                val template = templates[schedule.templateId]
                val templateName = template?.name ?: "未知训练"

                // Priority: Session state > SKIPPED override > Original date marker (RESCHEDULED) > Default SCHEDULED
                val status = if (session != null) {
                    mapSessionStatus(session.status)
                } else if (isSkipped) {
                    CalendarWorkoutStatus.SKIPPED
                } else if (isOriginalDateMarker) {
                    CalendarWorkoutStatus.RESCHEDULED
                } else {
                    CalendarWorkoutStatus.SCHEDULED
                }

                val occurrenceDate = LocalDate.ofEpochDay(current)
                dayOccurrences.add(
                    CalendarWorkoutOccurrence(
                        key = occKey,
                        displayKey = buildDisplayKey(schedule.id, current, isOriginalDateMarker = isOriginalDateMarker),
                        scheduleId = schedule.id,
                        templateId = schedule.templateId,
                        templateName = templateName,
                        occurrenceDate = occurrenceDate,
                        displayDate = LocalDate.ofEpochDay(plannedDate),
                        plannedDate = LocalDate.ofEpochDay(plannedDate),
                        sessionId = session?.id,
                        status = status,
                        isQuickWorkout = false,
                        isOriginalDateMarker = isOriginalDateMarker,
                        canStart = status == CalendarWorkoutStatus.SCHEDULED,
                    )
                )
            }

            // 2. Overrides rescheduled TO this date (plannedDate matches)
            val rescheduledToToday = overrides.filter { ov ->
                ov.action == OverrideAction.RESCHEDULED.name && ov.plannedDate == current &&
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
                    mapSessionStatus(session.status)
                } else {
                    CalendarWorkoutStatus.RESCHEDULED
                }

                dayOccurrences.add(
                    CalendarWorkoutOccurrence(
                        key = occKey,
                        displayKey = buildDisplayKey(override.scheduleId, override.plannedDate ?: override.occurrenceDate, isOriginalDateMarker = false),
                        scheduleId = override.scheduleId,
                        templateId = override.templateId,
                        templateName = templateName,
                        occurrenceDate = LocalDate.ofEpochDay(override.occurrenceDate),
                        displayDate = date,
                        plannedDate = date,
                        sessionId = session?.id,
                        status = status,
                        isQuickWorkout = false,
                        isOriginalDateMarker = true,
                        canStart = status == CalendarWorkoutStatus.SCHEDULED,
                    )
                )
            }

            // 3. One-time planned workouts on this date
            val dayPlanned = plannedByDate[current] ?: emptyList()
            for (planned in dayPlanned) {
                val occKey = "planned:${planned.id}"
                val template = templates[planned.templateId]
                val templateName = template?.name ?: "未知训练"

                // Check if a session was already created from this planned workout
                val session = sessions.find { s ->
                    s.templateId == planned.templateId && s.date == current && s.scheduleId == null
                }

                // Only match sessions that don't belong to any schedule
                // (we already checked schedule-based sessions above)
                val matchingSession = session?.takeIf { it.scheduleId == null }

                val status = if (matchingSession != null) {
                    mapSessionStatus(matchingSession.status)
                } else {
                    CalendarWorkoutStatus.SCHEDULED
                }

                dayOccurrences.add(
                    CalendarWorkoutOccurrence(
                        key = occKey,
                        displayKey = occKey,
                        scheduleId = null,
                        templateId = planned.templateId,
                        templateName = templateName,
                        occurrenceDate = date,
                        displayDate = date,
                        plannedDate = date,
                        sessionId = matchingSession?.id,
                        status = status,
                        isQuickWorkout = false,
                        isOriginalDateMarker = false,
                        canStart = status == CalendarWorkoutStatus.SCHEDULED,
                    )
                )
            }

            // 4. Quick workouts (no scheduleId) on this date
            // Exclude sessions that matched a planned workout above
            val matchedSessionIds = dayOccurrences.mapNotNull { it.sessionId }.toSet()
            val quickSessions = sessions.filter { s ->
                s.scheduleId == null && s.date == current && s.id !in matchedSessionIds
            }
            for (session in quickSessions) {
                val occKey = "session:${session.id}"
                val templateName = session.templateNameSnapshot ?: "快速训练"
                dayOccurrences.add(
                    CalendarWorkoutOccurrence(
                        key = occKey,
                        displayKey = occKey,
                        scheduleId = null,
                        templateId = session.templateId,
                        templateName = templateName,
                        occurrenceDate = date,
                        displayDate = date,
                        plannedDate = date,
                        sessionId = session.id,
                        status = mapSessionStatus(session.status),
                        isQuickWorkout = true,
                        isOriginalDateMarker = false,
                        canStart = false,
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
                    hasCheckIn = false,
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
        plannedWorkouts: List<PlannedWorkoutEntity> = emptyList(),
    ): List<CalendarDay> {
        val start = yearMonth.atDay(1).toEpochDay()
        val end = yearMonth.atEndOfMonth().toEpochDay()
        return resolveRange(start, end, schedules, overrides, sessions, templates, plannedWorkouts)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Determines whether a [schedule] is active on the given [dateEpochDay].
     *
     * Rules:
     * - If [startDate] is set, the date must be >= startDate
     * - If [endDate] is set, the date must be <= endDate
     * - If [repeatIntervalWeeks] > 1 and [startDate] is set, only every N-th week
     *   since the first occurrence counts
     */
    private fun isScheduleActiveOnDate(schedule: WorkoutScheduleEntity, dateEpochDay: Long): Boolean {
        val startDate = schedule.startDate
        val endDate = schedule.endDate

        // Check start date bound
        if (startDate != null && dateEpochDay < startDate) return false

        // Check end date bound
        if (endDate != null && dateEpochDay > endDate) return false

        // Check repeat interval (only matters when > 1 and we have a startDate)
        val intervalWeeks = schedule.repeatIntervalWeeks
        if (intervalWeeks > 1 && startDate != null) {
            // Find the first occurrence on or after startDate that matches this day_of_week
            val firstOccurrence = findFirstScheduleOccurrence(startDate, schedule.dayOfWeek)
            if (dateEpochDay < firstOccurrence) return false

            // Calculate weeks between first occurrence and current date
            val daysDiff = dateEpochDay - firstOccurrence
            val weeksDiff = daysDiff / 7L
            if (weeksDiff % intervalWeeks != 0L) return false
        }

        return true
    }

    /**
     * Finds the first epochDay on or after [startDateEpochDay] that falls on
     * the given [dayOfWeek] (1=Mon ... 7=Sun).
     */
    private fun findFirstScheduleOccurrence(startDateEpochDay: Long, targetDayOfWeek: Int): Long {
        val startDate = LocalDate.ofEpochDay(startDateEpochDay)
        val startDayOfWeek = startDate.dayOfWeek.value // 1=Mon ... 7=Sun
        var diff = targetDayOfWeek - startDayOfWeek
        if (diff < 0) diff += 7
        return startDateEpochDay + diff
    }

    private fun overrideKey(scheduleId: Long, occurrenceDate: Long): String =
        "${scheduleId}:${occurrenceDate}"

    private fun buildDisplayKey(
        scheduleId: Long,
        dateEpochDay: Long,
        isOriginalDateMarker: Boolean,
    ): String {
        val prefix = if (isOriginalDateMarker) "orig" else "tgt"
        return "schedule:${scheduleId}:${dateEpochDay}:${prefix}"
    }

    private fun mapSessionStatus(sessionStatus: String): CalendarWorkoutStatus = when {
        sessionStatus == "IN_PROGRESS" -> CalendarWorkoutStatus.IN_PROGRESS
        sessionStatus == "COMPLETED" -> CalendarWorkoutStatus.COMPLETED
        sessionStatus == "PARTIALLY_COMPLETED" -> CalendarWorkoutStatus.PARTIALLY_COMPLETED
        sessionStatus == "CANCELLED" -> CalendarWorkoutStatus.CANCELLED
        sessionStatus == "SKIPPED" -> CalendarWorkoutStatus.SKIPPED
        else -> CalendarWorkoutStatus.SCHEDULED
    }
}
