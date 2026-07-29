package com.example.fitlog.domain.body

data class BodyGoalPlan(
    val currentWeightKg: Double,
    val targetWeightKg: Double,
    val weightDifferenceKg: Double,
    val fatToLoseKg: Double,
    val estimatedWeeks: Int,
    val recommendedDailyCalories: Int,
)
