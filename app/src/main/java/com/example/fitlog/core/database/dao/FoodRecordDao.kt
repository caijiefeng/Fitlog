package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fitlog.core.database.entity.FoodRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FoodRecordEntity): Long

    @Update
    suspend fun update(entity: FoodRecordEntity)

    @Query("DELETE FROM food_records WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM food_records WHERE date = :epochDay ORDER BY id ASC")
    suspend fun getByDate(epochDay: Long): List<FoodRecordEntity>

    @Query("SELECT * FROM food_records WHERE date >= :start AND date <= :end ORDER BY date ASC, id ASC")
    suspend fun getByDateRange(start: Long, end: Long): List<FoodRecordEntity>

    @Query("SELECT * FROM food_records WHERE date = :epochDay ORDER BY id ASC")
    fun observeByDate(epochDay: Long): Flow<List<FoodRecordEntity>>

    @Query("SELECT * FROM food_records ORDER BY date ASC, id ASC")
    suspend fun getAll(): List<FoodRecordEntity>

    @Query("SELECT SUM(calories) FROM food_records WHERE date = :epochDay")
    suspend fun getTotalCaloriesByDate(epochDay: Long): Double?

    @Query("SELECT SUM(protein_grams) FROM food_records WHERE date = :epochDay")
    suspend fun getTotalProteinByDate(epochDay: Long): Double?

    @Query("SELECT SUM(carbs_grams) FROM food_records WHERE date = :epochDay")
    suspend fun getTotalCarbsByDate(epochDay: Long): Double?

    @Query("SELECT SUM(fat_grams) FROM food_records WHERE date = :epochDay")
    suspend fun getTotalFatByDate(epochDay: Long): Double?

    @Query("SELECT COUNT(*) FROM food_records")
    suspend fun count(): Int
}
