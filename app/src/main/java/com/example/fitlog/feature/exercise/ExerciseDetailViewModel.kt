package com.example.fitlog.feature.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.data.repository.ExerciseAssetRepository
import com.example.fitlog.data.repository.ExerciseRepository
import com.example.fitlog.data.repository.WorkoutSessionRepository
import com.example.fitlog.data.repository.WorkoutTemplateRepository
import com.example.fitlog.domain.exercise.ExerciseAsset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val asset: ExerciseAsset? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isAddingToSession: Boolean = false,
    val addedMessage: String? = null,
)

sealed interface ExerciseDetailEvent {
    data class NavigateToTemplate(val templateId: Long) : ExerciseDetailEvent
    data class NavigateToExecution(val sessionId: Long) : ExerciseDetailEvent
    data class ShowError(val message: String) : ExerciseDetailEvent
}

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val assetRepository: ExerciseAssetRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val templateRepository: WorkoutTemplateRepository,
) : ViewModel() {

    private val exerciseId: Long = checkNotNull(savedStateHandle["exerciseId"])

    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExerciseDetailEvent>()
    val events: SharedFlow<ExerciseDetailEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val exercise = exerciseRepository.getById(exerciseId)
            val asset = exercise?.builtInKey?.let { assetRepository.getByBuiltInKey(it) }
            _uiState.value = _uiState.value.copy(
                exercise = exercise,
                asset = asset,
                isLoading = false,
                error = if (exercise == null) "动作不存在" else null,
            )
        }
    }

    /** 创建一个以该动作为首个动作的新模板，并导航到模板编辑。 */
    fun onAddToTemplate() {
        val exercise = _uiState.value.exercise ?: return
        viewModelScope.launch {
            try {
                val templateId = templateRepository.create(
                    name = "新模板 · ${exercise.name}",
                )
                _events.emit(ExerciseDetailEvent.NavigateToTemplate(templateId))
            } catch (e: Exception) {
                _events.emit(ExerciseDetailEvent.ShowError("创建模板失败: ${e.message}"))
            }
        }
    }

    /** 把动作加入当前进行中的训练（没有则创建自由训练），并导航到训练页。 */
    fun onAddToWorkout() {
        val exercise = _uiState.value.exercise ?: return
        if (_uiState.value.isAddingToSession) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingToSession = true)
            try {
                val sessionId = sessionRepository.getInProgress()?.id
                    ?: sessionRepository.createQuick()
                sessionRepository.addExerciseToQuickWorkout(sessionId, exercise.id)
                _uiState.value = _uiState.value.copy(
                    isAddingToSession = false,
                    addedMessage = "已加入训练",
                )
                _events.emit(ExerciseDetailEvent.NavigateToExecution(sessionId))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAddingToSession = false,
                    error = "加入训练失败: ${e.message}",
                )
            }
        }
    }
}
