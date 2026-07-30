package com.example.fitlog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MainActivityLaunchTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivity_displaysTodayTitle() {
        composeTestRule
            .onNodeWithText("今日")
            .assertExists()
    }

    @Test
    fun mainActivity_noNestedScrollCrash() {
        // Verify the Today screen renders without a nested scroll crash
        // by asserting the empty-state content is displayed.
        composeTestRule
            .onNodeWithText("今日暂无训练计划")
            .assertIsDisplayed()
    }
}
