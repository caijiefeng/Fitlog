package com.example.fitlog.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.DailyNutritionSummary
import com.example.fitlog.data.repository.FoodRecord
import com.example.fitlog.data.repository.FoodRecordRepository
import com.example.fitlog.data.repository.UserProfileRepository
import com.example.fitlog.domain.nutrition.EnergyCalculator
import com.example.fitlog.domain.nutrition.NutritionAdvisor
import com.example.fitlog.domain.nutrition.NutritionAdvice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class NutritionUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate,
    val mealTypeFilter: String? = null,
    val foodRecords: List<FoodRecord> = emptyList(),
    val summary: DailyNutritionSummary = DailyNutritionSummary(),
    val advice: NutritionAdvice? = null,
    val error: String? = null,
)

data class FoodFormState(
    val isVisible: Boolean = false,
    val editId: Long? = null,
    val foodName: String = "",
    val mealType: String = "BREAKFAST",
    val calories: String = "",
    val proteinGrams: String = "",
    val carbsGrams: String = "",
    val fatGrams: String = "",
    val amount: String = "",
    val note: String = "",
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val foodRecordRepository: FoodRecordRepository,
    private val userProfileRepository: UserProfileRepository,
    private val dateProvider: CurrentDateProvider,
    private val nutritionAdvisor: NutritionAdvisor,
    private val energyCalculator: EnergyCalculator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NutritionUiState(selectedDate = dateProvider.today())
    )
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(FoodFormState())
    val formState: StateFlow<FoodFormState> = _formState.asStateFlow()

    init {
        loadToday()
    }

    private fun loadToday() {
        val today = dateProvider.today()
        _uiState.value = _uiState.value.copy(selectedDate = today)
        observeRecords(today)
        loadSummary(today)
    }

    private fun observeRecords(date: LocalDate) {
        viewModelScope.launch {
            foodRecordRepository.observeByDate(date).collect { records ->
                val filtered = if (_uiState.value.mealTypeFilter != null) {
                    records.filter { it.mealType == _uiState.value.mealTypeFilter }
                } else {
                    records
                }
                _uiState.value = _uiState.value.copy(
                    foodRecords = filtered,
                    isLoading = false,
                )
            }
        }
    }

    private fun loadSummary(date: LocalDate) {
        viewModelScope.launch {
            try {
                val summary = foodRecordRepository.getDailySummary(date)
                _uiState.value = _uiState.value.copy(summary = summary)

                // Generate nutrition advice from profile data
                val profile = userProfileRepository.get()
                if (profile != null && summary.targetCalories > 0) {
                    val weight = summary.let { 75.0 } // Fallback weight
                    val proteinG = (1.8 * weight).toInt()
                    val fatG = (0.9 * weight).toInt()
                    val proteinCalories = proteinG * 4
                    val fatCalories = fatG * 9
                    val remainingCalories = summary.targetCalories - proteinCalories - fatCalories
                    val carbsG = (remainingCalories / 4).coerceAtLeast(0)

                    val advice = nutritionAdvisor.generateAdvice(
                        goalType = profile.goalType,
                        tdee = 0, // We don't have TDEE in summary, but advisor text is optional
                        targetCalories = summary.targetCalories,
                        proteinG = proteinG,
                        carbsG = carbsG,
                        fatG = fatG,
                    )
                    _uiState.value = _uiState.value.copy(advice = advice)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        observeRecords(date)
        loadSummary(date)
    }

    fun setMealTypeFilter(mealType: String?) {
        _uiState.value = _uiState.value.copy(mealTypeFilter = mealType)
    }

    fun showAddForm() {
        _formState.value = FoodFormState(
            isVisible = true,
            mealType = _uiState.value.mealTypeFilter ?: "BREAKFAST",
        )
    }

    fun showEditForm(record: FoodRecord) {
        _formState.value = FoodFormState(
            isVisible = true,
            editId = record.id,
            foodName = record.foodName,
            mealType = record.mealType,
            calories = record.calories?.let { formatDouble(it) } ?: "",
            proteinGrams = record.proteinGrams?.let { formatDouble(it) } ?: "",
            carbsGrams = record.carbsGrams?.let { formatDouble(it) } ?: "",
            fatGrams = record.fatGrams?.let { formatDouble(it) } ?: "",
            amount = record.amount ?: "",
            note = record.note ?: "",
        )
    }

    fun hideForm() {
        _formState.value = FoodFormState()
    }

    fun updateFormFoodName(value: String) {
        _formState.value = _formState.value.copy(foodName = value)
    }

    fun updateFormMealType(value: String) {
        _formState.value = _formState.value.copy(mealType = value)
    }

    fun updateFormCalories(value: String) {
        _formState.value = _formState.value.copy(calories = value)
    }

    fun updateFormProtein(value: String) {
        _formState.value = _formState.value.copy(proteinGrams = value)
    }

    fun updateFormCarbs(value: String) {
        _formState.value = _formState.value.copy(carbsGrams = value)
    }

    fun updateFormFat(value: String) {
        _formState.value = _formState.value.copy(fatGrams = value)
    }

    fun updateFormAmount(value: String) {
        _formState.value = _formState.value.copy(amount = value)
    }

    fun updateFormNote(value: String) {
        _formState.value = _formState.value.copy(note = value)
    }

    fun saveFoodRecord() {
        val form = _formState.value
        if (form.foodName.isBlank()) return

        viewModelScope.launch {
            try {
                val record = FoodRecord(
                    id = form.editId ?: 0,
                    date = _uiState.value.selectedDate,
                    mealType = form.mealType,
                    foodName = form.foodName,
                    calories = form.calories.toDoubleOrNull(),
                    proteinGrams = form.proteinGrams.toDoubleOrNull(),
                    carbsGrams = form.carbsGrams.toDoubleOrNull(),
                    fatGrams = form.fatGrams.toDoubleOrNull(),
                    amount = form.amount.ifBlank { null },
                    note = form.note.ifBlank { null },
                )
                foodRecordRepository.save(record)
                hideForm()
                loadSummary(_uiState.value.selectedDate)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun refresh() {
        loadToday()
    }

    fun deleteFoodRecord(record: FoodRecord) {
        viewModelScope.launch {
            try {
                foodRecordRepository.delete(record.id)
                loadSummary(_uiState.value.selectedDate)
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
