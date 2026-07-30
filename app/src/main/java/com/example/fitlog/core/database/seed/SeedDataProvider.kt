package com.example.fitlog.core.database.seed

import com.example.fitlog.core.database.dao.ExerciseCategoryDao
import com.example.fitlog.core.database.dao.ExerciseDao
import com.example.fitlog.core.database.entity.ExerciseEntity
import javax.inject.Inject
import javax.inject.Singleton

data class BuiltInExerciseDef(
    val builtInKey: String,
    val name: String,
    val primaryMuscleGroup: String,
    val sortOrder: Int,
    val equipmentType: String = "OTHER",
    val trackingType: String = "WEIGHT_REPS",
)

@Singleton
class SeedDataProvider @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val categoryDao: ExerciseCategoryDao,
) {

    /**
     * Syncs built-in exercises using [builtInKey] as the stable identifier.
     * - New exercises are inserted.
     * - Existing exercises are updated (name, muscle group, equipment, tracking).
     * - Built-in exercises no longer in the seed list are set inactive.
     * - Custom exercises (built_in_key is null) are never touched.
     */
    suspend fun syncBuiltInExercises() {
        val allBuiltInDb = exerciseDao.getAllBuiltIn()
        val keyToDb = allBuiltInDb.filter { it.builtInKey != null }.associateBy { it.builtInKey!! }

        val currentKeys = mutableSetOf<String>()

        for (def in BUILT_IN_DEFS) {
            currentKeys.add(def.builtInKey)
            val existing = keyToDb[def.builtInKey]
            if (existing != null) {
                // Update existing
                exerciseDao.update(existing.copy(
                    name = def.name,
                    primaryMuscleGroup = def.primaryMuscleGroup,
                    sortOrder = def.sortOrder,
                    equipmentType = def.equipmentType,
                    trackingType = def.trackingType,
                    isActive = true,
                    updatedAt = System.currentTimeMillis(),
                ))
            } else {
                // Insert new
                exerciseDao.insert(ExerciseEntity(
                    name = def.name,
                    primaryMuscleGroup = def.primaryMuscleGroup,
                    sortOrder = def.sortOrder,
                    builtInKey = def.builtInKey,
                    equipmentType = def.equipmentType,
                    trackingType = def.trackingType,
                    isActive = true,
                ))
            }
        }

        // Deactivate built-in exercises that are no longer in the seed list
        for (existing in allBuiltInDb) {
            val key = existing.builtInKey
            if (key != null && key !in currentKeys && existing.isActive) {
                exerciseDao.update(existing.copy(
                    isActive = false,
                    updatedAt = System.currentTimeMillis(),
                ))
            }
        }
    }

    companion object {
        val BUILT_IN_DEFS = listOf(
            // Chest
            BuiltInExerciseDef("barbell_bench_press", "杠铃卧推", "CHEST", 0, "BARBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("dumbbell_incline_bench_press", "上斜哑铃卧推", "CHEST", 1, "DUMBBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("machine_chest_fly", "器械夹胸", "CHEST", 2, "MACHINE", "WEIGHT_REPS"),
            BuiltInExerciseDef("push_up", "俯卧撑", "CHEST", 4, "BODYWEIGHT", "BODYWEIGHT_REPS"),
            BuiltInExerciseDef("dumbbell_fly", "哑铃飞鸟", "CHEST", 5, "DUMBBELL", "WEIGHT_REPS"),

            // Back
            BuiltInExerciseDef("pull_up", "引体向上", "BACK", 10, "BODYWEIGHT", "BODYWEIGHT_REPS"),
            BuiltInExerciseDef("lat_pulldown", "高位下拉", "BACK", 11, "MACHINE", "WEIGHT_REPS"),
            BuiltInExerciseDef("barbell_row", "杠铃划船", "BACK", 12, "BARBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("seated_cable_row", "坐姿划船", "BACK", 13, "CABLE", "WEIGHT_REPS"),
            BuiltInExerciseDef("dumbbell_one_arm_row", "单臂哑铃划船", "BACK", 14, "DUMBBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("deadlift", "硬拉", "BACK", 15, "BARBELL", "WEIGHT_REPS"),

            // Shoulders — original
            BuiltInExerciseDef("barbell_overhead_press", "杠铃推举", "SHOULDERS", 20, "BARBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("dumbbell_overhead_press", "哑铃推举", "SHOULDERS", 21, "DUMBBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("dumbbell_lateral_raise", "哑铃侧平举", "SHOULDERS", 22, "DUMBBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("reverse_pec_deck_fly", "反向飞鸟", "SHOULDERS", 23, "MACHINE", "WEIGHT_REPS"),
            BuiltInExerciseDef("front_raise", "前平举", "SHOULDERS", 24, "DUMBBELL", "WEIGHT_REPS"),

            // Shoulders — new additions
            BuiltInExerciseDef("barbell_shoulder_press", "杠铃肩上推举", "SHOULDERS", 25, "BARBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("dumbbell_shoulder_press", "哑铃肩上推举", "SHOULDERS", 26, "DUMBBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("arnold_press", "阿诺德推举", "SHOULDERS", 27, "DUMBBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("dumbbell_front_raise", "哑铃前平举", "SHOULDERS", 28, "DUMBBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("cable_lateral_raise", "绳索侧平举", "SHOULDERS", 29, "CABLE", "WEIGHT_REPS"),
            BuiltInExerciseDef("machine_lateral_raise", "器械侧平举", "SHOULDERS", 30, "MACHINE", "WEIGHT_REPS"),
            BuiltInExerciseDef("reverse_pec_deck", "反向蝴蝶机", "SHOULDERS", 31, "MACHINE", "WEIGHT_REPS"),
            BuiltInExerciseDef("cable_face_pull", "绳索面拉", "SHOULDERS", 32, "CABLE", "WEIGHT_REPS"),
            BuiltInExerciseDef("bent_over_dumbbell_reverse_fly", "俯身哑铃反向飞鸟", "SHOULDERS", 33, "DUMBBELL", "WEIGHT_REPS"),

            // Biceps
            BuiltInExerciseDef("barbell_curl", "杠铃弯举", "BICEPS", 40, "BARBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("dumbbell_curl", "哑铃弯举", "BICEPS", 41, "DUMBBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("hammer_curl", "锤式弯举", "BICEPS", 42, "DUMBBELL", "WEIGHT_REPS"),

            // Triceps
            BuiltInExerciseDef("cable_triceps_pushdown", "绳索下压", "TRICEPS", 50, "CABLE", "WEIGHT_REPS"),
            BuiltInExerciseDef("dip", "双杠臂屈伸", "TRICEPS", 51, "BODYWEIGHT", "BODYWEIGHT_REPS"),
            BuiltInExerciseDef("close_grip_bench_press", "窄距卧推", "TRICEPS", 52, "BARBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("french_press", "法式弯举", "TRICEPS", 53, "DUMBBELL", "WEIGHT_REPS"),

            // Quadriceps
            BuiltInExerciseDef("barbell_squat", "杠铃深蹲", "QUADRICEPS", 60, "BARBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("leg_press", "腿举", "QUADRICEPS", 61, "MACHINE", "WEIGHT_REPS"),
            BuiltInExerciseDef("leg_extension", "腿屈伸", "QUADRICEPS", 62, "MACHINE", "WEIGHT_REPS"),
            BuiltInExerciseDef("front_squat", "前蹲", "QUADRICEPS", 63, "BARBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("bulgarian_split_squat", "保加利亚分腿蹲", "QUADRICEPS", 64, "DUMBBELL", "WEIGHT_REPS"),

            // Hamstrings
            BuiltInExerciseDef("romanian_deadlift", "罗马尼亚硬拉", "HAMSTRINGS", 70, "BARBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("leg_curl", "腿弯举", "HAMSTRINGS", 71, "MACHINE", "WEIGHT_REPS"),
            BuiltInExerciseDef("nordic_curl", "北欧弯举", "HAMSTRINGS", 72, "BODYWEIGHT", "BODYWEIGHT_REPS"),

            // Glutes
            BuiltInExerciseDef("hip_thrust", "臀推", "GLUTES", 80, "BARBELL", "WEIGHT_REPS"),
            BuiltInExerciseDef("glute_bridge", "臀桥", "GLUTES", 81, "BODYWEIGHT", "BODYWEIGHT_REPS"),
            BuiltInExerciseDef("kettlebell_swing", "壶铃摆荡", "GLUTES", 82, "KETTLEBELL", "WEIGHT_REPS"),

            // Calves
            BuiltInExerciseDef("standing_calf_raise", "站姿提踵", "CALVES", 90, "MACHINE", "WEIGHT_REPS"),
            BuiltInExerciseDef("seated_calf_raise", "坐姿提踵", "CALVES", 91, "MACHINE", "WEIGHT_REPS"),

            // Core
            BuiltInExerciseDef("plank", "平板支撑", "CORE", 100, "BODYWEIGHT", "DURATION"),
            BuiltInExerciseDef("crunch", "卷腹", "CORE", 101, "BODYWEIGHT", "BODYWEIGHT_REPS"),
            BuiltInExerciseDef("hanging_leg_raise", "悬垂举腿", "CORE", 102, "BODYWEIGHT", "BODYWEIGHT_REPS"),
            BuiltInExerciseDef("russian_twist", "俄罗斯转体", "CORE", 103, "BODYWEIGHT", "BODYWEIGHT_REPS"),

            // Cardio
            BuiltInExerciseDef("running", "跑步", "CARDIO", 110, "CARDIO_MACHINE", "DISTANCE_DURATION"),
            BuiltInExerciseDef("elliptical", "椭圆机", "CARDIO", 111, "CARDIO_MACHINE", "DISTANCE_DURATION"),
            BuiltInExerciseDef("cycling", "自行车", "CARDIO", 112, "CARDIO_MACHINE", "DISTANCE_DURATION"),
            BuiltInExerciseDef("rower", "划船机", "CARDIO", 113, "CARDIO_MACHINE", "DISTANCE_DURATION"),
            BuiltInExerciseDef("jump_rope", "跳绳", "CARDIO", 114, "BODYWEIGHT", "DURATION"),
        )
    }
}
