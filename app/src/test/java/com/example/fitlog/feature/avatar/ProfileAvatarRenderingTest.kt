package com.example.fitlog.feature.avatar

import com.example.fitlog.R
import com.example.fitlog.domain.avatar.AvatarType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The profile page renders avatars through [resolveAvatar]: a BUILT_IN key
 * (modern or legacy) shows the star artwork, a CUSTOM photo with a real
 * file shows the photo, and anything invalid falls back to the plain person
 * placeholder — never to the app launcher icon.
 */
class ProfileAvatarRenderingTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun resolve(
        type: AvatarType,
        key: String? = null,
        path: String? = null,
        fileOf: (String) -> File = { File(it) },
    ): AvatarResolution = resolveAvatar(type, key, path, fileOf)

    @Test
    fun `built-in with a valid key renders the star drawable`() {
        val resolution = resolve(AvatarType.BUILT_IN, "kobe")
        assertEquals(AvatarResolution.BuiltIn(R.drawable.avatar_kobe), resolution)
    }

    @Test
    fun `built-in with a legacy key maps to the star drawable`() {
        val resolution = resolve(AvatarType.BUILT_IN, "avatar_basketball_pg")
        assertEquals(AvatarResolution.BuiltIn(R.drawable.avatar_curry), resolution)
    }

    @Test
    fun `built-in with an unknown key falls back to the person placeholder`() {
        assertEquals(
            AvatarResolution.Default,
            resolve(AvatarType.BUILT_IN, "no_such_star"),
        )
    }

    @Test
    fun `built-in with a null key falls back to the person placeholder`() {
        assertEquals(AvatarResolution.Default, resolve(AvatarType.BUILT_IN, null))
    }

    @Test
    fun `custom with an existing file renders the photo`() {
        val photo = tmp.newFile("avatar_1.jpg")
        val resolution = resolve(AvatarType.CUSTOM, null, "avatars/avatar_1.jpg") { photo }
        assertEquals(AvatarResolution.Custom(photo), resolution)
    }

    @Test
    fun `custom with a missing file falls back to the person placeholder`() {
        assertEquals(
            AvatarResolution.Default,
            resolve(AvatarType.CUSTOM, null, "avatars/gone.jpg"),
        )
    }

    @Test
    fun `default type renders the person placeholder`() {
        assertEquals(AvatarResolution.Default, resolve(AvatarType.DEFAULT))
    }

    @Test
    fun `no resolution path returns the launcher icon`() {
        val resolutions = listOf(
            resolve(AvatarType.BUILT_IN, "kobe"),
            resolve(AvatarType.BUILT_IN, "no_such_star"),
            resolve(AvatarType.BUILT_IN, null),
            resolve(AvatarType.CUSTOM, null, "avatars/gone.jpg"),
            resolve(AvatarType.DEFAULT),
        )
        resolutions.forEach { resolution ->
            if (resolution is AvatarResolution.BuiltIn) {
                assertNotEquals(
                    "profile avatar must never be the launcher icon",
                    R.drawable.ic_launcher_foreground,
                    resolution.drawableRes,
                )
            }
        }
        assertTrue(
            resolutions.filterIsInstance<AvatarResolution.Default>().isNotEmpty(),
        )
    }
}
