package com.example.fitlog.feature.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.model.WorkoutTemplate
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.PlannedWorkoutRepository
import com.example.fitlog.data.repository.WorkoutScheduleRepository
import com.example.fitlog.data.repository.WorkoutTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TemplateListUiState(
    val templates: List<WorkoutTemplate> = emptyList(),
    val isLoading: Boolean = true,
    val showScheduleDialog: Boolean = false,
    val scheduleTemplateId: Long? = null,
    val scheduleTemplateName: String = "",
    val scheduleDate: LocalDate? = null,
    val isOneTime: Boolean = true,
    val repeatIntervalWeeks: Int = 1,
    val isScheduling: Boolean = false,
)

sealed interface TemplateListEvent {
    data class NavigateToEdit(val templateId: Long) : TemplateListEvent
    data object NavigateToCreate : TemplateListEvent
    data class ShowSnackbar(val message: String) : TemplateListEvent
}

@HiltViewModel
class TemplateListViewModel @Inject constructor(
    private val templateRepository: WorkoutTemplateRepository,
    private val plannedWorkoutRepository: PlannedWorkoutRepository,
    private val scheduleRepository: WorkoutScheduleRepository,
    private val dateProvider: CurrentDateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateListUiState())
    val uiState: StateFlow<TemplateListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TemplateListEvent>()
    val events: SharedFlow<TemplateListEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            templateRepository.getAllActive().collect { templates ->
                _uiState.value = _uiState.value.copy(templates = templates, isLoading = false)
            }
        }
    }

    fun onCreateNew() {
        viewModelScope.launch { _events.emit(TemplateListEvent.NavigateToCreate) }
    }

    fun onTemplateClicked(id: Long) {
        viewModelScope.launch { _events.emit(TemplateListEvent.NavigateToEdit(id)) }
    }

    fun onScheduleClick(templateId: Long, templateName: String) {
        _uiState.value = _uiState.value.copy(
            showScheduleDialog = true,
            scheduleTemplateId = templateId,
            scheduleTemplateName = templateName,
            scheduleDate = dateProvider.today(),
            isOneTime = true,
            repeatIntervalWeeks = 1,
        )
    }

    fun dismissScheduleDialog() {
        _uiState.value = _uiState.value.copy(showScheduleDialog = false)
    }

    fun setScheduleDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(scheduleDate = date)
    }

    fun setOneTime(oneTime: Boolean) {
        _uiState.value = _uiState.value.copy(isOneTime = oneTime)
    }

    fun setRepeatIntervalWeeks(weeks: Int) {
        _uiState.value = _uiState.value.copy(repeatIntervalWeeks = weeks)
    }

    fun confirmSchedule() {
        val state = _uiState.value
        val templateId = state.scheduleTemplateId ?: return
        val date = state.scheduleDate ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScheduling = true)
            try {
                if (state.isOneTime) {
                    plannedWorkoutRepository.create(
                        templateId = templateId,
                        plannedDate = date,
                    )
                } else {
                    scheduleRepository.setTemplate(
                        dayOfWeek = date.dayOfWeek.value,
                        templateId = templateId,
                        startDate = date,
                        repeatIntervalWeeks = state.repeatIntervalWeeks,
                    )
                }
                _uiState.value = _uiState.value.copy(
                    showScheduleDialog = false,
                    isScheduling = false,
                )
                _events.emit(TemplateListEvent.ShowSnackbar("已安排训练"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isScheduling = false)
                _events.emit(TemplateListEvent.ShowSnackbar("安排失败: ${e.message}"))
            }
        }
    }
}
