package com.example.fitlog.domain.workout

import com.example.fitlog.data.repository.CalendarRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Removes an override for a schedule occurrence, restoring the original
 * schedule behaviour.
 */
class RestoreWorkoutScheduleUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
) {

    /**
     * Removes the override (reschedule or skip) for [scheduleId] on [occurrenceDate].
     */
    suspend operator fun invoke(
        scheduleId: Long,
        occurrenceDate: LocalDate,
    ) {
        calendarRepository.removeOverride(
            scheduleId = scheduleId,
            occurrenceDate = occurrenceDate.toEpochDay(),
        )
    }
}
