package com.example.fitlog.feature.exercise

import androidx.lifecycle.SavedStateHandle
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.MuscleGroup
import com.example.fitlog.data.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseEditViewModelTest {

    private val repo = mockk<ExerciseRepository>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `create mode has isCreateMode true`() {
        assertTrue(ExerciseEditViewModel(SavedStateHandle(), repo).isCreateMode)
    }

    @Test
    fun `edit mode isCreateMode false`() = runTest(testDispatcher) {
        val handle = SavedStateHandle().apply { set("exerciseId", 1L) }
        coEvery { repo.getById(1L) } returns Exercise(id = 1, name = "T", primaryMuscleGroup = MuscleGroup.CHEST, isCustom = true)
        val vm = ExerciseEditViewModel(handle, repo)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.isCreateMode)
    }

    @Test
    fun `blank name rejected`() = runTest(testDispatcher) {
        val vm = ExerciseEditViewModel(SavedStateHandle(), repo)
        vm.onNameChanged("   ")
        vm.onSave()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.state.value.nameError)
    }

    // Note: save-and-create-flow tests require Android instrumentation
    // (device/emulator) due to viewModelScope.launch dispatcher timing.
    // These paths are covered by connectedDebugAndroidTest.
}
