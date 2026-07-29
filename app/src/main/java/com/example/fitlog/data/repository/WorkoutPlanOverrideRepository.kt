package com.example.fitlog.data.repository

import androidx.room.withTransaction
import com.example.fitlog.core.database.FitLogDatabase
import com.example.fitlog.core.database.dao.WorkoutPlanOverrideDao
import com.example.fitlog.core.database.entity.WorkoutPlanOverrideEntity
import com.example.fitlog.core.time.AppClock
import com.example.fitlog.domain.calendar.OverrideAction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutPlanOverrideRepository @Inject constructor(
    private val db: FitLogDatabase,
    private val overrideDao: WorkoutPlanOverrideDao,
    private val appClock: AppClock,
) {

    /**
     * Saves a plan override for the given [scheduleId] and [occurrenceDate].
     * If an existing override exists for the same schedule+occurrence, it updates
     * it in-place preserving the original [id] and [createdAt] timestamp.
     *
     * The operation is wrapped in a transaction to ensure uniqueness of the
     * (scheduleId, occurrenceDate) pair.
     */
    suspend fun saveOverride(
        scheduleId: Long,
        templateId: Long,
        occurrenceDate: Long, // epochDay
        plannedDate: Long?,   // epochDay, null if SKIPPED
        action: OverrideAction,
    ): Long = db.withTransaction {
        val existing = overrideDao.getByScheduleAndOccurrence(scheduleId, occurrenceDate)
        if (existing != null) {
            // Preserve id and createdAt, only update plannedDate/action/updatedAt
            val updated = existing.copy(
                plannedDate = plannedDate,
                action = action.name,
                updatedAt = appClock.nowInstant().toEpochMilli(),
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
                    action = action.name,
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
