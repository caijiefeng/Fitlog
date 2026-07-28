package com.example.fitlog.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.data.repository.DaySchedule
import com.example.fitlog.data.repository.WorkoutScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanUiState(
    val weeklyPlanName: String? = null,
    val weekSchedule: List<DaySchedule> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val scheduleRepository: WorkoutScheduleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scheduleRepository.getFullWeek().collect { schedule ->
                _uiState.value = PlanUiState(
                    weekSchedule = schedule,
                    isLoading = false,
                )
            }
        }
    }
}
