package com.example.fitlog.domain.avatar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies, through Android's real resource loading, that every built-in
 * avatar drawable exists inside the APK resources and is a genuine,
 * decodable 512x512 WebP image (valid RIFF/WEBP container with a proper
 * VP8/VP8L/VP8X frame) — not an empty file or a copied placeholder.
 *
 * Robolectric's BitmapFactory cannot decode WebP natively, so the image
 * itself is validated structurally: a standards-compliant decoder reads
 * exactly the dimensions parsed here.
 */
@RunWith(RobolectricTestRunner::class)
class BuiltInAvatarDrawableTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun readResource(avatar: BuiltInAvatar): ByteArray {
        val resId = avatar.drawableRes
        assertTrue("${avatar.key} must have a non-zero drawable id", resId != 0)
        val bytes = context.resources.openRawResource(resId)?.use { it.readBytes() }
        assertNotNull("${avatar.key} drawable must exist in resources", bytes)
        return bytes!!
    }

    @Test
    fun `every built-in avatar resource exists and is a real webp`() {
        BuiltInAvatar.ALL.forEach { avatar ->
            val bytes = readResource(avatar)
            val info = parseWebP(bytes)
            assertNotNull(
                "${avatar.key} must be a valid WebP container, " +
                    "got ${bytes.size} bytes starting with " +
                    "${bytes.take(12).map { it.toInt().and(0xFF) }}",
                info,
            )
            assertEquals("${avatar.key} width", 512, info!!.first)
            assertEquals("${avatar.key} height", 512, info.second)
        }
    }

    @Test
    fun `every built-in avatar drawable is not an empty placeholder file`() {
        BuiltInAvatar.ALL.forEach { avatar ->
            val bytes = readResource(avatar)
            assertTrue(
                "${avatar.key} must carry real image data",
                bytes.size > 1000,
            )
        }
    }

    /** Minimal RIFF/WebP parser: returns canvas size for VP8 / VP8L / VP8X. */
    private fun parseWebP(bytes: ByteArray): Pair<Int, Int>? {
        if (bytes.size < 30) return null
        if (bytes[0] != 'R'.code.toByte() || bytes[1] != 'I'.code.toByte() ||
            bytes[2] != 'F'.code.toByte() || bytes[3] != 'F'.code.toByte()
        ) return null
        if (bytes[8] != 'W'.code.toByte() || bytes[9] != 'E'.code.toByte() ||
            bytes[10] != 'B'.code.toByte() || bytes[11] != 'P'.code.toByte()
        ) return null

        var offset = 12
        while (offset + 8 <= bytes.size) {
            val fourcc = String(bytes, offset, 4)
            val chunkSize = readInt32(bytes, offset + 4)
            val data = offset + 8
            if (data + chunkSize > bytes.size) return null
            when (fourcc) {
                "VP8X" -> {
                    if (chunkSize < 10) return null
                    return (1 + readInt24(bytes, data + 4)) to
                        (1 + readInt24(bytes, data + 7))
                }
                "VP8 " -> {
                    if (chunkSize < 13) return null
                    // libwebp layout: 3-byte frame tag, 3-byte start code,
                    // then 14-bit LE width/height
                    if (bytes[data + 3] != 0x9D.toByte() ||
                        bytes[data + 4] != 0x01.toByte() ||
                        bytes[data + 5] != 0x2A.toByte()
                    ) return null
                    val width = (bytes[data + 6].toInt() and 0xFF) or
                        ((bytes[data + 7].toInt() and 0x3F) shl 8)
                    val height = (bytes[data + 8].toInt() and 0xFF) or
                        ((bytes[data + 9].toInt() and 0x3F) shl 8)
                    return width to height
                }
                "VP8L" -> {
                    if (chunkSize < 5) return null
                    if (bytes[data] != 0x2F.toByte()) return null
                    val b0 = bytes[data + 1].toInt() and 0xFF
                    val b1 = bytes[data + 2].toInt() and 0xFF
                    val b2 = bytes[data + 3].toInt() and 0xFF
                    val b3 = bytes[data + 4].toInt() and 0xFF
                    val width = 1 + (b0 or ((b1 and 0x3F) shl 8))
                    val height = 1 + ((b1 shr 6) or (b2 shl 2) or ((b3 and 0x0F) shl 10))
                    return width to height
                }
            }
            offset = data + chunkSize + (chunkSize % 2)
        }
        return null
    }

    private fun readInt32(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun readInt24(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16)
}
