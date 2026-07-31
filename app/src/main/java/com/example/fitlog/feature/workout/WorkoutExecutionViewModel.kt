package com.example.fitlog.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.fitlog.core.model.SetType
import com.example.fitlog.core.model.WorkoutStatus
import com.example.fitlog.data.repository.InvalidSetDataException
import com.example.fitlog.data.repository.WorkoutSessionRepository
import com.example.fitlog.domain.workout.WorkoutCompletion
import com.example.fitlog.domain.workout.WorkoutCompletionEvaluator
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
    val sessionDetail: com.example.fitlog.core.model.WorkoutSessionDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val restTimerState: RestTimerState = RestTimerState(),
    val showCompleteDialog: Boolean = false,
    val showCancelDialog: Boolean = false,
    val showPartialCompleteDialog: Boolean = false,
    val isSaving: Boolean = false,
    val elapsedSeconds: Long = 0L,
) {
    val exerciseCount: Int get() = sessionDetail?.exercises?.size ?: 0
}

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

    val timer = RestTimerController()

    private val completionEvaluator = WorkoutCompletionEvaluator()

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    private val _uiState = MutableStateFlow(WorkoutExecutionUiState())
    val uiState: StateFlow<WorkoutExecutionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WorkoutExecutionEvent>()
    val events: SharedFlow<WorkoutExecutionEvent> = _events.asSharedFlow()

    init {
        if (sessionId > 0) loadSession()
        viewModelScope.launch {
            timer.state.collect { s ->
                val prev = _uiState.value.restTimerState
                _uiState.update { it.copy(restTimerState = s) }
                // When timer finishes naturally (was running, now finished), clear persisted rest state
                if (prev.isRunning && s.isFinished) {
                    try {
                        sessionRepository.clearRestState(sessionId)
                    } catch (_: Exception) { }
                }
            }
        }
    }

    private fun loadSession() {
        viewModelScope.launch {
            try {
                val detail = sessionRepository.getDetail(sessionId)
                if (detail != null) {
                    _uiState.update { it.copy(sessionDetail = detail, isLoading = false) }
                    if (detail.session.status == WorkoutStatus.IN_PROGRESS) {
                        val (startedAt, durationSeconds, _) = sessionRepository.getRestState(sessionId)
                        if (startedAt != null && durationSeconds != null) {
                            timer.restore(startedAt, durationSeconds)
                            _uiState.update { it.copy(restTimerState = timer.state.value) }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(error = "Workout session not found", isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun completeSet(
        sessionId: Long,
        exerciseSessionId: Long,
        setRecordId: Long,
        reps: Int?,
        weightKg: Double?,
        rpe: Double?,
        rir: Int?,
        setType: SetType,
    ) {
        viewModelScope.launch {
            try {
                sessionRepository.completeSet(
                    setRecordId = setRecordId,
                    reps = reps,
                    weightKg = weightKg,
                    rpe = rpe,
                    rir = rir,
                    setType = setType.name,
                )

                val detail = _uiState.value.sessionDetail
                val exercise = detail?.exercises?.find { it.first.id == exerciseSessionId }
                val restSeconds = exercise?.first?.plannedRestSeconds ?: 90

                startRest(restSeconds)
                sessionRepository.updateRestState(
                    sessionId = sessionId,
                    startedAt = timer.state.value.startedAt,
                    durationSeconds = restSeconds,
                    setRecordId = setRecordId,
                )
                loadSession()
                autoCompleteExerciseIfAllSetsDone(exerciseSessionId)
            } catch (e: InvalidSetDataException) {
                Log.e("WorkoutExecutionVM", "Invalid set data", e)
                _events.emit(WorkoutExecutionEvent.ShowError(e.message ?: "数据验证失败"))
            } catch (e: Exception) {
                Log.e("WorkoutExecutionVM", "Failed to complete set", e)
                _events.emit(WorkoutExecutionEvent.ShowError("Save failed: ${e.message}"))
            }
        }
    }

    fun addSet(exerciseSessionId: Long) {
        viewModelScope.launch {
            try {
                sessionRepository.addSet(exerciseSessionId)
                loadSession()
            } catch (e: Exception) {
                Log.e("WorkoutExecutionVM", "Failed to add set", e)
                _events.emit(WorkoutExecutionEvent.ShowError("Failed to add set: ${e.message}"))
            }
        }
    }

    fun deleteSet(setRecordId: Long) {
        viewModelScope.launch {
            try {
                sessionRepository.deleteIncompleteSet(setRecordId)
                loadSession()
            } catch (e: Exception) {
                Log.e("WorkoutExecutionVM", "Failed to delete set", e)
                _events.emit(WorkoutExecutionEvent.ShowError("Failed to delete set: ${e.message}"))
            }
        }
    }

    fun updateSetType(setRecordId: Long, type: SetType) {
        viewModelScope.launch {
            try {
                sessionRepository.updateSetType(setRecordId, type.name)
                loadSession()
            } catch (e: Exception) {
                Log.e("WorkoutExecutionVM", "Failed to update set type", e)
                _events.emit(WorkoutExecutionEvent.ShowError("Failed to update set type: ${e.message}"))
            }
        }
    }

    fun skipExercise(exerciseSessionId: Long) {
        viewModelScope.launch {
            try {
                sessionRepository.skipExercise(sessionId, exerciseSessionId)
                loadSession()
            } catch (e: Exception) {
                Log.e("WorkoutExecutionVM", "Failed to skip exercise", e)
                _events.emit(WorkoutExecutionEvent.ShowError("Failed to update exercise: ${e.message}"))
            }
        }
    }

    fun updateNotes(exerciseSessionId: Long, notes: String) {
        viewModelScope.launch {
            try {
                sessionRepository.updateExerciseNotes(exerciseSessionId, notes)
            } catch (e: Exception) {
                Log.e("WorkoutExecutionVM", "Failed to update notes", e)
                _events.emit(WorkoutExecutionEvent.ShowError("Failed to save notes: ${e.message}"))
            }
        }
    }

    fun markExerciseCompleted(exerciseSessionId: Long) {
        viewModelScope.launch {
            try {
                sessionRepository.markExerciseCompleted(exerciseSessionId, completed = true)
                loadSession()
            } catch (e: Exception) {
                Log.e("WorkoutExecutionVM", "Failed to mark exercise completed", e)
                _events.emit(WorkoutExecutionEvent.ShowError("操作失败: ${e.message}"))
            }
        }
    }

    private fun autoCompleteExerciseIfAllSetsDone(exerciseSessionId: Long) {
        viewModelScope.launch {
            try {
                val detail = sessionRepository.getDetail(sessionId) ?: return@launch
                val (exercise, sets) = detail.exercises.find { it.first.id == exerciseSessionId } ?: return@launch
                if (exercise.isCompleted || exercise.isSkipped) return@launch
                if (completionEvaluator.isExerciseComplete(exercise, sets)) {
                    sessionRepository.markExerciseCompleted(exerciseSessionId, completed = true)
                    loadSession()
                }
            } catch (e: Exception) {
                Log.e("WorkoutExecutionVM", "Failed to auto-complete exercise", e)
            }
        }
    }

    fun startRest(plannedSeconds: Int) {
        timer.start(plannedSeconds)
    }

    fun tickRest() { timer.tick() }

    fun skipRest() {
        timer.skip()
        _uiState.update { it.copy(restTimerState = timer.state.value) }
        viewModelScope.launch {
            try {
                sessionRepository.updateRestState(sessionId, null, null, null)
            } catch (e: Exception) {
                Log.e("WorkoutExecutionVM", "Failed to clear rest state on skip", e)
            }
        }
    }

    fun add15Seconds() {
        timer.add15Seconds()
        _uiState.update { it.copy(restTimerState = timer.state.value) }
        viewModelScope.launch {
            try {
                val (_, _, setRecordId) = sessionRepository.getRestState(sessionId)
                val s = timer.state.value
                sessionRepository.updateRestState(sessionId, s.startedAt, s.durationSeconds, setRecordId)
            } catch (_: Exception) { }
        }
    }

    fun subtract15Seconds() {
        timer.subtract15Seconds()
        _uiState.update { it.copy(restTimerState = timer.state.value) }
        viewModelScope.launch {
            try {
                val (_, _, setRecordId) = sessionRepository.getRestState(sessionId)
                val s = timer.state.value
                sessionRepository.updateRestState(sessionId, s.startedAt, s.durationSeconds, setRecordId)
            } catch (_: Exception) { }
        }
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
                _uiState.update { it.copy(isSaving = true, showCompleteDialog = false) }

                // Reload the latest session detail from the DB so the completion
                // evaluation sees every set saved so far (state may be stale).
                val detail = sessionRepository.getDetail(sessionId)
                if (detail == null) {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.emit(WorkoutExecutionEvent.ShowError("训练数据加载失败，请重试"))
                    return@launch
                }
                _uiState.update { it.copy(sessionDetail = detail) }

                when (completionEvaluator.evaluate(detail)) {
                    WorkoutCompletion.NOTHING_COMPLETED -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _events.emit(WorkoutExecutionEvent.ShowError("请至少完成一组训练后再结束"))
                        return@launch
                    }
                    WorkoutCompletion.COMPLETED -> {
                        // One flow: status + endTime + cleared rest timer are
                        // persisted together by updateStatus for terminal states.
                        sessionRepository.updateStatus(sessionId, WorkoutStatus.COMPLETED)
                        timer.reset()
                        _uiState.update { it.copy(isSaving = false, restTimerState = timer.state.value) }
                        _events.emit(WorkoutExecutionEvent.NavigateToSummary(sessionId))
                    }
                    WorkoutCompletion.PARTIALLY_COMPLETED -> {
                        _uiState.update { it.copy(isSaving = false, showPartialCompleteDialog = true) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                Log.e("WorkoutExecutionVM", "Failed to complete workout", e)
                _events.emit(WorkoutExecutionEvent.ShowError("操作失败: ${e.message}"))
            }
        }
    }

    fun confirmPartialComplete() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true, showPartialCompleteDialog = false) }
                sessionRepository.updateStatus(sessionId, WorkoutStatus.PARTIALLY_COMPLETED)
                timer.reset()
                _events.emit(WorkoutExecutionEvent.NavigateToSummary(sessionId))
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                Log.e("WorkoutExecutionVM", "Failed to confirm partial complete", e)
                _events.emit(WorkoutExecutionEvent.ShowError("Complete failed: ${e.message}"))
            }
        }
    }

    fun dismissPartialCompleteDialog() {
        _uiState.update { it.copy(showPartialCompleteDialog = false) }
    }

    fun cancelWorkout() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }
                sessionRepository.updateStatus(sessionId, WorkoutStatus.CANCELLED)
                timer.reset()
                _events.emit(WorkoutExecutionEvent.NavigateBack)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                Log.e("WorkoutExecutionVM", "Failed to cancel workout", e)
                _events.emit(WorkoutExecutionEvent.ShowError("Cancel failed: ${e.message}"))
            }
        }
    }
}
