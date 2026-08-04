package com.example.fitlog.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.fitlog.core.datastore.UserPreferencesRepository
import com.example.fitlog.data.repository.UserProfileRepository
import com.example.fitlog.domain.avatar.AvatarType

/**
 * 把一套 [FitLogColorScheme] 映射为 Material 3 [ColorScheme]。
 * 球星主题只影响品牌色（primary/secondary 家族），
 * 背景、表面与 error/success/warning 等语义色始终来自 FitLog 中性配色。
 */
private fun buildColorScheme(colors: FitLogColorScheme, dark: Boolean): ColorScheme {
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        primaryContainer = colors.accentContainer,
        onPrimaryContainer = colors.textPrimary,
        secondary = colors.accentVariant,
        onSecondary = colors.onAccentVariant,
        secondaryContainer = colors.accentVariantContainer,
        onSecondaryContainer = colors.textPrimary,
        tertiary = colors.textTertiary,
        onTertiary = colors.textPrimary,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.surfaceVariant,
        onSurfaceVariant = colors.textSecondary,
        outline = colors.divider,
        outlineVariant = colors.divider,
        error = colors.error,
        onError = colors.textPrimary,
        errorContainer = colors.errorContainer,
        onErrorContainer = colors.textPrimary,
    )
}

/** 把球星品牌色应用到 FitLog 配色（只覆盖品牌相关 token）。 */
private fun FitLogColorScheme.withBrand(brand: StarBrandColors): FitLogColorScheme = copy(
    accent = brand.primary,
    onAccent = brand.onPrimary,
    accentVariant = brand.secondary,
    onAccentVariant = brand.onSecondary,
    accentContainer = brand.primaryContainer,
    accentVariantContainer = brand.secondaryContainer,
)

@Composable
fun FitLogTheme(
    darkTheme: Boolean = false,
    profile: StarVisualProfile = defaultStarVisualProfile,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) DarkFitLogColors else LightFitLogColors
    val brand = if (darkTheme) profile.darkColors else profile.lightColors
    val colors = base.withBrand(brand)
    val colorScheme = buildColorScheme(colors, darkTheme)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalFitLogColors provides colors,
        LocalStarVisualProfile provides profile,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FitLogTypography,
            content = content
        )
    }
}

/**
 * Top-level entry point：观察主题偏好（明暗模式）与用户头像（球星主题），
 * 在 App 根部统一解析并应用主题。
 *
 * - [UserPreferencesRepository.preferences] 负责 ThemeMode（明暗）
 * - [UserProfileRepository.observe] 负责 StarThemeId（品牌色）
 * - 两者相互独立：球星主题不修改 ThemeMode，明暗模式不修改 StarThemeId
 */
@Composable
fun FitLogAppTheme(
    preferencesRepository: UserPreferencesRepository,
    userProfileRepository: UserProfileRepository,
    content: @Composable () -> Unit,
) {
    val preferences by preferencesRepository.preferences.collectAsState(
        initial = com.example.fitlog.core.datastore.UserPreferences()
    )
    val profile by userProfileRepository.observe().collectAsState(initial = null)
    val themeMode = preferences.themeMode

    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val starProfile = resolveStarVisualProfile(
        avatarType = profile?.avatarType ?: AvatarType.DEFAULT,
        avatarKey = profile?.avatarKey,
    )

    FitLogTheme(darkTheme = isDark, profile = starProfile, content = content)
}
