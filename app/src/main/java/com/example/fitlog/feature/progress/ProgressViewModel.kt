package com.example.fitlog.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.data.repository.ProgressRepository
import com.example.fitlog.data.repository.TrendPoint
import com.example.fitlog.data.repository.TrendRange
import com.example.fitlog.data.repository.WorkoutPlanOverrideRepository
import com.example.fitlog.data.repository.WorkoutSessionRepository
import com.example.fitlog.domain.calendar.OverrideAction
import com.example.fitlog.domain.calendar.WorkoutPlanOverride
import com.example.fitlog.domain.stats.WorkoutStreakCalculator
import com.example.fitlog.core.model.WorkoutStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ProgressUiState(
    /** 当前体重 kg */
    val currentWeightKg: Double? = null,
    /** 本月训练次数 */
    val monthWorkoutCount: Int = 0,
    /** 本月总容量 kg */
    val monthVolumeKg: Double = 0.0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val adherenceRate: Double = 0.0,
    val isLoaded: Boolean = false,
    // Trend chart state
    val trendPoints: List<TrendPoint> = emptyList(),
    val selectedRange: TrendRange = TrendRange.MONTH_30,
    val isTrendLoading: Boolean = false,
    val trendError: String? = null,
    /** UI-only, derived from existing completed sessions for the heatmap. */
    val trainingDates: Set<LocalDate> = emptySet(),
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val calculator: WorkoutStreakCalculator,
    private val sessionRepository: WorkoutSessionRepository,
    private val overrideRepository: WorkoutPlanOverrideRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadStats()
        loadTrends()
        loadMonthMetrics()
    }

    private fun loadMonthMetrics() {
        viewModelScope.launch {
            try {
                val today = java.time.LocalDate.now()
                val monthStart = today.withDayOfMonth(1)
                val sessions = sessionRepository.getSessionsInRange(
                    startEpochDay = monthStart.toEpochDay(),
                    endEpochDay = today.toEpochDay(),
                )
                val monthPoints = progressRepository.getTrendPoints(
                    com.example.fitlog.data.repository.TrendRange.MONTH_30,
                )
                _uiState.value = _uiState.value.copy(
                    monthWorkoutCount = sessions.size,
                    monthVolumeKg = monthPoints.sumOf { it.volume ?: 0.0 },
                    currentWeightKg = monthPoints.lastOrNull { it.weight != null }?.weight,
                )
            } catch (_: Exception) { }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            // Load a generous window of data (last 365 days) to cover long streaks
            val today = LocalDate.now()
            val start = today.minusDays(365)
            val sessions = sessionRepository.getSessionsInRange(
                startEpochDay = start.toEpochDay(),
                endEpochDay = today.toEpochDay(),
            )
            val overrides = overrideRepository.observeAll().first().map { it.toDomain() }

            val currentStreak = calculator.currentStreak(sessions, overrides)
            val bestStreak = calculator.bestStreak(sessions, overrides)
            val adherenceRate = calculator.adherenceRate(sessions, overrides, days = 90)

            _uiState.value = _uiState.value.copy(
                currentStreak = currentStreak,
                bestStreak = bestStreak,
                adherenceRate = adherenceRate,
                trainingDates = sessions
                    .filter { it.status == WorkoutStatus.COMPLETED || it.status == WorkoutStatus.PARTIALLY_COMPLETED }
                    .map { it.date }
                    .toSet(),
                isLoaded = true,
            )
        }
    }

    fun loadTrends() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTrendLoading = true, trendError = null)
            try {
                val points = progressRepository.getTrendPoints(_uiState.value.selectedRange)
                _uiState.value = _uiState.value.copy(
                    trendPoints = points,
                    isTrendLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTrendLoading = false,
                    trendError = e.message,
                )
            }
        }
    }

    fun setTrendRange(range: TrendRange) {
        _uiState.value = _uiState.value.copy(selectedRange = range)
        loadTrends()
    }

    private fun com.example.fitlog.core.database.entity.WorkoutPlanOverrideEntity.toDomain() =
        WorkoutPlanOverride(
            scheduleId = scheduleId,
            templateId = templateId,
            occurrenceDate = LocalDate.ofEpochDay(occurrenceDate),
            plannedDate = plannedDate?.let { LocalDate.ofEpochDay(it) },
            action = try {
                OverrideAction.valueOf(action)
            } catch (_: IllegalArgumentException) {
                OverrideAction.SKIPPED
            },
        )
}
