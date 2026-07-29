package com.example.fitlog.feature.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.BodyMeasurementRepository
import com.example.fitlog.domain.body.BodyMeasurement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BodyMeasurementUiState(
    val isLoading: Boolean = true,
    val measurements: List<BodyMeasurement> = emptyList(),
    val error: String? = null,
)

data class BodyMeasurementFormState(
    val isVisible: Boolean = false,
    val editId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val weightKg: String = "",
    val bodyFatPercent: String = "",
    val muscleKg: String = "",
    val waistCm: String = "",
    val note: String = "",
)

@HiltViewModel
class BodyMeasurementViewModel @Inject constructor(
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val dateProvider: CurrentDateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyMeasurementUiState())
    val uiState: StateFlow<BodyMeasurementUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(BodyMeasurementFormState())
    val formState: StateFlow<BodyMeasurementFormState> = _formState.asStateFlow()

    init {
        loadMeasurements()
    }

    private fun loadMeasurements() {
        viewModelScope.launch {
            try {
                val all = bodyMeasurementRepository.observeAll()
                all.collect { list ->
                    _uiState.value = BodyMeasurementUiState(
                        isLoading = false,
                        measurements = list.sortedByDescending { it.date },
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }

    fun showAddForm() {
        _formState.value = BodyMeasurementFormState(
            isVisible = true,
            date = dateProvider.today(),
        )
    }

    fun showEditForm(measurement: BodyMeasurement) {
        _formState.value = BodyMeasurementFormState(
            isVisible = true,
            editId = measurement.id,
            date = measurement.date,
            weightKg = measurement.weightKg?.let { formatDouble(it) } ?: "",
            bodyFatPercent = measurement.bodyFatPercent?.let { formatDouble(it) } ?: "",
            muscleKg = measurement.muscleKg?.let { formatDouble(it) } ?: "",
            waistCm = measurement.waistCm?.let { formatDouble(it) } ?: "",
            note = measurement.note ?: "",
        )
    }

    fun hideForm() {
        _formState.value = BodyMeasurementFormState()
    }

    fun updateFormDate(date: LocalDate) {
        _formState.value = _formState.value.copy(date = date)
    }

    fun updateFormWeight(value: String) {
        _formState.value = _formState.value.copy(weightKg = value)
    }

    fun updateFormBodyFat(value: String) {
        _formState.value = _formState.value.copy(bodyFatPercent = value)
    }

    fun updateFormMuscle(value: String) {
        _formState.value = _formState.value.copy(muscleKg = value)
    }

    fun updateFormWaist(value: String) {
        _formState.value = _formState.value.copy(waistCm = value)
    }

    fun updateFormNote(value: String) {
        _formState.value = _formState.value.copy(note = value)
    }

    fun saveMeasurement() {
        val form = _formState.value
        viewModelScope.launch {
            try {
                val measurement = BodyMeasurement(
                    id = form.editId ?: 0,
                    date = form.date,
                    weightKg = form.weightKg.toDoubleOrNull(),
                    bodyFatPercent = form.bodyFatPercent.toDoubleOrNull(),
                    muscleKg = form.muscleKg.toDoubleOrNull(),
                    waistCm = form.waistCm.toDoubleOrNull(),
                    note = form.note.ifBlank { null },
                )
                bodyMeasurementRepository.saveMeasurement(measurement)
                hideForm()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            try {
                bodyMeasurementRepository.delete(measurement.date)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun formatDouble(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            "%.1f".format(value)
        }
    }
}
