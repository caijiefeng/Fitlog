package com.example.fitlog.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.CalendarRepository
import com.example.fitlog.data.repository.FoodRecordRepository
import com.example.fitlog.data.repository.ProgressRepository
import com.example.fitlog.data.repository.WorkoutSessionRepository

import com.example.fitlog.data.repository.DailyNutritionSummary
import com.example.fitlog.domain.calendar.CalendarWorkoutOccurrence
import com.example.fitlog.domain.calendar.CalendarWorkoutStatus
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
    val occurrences: List<CalendarWorkoutOccurrence> = emptyList(),
    val hasInProgressWorkout: Boolean = false,
    val inProgressSessionId: Long? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showStartWorkoutDialog: Boolean = false,
    // ── 仪表盘补充数据 ──────────────────────────────────────────────
    /** 本周训练天数（近 7 天有训练量的日期数） */
    val weekWorkoutCount: Int = 0,
    /** 本周总容量 kg */
    val weekVolumeKg: Double = 0.0,
    /** 最近体重 */
    val currentWeightKg: Double? = null,
    /** 今日营养摘要 */
    val nutritionSummary: DailyNutritionSummary? = null,
    /** 今日训练总组数（进行中会话） */
    val inProgressTotalSets: Int = 0,
    val inProgressCompletedSets: Int = 0,
)

sealed interface TodayEvent {
    data class StartWorkout(
        val sessionId: Long,
        val scheduleId: Long? = null,
        val occurrenceDate: Long? = null,
    ) : TodayEvent

    data class ResumeWorkout(val sessionId: Long) : TodayEvent

    data class NavigateToWorkoutDetail(val sessionId: Long) : TodayEvent

    data object NavigateToTemplatePicker : TodayEvent

    data object NavigateToQuickSetup : TodayEvent
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val progressRepository: ProgressRepository,
    private val foodRecordRepository: FoodRecordRepository,
    private val dateProvider: CurrentDateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TodayEvent>()
    val events: SharedFlow<TodayEvent> = _events.asSharedFlow()

    init {
        loadTodayOccurrences()
        loadDashboardStats()
        viewModelScope.launch {
            sessionRepository.observeInProgress().collect { s ->
                _uiState.value = _uiState.value.copy(
                    hasInProgressWorkout = s != null,
                    inProgressSessionId = s?.id,
                )
            }
        }
    }

    private fun loadTodayOccurrences() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(occurrences = emptyList())
            try {
                val today = dateProvider.today()
                val days = calendarRepository.getDayDetail(today.toEpochDay())
                val occurrences = days.firstOrNull()?.occurrences ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    occurrences = occurrences,
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

    private fun loadDashboardStats() {
        viewModelScope.launch {
            try {
                val today = dateProvider.today()
                val weekPoints = progressRepository.getTrendPoints(
                    com.example.fitlog.data.repository.TrendRange.WEEK_7,
                )
                val weekVolume = weekPoints.sumOf { it.volume ?: 0.0 }
                val weekDays = weekPoints.count { it.volume != null && it.volume!! > 0 }
                val weight = weekPoints.lastOrNull { it.weight != null }?.weight
                _uiState.value = _uiState.value.copy(
                    weekWorkoutCount = weekDays,
                    weekVolumeKg = weekVolume,
                    currentWeightKg = weight,
                )
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                foodRecordRepository.observeDailySummary(dateProvider.today()).collect { summary ->
                    _uiState.value = _uiState.value.copy(nutritionSummary = summary)
                }
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            // 进行中训练的组数进度
            val session = sessionRepository.getInProgress()
            if (session != null) {
                val detail = sessionRepository.getDetail(session.id)
                if (detail != null) {
                    val total = detail.exercises.sumOf { it.first.targetSets }
                    val completed = detail.exercises.sumOf { (ex, sets) ->
                        sets.count { it.completed && it.setNumber <= ex.targetSets }
                    }
                    _uiState.value = _uiState.value.copy(
                        inProgressTotalSets = total,
                        inProgressCompletedSets = completed,
                    )
                }
            }
        }
    }

    fun onStartWorkout(occurrence: CalendarWorkoutOccurrence) {
        viewModelScope.launch {
            val state = _uiState.value
            val targetOccurrence = occurrence

            // Validate: SKIPPED, RESCHEDULED-original, CANCELLED cannot start
            when (targetOccurrence.status) {
                CalendarWorkoutStatus.SKIPPED,
                CalendarWorkoutStatus.CANCELLED -> {
                    return@launch
                }
                CalendarWorkoutStatus.RESCHEDULED -> {
                    // RESCHEDULED on original date markers cannot start
                    if (targetOccurrence.isOriginalDateMarker) return@launch
                }
                CalendarWorkoutStatus.IN_PROGRESS -> {
                    // Resume the existing session
                    if (targetOccurrence.sessionId != null) {
                        _events.emit(TodayEvent.ResumeWorkout(targetOccurrence.sessionId))
                    } else {
                        // Fallback to detectable in-progress session
                        state.inProgressSessionId?.let { _events.emit(TodayEvent.ResumeWorkout(it)) }
                    }
                    return@launch
                }
                CalendarWorkoutStatus.COMPLETED,
                CalendarWorkoutStatus.PARTIALLY_COMPLETED -> {
                    // Navigate to workout detail instead of starting
                    if (targetOccurrence.sessionId != null) {
                        _events.emit(TodayEvent.NavigateToWorkoutDetail(targetOccurrence.sessionId))
                    }
                    return@launch
                }
                CalendarWorkoutStatus.SCHEDULED -> {
                    // Proceed to start
                }
            }

            try {
                val tid = targetOccurrence.templateId
                val sid = if (tid != null) {
                    sessionRepository.createFromTemplate(
                        templateId = tid,
                        scheduleId = targetOccurrence.scheduleId,
                        occurrenceDate = targetOccurrence.occurrenceDate?.toEpochDay(),
                    )
                } else {
                    sessionRepository.createQuick()
                }
                _events.emit(
                    TodayEvent.StartWorkout(
                        sessionId = sid,
                        scheduleId = targetOccurrence.scheduleId,
                        occurrenceDate = targetOccurrence.occurrenceDate?.toEpochDay(),
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun onResumeInProgressWorkout() {
        viewModelScope.launch {
            val sessionId = _uiState.value.inProgressSessionId
                ?: sessionRepository.getInProgress()?.id
            if (sessionId != null) {
                _events.emit(TodayEvent.ResumeWorkout(sessionId))
            }
        }
    }

    fun onQuickStart() {
        _uiState.value = _uiState.value.copy(showStartWorkoutDialog = true)
    }

    fun onDismissStartDialog() {
        _uiState.value = _uiState.value.copy(showStartWorkoutDialog = false)
    }

    fun onStartFromTemplate() {
        _uiState.value = _uiState.value.copy(showStartWorkoutDialog = false)
        viewModelScope.launch {
            _events.emit(TodayEvent.NavigateToTemplatePicker)
        }
    }

    fun onStartFreeWorkout() {
        _uiState.value = _uiState.value.copy(showStartWorkoutDialog = false)
        viewModelScope.launch {
            _events.emit(TodayEvent.NavigateToQuickSetup)
        }
    }

    fun refresh() {
        loadTodayOccurrences()
    }
}
