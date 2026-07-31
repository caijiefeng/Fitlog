package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.WorkoutScheduleDao
import com.example.fitlog.core.database.entity.WorkoutScheduleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class DaySchedule(
    val dayOfWeek: Int,
    val dayName: String,
    val templateId: Long?,
    val templateName: String?,
    val exerciseCount: Int,
)

@Singleton
class WorkoutScheduleRepository @Inject constructor(
    private val scheduleDao: WorkoutScheduleDao,
) {

    fun getFullWeek(): Flow<List<DaySchedule>> =
        scheduleDao.getFullWeekSchedule().map { schedules ->
            val scheduleMap = schedules.associateBy { it.dayOfWeek }
            (1..7).map { day ->
                val s = scheduleMap[day]
                DaySchedule(
                    dayOfWeek = day,
                    dayName = dayName(day),
                    templateId = s?.templateId,
                    templateName = s?.templateName,
                    exerciseCount = s?.exerciseCount ?: 0,
                )
            }
        }

    fun getTodaySchedule(): Flow<DaySchedule?> {
        val today = LocalDate.now().dayOfWeek
        val dayOfWeek = dayOfWeekToInt(today)
        return scheduleDao.getScheduleForDay(dayOfWeek).map { s ->
            if (s != null) {
                DaySchedule(
                    dayOfWeek = s.dayOfWeek,
                    dayName = dayName(s.dayOfWeek),
                    templateId = s.templateId,
                    templateName = s.templateName,
                    exerciseCount = s.exerciseCount,
                )
            } else {
                null
            }
        }
    }

    suspend fun setTemplate(
        dayOfWeek: Int,
        templateId: Long,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        repeatIntervalWeeks: Int = 1,
    ) {
        upsertSchedule(
            dayOfWeek = dayOfWeek,
            templateId = templateId,
            startDate = startDate,
            endDate = endDate,
            repeatIntervalWeeks = repeatIntervalWeeks,
        )
    }

    /**
     * Creates recurring schedules for multiple weekdays in one batch.
     *
     * Each (template, dayOfWeek) pair is upserted against the unique index
     * (template_id, day_of_week), so scheduling a template on a weekday only
     * replaces that template's own entry — schedules of *other* templates on
     * the same weekday are left untouched.
     *
     * @return the number of schedule rows upserted
     */
    suspend fun createRecurringForWeekdays(
        dayOfWeeks: Collection<Int>,
        templateId: Long,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        repeatIntervalWeeks: Int = 1,
    ): Int {
        return dayOfWeeks
            .filter { it in 1..7 }
            .distinct()
            .map { dayOfWeek ->
                upsertSchedule(
                    dayOfWeek = dayOfWeek,
                    templateId = templateId,
                    startDate = startDate,
                    endDate = endDate,
                    repeatIntervalWeeks = repeatIntervalWeeks,
                )
            }
            .size
    }

    private suspend fun upsertSchedule(
        dayOfWeek: Int,
        templateId: Long,
        startDate: LocalDate?,
        endDate: LocalDate?,
        repeatIntervalWeeks: Int,
    ) {
        // REPLACE only removes the row that collides with the unique index
        // (template_id, day_of_week) — other templates on this weekday survive.
        scheduleDao.upsert(
            WorkoutScheduleEntity(
                templateId = templateId,
                dayOfWeek = dayOfWeek,
                startDate = startDate?.toEpochDay(),
                endDate = endDate?.toEpochDay(),
                repeatIntervalWeeks = repeatIntervalWeeks,
            )
        )
    }

    suspend fun clearDay(dayOfWeek: Int) {
        scheduleDao.clearDay(dayOfWeek)
    }

    suspend fun getById(id: Long): WorkoutScheduleEntity? {
        return scheduleDao.getById(id)
    }

    suspend fun updateSchedule(entity: WorkoutScheduleEntity) {
        scheduleDao.update(entity)
    }

    /**
     * Stops future recurrences of a schedule by setting its endDate to
     * the day before [cutoffDate] (epochDay). This preserves all past
     * and current occurrences but prevents future ones.
     */
    suspend fun stopFutureRecurrences(scheduleId: Long, cutoffDateEpochDay: Long) {
        val entity = scheduleDao.getById(scheduleId) ?: return
        val newEndDate = cutoffDateEpochDay - 1
        scheduleDao.update(entity.copy(endDate = newEndDate))
    }

    /**
     * Deletes a schedule entirely.
     */
    suspend fun deleteScheduleById(scheduleId: Long) {
        scheduleDao.deleteById(scheduleId)
    }

    companion object {
        fun dayOfWeekToInt(day: DayOfWeek): Int = when (day) {
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY -> 4
            DayOfWeek.FRIDAY -> 5
            DayOfWeek.SATURDAY -> 6
            DayOfWeek.SUNDAY -> 7
        }

        fun dayName(day: Int): String = when (day) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> ""
        }
    }
}
