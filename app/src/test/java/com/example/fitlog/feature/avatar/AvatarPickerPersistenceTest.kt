package com.example.fitlog.feature.avatar

import com.example.fitlog.data.repository.UserProfileRepository
import com.example.fitlog.domain.avatar.AvatarType
import com.example.fitlog.domain.avatar.BuiltInAvatar
import com.example.fitlog.domain.body.ActivityLevel
import com.example.fitlog.domain.body.GoalType
import com.example.fitlog.domain.body.UserProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

/**
 * The picker works in two phases: tapping an avatar only updates the
 * preview; the selection is persisted when the user taps save. Once saved,
 * the state carries the new key so the profile page refreshes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AvatarPickerPersistenceTest {

    private val repository = mockk<UserProfileRepository>(relaxed = true)
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun profile(avatarType: AvatarType = AvatarType.DEFAULT, key: String? = null) =
        UserProfile(
            gender = "male",
            birthday = LocalDate.of(2000, 1, 1),
            activityLevel = ActivityLevel.ACTIVE,
            goalType = GoalType.MUSCLE_GAIN,
            avatarType = avatarType,
            avatarKey = key,
        )

    @Test
    fun `selecting a built-in updates the preview without persisting`() =
        runTest(dispatcher) {
            coEvery { repository.observe() } returns flowOf(null)
            val viewModel = AvatarPickerViewModel(repository)
            advanceUntilIdle()

            viewModel.selectBuiltIn(BuiltInAvatar.byKey("kobe")!!)

            val state = viewModel.uiState.value
            assertEquals(AvatarType.BUILT_IN, state.avatarType)
            assertEquals("kobe", state.avatarKey)
            assertFalse(state.saved)
            coVerify(exactly = 0) { repository.updateAvatar(any(), any(), any()) }
        }

    @Test
    fun `save persists the pending built-in selection`() = runTest(dispatcher) {
        coEvery { repository.observe() } returns flowOf(null)
        val viewModel = AvatarPickerViewModel(repository)
        advanceUntilIdle()

        viewModel.selectBuiltIn(BuiltInAvatar.byKey("lebron")!!)
        viewModel.save()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateAvatar(AvatarType.BUILT_IN, "lebron", null)
        }
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `save persists the pending custom photo`() = runTest(dispatcher) {
        coEvery { repository.observe() } returns flowOf(null)
        val viewModel = AvatarPickerViewModel(repository)
        advanceUntilIdle()

        viewModel.selectCustom("avatars/photo_123.jpg")
        viewModel.save()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateAvatar(AvatarType.CUSTOM, null, "avatars/photo_123.jpg")
        }
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `viewmodel loads the stored profile into the preview`() = runTest(dispatcher) {
        coEvery { repository.observe() } returns flowOf(profile(AvatarType.BUILT_IN, "curry"))
        val viewModel = AvatarPickerViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AvatarType.BUILT_IN, state.avatarType)
        assertEquals("curry", state.avatarKey)
    }

    @Test
    fun `picking another avatar after saving re-enables the save flow`() =
        runTest(dispatcher) {
            coEvery { repository.observe() } returns flowOf(null)
            val viewModel = AvatarPickerViewModel(repository)
            advanceUntilIdle()

            viewModel.selectBuiltIn(BuiltInAvatar.byKey("kobe")!!)
            viewModel.save()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.saved)

            viewModel.selectBuiltIn(BuiltInAvatar.byKey("messi")!!)
            val state = viewModel.uiState.value
            assertEquals("messi", state.avatarKey)
            assertFalse(state.saved)
        }
}
