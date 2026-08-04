package com.example.fitlog.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.fitlog.core.datastore.UserPreferences
import com.example.fitlog.core.datastore.UserPreferencesRepository
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.core.designsystem.theme.StarThemeId
import com.example.fitlog.data.repository.BodyMeasurementRepository
import com.example.fitlog.data.repository.UserProfileRepository
import com.example.fitlog.domain.avatar.AvatarType
import com.example.fitlog.domain.body.ActivityLevel
import com.example.fitlog.domain.body.GoalType
import com.example.fitlog.domain.body.UserProfile
import com.example.fitlog.feature.profile.ProfileScreen
import com.example.fitlog.feature.profile.ProfileViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * 球星主题截图回归：Kobe 主题下的个人页（浅色 + 深色）。
 * 新增基准图，不影响现有默认主题基准。
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class StarThemeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun profileViewModel(): ProfileViewModel {
        val prefsRepo = mockk<UserPreferencesRepository>(relaxed = true)
        val profileRepo = mockk<UserProfileRepository>(relaxed = true)
        val bodyRepo = mockk<BodyMeasurementRepository>(relaxed = true)
        every { prefsRepo.preferences } returns flowOf(UserPreferences())
        coEvery { profileRepo.observe() } returns flowOf(
            UserProfile(
                gender = "male",
                birthday = LocalDate.of(2000, 1, 1),
                activityLevel = ActivityLevel.ACTIVE,
                goalType = GoalType.MUSCLE_GAIN,
                heightCm = 178.0,
                avatarType = AvatarType.BUILT_IN,
                avatarKey = "kobe",
            ),
        )
        coEvery { bodyRepo.getLatestOnOrBefore(any()) } returns null
        return ProfileViewModel(prefsRepo, profileRepo, bodyRepo)
    }

    @Test
    fun profile_kobe_theme_light() = capture("profile_kobe_light_412") {
        FitLogTheme(starThemeId = StarThemeId.KOBE) {
            ProfileScreen(viewModel = profileViewModel())
        }
    }

    @Test
    fun profile_kobe_theme_dark() = capture("profile_kobe_dark_412") {
        FitLogTheme(darkTheme = true, starThemeId = StarThemeId.KOBE) {
            ProfileScreen(viewModel = profileViewModel())
        }
    }

    private fun capture(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent { content() }
        composeRule.onRoot().captureRoboImage()
    }
}
