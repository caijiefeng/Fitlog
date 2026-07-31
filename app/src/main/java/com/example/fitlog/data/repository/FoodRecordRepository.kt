package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.FoodRecordDao
import com.example.fitlog.core.database.entity.FoodRecordEntity
import com.example.fitlog.domain.nutrition.NutritionTargetCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class MealSubtotal(
    val mealType: String,
    val count: Int,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

data class DailyNutritionSummary(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val mealSubtotals: List<MealSubtotal> = emptyList(),
    val targetCalories: Int = 0,
    val targetProtein: Int = 0,
    val targetCarbs: Int = 0,
    val targetFat: Int = 0,
    val tdee: Int = 0,
    val completionRate: Double = 0.0, // 0.0 to 1.0
)

data class FoodRecord(
    val id: Long = 0,
    val date: LocalDate,
    val mealType: String,
    val foodName: String,
    val calories: Double? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
    val amount: String? = null,
    val note: String? = null,
    // Nutrition snapshot: which food was consumed and how much
    val foodSourceId: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val grams: Double? = null,
)

@Singleton
class FoodRecordRepository @Inject constructor(
    private val foodRecordDao: FoodRecordDao,
    private val userProfileRepository: UserProfileRepository,
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val nutritionTargetCalculator: NutritionTargetCalculator,
) {

    suspend fun save(record: FoodRecord): FoodRecord {
        val entity = FoodRecordEntity(
            id = record.id,
            date = record.date.toEpochDay(),
            mealType = record.mealType,
            foodName = record.foodName,
            calories = record.calories,
            proteinGrams = record.proteinGrams,
            carbsGrams = record.carbsGrams,
            fatGrams = record.fatGrams,
            amount = record.amount,
            note = record.note,
            foodSourceId = record.foodSourceId,
            quantity = record.quantity,
            unit = record.unit,
            grams = record.grams,
        )
        val id = foodRecordDao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    suspend fun delete(id: Long) {
        foodRecordDao.delete(id)
    }

    suspend fun getByDate(date: LocalDate): List<FoodRecord> {
        return foodRecordDao.getByDate(date.toEpochDay()).map { it.toDomain() }
    }

    suspend fun getByDateRange(start: LocalDate, end: LocalDate): List<FoodRecord> {
        return foodRecordDao.getByDateRange(start.toEpochDay(), end.toEpochDay())
            .map { it.toDomain() }
    }

    fun observeByDate(date: LocalDate): Flow<List<FoodRecord>> {
        return foodRecordDao.observeByDate(date.toEpochDay()).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Reactive daily summary: re-emits whenever the food records for [date] change,
     * so totals, meal subtotals and targets stay in sync without manual refresh.
     */
    fun observeDailySummary(date: LocalDate): Flow<DailyNutritionSummary> {
        return foodRecordDao.observeByDate(date.toEpochDay())
            .map { entities -> buildSummary(entities.map { it.toDomain() }, date) }
    }

    suspend fun getDailySummary(date: LocalDate): DailyNutritionSummary {
        val records = foodRecordDao.getByDate(date.toEpochDay()).map { it.toDomain() }
        return buildSummary(records, date)
    }

    private suspend fun buildSummary(
        records: List<FoodRecord>,
        date: LocalDate,
    ): DailyNutritionSummary {
        val totalCalories = records.sumOf { it.calories ?: 0.0 }
        val totalProtein = records.sumOf { it.proteinGrams ?: 0.0 }
        val totalCarbs = records.sumOf { it.carbsGrams ?: 0.0 }
        val totalFat = records.sumOf { it.fatGrams ?: 0.0 }

        val mealSubtotals = records.groupBy { it.mealType }
            .map { (mealType, list) ->
                MealSubtotal(
                    mealType = mealType,
                    count = list.size,
                    calories = list.sumOf { it.calories ?: 0.0 },
                    protein = list.sumOf { it.proteinGrams ?: 0.0 },
                    carbs = list.sumOf { it.carbsGrams ?: 0.0 },
                    fat = list.sumOf { it.fatGrams ?: 0.0 },
                )
            }

        // Calculate targets from user profile + latest body measurement
        val profile = userProfileRepository.get()
        val measurement = if (profile != null) {
            bodyMeasurementRepository.getLatestOnOrBefore(date)
        } else null

        if (profile != null && measurement != null) {
            val targets = nutritionTargetCalculator.calculateTargets(profile, measurement, date)
            if (targets != null) {
                val completionRate = if (targets.targetCalories > 0) {
                    (totalCalories / targets.targetCalories).coerceIn(0.0, 1.5)
                } else 0.0
                return DailyNutritionSummary(
                    calories = totalCalories,
                    protein = totalProtein,
                    carbs = totalCarbs,
                    fat = totalFat,
                    mealSubtotals = mealSubtotals,
                    targetCalories = targets.targetCalories,
                    targetProtein = targets.proteinG,
                    targetCarbs = targets.carbsG,
                    targetFat = targets.fatG,
                    tdee = targets.tdee,
                    completionRate = completionRate,
                )
            }
        }

        // No profile or measurement data available — return consumed amounts only
        return DailyNutritionSummary(
            calories = totalCalories,
            protein = totalProtein,
            carbs = totalCarbs,
            fat = totalFat,
            mealSubtotals = mealSubtotals,
        )
    }

    private fun FoodRecordEntity.toDomain(): FoodRecord = FoodRecord(
        id = id,
        date = LocalDate.ofEpochDay(date),
        mealType = mealType,
        foodName = foodName,
        calories = calories,
        proteinGrams = proteinGrams,
        carbsGrams = carbsGrams,
        fatGrams = fatGrams,
        amount = amount,
        note = note,
        foodSourceId = foodSourceId,
        quantity = quantity,
        unit = unit,
        grams = grams,
    )
}
