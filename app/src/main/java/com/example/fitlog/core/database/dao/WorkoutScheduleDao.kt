package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fitlog.core.database.entity.WorkoutScheduleEntity
import com.example.fitlog.core.database.relation.ScheduleWithTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutScheduleDao {

    @Query("SELECT * FROM workout_schedules WHERE is_active = 1 ORDER BY day_of_week ASC")
    fun getAllActive(): Flow<List<WorkoutScheduleEntity>>

    @Query("SELECT * FROM workout_schedules WHERE is_active = 1 ORDER BY day_of_week ASC")
    suspend fun getAllActiveList(): List<WorkoutScheduleEntity>

    @Query("SELECT * FROM workout_schedules ORDER BY day_of_week ASC")
    suspend fun getAll(): List<WorkoutScheduleEntity>

    @Query("SELECT * FROM workout_schedules WHERE day_of_week = :dayOfWeek AND is_active = 1")
    suspend fun getByDayOfWeek(dayOfWeek: Int): WorkoutScheduleEntity?

    @Query("SELECT * FROM workout_schedules WHERE day_of_week = :dayOfWeek AND is_active = 1")
    fun observeByDayOfWeek(dayOfWeek: Int): Flow<WorkoutScheduleEntity?>

    @Query("""
        SELECT ws.*, wt.name as template_name, wt.notes as template_notes,
               (SELECT COUNT(*) FROM workout_template_exercises WHERE template_id = wt.id) as exercise_count
        FROM workout_schedules ws
        INNER JOIN workout_templates wt ON ws.template_id = wt.id
        WHERE ws.is_active = 1 AND wt.is_active = 1
        ORDER BY ws.day_of_week ASC
    """)
    fun getFullWeekSchedule(): Flow<List<ScheduleWithTemplate>>

    @Query("""
        SELECT ws.*, wt.name as template_name, wt.notes as template_notes,
               (SELECT COUNT(*) FROM workout_template_exercises WHERE template_id = wt.id) as exercise_count
        FROM workout_schedules ws
        INNER JOIN workout_templates wt ON ws.template_id = wt.id
        WHERE ws.day_of_week = :dayOfWeek AND ws.is_active = 1 AND wt.is_active = 1
    """)
    fun getScheduleForDay(dayOfWeek: Int): Flow<ScheduleWithTemplate?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WorkoutScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WorkoutScheduleEntity): Long

    @Query("UPDATE workout_schedules SET is_active = 0 WHERE day_of_week = :dayOfWeek")
    suspend fun clearDay(dayOfWeek: Int)

    @Query("DELETE FROM workout_schedules WHERE day_of_week = :dayOfWeek")
    suspend fun deleteByDayOfWeek(dayOfWeek: Int)

    @Query("SELECT COUNT(*) FROM workout_schedules")
    suspend fun count(): Int
}
