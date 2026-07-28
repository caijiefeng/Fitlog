package com.example.fitlog.feature.today

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import org.junit.Rule
import org.junit.Test

class TodayScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun todayScreen_displaysTitleAndPlaceholderContent() {
        composeTestRule.setContent {
            FitLogTheme {
                TodayScreen(viewModel = TodayViewModel())
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
