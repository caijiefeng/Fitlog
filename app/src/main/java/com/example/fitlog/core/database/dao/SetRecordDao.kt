package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.fitlog.core.database.entity.SetRecordEntity
import kotlinx.coroutines.flow.Flow

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
}
