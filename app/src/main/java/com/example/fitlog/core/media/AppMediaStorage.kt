package com.example.fitlog.core.media

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages media file storage on the device's external files directory.
 *
 * All stored paths are **relative** to the storage root.  No absolute
 * path is ever persisted in the database.  The storage root is obtained
 * from [Context.getExternalFilesDir] so the files are automatically
 * cleaned up when the app is uninstalled.
 *
 * Write flow:
 * 1. Call [createPendingPhoto] or [createPendingVideo] to get a temporary
 *    `.pending` output stream.
 * 2. Write the media data.
 * 3. Call [commitPendingMedia] to atomically rename the `.pending` file
 *    to its final name and return the relative path.
 * 4. Call [discardPendingMedia] to delete a `.pending` file if the
 *    capture was cancelled.
 */
@Singleton
class AppMediaStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val PHOTO_DIR = "Pictures"
        private const val VIDEO_DIR = "Movies"
        private const val PENDING_SUFFIX = ".pending"
        private const val RANDOM_SUFFIX_LENGTH = 8

        /**
         * Characters used for the random filename suffix.
         */
        private val RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray()
    }

    private val random = SecureRandom()

    /** Root directory for pictures, relative to the app's external files dir. */
    private val picturesDir: File
        get() = File(
            context.getExternalFilesDir(null),
            PHOTO_DIR,
        ).also { it.mkdirs() }

    /** Root directory for videos, relative to the app's external files dir. */
    private val videosDir: File
        get() = File(
            context.getExternalFilesDir(null),
            VIDEO_DIR,
        ).also { it.mkdirs() }

    // ── Pending file creation ─────────────────────────────────────────────

    /**
     * Creates a pending photo file and returns a [PendingMedia] handle
     * containing the output stream and the target file path (before the
     * final rename).
     */
    fun createPendingPhoto(
        mimeType: String,
        extension: String = mimeTypeToExtension(mimeType, "jpg"),
    ): PendingMedia {
        val fileName = generateFileName(extension)
        val pendingFile = File(picturesDir, "$fileName$PENDING_SUFFIX")
        pendingFile.parentFile?.mkdirs()
        val outputStream = FileOutputStream(pendingFile)
        return PendingMedia(
            outputStream = outputStream,
            pendingFile = pendingFile,
            finalFile = File(picturesDir, fileName),
            relativePath = "$PHOTO_DIR/$fileName",
        )
    }

    /**
     * Creates a pending video file and returns a [PendingMedia] handle.
     */
    fun createPendingVideo(
        mimeType: String,
        extension: String = mimeTypeToExtension(mimeType, "mp4"),
    ): PendingMedia {
        val fileName = generateFileName(extension)
        val pendingFile = File(videosDir, "$fileName$PENDING_SUFFIX")
        pendingFile.parentFile?.mkdirs()
        val outputStream = FileOutputStream(pendingFile)
        return PendingMedia(
            outputStream = outputStream,
            pendingFile = pendingFile,
            finalFile = File(videosDir, fileName),
            relativePath = "$VIDEO_DIR/$fileName",
        )
    }

    // ── Commit / discard ──────────────────────────────────────────────────

    /**
     * Atomically renames the pending file to its final name.
     * The output stream is closed before the rename.
     *
     * @return the relative path that should be stored in the database.
     */
    fun commitPendingMedia(pending: PendingMedia): String {
        try {
            pending.outputStream.close()
        } catch (_: IOException) {
            // ignore close errors
        }
        if (!pending.pendingFile.renameTo(pending.finalFile)) {
            throw IOException("Failed to rename ${pending.pendingFile} to ${pending.finalFile}")
        }
        return pending.relativePath
    }

    /**
     * Deletes a pending file without committing it.
     * The output stream is closed first.
     */
    fun discardPendingMedia(pending: PendingMedia) {
        try {
            pending.outputStream.close()
        } catch (_: IOException) {
            // ignore close errors
        }
        pending.pendingFile.delete()
    }

    // ── File resolution ───────────────────────────────────────────────────

    /**
     * Resolves a relative path to an absolute [File] on disk.
     *
     * @throws IllegalArgumentException if [relativePath] contains `..`
     *         (path traversal protection).
     */
    fun resolveFile(relativePath: String): File {
        requireNoPathTraversal(relativePath)
        return File(context.getExternalFilesDir(null), relativePath)
    }

    /**
     * Deletes the file at the given relative path.  Does **not** touch
     * the database — the caller is responsible for that.
     *
     * @return true if the file was deleted, false if it did not exist.
     */
    fun deleteFile(relativePath: String): Boolean {
        return resolveFile(relativePath).delete()
    }

    /**
     * Returns the size in bytes of the file at the given relative path,
     * or 0 if the file does not exist.
     */
    fun calculateSize(relativePath: String): Long {
        return resolveFile(relativePath).length()
    }

    /**
     * Scans the storage directories and returns the relative paths of all
     * committed (non-pending) files.  Useful for diffing against the
     * database.
     */
    fun scanOrphanFiles(): List<String> {
        val result = mutableListOf<String>()

        fun scanDir(dir: File, prefix: String) {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory) {
                    scanDir(file, "$prefix/${file.name}")
                } else if (!file.name.endsWith(PENDING_SUFFIX)) {
                    result.add("$prefix/${file.name}")
                }
            }
        }

        scanDir(picturesDir, PHOTO_DIR)
        scanDir(videosDir, VIDEO_DIR)
        return result
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Generates a unique filename: `timestamp_<random>.ext`
     */
    private fun generateFileName(extension: String): String {
        val timestamp = System.currentTimeMillis()
        val randomPart = buildString(RANDOM_SUFFIX_LENGTH) {
            repeat(RANDOM_SUFFIX_LENGTH) { append(RANDOM_CHARS[random.nextInt(RANDOM_CHARS.size)]) }
        }
        return "${timestamp}_$randomPart.$extension"
    }

    private fun mimeTypeToExtension(mimeType: String, default: String): String {
        return when {
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("mp4") -> "mp4"
            mimeType.contains("3gpp") -> "3gp"
            mimeType.contains("webm") -> "webm"
            else -> default
        }
    }

    /**
     * Throws [IllegalArgumentException] if [path] contains `..` segments
     * that could escape the storage root.
     */
    private fun requireNoPathTraversal(path: String) {
        require(".." !in path.split(File.separatorChar)) {
            "Path traversal detected in: $path"
        }
    }

    /**
     * Handle for a pending media file being written.
     */
    data class PendingMedia(
        val outputStream: FileOutputStream,
        val pendingFile: File,
        val finalFile: File,
        val relativePath: String,
    )
}
