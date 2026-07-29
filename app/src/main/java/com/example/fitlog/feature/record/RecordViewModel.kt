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

data class SessionHistoryItem(
    val session: WorkoutSession,
    val volume: Double,
    val completedSetCount: Int,
    val exerciseCount: Int,
)

data class RecordUiState(
    val sessions: List<SessionHistoryItem> = emptyList(),
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
                val items = sessions.map { session ->
                    SessionHistoryItem(
                        session = session,
                        volume = sessionRepository.totalVolume(session.id),
                        completedSetCount = sessionRepository.completedSetCount(session.id),
                        exerciseCount = sessionRepository.completedExerciseCount(session.id),
                    )
                }
                _uiState.value = RecordUiState(
                    sessions = items,
                    isLoading = false,
                    isEmpty = items.isEmpty(),
                )
            }
        }
    }
}
