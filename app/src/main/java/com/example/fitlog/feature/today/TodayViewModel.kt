package com.example.fitlog.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.data.repository.WorkoutScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodayUiState(
    val hasWorkoutToday: Boolean = false,
    val todayTemplateName: String? = null,
    val todayExerciseCount: Int = 0,
    val isLoading: Boolean = true,
)

sealed interface TodayEvent {
    data object QuickStartNotAvailable : TodayEvent
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val scheduleRepository: WorkoutScheduleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TodayEvent>()
    val events: SharedFlow<TodayEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            scheduleRepository.getTodaySchedule().collect { schedule ->
                _uiState.value = TodayUiState(
                    hasWorkoutToday = schedule != null,
                    todayTemplateName = schedule?.templateName,
                    todayExerciseCount = schedule?.exerciseCount ?: 0,
                    isLoading = false,
                )
            }
        }
    }

    fun onQuickStart() {
        _events.tryEmit(TodayEvent.QuickStartNotAvailable)
    }
}
