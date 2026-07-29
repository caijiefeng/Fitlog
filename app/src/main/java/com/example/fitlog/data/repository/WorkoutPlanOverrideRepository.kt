package com.example.fitlog.data.repository

import androidx.room.Transaction
import com.example.fitlog.core.database.dao.WorkoutPlanOverrideDao
import com.example.fitlog.core.database.entity.WorkoutPlanOverrideEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutPlanOverrideRepository @Inject constructor(
    private val overrideDao: WorkoutPlanOverrideDao,
) {

    /**
     * Saves a plan override for the given [scheduleId] and [occurrenceDate].
     * If an existing override exists for the same schedule+occurrence, it updates
     * it in-place preserving the original [id] and [createdAt] timestamp.
     *
     * The operation is wrapped in a transaction to ensure uniqueness of the
     * (scheduleId, occurrenceDate) pair.
     */
    @Transaction
    suspend fun saveOverride(
        scheduleId: Long,
        templateId: Long,
        occurrenceDate: Long, // epochDay
        plannedDate: Long?,   // epochDay, null if SKIPPED
        action: String,       // "RESCHEDULED" or "SKIPPED"
    ): Long {
        val existing = overrideDao.getByScheduleAndOccurrence(scheduleId, occurrenceDate)
        return if (existing != null) {
            // Preserve id and createdAt
            val updated = existing.copy(
                templateId = templateId,
                plannedDate = plannedDate,
                action = action,
                updatedAt = System.currentTimeMillis(),
            )
            overrideDao.update(updated)
            existing.id
        } else {
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
    }

    /**
     * Removes the override for the given [scheduleId] and [occurrenceDate].
     */
    suspend fun removeOverride(scheduleId: Long, occurrenceDate: Long) {
        overrideDao.deleteByScheduleAndDate(scheduleId, occurrenceDate)
    }

    /**
     * Returns all overrides whose [occurrenceDate] or [plannedDate] falls within
     * the given date range. Delegates directly to the DAO.
     */
    suspend fun getRelevantToDateRange(startEpochDay: Long, endEpochDay: Long): List<WorkoutPlanOverrideEntity> {
        return overrideDao.getRelevantToDateRange(startEpochDay, endEpochDay)
    }

    /**
     * Observes all overrides as a Flow.
     */
    fun observeAll(): Flow<List<WorkoutPlanOverrideEntity>> =
        overrideDao.observeAll()
}
