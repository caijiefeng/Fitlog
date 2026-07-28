package com.example.fitlog.feature.today

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class TodayUiState(
    val greeting: String = "下午好",
    val hasWorkoutToday: Boolean = false,
    val todayWorkoutName: String? = null,
    val todayWorkoutProgress: String? = null,
)

@HiltViewModel
class TodayViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    fun onQuickStart() {
        _uiState.update { it.copy() }
    }
}
