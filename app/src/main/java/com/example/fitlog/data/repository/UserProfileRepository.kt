package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.UserProfileDao
import com.example.fitlog.core.database.entity.UserProfileEntity
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
    )
}
