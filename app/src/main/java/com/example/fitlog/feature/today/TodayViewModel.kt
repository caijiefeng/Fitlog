package com.example.fitlog.feature.today

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class TodayUiState(
    val hasWorkoutToday: Boolean = false,
    val todayWorkoutName: String? = null,
    val todayWorkoutProgress: String? = null,
)

sealed interface TodayEvent {
    data object QuickStartNotAvailable : TodayEvent
}

@HiltViewModel
class TodayViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TodayEvent>()
    val events: SharedFlow<TodayEvent> = _events.asSharedFlow()

    fun onQuickStart() {
        // Deferred to V2 — emit event so the screen can show feedback
        _events.tryEmit(TodayEvent.QuickStartNotAvailable)
    }
}
