package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fitlog.core.database.entity.CheckInEntity

@Dao
interface CheckInDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CheckInEntity): Long

    @Query("SELECT * FROM check_ins WHERE date = :epochDay")
    suspend fun getByDate(epochDay: Long): CheckInEntity?

    @Query("SELECT * FROM check_ins WHERE date >= :start AND date <= :end ORDER BY date ASC")
    suspend fun getByDateRange(start: Long, end: Long): List<CheckInEntity>
}
