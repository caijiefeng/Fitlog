package com.example.fitlog.feature.progress

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ProgressUiState(
    val totalWorkouts: Int = 0,
    val currentStreak: Int = 0,
)

@HiltViewModel
class ProgressViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()
}
