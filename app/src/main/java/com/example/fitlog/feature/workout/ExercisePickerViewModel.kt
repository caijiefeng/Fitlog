package com.example.fitlog.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class ExercisePickerUiState(
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val searchQuery: String = "",
    val selectedMuscleGroup: MuscleGroup? = null,
    val isAdding: Boolean = false,
)

sealed interface ExercisePickerEvent {
    data object NavigateBack : ExercisePickerEvent
    data class ShowError(val message: String) : ExercisePickerEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: WorkoutSessionRepository,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    private val _uiState = MutableStateFlow(ExercisePickerUiState())
    val uiState: StateFlow<ExercisePickerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExercisePickerEvent>()
    val events: SharedFlow<ExercisePickerEvent> = _events.asSharedFlow()

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
                    exercises = exercises,
                    isLoading = false,
                    isEmpty = exercises.isEmpty(),
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

    fun addExercise(exerciseId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isAdding = true)
                sessionRepository.addExerciseToQuickWorkout(sessionId, exerciseId)
                _events.emit(ExercisePickerEvent.NavigateBack)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isAdding = false)
                _events.emit(ExercisePickerEvent.ShowError("添加失败: ${e.message}"))
            }
        }
    }
}
