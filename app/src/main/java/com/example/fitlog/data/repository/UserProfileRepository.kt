package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.UserProfileDao
import com.example.fitlog.core.database.entity.UserProfileEntity
import com.example.fitlog.domain.avatar.AvatarType
import com.example.fitlog.domain.body.ActivityLevel
import com.example.fitlog.domain.body.GoalType
import com.example.fitlog.domain.body.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val userProfileDao: UserProfileDao,
) {

    suspend fun saveProfile(profile: UserProfile): UserProfile {
        val existing = userProfileDao.get()
        val entity = if (existing != null) {
            existing.copy(
                gender = profile.gender,
                birthday = profile.birthday.toEpochDay(),
                heightCm = profile.heightCm,
                activityLevel = profile.activityLevel.name,
                goalType = profile.goalType.name,
                targetBodyFat = profile.targetBodyFat,
                displayName = profile.displayName ?: existing.displayName,
                updatedAt = System.currentTimeMillis(),
            )
        } else {
            UserProfileEntity(
                gender = profile.gender,
                birthday = profile.birthday.toEpochDay(),
                heightCm = profile.heightCm,
                activityLevel = profile.activityLevel.name,
                goalType = profile.goalType.name,
                targetBodyFat = profile.targetBodyFat,
                displayName = profile.displayName,
            )
        }
        val id = userProfileDao.upsert(entity)
        return entity.copy(id = id).toDomain()
    }

    /**
     * Updates only the avatar fields of the user profile.
     *
     * If no profile row exists yet, a placeholder row is created so the
     * avatar survives; the rest of the profile can be completed later from
     * the body profile form.
     */
    suspend fun updateAvatar(
        avatarType: AvatarType,
        avatarKey: String?,
        customAvatarPath: String?,
    ): UserProfile {
        val existing = userProfileDao.get()
        val entity = if (existing != null) {
            existing.copy(
                avatarType = avatarType.name,
                avatarKey = avatarKey,
                customAvatarPath = customAvatarPath,
                updatedAt = System.currentTimeMillis(),
            )
        } else {
            UserProfileEntity(
                gender = "OTHER",
                birthday = LocalDate.of(2000, 1, 1).toEpochDay(),
                activityLevel = ActivityLevel.SEDENTARY.name,
                goalType = GoalType.MAINTAIN.name,
                avatarType = avatarType.name,
                avatarKey = avatarKey,
                customAvatarPath = customAvatarPath,
            )
        }
        val id = userProfileDao.upsert(entity)
        return entity.copy(id = id).toDomain()
    }

    suspend fun updateDisplayName(displayName: String): UserProfile {
        val normalizedName = displayName.trim().take(24).ifBlank { null }
        val existing = userProfileDao.get()
        val entity = if (existing != null) {
            existing.copy(
                displayName = normalizedName,
                updatedAt = System.currentTimeMillis(),
            )
        } else {
            UserProfileEntity(
                gender = "OTHER",
                birthday = LocalDate.of(2000, 1, 1).toEpochDay(),
                activityLevel = ActivityLevel.SEDENTARY.name,
                goalType = GoalType.MAINTAIN.name,
                displayName = normalizedName,
            )
        }
        val id = userProfileDao.upsert(entity)
        return entity.copy(id = id).toDomain()
    }

    suspend fun get(): UserProfile? {
        return userProfileDao.get()?.toDomain()
    }

    fun observe(): Flow<UserProfile?> {
        return userProfileDao.observe().map { it?.toDomain() }
    }

    private fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
        id = id,
        gender = gender,
        birthday = LocalDate.ofEpochDay(birthday),
        heightCm = heightCm,
        activityLevel = ActivityLevel.valueOf(activityLevel),
        goalType = GoalType.valueOf(goalType),
        targetBodyFat = targetBodyFat,
        avatarType = runCatching { AvatarType.valueOf(avatarType) }
            .getOrDefault(AvatarType.DEFAULT),
        avatarKey = avatarKey,
        customAvatarPath = customAvatarPath,
        displayName = displayName,
    )
}
