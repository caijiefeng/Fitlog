package com.example.fitlog.feature.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.CheckInRepository
import com.example.fitlog.domain.checkin.CheckIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckInUiState(
    val existingCheckIn: CheckIn? = null,
    val mood: Int? = null,
    val energyLevel: Int? = null,
    val notes: String = "",
    val isSaved: Boolean = false,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val checkInRepository: CheckInRepository,
    private val dateProvider: CurrentDateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    init {
        observeTodayCheckIn()
    }

    private fun observeTodayCheckIn() {
        viewModelScope.launch {
            val today = dateProvider.today()
            checkInRepository.observeByDate(today).collect { checkIn ->
                if (checkIn != null) {
                    _uiState.value = CheckInUiState(
                        existingCheckIn = checkIn,
                        mood = checkIn.mood,
                        energyLevel = checkIn.energyLevel,
                        notes = checkIn.notes ?: "",
                        isSaved = true,
                    )
                } else {
                    _uiState.value = CheckInUiState()
                }
            }
        }
    }

    fun onMoodChange(mood: Int) {
        _uiState.value = _uiState.value.copy(
            mood = mood,
            isSaved = false,
            error = null,
        )
    }

    fun onEnergyLevelChange(energyLevel: Int) {
        _uiState.value = _uiState.value.copy(
            energyLevel = energyLevel,
            isSaved = false,
            error = null,
        )
    }

    fun onNotesChange(notes: String) {
        _uiState.value = _uiState.value.copy(
            notes = notes,
            isSaved = false,
            error = null,
        )
    }

    fun startEditing() {
        val existing = _uiState.value.existingCheckIn ?: return
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            isSaved = false,
            mood = existing.mood,
            energyLevel = existing.energyLevel,
            notes = existing.notes ?: "",
        )
    }

    fun cancelEditing() {
        val existing = _uiState.value.existingCheckIn
        if (existing != null) {
            _uiState.value = _uiState.value.copy(
                isEditing = false,
                isSaved = true,
                mood = existing.mood,
                energyLevel = existing.energyLevel,
                notes = existing.notes ?: "",
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isEditing = false,
                mood = null,
                energyLevel = null,
                notes = "",
            )
        }
    }

    fun saveCheckIn() {
        val state = _uiState.value
        val today = dateProvider.today()

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            try {
                checkInRepository.saveCheckIn(
                    date = today,
                    mood = state.mood,
                    energyLevel = state.energyLevel,
                    notes = state.notes.ifBlank { null },
                )
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true, isEditing = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败",
                )
            }
        }
    }
}
