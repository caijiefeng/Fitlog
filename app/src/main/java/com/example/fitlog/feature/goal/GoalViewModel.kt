package com.example.fitlog.feature.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.BodyMeasurementRepository
import com.example.fitlog.data.repository.UserProfileRepository
import com.example.fitlog.domain.body.BodyGoalPlan
import com.example.fitlog.domain.body.BodyGoalPlanner
import com.example.fitlog.domain.body.BodyMeasurement
import com.example.fitlog.domain.body.GoalType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class GoalUiState(
    val isLoading: Boolean = true,
    val goalPlan: BodyGoalPlan? = null,
    val currentWeightKg: Double? = null,
    val currentBodyFatPercent: Double? = null,
    val targetBodyFatPercent: Double? = null,
    val goalType: GoalType = GoalType.MAINTAIN,
    val error: String? = null,
)

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val bodyGoalPlanner: BodyGoalPlanner,
    private val dateProvider: CurrentDateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    init {
        loadGoalData()
    }

    private fun loadGoalData() {
        viewModelScope.launch {
            try {
                val profile = userProfileRepository.get()
                val measurements = bodyMeasurementRepository.getByDateRange(
                    LocalDate.ofEpochDay(0), dateProvider.today()
                )
                val latest = measurements.lastOrNull()

                if (profile == null || latest == null) {
                    _uiState.value = GoalUiState(
                        isLoading = false,
                        goalType = profile?.goalType ?: GoalType.MAINTAIN,
                    )
                    return@launch
                }

                val currentWeight = latest.weightKg ?: 75.0
                val currentBodyFat = latest.bodyFatPercent ?: 20.0
                val targetBodyFat = profile.targetBodyFat ?: (currentBodyFat - 5.0)

                val age = ChronoUnit.YEARS.between(profile.birthday, dateProvider.today()).toInt()
                val height = profile.heightCm ?: 175.0

                // Calculate approximate TDEE
                val bmr = (10 * currentWeight + 6.25 * height - 5 * age).let { base ->
                    when (profile.gender.uppercase()) {
                        "MALE" -> (base + 5).toInt()
                        "FEMALE" -> (base - 161).toInt()
                        else -> ((base + 5) + (base - 161)) / 2
                    }
                }
                val tdee = (bmr.toDouble() * profile.activityLevel.factor).toInt()

                val goalPlan = bodyGoalPlanner.plan(
                    currentWeightKg = currentWeight,
                    currentBodyFatPercent = currentBodyFat,
                    targetBodyFatPercent = targetBodyFat,
                    tdee = tdee,
                )

                _uiState.value = GoalUiState(
                    isLoading = false,
                    goalPlan = goalPlan,
                    currentWeightKg = currentWeight,
                    currentBodyFatPercent = currentBodyFat,
                    targetBodyFatPercent = targetBodyFat,
                    goalType = profile.goalType,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }
}
