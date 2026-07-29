package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.WorkoutPlanOverrideDao
import com.example.fitlog.core.database.dao.WorkoutScheduleDao
import com.example.fitlog.core.database.dao.WorkoutSessionDao
import com.example.fitlog.core.database.dao.WorkoutTemplateDao
import com.example.fitlog.core.database.entity.WorkoutPlanOverrideEntity
import com.example.fitlog.domain.calendar.CalendarDay
import com.example.fitlog.domain.calendar.CalendarOccurrenceResolver
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    private val scheduleDao: WorkoutScheduleDao,
    private val overrideDao: WorkoutPlanOverrideDao,
    private val sessionDao: WorkoutSessionDao,
    private val templateDao: WorkoutTemplateDao,
    private val resolver: CalendarOccurrenceResolver,
) {

    /**
     * Loads all data for the given [yearMonth] in **one batch**, then passes
     * everything to [CalendarOccurrenceResolver.resolveMonth] to produce the
     * flat day list with resolved occurrences.
     *
     * No N+1 queries — all DAO calls happen before resolution begins.
     */
    suspend fun getMonth(yearMonth: YearMonth): List<CalendarDay> {
        val startEpochDay = yearMonth.atDay(1).toEpochDay()
        val endEpochDay = yearMonth.atEndOfMonth().toEpochDay()

        // ── Batch load ──────────────────────────────────────────────────
        val schedules = scheduleDao.getAllActiveList()
        val overrides = overrideDao.getRelevantToDateRange(startEpochDay, endEpochDay)
        val sessions = sessionDao.getByDateRange(startEpochDay, endEpochDay)
        val templates = templateDao.getAllActiveList().associateBy { it.id }

        return resolver.resolveMonth(yearMonth, schedules, overrides, sessions, templates)
    }

    /**
     * Loads a single day's occurrences. Useful for the day-detail panel.
     */
    suspend fun getDayDetail(epochDay: Long): List<CalendarDay> {
        val start = epochDay
        val end = epochDay

        val schedules = scheduleDao.getAllActiveList()
        val overrides = overrideDao.getRelevantToDateRange(start, end)
        val sessions = sessionDao.getByDateRange(start, end)
        val templates = templateDao.getAllActiveList().associateBy { it.id }

        return resolver.resolveRange(start, end, schedules, overrides, sessions, templates)
    }

    /**
     * Creates or updates a plan override.
     *
     * @param action "RESCHEDULED" or "SKIPPED"
     * @param plannedDate target epochDay (required when RESCHEDULED, null when SKIPPED)
     */
    suspend fun setOverride(
        scheduleId: Long,
        templateId: Long,
        occurrenceDate: Long,
        plannedDate: Long?,
        action: String,
    ) {
        val existing = overrideDao.getByScheduleAndOccurrence(scheduleId, occurrenceDate)
        if (existing != null) {
            overrideDao.deleteByScheduleAndDate(scheduleId, occurrenceDate)
        }
        overrideDao.insert(
            WorkoutPlanOverrideEntity(
                scheduleId = scheduleId,
                templateId = templateId,
                occurrenceDate = occurrenceDate,
                plannedDate = plannedDate,
                action = action,
            )
        )
    }

    /**
     * Removes a plan override, effectively restoring the original schedule.
     */
    suspend fun removeOverride(scheduleId: Long, occurrenceDate: Long) {
        overrideDao.deleteByScheduleAndDate(scheduleId, occurrenceDate)
    }
}
