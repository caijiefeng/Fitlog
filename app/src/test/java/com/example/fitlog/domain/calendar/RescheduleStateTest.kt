package com.example.fitlog.domain.calendar

import com.example.fitlog.core.database.entity.WorkoutPlanOverrideEntity
import com.example.fitlog.core.database.entity.WorkoutScheduleEntity
import com.example.fitlog.core.database.entity.WorkoutSessionEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import com.example.fitlog.core.time.CurrentDateProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RescheduleStateTest {

    @Test
    fun `rescheduled and completed shows COMPLETED`() {
        val monday = LocalDate.of(2026, 7, 27) // Monday
        val wednesday = LocalDate.of(2026, 7, 29) // Wednesday
        val template = WorkoutTemplateEntity(id = 1L, name = "Push Day")
        val schedule = WorkoutScheduleEntity(id = 1L, templateId = 1L, dayOfWeek = 1)
        val override = WorkoutPlanOverrideEntity(
            id = 1L, scheduleId = 1L, templateId = 1L,
            occurrenceDate = monday.toEpochDay(), plannedDate = wednesday.toEpochDay(),
            action = "RESCHEDULED",
        )
        val session = WorkoutSessionEntity(
            id = 1L, scheduleId = 1L, templateId = 1L,
            date = monday.toEpochDay(), startTime = monday.toEpochDay() * 86400000,
            status = "COMPLETED", occurrenceDate = monday.toEpochDay(),
        )

        val dateProvider = mockk<CurrentDateProvider>()
        every { dateProvider.today() } returns LocalDate.of(2026, 7, 29)

        val resolver = CalendarOccurrenceResolver(dateProvider)
        val result = resolver.resolveRange(
            startEpochDay = monday.toEpochDay(),
            endEpochDay = monday.toEpochDay(),
            schedules = listOf(schedule),
            overrides = listOf(override),
            sessions = listOf(session),
            templates = mapOf(1L to template),
        )

        val mondayDay = result.first()
        assertEquals(1, mondayDay.occurrences.size)
        val occurrence = mondayDay.occurrences.first()
        assertEquals(CalendarWorkoutStatus.COMPLETED, occurrence.status)
        assertTrue(occurrence.isOriginalDateMarker)
    }
}
