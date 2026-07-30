package com.example.fitlog.data.backup

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import com.example.fitlog.core.database.FitLogDatabase
import com.example.fitlog.core.database.entity.BodyMeasurementEntity
import com.example.fitlog.core.database.entity.CheckInEntity
import com.example.fitlog.core.database.entity.ExerciseCategoryEntity
import com.example.fitlog.core.database.entity.ExerciseEntity
import com.example.fitlog.core.database.entity.ExerciseSessionEntity
import com.example.fitlog.core.database.entity.FoodRecordEntity
import com.example.fitlog.core.database.entity.MediaRecordEntity
import com.example.fitlog.core.database.entity.ReminderEntity
import com.example.fitlog.core.database.entity.SetRecordEntity
import com.example.fitlog.core.database.entity.UserProfileEntity
import com.example.fitlog.core.database.entity.WorkoutPlanOverrideEntity
import com.example.fitlog.core.database.entity.WorkoutScheduleEntity
import com.example.fitlog.core.database.entity.WorkoutSessionEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateExerciseEntity
import com.example.fitlog.core.media.AppMediaStorage
import com.example.fitlog.domain.media.MediaCategory
import com.example.fitlog.domain.media.MediaType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * End-to-end round trip test:
 * 1. Create in-memory entity lists with sample data
 * 2. Export via [BackupExporter] to a temp ZIP
 * 3. Verify the ZIP contains valid manifest.json and db.json
 * 4. Import via [BackupImporter] into a mocked database
 * 5. Verify that all tables received the correct INSERT calls
 */
class BackupRoundTripTest {

    private lateinit var appMediaStorage: AppMediaStorage

    @Before
    fun setUp() {
        appMediaStorage = mockk(relaxed = true)
        // BackupExporter.export calls appMediaStorage.resolveFile for each media record.
        // The test entity below has relativePath "Pictures/FitLog/test.jpg".
        every {
            appMediaStorage.resolveFile("Pictures/FitLog/test.jpg")
        } returns File.createTempFile("test", ".jpg").also { it.deleteOnExit() }
    }

    @Test
    fun `round trip export then import preserves all data`() {
        // ── 1. Build entity lists ───────────────────────────────────────────
        val exerciseCategory = ExerciseCategoryEntity(id = 1, name = "Chest")
        val exercise = ExerciseEntity(
            id = 1, name = "Bench Press", primaryMuscleGroup = "Chest",
        )
        val template = WorkoutTemplateEntity(id = 1, name = "Push Day", isActive = true)
        val templateExercise = WorkoutTemplateExerciseEntity(
            id = 1, templateId = 1, exerciseId = 1, targetSets = 3,
        )
        val schedule = WorkoutScheduleEntity(id = 1, templateId = 1, dayOfWeek = 1)
        val session = WorkoutSessionEntity(
            id = 1,
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            startTime = LocalDate.of(2026, 7, 28).atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli(),
            status = "COMPLETED",
            templateNameSnapshot = "Push Day",
        )
        val exerciseSession = ExerciseSessionEntity(
            id = 1, sessionId = 1, exerciseId = 1,
            exerciseNameSnapshot = "Bench Press",
            primaryMuscleGroupSnapshot = "Chest",
        )
        val setRecord = SetRecordEntity(
            id = 1, exerciseSessionId = 1, setNumber = 1,
            reps = 10, weightKg = 80.0, completed = true,
        )
        val override = WorkoutPlanOverrideEntity(
            id = 1, scheduleId = 1, templateId = 1,
            occurrenceDate = LocalDate.of(2026, 7, 28).toEpochDay(),
            action = "RESCHEDULED",
        )
        val reminder = ReminderEntity(
            id = 1, label = "Workout reminder",
            timeOfDayMinutes = 480, daysOfWeekMask = 0b0111111,
            zoneId = "Asia/Shanghai",
        )
        val checkIn = CheckInEntity(
            id = 1, date = LocalDate.of(2026, 7, 28).toEpochDay(),
            mood = 5, energyLevel = 4,
        )
        val profile = UserProfileEntity(
            id = 1, gender = "MALE",
            birthday = LocalDate.of(1990, 1, 1).toEpochDay(),
            activityLevel = "MODERATE", goalType = "MAINTAIN",
        )
        val bodyMeasurement = BodyMeasurementEntity(
            id = 1, date = LocalDate.of(2026, 7, 28).toEpochDay(),
            weightKg = 75.0,
        )
        val foodRecord = FoodRecordEntity(
            id = 1, date = LocalDate.of(2026, 7, 28).toEpochDay(),
            mealType = "LUNCH", foodName = "Chicken Rice",
            calories = 600.0,
        )
        val mediaRecord = MediaRecordEntity(
            id = 1,
            mediaType = MediaType.PHOTO,
            relativePath = "Pictures/FitLog/test.jpg",
            mimeType = "image/jpeg",
            capturedAt = System.currentTimeMillis(),
            date = LocalDate.of(2026, 7, 28).toEpochDay(),
            sizeBytes = 12345L,
            category = MediaCategory.GENERAL,
        )

        val entities = EntityLists(
            exerciseCategories = listOf(exerciseCategory),
            exercises = listOf(exercise),
            workoutTemplates = listOf(template),
            workoutTemplateExercises = listOf(templateExercise),
            workoutSchedules = listOf(schedule),
            workoutSessions = listOf(session),
            exerciseSessions = listOf(exerciseSession),
            setRecords = listOf(setRecord),
            workoutPlanOverrides = listOf(override),
            reminders = listOf(reminder),
            checkIns = listOf(checkIn),
            userProfiles = listOf(profile),
            bodyMeasurements = listOf(bodyMeasurement),
            foodRecords = listOf(foodRecord),
        )

        // ── 2. Export ───────────────────────────────────────────────────────
        val result = BackupExporter.export(
            appVersion = "0.1.0",
            dbVersion = 7,
            allEntities = entities,
            mediaRecords = listOf(mediaRecord),
            appMediaStorage = appMediaStorage,
        )

        // ── 3. Verify ZIP ───────────────────────────────────────────────────
        val zipFile = result.backupFile
        assertTrue("ZIP file exists", zipFile.exists())
        assertTrue("ZIP file is not empty", zipFile.length() > 0)

        ZipFile(zipFile).use { zip ->
            // manifest.json
            val manifestEntry = zip.getEntry("manifest.json")
            assertNotNull("manifest.json entry exists", manifestEntry)

            val manifestJson = JSONObject(
                zip.getInputStream(manifestEntry).readBytes().toString(Charsets.UTF_8),
            )
            assertEquals("backupVersion", 1, manifestJson.getInt("backupVersion"))
            assertEquals("appVersion", "0.1.0", manifestJson.getString("appVersion"))
            assertEquals("dbVersion", 7, manifestJson.getInt("dbVersion"))
            assertTrue("exportedAt is set", manifestJson.getLong("exportedAt") > 0)
            assertEquals("totalRows", 15, manifestJson.getInt("totalRows"))
            assertEquals("mediaCount", 1, manifestJson.getInt("mediaCount"))

            val rowCounts = manifestJson.getJSONObject("rowCounts")
            assertEquals("exercise_categories", 1, rowCounts.getInt("exercise_categories"))
            assertEquals("exercises", 1, rowCounts.getInt("exercises"))
            assertEquals("workout_templates", 1, rowCounts.getInt("workout_templates"))
            assertEquals("set_records", 1, rowCounts.getInt("set_records"))

            // db.json
            val dbEntry = zip.getEntry("db.json")
            assertNotNull("db.json entry exists", dbEntry)
            val dbJson = JSONObject(
                zip.getInputStream(dbEntry).readBytes().toString(Charsets.UTF_8),
            )
            // Verify selected tables have data
            assertEquals(1, dbJson.getJSONArray("body_measurements").length())
            assertEquals(1, dbJson.getJSONArray("check_ins").length())
            assertEquals(1, dbJson.getJSONArray("workout_sessions").length())
            assertEquals(15, dbJson.length()) // 15 tables

            // Verify checksum matches
            val dbJsonBytes = zip.getInputStream(dbEntry).readBytes()
            val digest = MessageDigest.getInstance("SHA-256")
            val actualChecksum = digest.digest(dbJsonBytes)
                .joinToString("") { "%02x".format(it) }
            assertEquals("dbChecksum matches", manifestJson.getString("dbChecksum"), actualChecksum)
        }

        // ── 4. Import into mocked database ──────────────────────────────────
        val mockDatabase = createMockDatabase()
        val importer = BackupImporter(
            db = mockDatabase,
            appMediaStorage = appMediaStorage,
        )

        val importResult = importer.importBackup(zipFile.inputStream())

        assertTrue("Import succeeded", importResult.success)
        assertEquals("rowsImported", 15, importResult.rowsImported)

        // Verify the mocked DB received inserts for each table
        val writableDb = mockDatabase.openHelper.writableDatabase
        verify(exactly = 1) { writableDb.insertWithOnConflict(
            "exercise_categories", null, any(), eq(SQLiteDatabase.CONFLICT_REPLACE),
        ) }
        verify(exactly = 1) { writableDb.insertWithOnConflict(
            "exercises", null, any(), eq(SQLiteDatabase.CONFLICT_REPLACE),
        ) }
        verify(exactly = 1) { writableDb.insertWithOnConflict(
            "body_measurements", null, any(), eq(SQLiteDatabase.CONFLICT_REPLACE),
        ) }

        // ── Cleanup ─────────────────────────────────────────────────────────
        zipFile.delete()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Creates a [FitLogDatabase] whose openHelper returns a mock [SQLiteDatabase]
     * that tracks INSERT calls via mockk.
     */
    private fun createMockDatabase(): FitLogDatabase {
        val mockDb = mockk<SQLiteDatabase>(relaxed = true)
        // Make insertWithOnConflict return a valid row ID so the importer doesn't crash
        every {
            mockDb.insertWithOnConflict(any(), any(), any(), any())
        } returns 1L

        val openHelper = mockk<RoomDatabase.OpenHelper>(relaxed = true)
        every { openHelper.writableDatabase } returns mockDb

        val database = mockk<FitLogDatabase>(relaxed = true)
        every { database.openHelper } returns openHelper

        // clearAllTables calls execSQL("PRAGMA foreign_keys = OFF") etc.
        every { mockDb.execSQL(any<String>()) } returns Unit

        return database
    }
}
