package com.example.fitlog.feature.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.data.repository.MediaRecord
import com.example.fitlog.data.repository.MediaRepository
import com.example.fitlog.domain.media.MediaCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MediaLibraryUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val groups: List<MediaDateGroup> = emptyList(),
    val selectedCategory: MediaCategoryFilter = MediaCategoryFilter.ALL,
    val favoritesOnly: Boolean = false,
    val sortNewestFirst: Boolean = true,
    val allRecords: List<MediaRecord> = emptyList(), // unfiltered
    // Multi-select state
    val isSelectMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val showBatchDeleteConfirm: Boolean = false,
    val batchDeleteInProgress: Boolean = false,
    val showBatchExportSuccess: Boolean = false,
)

data class MediaDateGroup(
    val date: LocalDate,
    val label: String,
    val items: List<MediaRecord>,
)

enum class MediaCategoryFilter(val queryValue: MediaCategory?, val labelResName: String) {
    ALL(null, "media_category_all"),
    BODY_PROGRESS(MediaCategory.BODY_PROGRESS, "media_category_body_progress"),
    WORKOUT_FORM(MediaCategory.WORKOUT_FORM, "media_category_workout_form"),
    MEAL(MediaCategory.MEAL, "media_category_meal"),
    GENERAL(MediaCategory.GENERAL, "media_category_general"),
}

@HiltViewModel
class MediaLibraryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaLibraryUiState())
    val uiState: StateFlow<MediaLibraryUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE")

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val all = mediaRepository.getAll()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allRecords = all,
                )
                applyFilters()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error",
                )
            }
        }
    }

    fun setCategory(category: MediaCategoryFilter) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters()
    }

    fun setFavoritesOnly(favoritesOnly: Boolean) {
        _uiState.value = _uiState.value.copy(favoritesOnly = favoritesOnly)
        applyFilters()
    }

    fun setSortNewestFirst(newestFirst: Boolean) {
        _uiState.value = _uiState.value.copy(sortNewestFirst = newestFirst)
        applyFilters()
    }

    fun toggleFavorite(record: MediaRecord) {
        viewModelScope.launch {
            try {
                mediaRepository.update(record.copy(isFavorite = !record.isFavorite))
                loadAll()
            } catch (_: Exception) { }
        }
    }

    fun deleteRecord(record: MediaRecord) {
        viewModelScope.launch {
            try {
                mediaRepository.delete(record.id)
                loadAll()
            } catch (_: Exception) { }
        }
    }

    // ── Multi-select ────────────────────────────────────────────────────────────

    /** Enters multi-select mode and selects the given record. */
    fun enterSelectionMode(recordId: Long) {
        _uiState.value = _uiState.value.copy(
            isSelectMode = true,
            selectedIds = setOf(recordId),
        )
    }

    /** Toggles selection of a record in multi-select mode. */
    fun toggleSelection(recordId: Long) {
        val current = _uiState.value.selectedIds
        val updated = if (recordId in current) {
            current - recordId
        } else {
            current + recordId
        }
        _uiState.value = _uiState.value.copy(
            selectedIds = updated,
            isSelectMode = updated.isNotEmpty(),
        )
    }

    /** Exits multi-select mode without any action. */
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            isSelectMode = false,
            selectedIds = emptySet(),
        )
    }

    /** Gets the currently selected records from the filtered list. */
    private fun getSelectedRecords(): List<MediaRecord> {
        val allVisible = _uiState.value.groups.flatMap { it.items }
        return allVisible.filter { it.id in _uiState.value.selectedIds }
    }

    // ── Batch delete ────────────────────────────────────────────────────────────

    fun showBatchDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showBatchDeleteConfirm = true)
    }

    fun dismissBatchDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showBatchDeleteConfirm = false)
    }

    fun confirmBatchDelete() {
        val selected = _uiState.value.selectedIds.toList()
        _uiState.value = _uiState.value.copy(
            showBatchDeleteConfirm = false,
            batchDeleteInProgress = true,
        )
        viewModelScope.launch {
            var hasError = false
            for (id in selected) {
                try {
                    mediaRepository.delete(id)
                } catch (_: Exception) {
                    hasError = true
                }
            }
            _uiState.value = _uiState.value.copy(
                batchDeleteInProgress = false,
                isSelectMode = false,
                selectedIds = emptySet(),
            )
            if (hasError) {
                _uiState.value = _uiState.value.copy(
                    error = "部分文件删除失败",
                )
            }
            loadAll()
        }
    }

    // ── Batch share ─────────────────────────────────────────────────────────────

    /** Creates a share intent for all selected files. */
    fun createBatchShareIntent(context: Context): Intent? {
        val records = getSelectedRecords()
        if (records.isEmpty()) return null

        val uris = records.mapNotNull { record ->
            try {
                val file = mediaRepository.resolveFile(record.relativePath)
                if (file.exists()) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                } else null
            } catch (_: Exception) { null }
        }
        if (uris.isEmpty()) return null

        return if (uris.size == 1) {
            val record = records.first()
            Intent(Intent.ACTION_SEND).apply {
                type = record.mimeType
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    // ── Batch export to system gallery ──────────────────────────────────────────

    /** Exports selected media files to the system gallery via MediaStore. */
    fun batchExportToGallery(context: Context) {
        val records = getSelectedRecords()
        if (records.isEmpty()) return

        viewModelScope.launch {
            var successCount = 0
            for (record in records) {
                try {
                    val file = mediaRepository.resolveFile(record.relativePath)
                    if (file.exists()) {
                        if (exportToMediaStore(context, file, record.mimeType)) {
                            successCount++
                        }
                    }
                } catch (_: Exception) { }
            }
            _uiState.value = _uiState.value.copy(
                showBatchExportSuccess = successCount > 0,
                isSelectMode = false,
                selectedIds = emptySet(),
            )
        }
    }

    /** Copies a file into the system MediaStore content provider. */
    private fun exportToMediaStore(context: Context, file: File, mimeType: String): Boolean {
        return try {
            val relativePath = if (mimeType.startsWith("video")) {
                Environment.DIRECTORY_MOVIES + File.separator + "FitLog"
            } else {
                Environment.DIRECTORY_PICTURES + File.separator + "FitLog"
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collectionUri = if (mimeType.startsWith("video")) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val uri = context.contentResolver.insert(collectionUri, contentValues) ?: return false

            context.contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(file).use { input ->
                    input.copyTo(output)
                }
            } ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }

            true
        } catch (_: Exception) {
            false
        }
    }

    fun dismissBatchExportSuccess() {
        _uiState.value = _uiState.value.copy(showBatchExportSuccess = false)
    }

    // ── Filter / group logic ────────────────────────────────────────────────────

    /** Resolves the relative path to an absolute File for image loading. */
    fun resolveFile(relativePath: String): java.io.File {
        return mediaRepository.resolveFile(relativePath)
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.allRecords

        // Category filter
        if (state.selectedCategory != MediaCategoryFilter.ALL) {
            filtered = filtered.filter { it.category == state.selectedCategory.queryValue }
        }

        // Favorites only
        if (state.favoritesOnly) {
            filtered = filtered.filter { it.isFavorite }
        }

        // Sort
        filtered = if (state.sortNewestFirst) {
            filtered.sortedByDescending { it.capturedAt }
        } else {
            filtered.sortedBy { it.capturedAt }
        }

        // Group by date
        val groups = filtered
            .groupBy { record ->
                LocalDate.ofEpochDay(record.date)
            }
            .map { (date, items) ->
                MediaDateGroup(
                    date = date,
                    label = date.format(dateFormatter),
                    items = items,
                )
            }
            .sortedByDescending { it.date }

        _uiState.value = state.copy(groups = groups)
    }
}
