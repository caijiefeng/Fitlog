package com.example.fitlog.domain.calendar

import com.example.fitlog.core.database.entity.WorkoutPlanOverrideEntity
import com.example.fitlog.core.database.entity.WorkoutScheduleEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import com.example.fitlog.core.time.CurrentDateProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OccurrenceResolverTest {

    private val monday = LocalDate.of(2026, 7, 27)
    private val wednesday = LocalDate.of(2026, 7, 29)
    private val template = WorkoutTemplateEntity(id = 1L, name = "Push Day")
    private val schedule = WorkoutScheduleEntity(id = 1L, templateId = 1L, dayOfWeek = 1)

    private val dateProvider = mockk<CurrentDateProvider>().also {
        every { it.today() } returns monday
    }
    private val resolver = CalendarOccurrenceResolver(dateProvider)

    @Test
    fun `normal schedule shows SCHEDULED`() {
        val result = resolver.resolveRange(
            startEpochDay = monday.toEpochDay(),
            endEpochDay = monday.toEpochDay(),
            schedules = listOf(schedule),
            overrides = emptyList(),
            sessions = emptyList(),
            templates = mapOf(1L to template),
        )

        val occurrence = result.first().occurrences.first()
        assertEquals(CalendarWorkoutStatus.SCHEDULED, occurrence.status)
        assertFalse(occurrence.isOriginalDateMarker)
        assertTrue(occurrence.canStart)
    }

    @Test
    fun `rescheduled override shows RESCHEDULED on original date`() {
        val override = WorkoutPlanOverrideEntity(
            id = 1L, scheduleId = 1L, templateId = 1L,
            occurrenceDate = monday.toEpochDay(), plannedDate = wednesday.toEpochDay(),
            action = "RESCHEDULED",
        )

        val result = resolver.resolveRange(
            startEpochDay = monday.toEpochDay(),
            endEpochDay = monday.toEpochDay(),
            schedules = listOf(schedule),
            overrides = listOf(override),
            sessions = emptyList(),
            templates = mapOf(1L to template),
        )

        val occurrence = result.first().occurrences.first()
        assertEquals(CalendarWorkoutStatus.RESCHEDULED, occurrence.status)
        assertTrue(occurrence.isOriginalDateMarker)
        assertFalse(occurrence.canStart)
    }

    @Test
    fun `skipped override shows SKIPPED`() {
        val override = WorkoutPlanOverrideEntity(
            id = 2L, scheduleId = 1L, templateId = 1L,
            occurrenceDate = monday.toEpochDay(), plannedDate = null,
            action = "SKIPPED",
        )

        val result = resolver.resolveRange(
            startEpochDay = monday.toEpochDay(),
            endEpochDay = monday.toEpochDay(),
            schedules = listOf(schedule),
            overrides = listOf(override),
            sessions = emptyList(),
            templates = mapOf(1L to template),
        )

        val occurrence = result.first().occurrences.first()
        assertEquals(CalendarWorkoutStatus.SKIPPED, occurrence.status)
        assertFalse(occurrence.isOriginalDateMarker)
        assertFalse(occurrence.canStart)
    }
}
