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
    val accentContainer: Color,
    val onAccent: Color,
    /** 辅助色容器（球星主题 secondaryContainer） */
    val accentVariantContainer: Color,
    /** 辅助色之上的文字/图标颜色 */
    val onAccentVariant: Color,
    val divider: Color,
    val error: Color,
    val errorContainer: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
)

/**
 * V5.9 palette — light theme by default. Green is the single accent hue used
 * across the whole app; containers are soft tints for tonal surfaces/pills.
 */
val DarkFitLogColors = FitLogColorScheme(
    background = Color(0xFF111613),
    surface = Color(0xFF191F1C),
    surfaceVariant = Color(0xFF222925),
    card = Color(0xFF191F1C),
    textPrimary = Color(0xFFF2F5F3),
    textSecondary = Color(0xFFAAB4AF),
    textTertiary = Color(0xFF7F8C86),
    accent = Color(0xFF5BBFA4),
    accentVariant = Color(0xFF3E9C85),
    accentContainer = Color(0xFF1F4D40),
    onAccent = Color(0xFF0E1A16),
    accentVariantContainer = Color(0xFF1F4D40),
    onAccentVariant = Color(0xFF0E1A16),
    divider = Color(0xFF35403B),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF4C2B28),
    success = Color(0xFF8FD6A5),
    successContainer = Color(0xFF23482F),
    warning = Color(0xFFE3B26E),
    warningContainer = Color(0xFF4A3518),
)

val LightFitLogColors = FitLogColorScheme(
    background = Color(0xFFF4F6F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDF1EF),
    card = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF17201D),
    textSecondary = Color(0xFF64706C),
    textTertiary = Color(0xFF8A948F),
    accent = Color(0xFF287867),
    accentVariant = Color(0xFF1E5C4F),
    accentContainer = Color(0xFFDCEFE9),
    onAccent = Color(0xFFFFFFFF),
    accentVariantContainer = Color(0xFFDCEFE9),
    onAccentVariant = Color(0xFF1A3B33),
    divider = Color(0xFFDCE3E0),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    success = Color(0xFF2E7D4F),
    successContainer = Color(0xFFDCEFDF),
    warning = Color(0xFFB87320),
    warningContainer = Color(0xFFF9EBD8),
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
val FitLogAccentContainer: Color @Composable get() = LocalFitLogColors.current.accentContainer
val FitLogOnAccent: Color @Composable get() = LocalFitLogColors.current.onAccent
val FitLogAccentVariantContainer: Color @Composable get() = LocalFitLogColors.current.accentVariantContainer
val FitLogOnAccentVariant: Color @Composable get() = LocalFitLogColors.current.onAccentVariant
val FitLogDivider: Color @Composable get() = LocalFitLogColors.current.divider
val FitLogError: Color @Composable get() = LocalFitLogColors.current.error
val FitLogErrorContainer: Color @Composable get() = LocalFitLogColors.current.errorContainer
val FitLogSuccess: Color @Composable get() = LocalFitLogColors.current.success
val FitLogSuccessContainer: Color @Composable get() = LocalFitLogColors.current.successContainer
val FitLogWarning: Color @Composable get() = LocalFitLogColors.current.warning
val FitLogWarningContainer: Color @Composable get() = LocalFitLogColors.current.warningContainer
