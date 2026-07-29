package com.example.fitlog.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.data.repository.WorkoutScheduleRepository
import com.example.fitlog.data.repository.WorkoutSessionRepository
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
    val hasWorkoutToday: Boolean = false,
    val todayTemplateName: String? = null,
    val todayTemplateId: Long? = null,
    val todayExerciseCount: Int = 0,
    val hasInProgressWorkout: Boolean = false,
    val inProgressSessionId: Long? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface TodayEvent {
    data class StartWorkout(val sessionId: Long) : TodayEvent
    data class ResumeWorkout(val sessionId: Long) : TodayEvent
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val scheduleRepository: WorkoutScheduleRepository,
    private val sessionRepository: WorkoutSessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TodayEvent>()
    val events: SharedFlow<TodayEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            scheduleRepository.getTodaySchedule().collect { s ->
                _uiState.value = _uiState.value.copy(
                    hasWorkoutToday = s != null,
                    todayTemplateName = s?.templateName,
                    todayTemplateId = s?.templateId,
                    todayExerciseCount = s?.exerciseCount ?: 0,
                    isLoading = false,
                )
            }
        }
        viewModelScope.launch {
            sessionRepository.observeInProgress().collect { s ->
                _uiState.value = _uiState.value.copy(
                    hasInProgressWorkout = s != null,
                    inProgressSessionId = s?.id,
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
                val tid = state.todayTemplateId
                val sid = if (tid != null) sessionRepository.createFromTemplate(tid)
                          else sessionRepository.createQuick()
                _events.emit(TodayEvent.StartWorkout(sid))
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
                _events.emit(TodayEvent.StartWorkout(sid))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
