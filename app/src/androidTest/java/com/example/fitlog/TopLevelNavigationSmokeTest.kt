package com.example.fitlog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class TopLevelNavigationSmokeTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun navigateToAllTabs_processStaysAlive() {
        composeTestRule.setContent {
            FitLogApp()
        }

        // Today tab (start destination)
        composeTestRule.onNodeWithText("今日").assertExists()
        composeTestRule.onNodeWithText("今日暂无训练计划").assertIsDisplayed()

        // Plan tab
        composeTestRule.onNodeWithText("计划").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("还没有训练计划").assertIsDisplayed()

        // Record tab
        composeTestRule.onNodeWithText("记录").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("还没有训练记录").assertIsDisplayed()

        // Progress tab
        composeTestRule.onNodeWithText("进度").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("训练统计").assertIsDisplayed()

        // Profile tab
        composeTestRule.onNodeWithText("我的").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("设置").assertIsDisplayed()

        // Verify we can navigate back to Today without crashing
        composeTestRule.onNodeWithText("今日").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("今日暂无训练计划").assertIsDisplayed()
    }
}
