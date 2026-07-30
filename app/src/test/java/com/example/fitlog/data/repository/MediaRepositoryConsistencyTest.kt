package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.MediaRecordDao
import com.example.fitlog.core.database.entity.MediaRecordEntity
import com.example.fitlog.core.media.AppMediaStorage
import com.example.fitlog.domain.media.MediaCategory
import com.example.fitlog.domain.media.MediaType
import com.example.fitlog.domain.media.ProgressPose
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Tests for [MediaRepository] focusing on consistency between the database
 * and the file system.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MediaRepositoryConsistencyTest {

    private val dao = mockk<MediaRecordDao>(relaxed = true)
    private val storage = mockk<AppMediaStorage>(relaxed = true)
    private lateinit var repository: MediaRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = MediaRepository(dao, storage)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    @Test
    fun `save inserts entity with correct fields`() = runTest(testDispatcher) {
        val record = MediaRecord(
            mediaType = MediaType.PHOTO,
            relativePath = "Pictures/FitLog/test.jpg",
            mimeType = "image/jpeg",
            capturedAt = 1000L,
            date = 20000L,
            sizeBytes = 5000L,
            category = MediaCategory.GENERAL,
        )

        coEvery { dao.insert(any()) } returns 42L

        val result = repository.save(record)

        assertEquals("Returned record should have the new id", 42L, result.id)
        assertEquals("relativePath should match", record.relativePath, result.relativePath)

        coVerify {
            dao.insert(match { entity ->
                entity.relativePath == "Pictures/FitLog/test.jpg" &&
                    entity.mimeType == "image/jpeg" &&
                    entity.sizeBytes == 5000L &&
                    entity.mediaType == MediaType.PHOTO &&
                    entity.category == MediaCategory.GENERAL
            })
        }
    }

    @Test
    fun `save without sizeBytes falls back to storage size`() = runTest(testDispatcher) {
        val record = MediaRecord(
            mediaType = MediaType.PHOTO,
            relativePath = "Pictures/FitLog/photo.jpg",
            mimeType = "image/jpeg",
            capturedAt = 1000L,
            date = 20000L,
            sizeBytes = 0L,
            category = MediaCategory.GENERAL,
        )

        every { storage.calculateSize(any()) } returns 9999L
        coEvery { dao.insert(any()) } returns 1L

        repository.save(record)

        coVerify {
            dao.insert(match { entity ->
                entity.sizeBytes == 9999L // Should have looked up actual file size
            })
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Test
    fun `delete removes file then removes db row`() = runTest(testDispatcher) {
        val entity = MediaRecordEntity(
            id = 1,
            mediaType = MediaType.PHOTO,
            relativePath = "Pictures/FitLog/delete_test.jpg",
            mimeType = "image/jpeg",
            capturedAt = 1000L,
            date = 20000L,
            sizeBytes = 100L,
            category = MediaCategory.GENERAL,
        )

        coEvery { dao.getById(1L) } returns entity

        val testFile = createTempDir().resolve("delete_test.jpg")
        testFile.createNewFile()
        every { storage.resolveFile(any()) } returns testFile

        val result = repository.delete(1L)

        assertEquals("Should return Deleted", MediaDeleteResult.Deleted, result)
        assertTrue("File should be deleted", !testFile.exists())
        coVerify { dao.delete(1L) }
    }

    @Test
    fun `delete when file already missing deletes db and returns FileAlreadyMissing`() = runTest(testDispatcher) {
        val entity = MediaRecordEntity(
            id = 2,
            mediaType = MediaType.PHOTO,
            relativePath = "Pictures/FitLog/missing.jpg",
            mimeType = "image/jpeg",
            capturedAt = 1000L,
            date = 20000L,
            sizeBytes = 100L,
            category = MediaCategory.GENERAL,
        )

        coEvery { dao.getById(2L) } returns entity

        val missingFile = File("/nonexistent/missing.jpg")
        every { storage.resolveFile(any()) } returns missingFile
        every { missingFile.exists() } returns false

        val result = repository.delete(2L)

        assertEquals("Should return FileAlreadyMissing", MediaDeleteResult.FileAlreadyMissing, result)
        coVerify { dao.delete(2L) }
    }

    @Test
    fun `delete when entity not found returns FileAlreadyMissing`() = runTest(testDispatcher) {
        coEvery { dao.getById(999L) } returns null

        val result = repository.delete(999L)

        assertEquals("Should return FileAlreadyMissing for missing entity", MediaDeleteResult.FileAlreadyMissing, result)
        coVerify(exactly = 0) { dao.delete(any()) }
    }

    @Test(expected = java.io.IOException::class)
    fun `delete when file cannot be deleted throws IOException`() = runTest(testDispatcher) {
        val entity = MediaRecordEntity(
            id = 3,
            mediaType = MediaType.PHOTO,
            relativePath = "Pictures/FitLog/locked.jpg",
            mimeType = "image/jpeg",
            capturedAt = 1000L,
            date = 20000L,
            sizeBytes = 100L,
            category = MediaCategory.GENERAL,
        )

        coEvery { dao.getById(3L) } returns entity

        // Simulate a file that exists but cannot be deleted
        val lockedFile = mockk<File>()
        every { storage.resolveFile(any()) } returns lockedFile
        every { lockedFile.exists() } returns true
        every { lockedFile.delete() } returns false
        every { lockedFile.absolutePath } returns "/locked/locked.jpg"

        repository.delete(3L)
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Test
    fun `update modifies category, pose tag, note, favorite`() = runTest(testDispatcher) {
        val existingEntity = MediaRecordEntity(
            id = 1,
            mediaType = MediaType.PHOTO,
            relativePath = "Pictures/FitLog/update.jpg",
            mimeType = "image/jpeg",
            capturedAt = 1000L,
            date = 20000L,
            sizeBytes = 100L,
            category = MediaCategory.GENERAL,
        )

        coEvery { dao.getById(1L) } returns existingEntity

        val update = MediaRecord(
            id = 1,
            mediaType = MediaType.PHOTO,
            relativePath = "Pictures/FitLog/update.jpg",
            mimeType = "image/jpeg",
            capturedAt = 1000L,
            date = 20000L,
            sizeBytes = 100L,
            category = MediaCategory.BODY_PROGRESS,
            poseTag = ProgressPose.FRONT,
            note = "Progress photo",
            isFavorite = true,
        )

        repository.update(update)

        coVerify {
            dao.update(match { entity ->
                entity.category == MediaCategory.BODY_PROGRESS &&
                    entity.poseTag == ProgressPose.FRONT &&
                    entity.note == "Progress photo" &&
                    entity.isFavorite == true
            })
        }
    }

    @Test
    fun `update non-existent record does nothing`() = runTest(testDispatcher) {
        coEvery { dao.getById(999L) } returns null

        val record = MediaRecord(
            id = 999,
            mediaType = MediaType.PHOTO,
            relativePath = "gone.jpg",
            mimeType = "image/jpeg",
            capturedAt = 0L,
            date = 0L,
            sizeBytes = 0L,
            category = MediaCategory.GENERAL,
        )

        repository.update(record)

        coVerify(exactly = 0) { dao.update(any()) }
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Test
    fun `getById returns domain model when entity exists`() = runTest(testDispatcher) {
        val entity = MediaRecordEntity(
            id = 5,
            mediaType = MediaType.PHOTO,
            relativePath = "Pictures/FitLog/read.jpg",
            mimeType = "image/jpeg",
            capturedAt = 1000L,
            date = 20000L,
            sizeBytes = 100L,
            category = MediaCategory.BODY_PROGRESS,
        )
        coEvery { dao.getById(5L) } returns entity

        val result = repository.getById(5L)

        assertNotNull("Should return a domain record", result)
        assertEquals(5L, result?.id)
        assertEquals(MediaType.PHOTO, result?.mediaType)
        assertEquals(MediaCategory.BODY_PROGRESS, result?.category)
    }

    @Test
    fun `getById returns null when entity does not exist`() = runTest(testDispatcher) {
        coEvery { dao.getById(42L) } returns null

        val result = repository.getById(42L)
        assertEquals(null, result)
    }

    @Test
    fun `getAll returns all records mapped to domain`() = runTest(testDispatcher) {
        val entities = listOf(
            MediaRecordEntity(
                id = 1,
                mediaType = MediaType.PHOTO,
                relativePath = "pic1.jpg",
                mimeType = "image/jpeg",
                capturedAt = 100L,
                date = 20000L,
                sizeBytes = 50L,
                category = MediaCategory.GENERAL,
            ),
            MediaRecordEntity(
                id = 2,
                mediaType = MediaType.VIDEO,
                relativePath = "vid1.mp4",
                mimeType = "video/mp4",
                capturedAt = 200L,
                date = 20001L,
                sizeBytes = 5000L,
                category = MediaCategory.WORKOUT_FORM,
            ),
        )
        coEvery { dao.getAll() } returns entities

        val results = repository.getAll()

        assertEquals(2, results.size)
        assertEquals(MediaType.PHOTO, results[0].mediaType)
        assertEquals(MediaType.VIDEO, results[1].mediaType)
    }

    // ── File resolution ──────────────────────────────────────────────────────

    @Test
    fun `resolveFile delegates to storage`() {
        val expectedFile = File("/storage/emulated/0/Pictures/FitLog/test.jpg")
        every { storage.resolveFile("Pictures/FitLog/test.jpg") } returns expectedFile

        val result = repository.resolveFile("Pictures/FitLog/test.jpg")

        assertEquals(expectedFile, result)
        verify { storage.resolveFile("Pictures/FitLog/test.jpg") }
    }
}
