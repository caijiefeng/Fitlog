package com.example.fitlog.core.media

import android.content.Context
import android.os.Environment
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
 *    `.pending` file handle.
 * 2. Write the media data using [PendingMedia.file] (append `.pending`).
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
        private const val PICTURES_SUBDIR = "Pictures"
        private const val VIDEOS_SUBDIR = "Movies"
        private const val FITLOG_SUBDIR = "FitLog"
        private const val PENDING_SUFFIX = ".pending"
        private const val RANDOM_SUFFIX_LENGTH = 8
        private val RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray()
    }

    private val random = SecureRandom()

    /**
     * Root directory for pictures.
     * Uses [Context.getExternalFilesDir] with [Environment.DIRECTORY_PICTURES]
     * and a "FitLog" subdirectory so files are scoped to the app.
     */
    private val picturesDir: File
        get() = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            FITLOG_SUBDIR,
        ).also { it.mkdirs() }

    /**
     * Root directory for videos.
     * Uses [Context.getExternalFilesDir] with [Environment.DIRECTORY_MOVIES]
     * and a "FitLog" subdirectory so files are scoped to the app.
     */
    private val videosDir: File
        get() = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            FITLOG_SUBDIR,
        ).also { it.mkdirs() }

    // ── Public root accessors ────────────────────────────────────────────────

    /** Returns the canonical root directory for picture media. */
    fun pictureRoot(): File = picturesDir.canonicalFile

    /** Returns the canonical root directory for video media. */
    fun videoRoot(): File = videosDir.canonicalFile

    // ── Pending file creation ────────────────────────────────────────────────

    /**
     * Creates a pending photo file and returns a [PendingMedia] handle.
     * The output **stream** is no longer part of [PendingMedia]; use
     * [PendingMedia.file] (with `.pending` suffix) to write data.
     */
    fun createPendingPhoto(
        mimeType: String,
        extension: String = mimeTypeToExtension(mimeType, "jpg"),
    ): PendingMedia {
        val fileName = generateFileName(extension)
        val pendingFile = File(picturesDir, "$fileName$PENDING_SUFFIX")
        pendingFile.parentFile?.mkdirs()
        // Touch the file so it exists on disk
        pendingFile.createNewFile()
        return PendingMedia(
            pendingFile = pendingFile,
            finalFile = File(picturesDir, fileName),
            relativePath = "$PICTURES_SUBDIR/$FITLOG_SUBDIR/$fileName",
        )
    }

    /**
     * Creates a pending video file and returns a [PendingMedia] handle.
     * The output **stream** is no longer part of [PendingMedia]; use
     * [PendingMedia.file] (with `.pending` suffix) to write data.
     */
    fun createPendingVideo(
        mimeType: String,
        extension: String = mimeTypeToExtension(mimeType, "mp4"),
    ): PendingMedia {
        val fileName = generateFileName(extension)
        val pendingFile = File(videosDir, "$fileName$PENDING_SUFFIX")
        pendingFile.parentFile?.mkdirs()
        pendingFile.createNewFile()
        return PendingMedia(
            pendingFile = pendingFile,
            finalFile = File(videosDir, fileName),
            relativePath = "$VIDEOS_SUBDIR/$FITLOG_SUBDIR/$fileName",
        )
    }

    // ── Commit / discard ─────────────────────────────────────────────────────

    /**
     * Atomically renames the pending file to its final name.
     *
     * @return the relative path that should be stored in the database.
     */
    fun commitPendingMedia(pending: PendingMedia): String {
        if (!pending.pendingFile.renameTo(pending.finalFile)) {
            throw IOException("Failed to rename ${pending.pendingFile} to ${pending.finalFile}")
        }
        return pending.relativePath
    }

    /**
     * Deletes a pending file without committing it.
     */
    fun discardPendingMedia(pending: PendingMedia) {
        pending.pendingFile.delete()
    }

    // ── File resolution ──────────────────────────────────────────────────────

    /**
     * Resolves a relative path to an absolute [File] on disk.
     *
     * @throws IllegalArgumentException if [relativePath] contains path traversal
     *         (`..` segments), is an absolute path, or resolves outside the
     *         canonical storage root.
     */
    fun resolveFile(relativePath: String): File {
        validatePath(relativePath)
        return File(
            context.getExternalFilesDir(null),
            relativePath,
        )
    }

    /**
     * Validates that [path] is safe to resolve:
     * - Not an absolute path
     * - Does not contain `..` segments
     * - Resolves within the canonical external files directory
     *
     * @throws IllegalArgumentException if validation fails.
     */
    private fun validatePath(path: String) {
        require(path.isNotBlank()) { "Path must not be blank" }
        require(!File(path).isAbsolute) { "Absolute path not allowed: $path" }
        require(".." !in path.split(File.separatorChar)) {
            "Path traversal detected in: $path"
        }
        // Verify the resolved file is within the canonical root
        val resolved = File(context.getExternalFilesDir(null), path)
        val canonicalRoot = context.getExternalFilesDir(null)?.canonicalFile
            ?: throw IOException("External files dir is null")
        val canonicalResolved = resolved.canonicalFile
        require(canonicalResolved.startsWith(canonicalRoot)) {
            "Path $path resolves outside the storage root: $canonicalResolved"
        }
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

        scanDir(pictureRoot(), "$PICTURES_SUBDIR/$FITLOG_SUBDIR")
        scanDir(videoRoot(), "$VIDEOS_SUBDIR/$FITLOG_SUBDIR")
        return result
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

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
     * Handle for a pending media file being written.
     *
     * @property pendingFile  The temporary file (with `.pending` suffix) that
     *                        the caller should write data into.
     * @property finalFile    The target file after commit (no `.pending` suffix).
     * @property relativePath The relative path (from storage root) that should
     *                        be stored in the database after commit.
     */
    data class PendingMedia(
        val pendingFile: File,
        val finalFile: File,
        val relativePath: String,
    )
}
