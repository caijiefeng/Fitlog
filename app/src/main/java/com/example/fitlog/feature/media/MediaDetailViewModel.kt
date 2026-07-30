package com.example.fitlog.feature.media

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.data.repository.MediaRecord
import com.example.fitlog.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

data class MediaDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val record: MediaRecord? = null,
    val isEditingNote: Boolean = false,
    val editNoteText: String = "",
    val showDeleteConfirm: Boolean = false,
    val deleteCompleted: Boolean = false,
    val saveToGalleryInProgress: Boolean = false,
    val saveToGallerySuccess: Boolean = false,
    val saveToGalleryError: String? = null,
)

@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaDetailUiState())
    val uiState: StateFlow<MediaDetailUiState> = _uiState.asStateFlow()

    private var recordId: Long = 0

    fun load(mediaId: Long) {
        recordId = mediaId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val record = mediaRepository.getById(mediaId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    record = record,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load media",
                )
            }
        }
    }

    /** Returns the absolute file path for display in storage info. */
    fun getStoragePath(): String? {
        val record = _uiState.value.record ?: return null
        return try {
            mediaRepository.resolveFile(record.relativePath).absolutePath
        } catch (_: Exception) { null }
    }

    /** Exports the current media file to the system gallery via MediaStore. */
    fun saveCopyToGallery(context: Context) {
        val record = _uiState.value.record ?: return
        _uiState.value = _uiState.value.copy(
            saveToGalleryInProgress = true,
            saveToGalleryError = null,
        )
        viewModelScope.launch {
            try {
                val file = mediaRepository.resolveFile(record.relativePath)
                if (!file.exists()) {
                    _uiState.value = _uiState.value.copy(
                        saveToGalleryInProgress = false,
                        saveToGalleryError = "File not found",
                    )
                    return@launch
                }

                val relativePath = if (record.mediaType.name.startsWith("VIDEO")) {
                    Environment.DIRECTORY_MOVIES + File.separator + "FitLog"
                } else {
                    Environment.DIRECTORY_PICTURES + File.separator + "FitLog"
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, record.mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val collectionUri = if (record.mediaType.name.startsWith("VIDEO")) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val uri = context.contentResolver.insert(collectionUri, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        FileInputStream(file).use { input ->
                            input.copyTo(output)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(uri, contentValues, null, null)
                    }
                    _uiState.value = _uiState.value.copy(
                        saveToGalleryInProgress = false,
                        saveToGallerySuccess = true,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        saveToGalleryInProgress = false,
                        saveToGalleryError = "Failed to create gallery entry",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    saveToGalleryInProgress = false,
                    saveToGalleryError = e.message ?: "Export failed",
                )
            }
        }
    }

    fun dismissSaveToGallerySuccess() {
        _uiState.value = _uiState.value.copy(saveToGallerySuccess = false)
    }

    fun toggleFavorite() {
        val record = _uiState.value.record ?: return
        viewModelScope.launch {
            try {
                mediaRepository.update(record.copy(isFavorite = !record.isFavorite))
                _uiState.value = _uiState.value.copy(
                    record = record.copy(isFavorite = !record.isFavorite),
                )
            } catch (_: Exception) { }
        }
    }

    fun startEditingNote() {
        val record = _uiState.value.record ?: return
        _uiState.value = _uiState.value.copy(
            isEditingNote = true,
            editNoteText = record.note ?: "",
        )
    }

    fun updateEditNoteText(text: String) {
        _uiState.value = _uiState.value.copy(editNoteText = text)
    }

    fun saveNote() {
        val record = _uiState.value.record ?: return
        val newNote = _uiState.value.editNoteText.trim().ifBlank { null }
        viewModelScope.launch {
            try {
                mediaRepository.update(record.copy(note = newNote))
                _uiState.value = _uiState.value.copy(
                    record = record.copy(note = newNote),
                    isEditingNote = false,
                )
            } catch (_: Exception) { }
        }
    }

    fun cancelEditingNote() {
        _uiState.value = _uiState.value.copy(isEditingNote = false)
    }

    fun showDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = true)
    }

    fun dismissDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false)
    }

    fun confirmDelete() {
        viewModelScope.launch {
            try {
                mediaRepository.delete(recordId)
                _uiState.value = _uiState.value.copy(
                    showDeleteConfirm = false,
                    deleteCompleted = true,
                )
            } catch (_: Exception) { }
        }
    }

    /** Resolves the absolute file path for the current record (for sharing). */
    fun resolveFile(): java.io.File? {
        val record = _uiState.value.record ?: return null
        return try {
            mediaRepository.resolveFile(record.relativePath)
        } catch (_: Exception) {
            null
        }
    }
}
