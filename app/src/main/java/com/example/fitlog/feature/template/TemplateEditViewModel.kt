package com.example.fitlog.feature.template

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.MuscleGroup
import com.example.fitlog.core.model.WorkoutTemplate
import com.example.fitlog.core.model.WorkoutTemplateExercise
import com.example.fitlog.data.repository.ExerciseRepository
import com.example.fitlog.data.repository.WorkoutTemplateRepository
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

data class TemplateEditUiState(
    val name: String = "",
    val notes: String = "",
    val exercises: List<TemplateExerciseItem> = emptyList(),
    val availableExercises: List<Exercise> = emptyList(),
    val isSaving: Boolean = false,
    val isLoaded: Boolean = false,
    val error: String? = null,
    val nameError: String? = null,
)

data class TemplateExerciseItem(
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: MuscleGroup,
    val targetSets: String = "3",
    val targetRepsMin: String = "",
    val targetRepsMax: String = "",
    val targetWeightKg: String = "",
    val targetRpe: String = "",
    val targetRir: String = "",
    val restSeconds: String = "90",
    val notes: String = "",
)

sealed interface TemplateEditEvent {
    data object Saved : TemplateEditEvent
    data object Cancelled : TemplateEditEvent
    data class ShowError(val message: String) : TemplateEditEvent
}

@HiltViewModel
class TemplateEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val templateRepository: WorkoutTemplateRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val templateId: Long? = savedStateHandle.get<Long>("templateId")?.takeIf { it > 0 }
    val isCreateMode: Boolean = templateId == null

    private val _state = MutableStateFlow(TemplateEditUiState())
    val state: StateFlow<TemplateEditUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TemplateEditEvent>()
    val events: SharedFlow<TemplateEditEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            // Load available exercises
            exerciseRepository.getAllActive().collect { exercises ->
                _state.update { it.copy(availableExercises = exercises) }
            }
        }
        if (templateId != null) {
            loadTemplate(templateId)
        } else {
            _state.update { it.copy(isLoaded = true) }
        }
    }

    private fun loadTemplate(id: Long) {
        viewModelScope.launch {
            val detail = templateRepository.getDetail(id)
            if (detail != null) {
                _state.update {
                    it.copy(
                        name = detail.template.name,
                        notes = detail.template.notes ?: "",
                        exercises = detail.exercises.map { ex ->
                            val te = ex.templateExercise
                            TemplateExerciseItem(
                                exerciseId = te.exerciseId,
                                exerciseName = te.exerciseName,
                                muscleGroup = te.primaryMuscleGroup,
                                targetSets = te.targetSets.toString(),
                                targetRepsMin = te.targetRepsMin?.toString() ?: "",
                                targetRepsMax = te.targetRepsMax?.toString() ?: "",
                                targetWeightKg = te.targetWeightKg?.toString() ?: "",
                                targetRpe = te.targetRpe?.toString() ?: "",
                                targetRir = te.targetRir?.toString() ?: "",
                                restSeconds = te.restSeconds.toString(),
                                notes = te.notes ?: "",
                            )
                        },
                        isLoaded = true,
                    )
                }
            } else {
                _state.update { it.copy(error = "模板不存在", isLoaded = true) }
            }
        }
    }

    fun onNameChanged(name: String) {
        _state.update { it.copy(name = name, nameError = null) }
    }

    fun onNotesChanged(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    fun onAddExercise(exerciseId: Long) {
        val exercise = _state.value.availableExercises.find { it.id == exerciseId } ?: return
        _state.update {
            it.copy(exercises = it.exercises + TemplateExerciseItem(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                muscleGroup = exercise.primaryMuscleGroup,
            ))
        }
    }

    fun onRemoveExercise(index: Int) {
        _state.update {
            it.copy(exercises = it.exercises.toMutableList().also { list -> list.removeAt(index) })
        }
    }

    fun onExerciseFieldChanged(index: Int, field: String, value: String) {
        _state.update { state ->
            val exercises = state.exercises.toMutableList()
            if (index < exercises.size) {
                val item = exercises[index]
                exercises[index] = when (field) {
                    "targetSets" -> item.copy(targetSets = value)
                    "targetRepsMin" -> item.copy(targetRepsMin = value)
                    "targetRepsMax" -> item.copy(targetRepsMax = value)
                    "targetWeightKg" -> item.copy(targetWeightKg = value)
                    "targetRpe" -> item.copy(targetRpe = value)
                    "targetRir" -> item.copy(targetRir = value)
                    "restSeconds" -> item.copy(restSeconds = value)
                    "notes" -> item.copy(notes = value)
                    else -> item
                }
            }
            state.copy(exercises = exercises)
        }
    }

    fun onSave() {
        val name = _state.value.name.trim()
        if (name.isBlank()) {
            _state.update { it.copy(nameError = "模板名称不能为空") }
            return
        }
        if (_state.value.exercises.isEmpty()) {
            _state.update { it.copy(error = "模板至少需要一个动作") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val tId = if (templateId != null) {
                    val existing = templateRepository.getById(templateId) ?: return@launch
                    templateRepository.update(
                        existing.copy(name = name, notes = _state.value.notes.ifBlank { null })
                    )
                    templateId
                } else {
                    templateRepository.create(name, _state.value.notes.ifBlank { null })
                }

                val exercises = _state.value.exercises.mapIndexed { index, item ->
                    WorkoutTemplateExercise(
                        templateId = tId,
                        exerciseId = item.exerciseId,
                        targetSets = item.targetSets.toIntOrNull() ?: 3,
                        targetRepsMin = item.targetRepsMin.toIntOrNull(),
                        targetRepsMax = item.targetRepsMax.toIntOrNull(),
                        targetWeightKg = item.targetWeightKg.toDoubleOrNull(),
                        targetRpe = item.targetRpe.toDoubleOrNull(),
                        targetRir = item.targetRir.toIntOrNull(),
                        restSeconds = item.restSeconds.toIntOrNull() ?: 90,
                        notes = item.notes.ifBlank { null },
                        sortOrder = index,
                    )
                }
                templateRepository.replaceExercises(tId, exercises)
                _events.emit(TemplateEditEvent.Saved)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "保存失败: ${e.message}") }
            }
        }
    }
}
