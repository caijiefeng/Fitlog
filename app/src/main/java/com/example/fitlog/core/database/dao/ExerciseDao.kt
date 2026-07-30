package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fitlog.core.database.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises WHERE is_active = 1 ORDER BY sort_order ASC, name ASC")
    fun getAllActive(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE is_active = 1 AND primary_muscle_group = :muscleGroup ORDER BY sort_order ASC, name ASC")
    fun getByMuscleGroup(muscleGroup: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE is_active = 1 AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchByName(query: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id = :id AND is_active = 1")
    suspend fun getActiveById(id: Long): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<ExerciseEntity>): List<Long>

    @Update
    suspend fun update(entity: ExerciseEntity)

    @Query("UPDATE exercises SET is_active = 0, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM exercises WHERE is_active = 1 AND name = :name")
    suspend fun countByName(name: String): Int

    @Query("SELECT * FROM exercises ORDER BY sort_order ASC, name ASC")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE built_in_key = :key LIMIT 1")
    suspend fun getByBuiltInKey(key: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE built_in_key IS NOT NULL")
    suspend fun getAllBuiltIn(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM exercises WHERE is_active = 1")
    suspend fun count(): Int
}
