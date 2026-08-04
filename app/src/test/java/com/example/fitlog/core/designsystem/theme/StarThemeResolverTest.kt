package com.example.fitlog.core.designsystem.theme

import com.example.fitlog.domain.avatar.AvatarType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * 球星主题解析规则：
 * - 仅 BUILT_IN 启用球星主题
 * - key 经 BuiltInAvatar.byKey 解析（兼容 legacy key）
 * - CUSTOM / DEFAULT / null / 未知 key → DEFAULT
 * - 明暗模式不参与解析（resolveStarTheme 无明暗参数）
 */
class StarThemeResolverTest {

    @Test
    fun `every built-in star resolves to its own theme`() {
        val expected = mapOf(
            "kobe" to StarThemeId.KOBE,
            "lebron" to StarThemeId.LEBRON,
            "durant" to StarThemeId.DURANT,
            "curry" to StarThemeId.CURRY,
            "jordan" to StarThemeId.JORDAN,
            "harden" to StarThemeId.HARDEN,
            "irving" to StarThemeId.IRVING,
            "george" to StarThemeId.GEORGE,
            "westbrook" to StarThemeId.WESTBROOK,
            "ronaldo" to StarThemeId.RONALDO,
            "messi" to StarThemeId.MESSI,
            "mbappe" to StarThemeId.MBAPPE,
            "neymar" to StarThemeId.NEYMAR,
        )
        expected.forEach { (key, theme) ->
            assertEquals(
                "BUILT_IN + $key should resolve to $theme",
                theme,
                resolveStarTheme(AvatarType.BUILT_IN, key),
            )
        }
    }

    @Test
    fun `legacy avatar keys resolve through the legacy map`() {
        assertEquals(
            StarThemeId.CURRY,
            resolveStarTheme(AvatarType.BUILT_IN, "avatar_basketball_pg"),
        )
        assertEquals(
            StarThemeId.KOBE,
            resolveStarTheme(AvatarType.BUILT_IN, "avatar_basketball_sg"),
        )
        assertEquals(
            StarThemeId.LEBRON,
            resolveStarTheme(AvatarType.BUILT_IN, "avatar_basketball_dunker"),
        )
        assertEquals(
            StarThemeId.MESSI,
            resolveStarTheme(AvatarType.BUILT_IN, "avatar_soccer_goalkeeper"),
        )
        assertEquals(
            StarThemeId.WESTBROOK,
            resolveStarTheme(AvatarType.BUILT_IN, "avatar_street_athlete"),
        )
    }

    @Test
    fun `custom avatar returns default theme`() {
        assertEquals(
            StarThemeId.DEFAULT,
            resolveStarTheme(AvatarType.CUSTOM, "kobe"),
        )
        assertEquals(
            StarThemeId.DEFAULT,
            resolveStarTheme(AvatarType.CUSTOM, null),
        )
    }

    @Test
    fun `default type returns default theme even with a star key`() {
        assertEquals(
            StarThemeId.DEFAULT,
            resolveStarTheme(AvatarType.DEFAULT, "messi"),
        )
    }

    @Test
    fun `null key returns default theme`() {
        assertEquals(StarThemeId.DEFAULT, resolveStarTheme(AvatarType.BUILT_IN, null))
    }

    @Test
    fun `unknown key returns default theme instead of crashing`() {
        assertEquals(StarThemeId.DEFAULT, resolveStarTheme(AvatarType.BUILT_IN, "no_such_star"))
        assertEquals(StarThemeId.DEFAULT, resolveStarTheme(AvatarType.BUILT_IN, "kobe_typo"))
    }

    @Test
    fun `resolving does not depend on light or dark mode`() {
        // resolveStarTheme 是纯函数，不接收明暗参数——对同一头像恒返回同一主题
        assertEquals(
            resolveStarTheme(AvatarType.BUILT_IN, "jordan"),
            resolveStarTheme(AvatarType.BUILT_IN, "jordan"),
        )
        assertNotEquals(
            StarThemeId.DEFAULT,
            resolveStarTheme(AvatarType.BUILT_IN, "jordan"),
        )
    }
}
