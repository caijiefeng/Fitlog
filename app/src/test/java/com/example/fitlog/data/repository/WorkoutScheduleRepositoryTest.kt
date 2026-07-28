package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.WorkoutScheduleDao
import com.example.fitlog.core.database.entity.WorkoutScheduleEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WorkoutScheduleRepositoryTest {

    private val dao = mockk<WorkoutScheduleDao>(relaxed = true)
    private val repo = WorkoutScheduleRepository(dao)

    @Test
    fun `getFullWeek returns 7 days`() = runTest {
        coEvery { dao.getFullWeekSchedule() } returns flowOf(emptyList())

        val week = repo.getFullWeek().first()

        assertEquals(7, week.size)
        assertEquals("周一", week[0].dayName)
        assertEquals("周日", week[6].dayName)
    }

    @Test
    fun `getFullWeek maps scheduled days`() = runTest {
        coEvery { dao.getFullWeekSchedule() } returns flowOf(listOf(
            com.example.fitlog.core.database.relation.ScheduleWithTemplate(
                id = 1L, templateId = 10L, dayOfWeek = 1, isActive = true,
                createdAt = 1000L, templateName = "Push Day", templateNotes = null,
                exerciseCount = 5,
            ),
        ))

        val week = repo.getFullWeek().first()

        assertEquals("Push Day", week[0].templateName)
        assertEquals(5, week[0].exerciseCount)
        assertNull(week[1].templateName) // Tuesday empty
    }

    @Test
    fun `getTodaySchedule returns null when no schedule`() = runTest {
        val today = WorkoutScheduleRepository.dayOfWeekToInt(LocalDate.now().dayOfWeek)
        coEvery { dao.getScheduleForDay(today) } returns flowOf(null)

        val result = repo.getTodaySchedule().first()

        assertNull(result)
    }

    @Test
    fun `getTodaySchedule returns schedule when set`() = runTest {
        val today = WorkoutScheduleRepository.dayOfWeekToInt(LocalDate.now().dayOfWeek)
        coEvery { dao.getScheduleForDay(today) } returns flowOf(
            com.example.fitlog.core.database.relation.ScheduleWithTemplate(
                id = 1L, templateId = 20L, dayOfWeek = today, isActive = true,
                createdAt = 1000L, templateName = "Leg Day", templateNotes = null,
                exerciseCount = 6,
            )
        )

        val result = repo.getTodaySchedule().first()

        assertNotNull(result)
        assertEquals("Leg Day", result!!.templateName)
        assertEquals(6, result.exerciseCount)
    }

    @Test
    fun `setTemplate deletes existing then inserts`() = runTest {
        repo.setTemplate(1, 10L)

        coVerify { dao.deleteByDayOfWeek(1) }
        coVerify { dao.insert(any<WorkoutScheduleEntity>()) }
    }

    @Test
    fun `clearDay delegates to DAO`() = runTest {
        repo.clearDay(3)

        coVerify { dao.clearDay(3) }
    }

    @Test
    fun `dayOfWeekToInt maps correctly`() {
        assertEquals(1, WorkoutScheduleRepository.dayOfWeekToInt(DayOfWeek.MONDAY))
        assertEquals(7, WorkoutScheduleRepository.dayOfWeekToInt(DayOfWeek.SUNDAY))
        assertEquals(4, WorkoutScheduleRepository.dayOfWeekToInt(DayOfWeek.THURSDAY))
    }

    @Test
    fun `dayName returns correct Chinese labels`() {
        assertEquals("周一", WorkoutScheduleRepository.dayName(1))
        assertEquals("周三", WorkoutScheduleRepository.dayName(3))
        assertEquals("周日", WorkoutScheduleRepository.dayName(7))
        assertEquals("", WorkoutScheduleRepository.dayName(0))
    }
}
