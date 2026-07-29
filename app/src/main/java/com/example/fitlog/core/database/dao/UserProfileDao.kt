package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fitlog.core.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserProfileEntity): Long

    @Query("SELECT * FROM user_profiles LIMIT 1")
    suspend fun get(): UserProfileEntity?

    @Query("SELECT * FROM user_profiles LIMIT 1")
    fun observe(): Flow<UserProfileEntity?>
}
