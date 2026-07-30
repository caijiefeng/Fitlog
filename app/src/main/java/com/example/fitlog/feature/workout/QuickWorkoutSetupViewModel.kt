package com.example.fitlog.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.model.EquipmentType
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.MuscleGroup
import com.example.fitlog.data.repository.ExerciseRepository
import com.example.fitlog.data.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SelectedExercise(
    val exercise: Exercise,
    val targetSets: String = "3",
    val targetReps: String = "10",
    val targetWeightKg: String = "",
)

data class QuickWorkoutSetupUiState(
    val allExercises: List<Exercise> = emptyList(),
    val searchQuery: String = "",
    val selectedMuscleGroup: MuscleGroup? = null,
    val selectedExercises: List<SelectedExercise> = emptyList(),
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val error: String? = null,
)

sealed interface QuickWorkoutSetupEvent {
    data class NavigateToExecution(val sessionId: Long) : QuickWorkoutSetupEvent
    data class ShowError(val message: String) : QuickWorkoutSetupEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuickWorkoutSetupViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: WorkoutSessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickWorkoutSetupUiState())
    val uiState: StateFlow<QuickWorkoutSetupUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<QuickWorkoutSetupEvent>()
    val events: SharedFlow<QuickWorkoutSetupEvent> = _events.asSharedFlow()

    private val searchQuery = MutableStateFlow("")
    private val muscleGroupFilter = MutableStateFlow<MuscleGroup?>(null)

    init {
        viewModelScope.launch {
            searchQuery.flatMapLatest { query ->
                muscleGroupFilter.flatMapLatest { group ->
                    when {
                        !query.isNullOrBlank() -> exerciseRepository.searchByName(query)
                        group != null -> exerciseRepository.getByMuscleGroup(group)
                        else -> exerciseRepository.getAllActive()
                    }
                }
            }.collect { exercises ->
                _uiState.value = _uiState.value.copy(
                    allExercises = exercises,
                    isLoading = false,
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchQuery.value = query
    }

    fun onMuscleGroupSelected(group: MuscleGroup?) {
        _uiState.value = _uiState.value.copy(selectedMuscleGroup = group)
        muscleGroupFilter.value = group
    }

    fun toggleExercise(exercise: Exercise) {
        val current = _uiState.value.selectedExercises
        val exists = current.any { it.exercise.id == exercise.id }
        _uiState.value = _uiState.value.copy(
            selectedExercises = if (exists) {
                current.filter { it.exercise.id != exercise.id }
            } else {
                current + SelectedExercise(exercise = exercise)
            }
        )
    }

    fun updateExerciseField(index: Int, field: String, value: String) {
        val list = _uiState.value.selectedExercises.toMutableList()
        if (index in list.indices) {
            list[index] = when (field) {
                "sets" -> list[index].copy(targetSets = value)
                "reps" -> list[index].copy(targetReps = value)
                "weight" -> list[index].copy(targetWeightKg = value)
                else -> list[index]
            }
            _uiState.value = _uiState.value.copy(selectedExercises = list)
        }
    }

    fun removeExercise(index: Int) {
        val list = _uiState.value.selectedExercises.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _uiState.value = _uiState.value.copy(selectedExercises = list)
        }
    }

    fun onCreateSession() {
        val selected = _uiState.value.selectedExercises
        if (selected.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "请至少选择一个动作")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, error = null)
            try {
                val sessionId = sessionRepository.createQuick()
                for ((i, item) in selected.withIndex()) {
                    sessionRepository.addExerciseToQuickWorkout(sessionId, item.exercise.id)
                    // Update sets/reps/weight for the exercise session
                }
                _events.emit(QuickWorkoutSetupEvent.NavigateToExecution(sessionId))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    error = "创建失败: ${e.message}",
                )
            }
        }
    }
}
