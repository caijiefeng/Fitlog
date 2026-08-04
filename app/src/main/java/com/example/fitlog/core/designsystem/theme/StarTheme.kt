package com.example.fitlog.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.example.fitlog.domain.avatar.AvatarType
import com.example.fitlog.domain.avatar.BuiltInAvatar

/**
 * 球星主题 ID。由用户保存的内置球星头像派生，不单独持久化——
 * 避免头像与主题双份状态不一致。
 */
enum class StarThemeId {
    DEFAULT,
    KOBE,
    LEBRON,
    DURANT,
    CURRY,
    JORDAN,
    HARDEN,
    IRVING,
    GEORGE,
    WESTBROOK,
    RONALDO,
    MESSI,
    MBAPPE,
    NEYMAR,
}

/**
 * 球星品牌色。均为"球星灵感配色"，不含球队 Logo 或商标。
 *
 * - [primary]：主题主色（主按钮、底部导航选中、强调元素）
 * - [onPrimary]：主色之上的文字/图标颜色
 * - [secondary]：主题辅助色（双配色强调、次级品牌区域）
 * - [onSecondary]：辅助色之上的文字/图标颜色（亮色辅助色须配深色文字）
 * - [primaryContainer]：主色浅色/暗色容器（选中背景、指示器）
 * - [secondaryContainer]：辅助色容器
 */
data class StarBrandColors(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val primaryContainer: Color,
    val secondaryContainer: Color,
)

/**
 * 由头像类型与 key 解析当前球星主题。
 *
 * - 仅 [AvatarType.BUILT_IN] 启用球星主题
 * - key 先经 [BuiltInAvatar.byKey] 解析（兼容 legacy key）
 * - CUSTOM / DEFAULT / null / 未知 key → [StarThemeId.DEFAULT]
 */
fun resolveStarTheme(
    avatarType: AvatarType,
    avatarKey: String?,
): StarThemeId {
    if (avatarType != AvatarType.BUILT_IN) return StarThemeId.DEFAULT
    val builtIn = BuiltInAvatar.byKey(avatarKey) ?: return StarThemeId.DEFAULT
    return when (builtIn.key) {
        "kobe" -> StarThemeId.KOBE
        "lebron" -> StarThemeId.LEBRON
        "durant" -> StarThemeId.DURANT
        "curry" -> StarThemeId.CURRY
        "jordan" -> StarThemeId.JORDAN
        "harden" -> StarThemeId.HARDEN
        "irving" -> StarThemeId.IRVING
        "george" -> StarThemeId.GEORGE
        "westbrook" -> StarThemeId.WESTBROOK
        "ronaldo" -> StarThemeId.RONALDO
        "messi" -> StarThemeId.MESSI
        "mbappe" -> StarThemeId.MBAPPE
        "neymar" -> StarThemeId.NEYMAR
        else -> StarThemeId.DEFAULT
    }
}

/**
 * 每球星明暗双套品牌色。
 *
 * 规则：
 * - 浅色模式：太亮的黄/白不做浅色主按钮底色；亮色作为辅助色时 onSecondary 用深色
 * - 暗色模式：主色提亮；容器色使用低饱和、低亮度版本
 * - 背景与卡片保持 FitLog 中性风格，只做轻微主题色倾向
 */
object StarThemePalettes {

    fun light(id: StarThemeId): StarBrandColors = when (id) {
        StarThemeId.DEFAULT -> StarBrandColors(
            primary = Color(0xFF287867),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF1E5C4F),
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDCEFE9),
            secondaryContainer = Color(0xFFDCEFE9),
        )
        StarThemeId.KOBE -> StarBrandColors(
            primary = Color(0xFF552583),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFB8860B),
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE9E0F5),
            secondaryContainer = Color(0xFFFDF3D8),
        )
        StarThemeId.LEBRON -> StarBrandColors(
            primary = Color(0xFF6F263D),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFC99700),
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF3E0E6),
            secondaryContainer = Color(0xFFFDF1D3),
        )
        StarThemeId.DURANT -> StarBrandColors(
            primary = Color(0xFF1D428A),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFE56020),
            onSecondary = Color(0xFF2A0F02),
            primaryContainer = Color(0xFFDCE6F5),
            secondaryContainer = Color(0xFFFCE7D9),
        )
        StarThemeId.CURRY -> StarBrandColors(
            primary = Color(0xFF1D428A),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFD9A400),
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDCE6F5),
            secondaryContainer = Color(0xFFFCF1CF),
        )
        StarThemeId.JORDAN -> StarBrandColors(
            primary = Color(0xFFCE1141),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF3D3D3D),
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF9DCE3),
            secondaryContainer = Color(0xFFE6E6E6),
        )
        StarThemeId.HARDEN -> StarBrandColors(
            primary = Color(0xFFBA0C2F),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF6B7A82),
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF9DCE3),
            secondaryContainer = Color(0xFFE8EEF1),
        )
        StarThemeId.IRVING -> StarBrandColors(
            primary = Color(0xFF007A33),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF8A6D3B),
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD9F0E2),
            secondaryContainer = Color(0xFFF2E9D8),
        )
        StarThemeId.GEORGE -> StarBrandColors(
            primary = Color(0xFF1D428A),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFB0102A),
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDCE6F5),
            secondaryContainer = Color(0xFFF9DEE2),
        )
        StarThemeId.WESTBROOK -> StarBrandColors(
            primary = Color(0xFFEF3B24),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF002D62),
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFDE3DC),
            secondaryContainer = Color(0xFFDCE4F5),
        )
        StarThemeId.RONALDO -> StarBrandColors(
            primary = Color(0xFFC8102E),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFC7A400),
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF9DCE3),
            secondaryContainer = Color(0xFFFCF1CF),
        )
        StarThemeId.MESSI -> StarBrandColors(
            primary = Color(0xFF2E6FA3),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF4A6E8C),
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE3EFFA),
            secondaryContainer = Color(0xFFE9F1F8),
        )
        StarThemeId.MBAPPE -> StarBrandColors(
            primary = Color(0xFF001E62),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFD00612),
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDCE4F5),
            secondaryContainer = Color(0xFFF9DEE0),
        )
        StarThemeId.NEYMAR -> StarBrandColors(
            primary = Color(0xFFC9A800),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF007A33),
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFCF3CF),
            secondaryContainer = Color(0xFFD9F0E2),
        )
    }

    fun dark(id: StarThemeId): StarBrandColors = when (id) {
        StarThemeId.DEFAULT -> StarBrandColors(
            primary = Color(0xFF5BBFA4),
            onPrimary = Color(0xFF0E1A16),
            secondary = Color(0xFF3E9C85),
            onSecondary = Color(0xFF0E1A16),
            primaryContainer = Color(0xFF1F4D40),
            secondaryContainer = Color(0xFF1F4D40),
        )
        StarThemeId.KOBE -> StarBrandColors(
            primary = Color(0xFFA78BDA),
            onPrimary = Color(0xFF1A0F2E),
            secondary = Color(0xFFFDB927),
            onSecondary = Color(0xFF201503),
            primaryContainer = Color(0xFF3A2A55),
            secondaryContainer = Color(0xFF4A3A12),
        )
        StarThemeId.LEBRON -> StarBrandColors(
            primary = Color(0xFFD46A85),
            onPrimary = Color(0xFF2A0D16),
            secondary = Color(0xFFFFC94D),
            onSecondary = Color(0xFF201500),
            primaryContainer = Color(0xFF4A2631),
            secondaryContainer = Color(0xFF4A3A12),
        )
        StarThemeId.DURANT -> StarBrandColors(
            primary = Color(0xFF7FA3E0),
            onPrimary = Color(0xFF0E1E3A),
            secondary = Color(0xFFFF8A4D),
            onSecondary = Color(0xFF2A1002),
            primaryContainer = Color(0xFF22335A),
            secondaryContainer = Color(0xFF4A2A17),
        )
        StarThemeId.CURRY -> StarBrandColors(
            primary = Color(0xFF7FA3E0),
            onPrimary = Color(0xFF0E1E3A),
            secondary = Color(0xFFFFD84D),
            onSecondary = Color(0xFF201500),
            primaryContainer = Color(0xFF22335A),
            secondaryContainer = Color(0xFF4A3A12),
        )
        StarThemeId.JORDAN -> StarBrandColors(
            primary = Color(0xFFFF6B8E),
            onPrimary = Color(0xFF3A0612),
            secondary = Color(0xFFBDBDBD),
            onSecondary = Color(0xFF1A1A1A),
            primaryContainer = Color(0xFF4A1A28),
            secondaryContainer = Color(0xFF333333),
        )
        StarThemeId.HARDEN -> StarBrandColors(
            primary = Color(0xFFFF6B85),
            onPrimary = Color(0xFF3A0612),
            secondary = Color(0xFFD7DEE3),
            onSecondary = Color(0xFF1A1F22),
            primaryContainer = Color(0xFF4A1A24),
            secondaryContainer = Color(0xFF30373B),
        )
        StarThemeId.IRVING -> StarBrandColors(
            primary = Color(0xFF4DCC85),
            onPrimary = Color(0xFF0A2E1A),
            secondary = Color(0xFFD6B87E),
            onSecondary = Color(0xFF241A08),
            primaryContainer = Color(0xFF1D402C),
            secondaryContainer = Color(0xFF3D3423),
        )
        StarThemeId.GEORGE -> StarBrandColors(
            primary = Color(0xFF7FA3E0),
            onPrimary = Color(0xFF0E1E3A),
            secondary = Color(0xFFFF6078),
            onSecondary = Color(0xFF3A0612),
            primaryContainer = Color(0xFF22335A),
            secondaryContainer = Color(0xFF4A1A24),
        )
        StarThemeId.WESTBROOK -> StarBrandColors(
            primary = Color(0xFFFF7A5C),
            onPrimary = Color(0xFF3A0E02),
            secondary = Color(0xFF7FA3E0),
            onSecondary = Color(0xFF0E1E3A),
            primaryContainer = Color(0xFF4A2A1F),
            secondaryContainer = Color(0xFF22335A),
        )
        StarThemeId.RONALDO -> StarBrandColors(
            primary = Color(0xFFFF6B85),
            onPrimary = Color(0xFF3A0612),
            secondary = Color(0xFFFFD84D),
            onSecondary = Color(0xFF201500),
            primaryContainer = Color(0xFF4A1A24),
            secondaryContainer = Color(0xFF4A3A12),
        )
        StarThemeId.MESSI -> StarBrandColors(
            primary = Color(0xFF8FC1E8),
            onPrimary = Color(0xFF0E2438),
            secondary = Color(0xFFE8F1FA),
            onSecondary = Color(0xFF0E2438),
            primaryContainer = Color(0xFF26405C),
            secondaryContainer = Color(0xFF2C3A48),
        )
        StarThemeId.MBAPPE -> StarBrandColors(
            primary = Color(0xFF6B8FD8),
            onPrimary = Color(0xFF0A1430),
            secondary = Color(0xFFFF5A66),
            onSecondary = Color(0xFF3A0508),
            primaryContainer = Color(0xFF16264A),
            secondaryContainer = Color(0xFF4A1A1E),
        )
        StarThemeId.NEYMAR -> StarBrandColors(
            primary = Color(0xFFFFE14D),
            onPrimary = Color(0xFF241A00),
            secondary = Color(0xFF4DCC85),
            onSecondary = Color(0xFF0A2E1A),
            primaryContainer = Color(0xFF4A3E0E),
            secondaryContainer = Color(0xFF1D402C),
        )
    }
}
