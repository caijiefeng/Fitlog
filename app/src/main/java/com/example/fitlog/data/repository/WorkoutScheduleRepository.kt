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

    suspend fun setTemplate(dayOfWeek: Int, templateId: Long) {
        scheduleDao.deleteByDayOfWeek(dayOfWeek)
        scheduleDao.insert(
            WorkoutScheduleEntity(templateId = templateId, dayOfWeek = dayOfWeek)
        )
    }

    suspend fun clearDay(dayOfWeek: Int) {
        scheduleDao.clearDay(dayOfWeek)
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
