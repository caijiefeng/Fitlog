package com.example.fitlog.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.data.repository.WorkoutPlanOverrideRepository
import com.example.fitlog.data.repository.WorkoutSessionRepository
import com.example.fitlog.domain.calendar.OverrideAction
import com.example.fitlog.domain.calendar.WorkoutPlanOverride
import com.example.fitlog.domain.stats.WorkoutStreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ProgressUiState(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val adherenceRate: Double = 0.0,
    val isLoaded: Boolean = false,
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val calculator: WorkoutStreakCalculator,
    private val sessionRepository: WorkoutSessionRepository,
    private val overrideRepository: WorkoutPlanOverrideRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadStats()
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

            _uiState.value = ProgressUiState(
                currentStreak = currentStreak,
                bestStreak = bestStreak,
                adherenceRate = adherenceRate,
                isLoaded = true,
            )
        }
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
