package com.example.fitlog.feature.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.model.EquipmentType
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.ExerciseCategory
import com.example.fitlog.core.model.MuscleGroup
import com.example.fitlog.core.model.TrackingType
import com.example.fitlog.data.repository.ExerciseRepository
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

data class ExerciseFormState(
    val name: String = "",
    val primaryMuscleGroup: MuscleGroup? = null,
    val secondaryMuscleGroup: MuscleGroup? = null,
    val notes: String = "",
    val isSaving: Boolean = false,
    val isLoaded: Boolean = false,
    val error: String? = null,
    val nameError: String? = null,
    val isBuiltIn: Boolean = false,
    val equipmentType: EquipmentType? = null,
    val trackingType: TrackingType? = null,
)

sealed interface ExerciseFormEvent {
    data object Saved : ExerciseFormEvent
    data object Cancelled : ExerciseFormEvent
    data class ShowError(val message: String) : ExerciseFormEvent
}

@HiltViewModel
class ExerciseEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val exerciseId: Long? = savedStateHandle.get<Long>("exerciseId")?.takeIf { it > 0 }

    private val _state = MutableStateFlow(ExerciseFormState())
    val state: StateFlow<ExerciseFormState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ExerciseFormEvent>()
    val events: SharedFlow<ExerciseFormEvent> = _events.asSharedFlow()

    val isCreateMode: Boolean = exerciseId == null

    init {
        if (exerciseId != null) {
            loadExercise(exerciseId)
        } else {
            _state.update { it.copy(isLoaded = true) }
        }
    }

    private fun loadExercise(id: Long) {
        viewModelScope.launch {
            val exercise = exerciseRepository.getById(id)
            if (exercise != null) {
                _state.update {
                    it.copy(
                        name = exercise.name,
                        primaryMuscleGroup = exercise.primaryMuscleGroup,
                        secondaryMuscleGroup = exercise.secondaryMuscleGroup,
                        notes = exercise.notes ?: "",
                        isLoaded = true,
                        isBuiltIn = !exercise.isCustom,
                        equipmentType = exercise.equipmentType,
                        trackingType = exercise.trackingType,
                    )
                }
            } else {
                _state.update { it.copy(error = "动作不存在", isLoaded = true) }
            }
        }
    }

    fun onNameChanged(name: String) {
        _state.update { it.copy(name = name, nameError = null) }
    }

    fun onPrimaryMuscleGroupChanged(group: MuscleGroup?) {
        _state.update { it.copy(primaryMuscleGroup = group) }
    }

    fun onSecondaryMuscleGroupChanged(group: MuscleGroup?) {
        _state.update { it.copy(secondaryMuscleGroup = group) }
    }

    fun onNotesChanged(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    fun onSave() {
        val name = _state.value.name.trim()
        if (name.isBlank()) {
            _state.update { it.copy(nameError = "名称不能为空") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val existing = exerciseRepository.getById(exerciseId ?: 0)
                if (existing != null) {
                    exerciseRepository.update(
                        existing.copy(
                            name = name,
                            primaryMuscleGroup = _state.value.primaryMuscleGroup ?: MuscleGroup.FULL_BODY,
                            secondaryMuscleGroup = _state.value.secondaryMuscleGroup,
                            notes = _state.value.notes.ifBlank { null },
                        )
                    )
                } else {
                    val isDuplicate = exerciseRepository.isNameDuplicate(name)
                    if (isDuplicate) {
                        _state.update { it.copy(isSaving = false, nameError = "同名动作已存在") }
                        return@launch
                    }
                    exerciseRepository.create(
                        Exercise(
                            name = name,
                            primaryMuscleGroup = _state.value.primaryMuscleGroup ?: MuscleGroup.FULL_BODY,
                            secondaryMuscleGroup = _state.value.secondaryMuscleGroup,
                            notes = _state.value.notes.ifBlank { null },
                            isCustom = true,
                        )
                    )
                }
                _events.emit(ExerciseFormEvent.Saved)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "保存失败: ${e.message}") }
            }
        }
    }
}
