package com.example.fitlog.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
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

private val FitLogDarkColorScheme = darkColorScheme(
    primary = DarkFitLogColors.accent,
    onPrimary = DarkFitLogColors.background,
    primaryContainer = DarkFitLogColors.accentVariant,
    onPrimaryContainer = DarkFitLogColors.textPrimary,
    secondary = DarkFitLogColors.textSecondary,
    onSecondary = DarkFitLogColors.background,
    tertiary = DarkFitLogColors.textTertiary,
    onTertiary = DarkFitLogColors.background,
    background = DarkFitLogColors.background,
    onBackground = DarkFitLogColors.textPrimary,
    surface = DarkFitLogColors.surface,
    onSurface = DarkFitLogColors.textPrimary,
    surfaceVariant = DarkFitLogColors.surfaceVariant,
    onSurfaceVariant = DarkFitLogColors.textSecondary,
    outline = DarkFitLogColors.divider,
    outlineVariant = DarkFitLogColors.divider,
    error = DarkFitLogColors.error,
    onError = DarkFitLogColors.background,
)

private val FitLogLightColorScheme = lightColorScheme(
    primary = LightFitLogColors.accent,
    onPrimary = LightFitLogColors.background,
    primaryContainer = LightFitLogColors.accentVariant,
    onPrimaryContainer = LightFitLogColors.textPrimary,
    secondary = LightFitLogColors.textSecondary,
    onSecondary = LightFitLogColors.background,
    tertiary = LightFitLogColors.textTertiary,
    onTertiary = LightFitLogColors.background,
    background = LightFitLogColors.background,
    onBackground = LightFitLogColors.textPrimary,
    surface = LightFitLogColors.surface,
    onSurface = LightFitLogColors.textPrimary,
    surfaceVariant = LightFitLogColors.surfaceVariant,
    onSurfaceVariant = LightFitLogColors.textSecondary,
    outline = LightFitLogColors.divider,
    outlineVariant = LightFitLogColors.divider,
    error = LightFitLogColors.error,
    onError = LightFitLogColors.background,
)

@Composable
fun FitLogTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkFitLogColors else LightFitLogColors
    val colorScheme = if (darkTheme) FitLogDarkColorScheme else FitLogLightColorScheme
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

    CompositionLocalProvider(LocalFitLogColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FitLogTypography,
            content = content
        )
    }
}

/**
 * Top-level entry point that reads the user's theme preference from
 * [UserPreferencesRepository] and drives [FitLogTheme].
 */
@Composable
fun FitLogAppTheme(
    preferencesRepository: UserPreferencesRepository,
    content: @Composable () -> Unit,
) {
    val preferences by preferencesRepository.preferences.collectAsState(
        initial = com.example.fitlog.core.datastore.UserPreferences()
    )
    val themeMode = preferences.themeMode

    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    FitLogTheme(darkTheme = isDark, content = content)
}
