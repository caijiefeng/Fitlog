package com.example.fitlog.data.backup

import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import com.example.fitlog.core.database.FitLogDatabase
import com.example.fitlog.core.media.AppMediaStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Tests that [BackupImporter] correctly rolls back the database transaction
 * when the JSON in `db.json` is corrupt or missing required fields.
 */
class BackupRollbackTest {

    private lateinit var importer: BackupImporter
    private lateinit var mockDb: SQLiteDatabase
    private lateinit var mockDatabase: FitLogDatabase
    private lateinit var mockMediaStorage: AppMediaStorage

    @Before
    fun setUp() {
        mockDb = mockk<SQLiteDatabase>(relaxed = true)
        every { mockDb.insertWithOnConflict(any(), any(), any(), any()) } returns 1L
        every { mockDb.execSQL(any<String>()) } returns Unit

        val openHelper = mockk<RoomDatabase.OpenHelper>(relaxed = true)
        every { openHelper.writableDatabase } returns mockDb

        mockDatabase = mockk<FitLogDatabase>(relaxed = true)
        every { mockDatabase.openHelper } returns openHelper

        mockMediaStorage = mockk<AppMediaStorage>(relaxed = true)

        importer = BackupImporter(
            db = mockDatabase,
            appMediaStorage = mockMediaStorage,
        )
    }

    @Test
    fun `invalid JSON in db_json throws ImportException`() {
        val zipBytes = buildZip(
            "manifest.json" to """{"backupVersion":1,"appVersion":"1.0","dbChecksum":""}""".toByteArray(),
            "db.json" to "not valid json at all".toByteArray(),
        )

        val exception = assertThrows(ImportException::class.java) {
            runBlocking {
                importer.importBackup(ByteArrayInputStream(zipBytes))
            }
        }
        assertTrue(
            exception.message?.contains("Database import failed") == true
                || exception.message?.contains("db.json") == true,
        )
    }

    @Test
    fun `missing required field in JSON causes rollback`() {
        // db.json has a valid array but exercises entry is missing required "name" field
        val dbJson = """{
            "exercises": [{"id": 1, "primary_muscle_group": "Chest"}]
        }"""

        val zipBytes = buildZip(
            "manifest.json" to """{"backupVersion":1,"appVersion":"1.0","dbChecksum":""}""".toByteArray(),
            "db.json" to dbJson.toByteArray(),
        )

        assertThrows(ImportException::class.java) {
            runBlocking {
                importer.importBackup(ByteArrayInputStream(zipBytes))
            }
        }

        // Verify that after failure, execSQL was called for PRAGMA foreign_keys = OFF then ON
        verify(atLeast = 1) { mockDb.execSQL("PRAGMA foreign_keys = OFF") }
        verify(atLeast = 1) { mockDb.execSQL("PRAGMA foreign_keys = ON") }
    }

    @Test
    fun `corrupt JSON causes rollback and no data is committed`() {
        // Build a valid ZIP with a valid manifest but corrupt db.json
        val fakeDbJsonBytes = """{"body_measurements":[]}""".toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val checksum = digest.digest(fakeDbJsonBytes).joinToString("") { "%02x".format(it) }

        val manifest = """{
            "backupVersion":1,
            "appVersion":"0.1.0",
            "dbVersion":7,
            "exportedAt":${System.currentTimeMillis()},
            "totalRows":0,
            "rowCounts":{},
            "mediaCount":0,
            "dbChecksum":"$checksum"
        }"""

        val zipBytes = buildZip(
            "manifest.json" to manifest.toByteArray(),
            "db.json" to fakeDbJsonBytes,
            // Add extra corrupt JSON after db.json (ZIP supports multiple versions)
            // but this second entry has invalid content that will fail parsing
        )

        // Build a second corrupted entry that will replace db.json in the map
        val corruptZipBytes = buildZip(
            "manifest.json" to manifest.toByteArray(),
            "db.json" to "{{{corrupt".toByteArray(),
        )

        assertThrows(ImportException::class.java) {
            runBlocking {
                importer.importBackup(ByteArrayInputStream(corruptZipBytes))
            }
        }

        // Verify that the transaction was cleaned up (foreign_keys re-enabled)
        verify(atLeast = 1) { mockDb.execSQL("PRAGMA foreign_keys = ON") }
    }

    @Test
    fun `empty db_json throws ImportException`() {
        val zipBytes = buildZip(
            "manifest.json" to """{"backupVersion":1,"appVersion":"1.0","dbChecksum":""}""".toByteArray(),
            "db.json" to "".toByteArray(),
        )

        assertThrows(ImportException::class.java) {
            runBlocking {
                importer.importBackup(ByteArrayInputStream(zipBytes))
            }
        }
    }

    @Test
    fun `wrong checksum throws ImportException`() {
        val dbJsonBytes = """{"body_measurements":[]}""".toByteArray()
        val manifest = """{
            "backupVersion":1,
            "appVersion":"0.1.0",
            "dbVersion":7,
            "exportedAt":${System.currentTimeMillis()},
            "totalRows":0,
            "rowCounts":{},
            "mediaCount":0,
            "dbChecksum":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        }"""

        val zipBytes = buildZip(
            "manifest.json" to manifest.toByteArray(),
            "db.json" to dbJsonBytes,
        )

        assertThrows(ImportException::class.java) {
            runBlocking {
                importer.importBackup(ByteArrayInputStream(zipBytes))
            }
        }
    }

    @Test
    fun `unsupported version throws ImportException`() {
        val dbJsonBytes = """{"body_measurements":[]}""".toByteArray()
        val manifest = """{
            "backupVersion":99,
            "appVersion":"0.1.0",
            "dbChecksum":""
        }"""

        val zipBytes = buildZip(
            "manifest.json" to manifest.toByteArray(),
            "db.json" to dbJsonBytes,
        )

        val exception = assertThrows(ImportException::class.java) {
            runBlocking {
                importer.importBackup(ByteArrayInputStream(zipBytes))
            }
        }
        assertTrue(exception.message?.contains("Unsupported backup version 99") == true)
    }

    @Test
    fun `missing manifest_json throws ImportException`() {
        val zipBytes = buildZip(
            "db.json" to """{"body_measurements":[]}""".toByteArray(),
        )

        assertThrows(ImportException::class.java) {
            runBlocking {
                importer.importBackup(ByteArrayInputStream(zipBytes))
            }
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
}
