package com.example.fitlog.feature.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.CalendarRepository
import com.example.fitlog.data.repository.WorkoutSessionRepository
import com.example.fitlog.domain.calendar.CalendarDay
import com.example.fitlog.domain.calendar.CalendarWorkoutOccurrence
import com.example.fitlog.domain.calendar.CalendarWorkoutStatus
import com.example.fitlog.domain.workout.RescheduleWorkoutUseCase
import com.example.fitlog.domain.workout.RestoreWorkoutScheduleUseCase
import com.example.fitlog.domain.workout.SkipWorkoutUseCase
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

data class CalendarDayDetailUiState(
    val epochDay: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val occurrences: List<CalendarWorkoutOccurrence> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface CalendarDayDetailEvent {
    data class NavigateToWorkout(val sessionId: Long) : CalendarDayDetailEvent
    data class NavigateToExecution(val sessionId: Long) : CalendarDayDetailEvent
    data class ShowReschedulePicker(val scheduleId: Long, val templateId: Long, val occurrenceDate: LocalDate) :
        CalendarDayDetailEvent
    data class ShowSnackbar(val message: String) : CalendarDayDetailEvent
}

@HiltViewModel
class CalendarDayDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val calendarRepository: CalendarRepository,
    private val rescheduleWorkoutUseCase: RescheduleWorkoutUseCase,
    private val skipWorkoutUseCase: SkipWorkoutUseCase,
    private val restoreWorkoutScheduleUseCase: RestoreWorkoutScheduleUseCase,
    private val sessionRepository: WorkoutSessionRepository,
    private val dateProvider: CurrentDateProvider,
) : ViewModel() {

    private val epochDay: Long = savedStateHandle["epochDay"]!!

    private val _uiState = MutableStateFlow(CalendarDayDetailUiState())
    val uiState: StateFlow<CalendarDayDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CalendarDayDetailEvent>()
    val events: SharedFlow<CalendarDayDetailEvent> = _events.asSharedFlow()

    init {
        loadDayDetail()
    }

    private fun loadDayDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val days = calendarRepository.getDayDetail(epochDay)
                val day: CalendarDay? = days.firstOrNull()
                if (day != null) {
                    _uiState.value = CalendarDayDetailUiState(
                        epochDay = day.epochDay,
                        date = day.date,
                        occurrences = day.occurrences,
                        isLoading = false,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        date = LocalDate.ofEpochDay(epochDay),
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }

    fun onStartWorkout(occurrence: CalendarWorkoutOccurrence) {
        viewModelScope.launch {
            try {
                if (occurrence.sessionId != null) {
                    _events.emit(CalendarDayDetailEvent.NavigateToWorkout(occurrence.sessionId))
                    return@launch
                }
                val sid = sessionRepository.createFromTemplate(
                    templateId = occurrence.templateId ?: return@launch,
                    scheduleId = occurrence.scheduleId,
                    occurrenceDate = occurrence.occurrenceDate?.toEpochDay(),
                )
                _events.emit(CalendarDayDetailEvent.NavigateToExecution(sid))
            } catch (e: Exception) {
                _events.emit(CalendarDayDetailEvent.ShowSnackbar(e.message ?: "启动训练失败"))
            }
        }
    }

    fun onSkip(occurrence: CalendarWorkoutOccurrence) {
        viewModelScope.launch {
            try {
                skipWorkoutUseCase(
                    scheduleId = occurrence.scheduleId ?: return@launch,
                    templateId = occurrence.templateId ?: return@launch,
                    occurrenceDate = occurrence.occurrenceDate ?: return@launch,
                )
                loadDayDetail()
                _events.emit(CalendarDayDetailEvent.ShowSnackbar("已跳过该训练"))
            } catch (e: Exception) {
                _events.emit(CalendarDayDetailEvent.ShowSnackbar(e.message ?: "操作失败"))
            }
        }
    }

    fun onRestore(occurrence: CalendarWorkoutOccurrence) {
        viewModelScope.launch {
            try {
                restoreWorkoutScheduleUseCase(
                    scheduleId = occurrence.scheduleId ?: return@launch,
                    occurrenceDate = occurrence.occurrenceDate ?: return@launch,
                )
                loadDayDetail()
                _events.emit(CalendarDayDetailEvent.ShowSnackbar("已恢复原计划"))
            } catch (e: Exception) {
                _events.emit(CalendarDayDetailEvent.ShowSnackbar(e.message ?: "操作失败"))
            }
        }
    }

    fun onContinueWorkout(sessionId: Long) {
        viewModelScope.launch {
            _events.emit(CalendarDayDetailEvent.NavigateToExecution(sessionId))
        }
    }

    fun onViewDetail(sessionId: Long) {
        viewModelScope.launch {
            _events.emit(CalendarDayDetailEvent.NavigateToWorkout(sessionId))
        }
    }
}
