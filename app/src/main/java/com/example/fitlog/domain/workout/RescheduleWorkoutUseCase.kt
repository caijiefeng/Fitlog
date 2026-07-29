package com.example.fitlog.domain.workout

import com.example.fitlog.core.database.dao.WorkoutSessionDao
import com.example.fitlog.data.repository.CalendarRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Reschedules a scheduled occurrence to a different date.
 *
 * Validates that the occurrence hasn't already been started or completed
 * before creating the RESCHEDULED override.
 */
class RescheduleWorkoutUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val sessionDao: WorkoutSessionDao,
) {

    /**
     * Reschedules the occurrence identified by [scheduleId] on [occurrenceDate]
     * to [targetDate].
     *
     * @throws IllegalStateException if the occurrence has already started or completed.
     */
    suspend operator fun invoke(
        scheduleId: Long,
        templateId: Long,
        occurrenceDate: LocalDate,
        targetDate: LocalDate,
    ) {
        val occurrenceEpochDay = occurrenceDate.toEpochDay()
        val targetEpochDay = targetDate.toEpochDay()

        // Validate not already started/completed
        val existing = sessionDao.getByScheduleAndOccurrence(scheduleId, occurrenceEpochDay)
        if (existing != null) {
            when (existing.status) {
                "IN_PROGRESS", "COMPLETED", "PARTIALLY_COMPLETED" ->
                    throw IllegalStateException("Cannot reschedule an already started or completed workout")
            }
        }

        calendarRepository.setOverride(
            scheduleId = scheduleId,
            templateId = templateId,
            occurrenceDate = occurrenceEpochDay,
            plannedDate = targetEpochDay,
            action = "RESCHEDULED",
        )
    }

    /**
     * Convenience: postpones the occurrence by 1 day.
     */
    suspend fun postponeTomorrow(
        scheduleId: Long,
        templateId: Long,
        occurrenceDate: LocalDate,
    ) {
        val targetDate = occurrenceDate.plusDays(1)
        invoke(scheduleId, templateId, occurrenceDate, targetDate)
    }
}
