package com.example.fitlog.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManifestTest {

    @Test
    fun `manifest has current version 1`() {
        assertEquals(1, BackupManifest.CURRENT_VERSION)
    }

    @Test
    fun `manifest toString does not throw`() {
        val manifest = BackupManifest(
            version = 1,
            appVersion = "0.1.0",
            exportedAt = 1000L,
            dbRows = 42,
            mediaCount = 5,
            dbChecksum = "abc123",
        )
        assertTrue(manifest.toString().contains("abc123"))
    }

    @Test
    fun `manifest toJson produces correct keys`() {
        val manifest = BackupManifest(
            version = 1,
            appVersion = "0.1.0",
            exportedAt = 2000L,
            dbRows = 10,
            mediaCount = 3,
            dbChecksum = "deadbeef",
        )
        val json = manifest.toJson()

        assertEquals(1, json.getInt("version"))
        assertEquals("0.1.0", json.getString("appVersion"))
        assertEquals(2000L, json.getLong("exportedAt"))
        assertEquals(10, json.getInt("dbRows"))
        assertEquals(3, json.getInt("mediaCount"))
        assertEquals("deadbeef", json.getString("dbChecksum"))
    }

    @Test
    fun `manifest toJson round-trips`() {
        val original = BackupManifest(
            version = 1,
            appVersion = "0.1.0",
            exportedAt = 3000L,
            dbRows = 100,
            mediaCount = 7,
            dbChecksum = "feedface",
        )
        val json = original.toJson()
        val restored = BackupManifest(
            version = json.getInt("version"),
            appVersion = json.getString("appVersion"),
            exportedAt = json.getLong("exportedAt"),
            dbRows = json.getInt("dbRows"),
            mediaCount = json.getInt("mediaCount"),
            dbChecksum = json.getString("dbChecksum"),
        )
        assertEquals(original, restored)
    }

    @Test
    fun `manifest default values`() {
        val manifest = BackupManifest(appVersion = "test")
        assertEquals(BackupManifest.CURRENT_VERSION, manifest.version)
        assertTrue(manifest.exportedAt > 0)
        assertEquals(0, manifest.dbRows)
        assertEquals(0, manifest.mediaCount)
        assertEquals("", manifest.dbChecksum)
    }
}
