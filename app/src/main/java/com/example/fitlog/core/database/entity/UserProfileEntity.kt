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
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
