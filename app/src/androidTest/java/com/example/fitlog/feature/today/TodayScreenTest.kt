package com.example.fitlog.feature.today

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.data.repository.WorkoutScheduleRepository
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

    private val mockScheduleRepo = mockk<WorkoutScheduleRepository>(relaxed = true)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun todayScreen_displaysTitleAndPlaceholderContent() {
        composeTestRule.setContent {
            FitLogTheme {
                TodayScreen(viewModel = TodayViewModel(mockScheduleRepo))
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
