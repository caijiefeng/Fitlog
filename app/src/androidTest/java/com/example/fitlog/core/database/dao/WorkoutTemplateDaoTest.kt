package com.example.fitlog.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.fitlog.core.database.FitLogDatabase
import com.example.fitlog.core.database.entity.ExerciseEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateExerciseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkoutTemplateDaoTest {

    private lateinit var db: FitLogDatabase
    private lateinit var templateDao: WorkoutTemplateDao
    private lateinit var exerciseDao: ExerciseDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitLogDatabase::class.java,
        ).build()
        templateDao = db.workoutTemplateDao()
        exerciseDao = db.exerciseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun createExercise(name: String, group: String): Long =
        exerciseDao.insert(ExerciseEntity(name = name, primaryMuscleGroup = group))

    @Test
    fun createAndGetTemplate() = runBlocking {
        val id = templateDao.insert(WorkoutTemplateEntity(name = "Push Day"))
        val result = templateDao.getById(id)
        assertNotNull(result)
        assertEquals("Push Day", result!!.name)
    }

    @Test
    fun getTemplateWithExercises() = runBlocking {
        val ex1 = createExercise("杠铃卧推", "CHEST")
        val ex2 = createExercise("哑铃飞鸟", "CHEST")
        val tId = templateDao.insert(WorkoutTemplateEntity(name = "Push Day"))

        templateDao.insertTemplateExercises(listOf(
            WorkoutTemplateExerciseEntity(templateId = tId, exerciseId = ex1, sortOrder = 0),
            WorkoutTemplateExerciseEntity(templateId = tId, exerciseId = ex2, sortOrder = 1),
        ))

        val result = templateDao.getByIdWithExercises(tId)
        assertNotNull(result)
        assertEquals(2, result!!.exercises.size)
    }

    @Test
    fun exercisesSortedByOrder() = runBlocking {
        val ex1 = createExercise("A", "CHEST")
        val ex2 = createExercise("B", "CHEST")
        val ex3 = createExercise("C", "CHEST")
        val tId = templateDao.insert(WorkoutTemplateEntity(name = "Test"))

        templateDao.insertTemplateExercises(listOf(
            WorkoutTemplateExerciseEntity(templateId = tId, exerciseId = ex3, sortOrder = 2),
            WorkoutTemplateExerciseEntity(templateId = tId, exerciseId = ex1, sortOrder = 0),
            WorkoutTemplateExerciseEntity(templateId = tId, exerciseId = ex2, sortOrder = 1),
        ))

        val exercises = templateDao.getExercisesByTemplate(tId)
        assertEquals(3, exercises.size)
        assertEquals(ex1, exercises[0].exerciseId)
        assertEquals(ex2, exercises[1].exerciseId)
        assertEquals(ex3, exercises[2].exerciseId)
    }

    @Test
    fun replaceExercises_transactionally() = runBlocking {
        val ex1 = createExercise("A", "CHEST")
        val ex2 = createExercise("B", "CHEST")
        val tId = templateDao.insert(WorkoutTemplateEntity(name = "Test"))

        // Insert initial
        templateDao.insertTemplateExercises(listOf(
            WorkoutTemplateExerciseEntity(templateId = tId, exerciseId = ex1, sortOrder = 0),
        ))
        assertEquals(1, templateDao.exerciseCount(tId))

        // Replace
        templateDao.deleteTemplateExercises(tId)
        templateDao.insertTemplateExercises(listOf(
            WorkoutTemplateExerciseEntity(templateId = tId, exerciseId = ex2, sortOrder = 0),
        ))
        assertEquals(1, templateDao.exerciseCount(tId))
        assertEquals(ex2, templateDao.getExercisesByTemplate(tId)[0].exerciseId)
    }

    @Test
    fun softDelete_template_notInActiveList() = runBlocking {
        val tId = templateDao.insert(WorkoutTemplateEntity(name = "To Delete"))
        assertEquals(1, templateDao.getAllActive().first().size)

        templateDao.softDelete(tId)
        assertEquals(0, templateDao.getAllActive().first().size)
    }

    @Test
    fun templateWithTargetParams_savesCorrectly() = runBlocking {
        val ex1 = createExercise("Squat", "QUADRICEPS")
        val tId = templateDao.insert(WorkoutTemplateEntity(name = "Leg Day"))

        templateDao.insertTemplateExercise(
            WorkoutTemplateExerciseEntity(
                templateId = tId, exerciseId = ex1,
                targetSets = 5, targetRepsMin = 5, targetRepsMax = 8,
                targetRpe = 8.0, restSeconds = 180,
            )
        )

        val result = templateDao.getByIdWithExercises(tId)
        val te = result!!.exercises[0]
        assertEquals(5, te.targetSets)
        assertEquals(5, te.targetRepsMin)
        assertEquals(8, te.targetRepsMax)
        assertEquals(8.0, te.targetRpe!!, 0.01)
        assertEquals(180, te.restSeconds)
    }

    @Test
    fun deleteTemplateExercises_emptyList() = runBlocking {
        val ex1 = createExercise("A", "CHEST")
        val tId = templateDao.insert(WorkoutTemplateEntity(name = "Test"))
        templateDao.insertTemplateExercises(listOf(
            WorkoutTemplateExerciseEntity(templateId = tId, exerciseId = ex1),
        ))
        assertEquals(1, templateDao.exerciseCount(tId))

        templateDao.deleteTemplateExercises(tId)
        assertEquals(0, templateDao.exerciseCount(tId))
    }
}
