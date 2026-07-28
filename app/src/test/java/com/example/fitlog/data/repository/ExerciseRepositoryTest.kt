package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.ExerciseDao
import com.example.fitlog.core.database.entity.ExerciseEntity
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.MuscleGroup
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ExerciseRepositoryTest {

    private val dao = mockk<ExerciseDao>(relaxed = true)
    private val repo = ExerciseRepository(dao)

    @Test
    fun `getAllActive maps entities to domain`() = runTest {
        val entity = ExerciseEntity(
            id = 1, name = "Bench Press", primaryMuscleGroup = "CHEST",
            isCustom = true, createdAt = 1000L, updatedAt = 2000L,
        )
        coEvery { dao.getAllActive() } returns flowOf(listOf(entity))

        val result = repo.getAllActive().first()

        assertEquals(1, result.size)
        assertEquals("Bench Press", result[0].name)
        assertEquals(MuscleGroup.CHEST, result[0].primaryMuscleGroup)
        assertTrue(result[0].isCustom)
        assertEquals(Instant.ofEpochMilli(1000L), result[0].createdAt)
    }

    @Test
    fun `getByMuscleGroup filters correctly`() = runTest {
        coEvery { dao.getByMuscleGroup("BACK") } returns flowOf(
            listOf(ExerciseEntity(id = 1, name = "Pull Up", primaryMuscleGroup = "BACK"))
        )

        val result = repo.getByMuscleGroup(MuscleGroup.BACK).first()

        assertEquals(1, result.size)
        assertEquals(MuscleGroup.BACK, result[0].primaryMuscleGroup)
    }

    @Test
    fun `searchByName delegates to DAO`() = runTest {
        coEvery { dao.searchByName("squat") } returns flowOf(
            listOf(ExerciseEntity(id = 1, name = "Squat", primaryMuscleGroup = "QUADRICEPS"))
        )

        val result = repo.searchByName("squat").first()

        assertEquals(1, result.size)
        assertEquals("Squat", result[0].name)
    }

    @Test
    fun `create inserts entity`() = runTest {
        coEvery { dao.insert(any()) } returns 42L
        coEvery { dao.countByName("New Exercise") } returns 0

        val id = repo.create(Exercise(name = "New Exercise", primaryMuscleGroup = MuscleGroup.CHEST, isCustom = true))

        assertEquals(42L, id)
        coVerify { dao.insert(any()) }
    }

    @Test
    fun `isNameDuplicate returns true when count over 0`() = runTest {
        coEvery { dao.countByName("Bench Press") } returns 1

        assertTrue(repo.isNameDuplicate("Bench Press"))
    }

    @Test
    fun `isNameDuplicate trims whitespace`() = runTest {
        coEvery { dao.countByName("Bench Press") } returns 1

        assertTrue(repo.isNameDuplicate("  Bench Press  "))
    }

    @Test
    fun `softDelete delegates to DAO`() = runTest {
        repo.softDelete(42L)

        coVerify { dao.softDelete(eq(42L), any()) }
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.getActiveById(99L) } returns null

        val result = repo.getById(99L)

        assertEquals(null, result)
    }

    @Test
    fun `getById maps entity to domain`() = runTest {
        coEvery { dao.getActiveById(1L) } returns ExerciseEntity(
            id = 1, name = "Deadlift", primaryMuscleGroup = "BACK",
            secondaryMuscleGroup = "HAMSTRINGS",
            createdAt = 5000L, updatedAt = 6000L,
        )

        val result = repo.getById(1L)

        assertNotNull(result)
        assertEquals("Deadlift", result!!.name)
        assertEquals(MuscleGroup.BACK, result.primaryMuscleGroup)
        assertEquals(MuscleGroup.HAMSTRINGS, result.secondaryMuscleGroup)
    }
}
