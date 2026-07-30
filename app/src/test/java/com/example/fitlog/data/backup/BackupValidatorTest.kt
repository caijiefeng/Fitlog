package com.example.fitlog.data.backup

import android.content.Context
import androidx.room.RoomDatabase
import com.example.fitlog.core.media.AppMediaStorage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Tests [BackupImporter] validation logic:
 * - Valid manifest + db.json passes analysis
 * - Wrong SHA-256 checksum fails
 * - Unsupported manifest version fails
 * - Missing db.json fails
 */
class BackupValidatorTest {

    private val db = mockk<RoomDatabase>(relaxed = true)
    private val appMediaStorage = mockk<AppMediaStorage>(relaxed = true)
    private val backupExporter = mockk<BackupExporter>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val fitLogDb = mockk<com.example.fitlog.core.database.FitLogDatabase>(relaxed = true)

    private lateinit var importer: BackupImporter

    @Before
    fun setUp() {
        importer = BackupImporter(
            db = fitLogDb,
            appMediaStorage = appMediaStorage,
            backupExporter = backupExporter,
            context = context,
        )
    }

    @Test
    fun `valid manifest and db_json passes analysis`() = runBlocking {
        // Build a valid ZIP
        val dbJson = JSONObject()
        dbJson.put("body_measurements", org.json.JSONArray())
        dbJson.put("exercises", org.json.JSONArray())

        val dbJsonBytes = dbJson.toString(2).toByteArray(Charsets.UTF_8)
        val checksum = sha256(dbJsonBytes)

        val manifestJson = JSONObject().apply {
            put("version", 1)
            put("app_version", "0.1.0")
            put("exported_at", System.currentTimeMillis())
            put("db_rows", 0)
            put("media_count", 0)
            put("db_checksum", checksum)
        }

        val zipBytes = buildZip(
            "manifest.json" to manifestJson.toString(2).toByteArray(),
            "db.json" to dbJsonBytes,
        )

        val summary = importer.analyze(ByteArrayInputStream(zipBytes))
        assertNotNull(summary)
        assertEquals(1, summary.manifest.version)
        assertEquals(0, summary.manifest.dbRows)
        assertEquals(0, summary.manifest.mediaCount)
        assertTrue(summary.mediaFiles.isEmpty())
    }

    @Test(expected = ImportException::class)
    fun `wrong checksum fails`() = runBlocking {
        val dbJson = JSONObject()
        dbJson.put("body_measurements", org.json.JSONArray())
        val dbJsonBytes = dbJson.toString(2).toByteArray(Charsets.UTF_8)

        // Use an obviously wrong checksum
        val manifestJson = JSONObject().apply {
            put("version", 1)
            put("app_version", "0.1.0")
            put("exported_at", System.currentTimeMillis())
            put("db_rows", 0)
            put("media_count", 0)
            put("db_checksum", "0000000000000000000000000000000000000000000000000000000000000000")
        }

        val zipBytes = buildZip(
            "manifest.json" to manifestJson.toString(2).toByteArray(),
            "db.json" to dbJsonBytes,
        )

        importer.analyze(ByteArrayInputStream(zipBytes))
    }

    @Test(expected = ImportException::class)
    fun `unsupported version fails`() = runBlocking {
        val dbJson = JSONObject()
        dbJson.put("body_measurements", org.json.JSONArray())
        val dbJsonBytes = dbJson.toString(2).toByteArray(Charsets.UTF_8)

        val manifestJson = JSONObject().apply {
            put("version", 99) // Unsupported version
            put("app_version", "0.1.0")
            put("exported_at", System.currentTimeMillis())
            put("db_rows", 0)
            put("media_count", 0)
            put("db_checksum", sha256(dbJsonBytes))
        }

        val zipBytes = buildZip(
            "manifest.json" to manifestJson.toString(2).toByteArray(),
            "db.json" to dbJsonBytes,
        )

        importer.analyze(ByteArrayInputStream(zipBytes))
    }

    @Test(expected = ImportException::class)
    fun `missing db_json fails`() = runBlocking {
        val manifestJson = JSONObject().apply {
            put("version", 1)
            put("app_version", "0.1.0")
            put("exported_at", System.currentTimeMillis())
            put("db_rows", 0)
            put("media_count", 0)
            put("db_checksum", "")
        }

        val zipBytes = buildZip(
            "manifest.json" to manifestJson.toString(2).toByteArray(),
            // No db.json entry
        )

        importer.analyze(ByteArrayInputStream(zipBytes))
    }

    @Test(expected = ImportException::class)
    fun `missing manifest_json fails`() = runBlocking {
        val dbJson = JSONObject()
        val zipBytes = buildZip(
            "db.json" to dbJson.toString(2).toByteArray(),
            // No manifest.json entry
        )

        importer.analyze(ByteArrayInputStream(zipBytes))
    }

    @Test(expected = ImportException::class)
    fun `empty zip fails`() = runBlocking {
        val zipBytes = buildZip()
        importer.analyze(ByteArrayInputStream(zipBytes))
    }

    @Test
    fun `valid manifest with media files shows media in summary`() = runBlocking {
        val dbJson = JSONObject().apply {
            put("body_measurements", org.json.JSONArray())
        }
        val dbJsonBytes = dbJson.toString(2).toByteArray(Charsets.UTF_8)

        val manifestJson = JSONObject().apply {
            put("version", 1)
            put("app_version", "0.1.0")
            put("exported_at", System.currentTimeMillis())
            put("db_rows", 0)
            put("media_count", 2)
            put("db_checksum", sha256(dbJsonBytes))
        }

        val zipBytes = buildZip(
            "manifest.json" to manifestJson.toString(2).toByteArray(),
            "db.json" to dbJsonBytes,
            "media/Pictures/FitLog/img1.jpg" to "fake-image-data-1".toByteArray(),
            "media/Pictures/FitLog/img2.jpg" to "fake-image-data-2".toByteArray(),
        )

        val summary = importer.analyze(ByteArrayInputStream(zipBytes))
        assertNotNull(summary)
        assertEquals(2, summary.mediaFiles.size)
        assertTrue(summary.mediaFiles.contains("Pictures/FitLog/img1.jpg"))
        assertTrue(summary.mediaFiles.contains("Pictures/FitLog/img2.jpg"))
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }

    /**
     * Builds a ZIP archive in memory from entry name → content pairs.
     */
    private fun buildZip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bos = java.io.ByteArrayOutputStream()
        val zos = ZipOutputStream(bos)
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

        zos.finish()
        zos.close()
        return bos.toByteArray()
    }
}
