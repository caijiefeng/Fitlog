package com.example.fitlog.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "gender") val gender: String,  // MALE / FEMALE / OTHER
    @ColumnInfo(name = "birthday") val birthday: Long,  // epochDay
    @ColumnInfo(name = "height_cm") val heightCm: Double? = null,
    @ColumnInfo(name = "activity_level") val activityLevel: String,
    @ColumnInfo(name = "goal_type") val goalType: String,
    @ColumnInfo(name = "target_body_fat") val targetBodyFat: Double? = null,
    // Avatar: how the avatar is sourced, plus the payload for BUILT_IN (key) and CUSTOM (path).
    @ColumnInfo(name = "avatar_type") val avatarType: String = "DEFAULT",
    @ColumnInfo(name = "avatar_key") val avatarKey: String? = null,
    @ColumnInfo(name = "custom_avatar_path") val customAvatarPath: String? = null,
    @ColumnInfo(name = "display_name") val displayName: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
