package com.example.fitlog.domain.nutrition

data class NutritionTargets(
    val bmr: Int,
    val tdee: Int,
    val targetCalories: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int,
) {
    val proteinCalories: Int get() = proteinG * 4
    val fatCalories: Int get() = fatG * 9
    val carbsCalories: Int get() = carbsG * 4
    val totalMacroCalories: Int get() = proteinCalories + fatCalories + carbsCalories
}
