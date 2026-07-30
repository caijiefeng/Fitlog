package com.example.fitlog.feature.media

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
