package com.example.fitlog.data.export

import com.example.fitlog.core.database.dao.BodyMeasurementDao
import com.example.fitlog.core.database.dao.CheckInDao
import com.example.fitlog.core.database.dao.ExerciseSessionDao
import com.example.fitlog.core.database.dao.FoodRecordDao
import com.example.fitlog.core.database.dao.SetRecordDao
import com.example.fitlog.core.database.dao.UserProfileDao
import com.example.fitlog.core.database.dao.WorkoutSessionDao
import com.example.fitlog.core.database.dao.WorkoutTemplateDao
import com.example.fitlog.core.database.entity.BodyMeasurementEntity
import com.example.fitlog.core.database.entity.CheckInEntity
import com.example.fitlog.core.database.entity.FoodRecordEntity
import com.example.fitlog.core.database.entity.WorkoutSessionEntity
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneId

/**
 * Tests [CsvExporter] for:
 * - UTF-8 BOM presence
 * - RFC 4180 escaping (commas, double-quotes, newlines)
 * - Chinese/Unicode text
 * - ISO 8601 date rendering
 * - Null values rendered as empty fields
 * - Non-null numeric values rendered correctly
 */
class CsvExporterTest {

    private val workoutSessionDao = mockk<WorkoutSessionDao>()
    private val exerciseSessionDao = mockk<ExerciseSessionDao>()
    private val setRecordDao = mockk<SetRecordDao>()
    private val bodyMeasurementDao = mockk<BodyMeasurementDao>()
    private val checkInDao = mockk<CheckInDao>()
    private val foodRecordDao = mockk<FoodRecordDao>()
    private val userProfileDao = mockk<UserProfileDao>(relaxed = true)
    private val workoutTemplateDao = mockk<WorkoutTemplateDao>(relaxed = true)

    private lateinit var exporter: CsvExporter

    @Before
    fun setUp() {
        exporter = CsvExporter(
            workoutSessionDao = workoutSessionDao,
            exerciseSessionDao = exerciseSessionDao,
            setRecordDao = setRecordDao,
            bodyMeasurementDao = bodyMeasurementDao,
            checkInDao = checkInDao,
            foodRecordDao = foodRecordDao,
            userProfileDao = userProfileDao,
            workoutTemplateDao = workoutTemplateDao,
        )
    }

    @Test
    fun `exportAll outputs UTF-8 BOM`() {
        // Given: empty tables
        coEvery { workoutSessionDao.getAll() } returns emptyList()
        coEvery { bodyMeasurementDao.getAll() } returns emptyList()
        coEvery { checkInDao.getAll() } returns emptyList()
        coEvery { foodRecordDao.getAll() } returns emptyList()
        coEvery { exerciseSessionDao.getAll() } returns emptyList()
        coEvery { setRecordDao.getAll() } returns emptyList()

        val output = ByteArrayOutputStream()
        exporter.exportAll(output)

        val bytes = output.toByteArray()
        // BOM is 0xEF,0xBB,0xBF
        assertEquals(0xEF, bytes[0].toInt() and 0xFF)
        assertEquals(0xBB, bytes[1].toInt() and 0xFF)
        assertEquals(0xBF, bytes[2].toInt() and 0xFF)
    }

    @Test
    fun `exportAll with empty tables produces headers and section markers`() {
        coEvery { workoutSessionDao.getAll() } returns emptyList()
        coEvery { bodyMeasurementDao.getAll() } returns emptyList()
        coEvery { checkInDao.getAll() } returns emptyList()
        coEvery { foodRecordDao.getAll() } returns emptyList()
        coEvery { exerciseSessionDao.getAll() } returns emptyList()
        coEvery { setRecordDao.getAll() } returns emptyList()

        val output = ByteArrayOutputStream()
        exporter.exportAll(output)

        val csv = output.toString(Charsets.UTF_8)
        // Should contain all section headers
        assertTrue(csv.contains("# Workouts"))
        assertTrue(csv.contains("# Body Measurements"))
        assertTrue(csv.contains("# Nutrition"))
        assertTrue(csv.contains("# Check-ins"))
        // Should contain all CSV headers
        assertTrue(csv.contains("date,template_name,status"))
        assertTrue(csv.contains("date,weight_kg,body_fat_percent"))
        assertTrue(csv.contains("date,meal_type,food_name"))
        assertTrue(csv.contains("date,mood,energy_level"))
    }

    @Test
    fun `workout CSV includes ISO date, escaped text, and null handling`() {
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
        coEvery { workoutSessionDao.getAll() } returns listOf(session)
        coEvery { exerciseSessionDao.getBySession(1) } returns emptyList()
        coEvery { setRecordDao.getAll() } returns emptyList()
        coEvery { bodyMeasurementDao.getAll() } returns emptyList()
        coEvery { checkInDao.getAll() } returns emptyList()
        coEvery { foodRecordDao.getAll() } returns emptyList()

        val output = ByteArrayOutputStream()
        exporter.exportWorkouts(output)

        val csv = output.toString(Charsets.UTF_8)
        // Check ISO date
        assertTrue(csv.contains("2026-07-28"))
        // Check null end_time rendered as empty
        val dataLine = csv.lines().first { it.startsWith("2026-07-28") }
        // date, template, status, start_time, end_time(null), duration_minutes(null), notes(null), exercise_count, set_count, volume(null)
        val fields = dataLine.split(",")
        assertEquals("2026-07-28", fields[0])
        assertEquals("Push Day", fields[1])
        assertEquals("COMPLETED", fields[2])
        // start_time should not be empty
        assertTrue(fields[3].isNotEmpty())
        // end_time should be empty (null)
        assertEquals("", fields[4])
        // duration_minutes should be empty (null because end_time is null)
        assertEquals("", fields[5])
        // notes should be empty
        assertEquals("", fields[6])
    }

    @Test
    fun `body measurement with null fields renders empty cells`() {
        val measurement = BodyMeasurementEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            weightKg = null,
            bodyFatPercent = null,
            muscleKg = null,
            waistCm = null,
            note = null,
        )
        coEvery { bodyMeasurementDao.getAll() } returns listOf(measurement)
        coEvery { workoutSessionDao.getAll() } returns emptyList()
        coEvery { checkInDao.getAll() } returns emptyList()
        coEvery { foodRecordDao.getAll() } returns emptyList()
        coEvery { exerciseSessionDao.getAll() } returns emptyList()
        coEvery { setRecordDao.getAll() } returns emptyList()

        val output = ByteArrayOutputStream()
        exporter.exportBodyMeasurements(output)

        val csv = output.toString(Charsets.UTF_8)
        val dataLine = csv.lines().first { it.startsWith("2026-07-28") }
        // date, weight_kg, body_fat_percent, muscle_kg, waist_cm, note
        val fields = dataLine.split(",")
        assertEquals("2026-07-28", fields[0])
        assertEquals("", fields[1])  // weightKg null
        assertEquals("", fields[2])  // bodyFatPercent null
        assertEquals("", fields[3])  // muscleKg null
        assertEquals("", fields[4])  // waistCm null
        assertEquals("", fields[5])  // note null
    }

    @Test
    fun `body measurement with values renders numbers without trailing zeros`() {
        val measurement = BodyMeasurementEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            weightKg = 70.0,
            bodyFatPercent = 15.5,
            waistCm = 82.33,
        )
        coEvery { bodyMeasurementDao.getAll() } returns listOf(measurement)
        coEvery { workoutSessionDao.getAll() } returns emptyList()
        coEvery { checkInDao.getAll() } returns emptyList()
        coEvery { foodRecordDao.getAll() } returns emptyList()
        coEvery { exerciseSessionDao.getAll() } returns emptyList()
        coEvery { setRecordDao.getAll() } returns emptyList()

        val output = ByteArrayOutputStream()
        exporter.exportBodyMeasurements(output)

        val csv = output.toString(Charsets.UTF_8)
        val line = csv.lines().first { it.startsWith("2026-07-28") }
        val fields = line.split(",")
        assertEquals("70", fields[1])   // 70.0 → "70"
        assertEquals("15.5", fields[2]) // 15.5 → "15.5"
        assertEquals("82.33", fields[4]) // 82.33 → "82.33"
    }

    @Test
    fun `Chinese text and commas are properly escaped`() {
        val food = FoodRecordEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            mealType = "BREAKFAST",
            foodName = "鸡蛋, 牛奶, 面包",  // Contains Chinese commas and spaces
            calories = 350.0,
            proteinGrams = 25.0,
            carbsGrams = 30.0,
            fatGrams = 10.0,
            amount = "100g",
            note = "今天训练前吃的好饱",
        )
        coEvery { foodRecordDao.getAll() } returns listOf(food)
        coEvery { workoutSessionDao.getAll() } returns emptyList()
        coEvery { bodyMeasurementDao.getAll() } returns emptyList()
        coEvery { checkInDao.getAll() } returns emptyList()
        coEvery { exerciseSessionDao.getAll() } returns emptyList()
        coEvery { setRecordDao.getAll() } returns emptyList()

        val output = ByteArrayOutputStream()
        exporter.exportNutrition(output)

        val csv = output.toString(Charsets.UTF_8)
        assertTrue(csv.contains("2026-07-28"))
        // foodName contains commas, so it should be quoted
        assertTrue(csv.contains("\"鸡蛋, 牛奶, 面包\""))
        // Chinese note should be preserved
        assertTrue(csv.contains("今天训练前吃的好饱"))
    }

    @Test
    fun `double-quotes in values are doubled per RFC 4180`() {
        val checkIn = CheckInEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            mood = 5,
            energyLevel = 4,
            notes = "He said \"Great workout!\"",
        )
        coEvery { checkInDao.getAll() } returns listOf(checkIn)
        coEvery { workoutSessionDao.getAll() } returns emptyList()
        coEvery { bodyMeasurementDao.getAll() } returns emptyList()
        coEvery { foodRecordDao.getAll() } returns emptyList()
        coEvery { exerciseSessionDao.getAll() } returns emptyList()
        coEvery { setRecordDao.getAll() } returns emptyList()

        val output = ByteArrayOutputStream()
        exporter.exportCheckIns(output)

        val csv = output.toString(Charsets.UTF_8)
        // The double-quote should be escaped as ""
        assertTrue(csv.contains("\"He said \"\"Great workout!\"\"\""))
    }

    @Test
    fun `newlines in values are wrapped in quotes`() {
        val checkIn = CheckInEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            mood = 3,
            energyLevel = 3,
            notes = "Line one\nLine two\nLine three",
        )
        coEvery { checkInDao.getAll() } returns listOf(checkIn)
        coEvery { workoutSessionDao.getAll() } returns emptyList()
        coEvery { bodyMeasurementDao.getAll() } returns emptyList()
        coEvery { foodRecordDao.getAll() } returns emptyList()
        coEvery { exerciseSessionDao.getAll() } returns emptyList()
        coEvery { setRecordDao.getAll() } returns emptyList()

        val output = ByteArrayOutputStream()
        exporter.exportCheckIns(output)

        val csv = output.toString(Charsets.UTF_8)
        // Should be wrapped in quotes because of the newline
        assertTrue(csv.contains("\"Line one\nLine two\nLine three\""))
    }

    @Test
    fun `complex workout with null fields and Chinese text`() {
        val session = WorkoutSessionEntity(
            id = 1,
            templateNameSnapshot = "腿部训练",  // Chinese: Leg Day
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            startTime = LocalDate.of(2026, 7, 28).atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli(),
            endTime = LocalDate.of(2026, 7, 28).atStartOfDay(ZoneId.of("UTC"))
                .plusHours(1).plusMinutes(30).toInstant().toEpochMilli(),
            status = "COMPLETED",
            notes = "深蹲, 硬拉, 腿举",  // Note with commas
        )
        coEvery { workoutSessionDao.getAll() } returns listOf(session)
        coEvery { exerciseSessionDao.getBySession(1) } returns emptyList()
        coEvery { setRecordDao.getAll() } returns emptyList()
        coEvery { bodyMeasurementDao.getAll() } returns emptyList()
        coEvery { checkInDao.getAll() } returns emptyList()
        coEvery { foodRecordDao.getAll() } returns emptyList()

        val output = ByteArrayOutputStream()
        exporter.exportWorkouts(output)

        val csv = output.toString(Charsets.UTF_8)
        // Chinese template name
        assertTrue(csv.contains("腿部训练"))
        // Notes with commas should be quoted
        assertTrue(csv.contains("\"深蹲, 硬拉, 腿举\""))
        // Duration should be 90 minutes
        assertTrue(csv.contains("90"))
    }
}
