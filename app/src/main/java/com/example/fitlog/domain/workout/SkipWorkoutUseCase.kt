package com.example.fitlog.domain.workout

import com.example.fitlog.core.database.dao.WorkoutSessionDao
import com.example.fitlog.data.repository.WorkoutPlanOverrideRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Creates a SKIPPED override for a scheduled occurrence.
 *
 * Validates that the occurrence hasn't already been started or completed
 * before creating the override.
 */
class SkipWorkoutUseCase @Inject constructor(
    private val overrideRepository: WorkoutPlanOverrideRepository,
    private val sessionDao: WorkoutSessionDao,
) {

    /**
     * Skips the occurrence identified by [scheduleId] on [occurrenceDate].
     *
     * @throws IllegalStateException if the occurrence has already started or completed.
     */
    suspend operator fun invoke(
        scheduleId: Long,
        templateId: Long,
        occurrenceDate: LocalDate,
    ) {
        val occurrenceEpochDay = occurrenceDate.toEpochDay()

        val existing = sessionDao.getByScheduleAndOccurrence(scheduleId, occurrenceEpochDay)
        if (existing != null) {
            when (existing.status) {
                "IN_PROGRESS", "COMPLETED", "PARTIALLY_COMPLETED" ->
                    throw IllegalStateException("Cannot skip an already started or completed workout")
            }
        }

        overrideRepository.saveOverride(
            scheduleId = scheduleId,
            templateId = templateId,
            occurrenceDate = occurrenceEpochDay,
            plannedDate = null,
            action = "SKIPPED",
        )
    }
}
