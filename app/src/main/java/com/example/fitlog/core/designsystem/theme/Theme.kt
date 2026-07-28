package com.example.fitlog.core.designsystem.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FitLogDarkColorScheme = darkColorScheme(
    primary = FitLogAccent,
    onPrimary = FitLogBackground,
    primaryContainer = FitLogAccentVariant,
    onPrimaryContainer = FitLogTextPrimary,
    secondary = FitLogTextSecondary,
    onSecondary = FitLogBackground,
    tertiary = FitLogTextTertiary,
    onTertiary = FitLogBackground,
    background = FitLogBackground,
    onBackground = FitLogTextPrimary,
    surface = FitLogSurface,
    onSurface = FitLogTextPrimary,
    surfaceVariant = FitLogSurfaceVariant,
    onSurfaceVariant = FitLogTextSecondary,
    outline = FitLogDivider,
    outlineVariant = FitLogDivider,
    error = FitLogError,
    onError = FitLogBackground,
)

@Composable
fun FitLogTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = FitLogDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FitLogTypography,
        content = content
    )
}
