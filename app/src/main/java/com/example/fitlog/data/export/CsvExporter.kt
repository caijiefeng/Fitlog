package com.example.fitlog.data.export

import com.example.fitlog.core.database.entity.BodyMeasurementEntity
import com.example.fitlog.core.database.entity.CheckInEntity
import com.example.fitlog.core.database.entity.ExerciseSessionEntity
import com.example.fitlog.core.database.entity.FoodRecordEntity
import com.example.fitlog.core.database.entity.SetRecordEntity
import com.example.fitlog.core.database.entity.WorkoutSessionEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure Kotlin functions that export FitLog data as CSV byte arrays.
 *
 * All functions produce RFC 4180 compliant CSV with:
 * - UTF-8 BOM byte order mark (EF BB BF)
 * - ISO-8601 date strings via [LocalDate.toString]
 * - RFC 4180 field escaping (commas, double-quotes, and newlines trigger
 *   wrapping in double-quotes; embedded double-quotes are doubled)
 * - Null values rendered as empty strings
 */

private val UTF_8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

// ── Public API ─────────────────────────────────────────────────────────────

/**
 * Exports workout session data as CSV.
 *
 * Header: date, template_name, status, start_time, end_time, duration_minutes,
 *         notes, exercise_count, set_count, total_volume_kg
 *
 * @param sessions All workout sessions (typically sorted by date).
 * @param exerciseSessions All exercise sessions (filtered per session internally).
 * @param setRecords All set records (filtered per exercise session internally).
 * @param zoneId Time zone for formatting start/end timestamps.
 */
fun exportWorkouts(
    sessions: List<WorkoutSessionEntity>,
    exerciseSessions: List<ExerciseSessionEntity>,
    setRecords: List<SetRecordEntity>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): ByteArray {
    val sb = StringBuilder()
    sb.append("# Workouts\n")
    sb.append(workoutHeader())
    sb.append('\n')

    val esBySession = exerciseSessions.groupBy { it.sessionId }
    val srByExerciseSession = setRecords.groupBy { it.exerciseSessionId }

    for (session in sessions) {
        val exercises = esBySession[session.id].orEmpty()
        val allSets = exercises.flatMap { srByExerciseSession[it.id].orEmpty() }

        val dateStr = LocalDate.ofEpochDay(session.date).toString()
        val durationMinutes = if (session.endTime != null) {
            ((session.endTime - session.startTime) / 60000).toInt()
        } else null

        val totalVolume = allSets
            .filter { it.completed && it.reps != null && it.weightKg != null }
            .sumOf { (it.reps!!.toDouble() * it.weightKg!!) }

        val values = listOf(
            dateStr,
            session.templateNameSnapshot,
            session.status,
            session.startTime.let { formatEpochMillis(it, zoneId) },
            session.endTime?.let { formatEpochMillis(it, zoneId) },
            durationMinutes?.toString(),
            session.notes,
            exercises.size.toString(),
            allSets.size.toString(),
            if (totalVolume > 0.0) formatDouble(totalVolume) else null,
        )
        sb.append(escapeCsvRow(values))
        sb.append('\n')
    }
    sb.append('\n')
    return utf8WithBom(sb)
}

/**
 * Exports body measurements as CSV.
 *
 * Header: date, weight_kg, body_fat_percent, muscle_kg, waist_cm, note
 */
fun exportBodyMeasurements(
    measurements: List<BodyMeasurementEntity>,
): ByteArray {
    val sb = StringBuilder()
    sb.append("# Body Measurements\n")
    sb.append(bodyMeasurementHeader())
    sb.append('\n')

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
        sb.append(escapeCsvRow(values))
        sb.append('\n')
    }
    sb.append('\n')
    return utf8WithBom(sb)
}

/**
 * Exports nutrition (food records) as CSV.
 *
 * Header: date, meal_type, food_name, calories, protein_g, carbs_g, fat_g,
 *         amount, note, food_source_id, quantity, unit, grams
 */
fun exportNutrition(
    records: List<FoodRecordEntity>,
): ByteArray {
    val sb = StringBuilder()
    sb.append("# Nutrition\n")
    sb.append(nutritionHeader())
    sb.append('\n')

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
            r.foodSourceId,
            r.quantity?.let { formatDouble(it) },
            r.unit,
            r.grams?.let { formatDouble(it) },
        )
        sb.append(escapeCsvRow(values))
        sb.append('\n')
    }
    sb.append('\n')
    return utf8WithBom(sb)
}

/**
 * Exports daily check-ins as CSV.
 *
 * Header: date, mood, energy_level, notes
 */
fun exportCheckIns(
    checkIns: List<CheckInEntity>,
): ByteArray {
    val sb = StringBuilder()
    sb.append("# Check-ins\n")
    sb.append(checkInHeader())
    sb.append('\n')

    for (c in checkIns) {
        val dateStr = LocalDate.ofEpochDay(c.date).toString()
        val values = listOf(
            dateStr,
            c.mood?.toString(),
            c.energyLevel?.toString(),
            c.notes,
        )
        sb.append(escapeCsvRow(values))
        sb.append('\n')
    }
    sb.append('\n')
    return utf8WithBom(sb)
}

// ── Headers ─────────────────────────────────────────────────────────────────

private fun workoutHeader(): String = escapeCsvRow(listOf(
    "date", "template_name", "status", "start_time", "end_time",
    "duration_minutes", "notes", "exercise_count", "set_count", "total_volume_kg",
))

private fun bodyMeasurementHeader(): String = escapeCsvRow(listOf(
    "date", "weight_kg", "body_fat_percent", "muscle_kg", "waist_cm", "note",
))

private fun nutritionHeader(): String = escapeCsvRow(listOf(
    "date", "meal_type", "food_name", "calories", "protein_g",
    "carbs_g", "fat_g", "amount", "note", "food_source_id",
    "quantity", "unit", "grams",
))

private fun checkInHeader(): String = escapeCsvRow(listOf(
    "date", "mood", "energy_level", "notes",
))

// ── CSV escaping (RFC 4180) ─────────────────────────────────────────────────

/**
 * Escapes a single CSV value per RFC 4180.
 *
 * - If the value contains a comma, double-quote, or newline, it is wrapped
 *   in double-quotes.
 * - Embedded double-quotes are doubled ("" → """").
 * - Null values are returned as an empty string.
 */
internal fun escapeCsvValue(value: String?): String {
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
internal fun escapeCsvRow(values: List<String?>): String =
    values.joinToString(",") { escapeCsvValue(it) }

// ── Formatting helpers ──────────────────────────────────────────────────────

private fun formatEpochMillis(epochMillis: Long, zoneId: ZoneId): String {
    val zdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMillis), zoneId)
    return zdt.toLocalDateTime().toString() // ISO-8601: 2026-07-28T14:30:00
}

/**
 * Formats a Double without trailing zeros: 70.0 → "70", 70.5 → "70.5".
 */
internal fun formatDouble(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

private fun utf8WithBom(sb: StringBuilder): ByteArray {
    val content = sb.toString().toByteArray(Charsets.UTF_8)
    return UTF_8_BOM + content
}
