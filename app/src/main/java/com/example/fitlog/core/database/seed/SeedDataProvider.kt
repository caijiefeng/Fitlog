package com.example.fitlog.core.database.seed

import com.example.fitlog.core.database.dao.ExerciseCategoryDao
import com.example.fitlog.core.database.dao.ExerciseDao
import com.example.fitlog.core.database.entity.ExerciseCategoryEntity
import com.example.fitlog.core.database.entity.ExerciseEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedDataProvider @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val categoryDao: ExerciseCategoryDao,
) {

    /**
     * Inserts built-in exercises. Idempotent: skips if exercises already exist.
     */
    suspend fun seedIfEmpty() {
        val count = exerciseDao.count()
        if (count > 0) return

        exerciseDao.insertAll(BUILT_IN_EXERCISES)
    }

    companion object {
        val BUILT_IN_EXERCISES = listOf(
            // Chest
            ExerciseEntity(name = "杠铃卧推", primaryMuscleGroup = "CHEST", sortOrder = 0),
            ExerciseEntity(name = "上斜哑铃卧推", primaryMuscleGroup = "CHEST", sortOrder = 1),
            ExerciseEntity(name = "下斜杠铃卧推", primaryMuscleGroup = "CHEST", sortOrder = 2),
            ExerciseEntity(name = "器械夹胸", primaryMuscleGroup = "CHEST", sortOrder = 3),
            ExerciseEntity(name = "俯卧撑", primaryMuscleGroup = "CHEST", sortOrder = 4),
            ExerciseEntity(name = "哑铃飞鸟", primaryMuscleGroup = "CHEST", sortOrder = 5),
            // Back
            ExerciseEntity(name = "引体向上", primaryMuscleGroup = "BACK", sortOrder = 10),
            ExerciseEntity(name = "高位下拉", primaryMuscleGroup = "BACK", sortOrder = 11),
            ExerciseEntity(name = "杠铃划船", primaryMuscleGroup = "BACK", sortOrder = 12),
            ExerciseEntity(name = "坐姿划船", primaryMuscleGroup = "BACK", sortOrder = 13),
            ExerciseEntity(name = "单臂哑铃划船", primaryMuscleGroup = "BACK", sortOrder = 14),
            ExerciseEntity(name = "硬拉", primaryMuscleGroup = "BACK", sortOrder = 15),
            // Shoulders
            ExerciseEntity(name = "杠铃推举", primaryMuscleGroup = "SHOULDERS", sortOrder = 20),
            ExerciseEntity(name = "哑铃推举", primaryMuscleGroup = "SHOULDERS", sortOrder = 21),
            ExerciseEntity(name = "哑铃侧平举", primaryMuscleGroup = "SHOULDERS", sortOrder = 22),
            ExerciseEntity(name = "反向飞鸟", primaryMuscleGroup = "SHOULDERS", sortOrder = 23),
            ExerciseEntity(name = "前平举", primaryMuscleGroup = "SHOULDERS", sortOrder = 24),
            // Biceps
            ExerciseEntity(name = "杠铃弯举", primaryMuscleGroup = "BICEPS", sortOrder = 30),
            ExerciseEntity(name = "哑铃弯举", primaryMuscleGroup = "BICEPS", sortOrder = 31),
            ExerciseEntity(name = "锤式弯举", primaryMuscleGroup = "BICEPS", sortOrder = 32),
            // Triceps
            ExerciseEntity(name = "绳索下压", primaryMuscleGroup = "TRICEPS", sortOrder = 40),
            ExerciseEntity(name = "双杠臂屈伸", primaryMuscleGroup = "TRICEPS", sortOrder = 41),
            ExerciseEntity(name = "窄距卧推", primaryMuscleGroup = "TRICEPS", sortOrder = 42),
            ExerciseEntity(name = "法式弯举", primaryMuscleGroup = "TRICEPS", sortOrder = 43),
            // Quadriceps
            ExerciseEntity(name = "杠铃深蹲", primaryMuscleGroup = "QUADRICEPS", sortOrder = 50),
            ExerciseEntity(name = "腿举", primaryMuscleGroup = "QUADRICEPS", sortOrder = 51),
            ExerciseEntity(name = "腿屈伸", primaryMuscleGroup = "QUADRICEPS", sortOrder = 52),
            ExerciseEntity(name = "前蹲", primaryMuscleGroup = "QUADRICEPS", sortOrder = 53),
            ExerciseEntity(name = "保加利亚分腿蹲", primaryMuscleGroup = "QUADRICEPS", sortOrder = 54),
            // Hamstrings
            ExerciseEntity(name = "罗马尼亚硬拉", primaryMuscleGroup = "HAMSTRINGS", sortOrder = 60),
            ExerciseEntity(name = "腿弯举", primaryMuscleGroup = "HAMSTRINGS", sortOrder = 61),
            ExerciseEntity(name = "北欧弯举", primaryMuscleGroup = "HAMSTRINGS", sortOrder = 62),
            // Glutes
            ExerciseEntity(name = "臀推", primaryMuscleGroup = "GLUTES", sortOrder = 70),
            ExerciseEntity(name = "臀桥", primaryMuscleGroup = "GLUTES", sortOrder = 71),
            ExerciseEntity(name = "壶铃摆荡", primaryMuscleGroup = "GLUTES", sortOrder = 72),
            // Calves
            ExerciseEntity(name = "站姿提踵", primaryMuscleGroup = "CALVES", sortOrder = 80),
            ExerciseEntity(name = "坐姿提踵", primaryMuscleGroup = "CALVES", sortOrder = 81),
            // Core
            ExerciseEntity(name = "平板支撑", primaryMuscleGroup = "CORE", sortOrder = 90),
            ExerciseEntity(name = "卷腹", primaryMuscleGroup = "CORE", sortOrder = 91),
            ExerciseEntity(name = "悬垂举腿", primaryMuscleGroup = "CORE", sortOrder = 92),
            ExerciseEntity(name = "俄罗斯转体", primaryMuscleGroup = "CORE", sortOrder = 93),
            // Cardio
            ExerciseEntity(name = "跑步", primaryMuscleGroup = "CARDIO", sortOrder = 100),
            ExerciseEntity(name = "椭圆机", primaryMuscleGroup = "CARDIO", sortOrder = 101),
            ExerciseEntity(name = "自行车", primaryMuscleGroup = "CARDIO", sortOrder = 102),
            ExerciseEntity(name = "划船机", primaryMuscleGroup = "CARDIO", sortOrder = 103),
            ExerciseEntity(name = "跳绳", primaryMuscleGroup = "CARDIO", sortOrder = 104),
        )
    }
}
