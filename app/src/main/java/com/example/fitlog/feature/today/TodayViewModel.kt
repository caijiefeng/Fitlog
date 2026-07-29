package com.example.fitlog.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.CalendarRepository
import com.example.fitlog.data.repository.WorkoutSessionRepository
import com.example.fitlog.domain.calendar.CalendarWorkoutOccurrence
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodayUiState(
    val occurrences: List<CalendarWorkoutOccurrence> = emptyList(),
    val hasInProgressWorkout: Boolean = false,
    val inProgressSessionId: Long? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface TodayEvent {
    data class StartWorkout(
        val sessionId: Long,
        val scheduleId: Long? = null,
        val occurrenceDate: Long? = null,
    ) : TodayEvent

    data class ResumeWorkout(val sessionId: Long) : TodayEvent
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val dateProvider: CurrentDateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TodayEvent>()
    val events: SharedFlow<TodayEvent> = _events.asSharedFlow()

    init {
        loadTodayOccurrences()
        viewModelScope.launch {
            sessionRepository.observeInProgress().collect { s ->
                _uiState.value = _uiState.value.copy(
                    hasInProgressWorkout = s != null,
                    inProgressSessionId = s?.id,
                )
            }
        }
    }

    private fun loadTodayOccurrences() {
        viewModelScope.launch {
            try {
                val today = dateProvider.today()
                val days = calendarRepository.getDayDetail(today.toEpochDay())
                val occurrences = days.firstOrNull()?.occurrences ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    occurrences = occurrences,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }

    fun onStartWorkout() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.hasInProgressWorkout) {
                state.inProgressSessionId?.let { _events.emit(TodayEvent.ResumeWorkout(it)) }
                return@launch
            }
            try {
                val occurrence = state.occurrences.firstOrNull { it.canStart }
                val tid = occurrence?.templateId
                val sid = if (tid != null) {
                    sessionRepository.createFromTemplate(
                        templateId = tid,
                        scheduleId = occurrence.scheduleId,
                        occurrenceDate = occurrence.occurrenceDate?.toEpochDay(),
                    )
                } else {
                    sessionRepository.createQuick()
                }
                _events.emit(
                    TodayEvent.StartWorkout(
                        sessionId = sid,
                        scheduleId = occurrence?.scheduleId,
                        occurrenceDate = occurrence?.occurrenceDate?.toEpochDay(),
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun onQuickStart() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                if (state.hasInProgressWorkout) {
                    state.inProgressSessionId?.let { _events.emit(TodayEvent.ResumeWorkout(it)) }
                    return@launch
                }
                val sid = sessionRepository.createQuick()
                _events.emit(TodayEvent.StartWorkout(sessionId = sid))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
