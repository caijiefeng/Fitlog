package com.example.fitlog.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.CalendarRepository
import com.example.fitlog.data.repository.PlannedWorkoutRepository
import com.example.fitlog.data.repository.WorkoutScheduleRepository
import com.example.fitlog.data.repository.WorkoutTemplateRepository
import com.example.fitlog.domain.calendar.CalendarDay
import com.example.fitlog.domain.calendar.CalendarWorkoutOccurrence
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val days: List<CalendarDay> = emptyList(),
    val selectedDay: Long? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val templates: List<com.example.fitlog.core.model.WorkoutTemplate> = emptyList(),
    val showScheduleDialog: Boolean = false,
    val scheduleDialogDate: LocalDate? = null,
    val selectedTemplateId: Long? = null,
    val isOneTime: Boolean = true,
    val repeatIntervalWeeks: Int = 1,
    val isScheduling: Boolean = false,
)

sealed interface CalendarEvent {
    data class ShowSnackbar(val message: String) : CalendarEvent
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val plannedWorkoutRepository: PlannedWorkoutRepository,
    private val scheduleRepository: WorkoutScheduleRepository,
    private val templateRepository: WorkoutTemplateRepository,
    private val dateProvider: CurrentDateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CalendarEvent>()
    val events: SharedFlow<CalendarEvent> = _events.asSharedFlow()

    init {
        val today = dateProvider.today()
        loadMonth(YearMonth.of(today.year, today.month))
    }

    fun loadMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                yearMonth = yearMonth,
                isLoading = true,
                error = null,
            )
            try {
                val days = calendarRepository.getMonth(yearMonth)
                _uiState.value = _uiState.value.copy(
                    days = days,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }

    fun selectDay(epochDay: Long) {
        _uiState.value = _uiState.value.copy(selectedDay = epochDay)
    }

    fun showScheduleDialog(epochDay: Long) {
        viewModelScope.launch {
            try {
                val templates = templateRepository.getAllActive().first()
                _uiState.value = _uiState.value.copy(
                    showScheduleDialog = true,
                    scheduleDialogDate = LocalDate.ofEpochDay(epochDay),
                    templates = templates,
                    selectedTemplateId = null,
                    isOneTime = true,
                    repeatIntervalWeeks = 1,
                )
            } catch (e: Exception) {
                _events.emit(CalendarEvent.ShowSnackbar("加载模板失败"))
            }
        }
    }

    fun dismissScheduleDialog() {
        _uiState.value = _uiState.value.copy(
            showScheduleDialog = false,
            scheduleDialogDate = null,
            selectedTemplateId = null,
        )
    }

    fun selectTemplateForSchedule(templateId: Long) {
        _uiState.value = _uiState.value.copy(selectedTemplateId = templateId)
    }

    fun setOneTime(oneTime: Boolean) {
        _uiState.value = _uiState.value.copy(isOneTime = oneTime)
    }

    fun setRepeatIntervalWeeks(weeks: Int) {
        _uiState.value = _uiState.value.copy(repeatIntervalWeeks = weeks)
    }

    fun confirmSchedule() {
        val state = _uiState.value
        val templateId = state.selectedTemplateId ?: return
        val date = state.scheduleDialogDate ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScheduling = true)
            try {
                if (state.isOneTime) {
                    // Create one-time planned workout
                    plannedWorkoutRepository.create(
                        templateId = templateId,
                        plannedDate = date,
                    )
                } else {
                    // Create recurring schedule
                    scheduleRepository.setTemplate(
                        dayOfWeek = date.dayOfWeek.value,
                        templateId = templateId,
                        startDate = date,
                        repeatIntervalWeeks = state.repeatIntervalWeeks,
                    )
                }
                // Refresh the calendar
                loadMonth(state.yearMonth)
                _uiState.value = _uiState.value.copy(
                    showScheduleDialog = false,
                    isScheduling = false,
                )
                _events.emit(CalendarEvent.ShowSnackbar("已安排训练"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isScheduling = false)
                _events.emit(CalendarEvent.ShowSnackbar("安排失败: ${e.message}"))
            }
        }
    }

    fun nextMonth() {
        loadMonth(_uiState.value.yearMonth.plusMonths(1))
    }

    fun prevMonth() {
        loadMonth(_uiState.value.yearMonth.minusMonths(1))
    }

    fun goToToday() {
        val today = dateProvider.today()
        val yearMonth = YearMonth.of(today.year, today.month)
        loadMonth(yearMonth)
        _uiState.value = _uiState.value.copy(selectedDay = today.toEpochDay())
    }

    fun refresh() {
        loadMonth(_uiState.value.yearMonth)
    }
}
