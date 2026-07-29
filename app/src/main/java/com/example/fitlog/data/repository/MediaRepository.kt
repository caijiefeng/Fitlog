package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.MediaRecordDao
import com.example.fitlog.core.database.entity.MediaRecordEntity
import com.example.fitlog.core.media.AppMediaStorage
import com.example.fitlog.domain.media.MediaCategory
import com.example.fitlog.domain.media.MediaType
import com.example.fitlog.domain.media.ProgressPose
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain representation of a media record.
 */
data class MediaRecord(
    val id: Long = 0,
    val mediaType: MediaType,
    val relativePath: String,
    val mimeType: String,
    val capturedAt: Long,
    val date: Long, // epochDay
    val width: Int? = null,
    val height: Int? = null,
    val durationMillis: Long? = null,
    val sizeBytes: Long,
    val workoutSessionId: Long? = null,
    val bodyMeasurementId: Long? = null,
    val checkInId: Long? = null,
    val exerciseSessionId: Long? = null,
    val foodRecordId: Long? = null,
    val category: MediaCategory,
    val poseTag: ProgressPose? = null,
    val note: String? = null,
    val isFavorite: Boolean = false,
)

/**
 * Result of a delete operation on a media record.
 */
sealed interface MediaDeleteResult {
    /** The record and its file were both deleted successfully. */
    data object Deleted : MediaDeleteResult
    /** The database record was deleted but the file was already missing on disk. */
    data object FileAlreadyMissing : MediaDeleteResult
}

@Singleton
class MediaRepository @Inject constructor(
    private val mediaRecordDao: MediaRecordDao,
    private val mediaStorage: AppMediaStorage,
) {

    // ── Write ────────────────────────────────────────────────────────────────

    /**
     * Saves a media record to the database. The file must already be committed
     * via [AppMediaStorage.commitPendingMedia].
     */
    suspend fun save(record: MediaRecord): MediaRecord {
        val fileSize = if (record.sizeBytes > 0L) record.sizeBytes
        else mediaStorage.calculateSize(record.relativePath)

        val entity = MediaRecordEntity(
            mediaType = record.mediaType,
            relativePath = record.relativePath,
            mimeType = record.mimeType,
            capturedAt = record.capturedAt,
            date = record.date,
            width = record.width,
            height = record.height,
            durationMillis = record.durationMillis,
            sizeBytes = fileSize,
            workoutSessionId = record.workoutSessionId,
            bodyMeasurementId = record.bodyMeasurementId,
            checkInId = record.checkInId,
            exerciseSessionId = record.exerciseSessionId,
            foodRecordId = record.foodRecordId,
            category = record.category,
            poseTag = record.poseTag,
            note = record.note,
            isFavorite = record.isFavorite,
        )
        val id = mediaRecordDao.insert(entity)
        return record.copy(id = id)
    }

    /**
     * Deletes a media record: removes the file on disk first, then deletes
     * the database row.
     *
     * If the file exists but cannot be deleted, [MediaDeleteResult.Deleted] is
     * **not** returned — this throws [java.io.IOException] instead so the
     * caller (and the DB transaction) can roll back.
     *
     * If the file does not exist or is successfully deleted, the database row
     * is removed and the corresponding [MediaDeleteResult] is returned.
     */
    suspend fun delete(id: Long): MediaDeleteResult {
        val entity = mediaRecordDao.getById(id) ?: return MediaDeleteResult.FileAlreadyMissing

        val file = mediaStorage.resolveFile(entity.relativePath)
        return if (file.exists()) {
            // File exists — try to delete it
            if (file.delete()) {
                mediaRecordDao.delete(id)
                MediaDeleteResult.Deleted
            } else {
                // File exists but could not be deleted — throw to prevent DB delete
                throw java.io.IOException("Failed to delete file: ${file.absolutePath}")
            }
        } else {
            // File already missing — delete DB record only
            mediaRecordDao.delete(id)
            MediaDeleteResult.FileAlreadyMissing
        }
    }

    /**
     * Updates a media record's metadata in the database.
     */
    suspend fun update(record: MediaRecord) {
        val existing = mediaRecordDao.getById(record.id) ?: return
        mediaRecordDao.update(existing.copy(
            category = record.category,
            poseTag = record.poseTag,
            note = record.note,
            isFavorite = record.isFavorite,
            width = record.width,
            height = record.height,
            durationMillis = record.durationMillis,
            sizeBytes = record.sizeBytes,
            updatedAt = System.currentTimeMillis(),
        ))
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    suspend fun getById(id: Long): MediaRecord? {
        return mediaRecordDao.getById(id)?.toDomain()
    }

    suspend fun getByDateRange(startEpochDay: Long, endEpochDay: Long): List<MediaRecord> {
        return mediaRecordDao.getByDateRange(startEpochDay, endEpochDay).map { it.toDomain() }
    }

    suspend fun getByCategory(category: MediaCategory): List<MediaRecord> {
        return mediaRecordDao.getByCategory(category.name).map { it.toDomain() }
    }

    suspend fun getByWorkoutSession(sessionId: Long): List<MediaRecord> {
        return mediaRecordDao.getByWorkoutSession(sessionId).map { it.toDomain() }
    }

    suspend fun getByBodyMeasurement(measurementId: Long): List<MediaRecord> {
        return mediaRecordDao.getByBodyMeasurement(measurementId).map { it.toDomain() }
    }

    suspend fun getByCheckIn(checkInId: Long): List<MediaRecord> {
        return mediaRecordDao.getByCheckIn(checkInId).map { it.toDomain() }
    }

    suspend fun getFavorites(): List<MediaRecord> {
        return mediaRecordDao.getFavorites().map { it.toDomain() }
    }

    suspend fun getAll(): List<MediaRecord> {
        return mediaRecordDao.getAll().map { it.toDomain() }
    }

    /**
     * Returns the absolute [File] for a record's media, or null if the
     * file does not exist.
     */
    fun resolveFile(relativePath: String): File {
        return mediaStorage.resolveFile(relativePath)
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private fun MediaRecordEntity.toDomain(): MediaRecord = MediaRecord(
        id = id,
        mediaType = mediaType,
        relativePath = relativePath,
        mimeType = mimeType,
        capturedAt = capturedAt,
        date = date,
        width = width,
        height = height,
        durationMillis = durationMillis,
        sizeBytes = sizeBytes,
        workoutSessionId = workoutSessionId,
        bodyMeasurementId = bodyMeasurementId,
        checkInId = checkInId,
        exerciseSessionId = exerciseSessionId,
        foodRecordId = foodRecordId,
        category = category,
        poseTag = poseTag,
        note = note,
        isFavorite = isFavorite,
    )
}
