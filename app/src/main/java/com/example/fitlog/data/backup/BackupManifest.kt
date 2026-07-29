package com.example.fitlog.data.backup

/**
 * Metadata persisted inside the backup ZIP as `manifest.json`.
 */
data class BackupManifest(
    val version: Int = CURRENT_VERSION,
    val appVersion: String,
    val exportedAt: Long = System.currentTimeMillis(),
    val dbRows: Int = 0,
    val mediaCount: Int = 0,
    val dbChecksum: String = "",  // SHA-256 hex of the db.json content
) {
    companion object {
        /** Bump when the backup format changes. */
        const val CURRENT_VERSION = 1
    }
}
