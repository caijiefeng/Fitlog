package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.ReminderDao
import com.example.fitlog.core.database.entity.ReminderEntity
import com.example.fitlog.domain.reminder.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
) {

    /**
     * Saves a reminder after validation. Inserts a new record or updates an
     * existing one (matched by [id]), preserving the original [createdAt].
     *
     * @throws IllegalArgumentException if validation fails.
     */
    suspend fun saveReminder(
        id: Long = 0,
        scheduleId: Long?,
        label: String,
        timeOfDayMinutes: Int,
        daysOfWeekMask: Int,
        zoneId: String,
        isEnabled: Boolean = true,
    ): Long {
        // Validate
        val errors = ReminderEntity.validate(
            timeOfDayMinutes = timeOfDayMinutes,
            daysOfWeekMask = daysOfWeekMask,
            zoneId = zoneId,
            label = label,
        )
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException("Reminder validation failed: $errors")
        }

        val trimmedLabel = label.trim()

        if (id > 0) {
            val existing = reminderDao.getById(id)
            if (existing != null) {
                reminderDao.update(
                    existing.copy(
                        scheduleId = scheduleId,
                        label = trimmedLabel,
                        timeOfDayMinutes = timeOfDayMinutes,
                        daysOfWeekMask = daysOfWeekMask,
                        zoneId = zoneId,
                        isEnabled = isEnabled,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
                return existing.id
            }
        }

        return reminderDao.insert(
            ReminderEntity(
                scheduleId = scheduleId,
                label = trimmedLabel,
                timeOfDayMinutes = timeOfDayMinutes,
                daysOfWeekMask = daysOfWeekMask,
                zoneId = zoneId,
                isEnabled = isEnabled,
            )
        )
    }

    /**
     * Returns enabled reminders as a plain list.
     */
    suspend fun getEnabledReminders(): List<Reminder> =
        reminderDao.getEnabled().map { it.toDomain() }

    /**
     * Observes enabled reminders as a Flow.
     */
    fun observeEnabled(): Flow<List<Reminder>> =
        reminderDao.observeAll().map { list ->
            list.filter { it.isEnabled }.map { it.toDomain() }
        }

    /**
     * Deletes a reminder by [id].
     */
    suspend fun delete(id: Long) {
        reminderDao.delete(id)
    }

    private fun ReminderEntity.toDomain(): Reminder = Reminder(
        id = id,
        scheduleId = scheduleId,
        label = label,
        timeOfDayMinutes = timeOfDayMinutes,
        daysOfWeekMask = daysOfWeekMask,
        zoneId = zoneId,
        isEnabled = isEnabled,
    )
}
