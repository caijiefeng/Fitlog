package com.example.fitlog.data.repository

import androidx.room.withTransaction
import com.example.fitlog.core.database.FitLogDatabase
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
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

class InvalidSetDataException(message: String) : Exception(message)
class WorkoutInProgressException(val existingSessionId: Long) :
    Exception("已有进行中训练")

@Singleton
class WorkoutSessionRepository @Inject constructor(
    private val db: FitLogDatabase,
    private val sessionDao: WorkoutSessionDao,
    private val exerciseSessionDao: ExerciseSessionDao,
    private val setRecordDao: SetRecordDao,
    private val templateDao: WorkoutTemplateDao,
    private val exerciseDao: ExerciseDao,
) {

    // ── Create (atomic) ─────────────────────────────────────────────────────

    suspend fun createFromTemplate(templateId: Long, date: LocalDate = LocalDate.now()): Long {
        return db.withTransaction {
            val existing = sessionDao.getInProgress()
            if (existing != null) throw WorkoutInProgressException(existing.id)

            val template = templateDao.getByIdWithExercises(templateId)
                ?: throw IllegalStateException("模板不存在或已停用")
            if (template.exercises.isEmpty()) throw IllegalStateException("模板没有动作")

            val sessionId = sessionDao.insert(WorkoutSessionEntity(
                templateId = templateId, templateNameSnapshot = template.template.name,
                date = date.toEpochDay(), startTime = System.currentTimeMillis(),
                status = WorkoutStatus.IN_PROGRESS.name,
            ))
            template.exercises.forEachIndexed { index, te ->
                val ex = exerciseDao.getById(te.exerciseId)
                val esId = exerciseSessionDao.insert(ExerciseSessionEntity(
                    sessionId = sessionId, exerciseId = te.exerciseId,
                    exerciseNameSnapshot = ex?.name ?: "(已删除)",
                    primaryMuscleGroupSnapshot = ex?.primaryMuscleGroup ?: "FULL_BODY",
                    targetSets = te.targetSets, targetRepsMin = te.targetRepsMin,
                    targetRepsMax = te.targetRepsMax, targetWeightKg = te.targetWeightKg,
                    targetRpe = te.targetRpe, targetRir = te.targetRir,
                    plannedRestSeconds = te.restSeconds, notes = te.notes, sortOrder = index,
                ))
                repeat(te.targetSets) { setNum ->
                    setRecordDao.insert(SetRecordEntity(
                        exerciseSessionId = esId, setNumber = setNum + 1,
                        setType = "WORKING", completed = false,
                    ))
                }
            }
            sessionId
        }
    }

    suspend fun createQuick(date: LocalDate = LocalDate.now()): Long {
        return db.withTransaction {
            val existing = sessionDao.getInProgress()
            if (existing != null) throw WorkoutInProgressException(existing.id)
            sessionDao.insert(WorkoutSessionEntity(
                date = date.toEpochDay(), startTime = System.currentTimeMillis(),
                status = WorkoutStatus.IN_PROGRESS.name, templateNameSnapshot = "快速训练",
            ))
        }
    }

    // ── Read ────────────────────────────────────────────────────────────────

    suspend fun getInProgress(): DomainWorkoutSession? = sessionDao.getInProgress()?.toDomain()
    fun observeInProgress(): Flow<DomainWorkoutSession?> = sessionDao.observeInProgress().map { it?.toDomain() }
    suspend fun getById(id: Long): DomainWorkoutSession? = sessionDao.getById(id)?.toDomain()

    suspend fun getDetail(id: Long): WorkoutSessionDetail? {
        val sw = sessionDao.getByIdWithExercises(id) ?: return null
        val details = sw.exercises.map { es ->
            val sets = setRecordDao.getByExerciseSession(es.id)
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
        return WorkoutSessionDetail(session = sw.session.toDomain(), exercises = details)
    }

    fun getHistory(): Flow<List<DomainWorkoutSession>> = sessionDao.getHistory().map { l -> l.map { it.toDomain() } }

    // ── Set Operations ──────────────────────────────────────────────────────

    suspend fun completeSet(
        setRecordId: Long, reps: Int?, weightKg: Double?, rpe: Double?, rir: Int?,
        setType: String = "WORKING",
    ) {
        validateSetData(reps, weightKg, rpe, rir)
        val s = setRecordDao.getById(setRecordId) ?: throw IllegalStateException("Set不存在")
        setRecordDao.update(s.copy(
            reps = reps, weightKg = weightKg, rpe = rpe?.coerceIn(1.0, 10.0),
            rir = rir?.coerceIn(0, 5), setType = setType,
            completed = true, updatedAt = System.currentTimeMillis(),
        ))
    }

    suspend fun updateSetRecord(
        setRecordId: Long, reps: Int?, weightKg: Double?, rpe: Double?, rir: Int?,
    ) {
        validateSetData(reps, weightKg, rpe, rir)
        val s = setRecordDao.getById(setRecordId) ?: return
        setRecordDao.update(s.copy(
            reps = reps, weightKg = weightKg, rpe = rpe?.coerceIn(1.0, 10.0),
            rir = rir?.coerceIn(0, 5), updatedAt = System.currentTimeMillis(),
        ))
    }

    suspend fun updateSetType(setRecordId: Long, setType: String) {
        val s = setRecordDao.getById(setRecordId) ?: return
        setRecordDao.update(s.copy(setType = setType, updatedAt = System.currentTimeMillis()))
    }

    suspend fun addSet(exerciseSessionId: Long): Long {
        val sets = setRecordDao.getByExerciseSession(exerciseSessionId)
        val nextNum = (sets.maxOfOrNull { it.setNumber } ?: 0) + 1
        return setRecordDao.insert(SetRecordEntity(
            exerciseSessionId = exerciseSessionId, setNumber = nextNum,
            setType = "WORKING", completed = false,
        ))
    }

    suspend fun deleteIncompleteSet(setRecordId: Long) {
        setRecordDao.deleteIfIncomplete(setRecordId)
    }

    suspend fun addExerciseToQuickWorkout(sessionId: Long, exerciseId: Long) {
        val ex = exerciseDao.getById(exerciseId) ?: throw IllegalStateException("动作不存在")
        val esId = exerciseSessionDao.insert(ExerciseSessionEntity(
            sessionId = sessionId, exerciseId = exerciseId,
            exerciseNameSnapshot = ex.name,
            primaryMuscleGroupSnapshot = ex.primaryMuscleGroup,
            targetSets = 3, plannedRestSeconds = 90,
            sortOrder = (exerciseSessionDao.getBySession(sessionId).size),
        ))
        repeat(3) { setNum ->
            setRecordDao.insert(SetRecordEntity(
                exerciseSessionId = esId, setNumber = setNum + 1,
                setType = "WORKING", completed = false,
            ))
        }
    }

    suspend fun skipExercise(exerciseSessionId: Long) {
        exerciseSessionDao.setSkipped(exerciseSessionId, true)
    }

    suspend fun updateExerciseNotes(exerciseSessionId: Long, notes: String?) {
        exerciseSessionDao.updateNotes(exerciseSessionId, notes)
    }

    // ── Status ──────────────────────────────────────────────────────────────

    suspend fun updateStatus(id: Long, status: WorkoutStatus) {
        val s = sessionDao.getById(id) ?: return
        val isTerminal = status in listOf(WorkoutStatus.COMPLETED, WorkoutStatus.PARTIALLY_COMPLETED, WorkoutStatus.CANCELLED)
        sessionDao.update(s.copy(
            status = status.name, updatedAt = System.currentTimeMillis(),
            endTime = if (isTerminal) System.currentTimeMillis() else s.endTime,
            activeRestStartedAt = if (isTerminal) null else s.activeRestStartedAt,
            activeRestDurationSeconds = if (isTerminal) null else s.activeRestDurationSeconds,
            activeRestSetRecordId = if (isTerminal) null else s.activeRestSetRecordId,
        ))
    }

    suspend fun updateRestState(sessionId: Long, startedAt: Long?, durationSeconds: Int?, setRecordId: Long?) {
        val s = sessionDao.getById(sessionId) ?: return
        sessionDao.update(s.copy(
            activeRestStartedAt = startedAt, activeRestDurationSeconds = durationSeconds,
            activeRestSetRecordId = setRecordId, updatedAt = System.currentTimeMillis(),
        ))
    }

    suspend fun getRestState(sessionId: Long): Triple<Long?, Int?, Long?> {
        val s = sessionDao.getById(sessionId) ?: return Triple(null, null, null)
        return Triple(s.activeRestStartedAt, s.activeRestDurationSeconds, s.activeRestSetRecordId)
    }

    // ── Stats ───────────────────────────────────────────────────────────────

    suspend fun totalVolume(sessionId: Long): Double = setRecordDao.totalVolumeForSession(sessionId) ?: 0.0
    suspend fun completedSetCount(sessionId: Long): Int = setRecordDao.completedSetCountForSession(sessionId)
    suspend fun completedExerciseCount(sessionId: Long): Int {
        val exercises = exerciseSessionDao.getBySession(sessionId)
        return exercises.count { es ->
            setRecordDao.completedSetCount(es.id) > 0
        }
    }

    // ── Validation ──────────────────────────────────────────────────────────

    private fun validateSetData(reps: Int?, weightKg: Double?, rpe: Double?, rir: Int?) {
        if (reps != null && reps < 0) throw InvalidSetDataException("次数不能为负数")
        if (weightKg != null && weightKg < 0) throw InvalidSetDataException("重量不能为负数")
        if (rpe != null && (rpe < 1.0 || rpe > 10.0)) throw InvalidSetDataException("RPE范围1.0-10.0")
        if (rir != null && (rir < 0 || rir > 5)) throw InvalidSetDataException("RIR范围0-5")
    }

    // ── Mapping ─────────────────────────────────────────────────────────────

    private fun WorkoutSessionEntity.toDomain() = DomainWorkoutSession(
        id = id, scheduleId = scheduleId, templateId = templateId,
        templateNameSnapshot = templateNameSnapshot, date = LocalDate.ofEpochDay(date),
        startTime = Instant.ofEpochMilli(startTime), endTime = endTime?.let { Instant.ofEpochMilli(it) },
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
