package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.CheckInDao
import com.example.fitlog.core.database.entity.CheckInEntity
import com.example.fitlog.domain.checkin.CheckIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInRepository @Inject constructor(
    private val checkInDao: CheckInDao,
) {

    /**
     * Validates mood/energy values.
     */
    private fun validateMood(value: Int?): Boolean =
        value == null || (value in 1..5)

    private fun validateEnergyLevel(value: Int?): Boolean =
        value == null || (value in 1..5)

    /**
     * Saves a check-in for the given [date].
     *
     * If a check-in already exists for that date, it updates the existing record
     * preserving the original [id] and [createdAt]. Only non-null values are
     * used to overwrite existing fields — null values are ignored and the existing
     * value is kept.
     *
     * @param date The date of the check-in.
     * @param mood Mood rating 1-5, or null to keep existing value.
     * @param energyLevel Energy level 1-5, or null to keep existing value.
     * @param notes Optional notes, or null to keep existing value.
     * @return The [CheckIn] that was saved (either newly created or updated).
     * @throws IllegalArgumentException if a value falls outside allowed ranges,
     *         or if all supplied values are null/blank ("nothing to save").
     */
    suspend fun saveCheckIn(
        date: LocalDate,
        mood: Int? = null,
        energyLevel: Int? = null,
        notes: String? = null,
    ): CheckIn {
        val trimmedNotes = notes?.trim()?.takeIf { it.isNotEmpty() }

        // Validate
        require(validateMood(mood)) { "Mood must be null or between 1 and 5, got $mood" }
        require(validateEnergyLevel(energyLevel)) { "Energy level must be null or between 1 and 5, got $energyLevel" }

        // Reject completely empty
        val hasSomething = mood != null || energyLevel != null || trimmedNotes != null
        require(hasSomething) { "Nothing to save: all fields (mood, energyLevel, notes) are null or blank" }

        val epochDay = date.toEpochDay()
        val existing = checkInDao.getByDate(epochDay)

        return if (existing != null) {
            // Preserve id and createdAt; only overwrite with non-null values
            val updated = existing.copy(
                mood = mood ?: existing.mood,
                energyLevel = energyLevel ?: existing.energyLevel,
                notes = trimmedNotes ?: existing.notes,
                updatedAt = System.currentTimeMillis(),
            )
            checkInDao.upsert(updated)
            updated.toDomain()
        } else {
            val entity = CheckInEntity(
                date = epochDay,
                mood = mood,
                energyLevel = energyLevel,
                notes = trimmedNotes,
            )
            val id = checkInDao.upsert(entity)
            entity.copy(id = id).toDomain()
        }
    }

    /**
     * Returns check-ins within the given epoch-day range.
     */
    suspend fun getByDateRange(startEpochDay: Long, endEpochDay: Long): List<CheckIn> =
        checkInDao.getByDateRange(startEpochDay, endEpochDay).map { it.toDomain() }

    /**
     * Returns check-ins within the given [LocalDate] range.
     */
    suspend fun getByDateRange(start: LocalDate, end: LocalDate): List<CheckIn> =
        getByDateRange(start.toEpochDay(), end.toEpochDay())

    /**
     * Observes check-ins for a specific date as a Flow.
     */
    fun observeByDate(date: LocalDate): Flow<CheckIn?> {
        val epochDay = date.toEpochDay()
        return checkInDao.observeByDate(epochDay).map { it?.toDomain() }
    }

    private fun CheckInEntity.toDomain(): CheckIn = CheckIn(
        id = id,
        date = LocalDate.ofEpochDay(date),
        sessionId = sessionId,
        mood = mood,
        energyLevel = energyLevel,
        notes = notes,
    )
}
