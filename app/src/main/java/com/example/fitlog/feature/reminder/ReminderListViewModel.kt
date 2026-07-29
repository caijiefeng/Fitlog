package com.example.fitlog.feature.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.data.repository.ReminderRepository
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

data class ReminderListUiState(
    val reminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface ReminderListEvent {
    data class ShowSnackbar(val message: String) : ReminderListEvent
    data class NavigateToEdit(val reminderId: Long) : ReminderListEvent
    data object NavigateToCreate : ReminderListEvent
}

@HiltViewModel
class ReminderListViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderListUiState())
    val uiState: StateFlow<ReminderListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ReminderListEvent>()
    val events: SharedFlow<ReminderListEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { reminders ->
                _uiState.value = ReminderListUiState(
                    reminders = reminders,
                    isLoading = false,
                )
            }
        }
    }

    fun onToggleEnabled(reminder: Reminder) {
        viewModelScope.launch {
            try {
                val newEnabled = !reminder.isEnabled
                repository.setEnabled(reminder.id, newEnabled)
                if (newEnabled) {
                    scheduler.scheduleReminder(reminder.copy(isEnabled = true))
                } else {
                    scheduler.cancelReminder(reminder.id)
                }
            } catch (e: Exception) {
                _events.emit(ReminderListEvent.ShowSnackbar("操作失败: ${e.message}"))
            }
        }
    }

    fun onDelete(reminder: Reminder) {
        viewModelScope.launch {
            try {
                scheduler.cancelReminder(reminder.id)
                repository.delete(reminder.id)
                _events.emit(ReminderListEvent.ShowSnackbar("已删除"))
            } catch (e: Exception) {
                _events.emit(ReminderListEvent.ShowSnackbar("删除失败: ${e.message}"))
            }
        }
    }

    fun onEdit(reminder: Reminder) {
        viewModelScope.launch {
            _events.emit(ReminderListEvent.NavigateToEdit(reminder.id))
        }
    }

    fun onCreate() {
        viewModelScope.launch {
            _events.emit(ReminderListEvent.NavigateToCreate)
        }
    }
}
