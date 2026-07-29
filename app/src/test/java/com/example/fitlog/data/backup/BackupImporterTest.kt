package com.example.fitlog.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.example.fitlog.core.database.FitLogDatabase
import com.example.fitlog.core.media.AppMediaStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupImporterTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var db: FitLogDatabase
    private lateinit var backupManager: BackupManager
    private lateinit var mediaStorage: AppMediaStorage
    private lateinit var importer: BackupImporter

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        db = mockk(relaxed = true)
        backupManager = mockk(relaxed = true)
        mediaStorage = mockk(relaxed = true)

        every { context.contentResolver } returns contentResolver
        every { context.cacheDir } returns File("/tmp/test-cache")

        importer = BackupImporter(
            context = context,
            db = db,
            backupManager = backupManager,
            mediaStorage = mediaStorage,
        )
    }

    private fun createTestZip(
        version: Int = 1,
        dbContent: String = """{"exercises":[]}""",
        dbChecksum: String? = null,
        mediaPaths: List<String> = emptyList(),
    ): ByteArray {
        val checksum = dbChecksum ?: sha256Hex(dbContent.toByteArray())
        val manifest = BackupManifest(
            version = version,
            appVersion = "0.1.0",
            exportedAt = 1000L,
            dbRows = 0,
            mediaCount = 0,
            dbChecksum = checksum,
        )
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifest.toJson().toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("db.json"))
            zos.write(dbContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            for (path in mediaPaths) {
                zos.putNextEntry(ZipEntry("media/$path"))
                zos.write("fake-media-content".toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `import rejects unsupported version`() = runTest {
        val zipBytes = createTestZip(version = 99)
        every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(zipBytes)

        val result = importer.import(mockk())

        assertTrue("Expected VersionMismatch, got $result", result is BackupImporter.ImportResult.VersionMismatch)
        result as BackupImporter.ImportResult.VersionMismatch
        assertEquals(99, result.version)
        assertEquals(BackupManifest.CURRENT_VERSION, result.maxSupported)
    }

    @Test
    fun `import rejects version 0`() = runTest {
        val zipBytes = createTestZip(version = 0)
        every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(zipBytes)

        val result = importer.import(mockk())

        assertTrue("Expected VersionMismatch, got $result", result is BackupImporter.ImportResult.VersionMismatch)
    }

    @Test
    fun `import rejects checksum mismatch`() = runTest {
        val zipBytes = createTestZip(dbChecksum = "badchecksum")
        every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(zipBytes)

        val result = importer.import(mockk())

        assertTrue("Expected ChecksumMismatch, got $result", result is BackupImporter.ImportResult.ChecksumMismatch)
        result as BackupImporter.ImportResult.ChecksumMismatch
        assertEquals("badchecksum", result.expected)
        assertNotNull(result.actual)
    }

    @Test
    fun `import rejects corrupt zip`() = runTest {
        every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(
            "not a zip file".toByteArray()
        )

        val result = importer.import(mockk())

        assertTrue("Expected Corrupt, got $result", result is BackupImporter.ImportResult.Corrupt)
    }

    @Test
    fun `import rejects missing manifest`() = runTest {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("db.json"))
            zos.write("{}".toByteArray())
            zos.closeEntry()
        }
        every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(baos.toByteArray())

        val result = importer.import(mockk())

        assertTrue("Expected Corrupt, got $result", result is BackupImporter.ImportResult.Corrupt)
    }

    @Test
    fun `import rejects missing db json`() = runTest {
        val manifest = BackupManifest(version = 1, appVersion = "0.1.0")
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifest.toJson().toString(2).toByteArray())
            zos.closeEntry()
        }
        every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(baos.toByteArray())

        val result = importer.import(mockk())

        assertTrue("Expected Corrupt, got $result", result is BackupImporter.ImportResult.Corrupt)
    }

    @Test
    fun `import succeeds with valid zip`() = runTest {
        val dbContent = """{"exercises":[],"workout_templates":[],"body_measurements":[]}"""
        val zipBytes = createTestZip(dbContent = dbContent)

        every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(zipBytes)
        coEvery { backupManager.createBackup(any()) } returns BackupManager.BackupResult(mockk(), mockk())

        // Mock the database transaction support
        val mockDbRef = mockk<android.database.sqlite.SQLiteDatabase>(relaxed = true)
        every { db.openHelper } returns mockk {
            every { writableDatabase } returns mockDbRef
        }
        coEvery { db.withTransaction(any<suspend () -> Unit>()) } coAnswers {
            callOriginal()
        }

        val result = importer.import(mockk())

        assertTrue("Expected Success, got $result", result is BackupImporter.ImportResult.Success)
    }

    @Test
    fun `import creates pre-import backup`() = runTest {
        val dbContent = """{}"""
        val zipBytes = createTestZip(dbContent = dbContent)

        every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(zipBytes)
        coEvery { backupManager.createBackup(any()) } returns BackupManager.BackupResult(mockk(), mockk())

        val mockDbRef = mockk<android.database.sqlite.SQLiteDatabase>(relaxed = true)
        every { db.openHelper } returns mockk {
            every { writableDatabase } returns mockDbRef
        }
        coEvery { db.withTransaction(any<suspend () -> Unit>()) } coAnswers {
            callOriginal()
        }

        val result = importer.import(mockk())

        assertTrue(result is BackupImporter.ImportResult.Success)
        coVerify { backupManager.createBackup(any()) }
    }

    @Test
    fun `import succeeds when pre-import backup fails`() = runTest {
        val zipBytes = createTestZip()
        every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(zipBytes)
        // backup manager throws
        coEvery { backupManager.createBackup(any()) } throws RuntimeException("Backup failed")

        val mockDbRef = mockk<android.database.sqlite.SQLiteDatabase>(relaxed = true)
        every { db.openHelper } returns mockk {
            every { writableDatabase } returns mockDbRef
        }
        coEvery { db.withTransaction(any<suspend () -> Unit>()) } coAnswers {
            callOriginal()
        }

        val result = importer.import(mockk())

        // Should still succeed since pre-import backup failure is non-critical
        assertTrue("Expected Success, got $result", result is BackupImporter.ImportResult.Success)
        assertTrue((result as BackupImporter.ImportResult.Success).preImportBackupUri == null)
    }
}
