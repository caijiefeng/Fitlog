package com.example.fitlog.feature.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.model.EquipmentType
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.MuscleGroup
import com.example.fitlog.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExerciseScopeFilter { ALL, CUSTOM }

data class ExerciseListUiState(
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedMuscleGroup: MuscleGroup? = null,
    val selectedEquipment: EquipmentType? = null,
    val scopeFilter: ExerciseScopeFilter = ExerciseScopeFilter.ALL,
    val muscleGroups: List<MuscleGroup> = MuscleGroup.entries,
)

sealed interface ExerciseListEvent {
    data class ShowError(val message: String) : ExerciseListEvent
    data class NavigateToDetail(val exerciseId: Long) : ExerciseListEvent
    data object NavigateToCreate : ExerciseListEvent
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
    private val equipmentFilter = MutableStateFlow<EquipmentType?>(null)
    private val scopeFilter = MutableStateFlow(ExerciseScopeFilter.ALL)

    init {
        viewModelScope.launch {
            combine(
                searchQuery,
                muscleGroupFilter,
                equipmentFilter,
                scopeFilter,
            ) { query, muscle, equipment, scope ->
                Filter(query, muscle, equipment, scope)
            }.flatMapLatest { filter ->
                exerciseRepository.getAllActive().map { exercises ->
                    exercises.filter { ex ->
                        val matchesQuery = filter.query.isBlank() ||
                            ex.name.contains(filter.query, ignoreCase = true)
                        val matchesMuscle = filter.muscle == null ||
                            ex.primaryMuscleGroup == filter.muscle
                        val matchesEquipment = filter.equipment == null ||
                            ex.equipmentType == filter.equipment
                        val matchesScope = when (filter.scope) {
                            ExerciseScopeFilter.ALL -> true
                            ExerciseScopeFilter.CUSTOM -> ex.isCustom
                        }
                        matchesQuery && matchesMuscle && matchesEquipment && matchesScope
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

    fun onEquipmentSelected(equipment: EquipmentType?) {
        _uiState.value = _uiState.value.copy(selectedEquipment = equipment)
        equipmentFilter.value = equipment
    }

    fun onScopeFilterSelected(scope: ExerciseScopeFilter) {
        _uiState.value = _uiState.value.copy(scopeFilter = scope)
        scopeFilter.value = scope
    }

    fun onExerciseClicked(exercise: Exercise) {
        viewModelScope.launch {
            _events.emit(ExerciseListEvent.NavigateToDetail(exercise.id))
        }
    }

    fun onCreateNew() {
        viewModelScope.launch {
            _events.emit(ExerciseListEvent.NavigateToCreate)
        }
    }

    private data class Filter(
        val query: String,
        val muscle: MuscleGroup?,
        val equipment: EquipmentType?,
        val scope: ExerciseScopeFilter,
    )
}
