package com.example.fitlog.feature.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.database.dao.WorkoutSessionDao
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.CalendarRepository
import com.example.fitlog.data.repository.PlannedWorkoutRepository
import com.example.fitlog.data.repository.WorkoutPlanOverrideRepository
import com.example.fitlog.data.repository.WorkoutScheduleRepository
import com.example.fitlog.data.repository.WorkoutSessionRepository
import com.example.fitlog.domain.calendar.CalendarDay
import com.example.fitlog.domain.calendar.CalendarWorkoutOccurrence
import com.example.fitlog.domain.calendar.OverrideAction
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
    val isRescheduling: Boolean = false,
    val error: String? = null,
    // Delete dialog state
    val showDeleteDialog: Boolean = false,
    val deleteTarget: CalendarWorkoutOccurrence? = null,
    val isDeleting: Boolean = false,
)

sealed interface CalendarDayDetailEvent {
    data class NavigateToWorkout(val sessionId: Long) : CalendarDayDetailEvent
    data class NavigateToExecution(val sessionId: Long) : CalendarDayDetailEvent
    data class ShowSnackbar(val message: String) : CalendarDayDetailEvent
    data object NavigateBack : CalendarDayDetailEvent
}

@HiltViewModel
class CalendarDayDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val calendarRepository: CalendarRepository,
    private val rescheduleWorkoutUseCase: RescheduleWorkoutUseCase,
    private val skipWorkoutUseCase: SkipWorkoutUseCase,
    private val restoreWorkoutScheduleUseCase: RestoreWorkoutScheduleUseCase,
    private val sessionRepository: WorkoutSessionRepository,
    private val sessionDao: WorkoutSessionDao,
    private val dateProvider: CurrentDateProvider,
    private val plannedWorkoutRepository: PlannedWorkoutRepository,
    private val scheduleRepository: WorkoutScheduleRepository,
    private val overrideRepository: WorkoutPlanOverrideRepository,
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
                if (occurrence.scheduleId == null) {
                    // Planned workout - delete it instead
                    deletePlannedWorkout(occurrence)
                    return@launch
                }
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

    // ── Delete / Cancel logic ─────────────────────────────────────────────

    /**
     * Shows the delete dialog for the given occurrence.
     * For planned workouts (no scheduleId), shows simple confirm.
     * For recurring schedule occurrences, shows options dialog.
     */
    fun onRequestDelete(occurrence: CalendarWorkoutOccurrence) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            deleteTarget = occurrence,
        )
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            deleteTarget = null,
        )
    }

    /**
     * Delete a one-time planned workout.
     */
    fun onDeleteOneTime() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            try {
                // Planned workout: key format "planned:{id}"
                if (target.key.startsWith("planned:")) {
                    val id = target.key.substringAfter("planned:").toLongOrNull()
                    if (id != null) {
                        plannedWorkoutRepository.deleteById(id)
                    }
                } else if (target.scheduleId != null) {
                    // Recurring schedule occurrence: skip just this one
                    skipWorkoutUseCase(
                        scheduleId = target.scheduleId,
                        templateId = target.templateId ?: return@launch,
                        occurrenceDate = target.occurrenceDate ?: return@launch,
                    )
                }
                dismissDeleteDialog()
                loadDayDetail()
                _events.emit(CalendarDayDetailEvent.ShowSnackbar("已取消安排"))
            } catch (e: Exception) {
                _events.emit(CalendarDayDetailEvent.ShowSnackbar(e.message ?: "操作失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isDeleting = false)
            }
        }
    }

    /**
     * Stop future recurrences of a recurring schedule.
     * Sets endDate on the schedule to the day before the current occurrence.
     */
    fun onStopFutureRecurrences() {
        val target = _uiState.value.deleteTarget ?: return
        val scheduleId = target.scheduleId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            try {
                val occurrenceDate = target.occurrenceDate ?: return@launch
                scheduleRepository.stopFutureRecurrences(scheduleId, occurrenceDate.toEpochDay())
                dismissDeleteDialog()
                loadDayDetail()
                _events.emit(CalendarDayDetailEvent.ShowSnackbar("已停止今后重复"))
            } catch (e: Exception) {
                _events.emit(CalendarDayDetailEvent.ShowSnackbar(e.message ?: "操作失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isDeleting = false)
            }
        }
    }

    // ── Reschedule ────────────────────────────────────────────────────────

    fun onReschedule(occurrence: CalendarWorkoutOccurrence, targetDate: LocalDate) {
        viewModelScope.launch {
            if (_uiState.value.isRescheduling) return@launch

            val scheduleId = occurrence.scheduleId ?: run {
                // Planned workout - just reschedule the planned_workouts record
                if (occurrence.key.startsWith("planned:")) {
                    val id = occurrence.key.substringAfter("planned:").toLongOrNull()
                    if (id != null) {
                        try {
                            plannedWorkoutRepository.reschedule(id, targetDate)
                            loadDayDetail()
                            _events.emit(CalendarDayDetailEvent.ShowSnackbar("已改期"))
                        } catch (e: Exception) {
                            _events.emit(CalendarDayDetailEvent.ShowSnackbar(e.message ?: "改期失败"))
                        }
                    }
                } else {
                    _events.emit(CalendarDayDetailEvent.ShowSnackbar("无法改期：缺少日程信息"))
                }
                return@launch
            }
            val templateId = occurrence.templateId ?: run {
                _events.emit(CalendarDayDetailEvent.ShowSnackbar("无法改期：缺少模板信息"))
                return@launch
            }
            val occurrenceDate = occurrence.occurrenceDate ?: run {
                _events.emit(CalendarDayDetailEvent.ShowSnackbar("无法改期：缺少日期信息"))
                return@launch
            }
            val today = dateProvider.today()

            if (targetDate < today) {
                _events.emit(CalendarDayDetailEvent.ShowSnackbar("改期目标日期不能早于今天"))
                return@launch
            }

            if (targetDate == occurrenceDate) {
                _events.emit(CalendarDayDetailEvent.ShowSnackbar("目标日期与原日期相同，无需改期"))
                return@launch
            }

            val existingSession = sessionDao.getByScheduleAndOccurrence(scheduleId, targetDate.toEpochDay())
            if (existingSession != null) {
                _events.emit(CalendarDayDetailEvent.ShowSnackbar("目标日期已有训练记录，无法改期"))
                return@launch
            }

            _uiState.value = _uiState.value.copy(isRescheduling = true)
            try {
                rescheduleWorkoutUseCase(
                    scheduleId = scheduleId,
                    templateId = templateId,
                    occurrenceDate = occurrenceDate,
                    targetDate = targetDate,
                )
                loadDayDetail()
                _events.emit(CalendarDayDetailEvent.ShowSnackbar("已改期至 ${targetDate.monthValue}/${targetDate.dayOfMonth}"))
                _events.emit(CalendarDayDetailEvent.NavigateBack)
            } catch (e: Exception) {
                _events.emit(CalendarDayDetailEvent.ShowSnackbar(e.message ?: "改期失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isRescheduling = false)
            }
        }
    }

    fun onPostpone(occurrence: CalendarWorkoutOccurrence) {
        val occurrenceDate = occurrence.occurrenceDate ?: return
        val targetDate = occurrenceDate.plusDays(1)
        onReschedule(occurrence, targetDate)
    }

    fun refresh() {
        loadDayDetail()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private suspend fun deletePlannedWorkout(occurrence: CalendarWorkoutOccurrence) {
        if (occurrence.key.startsWith("planned:")) {
            val id = occurrence.key.substringAfter("planned:").toLongOrNull()
            if (id != null) {
                plannedWorkoutRepository.deleteById(id)
                loadDayDetail()
                _events.emit(CalendarDayDetailEvent.ShowSnackbar("已取消安排"))
            }
        }
    }
}
