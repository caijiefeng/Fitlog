package com.example.fitlog.screenshot

import com.example.fitlog.core.model.EquipmentType
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.ExerciseSession
import com.example.fitlog.core.model.MuscleGroup
import com.example.fitlog.core.model.SetRecord
import com.example.fitlog.core.model.SetType
import com.example.fitlog.core.model.TrackingType
import com.example.fitlog.core.model.WorkoutSession
import com.example.fitlog.core.model.WorkoutSessionDetail
import com.example.fitlog.core.model.WorkoutStatus
import com.example.fitlog.core.model.WorkoutTemplate
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate

/** 截图测试共享夹具：用 mockk 构造真实数据，注入各屏幕 ViewModel。 */

fun exercise(
    id: Long,
    name: String,
    muscle: MuscleGroup = MuscleGroup.CHEST,
    equipment: EquipmentType = EquipmentType.BARBELL,
    tracking: TrackingType = TrackingType.WEIGHT_REPS,
    builtInKey: String? = null,
    isCustom: Boolean = false,
): Exercise = Exercise(
    id = id,
    name = name,
    primaryMuscleGroup = muscle,
    secondaryMuscleGroup = null,
    categoryId = null,
    notes = null,
    isCustom = isCustom,
    isActive = true,
    sortOrder = id.toInt(),
    builtInKey = builtInKey,
    equipmentType = equipment,
    trackingType = tracking,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH,
)

fun exerciseList(): List<Exercise> = listOf(
    exercise(1, "杠铃卧推", MuscleGroup.CHEST, EquipmentType.BARBELL, builtInKey = "barbell_bench_press"),
    exercise(2, "上斜哑铃卧推（长名称动作示例，用于测试长文本排版）", MuscleGroup.CHEST, EquipmentType.DUMBBELL, builtInKey = "dumbbell_incline_bench_press"),
    exercise(3, "器械夹胸", MuscleGroup.CHEST, EquipmentType.MACHINE, builtInKey = "machine_chest_fly"),
    exercise(4, "俯卧撑", MuscleGroup.CHEST, EquipmentType.BODYWEIGHT, TrackingType.BODYWEIGHT_REPS, builtInKey = "push_up"),
    exercise(5, "引体向上", MuscleGroup.BACK, EquipmentType.BODYWEIGHT, TrackingType.BODYWEIGHT_REPS, builtInKey = "pull_up"),
    exercise(6, "高位下拉", MuscleGroup.BACK, EquipmentType.MACHINE, builtInKey = "lat_pulldown"),
    exercise(7, "杠铃划船", MuscleGroup.BACK, EquipmentType.BARBELL, builtInKey = "barbell_row"),
    exercise(8, "硬拉", MuscleGroup.BACK, EquipmentType.BARBELL, builtInKey = "deadlift"),
    exercise(9, "杠铃深蹲", MuscleGroup.QUADRICEPS, EquipmentType.BARBELL, builtInKey = "barbell_squat"),
    exercise(10, "腿举", MuscleGroup.QUADRICEPS, EquipmentType.MACHINE, builtInKey = "leg_press"),
    exercise(11, "哑铃侧平举", MuscleGroup.SHOULDERS, EquipmentType.DUMBBELL, builtInKey = "dumbbell_lateral_raise"),
    exercise(12, "自定义测试动作", MuscleGroup.CORE, EquipmentType.OTHER, TrackingType.DURATION, isCustom = true),
)

fun setRecord(
    id: Long,
    setNumber: Int,
    completed: Boolean,
    weightKg: Double? = null,
    reps: Int? = null,
): SetRecord = SetRecord(
    id = id,
    exerciseSessionId = 1,
    setNumber = setNumber,
    setType = SetType.WORKING,
    reps = reps,
    weightKg = weightKg,
    rpe = null,
    rir = null,
    restSeconds = null,
    completed = completed,
)

fun exerciseSession(
    id: Long,
    name: String,
    targetSets: Int = 3,
    isSkipped: Boolean = false,
    isCompleted: Boolean = false,
): ExerciseSession = ExerciseSession(
    id = id,
    sessionId = 1,
    exerciseId = id,
    exerciseNameSnapshot = name,
    primaryMuscleGroupSnapshot = MuscleGroup.CHEST,
    targetSets = targetSets,
    targetRepsMin = 8,
    targetRepsMax = 12,
    targetWeightKg = 60.0,
    targetRpe = null,
    targetRir = null,
    plannedRestSeconds = 90,
    sortOrder = id.toInt(),
    isSkipped = isSkipped,
    isCompleted = isCompleted,
    completedAt = null,
)

fun sessionDetail(
    inProgress: Boolean = true,
    exercises: List<Pair<ExerciseSession, List<SetRecord>>> = listOf(
        exerciseSession(1, "杠铃卧推") to listOf(
            setRecord(1, 1, true, 60.0, 12),
            setRecord(2, 2, true, 60.0, 10),
            setRecord(3, 3, false),
        ),
        exerciseSession(2, "上斜哑铃卧推") to listOf(
            setRecord(4, 1, false),
            setRecord(5, 2, false),
            setRecord(6, 3, false),
        ),
    ),
): WorkoutSessionDetail {
    val session = WorkoutSession(
        id = 1,
        templateNameSnapshot = "Push A",
        templateId = null,
        scheduleId = null,
        status = if (inProgress) WorkoutStatus.IN_PROGRESS else WorkoutStatus.COMPLETED,
        startTime = Instant.parse("2026-07-31T10:00:00Z"),
        endTime = if (inProgress) null else Instant.parse("2026-07-31T11:00:00Z"),
    )
    return WorkoutSessionDetail(session = session, exercises = exercises)
}

fun template(id: Long, name: String, notes: String? = null): WorkoutTemplate = WorkoutTemplate(
    id = id,
    name = name,
    notes = notes,
    sortOrder = id.toInt(),
    isActive = true,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH,
)

/** 常用 mock 仓库：无参构造 + relaxed。 */
fun mockRepo(): Any = mockk(relaxed = true)

fun <T> coEveryFlow(value: T) = flowOf(value)
