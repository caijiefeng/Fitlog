package com.example.fitlog.feature.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.BodyMeasurementRepository
import com.example.fitlog.data.repository.UserProfileRepository
import com.example.fitlog.domain.body.ActivityLevel
import com.example.fitlog.domain.body.BodyMeasurement
import com.example.fitlog.domain.body.GoalType
import com.example.fitlog.domain.body.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BodyProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val gender: String = "",
    val birthday: LocalDate? = null,
    val heightCm: String = "",
    val weightKg: String = "",
    val bodyFatPercent: String = "",
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goalType: GoalType = GoalType.MAINTAIN,
    val targetBodyFat: String = "",
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class BodyProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val dateProvider: CurrentDateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyProfileUiState())
    val uiState: StateFlow<BodyProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val profile = userProfileRepository.get()
                val measurements = bodyMeasurementRepository.getByDateRange(
                    LocalDate.ofEpochDay(0), dateProvider.today()
                )
                val latestMeasurement = measurements.lastOrNull()

                if (profile != null) {
                    _uiState.value = BodyProfileUiState(
                        isLoading = false,
                        gender = profile.gender,
                        birthday = profile.birthday,
                        heightCm = profile.heightCm?.let { formatDouble(it) } ?: "",
                        weightKg = latestMeasurement?.weightKg?.let { formatDouble(it) } ?: "",
                        bodyFatPercent = latestMeasurement?.bodyFatPercent?.let { formatDouble(it) } ?: "",
                        activityLevel = profile.activityLevel,
                        goalType = profile.goalType,
                        targetBodyFat = profile.targetBodyFat?.let { formatDouble(it) } ?: "",
                    )
                } else {
                    _uiState.value = BodyProfileUiState(
                        isLoading = false,
                        weightKg = latestMeasurement?.weightKg?.let { formatDouble(it) } ?: "",
                        bodyFatPercent = latestMeasurement?.bodyFatPercent?.let { formatDouble(it) } ?: "",
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

    fun updateGender(value: String) {
        _uiState.value = _uiState.value.copy(gender = value)
    }

    fun updateBirthday(value: LocalDate) {
        _uiState.value = _uiState.value.copy(birthday = value)
    }

    fun updateHeightCm(value: String) {
        _uiState.value = _uiState.value.copy(heightCm = value)
    }

    fun updateWeightKg(value: String) {
        _uiState.value = _uiState.value.copy(weightKg = value)
    }

    fun updateBodyFatPercent(value: String) {
        _uiState.value = _uiState.value.copy(bodyFatPercent = value)
    }

    fun updateActivityLevel(value: ActivityLevel) {
        _uiState.value = _uiState.value.copy(activityLevel = value)
    }

    fun updateGoalType(value: GoalType) {
        _uiState.value = _uiState.value.copy(goalType = value)
    }

    fun updateTargetBodyFat(value: String) {
        _uiState.value = _uiState.value.copy(targetBodyFat = value)
    }

    fun save() {
        val state = _uiState.value
        if (state.gender.isBlank() || state.birthday == null || state.heightCm.isBlank()) {
            _uiState.value = state.copy(error = "请填写完整信息（性别、出生日期、身高）")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val profile = UserProfile(
                    gender = state.gender,
                    birthday = state.birthday,
                    heightCm = state.heightCm.toDoubleOrNull(),
                    activityLevel = state.activityLevel,
                    goalType = state.goalType,
                    targetBodyFat = state.targetBodyFat.toDoubleOrNull(),
                )
                userProfileRepository.saveProfile(profile)

                // Save weight and body fat as a body measurement for today
                val weight = state.weightKg.toDoubleOrNull()
                val bodyFat = state.bodyFatPercent.toDoubleOrNull()
                if (weight != null || bodyFat != null) {
                    bodyMeasurementRepository.saveMeasurement(
                        BodyMeasurement(
                            date = dateProvider.today(),
                            weightKg = weight,
                            bodyFatPercent = bodyFat,
                        )
                    )
                }

                _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message,
                )
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
