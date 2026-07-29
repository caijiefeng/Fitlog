package com.example.fitlog.feature.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.model.WorkoutSession
import com.example.fitlog.data.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordUiState(
    val sessions: List<WorkoutSession> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
)

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val sessionRepository: WorkoutSessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.getHistory().collect { sessions ->
                _uiState.value = RecordUiState(
                    sessions = sessions,
                    isLoading = false,
                    isEmpty = sessions.isEmpty(),
                )
            }
        }
    }
}
