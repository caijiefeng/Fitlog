package com.example.fitlog.domain.avatar

import com.example.fitlog.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the built-in avatar catalog: it must hold exactly the 13 sports
 * stars, each backed by its own drawable and key, and must never point at
 * the launcher foreground placeholder.
 */
class BuiltInAvatarCatalogTest {

    @Test
    fun `catalog contains 13 built-in avatars`() {
        assertEquals(13, BuiltInAvatar.ALL.size)
    }

    @Test
    fun `no avatar references the launcher foreground placeholder`() {
        assertTrue(
            "no avatar may reuse R.drawable.ic_launcher_foreground",
            BuiltInAvatar.ALL.none { it.drawableRes == R.drawable.ic_launcher_foreground },
        )
    }

    @Test
    fun `every avatar maps to a distinct drawable resource`() {
        assertEquals(
            "all 13 avatars must point at their own artwork",
            13,
            BuiltInAvatar.ALL.map { it.drawableRes }.distinct().size,
        )
    }

    @Test
    fun `every avatar has a distinct key`() {
        assertEquals(
            13,
            BuiltInAvatar.ALL.map { it.key }.distinct().size,
        )
    }

    @Test
    fun `catalog splits into 9 basketball and 4 soccer stars`() {
        assertEquals(9, BuiltInAvatar.BASKETBALL.size)
        assertEquals(4, BuiltInAvatar.SOCCER.size)
        assertEquals(13, BuiltInAvatar.BASKETBALL.size + BuiltInAvatar.SOCCER.size)
        assertTrue(BuiltInAvatar.BASKETBALL.all { it.category == AvatarCategory.BASKETBALL })
        assertTrue(BuiltInAvatar.SOCCER.all { it.category == AvatarCategory.SOCCER })
    }

    @Test
    fun `basketball stars are the expected nine`() {
        assertEquals(
            listOf("kobe", "lebron", "durant", "curry", "jordan",
                   "harden", "irving", "george", "westbrook"),
            BuiltInAvatar.BASKETBALL.map { it.key },
        )
    }

    @Test
    fun `soccer stars are the expected four`() {
        assertEquals(
            listOf("ronaldo", "messi", "mbappe", "neymar"),
            BuiltInAvatar.SOCCER.map { it.key },
        )
    }
}
