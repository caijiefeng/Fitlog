package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fitlog.core.database.entity.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BodyMeasurementEntity): Long

    @Query("SELECT * FROM body_measurements WHERE date >= :start AND date <= :end ORDER BY date ASC")
    suspend fun getByDateRange(start: Long, end: Long): List<BodyMeasurementEntity>

    @Query("SELECT * FROM body_measurements ORDER BY date ASC")
    suspend fun getAll(): List<BodyMeasurementEntity>

    @Query("DELETE FROM body_measurements WHERE date = :epochDay")
    suspend fun deleteByDate(epochDay: Long)

    @Query("SELECT * FROM body_measurements ORDER BY date ASC")
    fun observeAll(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements WHERE date <= :epochDay ORDER BY date DESC LIMIT 1")
    suspend fun getLatestOnOrBefore(epochDay: Long): BodyMeasurementEntity?

    @Query("SELECT COUNT(*) FROM body_measurements")
    suspend fun count(): Int
}
