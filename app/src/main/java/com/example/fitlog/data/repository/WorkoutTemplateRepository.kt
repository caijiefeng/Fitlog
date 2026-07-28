package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.ExerciseDao
import com.example.fitlog.core.database.dao.WorkoutTemplateDao
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateExerciseEntity
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.MuscleGroup
import com.example.fitlog.core.model.WorkoutTemplate
import com.example.fitlog.core.model.WorkoutTemplateExercise
import com.example.fitlog.core.model.WorkoutTemplateDetail
import com.example.fitlog.core.model.WorkoutTemplateExerciseDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutTemplateRepository @Inject constructor(
    private val templateDao: WorkoutTemplateDao,
    private val exerciseDao: ExerciseDao,
) {

    fun getAllActive(): Flow<List<WorkoutTemplate>> =
        templateDao.getAllActive().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): WorkoutTemplate? =
        templateDao.getById(id)?.toDomain()

    suspend fun getDetail(id: Long): WorkoutTemplateDetail? {
        val result = templateDao.getByIdWithExercises(id) ?: return null
        return result.toDetail(exerciseDao)
    }

    suspend fun create(name: String, notes: String? = null): Long {
        val entity = WorkoutTemplateEntity(name = name, notes = notes)
        return templateDao.insert(entity)
    }

    suspend fun update(template: WorkoutTemplate) {
        templateDao.update(
            WorkoutTemplateEntity(
                id = template.id,
                name = template.name,
                notes = template.notes,
                sortOrder = template.sortOrder,
                isActive = template.isActive,
                createdAt = template.createdAt.toEpochMilli(),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun softDelete(id: Long) {
        templateDao.softDelete(id)
    }

    suspend fun replaceExercises(templateId: Long, exercises: List<WorkoutTemplateExercise>) {
        templateDao.deleteTemplateExercises(templateId)
        val entities = exercises.mapIndexed { index, exercise ->
            WorkoutTemplateExerciseEntity(
                templateId = templateId,
                exerciseId = exercise.exerciseId,
                targetSets = exercise.targetSets,
                targetRepsMin = exercise.targetRepsMin,
                targetRepsMax = exercise.targetRepsMax,
                targetWeightKg = exercise.targetWeightKg,
                targetRpe = exercise.targetRpe,
                targetRir = exercise.targetRir,
                restSeconds = exercise.restSeconds,
                notes = exercise.notes,
                sortOrder = index,
            )
        }
        templateDao.insertTemplateExercises(entities)
    }

    suspend fun getExerciseCount(templateId: Long): Int =
        templateDao.exerciseCount(templateId)
}

// ── Mapping ──────────────────────────────────────────────────────────────────

private fun WorkoutTemplateEntity.toDomain() = WorkoutTemplate(
    id = id,
    name = name,
    notes = notes,
    sortOrder = sortOrder,
    isActive = isActive,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

private fun WorkoutTemplateExerciseEntity.toDomain(exercise: Exercise?) = WorkoutTemplateExercise(
    id = id,
    templateId = templateId,
    exerciseId = exerciseId,
    exerciseName = exercise?.name ?: "(unknown)",
    primaryMuscleGroup = exercise?.primaryMuscleGroup ?: MuscleGroup.FULL_BODY,
    targetSets = targetSets,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    targetWeightKg = targetWeightKg,
    targetRpe = targetRpe,
    targetRir = targetRir,
    restSeconds = restSeconds,
    notes = notes,
    sortOrder = sortOrder,
)

private suspend fun com.example.fitlog.core.database.relation.TemplateWithExercises.toDetail(
    exerciseDao: ExerciseDao,
): WorkoutTemplateDetail {
    val exercises = this.exercises.map { entity ->
        val ex = exerciseDao.getById(entity.exerciseId)
        WorkoutTemplateExerciseDetail(
            templateExercise = entity.toDomain(ex?.toDomain()),
            exercise = ex?.toDomain(),
        )
    }
    return WorkoutTemplateDetail(
        template = this.template.toDomain(),
        exercises = exercises,
    )
}

private fun com.example.fitlog.core.database.entity.ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    primaryMuscleGroup = try { MuscleGroup.valueOf(primaryMuscleGroup) } catch (_: Exception) { MuscleGroup.FULL_BODY },
    secondaryMuscleGroup = secondaryMuscleGroup?.let { try { MuscleGroup.valueOf(it) } catch (_: Exception) { null } },
    categoryId = categoryId,
    notes = notes,
    isCustom = isCustom,
    isActive = isActive,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)
