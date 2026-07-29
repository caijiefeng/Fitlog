package com.example.fitlog.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MediaFileNameTest {

    private lateinit var storage: AppMediaStorage

    @Before
    fun setUp() {
        storage = AppMediaStorage(RuntimeEnvironment.getApplication())
    }

    @Test
    fun `generated photo filenames are unique across multiple calls`() {
        val names = mutableSetOf<String>()
        repeat(100) {
            val pending = storage.createPendingPhoto("image/jpeg")
            val relativePath = pending.relativePath
            storage.discardPendingMedia(pending)
            // relativePath format: "Pictures/FitLog/<timestamp>_<random>.ext"
            assertTrue("Path should start with Pictures/", relativePath.startsWith("Pictures/"))
            assertTrue("Path should end with .jpg", relativePath.endsWith(".jpg"))
            assertTrue("Path should not contain .pending", !relativePath.contains(".pending"))
            names.add(relativePath)
        }
        assertEquals("All 100 generated names should be unique", 100, names.size)
    }

    @Test
    fun `generated video filenames are unique across multiple calls`() {
        val names = mutableSetOf<String>()
        repeat(100) {
            val pending = storage.createPendingVideo("video/mp4")
            val path = pending.relativePath
            storage.discardPendingMedia(pending)
            assertTrue("Path should start with Movies/", path.startsWith("Movies/"))
            assertTrue("Path should end with .mp4", path.endsWith(".mp4"))
            names.add(path)
        }
        assertEquals("All 100 generated names should be unique", 100, names.size)
    }

    @Test
    fun `generated filenames use timestamp plus random suffix`() {
        val pending = storage.createPendingPhoto("image/jpeg")
        try {
            // Extract filename from relative path: "Pictures/FitLog/<name>.jpg"
            val fileName = pending.relativePath.removePrefix("Pictures/FitLog/").removeSuffix(".jpg")
            assertTrue("Filename should contain underscore separator", fileName.contains("_"))
            val parts = fileName.split("_")
            assertTrue("Filename should have at least 2 parts (timestamp_random)", parts.size >= 2)
            val randomPart = parts.last()
            assertEquals("Random suffix should be 8 characters long", 8, randomPart.length)
            assertTrue("Random suffix should be alphanumeric", randomPart.all { it.isLetterOrDigit() })
        } finally {
            storage.discardPendingMedia(pending)
        }
    }

    @Test
    fun `resolveFile with simple relative path returns valid file`() {
        val file = storage.resolveFile("Pictures/FitLog/photo.jpg")
        assertNotNull(file)
        assertTrue("Path should be absolute", file.isAbsolute)
    }

    @Test
    fun `resolveFile with multiple segments works correctly`() {
        val file = storage.resolveFile("Pictures/FitLog/subdir/photo.jpg")
        assertTrue(file.path.endsWith("Pictures/FitLog/subdir/photo.jpg"))
    }

    @Test
    fun `resolveFile with parent dir traversal throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            storage.resolveFile("../private_file")
        }
    }

    @Test
    fun `resolveFile with deep parent dir traversal throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            storage.resolveFile("Pictures/FitLog/../../private_file")
        }
    }

    @Test
    fun `resolveFile with parent dir only traversal throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            storage.resolveFile("..")
        }
    }

    @Test
    fun `resolveFile with absolute path throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            storage.resolveFile("/absolute/path/file.jpg")
        }
    }

    @Test
    fun `resolveFile with blank path throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            storage.resolveFile("")
        }
    }

    @Test
    fun `relative paths never start with slash`() {
        val pending = storage.createPendingPhoto("image/png", "png")
        try {
            val path = pending.relativePath
            assertTrue("Relative path should not start with /", !path.startsWith("/"))
            assertTrue("Relative path should not contain /../", !path.contains("/../"))
            assertTrue("Relative path should not be absolute", !File(path).isAbsolute)
        } finally {
            storage.discardPendingMedia(pending)
        }
    }

    @Test
    fun `mime type to extension mapping covers common types`() {
        val jpeg = storage.createPendingPhoto("image/jpeg", "jpg")
        assertTrue(jpeg.relativePath.startsWith("Pictures/"))
        assertTrue(jpeg.relativePath.endsWith(".jpg"))
        storage.discardPendingMedia(jpeg)

        val png = storage.createPendingPhoto("image/png", "png")
        assertTrue(png.relativePath.startsWith("Pictures/"))
        assertTrue(png.relativePath.endsWith(".png"))
        storage.discardPendingMedia(png)

        val video = storage.createPendingVideo("video/mp4", "mp4")
        assertTrue(video.relativePath.startsWith("Movies/"))
        assertTrue(video.relativePath.endsWith(".mp4"))
        storage.discardPendingMedia(video)
    }

    @Test
    fun `commitPendingMedia renames file from pending to final`() {
        val pending = storage.createPendingPhoto("image/jpeg")
        val relativePath = storage.commitPendingMedia(pending)

        // Verify the pending file is gone
        assertTrue("Pending file should no longer exist", !pending.pendingFile.exists())
        // Verify the final file exists
        assertTrue("Committed file should exist", pending.finalFile.exists())
        // Clean up
        storage.deleteFile(relativePath)
    }

    @Test
    fun `discardPendingMedia removes pending file`() {
        val pending = storage.createPendingPhoto("image/jpeg")
        storage.discardPendingMedia(pending)

        assertTrue("Pending file should be deleted", !pending.pendingFile.exists())
        assertTrue("Final file should not exist before commit", !pending.finalFile.exists())
    }

    @Test
    fun `deleteFile removes resolved file`() {
        // Create and commit a file
        val pending = storage.createPendingPhoto("image/jpeg")
        val relativePath = storage.commitPendingMedia(pending)

        // Verify it exists
        assertTrue("File should exist after commit", storage.resolveFile(relativePath).exists())

        // Delete it
        val deleted = storage.deleteFile(relativePath)
        assertTrue("deleteFile should return true", deleted)
        assertTrue("File should be gone", !storage.resolveFile(relativePath).exists())
    }

    @Test
    fun `calculateSize returns correct size`() {
        val pending = storage.createPendingPhoto("image/jpeg")
        try {
            // Write some data via the pending file
            pending.pendingFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
            val relativePath = storage.commitPendingMedia(pending)

            val size = storage.calculateSize(relativePath)
            assertEquals(5L, size)
            storage.deleteFile(relativePath)
        } finally {
            // Ensure cleanup in case of exception
            if (pending.pendingFile.exists()) {
                pending.pendingFile.delete()
            }
            if (pending.finalFile.exists()) {
                pending.finalFile.delete()
            }
        }
    }

    @Test
    fun `scanOrphanFiles does not return pending files`() {
        val pending = storage.createPendingPhoto("image/jpeg")
        try {
            // Before committing, the orphan scan should not see the pending file
            val orphans = storage.scanOrphanFiles()
            assertTrue("Pending file should not appear in scan", !orphans.contains(pending.relativePath))
        } finally {
            storage.discardPendingMedia(pending)
        }
    }

    @Test
    fun `pictureRoot returns canonical directory`() {
        val root = storage.pictureRoot()
        assertTrue("pictureRoot should be a directory", root.isDirectory)
        assertTrue("pictureRoot should be canonical", root.isAbsolute)
        assertTrue("pictureRoot should end with FitLog", root.name == "FitLog")
    }

    @Test
    fun `videoRoot returns canonical directory`() {
        val root = storage.videoRoot()
        assertTrue("videoRoot should be a directory", root.isDirectory)
        assertTrue("videoRoot should be canonical", root.isAbsolute)
        assertTrue("videoRoot should end with FitLog", root.name == "FitLog")
    }

    @Test
    fun `commitPendingMedia with non-existent pending file throws`() {
        val pending = storage.createPendingPhoto("image/jpeg")
        // Delete the pending file before commit
        pending.pendingFile.delete()
        assertThrows(java.io.IOException::class.java) {
            storage.commitPendingMedia(pending)
        }
    }
}
