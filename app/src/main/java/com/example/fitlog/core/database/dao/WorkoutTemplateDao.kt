package com.example.fitlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateExerciseEntity
import com.example.fitlog.core.database.relation.TemplateWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTemplateDao {

    @Query("SELECT * FROM workout_templates WHERE is_active = 1 ORDER BY sort_order ASC, name ASC")
    fun getAllActive(): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates WHERE is_active = 1 ORDER BY sort_order ASC, name ASC")
    suspend fun getAllActiveList(): List<WorkoutTemplateEntity>

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE is_active = 1 ORDER BY sort_order ASC, name ASC")
    fun getAllWithExercises(): Flow<List<TemplateWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :id AND is_active = 1")
    suspend fun getByIdWithExercises(id: Long): TemplateWithExercises?

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getById(id: Long): WorkoutTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: WorkoutTemplateEntity): Long

    @Update
    suspend fun update(entity: WorkoutTemplateEntity)

    @Query("UPDATE workout_templates SET is_active = 0, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long = System.currentTimeMillis())

    // Template exercises
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercise(entity: WorkoutTemplateExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercises(entities: List<WorkoutTemplateExerciseEntity>)

    @Query("DELETE FROM workout_template_exercises WHERE template_id = :templateId")
    suspend fun deleteTemplateExercises(templateId: Long)

    @Query("SELECT * FROM workout_template_exercises WHERE template_id = :templateId ORDER BY sort_order ASC")
    suspend fun getExercisesByTemplate(templateId: Long): List<WorkoutTemplateExerciseEntity>

    @Query("SELECT COUNT(*) FROM workout_template_exercises WHERE template_id = :templateId")
    suspend fun exerciseCount(templateId: Long): Int
}
