package com.example.fitlog.data.export

import com.example.fitlog.core.database.entity.BodyMeasurementEntity
import com.example.fitlog.core.database.entity.CheckInEntity
import com.example.fitlog.core.database.entity.FoodRecordEntity
import com.example.fitlog.core.database.entity.WorkoutSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneId

/**
 * Tests for [CsvExporter] pure functions covering:
 * - UTF-8 BOM presence
 * - RFC 4180 escaping (commas, double-quotes, newlines)
 * - Chinese / Unicode text preservation
 * - ISO 8601 date rendering
 * - Null values rendered as empty fields
 * - Non-null numeric values without trailing zeros
 */
class CsvExporterTest {

    // ── BOM ─────────────────────────────────────────────────────────────────

    @Test
    fun `exportWorkouts starts with UTF-8 BOM`() {
        val bytes = exportWorkouts(emptyList(), emptyList(), emptyList())
        assertBom(bytes)
    }

    @Test
    fun `exportBodyMeasurements starts with UTF-8 BOM`() {
        val bytes = exportBodyMeasurements(emptyList())
        assertBom(bytes)
    }

    @Test
    fun `exportNutrition starts with UTF-8 BOM`() {
        val bytes = exportNutrition(emptyList())
        assertBom(bytes)
    }

    @Test
    fun `exportCheckIns starts with UTF-8 BOM`() {
        val bytes = exportCheckIns(emptyList())
        assertBom(bytes)
    }

    // ── Empty data → section markers and headers ────────────────────────────

    @Test
    fun `exportWorkouts with empty data produces header`() {
        val csv = csvString(exportWorkouts(emptyList(), emptyList(), emptyList()))
        assertTrue(csv, csv.contains("# Workouts"))
        assertTrue(csv, csv.contains("date,template_name,status"))
    }

    @Test
    fun `exportBodyMeasurements with empty data produces header`() {
        val csv = csvString(exportBodyMeasurements(emptyList()))
        assertTrue(csv, csv.contains("# Body Measurements"))
        assertTrue(csv, csv.contains("date,weight_kg,body_fat_percent"))
    }

    @Test
    fun `exportNutrition with empty data produces header`() {
        val csv = csvString(exportNutrition(emptyList()))
        assertTrue(csv, csv.contains("# Nutrition"))
        assertTrue(csv, csv.contains("date,meal_type,food_name"))
    }

    @Test
    fun `exportCheckIns with empty data produces header`() {
        val csv = csvString(exportCheckIns(emptyList()))
        assertTrue(csv, csv.contains("# Check-ins"))
        assertTrue(csv, csv.contains("date,mood,energy_level"))
    }

    // ── ISO date ────────────────────────────────────────────────────────────

    @Test
    fun `workout CSV uses ISO-8601 date`() {
        val session = WorkoutSessionEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            startTime = LocalDate.of(2026, 7, 28).atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli(),
            status = "COMPLETED",
            templateNameSnapshot = "Push Day",
        )
        val csv = csvString(exportWorkouts(listOf(session), emptyList(), emptyList()))
        assertTrue(csv, csv.contains("2026-07-28"))
    }

    @Test
    fun `body measurement CSV uses ISO-8601 date`() {
        val m = BodyMeasurementEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            weightKg = 70.0,
        )
        val csv = csvString(exportBodyMeasurements(listOf(m)))
        assertTrue(csv, csv.contains("2026-07-28"))
    }

    @Test
    fun `nutrition CSV uses ISO-8601 date`() {
        val r = FoodRecordEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            mealType = "LUNCH",
            foodName = "Rice",
        )
        val csv = csvString(exportNutrition(listOf(r)))
        assertTrue(csv, csv.contains("2026-07-28"))
    }

    @Test
    fun `check-in CSV uses ISO-8601 date`() {
        val c = CheckInEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
        )
        val csv = csvString(exportCheckIns(listOf(c)))
        assertTrue(csv, csv.contains("2026-07-28"))
    }

    // ── Null handling ───────────────────────────────────────────────────────

    @Test
    fun `body measurement with all-null fields renders empty cells`() {
        val m = BodyMeasurementEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            weightKg = null,
            bodyFatPercent = null,
            muscleKg = null,
            waistCm = null,
            note = null,
        )
        val csv = csvString(exportBodyMeasurements(listOf(m)))
        val line = csv.lines().first { it.startsWith("2026-07-28") }
        val fields = line.split(",")
        assertEquals(6, fields.size)
        assertEquals("2026-07-28", fields[0])
        assertEquals("", fields[1]) // weightKg
        assertEquals("", fields[2]) // bodyFatPercent
        assertEquals("", fields[3]) // muscleKg
        assertEquals("", fields[4]) // waistCm
        assertEquals("", fields[5]) // note
    }

    @Test
    fun `workout with null endTime and notes renders empty cells`() {
        val session = WorkoutSessionEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            startTime = LocalDate.of(2026, 7, 28).atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli(),
            endTime = null,
            status = "COMPLETED",
            templateNameSnapshot = "Push Day",
            notes = null,
        )
        val csv = csvString(exportWorkouts(listOf(session), emptyList(), emptyList()))
        val line = csv.lines().first { it.startsWith("2026-07-28") }
        val fields = line.split(",")
        assertEquals("2026-07-28", fields[0])
        assertEquals("Push Day", fields[1])
        assertEquals("COMPLETED", fields[2])
        assertTrue(fields[3].isNotEmpty()) // start_time
        assertEquals("", fields[4]) // end_time (null)
        assertEquals("", fields[5]) // duration_minutes (null because end_time null)
        assertEquals("", fields[6]) // notes (null)
    }

    // ── Numeric formatting ──────────────────────────────────────────────────

    @Test
    fun `body measurement with values strips trailing zeros from integers`() {
        val m = BodyMeasurementEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            weightKg = 70.0,
            bodyFatPercent = 15.5,
            waistCm = 82.33,
        )
        val csv = csvString(exportBodyMeasurements(listOf(m)))
        val line = csv.lines().first { it.startsWith("2026-07-28") }
        val fields = line.split(",")
        assertEquals("70", fields[1])   // 70.0 → "70"
        assertEquals("15.5", fields[2]) // 15.5 → "15.5"
        assertEquals("82.33", fields[4]) // 82.33 → "82.33"
    }

    // ── Chinese text and commas ─────────────────────────────────────────────

    @Test
    fun `Chinese text and commas are properly escaped`() {
        val food = FoodRecordEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            mealType = "BREAKFAST",
            foodName = "鸡蛋, 牛奶, 面包",
            calories = 350.0,
        )
        val csv = csvString(exportNutrition(listOf(food)))
        assertTrue(csv, csv.contains("2026-07-28"))
        // foodName contains commas → should be quoted
        assertTrue(csv, csv.contains("\"鸡蛋, 牛奶, 面包\""))
    }

    @Test
    fun `Chinese note in workout is preserved`() {
        val session = WorkoutSessionEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            startTime = LocalDate.of(2026, 7, 28).atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli(),
            status = "COMPLETED",
            templateNameSnapshot = "腿部训练",
            notes = "深蹲, 硬拉, 腿举",
        )
        val csv = csvString(exportWorkouts(listOf(session), emptyList(), emptyList()))
        assertTrue(csv, csv.contains("腿部训练"))
        assertTrue(csv, csv.contains("\"深蹲, 硬拉, 腿举\""))
    }

    // ── Double-quote escaping (RFC 4180) ────────────────────────────────────

    @Test
    fun `double-quotes in values are doubled per RFC 4180`() {
        val checkIn = CheckInEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            mood = 5,
            energyLevel = 4,
            notes = "He said \"Great workout!\"",
        )
        val csv = csvString(exportCheckIns(listOf(checkIn)))
        // The field is quoted because it contains a double-quote,
        // and the embedded quote is doubled: "He said ""Great workout!"""
        assertTrue(csv, csv.contains("\"He said \"\"Great workout!\"\"\""))
    }

    // ── Newline handling ────────────────────────────────────────────────────

    @Test
    fun `newlines in values are wrapped in quotes`() {
        val checkIn = CheckInEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            mood = 3,
            energyLevel = 3,
            notes = "Line one\nLine two\nLine three",
        )
        val csv = csvString(exportCheckIns(listOf(checkIn)))
        assertTrue(csv, csv.contains("\"Line one\nLine two\nLine three\""))
    }

    // ── Exercise / set record aggregation ───────────────────────────────────

    @Test
    fun `workout with completed sets includes volume and counts`() {
        val session = WorkoutSessionEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            startTime = LocalDate.of(2026, 7, 28).atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli(),
            endTime = LocalDate.of(2026, 7, 28).atStartOfDay(ZoneId.of("UTC"))
                .plusHours(1).toInstant().toEpochMilli(),
            status = "COMPLETED",
            templateNameSnapshot = "Leg Day",
        )
        val es = com.example.fitlog.core.database.entity.ExerciseSessionEntity(
            id = 10,
            sessionId = 1,
            exerciseNameSnapshot = "Squat",
            primaryMuscleGroupSnapshot = "Legs",
        )
        val set1 = com.example.fitlog.core.database.entity.SetRecordEntity(
            id = 100,
            exerciseSessionId = 10,
            setNumber = 1,
            reps = 10,
            weightKg = 100.0,
            completed = true,
        )
        val set2 = com.example.fitlog.core.database.entity.SetRecordEntity(
            id = 101,
            exerciseSessionId = 10,
            setNumber = 2,
            reps = 8,
            weightKg = 100.0,
            completed = true,
        )

        val csv = csvString(exportWorkouts(listOf(session), listOf(es), listOf(set1, set2)))
        val line = csv.lines().first { it.startsWith("2026-07-28") }
        assertTrue(line, line.contains("Leg Day"))
        // 1 exercise, 2 sets
        assertTrue(line, line.contains(",1,2,"))
        // volume = 10*100 + 8*100 = 1800
        assertTrue(line, line.contains(",1800"))
        // duration = 60 min
        assertTrue(line, line.contains(",60,"))
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun assertBom(bytes: ByteArray) {
        assertEquals(bytes.toString(), 0xEF, bytes[0].toInt() and 0xFF)
        assertEquals(bytes.toString(), 0xBB, bytes[1].toInt() and 0xFF)
        assertEquals(bytes.toString(), 0xBF, bytes[2].toInt() and 0xFF)
    }

    private fun csvString(bytes: ByteArray): String {
        // Strip BOM for easier assertions
        val content = if (bytes.size >= 3 &&
            bytes[0].toInt() and 0xFF == 0xEF &&
            bytes[1].toInt() and 0xFF == 0xBB &&
            bytes[2].toInt() and 0xFF == 0xBF
        ) {
            bytes.copyOfRange(3, bytes.size)
        } else bytes
        return String(content, Charsets.UTF_8)
    }
}
