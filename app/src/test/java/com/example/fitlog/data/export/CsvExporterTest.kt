package com.example.fitlog.data.export

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.example.fitlog.data.repository.BodyMeasurementRepository
import com.example.fitlog.data.repository.CheckInRepository
import com.example.fitlog.data.repository.FoodRecordRepository
import com.example.fitlog.data.repository.WorkoutSessionRepository
import com.example.fitlog.domain.body.BodyMeasurement
import com.example.fitlog.domain.checkin.CheckIn
import com.example.fitlog.data.repository.FoodRecord
import com.example.fitlog.core.model.WorkoutSession as DomainWorkoutSession
import com.example.fitlog.core.model.WorkoutStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate

class CsvExporterTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var bodyMeasurementRepo: BodyMeasurementRepository
    private lateinit var checkInRepo: CheckInRepository
    private lateinit var foodRecordRepo: FoodRecordRepository
    private lateinit var workoutSessionRepo: WorkoutSessionRepository
    private lateinit var exporter: CsvExporter

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        bodyMeasurementRepo = mockk(relaxed = true)
        checkInRepo = mockk(relaxed = true)
        foodRecordRepo = mockk(relaxed = true)
        workoutSessionRepo = mockk(relaxed = true)

        every { context.contentResolver } returns contentResolver

        exporter = CsvExporter(
            context = context,
            bodyMeasurementRepository = bodyMeasurementRepo,
            checkInRepository = checkInRepo,
            foodRecordRepository = foodRecordRepo,
            workoutSessionRepository = workoutSessionRepo,
        )
    }

    @Test
    fun `exportBodyMeasurements writes BOM and headers`() = runTest {
        val uri = mockk<Uri>()
        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(uri) } returns outputStream
        coEvery { bodyMeasurementRepo.observeAll() } returns flowOf(emptyList())

        val result = exporter.exportBodyMeasurements(uri)
        val csv = outputStream.toString(Charsets.UTF_8.name())

        assertTrue(result is CsvExporter.ExportResult.Success)
        // Check BOM
        assertEquals(0xFEFF, csv[0].code)
        // Check header
        assertTrue(csv.contains("Date,WeightKg,BodyFatPercent"))
    }

    @Test
    fun `exportBodyMeasurements includes data rows`() = runTest {
        val uri = mockk<Uri>()
        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(uri) } returns outputStream
        coEvery { bodyMeasurementRepo.observeAll() } returns flowOf(
            listOf(
                BodyMeasurement(
                    id = 1, date = LocalDate.of(2024, 1, 15),
                    weightKg = 75.5, bodyFatPercent = null,
                    waistCm = 85.0, note = "Test note",
                ),
            )
        )

        val result = exporter.exportBodyMeasurements(uri)
        val csv = outputStream.toString(Charsets.UTF_8.name())

        assertTrue(result is CsvExporter.ExportResult.Success)
        assertTrue(csv.contains("2024-01-15"))
        assertTrue(csv.contains("75.5"))
        assertTrue(csv.contains("85.0"))
        assertTrue(csv.contains("Test note"))
        // bodyFatPercent is null -> empty field
    }

    @Test
    fun `exportBodyMeasurements escapes commas in notes`() = runTest {
        val uri = mockk<Uri>()
        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(uri) } returns outputStream
        coEvery { bodyMeasurementRepo.observeAll() } returns flowOf(
            listOf(
                BodyMeasurement(
                    id = 1, date = LocalDate.of(2024, 1, 15),
                    weightKg = 70.0, note = "Has, comma",
                ),
            )
        )

        exporter.exportBodyMeasurements(uri)
        val csv = outputStream.toString(Charsets.UTF_8.name())

        assertTrue(csv.contains("\"Has, comma\""))
    }

    @Test
    fun `exportCheckIns writes date and nullable values`() = runTest {
        val uri = mockk<Uri>()
        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(uri) } returns outputStream
        coEvery { checkInRepo.getByDateRange(any(), any()) } returns listOf(
            CheckIn(id = 1, date = LocalDate.of(2024, 3, 10), sessionId = null, mood = 4, energyLevel = null, notes = "Good"),
        )

        val result = exporter.exportCheckIns(uri)
        val csv = outputStream.toString(Charsets.UTF_8.name())

        assertTrue(result is CsvExporter.ExportResult.Success)
        assertTrue(csv.contains("2024-03-10"))
        assertTrue(csv.contains("4"))
        assertTrue(csv.contains("Good"))
    }

    @Test
    fun `exportNutrition includes all fields`() = runTest {
        val uri = mockk<Uri>()
        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(uri) } returns outputStream
        coEvery { foodRecordRepo.getByDateRange(any(), any()) } returns listOf(
            FoodRecord(
                id = 1, date = LocalDate.of(2024, 5, 20),
                mealType = "LUNCH", foodName = "Rice",
                calories = 500.0, proteinGrams = 10.0,
                carbsGrams = 100.0, fatGrams = 2.0,
                amount = "1 bowl", note = "White rice",
            ),
        )

        val result = exporter.exportNutrition(uri)
        val csv = outputStream.toString(Charsets.UTF_8.name())

        assertTrue(result is CsvExporter.ExportResult.Success)
        assertTrue(csv.contains("LUNCH"))
        assertTrue(csv.contains("Rice"))
        assertTrue(csv.contains("500.0"))
        assertTrue(csv.contains("1 bowl"))
    }

    @Test
    fun `exportWorkouts includes computed fields`() = runTest {
        val uri = mockk<Uri>()
        val outputStream = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(uri) } returns outputStream
        coEvery { workoutSessionRepo.getSessionsInRange(any(), any()) } returns listOf(
            DomainWorkoutSession(
                id = 1, date = LocalDate.of(2024, 6, 1),
                templateId = 1, templateNameSnapshot = "Push Day",
                startTime = Instant.parse("2024-06-01T10:00:00Z"),
                endTime = Instant.parse("2024-06-01T11:30:00Z"),
                status = WorkoutStatus.COMPLETED,
                notes = null,
            ),
        )
        coEvery { workoutSessionRepo.getDetail(any()) } returns null

        val result = exporter.exportWorkouts(uri)
        val csv = outputStream.toString(Charsets.UTF_8.name())

        assertTrue(result is CsvExporter.ExportResult.Success)
        assertTrue(csv.contains("2024-06-01"))
        assertTrue(csv.contains("Push Day"))
        assertTrue(csv.contains("COMPLETED"))
    }

    @Test
    fun `export returns error when uri cannot be opened`() = runTest {
        val uri = mockk<Uri>()
        every { contentResolver.openOutputStream(uri) } returns null

        val result = exporter.exportBodyMeasurements(uri)

        assertTrue(result is CsvExporter.ExportResult.Error)
    }
}
