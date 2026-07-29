package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.fitlog.core.database.entity.WorkoutSessionEntity
import com.example.fitlog.core.database.relation.SessionWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    @Insert
    suspend fun insert(entity: WorkoutSessionEntity): Long

    @Update
    suspend fun update(entity: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSessionEntity?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getByIdWithExercises(id: Long): SessionWithExercises?

    @Query("SELECT * FROM workout_sessions WHERE status = 'IN_PROGRESS' LIMIT 1")
    suspend fun getInProgress(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE status = 'IN_PROGRESS' LIMIT 1")
    fun observeInProgress(): Flow<WorkoutSessionEntity?>

    @Query("""
        SELECT * FROM workout_sessions
        WHERE status IN ('COMPLETED','PARTIALLY_COMPLETED','CANCELLED')
        ORDER BY start_time DESC
    """)
    fun getHistory(): Flow<List<WorkoutSessionEntity>>

    @Query("""
        SELECT * FROM workout_sessions
        WHERE status IN ('COMPLETED','PARTIALLY_COMPLETED','CANCELLED')
        AND date = :date
        ORDER BY start_time DESC
    """)
    fun getHistoryByDate(date: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE status = 'PLANNED' AND date = :date")
    suspend fun getPlannedByDate(date: Long): List<WorkoutSessionEntity>
}
