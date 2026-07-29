package com.example.fitlog.feature.today

import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.CalendarRepository
import com.example.fitlog.data.repository.WorkoutSessionRepository
import com.example.fitlog.domain.calendar.CalendarDay
import com.example.fitlog.domain.calendar.CalendarWorkoutOccurrence
import com.example.fitlog.domain.calendar.CalendarWorkoutStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TodayMultiPlanTest {

    private val calendarRepo = mockk<CalendarRepository>(relaxed = true)
    private val sessionRepo = mockk<WorkoutSessionRepository>(relaxed = true)
    private val dateProvider = mockk<CurrentDateProvider>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `onStartWorkout with occurrence 2 starts occurrence 2`() = runTest(testDispatcher) {
        val today = LocalDate.of(2026, 7, 29)
        coEvery { dateProvider.today() } returns today
        coEvery { sessionRepo.observeInProgress() } returns flowOf(null)

        val occurrence1 = CalendarWorkoutOccurrence(
            key = "1:1", scheduleId = 1L, templateId = 10L,
            templateName = "Push Day", occurrenceDate = today, plannedDate = today,
            sessionId = null, status = CalendarWorkoutStatus.SCHEDULED,
            isQuickWorkout = false, isOriginalDateMarker = false, canStart = true,
        )
        val occurrence2 = CalendarWorkoutOccurrence(
            key = "1:2", scheduleId = 1L, templateId = 20L,
            templateName = "Leg Day", occurrenceDate = today, plannedDate = today,
            sessionId = null, status = CalendarWorkoutStatus.SCHEDULED,
            isQuickWorkout = false, isOriginalDateMarker = false, canStart = true,
        )
        val day = CalendarDay(
            epochDay = today.toEpochDay(), date = today,
            dayOfMonth = today.dayOfMonth, dayOfWeek = today.dayOfWeek.value,
            occurrences = listOf(occurrence1, occurrence2),
        )
        coEvery { calendarRepo.getDayDetail(today.toEpochDay()) } returns listOf(day)

        val vm = TodayViewModel(calendarRepo, sessionRepo, dateProvider)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onStartWorkout(occurrence2)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            sessionRepo.createFromTemplate(
                templateId = 20L, scheduleId = 1L, occurrenceDate = today.toEpochDay(),
            )
        }
        coVerify(exactly = 0) {
            sessionRepo.createFromTemplate(templateId = 10L, any(), any())
        }
    }
}
