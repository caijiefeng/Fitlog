package com.example.fitlog.feature.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.MuscleGroup
import com.example.fitlog.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseListUiState(
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedMuscleGroup: MuscleGroup? = null,
    val muscleGroups: List<MuscleGroup> = MuscleGroup.entries,
)

sealed interface ExerciseListEvent {
    data class ShowError(val message: String) : ExerciseListEvent
    data class NavigateToEdit(val exerciseId: Long) : ExerciseListEvent
    data object NavigateToCreate : ExerciseListEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseListViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseListUiState())
    val uiState: StateFlow<ExerciseListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExerciseListEvent>()
    val events: SharedFlow<ExerciseListEvent> = _events.asSharedFlow()

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
                    error = null,
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

    fun onExerciseClicked(exercise: Exercise) {
        viewModelScope.launch {
            _events.emit(ExerciseListEvent.NavigateToEdit(exercise.id))
        }
    }

    fun onCreateNew() {
        viewModelScope.launch {
            _events.emit(ExerciseListEvent.NavigateToCreate)
        }
    }

    fun onDeleteExercise(id: Long) {
        viewModelScope.launch {
            try {
                exerciseRepository.softDelete(id)
            } catch (e: Exception) {
                _events.emit(ExerciseListEvent.ShowError("删除失败: ${e.message}"))
            }
        }
    }
}
