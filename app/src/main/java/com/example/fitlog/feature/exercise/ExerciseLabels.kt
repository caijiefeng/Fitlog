package com.example.fitlog.feature.exercise

import com.example.fitlog.core.model.EquipmentType
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.MuscleGroup
import com.example.fitlog.core.model.TrackingType

// 标签助手（中文硬编码为既有应用惯例，供列表/详情/选择器共享）

fun muscleGroupLabel(group: MuscleGroup): String = when (group) {
    MuscleGroup.CHEST -> "胸"
    MuscleGroup.BACK -> "背"
    MuscleGroup.SHOULDERS -> "肩"
    MuscleGroup.BICEPS -> "肱二头肌"
    MuscleGroup.TRICEPS -> "肱三头肌"
    MuscleGroup.FOREARMS -> "前臂"
    MuscleGroup.QUADRICEPS -> "股四头肌"
    MuscleGroup.HAMSTRINGS -> "腘绳肌"
    MuscleGroup.GLUTES -> "臀"
    MuscleGroup.CALVES -> "小腿"
    MuscleGroup.CORE -> "核心"
    MuscleGroup.CARDIO -> "有氧"
    MuscleGroup.FULL_BODY -> "全身"
}

fun muscleGroupFullLabel(group: MuscleGroup): String = when (group) {
    MuscleGroup.CHEST -> "胸部"
    MuscleGroup.BACK -> "背部"
    MuscleGroup.SHOULDERS -> "肩部"
    MuscleGroup.BICEPS -> "肱二头肌"
    MuscleGroup.TRICEPS -> "肱三头肌"
    MuscleGroup.FOREARMS -> "前臂"
    MuscleGroup.QUADRICEPS -> "股四头肌"
    MuscleGroup.HAMSTRINGS -> "腘绳肌"
    MuscleGroup.GLUTES -> "臀部"
    MuscleGroup.CALVES -> "小腿"
    MuscleGroup.CORE -> "核心"
    MuscleGroup.CARDIO -> "有氧"
    MuscleGroup.FULL_BODY -> "全身"
}

fun equipmentLabel(type: EquipmentType): String = when (type) {
    EquipmentType.BARBELL -> "杠铃"
    EquipmentType.DUMBBELL -> "哑铃"
    EquipmentType.MACHINE -> "器械"
    EquipmentType.CABLE -> "绳索"
    EquipmentType.BODYWEIGHT -> "自重"
    EquipmentType.KETTLEBELL -> "壶铃"
    EquipmentType.CARDIO_MACHINE -> "有氧"
    EquipmentType.OTHER -> "其他"
}

fun trackingLabel(type: TrackingType): String = when (type) {
    TrackingType.WEIGHT_REPS -> "重量×次数"
    TrackingType.BODYWEIGHT_REPS -> "自重次数"
    TrackingType.DURATION -> "计时"
    TrackingType.DISTANCE_DURATION -> "距离/时长"
}

/** 表单页使用的别名 */
fun equipmentTypeLabel(type: EquipmentType): String = when (type) {
    EquipmentType.BARBELL -> "杠铃"
    EquipmentType.DUMBBELL -> "哑铃"
    EquipmentType.MACHINE -> "器械"
    EquipmentType.CABLE -> "绳索"
    EquipmentType.BODYWEIGHT -> "自重"
    EquipmentType.KETTLEBELL -> "壶铃"
    EquipmentType.CARDIO_MACHINE -> "有氧器械"
    EquipmentType.OTHER -> "其他"
}

/** 表单页使用的别名 */
fun trackingTypeLabel(type: TrackingType): String = when (type) {
    TrackingType.WEIGHT_REPS -> "重量 × 次数"
    TrackingType.BODYWEIGHT_REPS -> "自重次数 (可加附加重量)"
    TrackingType.DURATION -> "计时"
    TrackingType.DISTANCE_DURATION -> "距离 / 时长"
}

/** 动作列表副标题：器械 · 肌群 · 记录方式（· 自定义） */
fun exerciseSubtitle(exercise: Exercise): String {
    val muscleLabel = muscleGroupLabel(exercise.primaryMuscleGroup)
    val customTag = if (exercise.isCustom) " · 自定义" else ""
    return when (exercise.equipmentType) {
        EquipmentType.BARBELL, EquipmentType.DUMBBELL,
        EquipmentType.MACHINE, EquipmentType.CABLE,
        EquipmentType.KETTLEBELL -> {
            "${equipmentLabel(exercise.equipmentType)} · $muscleLabel · ${trackingLabel(exercise.trackingType)}$customTag"
        }
        EquipmentType.BODYWEIGHT -> {
            when (exercise.trackingType) {
                TrackingType.BODYWEIGHT_REPS -> "自重次数 + 附加重量(可选)$customTag"
                TrackingType.DURATION -> "计时 · 保持姿势$customTag"
                else -> "自重 · $muscleLabel$customTag"
            }
        }
        EquipmentType.CARDIO_MACHINE -> {
            when (exercise.trackingType) {
                TrackingType.DISTANCE_DURATION -> "有氧 · 距离/时长$customTag"
                TrackingType.DURATION -> "有氧 · 计时$customTag"
                else -> "有氧 · $muscleLabel$customTag"
            }
        }
        EquipmentType.OTHER -> "$muscleLabel$customTag"
    }
}
