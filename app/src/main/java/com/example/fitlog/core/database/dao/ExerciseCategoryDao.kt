package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fitlog.core.database.entity.ExerciseCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseCategoryDao {

    @Query("SELECT * FROM exercise_categories ORDER BY sort_order ASC, name ASC")
    fun getAll(): Flow<List<ExerciseCategoryEntity>>

    @Query("SELECT * FROM exercise_categories WHERE id = :id")
    suspend fun getById(id: Long): ExerciseCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ExerciseCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<ExerciseCategoryEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM exercise_categories")
    suspend fun count(): Int
}
