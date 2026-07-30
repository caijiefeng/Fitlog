package com.example.fitlog.feature.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.data.repository.MediaRecord
import com.example.fitlog.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val record: MediaRecord? = null,
    val isEditingNote: Boolean = false,
    val editNoteText: String = "",
    val showDeleteConfirm: Boolean = false,
    val deleteCompleted: Boolean = false,
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
