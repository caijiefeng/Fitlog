package com.example.fitlog.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.model.WorkoutSession
import com.example.fitlog.core.model.WorkoutStatus
import com.example.fitlog.data.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseCompletionSummary(
    val name: String,
    val targetSets: Int,
    val completedSets: Int,
    val isSkipped: Boolean,
)

data class WorkoutSummaryUiState(
    val session: WorkoutSession? = null,
    val exerciseCount: Int = 0,
    val completedExerciseCount: Int = 0,
    val completedSetCount: Int = 0,
    val totalVolume: Double = 0.0,
    val exerciseSummaries: List<ExerciseCompletionSummary> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: WorkoutSessionRepository,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    private val _uiState = MutableStateFlow(WorkoutSummaryUiState())
    val uiState: StateFlow<WorkoutSummaryUiState> = _uiState.asStateFlow()

    init {
        if (sessionId > 0) loadSummary()
    }

    private fun loadSummary() {
        viewModelScope.launch {
            try {
                val detail = sessionRepository.getDetail(sessionId)
                if (detail != null) {
                    val totalVolume = sessionRepository.totalVolume(sessionId)
                    val completedSets = sessionRepository.completedSetCount(sessionId)
                    val completedExercises = sessionRepository.completedExerciseCount(sessionId)

                    val exerciseSummaries = detail.exercises.map { (es, sets) ->
                        val completedInExercise = sets.count { it.completed }
                        ExerciseCompletionSummary(
                            name = es.exerciseNameSnapshot,
                            targetSets = es.targetSets,
                            completedSets = completedInExercise,
                            isSkipped = es.isSkipped,
                        )
                    }

                    _uiState.update {
                        it.copy(
                            session = detail.session,
                            exerciseCount = detail.exercises.size,
                            completedExerciseCount = completedExercises,
                            completedSetCount = completedSets,
                            totalVolume = totalVolume,
                            exerciseSummaries = exerciseSummaries,
                            isLoading = false,
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "记录未找到", isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
