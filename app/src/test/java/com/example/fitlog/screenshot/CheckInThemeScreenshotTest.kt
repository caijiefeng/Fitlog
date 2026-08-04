package com.example.fitlog.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.core.designsystem.theme.StarVisualIdentity
import com.example.fitlog.core.designsystem.theme.starVisualProfiles
import com.example.fitlog.data.repository.CheckInRepository
import com.example.fitlog.feature.checkin.CheckInCard
import com.example.fitlog.feature.checkin.CheckInViewModel
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
 * 每日打卡卡片在多个球星主题下的截图回归。
 * 覆盖：DEFAULT / KOBE / HARDEN / DURANT / RONALDO（浅色）。
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class CheckInThemeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun checkInViewModel(mood: Int? = null, energy: Int? = null): CheckInViewModel {
        val repo = mockk<CheckInRepository>(relaxed = true)
        val dateProvider = mockk<com.example.fitlog.core.time.CurrentDateProvider>(relaxed = true)
        every { dateProvider.today() } returns LocalDate.of(2026, 8, 4)
        coEvery { repo.observeByDate(any()) } returns flowOf(null)
        val vm = CheckInViewModel(repo, dateProvider)
        if (mood != null) vm.onMoodChange(mood)
        if (energy != null) vm.onEnergyLevelChange(energy)
        return vm
    }

    @Test
    fun checkin_default_theme() = capture("checkin_default_light_412") {
        FitLogTheme {
            CheckInCard(viewModel = checkInViewModel(mood = 4, energy = 4))
        }
    }

    @Test
    fun checkin_kobe_theme() = capture("checkin_kobe_light_412") {
        FitLogTheme(profile = starVisualProfiles[StarVisualIdentity.KOBE_LAKERS]!!) {
            CheckInCard(viewModel = checkInViewModel(mood = 4, energy = 4))
        }
    }

    @Test
    fun checkin_harden_theme() = capture("checkin_harden_light_412") {
        FitLogTheme(profile = starVisualProfiles[StarVisualIdentity.HARDEN_ROCKETS]!!) {
            CheckInCard(viewModel = checkInViewModel(mood = 4, energy = 4))
        }
    }

    @Test
    fun checkin_durant_theme() = capture("checkin_durant_light_412") {
        FitLogTheme(profile = starVisualProfiles[StarVisualIdentity.DURANT_NETS]!!) {
            CheckInCard(viewModel = checkInViewModel(mood = 4, energy = 4))
        }
    }

    @Test
    fun checkin_ronaldo_theme() = capture("checkin_ronaldo_light_412") {
        FitLogTheme(profile = starVisualProfiles[StarVisualIdentity.RONALDO_REAL_MADRID]!!) {
            CheckInCard(viewModel = checkInViewModel(mood = 4, energy = 4))
        }
    }

    private fun capture(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent { content() }
        composeRule.onRoot().captureRoboImage()
    }
}
