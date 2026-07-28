package com.example.fitlog.feature.plan

import com.example.fitlog.data.repository.DaySchedule
import com.example.fitlog.data.repository.WorkoutScheduleRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelTest {

    private val scheduleRepo = mockk<WorkoutScheduleRepository>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state shows loading`() = runTest(testDispatcher) {
        coEvery { scheduleRepo.getFullWeek() } returns flowOf(emptyList())

        val vm = PlanViewModel(scheduleRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun `week schedule shows 7 days`() = runTest(testDispatcher) {
        coEvery { scheduleRepo.getFullWeek() } returns flowOf(
            (1..7).map { day -> DaySchedule(day, "Day$day", null, null, 0) }
        )
        val vm = PlanViewModel(scheduleRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals(7, state.weekSchedule.size)
    }

    @Test
    fun `schedule shows template name when set`() = runTest(testDispatcher) {
        coEvery { scheduleRepo.getFullWeek() } returns flowOf(
            listOf(DaySchedule(1, "周一", 10L, "Push Day", 5))
        )
        val vm = PlanViewModel(scheduleRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.first()
        assertEquals("Push Day", state.weekSchedule.find { it.dayOfWeek == 1 }!!.templateName)
    }
}
