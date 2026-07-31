package com.example.fitlog.domain.avatar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Profiles saved before the star catalog existed may store the old generic
 * position keys. [BuiltInAvatar.byKey] must map those to a star so the UI
 * never crashes and never falls back to a placeholder.
 */
class LegacyAvatarKeyTest {

    private val legacyToModern = mapOf(
        "avatar_basketball_pg" to "curry",
        "avatar_basketball_sg" to "kobe",
        "avatar_basketball_dunker" to "lebron",
        "avatar_basketball_forward" to "durant",
        "avatar_basketball_center" to "jordan",
        "avatar_basketball_defender" to "george",
        "avatar_soccer_forward" to "ronaldo",
        "avatar_soccer_winger" to "neymar",
        "avatar_soccer_midfielder" to "messi",
        "avatar_soccer_defender" to "mbappe",
        "avatar_soccer_goalkeeper" to "messi",
        "avatar_street_athlete" to "westbrook",
    )

    @Test
    fun `every legacy key resolves to its mapped star`() {
        legacyToModern.forEach { (legacy, expectedKey) ->
            val avatar = BuiltInAvatar.byKey(legacy)
            assertNotNull("legacy key $legacy must resolve", avatar)
            assertEquals("$legacy should map to $expectedKey", expectedKey, avatar!!.key)
        }
    }

    @Test
    fun `modern star keys resolve directly`() {
        BuiltInAvatar.ALL.forEach { avatar ->
            assertEquals(avatar.key, BuiltInAvatar.byKey(avatar.key)?.key)
        }
    }

    @Test
    fun `unknown key returns null instead of crashing`() {
        assertNull(BuiltInAvatar.byKey("avatar_mystery_position"))
        assertNull(BuiltInAvatar.byKey("kobe_typo"))
    }

    @Test
    fun `null key returns null`() {
        assertNull(BuiltInAvatar.byKey(null))
    }
}
