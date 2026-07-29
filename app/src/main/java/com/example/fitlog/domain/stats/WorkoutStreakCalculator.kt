package com.example.fitlog.domain.stats

import com.example.fitlog.core.model.WorkoutSession
import com.example.fitlog.core.model.WorkoutStatus
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.domain.calendar.OverrideAction
import com.example.fitlog.domain.calendar.WorkoutPlanOverride
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure Kotlin calculator for workout streak and adherence statistics.
 *
 * Accepts domain models from the repository layer and computes stats
 * without any framework dependencies. The caller is responsible for
 * providing all relevant sessions and overrides for the date range
 * of interest.
 */
@Singleton
class WorkoutStreakCalculator @Inject constructor(
    private val dateProvider: CurrentDateProvider,
) {

    /**
     * Returns the current streak: the number of consecutive past planned-days
     * (including today if already completed) where a workout session exists
     * with status [WorkoutStatus.COMPLETED] or [WorkoutStatus.PARTIALLY_COMPLETED].
     *
     * A planned day is any date that has either a [WorkoutSession] entry or a
     * [WorkoutPlanOverride] with action [OverrideAction.SKIPPED].  Non-planned
     * days are transparent (they neither extend nor break the streak).
     *
     * The streak is broken when a planned day is encountered whose session is
     * missing or has any status other than COMPLETED / PARTIALLY_COMPLETED.
     */
    fun currentStreak(
        sessions: List<WorkoutSession>,
        overrides: List<WorkoutPlanOverride>,
    ): Int {
        val today = dateProvider.today()
        val plannedDays = buildPlannedDays(sessions, overrides)
        if (plannedDays.isEmpty()) return 0

        val minEpochDay = plannedDays.min()
        var streak = 0
        var day = today

        // Bound iteration to avoid infinite loops
        while (day.toEpochDay() >= minEpochDay) {
            val epochDay = day.toEpochDay()
            if (!plannedDays.contains(epochDay)) {
                day = day.minusDays(1)
                continue
            }

            val session = sessions.find { it.date == day }
            if (session != null && session.isCompleted()) {
                streak++
            } else {
                break
            }
            day = day.minusDays(1)
        }

        return streak
    }

    /**
     * Returns the longest historical streak of consecutive planned-days with
     * completed workouts. Scans all planned days in chronological order.
     */
    fun bestStreak(
        sessions: List<WorkoutSession>,
        overrides: List<WorkoutPlanOverride>,
    ): Int {
        val plannedDays = buildPlannedDays(sessions, overrides)
        if (plannedDays.isEmpty()) return 0

        val sortedDays = plannedDays.sorted()
        var best = 0
        var current = 0

        for (epochDay in sortedDays) {
            val date = LocalDate.ofEpochDay(epochDay)
            val session = sessions.find { it.date == date }
            if (session != null && session.isCompleted()) {
                current++
                if (current > best) best = current
            } else {
                current = 0
            }
        }

        return best
    }

    /**
     * Calculates the adherence rate over the last [days] calendar days
     * (excluding today): the number of planned days that were completed
     * divided by the total number of planned occurrences in that window.
     *
     * Returns a value between 0.0 and 1.0.  Returns 0.0 when there are
     * no planned days in the window.
     */
    fun adherenceRate(
        sessions: List<WorkoutSession>,
        overrides: List<WorkoutPlanOverride>,
        days: Int,
    ): Double {
        val today = dateProvider.today()
        val windowStart = today.minusDays(days.toLong())
        val startEpoch = windowStart.toEpochDay()
        val todayEpoch = today.toEpochDay()

        val plannedInWindow = buildPlannedDays(sessions, overrides)
            .filter { it >= startEpoch && it < todayEpoch }

        if (plannedInWindow.isEmpty()) return 0.0

        val completed = plannedInWindow.count { epochDay ->
            val date = LocalDate.ofEpochDay(epochDay)
            val session = sessions.find { it.date == date }
            session != null && session.isCompleted()
        }

        return completed.toDouble() / plannedInWindow.size
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    /**
     * Builds the set of epoch-days representing "planned" workout days.
     *
     * A day is considered planned if:
     * - a [WorkoutSession] exists for that date, OR
     * - a [WorkoutPlanOverride] with action [OverrideAction.SKIPPED] exists
     *   for that occurrence date.
     *
     * RESCHEDULED overrides are not included because the workout was moved
     * to another date (the session will appear on the rescheduled date).
     */
    private fun buildPlannedDays(
        sessions: List<WorkoutSession>,
        overrides: List<WorkoutPlanOverride>,
    ): Set<Long> {
        val days = mutableSetOf<Long>()
        sessions.forEach { days.add(it.date.toEpochDay()) }
        overrides
            .filter { it.action == OverrideAction.SKIPPED }
            .forEach { days.add(it.occurrenceDate.toEpochDay()) }
        return days
    }

    private fun WorkoutSession.isCompleted(): Boolean =
        status == WorkoutStatus.COMPLETED || status == WorkoutStatus.PARTIALLY_COMPLETED
}
