package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.PlannedWorkoutDao
import com.example.fitlog.core.database.dao.WorkoutPlanOverrideDao
import com.example.fitlog.core.database.dao.WorkoutScheduleDao
import com.example.fitlog.core.database.dao.WorkoutSessionDao
import com.example.fitlog.core.database.dao.WorkoutTemplateDao
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
    private val plannedWorkoutDao: PlannedWorkoutDao,
    private val resolver: CalendarOccurrenceResolver,
) {

    /**
     * Loads all data for the given [yearMonth] in **one batch**, then passes
     * everything to [CalendarOccurrenceResolver.resolveMonth] to produce the
     * flat day list with resolved occurrences.
     */
    suspend fun getMonth(yearMonth: YearMonth): List<CalendarDay> {
        val startEpochDay = yearMonth.atDay(1).toEpochDay()
        val endEpochDay = yearMonth.atEndOfMonth().toEpochDay()

        val schedules = scheduleDao.getAllActiveList()
        val overrides = overrideDao.getRelevantToDateRange(startEpochDay, endEpochDay)
        val sessions = sessionDao.getByDateRange(startEpochDay, endEpochDay)
        val templates = templateDao.getAllActiveList().associateBy { it.id }
        val plannedWorkouts = plannedWorkoutDao.getByDateRange(startEpochDay, endEpochDay)

        return resolver.resolveMonth(yearMonth, schedules, overrides, sessions, templates, plannedWorkouts)
    }

    /**
     * Loads a single day's occurrences.
     */
    suspend fun getDayDetail(epochDay: Long): List<CalendarDay> {
        val start = epochDay
        val end = epochDay

        val schedules = scheduleDao.getAllActiveList()
        val overrides = overrideDao.getRelevantToDateRange(start, end)
        val sessions = sessionDao.getByDateRange(start, end)
        val templates = templateDao.getAllActiveList().associateBy { it.id }
        val plannedWorkouts = plannedWorkoutDao.getByDateRange(start, end)

        return resolver.resolveRange(start, end, schedules, overrides, sessions, templates, plannedWorkouts)
    }
}
