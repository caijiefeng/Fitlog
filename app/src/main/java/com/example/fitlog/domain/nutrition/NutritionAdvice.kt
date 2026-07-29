package com.example.fitlog.domain.nutrition

data class NutritionAdvice(
    val dailyTargetText: String,
    val macroBreakdown: MacroBreakdown,
)

data class MacroBreakdown(
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val proteinCalories: Int,
    val carbsCalories: Int,
    val fatCalories: Int,
    val totalCalories: Int,
)
