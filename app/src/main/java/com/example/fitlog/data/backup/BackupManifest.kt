package com.example.fitlog.data.backup

/**
 * Metadata persisted inside the backup ZIP as `manifest.json`.
 *
 * @property version Backup format version (bump when the format changes).
 * @property appVersion Human-readable app version string.
 * @property dbVersion Room database version number.
 * @property exportedAt Unix epoch millis when the backup was created.
 * @property totalRows Total number of rows across all tables.
 * @property rowCounts Per-table row counts keyed by table name.
 * @property mediaCount Number of media records in the database (not files packed).
 * @property dbChecksum SHA-256 hex digest of the raw `db.json` content.
 */
data class BackupManifest(
    val version: Int = CURRENT_VERSION,
    val appVersion: String,
    val dbVersion: Int = 0,
    val exportedAt: Long = System.currentTimeMillis(),
    val totalRows: Int = 0,
    val rowCounts: Map<String, Int> = emptyMap(),
    val mediaCount: Int = 0,
    val dbChecksum: String = "",
) {
    companion object {
        /** Bump when the backup format changes. */
        const val CURRENT_VERSION = 1
    }
}
