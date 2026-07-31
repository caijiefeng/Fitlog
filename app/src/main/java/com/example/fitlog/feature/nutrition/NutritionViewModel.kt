package com.example.fitlog.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.DailyNutritionSummary
import com.example.fitlog.data.repository.FoodRecord
import com.example.fitlog.data.repository.FoodRecordRepository
import com.example.fitlog.data.repository.UserProfileRepository
import com.example.fitlog.domain.body.UserProfile
import com.example.fitlog.domain.nutrition.FoodDataProvider
import com.example.fitlog.domain.nutrition.FoodNutrition
import com.example.fitlog.domain.nutrition.FoodPortionCalculator
import com.example.fitlog.domain.nutrition.FoodSearchResult
import com.example.fitlog.domain.nutrition.NutritionAdvisor
import com.example.fitlog.domain.nutrition.NutritionAdvice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class NutritionUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate,
    val mealTypeFilter: String? = null,
    val allFoodRecords: List<FoodRecord> = emptyList(),
    val foodRecords: List<FoodRecord> = emptyList(),
    val summary: DailyNutritionSummary = DailyNutritionSummary(),
    val advice: NutritionAdvice? = null,
    val missingDataMessage: String? = null,
    val error: String? = null,
)

/**
 * State of the food entry dialog. Supports two paths:
 * - Food search: type a query, pick a [FoodSearchResult], then adjust servings
 *   ([quantity] x [unit]) or [grams]; macros are computed in real time.
 * - Manual entry (used for editing records that have no food source): the
 *   user fills in [manualName] and macro values directly.
 */
data class FoodEntryState(
    val isVisible: Boolean = false,
    val editId: Long? = null,
    val mealType: String = "BREAKFAST",
    // Food search (useManual = false) or manual macro entry (useManual = true)
    val useManual: Boolean = false,
    // Food search
    val query: String = "",
    val searchResults: List<FoodSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val selectedFood: FoodSearchResult? = null,
    // Amount: servings + unit, or direct grams
    val quantity: String = "1",
    val unit: String = "",
    val grams: String = "",
    // Computed macros from grams (real-time preview)
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    // Manual entry (edit mode / custom food)
    val manualName: String = "",
    val manualCalories: String = "",
    val manualProtein: String = "",
    val manualCarbs: String = "",
    val manualFat: String = "",
    val manualAmount: String = "",
    val note: String = "",
) {
    val isEdit: Boolean get() = editId != null
    val hasSelectedFood: Boolean get() = selectedFood != null
    val quantityValue: Double get() = quantity.toDoubleOrNull() ?: 0.0
    val gramsValue: Double get() = grams.toDoubleOrNull() ?: 0.0

    /** Grams actually used for calculation: explicit grams, else servings x serving size. */
    val effectiveGrams: Double
        get() = gramsValue.takeIf { it > 0 }
            ?: selectedFood?.servingSizeG?.let { servingG ->
                servingG * (quantityValue.takeIf { q -> q > 0 } ?: 0.0)
            }
            ?: 0.0
}

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val foodRecordRepository: FoodRecordRepository,
    private val userProfileRepository: UserProfileRepository,
    private val dateProvider: CurrentDateProvider,
    private val nutritionAdvisor: NutritionAdvisor,
    private val foodDataProvider: FoodDataProvider,
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(dateProvider.today())
    private val _mealTypeFilter = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)

    private val _entryState = MutableStateFlow(FoodEntryState())
    val entryState: StateFlow<FoodEntryState> = _entryState.asStateFlow()

    /**
     * Records + summary for the selected date. flatMapLatest keeps a single
     * collection active: changing the date cancels the previous one, so no
     * duplicate/leaked flows.
     */
    private val dayData = _selectedDate
        .flatMapLatest { date ->
            combine(
                foodRecordRepository.observeByDate(date),
                foodRecordRepository.observeDailySummary(date),
            ) { records, summary -> records to summary }
        }

    /**
     * Reactive UI state: records are always kept unfiltered (allFoodRecords);
     * the meal type filter is applied here, so switching tabs takes effect
     * immediately without waiting on the database.
     */
    val uiState: StateFlow<NutritionUiState> = combine(
        dayData,
        _mealTypeFilter,
        userProfileRepository.observe(),
        _error,
    ) { (records, summary), filter, profile, error ->
        val filtered = filter?.let { f -> records.filter { it.mealType == f } } ?: records
        val (advice, missingMessage) = buildAdvice(profile, summary)
        NutritionUiState(
            isLoading = false,
            selectedDate = _selectedDate.value,
            mealTypeFilter = filter,
            allFoodRecords = records,
            foodRecords = filtered,
            summary = summary,
            advice = advice,
            missingDataMessage = missingMessage,
            error = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NutritionUiState(selectedDate = dateProvider.today()),
    )

    init {
        observeFoodSearch()
    }

    /**
     * Search-as-you-type: the query stream is debounced and switched with
     * flatMapLatest, so each keystroke cancels the previous search.
     */
    private fun observeFoodSearch() {
        viewModelScope.launch {
            _entryState
                .map { it.query }
                .distinctUntilChanged()
                .debounce(250)
                .flatMapLatest { query ->
                    if (query.isBlank()) flowOf(emptyList())
                    else flow { emit(foodDataProvider.search(query.trim())) }
                }
                .collect { results ->
                    val entry = _entryState.value
                    if (entry.isVisible && !entry.useManual && entry.selectedFood == null) {
                        _entryState.value = entry.copy(searchResults = results, isSearching = false)
                    }
                }
        }
    }

    private fun buildAdvice(
        profile: UserProfile?,
        summary: DailyNutritionSummary,
    ): Pair<NutritionAdvice?, String?> {
        if (profile != null && summary.targetCalories > 0 && summary.tdee > 0) {
            val advice = nutritionAdvisor.generateAdvice(
                goalType = profile.goalType,
                tdee = summary.tdee,
                targetCalories = summary.targetCalories,
                proteinG = summary.targetProtein,
                carbsG = summary.targetCarbs,
                fatG = summary.targetFat,
            )
            return advice to null
        }
        // No targets available — check if profile/measurement data is missing
        val missingData = profile == null ||
            profile.heightCm == null ||
            profile.gender.isBlank() ||
            profile.activityLevel.name.isBlank()
        if (missingData) {
            return null to "请先完善身高、出生日期、性别、活动水平和当前体重，完成后才能生成营养目标。"
        }
        return null to null
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setMealTypeFilter(mealType: String?) {
        _mealTypeFilter.value = mealType
    }

    // ── Food entry dialog ──────────────────────────────────────────────────

    fun showAddForm() {
        _entryState.value = FoodEntryState(
            isVisible = true,
            mealType = _mealTypeFilter.value ?: "BREAKFAST",
        )
    }

    fun showEditForm(record: FoodRecord) {
        viewModelScope.launch {
            val food = record.foodSourceId?.let { foodDataProvider.getFood(it) }
            if (food != null) {
                _entryState.value = FoodEntryState(
                    isVisible = true,
                    editId = record.id,
                    mealType = record.mealType,
                    query = record.foodName,
                    selectedFood = food.toSearchResult(),
                    quantity = record.quantity?.let { formatDouble(it) } ?: "1",
                    unit = record.unit ?: food.servingDesc?.toUnit() ?: "",
                    grams = record.grams?.let { formatDouble(it) }
                        ?: food.servingSizeG?.let { formatDouble(it) } ?: "",
                    calories = record.calories ?: 0.0,
                    protein = record.proteinGrams ?: 0.0,
                    carbs = record.carbsGrams ?: 0.0,
                    fat = record.fatGrams ?: 0.0,
                    note = record.note ?: "",
                )
            } else {
                _entryState.value = FoodEntryState(
                    isVisible = true,
                    editId = record.id,
                    mealType = record.mealType,
                    useManual = true,
                    manualName = record.foodName,
                    manualCalories = record.calories?.let { formatDouble(it) } ?: "",
                    manualProtein = record.proteinGrams?.let { formatDouble(it) } ?: "",
                    manualCarbs = record.carbsGrams?.let { formatDouble(it) } ?: "",
                    manualFat = record.fatGrams?.let { formatDouble(it) } ?: "",
                    manualAmount = record.amount ?: "",
                    note = record.note ?: "",
                )
            }
        }
    }

    fun hideForm() {
        _entryState.value = FoodEntryState()
    }

    /** Switch between food search and manual macro entry (only before a food is chosen). */
    fun toggleEntryMode() {
        val entry = _entryState.value
        if (entry.selectedFood != null) return
        _entryState.value = entry.copy(useManual = !entry.useManual)
    }

    fun updateEntryQuery(value: String) {
        val current = _entryState.value
        val selectedFood = current.selectedFood
        _entryState.value = if (selectedFood != null && value != selectedFood.name) {
            // User started typing a new search — clear the selected food
            current.copy(
                query = value,
                selectedFood = null,
                searchResults = emptyList(),
                isSearching = value.isNotBlank(),
                quantity = "1",
                unit = "",
                grams = "",
                calories = 0.0,
                protein = 0.0,
                carbs = 0.0,
                fat = 0.0,
            )
        } else {
            current.copy(query = value, isSearching = value.isNotBlank())
        }
    }

    fun selectFood(result: FoodSearchResult) {
        val unit = result.servingDesc?.toUnit() ?: "份"
        var entry = _entryState.value.copy(
            query = result.name,
            selectedFood = result,
            searchResults = emptyList(),
            isSearching = false,
            quantity = "1",
            unit = unit,
            grams = result.servingSizeG?.let { formatDouble(it) } ?: "",
        )
        entry = withComputedMacros(entry)
        _entryState.value = entry
    }

    fun updateEntryMealType(value: String) {
        _entryState.value = _entryState.value.copy(mealType = value)
    }

    /** Servings-based entry: changing the number of servings rescales grams. */
    fun updateEntryQuantity(value: String) {
        val entry = _entryState.value
        val food = entry.selectedFood ?: return
        val qty = value.toDoubleOrNull()
        val grams = if (qty != null && qty > 0 && food.servingSizeG != null) {
            formatDouble(qty * food.servingSizeG)
        } else {
            ""
        }
        _entryState.value = withComputedMacros(entry.copy(quantity = value, grams = grams))
    }

    /** Gram-based entry: changing grams rescales servings and recomputes macros. */
    fun updateEntryGrams(value: String) {
        val entry = _entryState.value
        val food = entry.selectedFood ?: return
        val g = value.toDoubleOrNull()
        val quantity = if (g != null && g > 0 && food.servingSizeG != null && food.servingSizeG > 0) {
            formatDouble(g / food.servingSizeG)
        } else {
            "1"
        }
        _entryState.value = withComputedMacros(entry.copy(grams = value, quantity = quantity))
    }

    fun updateEntryUnit(value: String) {
        _entryState.value = _entryState.value.copy(unit = value)
    }

    fun updateEntryNote(value: String) {
        _entryState.value = _entryState.value.copy(note = value)
    }

    fun updateEntryManualName(value: String) {
        _entryState.value = _entryState.value.copy(manualName = value)
    }

    fun updateEntryManualCalories(value: String) {
        _entryState.value = _entryState.value.copy(manualCalories = value)
    }

    fun updateEntryManualProtein(value: String) {
        _entryState.value = _entryState.value.copy(manualProtein = value)
    }

    fun updateEntryManualCarbs(value: String) {
        _entryState.value = _entryState.value.copy(manualCarbs = value)
    }

    fun updateEntryManualFat(value: String) {
        _entryState.value = _entryState.value.copy(manualFat = value)
    }

    fun updateEntryManualAmount(value: String) {
        _entryState.value = _entryState.value.copy(manualAmount = value)
    }

    fun saveFoodRecord() {
        val entry = _entryState.value
        val date = _selectedDate.value

        val record = if (!entry.useManual && entry.selectedFood != null) {
            buildRecordFromFood(entry, date)
        } else {
            if (entry.manualName.isBlank()) return
            FoodRecord(
                id = entry.editId ?: 0,
                date = date,
                mealType = entry.mealType,
                foodName = entry.manualName,
                calories = entry.manualCalories.toDoubleOrNull(),
                proteinGrams = entry.manualProtein.toDoubleOrNull(),
                carbsGrams = entry.manualCarbs.toDoubleOrNull(),
                fatGrams = entry.manualFat.toDoubleOrNull(),
                amount = entry.manualAmount.ifBlank { null },
                note = entry.note.ifBlank { null },
            )
        }

        viewModelScope.launch {
            try {
                foodRecordRepository.save(record)
                _error.value = null
                _entryState.value = FoodEntryState()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Builds a record with a nutrition snapshot: factor = grams / 100.0 and
     * every macro scaled by the food's per-100g values.
     */
    private fun buildRecordFromFood(entry: FoodEntryState, date: LocalDate): FoodRecord {
        val food = entry.selectedFood ?: error("no food selected")
        val grams = entry.effectiveGrams
        val portion = FoodPortionCalculator.calculate(food, grams)
        return FoodRecord(
            id = entry.editId ?: 0,
            date = date,
            mealType = entry.mealType,
            foodName = food.name,
            calories = portion.calories,
            proteinGrams = portion.protein,
            carbsGrams = portion.carbs,
            fatGrams = portion.fat,
            amount = buildAmount(entry),
            note = entry.note.ifBlank { null },
            foodSourceId = food.id,
            quantity = entry.quantityValue.takeIf { it > 0 },
            unit = entry.unit.trim().ifBlank { null },
            grams = grams.takeIf { it > 0 },
        )
    }

    private fun buildAmount(entry: FoodEntryState): String? {
        val unit = entry.unit.trim()
        return when {
            unit.isNotEmpty() -> "${formatDouble(entry.quantityValue.takeIf { it > 0 } ?: 1.0)} $unit"
            entry.effectiveGrams > 0 -> "${formatDouble(entry.effectiveGrams)}g"
            else -> null
        }
    }

    fun refresh() {
        _selectedDate.value = dateProvider.today()
    }

    fun deleteFoodRecord(record: FoodRecord) {
        viewModelScope.launch {
            try {
                foodRecordRepository.delete(record.id)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun withComputedMacros(entry: FoodEntryState): FoodEntryState {
        val food = entry.selectedFood ?: return entry
        val portion = FoodPortionCalculator.calculate(food, entry.effectiveGrams)
        return entry.copy(
            calories = portion.calories,
            protein = portion.protein,
            carbs = portion.carbs,
            fat = portion.fat,
        )
    }

    private fun formatDouble(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            "%.1f".format(value)
        }
    }

    /** "1碗" -> "碗", "100g" -> "g" */
    private fun String.toUnit(): String = replace(Regex("^[0-9.,\\s]+"), "").ifBlank { "份" }

    private fun FoodNutrition.toSearchResult(): FoodSearchResult = FoodSearchResult(
        id = id,
        name = name,
        category = category,
        caloriesPer100g = caloriesPer100g,
        proteinPer100g = proteinPer100g,
        carbsPer100g = carbsPer100g,
        fatPer100g = fatPer100g,
        servingSizeG = servingSizeG,
        servingDesc = servingDesc,
    )
}
