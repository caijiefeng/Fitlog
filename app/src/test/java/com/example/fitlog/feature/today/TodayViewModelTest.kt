package com.example.fitlog.feature.today

import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.CalendarRepository
import com.example.fitlog.data.repository.FoodRecordRepository
import com.example.fitlog.data.repository.ProgressRepository
import com.example.fitlog.data.repository.WorkoutSessionRepository
import com.example.fitlog.domain.calendar.CalendarDay
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val calendarRepo = mockk<CalendarRepository>(relaxed = true)
    private val sessionRepo = mockk<WorkoutSessionRepository>(relaxed = true)
    private val dateProvider = mockk<CurrentDateProvider>(relaxed = true)
    private val progressRepo = mockk<ProgressRepository>(relaxed = true)
    private val foodRepo = mockk<FoodRecordRepository>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state after load`() = runTest(testDispatcher) {
        val today = LocalDate.of(2026, 7, 29)
        coEvery { dateProvider.today() } returns today
        coEvery { calendarRepo.getDayDetail(today.toEpochDay()) } returns emptyList()
        coEvery { sessionRepo.observeInProgress() } returns flowOf(null)
        val vm = TodayViewModel(calendarRepo, sessionRepo, progressRepo, foodRepo, dateProvider)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.first()
        assertFalse(state.isLoading)
        assertTrue(state.occurrences.isEmpty())
    }

    @Test
    fun `occurrences populated when day has workouts`() = runTest(testDispatcher) {
        val today = LocalDate.of(2026, 7, 29)
        coEvery { dateProvider.today() } returns today
        coEvery { sessionRepo.observeInProgress() } returns flowOf(null)
        val vm = TodayViewModel(calendarRepo, sessionRepo, progressRepo, foodRepo, dateProvider)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun `hasInProgress true when session exists`() = runTest(testDispatcher) {
        val today = LocalDate.of(2026, 7, 29)
        coEvery { dateProvider.today() } returns today
        coEvery { calendarRepo.getDayDetail(today.toEpochDay()) } returns emptyList()
        coEvery { sessionRepo.observeInProgress() } returns flowOf(
            com.example.fitlog.core.model.WorkoutSession(id = 1, status = com.example.fitlog.core.model.WorkoutStatus.IN_PROGRESS)
        )
        val vm = TodayViewModel(calendarRepo, sessionRepo, progressRepo, foodRepo, dateProvider)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.first()
        assertTrue(state.hasInProgressWorkout)
        assertEquals(1L, state.inProgressSessionId)
    }
}
