package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.ExerciseDao
import com.example.fitlog.core.database.dao.WorkoutTemplateDao
import com.example.fitlog.core.database.entity.ExerciseEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateExerciseEntity
import com.example.fitlog.core.database.relation.TemplateWithExercises
import com.example.fitlog.core.model.WorkoutTemplateExercise
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutTemplateRepositoryTest {

    private val templateDao = mockk<WorkoutTemplateDao>(relaxed = true)
    private val exerciseDao = mockk<ExerciseDao>(relaxed = true)
    private val repo = WorkoutTemplateRepository(templateDao, exerciseDao)

    @Test
    fun `getAllActive maps to domain`() = runTest {
        coEvery { templateDao.getAllActive() } returns flowOf(
            listOf(WorkoutTemplateEntity(id = 1, name = "Push Day", createdAt = 1000L, updatedAt = 2000L))
        )

        val result = repo.getAllActive().first()

        assertEquals(1, result.size)
        assertEquals("Push Day", result[0].name)
    }

    @Test
    fun `getDetail with exercises`() = runTest {
        val exEntity = ExerciseEntity(id = 10, name = "Bench Press", primaryMuscleGroup = "CHEST",
            createdAt = 1000L, updatedAt = 2000L)
        coEvery { exerciseDao.getById(10) } returns exEntity

        val tWithEx = TemplateWithExercises(
            template = WorkoutTemplateEntity(id = 1, name = "Push Day"),
            exercises = listOf(
                WorkoutTemplateExerciseEntity(id = 1, templateId = 1, exerciseId = 10,
                    targetSets = 4, targetRepsMin = 6, targetRepsMax = 10,
                    targetRpe = 8.0, restSeconds = 120, sortOrder = 0),
            ),
        )
        coEvery { templateDao.getByIdWithExercises(1) } returns tWithEx

        val detail = repo.getDetail(1)

        assertNotNull(detail)
        assertEquals("Push Day", detail!!.template.name)
        assertEquals(1, detail.exercises.size)
        assertEquals("Bench Press", detail.exercises[0].templateExercise.exerciseName)
        assertEquals(4, detail.exercises[0].templateExercise.targetSets)
    }

    @Test
    fun `replaceExercises deletes then inserts`() = runTest {
        coEvery { templateDao.deleteTemplateExercises(1) } returns Unit
        coEvery { templateDao.insertTemplateExercises(any()) } returns Unit

        repo.replaceExercises(1, listOf(
            WorkoutTemplateExercise(templateId = 1, exerciseId = 10, targetSets = 3, sortOrder = 0),
            WorkoutTemplateExercise(templateId = 1, exerciseId = 20, targetSets = 4, sortOrder = 1),
        ))

        coVerify { templateDao.deleteTemplateExercises(1) }
        coVerify { templateDao.insertTemplateExercises(any()) }
    }

    @Test
    fun `create returns new id`() = runTest {
        coEvery { templateDao.insert(any()) } returns 99L

        val id = repo.create("New Template", "notes")

        assertEquals(99L, id)
    }

    @Test
    fun `softDelete delegates to DAO`() = runTest {
        repo.softDelete(42L)

        coVerify { templateDao.softDelete(eq(42L), any()) }
    }
}
