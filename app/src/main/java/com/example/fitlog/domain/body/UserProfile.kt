package com.example.fitlog.domain.body

import com.example.fitlog.domain.avatar.AvatarType
import java.time.LocalDate

data class UserProfile(
    val id: Long = 0,
    val gender: String,
    val birthday: LocalDate,
    val heightCm: Double? = null,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,
    val targetBodyFat: Double? = null,
    val avatarType: AvatarType = AvatarType.DEFAULT,
    val avatarKey: String? = null,
    val customAvatarPath: String? = null,
)
