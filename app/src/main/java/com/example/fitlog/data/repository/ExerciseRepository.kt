package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.ExerciseDao
import com.example.fitlog.core.database.entity.ExerciseEntity
import com.example.fitlog.core.model.EquipmentType
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.MuscleGroup
import com.example.fitlog.core.model.TrackingType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
) {

    fun getAllActive(): Flow<List<Exercise>> =
        exerciseDao.getAllActive().map { list -> list.map { it.toDomain() } }

    fun getByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>> =
        exerciseDao.getByMuscleGroup(muscleGroup.name).map { list -> list.map { it.toDomain() } }

    fun searchByName(query: String): Flow<List<Exercise>> =
        exerciseDao.searchByName(query).map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Exercise? =
        exerciseDao.getActiveById(id)?.toDomain()

    suspend fun getByBuiltInKey(key: String): Exercise? =
        exerciseDao.getByBuiltInKey(key)?.toDomain()

    suspend fun getAllBuiltIn(): List<Exercise> =
        exerciseDao.getAllBuiltIn().map { it.toDomain() }

    suspend fun create(exercise: Exercise): Long {
        val entity = exercise.toEntity()
        return exerciseDao.insert(entity)
    }

    suspend fun update(exercise: Exercise) {
        exerciseDao.update(exercise.toEntity())
    }

    suspend fun softDelete(id: Long) {
        exerciseDao.softDelete(id)
    }

    suspend fun isNameDuplicate(name: String): Boolean =
        exerciseDao.countByName(name.trim()) > 0
}

// ── Mapping ──────────────────────────────────────────────────────────────────

private fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    primaryMuscleGroup = try { MuscleGroup.valueOf(primaryMuscleGroup) } catch (_: Exception) { MuscleGroup.FULL_BODY },
    secondaryMuscleGroup = secondaryMuscleGroup?.let { try { MuscleGroup.valueOf(it) } catch (_: Exception) { null } },
    categoryId = categoryId,
    notes = notes,
    isCustom = isCustom,
    isActive = isActive,
    sortOrder = sortOrder,
    builtInKey = builtInKey,
    equipmentType = try { EquipmentType.valueOf(equipmentType) } catch (_: Exception) { EquipmentType.OTHER },
    trackingType = try { TrackingType.valueOf(trackingType) } catch (_: Exception) { TrackingType.WEIGHT_REPS },
    createdAt = java.time.Instant.ofEpochMilli(createdAt),
    updatedAt = java.time.Instant.ofEpochMilli(updatedAt),
)

private fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    primaryMuscleGroup = primaryMuscleGroup.name,
    secondaryMuscleGroup = secondaryMuscleGroup?.name,
    categoryId = categoryId,
    notes = notes,
    isCustom = isCustom,
    isActive = isActive,
    sortOrder = sortOrder,
    builtInKey = builtInKey,
    equipmentType = equipmentType.name,
    trackingType = trackingType.name,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)
