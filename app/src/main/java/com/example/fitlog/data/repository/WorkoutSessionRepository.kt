package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.ExerciseSessionDao
import com.example.fitlog.core.database.dao.ExerciseDao
import com.example.fitlog.core.database.dao.SetRecordDao
import com.example.fitlog.core.database.dao.WorkoutSessionDao
import com.example.fitlog.core.database.dao.WorkoutTemplateDao
import com.example.fitlog.core.database.entity.ExerciseSessionEntity
import com.example.fitlog.core.database.entity.SetRecordEntity
import com.example.fitlog.core.database.entity.WorkoutSessionEntity
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.ExerciseSession as DomainExerciseSession
import com.example.fitlog.core.model.MuscleGroup
import com.example.fitlog.core.model.SetRecord as DomainSetRecord
import com.example.fitlog.core.model.SetType
import com.example.fitlog.core.model.WorkoutSession as DomainWorkoutSession
import com.example.fitlog.core.model.WorkoutSessionDetail
import com.example.fitlog.core.model.WorkoutStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutSessionRepository @Inject constructor(
    private val sessionDao: WorkoutSessionDao,
    private val exerciseSessionDao: ExerciseSessionDao,
    private val setRecordDao: SetRecordDao,
    private val templateDao: WorkoutTemplateDao,
    private val exerciseDao: ExerciseDao,
) {

    // ── Create ──────────────────────────────────────────────────────────────

    suspend fun createFromTemplate(
        templateId: Long,
        date: LocalDate = LocalDate.now(),
    ): Long {
        val template = templateDao.getByIdWithExercises(templateId)
            ?: throw IllegalStateException("模板不存在或已停用")
        if (template.exercises.isEmpty()) throw IllegalStateException("模板没有动作")

        val sessionId = sessionDao.insert(
            WorkoutSessionEntity(
                templateId = templateId,
                templateNameSnapshot = template.template.name,
                date = date.toEpochDay(),
                startTime = System.currentTimeMillis(),
                status = WorkoutStatus.IN_PROGRESS.name,
            )
        )

        template.exercises.forEachIndexed { index, te ->
            val ex = exerciseDao.getById(te.exerciseId)
            val esId = exerciseSessionDao.insert(
                ExerciseSessionEntity(
                    sessionId = sessionId,
                    exerciseId = te.exerciseId,
                    exerciseNameSnapshot = ex?.name ?: "(已删除)",
                    primaryMuscleGroupSnapshot = ex?.primaryMuscleGroup ?: "FULL_BODY",
                    targetSets = te.targetSets,
                    targetRepsMin = te.targetRepsMin,
                    targetRepsMax = te.targetRepsMax,
                    targetWeightKg = te.targetWeightKg,
                    targetRpe = te.targetRpe,
                    targetRir = te.targetRir,
                    plannedRestSeconds = te.restSeconds,
                    notes = te.notes,
                    sortOrder = index,
                )
            )
            // Create target set placeholders (uncompleted)
            repeat(te.targetSets) { setNum ->
                setRecordDao.insert(
                    SetRecordEntity(
                        exerciseSessionId = esId,
                        setNumber = setNum + 1,
                        setType = "WORKING",
                        completed = false,
                    )
                )
            }
        }

        return sessionId
    }

    suspend fun createQuick(date: LocalDate = LocalDate.now()): Long {
        return sessionDao.insert(
            WorkoutSessionEntity(
                date = date.toEpochDay(),
                startTime = System.currentTimeMillis(),
                status = WorkoutStatus.IN_PROGRESS.name,
                templateNameSnapshot = "快速训练",
            )
        )
    }

    // ── Read ────────────────────────────────────────────────────────────────

    suspend fun getInProgress(): DomainWorkoutSession? =
        sessionDao.getInProgress()?.toDomain()

    fun observeInProgress(): Flow<DomainWorkoutSession?> =
        sessionDao.observeInProgress().map { it?.toDomain() }

    suspend fun getById(id: Long): DomainWorkoutSession? =
        sessionDao.getById(id)?.toDomain()

    suspend fun getDetail(id: Long): WorkoutSessionDetail? {
        val sw = sessionDao.getByIdWithExercises(id) ?: return null
        val exerciseDetails = sw.exercises.map { es ->
            val sets = setRecordDao.getByExerciseSession(es.id)
            val ex = es.exerciseId?.let { exerciseDao.getById(it) }
            DomainExerciseSession(
                id = es.id, sessionId = es.sessionId, exerciseId = es.exerciseId,
                exerciseNameSnapshot = es.exerciseNameSnapshot,
                primaryMuscleGroupSnapshot = try { MuscleGroup.valueOf(es.primaryMuscleGroupSnapshot) } catch (_: Exception) { MuscleGroup.FULL_BODY },
                targetSets = es.targetSets, targetRepsMin = es.targetRepsMin,
                targetRepsMax = es.targetRepsMax, targetWeightKg = es.targetWeightKg,
                targetRpe = es.targetRpe, targetRir = es.targetRir,
                plannedRestSeconds = es.plannedRestSeconds, notes = es.notes,
                sortOrder = es.sortOrder, isSkipped = es.isSkipped,
            ) to sets.map { it.toDomain() }
        }
        return WorkoutSessionDetail(session = sw.session.toDomain(), exercises = exerciseDetails)
    }

    fun getHistory(): Flow<List<DomainWorkoutSession>> =
        sessionDao.getHistory().map { list -> list.map { it.toDomain() } }

    // ── Update ──────────────────────────────────────────────────────────────

    suspend fun updateStatus(id: Long, status: WorkoutStatus) {
        val s = sessionDao.getById(id) ?: return
        sessionDao.update(
            s.copy(
                status = status.name,
                endTime = if (status in listOf(WorkoutStatus.COMPLETED, WorkoutStatus.PARTIALLY_COMPLETED, WorkoutStatus.CANCELLED))
                    System.currentTimeMillis() else s.endTime,
                updatedAt = System.currentTimeMillis(),
                activeRestStartedAt = null,
                activeRestDurationSeconds = null,
                activeRestSetRecordId = null,
            )
        )
    }

    suspend fun updateRestState(sessionId: Long, startedAt: Long?, durationSeconds: Int?, setRecordId: Long?) {
        val s = sessionDao.getById(sessionId) ?: return
        sessionDao.update(s.copy(
            activeRestStartedAt = startedAt,
            activeRestDurationSeconds = durationSeconds,
            activeRestSetRecordId = setRecordId,
            updatedAt = System.currentTimeMillis(),
        ))
    }

    // ── Volume ──────────────────────────────────────────────────────────────

    suspend fun totalVolume(sessionId: Long): Double =
        setRecordDao.totalVolumeForSession(sessionId) ?: 0.0

    suspend fun completedSetCount(sessionId: Long): Int =
        setRecordDao.completedSetCountForSession(sessionId)

    // ── Mapping ─────────────────────────────────────────────────────────────

    private fun WorkoutSessionEntity.toDomain() = DomainWorkoutSession(
        id = id, scheduleId = scheduleId, templateId = templateId,
        templateNameSnapshot = templateNameSnapshot,
        date = LocalDate.ofEpochDay(date),
        startTime = Instant.ofEpochMilli(startTime),
        endTime = endTime?.let { Instant.ofEpochMilli(it) },
        status = try { WorkoutStatus.valueOf(status) } catch (_: Exception) { WorkoutStatus.IN_PROGRESS },
        notes = notes,
    )

    private fun SetRecordEntity.toDomain() = DomainSetRecord(
        id = id, exerciseSessionId = exerciseSessionId, setNumber = setNumber,
        setType = try { SetType.valueOf(setType) } catch (_: Exception) { SetType.WORKING },
        reps = reps, weightKg = weightKg, rpe = rpe, rir = rir,
        restSeconds = restSeconds, completed = completed, notes = notes,
    )
}
