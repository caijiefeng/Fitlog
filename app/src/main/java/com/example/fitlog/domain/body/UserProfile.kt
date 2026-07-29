package com.example.fitlog.domain.body

import java.time.LocalDate

data class UserProfile(
    val id: Long = 0,
    val gender: String,
    val birthday: LocalDate,
    val heightCm: Double? = null,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,
    val targetBodyFat: Double? = null,
)
