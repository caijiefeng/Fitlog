package com.example.fitlog.data.export

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages file export via Android's Storage Access Framework (SAF).
 *
 * The [DataManagementScreen] creates an SAF launcher
 * ([androidx.activity.result.contract.ActivityResultContracts.CreateDocument])
 * and passes the returned [Uri] to the ViewModel, which calls methods on this
 * manager to write data.
 */
@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Writes [data] to the given [uri] via ContentResolver.
     *
     * @return [Result.success] with the same [uri] on success, or
     *         [Result.failure] with the exception on failure.
     */
    fun writeToUri(uri: Uri, data: ByteArray): Result<Uri> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedOutputStream(outputStream).use { buffered ->
                    buffered.write(data)
                    buffered.flush()
                }
            } ?: return Result.failure(IllegalStateException("Cannot open output stream for $uri"))
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Writes [data] to the given [uri] via ContentResolver.
     * Convenience overload for [writeToUri] that returns `true` on success.
     *
     * @return `true` if the write succeeded, `false` otherwise.
     */
    fun writeToUriOrThrow(uri: Uri, data: ByteArray) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            BufferedOutputStream(outputStream).use { buffered ->
                buffered.write(data)
                buffered.flush()
            }
        } ?: throw IllegalStateException("Cannot open output stream for $uri")
    }
}
