package com.example.fitlog.feature.workout

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExercisePickerUiState(
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val searchQuery: String = "",
    val selectedMuscleGroup: MuscleGroup? = null,
    val selectedEquipment: EquipmentType? = null,
    /** 多选集合 */
    val selectedIds: Set<Long> = emptySet(),
    val isAdding: Boolean = false,
)

sealed interface ExercisePickerEvent {
    data object NavigateBack : ExercisePickerEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: WorkoutSessionRepository,
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(ExercisePickerUiState())
    val uiState: StateFlow<ExercisePickerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExercisePickerEvent>()
    val events: SharedFlow<ExercisePickerEvent> = _events.asSharedFlow()

    private val searchQuery = MutableStateFlow("")
    private val muscleGroupFilter = MutableStateFlow<MuscleGroup?>(null)
    private val equipmentFilter = MutableStateFlow<EquipmentType?>(null)

    init {
        viewModelScope.launch {
            combine(searchQuery, muscleGroupFilter, equipmentFilter) { q, m, e ->
                Triple(q, m, e)
            }.flatMapLatest { (q, m, e) ->
                exerciseRepository.getAllActive().map { exercises ->
                    exercises.filter { ex ->
                        val matchesQuery = q.isBlank() || ex.name.contains(q, ignoreCase = true)
                        val matchesMuscle = m == null || ex.primaryMuscleGroup == m
                        val matchesEquipment = e == null || ex.equipmentType == e
                        matchesQuery && matchesMuscle && matchesEquipment
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

    fun onEquipmentSelected(equipment: EquipmentType?) {
        _uiState.value = _uiState.value.copy(selectedEquipment = equipment)
        equipmentFilter.value = equipment
    }

    /** 勾选/取消勾选（不立即退出）。 */
    fun toggleSelection(exerciseId: Long) {
        val current = _uiState.value.selectedIds
        _uiState.value = _uiState.value.copy(
            selectedIds = if (exerciseId in current) {
                current - exerciseId
            } else {
                current + exerciseId
            },
        )
    }

    /** 一次性把全部所选动作加入训练，然后返回。 */
    fun confirmSelection() {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty() || _uiState.value.isAdding) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAdding = true)
            try {
                ids.forEach { id ->
                    sessionRepository.addExerciseToQuickWorkout(sessionId, id)
                }
                _events.emit(ExercisePickerEvent.NavigateBack)
            } finally {
                _uiState.value = _uiState.value.copy(isAdding = false)
            }
        }
    }
}
