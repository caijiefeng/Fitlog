package com.example.fitlog.feature.reminder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.data.repository.ReminderRepository
import com.example.fitlog.data.repository.WorkoutScheduleRepository
import com.example.fitlog.domain.reminder.Reminder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val label: String = "",
    val timeOfDayMinutes: Int = 480, // 08:00 default
    val daysOfWeekMask: Int = 0,
    val scheduleId: Long? = null,
    val isEnabled: Boolean = true,
    val fieldErrors: Map<String, String> = emptyMap(),
    val schedules: List<ScheduleOption> = emptyList(),
)

data class ScheduleOption(
    val id: Long,
    val label: String,
)

sealed interface ReminderEditEvent {
    data object Saved : ReminderEditEvent
    data class ShowError(val message: String) : ReminderEditEvent
}

@HiltViewModel
class ReminderEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val scheduleRepository: WorkoutScheduleRepository,
) : ViewModel() {

    private val reminderId: Long? = savedStateHandle.get<Long>("reminderId")

    private val _uiState = MutableStateFlow(ReminderEditUiState())
    val uiState: StateFlow<ReminderEditUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ReminderEditEvent>()
    val events: SharedFlow<ReminderEditEvent> = _events.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // Load schedule options
                scheduleRepository.getFullWeek().collect { daySchedules ->
                    val schedules = daySchedules
                        .filter { it.templateId != null }
                        .map { ScheduleOption(id = it.dayOfWeek.toLong(), label = "${it.dayName}: ${it.templateName ?: "训练"}") }

                    val current = _uiState.value
                    _uiState.value = current.copy(
                        schedules = schedules,
                        isLoading = false,
                    )
                }
            } catch (_: Exception) {
                // Schedules are optional, continue with empty list
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }

        // If editing, load the existing reminder
        if (reminderId != null && reminderId > 0) {
            viewModelScope.launch {
                val reminder = repository.getById(reminderId)
                if (reminder != null) {
                    _uiState.value = _uiState.value.copy(
                        isEditMode = true,
                        isLoading = false,
                        label = reminder.label,
                        timeOfDayMinutes = reminder.timeOfDayMinutes,
                        daysOfWeekMask = reminder.daysOfWeekMask,
                        scheduleId = reminder.scheduleId,
                        isEnabled = reminder.isEnabled,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun onLabelChanged(value: String) {
        _uiState.value = _uiState.value.copy(label = value, fieldErrors = _uiState.value.fieldErrors - "label")
    }

    fun onTimeChanged(minutes: Int) {
        _uiState.value = _uiState.value.copy(timeOfDayMinutes = minutes)
    }

    fun onDayToggled(bitIndex: Int) {
        val current = _uiState.value
        val newMask = current.daysOfWeekMask xor (1 shl bitIndex)
        _uiState.value = current.copy(daysOfWeekMask = newMask)
    }

    fun onScheduleSelected(scheduleDayOfWeekIndex: Long?) {
        _uiState.value = _uiState.value.copy(scheduleId = scheduleDayOfWeekIndex?.takeIf { it > 0 })
    }

    fun onEnabledChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isEnabled = enabled)
    }

    fun save() {
        val state = _uiState.value

        // Client-side validation
        val errors = mutableMapOf<String, String>()
        if (state.label.isBlank()) {
            errors["label"] = "标签不能为空"
        }
        if (state.daysOfWeekMask == 0) {
            errors["daysOfWeekMask"] = "请至少选择一天"
        }
        if (state.timeOfDayMinutes !in 0..1439) {
            errors["timeOfDayMinutes"] = "时间无效"
        }

        if (errors.isNotEmpty()) {
            _uiState.value = state.copy(fieldErrors = errors)
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val savedId = repository.saveReminder(
                    id = reminderId ?: 0,
                    scheduleId = state.scheduleId,
                    label = state.label.trim(),
                    timeOfDayMinutes = state.timeOfDayMinutes,
                    daysOfWeekMask = state.daysOfWeekMask,
                    zoneId = java.time.ZoneId.systemDefault().id,
                    isEnabled = state.isEnabled,
                )

                // Update scheduler
                val savedReminder = Reminder(
                    id = savedId,
                    scheduleId = state.scheduleId,
                    label = state.label.trim(),
                    timeOfDayMinutes = state.timeOfDayMinutes,
                    daysOfWeekMask = state.daysOfWeekMask,
                    zoneId = java.time.ZoneId.systemDefault().id,
                    isEnabled = state.isEnabled,
                )
                if (state.isEnabled) {
                    scheduler.scheduleReminder(savedReminder)
                } else {
                    scheduler.cancelReminder(savedId)
                }

                _events.emit(ReminderEditEvent.Saved)
            } catch (e: Exception) {
                _uiState.value = state.copy(isSaving = false)
                _events.emit(ReminderEditEvent.ShowError("保存失败: ${e.message}"))
            }
        }
    }
}
