package com.example.fitlog.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.fitlog.domain.avatar.AvatarType
import com.example.fitlog.domain.avatar.BuiltInAvatar

/**
 * 球星视觉身份。每个身份对应头像所展示的时期/代表形象，
 * 配色为"球星灵感配色"，不含球队 Logo 或商标。
 */
enum class StarVisualIdentity {
    DEFAULT,

    KOBE_LAKERS,
    LEBRON_LAKERS,
    DURANT_NETS,
    CURRY_WARRIORS,
    JORDAN_BULLS,
    HARDEN_ROCKETS,
    IRVING_CURRENT,
    GEORGE_CLIPPERS,
    WESTBROOK_THUNDER,

    RONALDO_REAL_MADRID,
    MESSI_ARGENTINA,
    MBAPPE_FRANCE,
    NEYMAR_BRAZIL,
}

/**
 * 原创抽象图案元素（Canvas 绘制，不使用任何球队 Logo）。
 */
enum class StarMotif {
    NONE,
    MAMBA_SCALE,
    CROWN,
    NET_GRID,
    SPLASH_ARC,
    WINGS,
    ROCKET,
    LIGHTNING,
    SPEED_LINES,
    STAR_RAYS,
    FREESTYLE_ORBIT,
}

/** 主题强调角色：主色 / 辅助色（用于选中态与强调元素）。 */
enum class StarAccentRole {
    PRIMARY,
    SECONDARY,
}

/** 卡片/图形几何语言。 */
enum class StarGeometry {
    NEUTRAL,
    SHARP,
    HEAVY,
    MINIMAL,
    ROUNDED,
    DYNAMIC,
}

/** Text-safe treatment applied above a full-bleed star scene. */
enum class StarHeroOverlayStyle {
    DARK_BOTTOM,
    DARK_LEFT,
    DARK_RIGHT,
    CINEMATIC,
}

/**
 * 球星品牌色。
 *
 * - [primary]：主题主色（主按钮、底部导航选中、强调元素）
 * - [onPrimary]：主色之上的文字/图标颜色
 * - [secondary]：主题辅助色（双配色强调、次级品牌区域）
 * - [onSecondary]：辅助色之上的文字/图标颜色（亮色辅助色须配深色文字）
 * - [primaryContainer]：主色容器（选中背景、指示器）
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
 * 完整球星视觉身份：颜色 + 图案 + 号码 + 几何语言。
 * 页面和组件通过 [LocalStarVisualProfile] 读取，不得自行判断 avatarKey。
 */
data class StarVisualProfile(
    val identity: StarVisualIdentity,
    val lightColors: StarBrandColors,
    val darkColors: StarBrandColors,
    val motif: StarMotif,
    val jerseyNumber: String?,
    val geometry: StarGeometry,
    /** 图案透明度（0.04～0.12，不影响文字可读性） */
    val patternAlpha: Float,
    /** Optional owner-approved full-bleed scene art. Null uses the themed fallback. */
    @DrawableRes val homeBackgroundRes: Int? = null,
    @DrawableRes val profileBackgroundRes: Int? = null,
    val heroFocusX: Float = 0.5f,
    val heroFocusY: Float = 0.35f,
    val slogan: String? = null,
    val shortName: String? = null,
    val heroOverlayStyle: StarHeroOverlayStyle = StarHeroOverlayStyle.CINEMATIC,
)

/**
 * 解析流程：avatarKey → BuiltInAvatar.byKey() → visualIdentity → StarVisualProfile。
 * - 仅 [AvatarType.BUILT_IN] 启用球星主题
 * - key 经 [BuiltInAvatar.byKey] 解析（兼容 legacy key）
 * - CUSTOM / DEFAULT / null / 未知 key → [StarVisualIdentity.DEFAULT]
 */
fun resolveStarVisualIdentity(
    avatarType: AvatarType,
    avatarKey: String?,
): StarVisualIdentity {
    if (avatarType != AvatarType.BUILT_IN) return StarVisualIdentity.DEFAULT
    return BuiltInAvatar.byKey(avatarKey)?.visualIdentity ?: StarVisualIdentity.DEFAULT
}

fun resolveStarVisualProfile(
    avatarType: AvatarType,
    avatarKey: String?,
): StarVisualProfile = starVisualProfiles[resolveStarVisualIdentity(avatarType, avatarKey)]
    ?: defaultStarVisualProfile

val defaultStarVisualProfile = StarVisualProfile(
    identity = StarVisualIdentity.DEFAULT,
    lightColors = StarBrandColors(
        primary = Color(0xFF287867),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF1E5C4F),
        onSecondary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDCEFE9),
        secondaryContainer = Color(0xFFDCEFE9),
    ),
    darkColors = StarBrandColors(
        primary = Color(0xFF5BBFA4),
        onPrimary = Color(0xFF0E1A16),
        secondary = Color(0xFF3E9C85),
        onSecondary = Color(0xFF0E1A16),
        primaryContainer = Color(0xFF1F4D40),
        secondaryContainer = Color(0xFF1F4D40),
    ),
    motif = StarMotif.NONE,
    jerseyNumber = null,
    geometry = StarGeometry.NEUTRAL,
    patternAlpha = 0f,
)

/**
 * 每个视觉身份的完整档案。
 *
 * 规则：
 * - 浅色模式：亮黄/白不做浅色主按钮底色；亮色作辅助色时 onSecondary 用深色
 * - 暗色模式：主色提亮；容器色低饱和、低亮度
 * - 背景与卡片保持 FitLog 中性风格，只做轻微主题色倾向
 */
private data class StarHeroCopy(
    val shortName: String,
    val slogan: String,
    val overlayStyle: StarHeroOverlayStyle = StarHeroOverlayStyle.CINEMATIC,
)

private val starHeroCopy = mapOf(
    StarVisualIdentity.KOBE_LAKERS to StarHeroCopy("KOBE", "SECOND PLACE JUST MEANS YOU'RE THE FIRST LOSER."),
    StarVisualIdentity.LEBRON_LAKERS to StarHeroCopy("KING", "NO MATTER. I GOT NO WORDS."),
    StarVisualIdentity.DURANT_NETS to StarHeroCopy("KD", "GREATNESS DON'T SHAKE HIS HEAD.", StarHeroOverlayStyle.DARK_LEFT),
    StarVisualIdentity.CURRY_WARRIORS to StarHeroCopy("30", "YOU DON'T WANT TO SEE US NEXT YEAR."),
    StarVisualIdentity.JORDAN_BULLS to StarHeroCopy("23", "I CAN'T ACCEPT NOT TRYING."),
    StarVisualIdentity.HARDEN_ROCKETS to StarHeroCopy("13", "I JUST WANT TO WIN. WHATEVER IT TAKES."),
    StarVisualIdentity.IRVING_CURRENT to StarHeroCopy("11", "I'M BUILT FOR THESE MOMENTS."),
    StarVisualIdentity.GEORGE_CLIPPERS to StarHeroCopy("13", "I THRIVE UNDER PRESSURE."),
    StarVisualIdentity.WESTBROOK_THUNDER to StarHeroCopy("WHY NOT?", "WHY NOT?"),
    StarVisualIdentity.RONALDO_REAL_MADRID to StarHeroCopy("CR7", "I DON'T FOLLOW THE RECORDS. THE RECORDS FOLLOW ME."),
    StarVisualIdentity.MESSI_ARGENTINA to StarHeroCopy("10", "EVERY SEASON IS A NEW CHALLENGE."),
    StarVisualIdentity.MBAPPE_FRANCE to StarHeroCopy("10", "I ALWAYS WANT MORE."),
    StarVisualIdentity.NEYMAR_BRAZIL to StarHeroCopy("NEY", "1% CHANCE. 99% FAITH."),
)

val starVisualProfiles: Map<StarVisualIdentity, StarVisualProfile> = mapOf(
    // ── 篮球 ────────────────────────────────────────────────────────────
    StarVisualIdentity.KOBE_LAKERS to StarVisualProfile(
        identity = StarVisualIdentity.KOBE_LAKERS,
        lightColors = StarBrandColors(
            primary = Color(0xFF4B2A73),   // 深紫（偏黑、锋利）
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFB8860B), // 暗金（浅色下保证白字对比）
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE9E0F5),
            secondaryContainer = Color(0xFFFDF3D8),
        ),
        darkColors = StarBrandColors(
            primary = Color(0xFFA78BDA),
            onPrimary = Color(0xFF1A0F2E),
            secondary = Color(0xFFFDB927),
            onSecondary = Color(0xFF201503),
            primaryContainer = Color(0xFF3A2A55),
            secondaryContainer = Color(0xFF4A3A12),
        ),
        motif = StarMotif.MAMBA_SCALE,
        jerseyNumber = "24",
        geometry = StarGeometry.SHARP,
        patternAlpha = 0.07f,
    ),
    StarVisualIdentity.LEBRON_LAKERS to StarVisualProfile(
        identity = StarVisualIdentity.LEBRON_LAKERS,
        lightColors = StarBrandColors(
            primary = Color(0xFF5C3A9E),   // 皇家紫（比 Kobe 更亮、更厚重）
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFC99700), // 暖金
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFEAE0F7),
            secondaryContainer = Color(0xFFFDF1D3),
        ),
        darkColors = StarBrandColors(
            primary = Color(0xFFB49AE0),
            onPrimary = Color(0xFF1A0F2E),
            secondary = Color(0xFFFFC94D),
            onSecondary = Color(0xFF201500),
            primaryContainer = Color(0xFF3F2A66),
            secondaryContainer = Color(0xFF4A3A12),
        ),
        motif = StarMotif.CROWN,
        jerseyNumber = "23",
        geometry = StarGeometry.HEAVY,
        patternAlpha = 0.06f,
    ),
    StarVisualIdentity.DURANT_NETS to StarVisualProfile(
        identity = StarVisualIdentity.DURANT_NETS,
        lightColors = StarBrandColors(
            primary = Color(0xFF14161A),   // 近黑
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF9AA3AB), // 银白冷灰
            onSecondary = Color(0xFF1A1C1F),
            primaryContainer = Color(0xFFE2E4E7),
            secondaryContainer = Color(0xFFE8EBEE),
        ),
        darkColors = StarBrandColors(
            primary = Color(0xFFC9CFD6),   // 亮银白
            onPrimary = Color(0xFF16181B),
            secondary = Color(0xFF7EC8E3), // 冰蓝点缀
            onSecondary = Color(0xFF0E2438),
            primaryContainer = Color(0xFF2A2D31),
            secondaryContainer = Color(0xFF22333C),
        ),
        motif = StarMotif.NET_GRID,
        jerseyNumber = "7",
        geometry = StarGeometry.MINIMAL,
        patternAlpha = 0.06f,
    ),
    StarVisualIdentity.CURRY_WARRIORS to StarVisualProfile(
        identity = StarVisualIdentity.CURRY_WARRIORS,
        lightColors = StarBrandColors(
            primary = Color(0xFF1D428A),   // 皇家蓝
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFC99700), // 金
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDCE6F5),
            secondaryContainer = Color(0xFFFCF1CF),
        ),
        darkColors = StarBrandColors(
            primary = Color(0xFF7FA3E0),
            onPrimary = Color(0xFF0E1E3A),
            secondary = Color(0xFFFFD84D),
            onSecondary = Color(0xFF201500),
            primaryContainer = Color(0xFF22335A),
            secondaryContainer = Color(0xFF4A3A12),
        ),
        motif = StarMotif.SPLASH_ARC,
        jerseyNumber = "30",
        geometry = StarGeometry.ROUNDED,
        patternAlpha = 0.07f,
    ),
    StarVisualIdentity.JORDAN_BULLS to StarVisualProfile(
        identity = StarVisualIdentity.JORDAN_BULLS,
        lightColors = StarBrandColors(
            primary = Color(0xFFCE1141),   // 正红
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF2B2B2B), // 纯黑
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF9DCE3),
            secondaryContainer = Color(0xFFE0E0E0), // 白中性
        ),
        darkColors = StarBrandColors(
            primary = Color(0xFFFF6B8E),
            onPrimary = Color(0xFF3A0612),
            secondary = Color(0xFFD9D9D9),
            onSecondary = Color(0xFF1A1A1A),
            primaryContainer = Color(0xFF4A1A28),
            secondaryContainer = Color(0xFF333333),
        ),
        motif = StarMotif.WINGS,
        jerseyNumber = "23",
        geometry = StarGeometry.SHARP,
        patternAlpha = 0.07f,
    ),
    StarVisualIdentity.HARDEN_ROCKETS to StarVisualProfile(
        identity = StarVisualIdentity.HARDEN_ROCKETS,
        lightColors = StarBrandColors(
            primary = Color(0xFF9E0E24),   // 深火箭红
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFC85A2B), // 橙红
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF9DEE2),
            secondaryContainer = Color(0xFFFBE8DC),
        ),
        darkColors = StarBrandColors(
            primary = Color(0xFFFF6B85),
            onPrimary = Color(0xFF3A0612),
            secondary = Color(0xFFF5A062),
            onSecondary = Color(0xFF2A1404),
            primaryContainer = Color(0xFF4A1A24),
            secondaryContainer = Color(0xFF4A2A17),
        ),
        motif = StarMotif.ROCKET,
        jerseyNumber = "13",
        geometry = StarGeometry.ROUNDED,
        patternAlpha = 0.08f,
    ),
    StarVisualIdentity.IRVING_CURRENT to StarVisualProfile(
        identity = StarVisualIdentity.IRVING_CURRENT,
        lightColors = StarBrandColors(
            primary = Color(0xFF0E5A34),   // 深绿（头像为凯尔特人时期绿球衣）
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF8A6D3B), // 古金点缀
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD9F0E2),
            secondaryContainer = Color(0xFFF2E9D8),
        ),
        darkColors = StarBrandColors(
            primary = Color(0xFF4DCC85),
            onPrimary = Color(0xFF0A2E1A),
            secondary = Color(0xFFD6B87E),
            onSecondary = Color(0xFF241A08),
            primaryContainer = Color(0xFF1D402C),
            secondaryContainer = Color(0xFF3D3423),
        ),
        motif = StarMotif.FREESTYLE_ORBIT,
        jerseyNumber = "11",
        geometry = StarGeometry.ROUNDED,
        patternAlpha = 0.07f,
    ),
    StarVisualIdentity.GEORGE_CLIPPERS to StarVisualProfile(
        identity = StarVisualIdentity.GEORGE_CLIPPERS,
        lightColors = StarBrandColors(
            primary = Color(0xFF1D428A),   // 快船蓝
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFB0102A), // 快船红
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDCE6F5),
            secondaryContainer = Color(0xFFF9DEE2),
        ),
        darkColors = StarBrandColors(
            primary = Color(0xFF7FA3E0),
            onPrimary = Color(0xFF0E1E3A),
            secondary = Color(0xFFFF6078),
            onSecondary = Color(0xFF3A0612),
            primaryContainer = Color(0xFF22335A),
            secondaryContainer = Color(0xFF4A1A24),
        ),
        motif = StarMotif.NET_GRID,
        jerseyNumber = "13",
        geometry = StarGeometry.MINIMAL,
        patternAlpha = 0.05f,
    ),
    StarVisualIdentity.WESTBROOK_THUNDER to StarVisualProfile(
        identity = StarVisualIdentity.WESTBROOK_THUNDER,
        lightColors = StarBrandColors(
            primary = Color(0xFF005A9C),   // 雷霆蓝（深）
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFE8621B), // 雷霆橙
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDCEAF5),
            secondaryContainer = Color(0xFFFCE7D9),
        ),
        darkColors = StarBrandColors(
            primary = Color(0xFF6FB5E8),
            onPrimary = Color(0xFF0E2438),
            secondary = Color(0xFFFF8A4D),
            onSecondary = Color(0xFF2A1002),
            primaryContainer = Color(0xFF1F3D57),
            secondaryContainer = Color(0xFF4A2A17),
        ),
        motif = StarMotif.LIGHTNING,
        jerseyNumber = "0",
        geometry = StarGeometry.DYNAMIC,
        patternAlpha = 0.07f,
    ),

    // ── 足球 ────────────────────────────────────────────────────────────
    StarVisualIdentity.RONALDO_REAL_MADRID to StarVisualProfile(
        identity = StarVisualIdentity.RONALDO_REAL_MADRID,
        lightColors = StarBrandColors(
            primary = Color(0xFF1A2A6C),   // 深海军蓝（主交互色）
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFC9A227), // 香槟金（用于边缘/高光/图案）
            onSecondary = Color(0xFF241A00),
            primaryContainer = Color(0xFFDCE2F2),
            secondaryContainer = Color(0xFFFCF3CF),
        ),
        darkColors = StarBrandColors(
            primary = Color(0xFF8FA3E0),
            onPrimary = Color(0xFF0E1E3A),
            secondary = Color(0xFFE5C55C),
            onSecondary = Color(0xFF241A00),
            primaryContainer = Color(0xFF22335A),
            secondaryContainer = Color(0xFF4A3A12),
        ),
        motif = StarMotif.SPEED_LINES,
        jerseyNumber = "7",
        geometry = StarGeometry.SHARP,
        patternAlpha = 0.06f,
    ),
    StarVisualIdentity.MESSI_ARGENTINA to StarVisualProfile(
        identity = StarVisualIdentity.MESSI_ARGENTINA,
        lightColors = StarBrandColors(
            primary = Color(0xFF3E7CB1),   // 阿根廷天蓝（加深保证对比）
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF5A6E85), // 白/灰蓝系
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE3EFFA),
            secondaryContainer = Color(0xFFF2F4F6),
        ),
        darkColors = StarBrandColors(
            primary = Color(0xFF8FC1E8),
            onPrimary = Color(0xFF0E2438),
            secondary = Color(0xFFF2F5F8), // 白（暗色下配深字）
            onSecondary = Color(0xFF0E2438),
            primaryContainer = Color(0xFF26405C),
            secondaryContainer = Color(0xFF2C3A48),
        ),
        motif = StarMotif.STAR_RAYS,
        jerseyNumber = "10",
        geometry = StarGeometry.ROUNDED,
        patternAlpha = 0.06f,
    ),
    StarVisualIdentity.MBAPPE_FRANCE to StarVisualProfile(
        identity = StarVisualIdentity.MBAPPE_FRANCE,
        lightColors = StarBrandColors(
            primary = Color(0xFF001E62),   // 深海军蓝
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF0055A4), // 皇家蓝
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDCE4F5),
            secondaryContainer = Color(0xFFE0EAF7),
        ),
        darkColors = StarBrandColors(
            primary = Color(0xFF6B8FD8),
            onPrimary = Color(0xFF0A1430),
            secondary = Color(0xFF7FB2E8),
            onSecondary = Color(0xFF0E2438),
            primaryContainer = Color(0xFF16264A),
            secondaryContainer = Color(0xFF1F3A57),
        ),
        motif = StarMotif.SPEED_LINES,
        jerseyNumber = "10",
        geometry = StarGeometry.DYNAMIC,
        patternAlpha = 0.07f,
    ),
    StarVisualIdentity.NEYMAR_BRAZIL to StarVisualProfile(
        identity = StarVisualIdentity.NEYMAR_BRAZIL,
        lightColors = StarBrandColors(
            primary = Color(0xFFFFDF00),   // 巴西黄（配深色文字）
            onPrimary = Color(0xFF241A00),
            secondary = Color(0xFF007A33), // 巴西绿
            onSecondary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFCF3CF),
            secondaryContainer = Color(0xFFD9F0E2),
        ),
        darkColors = StarBrandColors(
            primary = Color(0xFFFFE14D),
            onPrimary = Color(0xFF241A00),
            secondary = Color(0xFF4DCC85),
            onSecondary = Color(0xFF0A2E1A),
            primaryContainer = Color(0xFF4A3E0E),
            secondaryContainer = Color(0xFF1D402C),
        ),
        motif = StarMotif.FREESTYLE_ORBIT,
        jerseyNumber = "10",
        geometry = StarGeometry.ROUNDED,
        patternAlpha = 0.07f,
    ),
).mapValues { (identity, profile) ->
    val copy = starHeroCopy[identity]
    profile.copy(
        shortName = copy?.shortName,
        slogan = copy?.slogan,
        heroOverlayStyle = copy?.overlayStyle ?: profile.heroOverlayStyle,
        homeBackgroundRes = profile.homeSceneArtRes(),
        profileBackgroundRes = profile.profileSceneArtRes(),
    )
}

/** App 根部通过此 CompositionLocal 提供当前完整视觉身份。 */
val LocalStarVisualProfile = staticCompositionLocalOf { defaultStarVisualProfile }
