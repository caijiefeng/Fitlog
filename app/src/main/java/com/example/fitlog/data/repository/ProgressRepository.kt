package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.BodyMeasurementDao
import com.example.fitlog.core.database.dao.FoodRecordDao
import com.example.fitlog.core.database.dao.SetRecordDao
import com.example.fitlog.core.database.dao.WorkoutSessionDao
import com.example.fitlog.core.database.entity.BodyMeasurementEntity
import com.example.fitlog.core.database.entity.WorkoutSessionEntity
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
) {

    /**
     * Returns aggregated trend points within [range], ordered by date ascending.
     *
     * Body measurements join on epochDay.  Food records sum per day in bulk.
     * Workout volume sums per session in bulk.  The three lists are merged
     * into [TrendPoint] instances — one per date where any metric exists.
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
        val foodSums = mutableMapOf<Long, DoubleArray>() // epochDay -> [calories, protein]
        for (f in foodRows) {
            val sums = foodSums.getOrPut(f.date) { doubleArrayOf(0.0, 0.0) }
            f.calories?.let { sums[0] += it }
            f.proteinGrams?.let { sums[1] += it }
        }
        for ((epochDay, sums) in foodSums) {
            points.getOrPut(epochDay) { TrendPointBuilder() }.apply {
                calories = sums[0]
                protein = sums[1]
            }
        }

        // ── Workout volume — aggregate per session in one pass ─────────
        val sessions = workoutSessionDao.getByDateRange(startEpochDay, endEpochDay)
        val terminalStatuses = setOf("COMPLETED", "PARTIALLY_COMPLETED")
        for (session in sessions) {
            if (session.status !in terminalStatuses) continue
            val volume = setRecordDao.totalVolumeForSession(session.id) ?: 0.0
            if (volume > 0.0) {
                points.getOrPut(session.date) { TrendPointBuilder() }.apply {
                    this.volume = (this.volume ?: 0.0) + volume
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
        val today = LocalDate.now()
        val start = if (range.days < 0) {
            LocalDate.ofEpochDay(0) // earliest representable
        } else {
            today.minusDays(range.days.toLong() - 1)
        }
        return start.toEpochDay() to today.toEpochDay()
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
