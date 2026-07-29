package com.example.fitlog.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.database.dao.ExerciseSessionDao
import com.example.fitlog.core.model.SetType
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
    val sessionDetail: com.example.fitlog.core.model.WorkoutSessionDetail? = null,
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
    private val exerciseSessionDao: ExerciseSessionDao,
) : ViewModel() {

    val timer = RestTimerController()

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    private val _uiState = MutableStateFlow(WorkoutExecutionUiState())
    val uiState: StateFlow<WorkoutExecutionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WorkoutExecutionEvent>()
    val events: SharedFlow<WorkoutExecutionEvent> = _events.asSharedFlow()

    init {
        if (sessionId > 0) loadSession()
        viewModelScope.launch {
            timer.state.collect { s ->
                _uiState.update { it.copy(restTimerState = s) }
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
            } catch (e: Exception) {
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
                _events.emit(WorkoutExecutionEvent.ShowError("Failed to update set type: ${e.message}"))
            }
        }
    }

    fun skipExercise(exerciseSessionId: Long) {
        viewModelScope.launch {
            try {
                val detail = _uiState.value.sessionDetail ?: return@launch
                val exercise = detail.exercises.find { it.first.id == exerciseSessionId } ?: return@launch
                exerciseSessionDao.setSkipped(exerciseSessionId, !exercise.first.isSkipped)
                loadSession()
            } catch (e: Exception) {
                _events.emit(WorkoutExecutionEvent.ShowError("Failed to update exercise: ${e.message}"))
            }
        }
    }

    fun updateNotes(exerciseSessionId: Long, notes: String) {
        viewModelScope.launch {
            try {
                sessionRepository.updateExerciseNotes(exerciseSessionId, notes)
            } catch (_: Exception) {
                // Non-critical — silently ignore note save errors
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
            } catch (_: Exception) {
                // Non-critical — best-effort cleanup of persisted rest state
            }
        }
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
                val completedCount = sessionRepository.completedSetCount(sessionId)
                if (completedCount == 0) {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.emit(WorkoutExecutionEvent.ShowError("请至少完成一组"))
                    return@launch
                }

                // Calculate total target sets from non-skipped exercises
                val detail = _uiState.value.sessionDetail
                val totalTargetSets = detail?.exercises?.sumOf { (exercise, _) ->
                    if (exercise.isSkipped) 0 else exercise.targetSets
                } ?: 0

                val status = if (completedCount >= totalTargetSets) {
                    WorkoutStatus.COMPLETED
                } else {
                    WorkoutStatus.PARTIALLY_COMPLETED
                }

                sessionRepository.updateStatus(sessionId, status)
                timer.reset()
                _events.emit(WorkoutExecutionEvent.NavigateToSummary(sessionId))
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(WorkoutExecutionEvent.ShowError("Complete failed: ${e.message}"))
            }
        }
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
                _events.emit(WorkoutExecutionEvent.ShowError("Cancel failed: ${e.message}"))
            }
        }
    }
}
