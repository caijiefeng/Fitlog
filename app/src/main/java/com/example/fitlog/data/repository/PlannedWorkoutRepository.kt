package com.example.fitlog.data.repository

import androidx.room.Transaction
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

/**
 * Outcome of a batch scheduling operation.
 *
 * @property created plans that were successfully inserted
 * @property skipped dates that already had a plan for the same template
 *   (unique index on template_id + planned_date), i.e. nothing was inserted
 */
data class BatchScheduleResult(
    val created: List<PlannedWorkout>,
    val skipped: List<LocalDate>,
) {
    val createdCount: Int get() = created.size
    val skippedCount: Int get() = skipped.size
}

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

    /**
     * Atomically schedules one template on multiple dates (single transaction,
     * see [PlannedWorkoutDao.insertAllIgnore]). Duplicate dates within the input
     * are collapsed, and dates that already carry a plan for this template are
     * reported in [BatchScheduleResult.skipped] instead of failing the batch.
     */
    @Transaction
    suspend fun createMany(
        templateId: Long,
        plannedDates: List<LocalDate>,
        note: String? = null,
    ): BatchScheduleResult {
        if (plannedDates.isEmpty()) return BatchScheduleResult(created = emptyList(), skipped = emptyList())

        val entities = plannedDates.distinct().sorted().map { date ->
            PlannedWorkoutEntity(
                templateId = templateId,
                plannedDate = date.toEpochDay(),
                note = note,
            )
        }
        val ids = dao.insertAllIgnore(entities)

        val created = mutableListOf<PlannedWorkout>()
        val skipped = mutableListOf<LocalDate>()
        entities.zip(ids.toList()).forEach { (entity, id) ->
            if (id != -1L) {
                created.add(entity.toDomain(id))
            } else {
                skipped.add(LocalDate.ofEpochDay(entity.plannedDate))
            }
        }
        return BatchScheduleResult(created = created, skipped = skipped)
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

    private fun PlannedWorkoutEntity.toDomain() = toDomain(id)

    private fun PlannedWorkoutEntity.toDomain(domainId: Long) = PlannedWorkout(
        id = domainId,
        templateId = templateId,
        plannedDate = LocalDate.ofEpochDay(plannedDate),
        note = note,
        createdAt = createdAt,
    )
}
