package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.PlannedWorkoutDao
import com.example.fitlog.core.database.entity.PlannedWorkoutEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class PlannedWorkout(
    val id: Long,
    val templateId: Long,
    val plannedDate: LocalDate,
    val note: String?,
    val createdAt: Long,
)

@Singleton
class PlannedWorkoutRepository @Inject constructor(
    private val dao: PlannedWorkoutDao,
) {

    suspend fun create(templateId: Long, plannedDate: LocalDate, note: String? = null): Long {
        return dao.insert(
            PlannedWorkoutEntity(
                templateId = templateId,
                plannedDate = plannedDate.toEpochDay(),
                note = note,
            )
        )
    }

    suspend fun getById(id: Long): PlannedWorkout? {
        return dao.getById(id)?.toDomain()
    }

    suspend fun getByDate(date: LocalDate): List<PlannedWorkout> {
        return dao.getByDate(date.toEpochDay()).map { it.toDomain() }
    }

    fun observeByDate(date: LocalDate): Flow<List<PlannedWorkout>> {
        return dao.observeByDate(date.toEpochDay()).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getByDateRange(start: LocalDate, end: LocalDate): List<PlannedWorkout> {
        return dao.getByDateRange(start.toEpochDay(), end.toEpochDay()).map { it.toDomain() }
    }

    suspend fun reschedule(id: Long, newDate: LocalDate) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(plannedDate = newDate.toEpochDay()))
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteByTemplateId(templateId: Long) {
        dao.deleteByTemplateId(templateId)
    }

    suspend fun getAll(): List<PlannedWorkout> {
        return dao.getAll().map { it.toDomain() }
    }

    private fun PlannedWorkoutEntity.toDomain() = PlannedWorkout(
        id = id,
        templateId = templateId,
        plannedDate = LocalDate.ofEpochDay(plannedDate),
        note = note,
        createdAt = createdAt,
    )
}
