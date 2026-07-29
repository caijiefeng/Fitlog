package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.fitlog.core.database.entity.SetRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Daily volume aggregated from completed sets across all sessions on a given day.
 */
data class DailyVolume(
    /** Epoch day (date) the volume was performed on. */
    val date: Long,
    /** Total volume (reps * weight) for that day. */
    val volume: Double,
)

@Dao
interface SetRecordDao {

    @Insert
    suspend fun insert(entity: SetRecordEntity): Long

    @Update
    suspend fun update(entity: SetRecordEntity)

    @Query("SELECT * FROM set_records WHERE exercise_session_id = :exerciseSessionId ORDER BY set_number ASC")
    suspend fun getByExerciseSession(exerciseSessionId: Long): List<SetRecordEntity>

    @Query("SELECT * FROM set_records WHERE exercise_session_id = :exerciseSessionId ORDER BY set_number ASC")
    fun observeByExerciseSession(exerciseSessionId: Long): Flow<List<SetRecordEntity>>

    @Query("SELECT * FROM set_records ORDER BY exercise_session_id ASC, set_number ASC")
    suspend fun getAll(): List<SetRecordEntity>

    @Query("SELECT COUNT(*) FROM set_records")
    suspend fun count(): Int

    @Query("SELECT * FROM set_records WHERE id = :id")
    suspend fun getById(id: Long): SetRecordEntity?

    @Query("DELETE FROM set_records WHERE id = :id AND completed = 0")
    suspend fun deleteIfIncomplete(id: Long)

    @Query("SELECT COUNT(*) FROM set_records WHERE exercise_session_id = :exerciseSessionId AND completed = 1")
    suspend fun completedSetCount(exerciseSessionId: Long): Int

    @Query("""
        SELECT SUM(reps * weight_kg) FROM set_records
        WHERE exercise_session_id IN (
            SELECT id FROM exercise_sessions WHERE session_id = :sessionId
        )
        AND completed = 1 AND reps IS NOT NULL AND weight_kg IS NOT NULL
    """)
    suspend fun totalVolumeForSession(sessionId: Long): Double?

    @Query("""
        SELECT COUNT(*) FROM set_records
        WHERE exercise_session_id IN (
            SELECT id FROM exercise_sessions WHERE session_id = :sessionId
        )
        AND completed = 1
    """)
    suspend fun completedSetCountForSession(sessionId: Long): Int

    /**
     * Returns total volume (reps * weight_kg) aggregated per day for sessions
     * within the given date range with a terminal status.
     *
     * This avoids the N+1 pattern of calling [totalVolumeForSession] per session.
     */
    @Query("""
        SELECT ws.date AS date, COALESCE(SUM(sr.reps * sr.weight_kg), 0.0) AS volume
        FROM workout_sessions ws
        INNER JOIN exercise_sessions es ON es.session_id = ws.id
        INNER JOIN set_records sr ON sr.exercise_session_id = es.id
        WHERE ws.date BETWEEN :startEpochDay AND :endEpochDay
          AND ws.status IN ('COMPLETED', 'PARTIALLY_COMPLETED')
          AND sr.completed = 1
          AND sr.reps IS NOT NULL AND sr.weight_kg IS NOT NULL
        GROUP BY ws.date
        ORDER BY ws.date ASC
    """)
    suspend fun getDailyVolumeByDateRange(startEpochDay: Long, endEpochDay: Long): List<DailyVolume>
}
