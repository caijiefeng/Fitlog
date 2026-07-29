package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.fitlog.core.database.entity.ExerciseSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSessionDao {

    @Insert
    suspend fun insert(entity: ExerciseSessionEntity): Long

    @Update
    suspend fun update(entity: ExerciseSessionEntity)

    @Query("SELECT * FROM exercise_sessions ORDER BY session_id ASC, sort_order ASC")
    suspend fun getAll(): List<ExerciseSessionEntity>

    @Query("SELECT * FROM exercise_sessions WHERE session_id = :sessionId ORDER BY sort_order ASC")
    suspend fun getBySession(sessionId: Long): List<ExerciseSessionEntity>

    @Query("SELECT * FROM exercise_sessions WHERE session_id = :sessionId ORDER BY sort_order ASC")
    fun observeBySession(sessionId: Long): Flow<List<ExerciseSessionEntity>>

    @Query("SELECT * FROM exercise_sessions WHERE id = :id")
    suspend fun getById(id: Long): ExerciseSessionEntity?

    @Query("UPDATE exercise_sessions SET is_skipped = :skipped WHERE id = :id")
    suspend fun setSkipped(id: Long, skipped: Boolean)

    @Query("UPDATE exercise_sessions SET sort_order = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)

    @Query("UPDATE exercise_sessions SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String?)

    @Query("SELECT COUNT(*) FROM exercise_sessions")
    suspend fun count(): Int
}
