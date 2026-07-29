package com.example.fitlog.domain.nutrition

data class EnergySummary(
    val bmr: Int,
    val tdee: Int,
    val targetCalories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
)
