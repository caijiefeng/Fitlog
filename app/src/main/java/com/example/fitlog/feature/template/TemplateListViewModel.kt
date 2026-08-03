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
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class TemplateListUiState(
    val templates: List<WorkoutTemplate> = emptyList(),
    /** templateId -> 第一个动作的 builtInKey（用于列表缩略图） */
    val firstBuiltInKeys: Map<Long, String> = emptyMap(),
    val isLoading: Boolean = true,
    val showScheduleDialog: Boolean = false,
    val scheduleTemplateId: Long? = null,
    val scheduleTemplateName: String = "",
    // One-time mode: multiple selectable dates (past dates disabled in the UI)
    val selectedDates: Set<LocalDate> = emptySet(),
    // Recurring mode: multiple weekdays (1=Mon … 7=Sun)
    val selectedWeekdays: Set<Int> = emptySet(),
    val isOneTime: Boolean = true,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val repeatIntervalWeeks: Int = 1,
    val isScheduling: Boolean = false,
) {
    val canConfirmSchedule: Boolean get() =
        if (isOneTime) selectedDates.isNotEmpty() else selectedWeekdays.isNotEmpty()
}

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
                val firstKeys = HashMap<Long, String>()
                templates.forEach { template ->
                    try {
                        val detail = templateRepository.getDetail(template.id)
                        firstKeys[template.id] = detail?.exercises
                            ?.firstNotNullOfOrNull { it.exercise?.builtInKey }
                            .orEmpty()
                    } catch (_: Exception) { }
                }
                _uiState.value = _uiState.value.copy(
                    templates = templates,
                    firstBuiltInKeys = firstKeys,
                    isLoading = false,
                )
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
            selectedDates = emptySet(),
            selectedWeekdays = emptySet(),
            isOneTime = true,
            startDate = dateProvider.today(),
            endDate = null,
            repeatIntervalWeeks = 1,
        )
    }

    fun dismissScheduleDialog() {
        _uiState.value = _uiState.value.copy(showScheduleDialog = false)
    }

    // ── One-time multi-date selection ───────────────────────────────────────

    /** Toggles a date in the multi-select calendar. Past dates are ignored. */
    fun toggleScheduleDate(date: LocalDate) {
        if (date.isBefore(dateProvider.today())) return
        val selected = _uiState.value.selectedDates
        _uiState.value = _uiState.value.copy(
            selectedDates = if (date in selected) selected - date else selected + date,
        )
    }

    fun clearSelectedDates() {
        _uiState.value = _uiState.value.copy(selectedDates = emptySet())
    }

    /** Selects the Mon-Sun days of the current week (past days excluded). */
    fun selectThisWeek() {
        val today = dateProvider.today()
        val monday = today.with(DayOfWeek.MONDAY)
        val dates = (0L..6L).map { monday.plusDays(it) }.filter { !it.isBefore(today) }
        _uiState.value = _uiState.value.copy(selectedDates = dates.toSet())
    }

    /** Selects the Mon-Sun days of next week. */
    fun selectNextWeek() {
        val today = dateProvider.today()
        val nextMonday = today.with(DayOfWeek.MONDAY).plusWeeks(1)
        val dates = (0L..6L).map { nextMonday.plusDays(it) }
        _uiState.value = _uiState.value.copy(selectedDates = dates.toSet())
    }

    // ── Recurring weekday selection ─────────────────────────────────────────

    /** Toggles a weekday (1=Mon … 7=Sun) for the recurring schedule. */
    fun toggleWeekday(dayOfWeek: Int) {
        val selected = _uiState.value.selectedWeekdays
        _uiState.value = _uiState.value.copy(
            selectedWeekdays = if (dayOfWeek in selected) selected - dayOfWeek else selected + dayOfWeek,
        )
    }

    fun setOneTime(oneTime: Boolean) {
        _uiState.value = _uiState.value.copy(isOneTime = oneTime)
    }

    fun setStartDate(date: LocalDate) {
        if (date.isBefore(dateProvider.today())) return
        val current = _uiState.value
        _uiState.value = _uiState.value.copy(
            startDate = date,
            endDate = current.endDate?.takeIf { !it.isBefore(date) },
        )
    }

    fun setEndDate(date: LocalDate) {
        val current = _uiState.value
        val start = current.startDate ?: dateProvider.today()
        if (date.isBefore(start)) return
        _uiState.value = _uiState.value.copy(endDate = date)
    }

    fun clearEndDate() {
        _uiState.value = _uiState.value.copy(endDate = null)
    }

    fun setRepeatIntervalWeeks(weeks: Int) {
        _uiState.value = _uiState.value.copy(repeatIntervalWeeks = weeks)
    }

    // ── Confirm ─────────────────────────────────────────────────────────────

    fun confirmSchedule() {
        val state = _uiState.value
        val templateId = state.scheduleTemplateId ?: return
        if (!state.canConfirmSchedule) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScheduling = true)
            try {
                val message = if (state.isOneTime) {
                    scheduleOneTime(templateId, state)
                } else {
                    scheduleRecurring(templateId, state)
                }
                _uiState.value = _uiState.value.copy(
                    showScheduleDialog = false,
                    isScheduling = false,
                )
                _events.emit(TemplateListEvent.ShowSnackbar(message))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isScheduling = false)
                _events.emit(TemplateListEvent.ShowSnackbar("安排失败: ${e.message}"))
            }
        }
    }

    private suspend fun scheduleOneTime(templateId: Long, state: TemplateListUiState): String {
        val result = plannedWorkoutRepository.createMany(
            templateId = templateId,
            plannedDates = state.selectedDates.sorted(),
        )
        return if (result.skipped.isEmpty()) {
            "已安排训练"
        } else {
            "已安排 ${result.created.size} 天，跳过 ${result.skipped.size} 天重复计划"
        }
    }

    private suspend fun scheduleRecurring(templateId: Long, state: TemplateListUiState): String {
        scheduleRepository.createRecurringForWeekdays(
            dayOfWeeks = state.selectedWeekdays.sorted(),
            templateId = templateId,
            startDate = state.startDate,
            endDate = state.endDate,
            repeatIntervalWeeks = state.repeatIntervalWeeks,
        )
        return "已安排训练"
    }
}
