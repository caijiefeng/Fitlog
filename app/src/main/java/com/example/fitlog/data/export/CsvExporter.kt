package com.example.fitlog.data.export

import com.example.fitlog.core.database.dao.BodyMeasurementDao
import com.example.fitlog.core.database.dao.CheckInDao
import com.example.fitlog.core.database.dao.ExerciseSessionDao
import com.example.fitlog.core.database.dao.FoodRecordDao
import com.example.fitlog.core.database.dao.SetRecordDao
import com.example.fitlog.core.database.dao.UserProfileDao
import com.example.fitlog.core.database.dao.WorkoutSessionDao
import com.example.fitlog.core.database.dao.WorkoutTemplateDao
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports FitLog data as CSV with:
 * - UTF-8 BOM (﻿)
 * - RFC 4180 escaping (fields containing comma, double-quote, or newline
 *   are wrapped in double-quotes; embedded double-quotes are doubled)
 * - ISO-8601 date representation (LocalDate.toString())
 * - Null values rendered as empty string (never "0" or "null")
 */
@Singleton
class CsvExporter @Inject constructor(
    private val workoutSessionDao: WorkoutSessionDao,
    private val exerciseSessionDao: ExerciseSessionDao,
    private val setRecordDao: SetRecordDao,
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val checkInDao: CheckInDao,
    private val foodRecordDao: FoodRecordDao,
    private val userProfileDao: UserProfileDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
) {

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Writes all CSV export sections (workouts, body measurements, nutrition,
     * check-ins) to [output] as a single concatenated document.
     */
    fun exportAll(
        output: OutputStream,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        val writer = OutputStreamWriter(output, UTF_8)

        // Write BOM
        writer.write(BOM)

        writeWorkouts(writer, zoneId)
        writeBodyMeasurements(writer)
        writeNutrition(writer)
        writeCheckIns(writer)

        writer.flush()
    }

    /**
     * Exports only the workout sessions section.
     */
    fun exportWorkouts(output: OutputStream, zoneId: ZoneId = ZoneId.systemDefault()) {
        val writer = OutputStreamWriter(output, UTF_8)
        writer.write(BOM)
        writeWorkouts(writer, zoneId)
        writer.flush()
    }

    /**
     * Exports only the body measurements section.
     */
    fun exportBodyMeasurements(output: OutputStream) {
        val writer = OutputStreamWriter(output, UTF_8)
        writer.write(BOM)
        writeBodyMeasurements(writer)
        writer.flush()
    }

    /**
     * Exports only the nutrition (food records) section.
     */
    fun exportNutrition(output: OutputStream) {
        val writer = OutputStreamWriter(output, UTF_8)
        writer.write(BOM)
        writeNutrition(writer)
        writer.flush()
    }

    /**
     * Exports only the check-ins section.
     */
    fun exportCheckIns(output: OutputStream) {
        val writer = OutputStreamWriter(output, UTF_8)
        writer.write(BOM)
        writeCheckIns(writer)
        writer.flush()
    }

    // ── Section writers ─────────────────────────────────────────────────────

    private fun writeWorkouts(writer: OutputStreamWriter, zoneId: ZoneId) {
        writer.write("# Workouts\n")
        writer.write(workoutHeader())
        writer.write('\n'.code) // newline after header

        val sessions = workoutSessionDao.getAll()
        for (session in sessions) {
            val exercises = exerciseSessionDao.getBySession(session.id)
            val allSets = mutableListOf<com.example.fitlog.core.database.entity.SetRecordEntity>()
            for (es in exercises) {
                allSets.addAll(setRecordDao.getByExerciseSession(es.id))
            }

            val dateStr = LocalDate.ofEpochDay(session.date).toString()

            // One row per session with aggregated set data encoded as sub-columns
            // Columns: date, template_name, status, start_time, end_time,
            //          duration_minutes, notes, exercise_count, set_count, total_volume_kg
            val durationMinutes = if (session.startTime != null && session.endTime != null) {
                ((session.endTime - session.startTime) / 60000).toInt()
            } else null

            val totalVolume = allSets
                .filter { it.completed && it.reps != null && it.weightKg != null }
                .sumOf { (it.reps!! * it.weightKg!!).toDouble() }

            val values = listOf(
                dateStr,
                session.templateNameSnapshot,
                session.status,
                session.startTime?.let { formatEpochMillis(it, zoneId) },
                session.endTime?.let { formatEpochMillis(it, zoneId) },
                durationMinutes?.toString(),
                session.notes,
                exercises.size.toString(),
                allSets.size.toString(),
                if (totalVolume > 0.0) formatDouble(totalVolume) else null,
            )
            writer.write(escapeCsvRow(values))
            writer.write('\n'.code)
        }
        writer.write('\n'.code)
    }

    private fun writeBodyMeasurements(writer: OutputStreamWriter) {
        writer.write("# Body Measurements\n")
        writer.write(bodyMeasurementHeader())
        writer.write('\n'.code)

        val measurements = bodyMeasurementDao.getAll()
        for (m in measurements) {
            val dateStr = LocalDate.ofEpochDay(m.date).toString()
            val values = listOf(
                dateStr,
                m.weightKg?.let { formatDouble(it) },
                m.bodyFatPercent?.let { formatDouble(it) },
                m.muscleKg?.let { formatDouble(it) },
                m.waistCm?.let { formatDouble(it) },
                m.note,
            )
            writer.write(escapeCsvRow(values))
            writer.write('\n'.code)
        }
        writer.write('\n'.code)
    }

    private fun writeNutrition(writer: OutputStreamWriter) {
        writer.write("# Nutrition\n")
        writer.write(nutritionHeader())
        writer.write('\n'.code)

        val records = foodRecordDao.getAll()
        for (r in records) {
            val dateStr = LocalDate.ofEpochDay(r.date).toString()
            val values = listOf(
                dateStr,
                r.mealType,
                r.foodName,
                r.calories?.let { formatDouble(it) },
                r.proteinGrams?.let { formatDouble(it) },
                r.carbsGrams?.let { formatDouble(it) },
                r.fatGrams?.let { formatDouble(it) },
                r.amount,
                r.note,
            )
            writer.write(escapeCsvRow(values))
            writer.write('\n'.code)
        }
        writer.write('\n'.code)
    }

    private fun writeCheckIns(writer: OutputStreamWriter) {
        writer.write("# Check-ins\n")
        writer.write(checkInHeader())
        writer.write('\n'.code)

        val checkIns = checkInDao.getAll()
        for (c in checkIns) {
            val dateStr = LocalDate.ofEpochDay(c.date).toString()
            val values = listOf(
                dateStr,
                c.mood?.toString(),
                c.energyLevel?.toString(),
                c.notes,
            )
            writer.write(escapeCsvRow(values))
            writer.write('\n'.code)
        }
        writer.write('\n'.code)
    }

    // ── Headers ─────────────────────────────────────────────────────────────

    private fun workoutHeader(): String = escapeCsvRow(listOf(
        "date", "template_name", "status", "start_time", "end_time",
        "duration_minutes", "notes", "exercise_count", "set_count", "total_volume_kg",
    ))

    private fun bodyMeasurementHeader(): String = escapeCsvRow(listOf(
        "date", "weight_kg", "body_fat_percent", "muscle_kg", "waist_cm", "note",
    ))

    private fun nutritionHeader(): String = escapeCsvRow(listOf(
        "date", "meal_type", "food_name", "calories", "protein_g",
        "carbs_g", "fat_g", "amount", "note",
    ))

    private fun checkInHeader(): String = escapeCsvRow(listOf(
        "date", "mood", "energy_level", "notes",
    ))

    // ── CSV escaping (RFC 4180) ─────────────────────────────────────────────

    /**
     * Escapes a single CSV value per RFC 4180.
     *
     * - If the value contains a comma, double-quote, or newline it is wrapped
     *   in double-quotes.
     * - Embedded double-quotes are doubled ("").
     * - Null values are returned as empty string.
     */
    private fun escapeCsvValue(value: String?): String {
        if (value == null) return ""
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Joins [values] into a single RFC 4180 CSV row.
     */
    private fun escapeCsvRow(values: List<String?>): String =
        values.joinToString(",") { escapeCsvValue(it) }

    // ── Formatting helpers ──────────────────────────────────────────────────

    private fun formatEpochMillis(epochMillis: Long, zoneId: ZoneId): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val zdt = ZonedDateTime.ofInstant(instant, zoneId)
        return zdt.toLocalDateTime().toString() // ISO-8601: 2026-07-28T14:30:00
    }

    private fun formatDouble(value: Double): String {
        // Strip trailing zeros: 70.0 -> "70", 70.5 -> "70.5", 70.55 -> "70.55"
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    companion object {
        private val UTF_8 = Charset.forName("UTF-8")
        private const val BOM = "﻿"
    }
}
