package com.example.fitlog.domain.avatar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guards against "one artwork copied into 13 files": every built-in avatar
 * must be backed by byte-distinct WebP resources.
 */
@RunWith(RobolectricTestRunner::class)
class BuiltInAvatarUniqueResourceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun readResource(avatar: BuiltInAvatar): ByteArray =
        context.resources.openRawResource(avatar.drawableRes)!!.use { it.readBytes() }

    @Test
    fun `no two built-in avatars share the same image bytes`() {
        val resources = BuiltInAvatar.ALL.map { readResource(it) }
        for (i in BuiltInAvatar.ALL.indices) {
            for (j in i + 1 until BuiltInAvatar.ALL.size) {
                assertFalse(
                    "${BuiltInAvatar.ALL[i].key} and ${BuiltInAvatar.ALL[j].key} " +
                        "must not be the same image file",
                    resources[i].contentEquals(resources[j]),
                )
            }
        }
    }

    @Test
    fun `every avatar has a distinct drawable resource id`() {
        val ids = BuiltInAvatar.ALL.map { it.drawableRes }
        assertTrue(
            "13 avatars need 13 distinct resource ids",
            ids.distinct().size == BuiltInAvatar.ALL.size,
        )
    }
}
