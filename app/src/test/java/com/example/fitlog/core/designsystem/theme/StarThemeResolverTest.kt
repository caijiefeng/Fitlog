package com.example.fitlog.core.designsystem.theme

import com.example.fitlog.domain.avatar.AvatarType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * 球星视觉身份解析规则：
 * - 仅 BUILT_IN 启用球星主题
 * - key 经 BuiltInAvatar.byKey 解析（兼容 legacy key）
 * - CUSTOM / DEFAULT / null / 未知 key → DEFAULT
 * - 解析流程 avatarKey → byKey → visualIdentity → StarVisualProfile
 */
class StarThemeResolverTest {

    @Test
    fun `every built-in star resolves to its visual identity`() {
        val expected = mapOf(
            "kobe" to StarVisualIdentity.KOBE_LAKERS,
            "lebron" to StarVisualIdentity.LEBRON_LAKERS,
            "durant" to StarVisualIdentity.DURANT_NETS,
            "curry" to StarVisualIdentity.CURRY_WARRIORS,
            "jordan" to StarVisualIdentity.JORDAN_BULLS,
            "harden" to StarVisualIdentity.HARDEN_ROCKETS,
            "irving" to StarVisualIdentity.IRVING_CURRENT,
            "george" to StarVisualIdentity.GEORGE_CLIPPERS,
            "westbrook" to StarVisualIdentity.WESTBROOK_THUNDER,
            "ronaldo" to StarVisualIdentity.RONALDO_REAL_MADRID,
            "messi" to StarVisualIdentity.MESSI_ARGENTINA,
            "mbappe" to StarVisualIdentity.MBAPPE_FRANCE,
            "neymar" to StarVisualIdentity.NEYMAR_BRAZIL,
        )
        expected.forEach { (key, identity) ->
            assertEquals(
                "BUILT_IN + $key should resolve to $identity",
                identity,
                resolveStarVisualIdentity(AvatarType.BUILT_IN, key),
            )
        }
    }

    @Test
    fun `legacy avatar keys resolve through the legacy map`() {
        assertEquals(
            StarVisualIdentity.CURRY_WARRIORS,
            resolveStarVisualIdentity(AvatarType.BUILT_IN, "avatar_basketball_pg"),
        )
        assertEquals(
            StarVisualIdentity.KOBE_LAKERS,
            resolveStarVisualIdentity(AvatarType.BUILT_IN, "avatar_basketball_sg"),
        )
        assertEquals(
            StarVisualIdentity.LEBRON_LAKERS,
            resolveStarVisualIdentity(AvatarType.BUILT_IN, "avatar_basketball_dunker"),
        )
        assertEquals(
            StarVisualIdentity.MESSI_ARGENTINA,
            resolveStarVisualIdentity(AvatarType.BUILT_IN, "avatar_soccer_goalkeeper"),
        )
    }

    @Test
    fun `custom avatar returns default identity and profile`() {
        assertEquals(
            StarVisualIdentity.DEFAULT,
            resolveStarVisualIdentity(AvatarType.CUSTOM, "kobe"),
        )
        assertSame(
            defaultStarVisualProfile,
            resolveStarVisualProfile(AvatarType.CUSTOM, "kobe"),
        )
    }

    @Test
    fun `default type returns default even with a star key`() {
        assertEquals(
            StarVisualIdentity.DEFAULT,
            resolveStarVisualIdentity(AvatarType.DEFAULT, "messi"),
        )
    }

    @Test
    fun `null key returns default`() {
        assertEquals(
            StarVisualIdentity.DEFAULT,
            resolveStarVisualIdentity(AvatarType.BUILT_IN, null),
        )
    }

    @Test
    fun `unknown key returns default instead of crashing`() {
        assertEquals(
            StarVisualIdentity.DEFAULT,
            resolveStarVisualIdentity(AvatarType.BUILT_IN, "no_such_star"),
        )
        assertEquals(
            StarVisualIdentity.DEFAULT,
            resolveStarVisualIdentity(AvatarType.BUILT_IN, "kobe_typo"),
        )
    }

    @Test
    fun `profile carries motif number and geometry for the identity`() {
        val kobe = resolveStarVisualProfile(AvatarType.BUILT_IN, "kobe")
        assertEquals(StarVisualIdentity.KOBE_LAKERS, kobe.identity)
        assertEquals(StarMotif.MAMBA_SCALE, kobe.motif)
        assertEquals("24", kobe.jerseyNumber)
        assertEquals(StarGeometry.SHARP, kobe.geometry)
        assertNotEquals(0f, kobe.patternAlpha)
    }
}
