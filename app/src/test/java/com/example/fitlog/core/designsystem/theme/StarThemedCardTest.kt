package com.example.fitlog.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.fitlog.core.designsystem.component.StarCardEmphasis
import com.example.fitlog.core.designsystem.component.StarThemedCard
import com.example.fitlog.feature.checkin.checkInSaveButtonColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * 打卡/主题卡片行为：
 * - 卡片容器颜色随球星主题变化
 * - 保存按钮文字使用 onAccent
 * - 错误/成功语义色不被球星色覆盖
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class StarThemedCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `star themed card container changes with theme`() {
        var container: Color = Color.Unspecified
        var motif = StarMotif.NONE
        var useKobe by mutableStateOf(false)

        composeRule.setContent {
            FitLogTheme(
                profile = if (useKobe) {
                    starVisualProfiles[StarVisualIdentity.KOBE_LAKERS]!!
                } else {
                    defaultStarVisualProfile
                },
            ) {
                container = FitLogAccentContainer
                motif = LocalStarVisualProfile.current.motif
                Text("x")
            }
        }
        composeRule.waitForIdle()
        val defaultContainer = container

        useKobe = true
        composeRule.waitForIdle()

        assertNotEquals("卡片容器颜色应随主题变化", defaultContainer, container)
        assertNotEquals("Kobe 卡片应携带 motif", StarMotif.NONE, motif)
        assertEquals(StarMotif.MAMBA_SCALE, motif)
    }

    @Test
    fun `kobe themed card uses kobe accent container`() {
        var container: Color = Color.Unspecified
        var accent: Color = Color.Unspecified
        composeRule.setContent {
            FitLogTheme(profile = starVisualProfiles[StarVisualIdentity.KOBE_LAKERS]!!) {
                container = LocalStarVisualProfile.current.lightColors.primaryContainer
                accent = MaterialTheme.colorScheme.primary
                Text("x")
            }
        }
        composeRule.waitForIdle()
        assertEquals(Color(0xFFE9E0F5), container)
        assertEquals(Color(0xFF4B2A73), accent)
    }

    @Test
    fun `check-in save button uses accent container and onAccent content`() {
        var container: Color = Color.Unspecified
        var content: Color = Color.Unspecified
        composeRule.setContent {
            FitLogTheme {
                val colors = checkInSaveButtonColors()
                container = colors.containerColor
                content = colors.contentColor
                Text("x")
            }
        }
        composeRule.waitForIdle()
        assertEquals(Color(0xFF287867), container)   // 默认主题主色
        assertEquals(Color(0xFFFFFFFF), content)    // onAccent 白
    }

    @Test
    fun `check-in save button follows star theme`() {
        var container: Color = Color.Unspecified
        var content: Color = Color.Unspecified
        composeRule.setContent {
            FitLogTheme(profile = starVisualProfiles[StarVisualIdentity.KOBE_LAKERS]!!) {
                val colors = checkInSaveButtonColors()
                container = colors.containerColor
                content = colors.contentColor
                Text("x")
            }
        }
        composeRule.waitForIdle()
        assertEquals(Color(0xFF4B2A73), container)   // Kobe 主色
        assertEquals(Color(0xFFFFFFFF), content)    // Kobe onAccent
    }

    @Test
    fun `error and success stay semantic under star themes`() {
        var error: Color = Color.Unspecified
        var success: Color = Color.Unspecified
        composeRule.setContent {
            FitLogTheme(profile = starVisualProfiles[StarVisualIdentity.NEYMAR_BRAZIL]!!) {
                error = MaterialTheme.colorScheme.error
                success = com.example.fitlog.core.designsystem.theme.FitLogSuccess
                Text("x")
            }
        }
        composeRule.waitForIdle()
        assertEquals(LightFitLogColors.error, error)
        assertEquals(LightFitLogColors.success, success)
    }
}

