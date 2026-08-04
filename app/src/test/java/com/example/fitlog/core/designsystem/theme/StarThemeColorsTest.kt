package com.example.fitlog.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 球星主题配色校验：Kobe 紫金、默认保持绿色、球星之间互不相同、
 * 明暗双套都属于同一球星体系。
 */
class StarThemeColorsTest {

    @Test
    fun `kobe light theme uses purple primary and gold secondary`() {
        val kobe = StarThemePalettes.light(StarThemeId.KOBE)
        assertEquals(Color(0xFF552583), kobe.primary)
        // 金色系：暗金（浅色下保证白字对比度）或亮金
        assertTrue(
            "Kobe 辅助色应为金色系，实际 ${kobe.secondary}",
            isGoldFamily(kobe.secondary),
        )
    }

    @Test
    fun `kobe dark theme stays in the purple-gold family`() {
        val dark = StarThemePalettes.dark(StarThemeId.KOBE)
        assertTrue("暗色主色应为紫色系，实际 ${dark.primary}", isPurpleFamily(dark.primary))
        assertTrue("暗色辅助色应为金色系，实际 ${dark.secondary}", isGoldFamily(dark.secondary))
        // 暗色主色应比浅色主色更亮（对比度要求）
        assertTrue(
            "暗色主色亮度应高于浅色主色",
            dark.primary.luminance() > StarThemePalettes.light(StarThemeId.KOBE).primary.luminance(),
        )
    }

    @Test
    fun `messi and jordan resolve to different themes`() {
        assertNotEquals(
            StarThemePalettes.light(StarThemeId.MESSI),
            StarThemePalettes.light(StarThemeId.JORDAN),
        )
        assertNotEquals(StarThemeId.MESSI, StarThemeId.JORDAN)
    }

    @Test
    fun `default theme keeps the existing green`() {
        val light = StarThemePalettes.light(StarThemeId.DEFAULT)
        assertEquals(Color(0xFF287867), light.primary)
        assertEquals(Color(0xFF1E5C4F), light.secondary)
        val dark = StarThemePalettes.dark(StarThemeId.DEFAULT)
        assertEquals(Color(0xFF5BBFA4), dark.primary)
    }

    @Test
    fun `star themes do not touch semantic colors`() {
        // 品牌色只覆盖 accent 家族；语义色来自 FitLogColorScheme 本身
        val default = LightFitLogColors
        val kobeColors = default.withBrandForTest(StarThemePalettes.light(StarThemeId.KOBE))
        assertEquals(default.error, kobeColors.error)
        assertEquals(default.success, kobeColors.success)
        assertEquals(default.warning, kobeColors.warning)
        assertEquals(default.background, kobeColors.background)
    }

    @Test
    fun `every star theme is distinct from default`() {
        StarThemeId.entries.filter { it != StarThemeId.DEFAULT }.forEach { id ->
            assertNotEquals(
                "star theme $id must differ from default",
                StarThemePalettes.light(StarThemeId.DEFAULT),
                StarThemePalettes.light(id),
            )
        }
    }

    private fun isGoldFamily(color: Color): Boolean {
        val (h, s, l) = color.hsl()
        return h in 38f..55f && s > 0.5f && l in 0.25f..0.75f
    }

    private fun isPurpleFamily(color: Color): Boolean {
        val (h, _, l) = color.hsl()
        return h in 255f..285f && l in 0.2f..0.8f
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
