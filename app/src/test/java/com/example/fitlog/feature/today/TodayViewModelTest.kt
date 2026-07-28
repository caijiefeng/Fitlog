package com.example.fitlog.feature.today

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TodayViewModelTest {

    @Test
    fun `initial UI state has correct defaults`() = runTest {
        val viewModel = TodayViewModel()

        val state = viewModel.uiState.first()

        assertFalse(state.hasWorkoutToday)
        assertNull(state.todayWorkoutName)
        assertNull(state.todayWorkoutProgress)
    }
}
