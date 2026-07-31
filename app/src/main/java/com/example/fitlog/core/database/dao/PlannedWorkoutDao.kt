package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.fitlog.core.database.entity.PlannedWorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedWorkoutDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PlannedWorkoutEntity): Long

    /** Returns -1 for rows skipped by the unique (template_id, planned_date) index. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: PlannedWorkoutEntity): Long

    /**
     * Bulk-inserts a batch of plans inside one transaction. Rows that collide
     * with the unique (template_id, planned_date) index are skipped and map to
     * -1 in the returned array, in the same order as [entities].
     */
    @Transaction
    suspend fun insertAllIgnore(entities: List<PlannedWorkoutEntity>): LongArray {
        val ids = LongArray(entities.size)
        entities.forEachIndexed { index, entity ->
            ids[index] = insertIgnore(entity)
        }
        return ids
    }

    @Update
    suspend fun update(entity: PlannedWorkoutEntity)

    @Query("SELECT * FROM planned_workouts WHERE id = :id")
    suspend fun getById(id: Long): PlannedWorkoutEntity?

    @Query("SELECT * FROM planned_workouts WHERE planned_date = :epochDay ORDER BY created_at ASC")
    suspend fun getByDate(epochDay: Long): List<PlannedWorkoutEntity>

    @Query("SELECT * FROM planned_workouts WHERE planned_date = :epochDay ORDER BY created_at ASC")
    fun observeByDate(epochDay: Long): Flow<List<PlannedWorkoutEntity>>

    @Query("""
        SELECT * FROM planned_workouts
        WHERE planned_date >= :startEpochDay AND planned_date <= :endEpochDay
        ORDER BY planned_date ASC, created_at ASC
    """)
    suspend fun getByDateRange(startEpochDay: Long, endEpochDay: Long): List<PlannedWorkoutEntity>

    @Query("""
        SELECT * FROM planned_workouts
        WHERE planned_date >= :startEpochDay AND planned_date <= :endEpochDay
        ORDER BY planned_date ASC, created_at ASC
    """)
    fun observeByDateRange(startEpochDay: Long, endEpochDay: Long): Flow<List<PlannedWorkoutEntity>>

    @Query("SELECT * FROM planned_workouts ORDER BY planned_date ASC, created_at ASC")
    suspend fun getAll(): List<PlannedWorkoutEntity>

    @Query("SELECT COUNT(*) FROM planned_workouts")
    suspend fun count(): Int

    @Query("DELETE FROM planned_workouts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM planned_workouts WHERE template_id = :templateId")
    suspend fun deleteByTemplateId(templateId: Long)
}
