package com.example.fitlog.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.fitlog.core.datastore.UserPreferences
import com.example.fitlog.core.datastore.UserPreferencesRepository
import com.example.fitlog.data.repository.UserProfileRepository
import com.example.fitlog.domain.avatar.AvatarType
import com.example.fitlog.domain.body.ActivityLevel
import com.example.fitlog.domain.body.GoalType
import com.example.fitlog.domain.body.UserProfile
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * 根主题观察头像变化并重组：
 * 初始默认主题 → Repository 发出 avatarKey="kobe" → MaterialTheme
 * 的 primary 变为 Kobe 紫、secondary 变为 Kobe 金。
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class StarThemeComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun preferencesRepository(): UserPreferencesRepository {
        val repo = mockk<UserPreferencesRepository>(relaxed = true)
        every { repo.preferences } returns flowOf(UserPreferences())
        return repo
    }

    private fun profileRepository(flow: MutableStateFlow<UserProfile?>): UserProfileRepository {
        val repo = mockk<UserProfileRepository>(relaxed = true)
        every { repo.observe() } returns flow
        return repo
    }

    private fun profile(avatarType: AvatarType, avatarKey: String? = null) = UserProfile(
        gender = "male",
        birthday = LocalDate.of(2000, 1, 1),
        activityLevel = ActivityLevel.ACTIVE,
        goalType = GoalType.MAINTAIN,
        avatarType = avatarType,
        avatarKey = avatarKey,
    )

    @Test
    fun `root theme starts at default green and switches to kobe purple`() {
        val profileFlow = MutableStateFlow<UserProfile?>(null)
        var primary by mutableStateOf(Color.Unspecified)
        var secondary by mutableStateOf(Color.Unspecified)

        composeRule.setContent {
            FitLogAppTheme(
                preferencesRepository = preferencesRepository(),
                userProfileRepository = profileRepository(profileFlow),
            ) {
                primary = MaterialTheme.colorScheme.primary
                secondary = MaterialTheme.colorScheme.secondary
                Text("主题观察")
            }
        }
        composeRule.waitForIdle()

        // 初始：默认绿色主题
        assertEquals(Color(0xFF287867), primary)
        assertEquals(Color(0xFF1E5C4F), secondary)

        // 头像保存为 kobe → 全局重组为紫金
        profileFlow.value = profile(AvatarType.BUILT_IN, "kobe")
        composeRule.waitForIdle()

        assertEquals(Color(0xFF552583), primary)
        assertEquals(Color(0xFFB8860B), secondary)
    }

    @Test
    fun `custom avatar falls back to default theme`() {
        val profileFlow = MutableStateFlow<UserProfile?>(null)
        var primary by mutableStateOf(Color.Unspecified)

        composeRule.setContent {
            FitLogAppTheme(
                preferencesRepository = preferencesRepository(),
                userProfileRepository = profileRepository(profileFlow),
            ) {
                primary = MaterialTheme.colorScheme.primary
                Text("主题观察")
            }
        }
        composeRule.waitForIdle()

        profileFlow.value = profile(AvatarType.CUSTOM, "kobe")
        composeRule.waitForIdle()

        assertEquals(Color(0xFF287867), primary)
    }

    @Test
    fun `dark mode keeps the star theme id`() {
        val profileFlow = MutableStateFlow<UserProfile?>(null)
        val prefsFlow = MutableStateFlow(UserPreferences(themeMode = ThemeMode.DARK))
        val prefsRepo = mockk<UserPreferencesRepository>(relaxed = true)
        every { prefsRepo.preferences } returns prefsFlow
        var primary by mutableStateOf(Color.Unspecified)

        composeRule.setContent {
            FitLogAppTheme(
                preferencesRepository = prefsRepo,
                userProfileRepository = profileRepository(profileFlow),
            ) {
                primary = MaterialTheme.colorScheme.primary
                Text("主题观察")
            }
        }
        composeRule.waitForIdle()

        // 深色默认主题
        assertEquals(Color(0xFF5BBFA4), primary)

        // 深色 + kobe → Kobe 暗色紫
        profileFlow.value = profile(AvatarType.BUILT_IN, "kobe")
        composeRule.waitForIdle()
        assertEquals(Color(0xFFA78BDA), primary)
    }
}
