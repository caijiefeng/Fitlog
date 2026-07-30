package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fitlog.core.database.entity.MediaRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MediaRecordEntity): Long

    @Update
    suspend fun update(entity: MediaRecordEntity)

    @Query("DELETE FROM media_records WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM media_records WHERE id = :id")
    suspend fun getById(id: Long): MediaRecordEntity?

    @Query("SELECT * FROM media_records WHERE id = :id")
    fun observeById(id: Long): Flow<MediaRecordEntity?>

    @Query("SELECT * FROM media_records WHERE date >= :startEpochDay AND date <= :endEpochDay ORDER BY captured_at DESC")
    suspend fun getByDateRange(startEpochDay: Long, endEpochDay: Long): List<MediaRecordEntity>

    @Query("SELECT * FROM media_records WHERE workout_session_id = :sessionId ORDER BY captured_at ASC")
    suspend fun getByWorkoutSession(sessionId: Long): List<MediaRecordEntity>

    @Query("SELECT * FROM media_records WHERE body_measurement_id = :measurementId ORDER BY captured_at ASC")
    suspend fun getByBodyMeasurement(measurementId: Long): List<MediaRecordEntity>

    @Query("SELECT * FROM media_records WHERE check_in_id = :checkInId ORDER BY captured_at ASC")
    suspend fun getByCheckIn(checkInId: Long): List<MediaRecordEntity>

    @Query("SELECT * FROM media_records WHERE category = :category ORDER BY captured_at DESC")
    suspend fun getByCategory(category: String): List<MediaRecordEntity>

    @Query("SELECT * FROM media_records WHERE is_favorite = 1 ORDER BY captured_at DESC")
    suspend fun getFavorites(): List<MediaRecordEntity>

    @Query("SELECT * FROM media_records ORDER BY captured_at DESC")
    suspend fun getAll(): List<MediaRecordEntity>

    @Query("SELECT * FROM media_records ORDER BY captured_at DESC")
    fun observeAll(): Flow<List<MediaRecordEntity>>

    /**
     * Returns records whose relativePath points to a file that does not exist.
     * The caller must provide the storage root directory to prepend to each path.
     * This query returns candidates; the actual file-existence check is done
     * by [com.example.fitlog.core.media.MediaCleanupManager].
     */
    @Query("SELECT * FROM media_records")
    suspend fun getAllRecords(): List<MediaRecordEntity>

    @Query("DELETE FROM media_records WHERE id IN (SELECT id FROM media_records ORDER BY captured_at ASC LIMIT :limit)")
    suspend fun deleteOldest(limit: Int): Int

    @Query("SELECT COUNT(*) FROM media_records")
    suspend fun count(): Int

    @Query("UPDATE media_records SET workout_session_id = NULL WHERE workout_session_id = :sessionId")
    suspend fun unlinkWorkoutSession(sessionId: Long)

    @Query("UPDATE media_records SET exercise_session_id = NULL WHERE exercise_session_id = :exerciseSessionId")
    suspend fun unlinkExerciseSession(exerciseSessionId: Long)
}
