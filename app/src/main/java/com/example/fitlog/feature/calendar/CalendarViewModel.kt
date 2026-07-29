package com.example.fitlog.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.CalendarRepository
import com.example.fitlog.domain.calendar.CalendarDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val days: List<CalendarDay> = emptyList(),
    val selectedDay: Long? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val dateProvider: CurrentDateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

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
