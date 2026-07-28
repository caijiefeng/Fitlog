package com.example.fitlog.core.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.fitlog.FitLogApp
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NavigationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun bottomNav_hasAllFiveTabs() {
        composeTestRule.setContent {
            FitLogApp()
        }

        composeTestRule
            .onNodeWithText("今日")
            .assertExists()

        composeTestRule
            .onNodeWithText("计划")
            .assertExists()

        composeTestRule
            .onNodeWithText("记录")
            .assertExists()

        composeTestRule
            .onNodeWithText("进度")
            .assertExists()

        composeTestRule
            .onNodeWithText("我的")
            .assertExists()
    }

    @Test
    fun navigateToPlan_tab_showsPlanScreen() {
        composeTestRule.setContent {
            FitLogApp()
        }

        composeTestRule
            .onNodeWithText("计划")
            .performClick()

        composeTestRule
            .onNodeWithText("还没有训练计划")
            .assertExists()
    }

    @Test
    fun navigateToProfile_tab_showsProfileScreen() {
        composeTestRule.setContent {
            FitLogApp()
        }

        composeTestRule
            .onNodeWithText("我的")
            .performClick()

        composeTestRule
            .onNodeWithText("设置")
            .assertExists()
    }
}
