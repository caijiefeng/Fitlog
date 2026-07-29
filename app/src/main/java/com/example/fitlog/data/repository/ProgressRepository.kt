package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.BodyMeasurementDao
import com.example.fitlog.core.database.dao.FoodRecordDao
import com.example.fitlog.core.database.dao.SetRecordDao
import com.example.fitlog.core.database.dao.WorkoutSessionDao
import com.example.fitlog.core.time.CurrentDateProvider
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregated data point for trend charts.
 *
 * Each field maps to a chartable metric.  Null means "no data for this date".
 * Use [date] as the x-axis and any non-null field as the y-axis value.
 */
data class TrendPoint(
    val date: LocalDate,
    val weight: Double? = null,
    val bodyFat: Double? = null,
    val waist: Double? = null,
    val calories: Double? = null,
    val protein: Double? = null,
    val volume: Double? = null,
)

/**
 * Time window used for chart range selectors.
 */
enum class TrendRange(val labelResName: String, val days: Int) {
    WEEK_7("trend_range_7d", 7),
    MONTH_30("trend_range_30d", 30),
    MONTH_90("trend_range_90d", 90),
    MONTHS_6("trend_range_6m", 180),
    ALL("trend_range_all", -1), // -1 means unlimited
}

/**
 * Repository that aggregates data across multiple tables to produce
 * [TrendPoint] lists for charts.  Queries are designed to avoid N+1:
 * every data source is fetched in a single DAO call per range.
 */
@Singleton
class ProgressRepository @Inject constructor(
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val foodRecordDao: FoodRecordDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val setRecordDao: SetRecordDao,
    private val currentDateProvider: CurrentDateProvider,
) {

    /**
     * Returns aggregated trend points within [range], ordered by date ascending.
     *
     * Body measurements join on epochDay.  Food records sum per day in bulk.
     * Workout volume sums per day in bulk via [SetRecordDao.getDailyVolumeByDateRange].
     * The three lists are merged into [TrendPoint] instances — one per date
     * where any metric exists.
     */
    suspend fun getTrendPoints(range: TrendRange): List<TrendPoint> {
        val (startEpochDay, endEpochDay) = computeRange(range)
        val points = mutableMapOf<Long, TrendPointBuilder>()

        // ── Body measurements ───────────────────────────────────────────
        val measurements = bodyMeasurementDao.getByDateRange(startEpochDay, endEpochDay)
        for (m in measurements) {
            points.getOrPut(m.date) { TrendPointBuilder() }.apply {
                weight = m.weightKg
                bodyFat = m.bodyFatPercent
                waist = m.waistCm
            }
        }

        // ── Food records — aggregate sums per day in one pass ──────────
        val foodRows = foodRecordDao.getByDateRange(startEpochDay, endEpochDay)
        // epochDay -> Pair(sum, hasValue) for [calories, protein]
        val foodSums = mutableMapOf<Long, FoodAccumulator>()
        for (f in foodRows) {
            val acc = foodSums.getOrPut(f.date) { FoodAccumulator() }
            f.calories?.let { acc.caloriesSum += it; acc.hasCalories = true }
            f.proteinGrams?.let { acc.proteinSum += it; acc.hasProtein = true }
        }
        for ((epochDay, acc) in foodSums) {
            points.getOrPut(epochDay) { TrendPointBuilder() }.apply {
                calories = if (acc.hasCalories) acc.caloriesSum else null
                protein = if (acc.hasProtein) acc.proteinSum else null
            }
        }

        // ── Workout volume — batch query per date range ────────────────
        val dailyVolumes = setRecordDao.getDailyVolumeByDateRange(startEpochDay, endEpochDay)
        for (dv in dailyVolumes) {
            if (dv.volume > 0.0) {
                points.getOrPut(dv.date) { TrendPointBuilder() }.apply {
                    volume = (this.volume ?: 0.0) + dv.volume
                }
            }
        }

        // ── Assemble sorted result ─────────────────────────────────────
        return points.entries
            .map { (epochDay, builder) ->
                TrendPoint(
                    date = LocalDate.ofEpochDay(epochDay),
                    weight = builder.weight,
                    bodyFat = builder.bodyFat,
                    waist = builder.waist,
                    calories = builder.calories,
                    protein = builder.protein,
                    volume = builder.volume,
                )
            }
            .sortedBy { it.date }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private fun computeRange(range: TrendRange): Pair<Long, Long> {
        val today = currentDateProvider.today()
        val start = if (range.days < 0) {
            LocalDate.ofEpochDay(0) // earliest representable
        } else {
            today.minusDays(range.days.toLong() - 1)
        }
        return start.toEpochDay() to today.toEpochDay()
    }

    /**
     * Accumulates food-nutrient values per day, tracking whether any non-null
     * values were seen so the result can be null (not 0) when all values are null.
     */
    private class FoodAccumulator {
        var caloriesSum: Double = 0.0
        var hasCalories: Boolean = false
        var proteinSum: Double = 0.0
        var hasProtein: Boolean = false
    }

    /**
     * Mutable builder used during the merge loop.
     */
    private data class TrendPointBuilder(
        var weight: Double? = null,
        var bodyFat: Double? = null,
        var waist: Double? = null,
        var calories: Double? = null,
        var protein: Double? = null,
        var volume: Double? = null,
    )
}
