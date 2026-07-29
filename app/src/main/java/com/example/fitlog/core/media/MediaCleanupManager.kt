package com.example.fitlog.core.media

import com.example.fitlog.core.database.dao.MediaRecordDao
import com.example.fitlog.core.database.entity.MediaRecordEntity
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a cleanup operation.
 */
data class CleanupResult(
    /** Number of orphan database records that were removed. */
    val recordsRemoved: Int = 0,
    /** Number of orphan files on disk that were deleted. */
    val filesRemoved: Int = 0,
    /** Number of stale `.pending` temp files that were removed. */
    val pendingRemoved: Int = 0,
)

/**
 * Handles cleanup of media records and files that are no longer needed.
 *
 * * **Orphan records** — database rows whose backing file no longer exists.
 * * **Orphan files** — files on disk with no corresponding database row.
 * * **Temp files** — stale `.pending` files left over from interrupted captures.
 * * **Storage stats** — aggregate counts and sizes.
 */
@Singleton
class MediaCleanupManager @Inject constructor(
    private val mediaRecordDao: MediaRecordDao,
    private val mediaStorage: AppMediaStorage,
) {

    /**
     * Returns records in the database whose backing file does not exist.
     * These can safely be deleted from the database (or the user can be
     * prompted first).
     */
    suspend fun findOrphanRecords(): List<MediaRecordEntity> {
        return mediaRecordDao.getAllRecords().filter { entity ->
            val file = mediaStorage.resolveFile(entity.relativePath)
            !file.exists()
        }
    }

    /**
     * Returns the relative paths of files on disk that have no corresponding
     * record in the database.
     */
    suspend fun findOrphanFiles(): List<String> {
        val dbPaths = mediaRecordDao.getAllRecords()
            .map { it.relativePath }
            .toHashSet()
        return mediaStorage.scanOrphanFiles().filter { it !in dbPaths }
    }

    /**
     * Deletes database records whose backing file no longer exists on disk.
     *
     * @return the number of records deleted.
     */
    suspend fun removeOrphanRecords(): Int {
        val orphans = findOrphanRecords()
        for (entity in orphans) {
            mediaRecordDao.delete(entity.id)
        }
        return orphans.size
    }

    /**
     * Deletes files on disk that have no corresponding record in the database.
     *
     * @return the number of files deleted.
     */
    suspend fun removeOrphanFiles(): Int {
        val orphans = findOrphanFiles()
        var count = 0
        for (relativePath in orphans) {
            try {
                if (mediaStorage.deleteFile(relativePath)) {
                    count++
                }
            } catch (_: Exception) {
                // Skip files that fail to delete
            }
        }
        return count
    }

    /**
     * Deletes all `.pending` temp files left over from interrupted captures.
     *
     * @return the number of temp files deleted.
     */
    fun removePendingFiles(): Int {
        var count = 0

        fun cleanDir(dir: File) {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory) {
                    cleanDir(file)
                } else if (file.name.endsWith(".pending") && file.delete()) {
                    count++
                }
            }
        }

        cleanDir(mediaStorage.pictureRoot())
        cleanDir(mediaStorage.videoRoot())
        return count
    }

    /**
     * Runs all three cleanup operations and returns a combined [CleanupResult].
     */
    suspend fun cleanupAll(): CleanupResult {
        return CleanupResult(
            recordsRemoved = removeOrphanRecords(),
            filesRemoved = removeOrphanFiles(),
            pendingRemoved = removePendingFiles(),
        )
    }

    /**
     * Storage statistics.
     */
    data class StorageStats(
        /** Total number of media records in the database. */
        val totalCount: Int,
        /** Total size in bytes of all files currently on disk. */
        val totalSizeBytes: Long,
        /** Number of records whose file does not exist on disk. */
        val orphanRecordCount: Int = 0,
        /** Number of files on disk with no database row. */
        val orphanFileCount: Int = 0,
    )

    /**
     * Computes aggregate storage statistics.
     */
    suspend fun getStorageStats(): StorageStats {
        val allRecords = mediaRecordDao.getAllRecords()
        val existingFiles = allRecords.filter { entity ->
            mediaStorage.resolveFile(entity.relativePath).exists()
        }

        val totalSizeBytes = existingFiles.sumOf { entity ->
            mediaStorage.calculateSize(entity.relativePath)
        }

        val orphanRecordCount = allRecords.size - existingFiles.size
        val orphanFileCount = findOrphanFiles().size

        return StorageStats(
            totalCount = allRecords.size,
            totalSizeBytes = totalSizeBytes,
            orphanRecordCount = orphanRecordCount,
            orphanFileCount = orphanFileCount,
        )
    }
}
