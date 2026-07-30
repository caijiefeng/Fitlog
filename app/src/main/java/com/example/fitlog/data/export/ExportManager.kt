package com.example.fitlog.data.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages file export via Storage Access Framework (SAF).
 *
 * The caller (typically a ViewModel or UI layer) obtains a [Uri] through
 * [android.content.Intent.ACTION_CREATE_DOCUMENT] and passes it to
 * [writeToUri] — this class provides the [OutputStream] and handles
 * the ContentResolver plumbing.
 */
@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Opens an [OutputStream] for the given [uri] and writes the content
     * produced by [block].  The stream is flushed and closed automatically
     * upon return — the caller does **not** need to close it.
     *
     * @param uri  A content URI from [android.content.Intent.ACTION_CREATE_DOCUMENT].
     * @param mimeType  The MIME type of the file (e.g. "text/csv", "application/zip").
     * @param block  Lambda that receives an open [OutputStream] and writes content.
     */
    fun writeToUri(
        uri: Uri,
        mimeType: String,
        block: (OutputStream) -> Unit,
    ) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            block(stream)
        } ?: throw IllegalStateException("Cannot open output stream for $uri")
    }

    /**
     * Creates a new file in a user-selected directory (from
     * [android.content.Intent.ACTION_OPEN_DOCUMENT_TREE]) and writes content.
     *
     * @param treeUri  A content URI from ACTION_OPEN_DOCUMENT_TREE.
     * @param fileName  The desired file name.
     * @param mimeType  The MIME type of the file.
     * @param block  Lambda that receives an open [OutputStream] and writes content.
     * @return The [Uri] of the created file, or `null` if creation failed.
     */
    fun writeToDirectory(
        treeUri: Uri,
        fileName: String,
        mimeType: String,
        block: (OutputStream) -> Unit,
    ): Uri? {
        val contentResolver = context.contentResolver
        val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            fileName,
        )
        val values = ContentValues().apply {
            put(OpenableColumns.DISPLAY_NAME, fileName)
            put(android.provider.DocumentsContract.COLUMN_MIME_TYPE, mimeType)
        }
        val createdUri = contentResolver.insert(docUri, values) ?: return null
        contentResolver.openOutputStream(createdUri)?.use { stream ->
            block(stream)
        }
        return createdUri
    }
}
