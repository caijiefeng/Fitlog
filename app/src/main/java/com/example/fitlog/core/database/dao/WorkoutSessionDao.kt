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

    @Query("SELECT * FROM workout_sessions WHERE status = 'IN_PROGRESS' ORDER BY start_time DESC LIMIT 1")
    suspend fun getInProgress(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE status = 'IN_PROGRESS' ORDER BY start_time DESC LIMIT 1")
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

    @Query("SELECT * FROM workout_sessions WHERE schedule_id = :scheduleId AND occurrence_date = :occurrenceDate LIMIT 1")
    suspend fun getByScheduleAndOccurrence(scheduleId: Long, occurrenceDate: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions ORDER BY date ASC")
    suspend fun getAll(): List<WorkoutSessionEntity>

    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun count(): Int

    @Query("""
        SELECT * FROM workout_sessions
        WHERE date >= :startEpochDay AND date <= :endEpochDay
        ORDER BY date ASC
    """)
    suspend fun getByDateRange(startEpochDay: Long, endEpochDay: Long): List<WorkoutSessionEntity>

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
