package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fitlog.core.database.entity.WorkoutPlanOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanOverrideDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: WorkoutPlanOverrideEntity): Long

    @Update
    suspend fun update(entity: WorkoutPlanOverrideEntity)

    @Query("SELECT * FROM workout_plan_overrides WHERE schedule_id = :scheduleId AND occurrence_date = :occurrenceDate")
    suspend fun getByScheduleAndOccurrence(scheduleId: Long, occurrenceDate: Long): WorkoutPlanOverrideEntity?

    @Query("SELECT * FROM workout_plan_overrides WHERE planned_date = :epochDay")
    suspend fun getByPlannedDate(epochDay: Long): List<WorkoutPlanOverrideEntity>

    @Query("SELECT * FROM workout_plan_overrides WHERE occurrence_date >= :startEpochDay AND occurrence_date <= :endEpochDay ORDER BY occurrence_date ASC")
    suspend fun getByOccurrenceDateRange(startEpochDay: Long, endEpochDay: Long): List<WorkoutPlanOverrideEntity>

    @Query("""
        SELECT * FROM workout_plan_overrides
        WHERE (occurrence_date >= :startEpochDay AND occurrence_date <= :endEpochDay)
           OR (planned_date >= :startEpochDay AND planned_date <= :endEpochDay)
        ORDER BY COALESCE(planned_date, occurrence_date) ASC
    """)
    suspend fun getRelevantToDateRange(startEpochDay: Long, endEpochDay: Long): List<WorkoutPlanOverrideEntity>

    @Query("DELETE FROM workout_plan_overrides WHERE schedule_id = :scheduleId AND occurrence_date = :occurrenceDate")
    suspend fun deleteByScheduleAndDate(scheduleId: Long, occurrenceDate: Long)

    @Query("SELECT * FROM workout_plan_overrides ORDER BY planned_date ASC")
    fun getActiveOverrides(): Flow<List<WorkoutPlanOverrideEntity>>

    @Query("SELECT * FROM workout_plan_overrides ORDER BY created_at DESC")
    fun observeAll(): Flow<List<WorkoutPlanOverrideEntity>>
}
