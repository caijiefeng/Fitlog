package com.example.fitlog.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.model.SetType
import com.example.fitlog.core.model.WorkoutSessionDetail
import com.example.fitlog.core.model.WorkoutStatus
import com.example.fitlog.data.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutExecutionUiState(
    val sessionDetail: WorkoutSessionDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val restTimerState: RestTimerState = RestTimerState(),
    val showCompleteDialog: Boolean = false,
    val showCancelDialog: Boolean = false,
    val isSaving: Boolean = false,
    val elapsedSeconds: Long = 0L,
)

sealed interface WorkoutExecutionEvent {
    data class NavigateToSummary(val sessionId: Long) : WorkoutExecutionEvent
    data class ShowError(val message: String) : WorkoutExecutionEvent
    data object NavigateBack : WorkoutExecutionEvent
}

@HiltViewModel
class WorkoutExecutionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: WorkoutSessionRepository,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L
    val timer = RestTimerController()

    private val _uiState = MutableStateFlow(WorkoutExecutionUiState())
    val uiState: StateFlow<WorkoutExecutionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WorkoutExecutionEvent>()
    val events: SharedFlow<WorkoutExecutionEvent> = _events.asSharedFlow()

    init {
        if (sessionId > 0) loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            try {
                val detail = sessionRepository.getDetail(sessionId)
                if (detail != null) {
                    _uiState.update { it.copy(sessionDetail = detail, isLoading = false) }
                    // Restore rest timer if active
                    val s = detail.session
                    if (s.status == WorkoutStatus.IN_PROGRESS) {
                        val sessionEntity = sessionRepository.getById(sessionId)
                        // Restore timer from DB state
                    }
                } else {
                    _uiState.update { it.copy(error = "训练不存在", isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun completeSet(
        exerciseSessionId: Long,
        setRecordId: Long,
        reps: Int?,
        weightKg: Double?,
        rpe: Double?,
        rir: Int?,
    ) {
        viewModelScope.launch {
            try {
                val detail = _uiState.value.sessionDetail ?: return@launch
                // Find the set record and update it
                val exercise = detail.exercises.find { it.first.id == exerciseSessionId } ?: return@launch
                val setRecord = exercise.second.find { it.id == setRecordId } ?: return@launch

                // Update set via repository
                // sessionRepository.updateSetRecord(setRecordId, reps, weightKg, rpe, rir)

                // Reload session
                loadSession()
            } catch (e: Exception) {
                _events.emit(WorkoutExecutionEvent.ShowError("保存失败: ${e.message}"))
            }
        }
    }

    fun startRest(plannedSeconds: Int) {
        timer.start(plannedSeconds)
        _uiState.update { it.copy(restTimerState = timer.state.value) }
        viewModelScope.launch {
            timer.state.collect { s -> _uiState.update { it.copy(restTimerState = s) } }
        }
    }

    fun tickRest() { timer.tick() }

    fun skipRest() {
        timer.skip()
        _uiState.update { it.copy(restTimerState = timer.state.value) }
    }

    fun add15Seconds() {
        timer.add15Seconds()
        _uiState.update { it.copy(restTimerState = timer.state.value) }
    }

    fun subtract15Seconds() {
        timer.subtract15Seconds()
        _uiState.update { it.copy(restTimerState = timer.state.value) }
    }

    fun showCompleteDialog() {
        _uiState.update { it.copy(showCompleteDialog = true) }
    }

    fun dismissCompleteDialog() {
        _uiState.update { it.copy(showCompleteDialog = false) }
    }

    fun showCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = true) }
    }

    fun dismissCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = false) }
    }

    fun completeWorkout() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }
                val detail = _uiState.value.sessionDetail ?: return@launch
                val completedCount = sessionRepository.completedSetCount(sessionId)
                val status = if (completedCount > 0) WorkoutStatus.COMPLETED else WorkoutStatus.CANCELLED
                sessionRepository.updateStatus(sessionId, status)
                timer.reset()
                _events.emit(WorkoutExecutionEvent.NavigateToSummary(sessionId))
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(WorkoutExecutionEvent.ShowError("完成失败: ${e.message}"))
            }
        }
    }

    fun cancelWorkout() {
        viewModelScope.launch {
            try {
                sessionRepository.updateStatus(sessionId, WorkoutStatus.CANCELLED)
                timer.reset()
                _events.emit(WorkoutExecutionEvent.NavigateBack)
            } catch (e: Exception) {
                _events.emit(WorkoutExecutionEvent.ShowError("取消失败: ${e.message}"))
            }
        }
    }
}
