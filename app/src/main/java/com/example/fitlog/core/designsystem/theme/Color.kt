package com.example.fitlog.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Theme Mode ──────────────────────────────────────────────────────────────

enum class ThemeMode { LIGHT, DARK, SYSTEM }

// ── Color Scheme ────────────────────────────────────────────────────────────

data class FitLogColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentVariant: Color,
    val divider: Color,
    val error: Color,
    val success: Color,
)

val DarkFitLogColors = FitLogColorScheme(
    background = Color(0xFF0D0D0D),
    surface = Color(0xFF0D0D0D),
    surfaceVariant = Color(0xFF1A1A1A),
    card = Color(0xFF1A1A1A),
    textPrimary = Color(0xFFF2F2F2),
    textSecondary = Color(0xFF999999),
    textTertiary = Color(0xFF666666),
    accent = Color(0xFF4CAF9B),
    accentVariant = Color(0xFF388E7C),
    divider = Color(0xFF2A2A2A),
    error = Color(0xFFCF6679),
    success = Color(0xFF81C784),
)

val LightFitLogColors = FitLogColorScheme(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF5F5F5),
    card = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF666666),
    textTertiary = Color(0xFF999999),
    accent = Color(0xFF388E7C),
    accentVariant = Color(0xFF2E7D6F),
    divider = Color(0xFFE0E0E0),
    error = Color(0xFFB00020),
    success = Color(0xFF4CAF50),
)

val LocalFitLogColors = staticCompositionLocalOf { DarkFitLogColors }

// ── Convenience vals (composable-aware, delegate to LocalFitLogColors) ──────

val FitLogBackground: Color @Composable get() = LocalFitLogColors.current.background
val FitLogSurface: Color @Composable get() = LocalFitLogColors.current.surface
val FitLogSurfaceVariant: Color @Composable get() = LocalFitLogColors.current.surfaceVariant
val FitLogCard: Color @Composable get() = LocalFitLogColors.current.card
val FitLogTextPrimary: Color @Composable get() = LocalFitLogColors.current.textPrimary
val FitLogTextSecondary: Color @Composable get() = LocalFitLogColors.current.textSecondary
val FitLogTextTertiary: Color @Composable get() = LocalFitLogColors.current.textTertiary
val FitLogAccent: Color @Composable get() = LocalFitLogColors.current.accent
val FitLogAccentVariant: Color @Composable get() = LocalFitLogColors.current.accentVariant
val FitLogDivider: Color @Composable get() = LocalFitLogColors.current.divider
val FitLogError: Color @Composable get() = LocalFitLogColors.current.error
val FitLogSuccess: Color @Composable get() = LocalFitLogColors.current.success
