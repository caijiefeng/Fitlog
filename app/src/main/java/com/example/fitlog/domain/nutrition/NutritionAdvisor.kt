package com.example.fitlog.domain.nutrition

import com.example.fitlog.domain.body.GoalType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionAdvisor @Inject constructor() {

    /**
     * Generates personalized nutrition advice based on the user's goal and targets.
     *
     * @param goalType The user's fitness goal type (FAT_LOSS, MAINTAIN, LEAN_GAIN, MUSCLE_GAIN).
     * @param tdee The user's Total Daily Energy Expenditure.
     * @param targetCalories The recommended daily calorie target.
     * @param proteinG Recommended daily protein in grams.
     * @param carbsG Recommended daily carbs in grams.
     * @param fatG Recommended daily fat in grams.
     * @return A [NutritionAdvice] with human-readable advice and macro breakdown.
     */
    fun generateAdvice(
        goalType: GoalType,
        tdee: Int,
        targetCalories: Int,
        proteinG: Int,
        carbsG: Int,
        fatG: Int,
    ): NutritionAdvice {
        val dailyTargetText = when (goalType) {
            GoalType.FAT_LOSS -> {
                val deficit = tdee - targetCalories
                "目标每日摄入 ${targetCalories} kcal（基础代谢所耗 TDEE $tdee kcal，减少 $deficit kcal）"
            }
            GoalType.MAINTAIN ->
                "目标每日摄入 ${targetCalories} kcal（维持当前体重）"
            GoalType.LEAN_GAIN -> {
                val surplus = targetCalories - tdee
                "目标每日摄入 ${targetCalories} kcal（TDEE $tdee kcal，增加 $surplus kcal 用于瘦增肌）"
            }
            GoalType.MUSCLE_GAIN -> {
                val surplus = targetCalories - tdee
                "目标每日摄入 ${targetCalories} kcal（TDEE $tdee kcal，增加 $surplus kcal 用于增肌）"
            }
        }

        val macroBreakdown = MacroBreakdown(
            proteinGrams = proteinG,
            carbsGrams = carbsG,
            fatGrams = fatG,
            proteinCalories = proteinG * 4,
            carbsCalories = carbsG * 4,
            fatCalories = fatG * 9,
            totalCalories = proteinG * 4 + carbsG * 4 + fatG * 9,
        )

        return NutritionAdvice(
            dailyTargetText = dailyTargetText,
            macroBreakdown = macroBreakdown,
        )
    }
}
