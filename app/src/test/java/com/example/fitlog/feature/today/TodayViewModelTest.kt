package com.example.fitlog.feature.today

import com.example.fitlog.data.repository.DaySchedule
import com.example.fitlog.data.repository.WorkoutScheduleRepository
import com.example.fitlog.data.repository.WorkoutSessionRepository
import io.mockk.coEvery
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

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val scheduleRepo = mockk<WorkoutScheduleRepository>(relaxed = true)
    private val sessionRepo = mockk<WorkoutSessionRepository>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state after collect`() = runTest(testDispatcher) {
        coEvery { scheduleRepo.getTodaySchedule() } returns flowOf(null)
        coEvery { sessionRepo.observeInProgress() } returns flowOf(null)
        val vm = TodayViewModel(scheduleRepo, sessionRepo)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.first()
        assertFalse(state.isLoading)
        assertFalse(state.hasWorkoutToday)
    }

    @Test
    fun `hasWorkoutToday true when schedule exists`() = runTest(testDispatcher) {
        coEvery { scheduleRepo.getTodaySchedule() } returns flowOf(
            DaySchedule(1, "周一", 10L, "Push Day", 5)
        )
        coEvery { sessionRepo.observeInProgress() } returns flowOf(null)
        val vm = TodayViewModel(scheduleRepo, sessionRepo)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.first()
        assertTrue(state.hasWorkoutToday)
        assertEquals("Push Day", state.todayTemplateName)
    }

    @Test
    fun `hasInProgress true when session exists`() = runTest(testDispatcher) {
        coEvery { scheduleRepo.getTodaySchedule() } returns flowOf(null)
        coEvery { sessionRepo.observeInProgress() } returns flowOf(
            com.example.fitlog.core.model.WorkoutSession(id = 1, status = com.example.fitlog.core.model.WorkoutStatus.IN_PROGRESS)
        )
        val vm = TodayViewModel(scheduleRepo, sessionRepo)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.first()
        assertTrue(state.hasInProgressWorkout)
        assertEquals(1L, state.inProgressSessionId)
    }
}
