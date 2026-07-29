package com.example.fitlog.feature.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.data.repository.MediaRecord
import com.example.fitlog.data.repository.MediaRepository
import com.example.fitlog.domain.body.BodyMeasurement
import com.example.fitlog.domain.media.MediaCategory
import com.example.fitlog.domain.media.ProgressPose
import com.example.fitlog.data.repository.BodyMeasurementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ProgressPhotoUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val groups: List<ProgressPhotoGroup> = emptyList(),
    val selectedPose: ProgressPose? = null,
    val selectedPhotoIds: Set<Long> = emptySet(),
    val isCompareMode: Boolean = false,
    val comparison: PhotoComparisonData? = null,
)

data class ProgressPhotoGroup(
    val date: LocalDate,
    val label: String,
    val items: List<MediaRecord>,
)

data class PhotoComparisonData(
    val photo1: MediaRecord,
    val photo2: MediaRecord,
    val measurement1: BodyMeasurement?,
    val measurement2: BodyMeasurement?,
    val daysBetween: Long,
    val weightChange: Double?,
    val bodyFatChange: Double?,
    val waistChange: Double?,
)

@HiltViewModel
class ProgressPhotoViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val bodyMeasurementRepository: BodyMeasurementRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressPhotoUiState())
    val uiState: StateFlow<ProgressPhotoUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE")

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val all = mediaRepository.getByCategory(MediaCategory.BODY_PROGRESS)
                _uiState.value = _uiState.value.copy(isLoading = false)
                applyFilters(all)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error",
                )
            }
        }
    }

    fun setPoseFilter(pose: ProgressPose?) {
        _uiState.value = _uiState.value.copy(selectedPose = pose)
        refreshGrouping()
    }

    fun togglePhotoSelection(photoId: Long) {
        val current = _uiState.value.selectedPhotoIds.toMutableSet()
        if (current.contains(photoId)) {
            current.remove(photoId)
            _uiState.value = _uiState.value.copy(
                selectedPhotoIds = current,
                isCompareMode = false,
                comparison = null,
            )
        } else if (current.size < 2) {
            current.add(photoId)
            if (current.size == 2) {
                buildComparison(current)
            } else {
                _uiState.value = _uiState.value.copy(selectedPhotoIds = current)
            }
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedPhotoIds = emptySet(),
            isCompareMode = false,
            comparison = null,
        )
    }

    private fun refreshGrouping() {
        viewModelScope.launch {
            try {
                val all = mediaRepository.getByCategory(MediaCategory.BODY_PROGRESS)
                applyFilters(all)
            } catch (_: Exception) { }
        }
    }

    private fun applyFilters(all: List<MediaRecord>) {
        val state = _uiState.value
        var filtered = all

        // Pose filter
        if (state.selectedPose != null) {
            filtered = filtered.filter { it.poseTag == state.selectedPose }
        }

        // Sort by date desc
        filtered = filtered.sortedByDescending { it.capturedAt }

        // Group by date
        val groups = filtered
            .groupBy { record -> LocalDate.ofEpochDay(record.date) }
            .map { (date, items) ->
                ProgressPhotoGroup(
                    date = date,
                    label = date.format(dateFormatter),
                    items = items,
                )
            }
            .sortedByDescending { it.date }

        _uiState.value = state.copy(groups = groups)
    }

    private fun buildComparison(selectedIds: Set<Long>) {
        viewModelScope.launch {
            try {
                val ids = selectedIds.toList()
                val all = mediaRepository.getByCategory(MediaCategory.BODY_PROGRESS)
                val photo1 = all.find { it.id == ids[0] }
                val photo2 = all.find { it.id == ids[1] }
                if (photo1 == null || photo2 == null) return@launch

                // Order by date (older first for meaningful delta)
                val (older, newer) = if (photo1.date <= photo2.date) photo1 to photo2
                else photo2 to photo1

                val daysBetween = ChronoUnit.DAYS.between(
                    LocalDate.ofEpochDay(older.date),
                    LocalDate.ofEpochDay(newer.date),
                )

                // Fetch body measurements for both dates
                val measurements = bodyMeasurementRepository.getByDateRange(
                    start = LocalDate.ofEpochDay(older.date),
                    end = LocalDate.ofEpochDay(newer.date),
                )

                val measOlder = measurements.find { it.date.toEpochDay() == older.date }
                val measNewer = measurements.find { it.date.toEpochDay() == newer.date }

                val comparison = PhotoComparisonData(
                    photo1 = older,
                    photo2 = newer,
                    measurement1 = measOlder,
                    measurement2 = measNewer,
                    daysBetween = daysBetween,
                    weightChange = calculateDelta(measOlder?.weightKg, measNewer?.weightKg),
                    bodyFatChange = calculateDelta(measOlder?.bodyFatPercent, measNewer?.bodyFatPercent),
                    waistChange = calculateDelta(measOlder?.waistCm, measNewer?.waistCm),
                )

                _uiState.value = _uiState.value.copy(
                    isCompareMode = true,
                    comparison = comparison,
                )
            } catch (_: Exception) { }
        }
    }

    private fun calculateDelta(old: Double?, new: Double?): Double? {
        if (old == null || new == null) return null
        return new - old
    }
}
