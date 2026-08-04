package com.example.fitlog.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 球星视觉身份配色校验：
 * - 各身份不再使用错误时期的球队配色
 * - Jordan 与 Harden 全维度区分
 * - Kobe 与 LeBron 同为紫金但 motif/geometry 不同
 * - 默认主题保持绿色；语义色不被球星色覆盖
 */
class StarThemeColorsTest {

    @Test
    fun `lebron lakers theme is purple-gold, not cavaliers wine`() {
        val light = starVisualProfiles[StarVisualIdentity.LEBRON_LAKERS]!!.lightColors
        // 主色必须是紫色系（非酒红 #6F263D 之类）
        assertTrue("LeBron 主色应为紫色系，实际 ${light.primary}", isPurpleFamily(light.primary))
        assertTrue("LeBron 辅助色应为金色系，实际 ${light.secondary}", isGoldFamily(light.secondary))
        // 明确不是骑士酒红金
        assertNotEquals(Color(0xFF6F263D), light.primary)
        assertNotEquals(Color(0xFFFFB81C), light.secondary)
    }

    @Test
    fun `durant nets theme is black-silver, not blue-orange`() {
        val light = starVisualProfiles[StarVisualIdentity.DURANT_NETS]!!.lightColors
        // 近黑主色 + 银白冷灰辅助
        assertTrue(
            "Durant 主色应为近黑色系，实际 ${light.primary}",
            light.primary.luminance() < 0.15f,
        )
        assertTrue(
            "Durant 辅助色应为银白冷灰，实际 ${light.secondary}",
            light.secondary.luminance() > 0.3f,
        )
        // 明确不是蓝橙
        assertNotEquals(Color(0xFF1D428A), light.primary)
        assertNotEquals(Color(0xFFE56020), light.secondary)
    }

    @Test
    fun `ronaldo real madrid theme is navy-ivory, not portugal red-gold`() {
        val light = starVisualProfiles[StarVisualIdentity.RONALDO_REAL_MADRID]!!.lightColors
        // 深海军蓝主色（非红）
        assertTrue(
            "Ronaldo 主色应为深海军蓝，实际 ${light.primary}",
            isBlueFamily(light.primary),
        )
        // 明确不是葡萄牙红金
        assertNotEquals(Color(0xFFC8102E), light.primary)
        assertNotEquals(Color(0xFFF1C40F), light.secondary)
        // 香槟金装饰辅助色
        assertTrue("Ronaldo 辅助色应为香槟金系", isGoldFamily(light.secondary))
    }

    @Test
    fun `jordan and harden differ in colors motif and geometry`() {
        val jordan = starVisualProfiles[StarVisualIdentity.JORDAN_BULLS]!!
        val harden = starVisualProfiles[StarVisualIdentity.HARDEN_ROCKETS]!!
        assertNotEquals("主色不同", jordan.lightColors.primary, harden.lightColors.primary)
        assertNotEquals("辅助色不同", jordan.lightColors.secondary, harden.lightColors.secondary)
        assertNotEquals("motif 不同", jordan.motif, harden.motif)
        assertNotEquals("geometry 不同", jordan.geometry, harden.geometry)
        assertEquals(StarMotif.WINGS, jordan.motif)
        assertEquals(StarMotif.ROCKET, harden.motif)
        assertEquals(StarGeometry.SHARP, jordan.geometry)
        assertEquals(StarGeometry.ROUNDED, harden.geometry)
    }

    @Test
    fun `kobe and lebron are both purple-gold but differ in motif and geometry`() {
        val kobe = starVisualProfiles[StarVisualIdentity.KOBE_LAKERS]!!
        val lebron = starVisualProfiles[StarVisualIdentity.LEBRON_LAKERS]!!
        assertTrue(isPurpleFamily(kobe.lightColors.primary))
        assertTrue(isPurpleFamily(lebron.lightColors.primary))
        assertTrue(isGoldFamily(kobe.lightColors.secondary))
        assertTrue(isGoldFamily(lebron.lightColors.secondary))
        assertNotEquals("Kobe 与 LeBron motif 不同", kobe.motif, lebron.motif)
        assertNotEquals("Kobe 与 LeBron geometry 不同", kobe.geometry, lebron.geometry)
        assertEquals(StarMotif.MAMBA_SCALE, kobe.motif)
        assertEquals(StarMotif.CROWN, lebron.motif)
        assertEquals(StarGeometry.SHARP, kobe.geometry)
        assertEquals(StarGeometry.HEAVY, lebron.geometry)
        // Kobe 更黑更锋利：Kobe 主色亮度应低于 LeBron
        assertTrue(
            "Kobe 深紫应比 LeBron 皇家紫更暗",
            kobe.lightColors.primary.luminance() < lebron.lightColors.primary.luminance(),
        )
    }

    @Test
    fun `custom and default avatars keep the fitlog default theme`() {
        val default = defaultStarVisualProfile
        assertEquals(StarMotif.NONE, default.motif)
        assertEquals(null, default.jerseyNumber)
        assertEquals(StarGeometry.NEUTRAL, default.geometry)
        assertEquals(Color(0xFF287867), default.lightColors.primary)
        assertEquals(Color(0xFF1E5C4F), default.lightColors.secondary)
        assertEquals(Color(0xFF5BBFA4), default.darkColors.primary)
    }

    @Test
    fun `dark mode raises primary brightness within the same identity`() {
        StarVisualIdentity.entries.filter { it != StarVisualIdentity.DEFAULT }.forEach { id ->
            val profile = starVisualProfiles[id]!!
            assertTrue(
                "$id 暗色主色应比浅色主色更亮",
                profile.darkColors.primary.luminance() > profile.lightColors.primary.luminance(),
            )
        }
    }

    @Test
    fun `semantic colors stay untouched by star themes`() {
        val kobeColors = LightFitLogColors.withBrandForTest(
            starVisualProfiles[StarVisualIdentity.KOBE_LAKERS]!!.lightColors,
        )
        assertEquals(LightFitLogColors.error, kobeColors.error)
        assertEquals(LightFitLogColors.success, kobeColors.success)
        assertEquals(LightFitLogColors.warning, kobeColors.warning)
        assertEquals(LightFitLogColors.background, kobeColors.background)
    }

    private fun isGoldFamily(color: Color): Boolean {
        val (h, s, l) = color.hsl()
        return h in 38f..55f && s > 0.4f && l in 0.25f..0.75f
    }

    private fun isPurpleFamily(color: Color): Boolean {
        val (h, _, l) = color.hsl()
        return h in 255f..285f && l in 0.2f..0.8f
    }

    private fun isBlueFamily(color: Color): Boolean {
        val (h, _, l) = color.hsl()
        return h in 215f..250f && l in 0.15f..0.5f
    }
}

/** 测试辅助：把品牌色应用到配色（与 Theme.kt 中 withBrand 逻辑一致）。 */
private fun FitLogColorScheme.withBrandForTest(brand: StarBrandColors): FitLogColorScheme = copy(
    accent = brand.primary,
    onAccent = brand.onPrimary,
    accentVariant = brand.secondary,
    onAccentVariant = brand.onSecondary,
    accentContainer = brand.primaryContainer,
    accentVariantContainer = brand.secondaryContainer,
)

/** 简易 HSL 转换（0..360 / 0..1 / 0..1），仅供测试断言使用。 */
private fun Color.hsl(): Triple<Float, Float, Float> {
    val r = red; val g = green; val b = blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return Triple(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> ((g - b) / d + (if (g < b) 6f else 0f)) * 60f
        g -> ((b - r) / d + 2f) * 60f
        else -> ((r - g) / d + 4f) * 60f
    }
    return Triple(h, s, l)
}
