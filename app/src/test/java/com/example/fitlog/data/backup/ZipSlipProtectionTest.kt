package com.example.fitlog.data.backup

import com.example.fitlog.core.database.FitLogDatabase
import com.example.fitlog.core.media.AppMediaStorage
import io.mockk.mockk
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Tests that [BackupImporter] rejects ZIP entries with path traversal
 * (ZIP Slip) and enforces entry count / size limits.
 */
class ZipSlipProtectionTest {

    private lateinit var importer: BackupImporter
    private lateinit var mockDatabase: FitLogDatabase
    private lateinit var mockMediaStorage: AppMediaStorage

    @Before
    fun setUp() {
        mockDatabase = mockk(relaxed = true)
        mockMediaStorage = mockk(relaxed = true)
        importer = BackupImporter(
            db = mockDatabase,
            appMediaStorage = mockMediaStorage,
        )
    }

    // ── ZIP Slip: path traversal ────────────────────────────────────────────

    @Test
    fun `entry with leading slash is treated as absolute path`() {
        val zipBytes = buildZip(
            "manifest.json" to """{"backupVersion":1,"appVersion":"1.0","dbChecksum":""}""".toByteArray(),
            "db.json" to """{"body_measurements":[]}""".toByteArray(),
            "/etc/passwd" to "malicious".toByteArray(),
        )
        assertThrows(ImportException::class.java) {
            importer.analyze(ByteArrayInputStream(zipBytes))
        }
    }

    @Test
    fun `entry with parent directory traversal is rejected`() {
        val zipBytes = buildZip(
            "manifest.json" to """{"backupVersion":1,"appVersion":"1.0","dbChecksum":""}""".toByteArray(),
            "db.json" to """{"body_measurements":[]}""".toByteArray(),
            "../outside.txt" to "malicious".toByteArray(),
        )
        assertThrows(ImportException::class.java) {
            importer.analyze(ByteArrayInputStream(zipBytes))
        }
    }

    @Test
    fun `entry with deeply nested parent traversal is rejected`() {
        val zipBytes = buildZip(
            "manifest.json" to """{"backupVersion":1,"appVersion":"1.0","dbChecksum":""}""".toByteArray(),
            "db.json" to """{"body_measurements":[]}""".toByteArray(),
            "media/../../outside.txt" to "malicious".toByteArray(),
        )
        assertThrows(ImportException::class.java) {
            importer.analyze(ByteArrayInputStream(zipBytes))
        }
    }

    @Test
    fun `entry with encoded traversal (using backslash on linux) is rejected`() {
        val zipBytes = buildZip(
            "manifest.json" to """{"backupVersion":1,"appVersion":"1.0","dbChecksum":""}""".toByteArray(),
            "db.json" to """{"body_measurements":[]}""".toByteArray(),
            "..\\outside.txt" to "malicious".toByteArray(),
        )
        assertThrows(ImportException::class.java) {
            importer.analyze(ByteArrayInputStream(zipBytes))
        }
    }

    @Test
    fun `entry with double dot in middle is rejected`() {
        val zipBytes = buildZip(
            "manifest.json" to """{"backupVersion":1,"appVersion":"1.0","dbChecksum":""}""".toByteArray(),
            "db.json" to """{"body_measurements":[]}""".toByteArray(),
            "media/../db.json" to "shadow".toByteArray(),
        )
        assertThrows(ImportException::class.java) {
            importer.analyze(ByteArrayInputStream(zipBytes))
        }
    }

    @Test
    fun `valid paths are not rejected`() {
        val zipBytes = buildZip(
            "manifest.json" to """{"backupVersion":1,"appVersion":"1.0","dbChecksum":""}""".toByteArray(),
            "db.json" to """{"body_measurements":[]}""".toByteArray(),
            "media/Pictures/FitLog/photo.jpg" to "valid".toByteArray(),
            "media/Movies/FitLog/video.mp4" to "valid".toByteArray(),
        )
        // Should not throw
        val summary = importer.analyze(ByteArrayInputStream(zipBytes))
        assert(summary.mediaFiles.size == 2)
    }

    // ── Entry count limit ───────────────────────────────────────────────────

    @Test
    fun `backup with more than max entries is rejected`() {
        val zipBytes = buildZipWithEntryCount(BackupImporter.MAX_ENTRIES + 1)
        assertThrows(ImportException::class.java) {
            importer.analyze(ByteArrayInputStream(zipBytes))
        }
    }

    @Test
    fun `backup with exactly max entries is accepted`() {
        val zipBytes = buildZipWithEntryCount(BackupImporter.MAX_ENTRIES)
        // Should not throw
        importer.analyze(ByteArrayInputStream(zipBytes))
    }

    // ── Entry size limit ────────────────────────────────────────────────────

    @Test
    fun `entry exceeding max size is rejected`() {
        val largeData = ByteArray(BackupImporter.MAX_ENTRY_SIZE.toInt() + 1)
        val zipBytes = buildZip(
            "manifest.json" to """{"backupVersion":1,"appVersion":"1.0","dbChecksum":""}""".toByteArray(),
            "db.json" to """{"body_measurements":[]}""".toByteArray(),
            "large_file.bin" to largeData,
        )
        assertThrows(ImportException::class.java) {
            importer.analyze(ByteArrayInputStream(zipBytes))
        }
    }

    // ── Total size limit ────────────────────────────────────────────────────

    @Test
    fun `total uncompressed size exceeding max is rejected`() {
        // Create entries that together exceed MAX_TOTAL_SIZE
        val entrySize = BackupImporter.MAX_ENTRY_SIZE // 10 MB each
        val entryCount = (BackupImporter.MAX_TOTAL_SIZE / entrySize).toInt() + 2 // 52 entries
        val data = ByteArray(entrySize.toInt())

        val entries = mutableListOf<Pair<String, ByteArray>>()
        entries.add("manifest.json" to """{"backupVersion":1,"appVersion":"1.0","dbChecksum":""}""".toByteArray())
        entries.add("db.json" to """{"body_measurements":[]}""".toByteArray())
        for (i in 0 until entryCount) {
            entries.add("file_$i.bin" to data)
        }
        val zipBytes = buildZip(*entries.toTypedArray())

        assertThrows(ImportException::class.java) {
            importer.analyze(ByteArrayInputStream(zipBytes))
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun buildZip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            val crc = CRC32()
            for ((name, data) in entries) {
                val entry = ZipEntry(name).apply {
                    size = data.size.toLong()
                    crc.reset(); crc.update(data); this.crc = crc.value
                }
                zos.putNextEntry(entry)
                zos.write(data)
                zos.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    /**
     * Builds a ZIP with [count] entries. Entry names are `entry_0`, `entry_1`, etc.
     */
    private fun buildZipWithEntryCount(count: Int): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            val data = "x".toByteArray()
            val crc = CRC32()
            for (i in 0 until count) {
                val entry = ZipEntry("entry_$i").apply {
                    size = data.size.toLong()
                    crc.reset(); crc.update(data); this.crc = crc.value
                }
                zos.putNextEntry(entry)
                zos.write(data)
                zos.closeEntry()
            }
        }
        return bos.toByteArray()
    }
}
