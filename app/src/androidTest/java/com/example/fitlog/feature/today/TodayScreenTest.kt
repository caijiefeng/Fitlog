package com.example.fitlog.feature.today

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class TodayScreenTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockCalendarRepo = mockk<com.example.fitlog.data.repository.CalendarRepository>(relaxed = true)
    private val mockSessionRepo = mockk<com.example.fitlog.data.repository.WorkoutSessionRepository>(relaxed = true)
    private val mockDateProvider = mockk<com.example.fitlog.core.time.CurrentDateProvider>(relaxed = true)
    private val mockProgressRepo = mockk<com.example.fitlog.data.repository.ProgressRepository>(relaxed = true)
    private val mockFoodRepo = mockk<com.example.fitlog.data.repository.FoodRecordRepository>(relaxed = true)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun todayScreen_displaysTitleAndPlaceholderContent() {
        composeTestRule.setContent {
            FitLogTheme {
                TodayScreen(viewModel = TodayViewModel(mockCalendarRepo, mockSessionRepo, mockProgressRepo, mockFoodRepo, mockDateProvider))
            }
        }

        composeTestRule
            .onNodeWithText("今日")
            .assertExists()

        composeTestRule
            .onNodeWithText("今日暂无训练计划")
            .assertExists()

        composeTestRule
            .onNodeWithText("快速开始训练")
            .assertExists()
    }
}
