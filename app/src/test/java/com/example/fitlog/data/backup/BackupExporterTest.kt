package com.example.fitlog.data.backup

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
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
class BackupExporterTest {

    private lateinit var appMediaStorage: AppMediaStorage
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        appMediaStorage = AppMediaStorage(RuntimeEnvironment.getApplication())
        tempDir = createTempDir()
    }

    // ── Empty backup ─────────────────────────────────────────────────────────

    @Test
    fun `export with empty entities creates valid ZIP with manifest and empty db`() {
        val result = BackupExporter.export(
            appVersion = "1.0.0",
            dbVersion = 7,
            allEntities = emptyEntityLists(),
            mediaRecords = emptyList(),
            appMediaStorage = appMediaStorage,
            outputDir = tempDir,
        )

        assertTrue("Backup file should exist", result.backupFile.exists())
        assertTrue("Backup file should be a valid ZIP", result.backupFile.length() > 0)
        assertEquals("BackupManifest version should match", BackupManifest.CURRENT_VERSION, result.manifest.version)
        assertEquals("App version should match", "1.0.0", result.manifest.appVersion)
        assertEquals("DB version should match", 7, result.manifest.dbVersion)
        assertEquals("Total rows should be 0 for empty backup", 0, result.manifest.totalRows)
    }

    @Test
    fun `empty backup contains manifest and db json entries`() {
        val result = BackupExporter.export(
            appVersion = "1.0.0",
            dbVersion = 7,
            allEntities = emptyEntityLists(),
            mediaRecords = emptyList(),
            appMediaStorage = appMediaStorage,
            outputDir = tempDir,
        )

        ZipFile(result.backupFile).use { zip ->
            assertNotNull("manifest.json entry should exist", zip.getEntry("manifest.json"))
            assertNotNull("db.json entry should exist", zip.getEntry("db.json"))
            assertEquals("Should have exactly 2 entries", 2, zip.size())
        }
    }

    // ── Backup with data ─────────────────────────────────────────────────────

    @Test
    fun `export with entities includes all tables in dbJson`() {
        val cats = listOf(ExerciseCategoryEntity(id = 1, name = "Chest"))
        val exercises = listOf(ExerciseEntity(id = 1, name = "Bench Press", primaryMuscleGroup = "Chest"))

        val entities = EntityLists(
            exerciseCategories = cats,
            exercises = exercises,
            workoutTemplates = emptyList(),
            workoutTemplateExercises = emptyList(),
            workoutSchedules = emptyList(),
            workoutSessions = emptyList(),
            exerciseSessions = emptyList(),
            setRecords = emptyList(),
            workoutPlanOverrides = emptyList(),
            reminders = emptyList(),
            checkIns = emptyList(),
            userProfiles = emptyList(),
            bodyMeasurements = emptyList(),
            foodRecords = emptyList(),
        )

        val result = BackupExporter.export(
            appVersion = "1.0",
            dbVersion = 7,
            allEntities = entities,
            mediaRecords = emptyList(),
            appMediaStorage = appMediaStorage,
            outputDir = tempDir,
        )

        assertEquals("Total rows should be 2", 2, result.manifest.totalRows)
        assertEquals("exercise_categories should have 1 row", 1, result.manifest.rowCounts["exercise_categories"])

        ZipFile(result.backupFile).use { zip ->
            val entry = zip.getEntry("db.json")
            assertNotNull("db.json should exist", entry)
            val jsonStr = zip.getInputStream(entry).readBytes().toString(Charsets.UTF_8)
            val dbJson = JSONObject(jsonStr)

            assertTrue("db.json should have exercise_categories", dbJson.has("exercise_categories"))
            assertEquals("exercise_categories should have 1 item", 1, dbJson.getJSONArray("exercise_categories").length())
            assertTrue("db.json should have exercises", dbJson.has("exercises"))
            assertEquals("exercises should have 1 item", 1, dbJson.getJSONArray("exercises").length())
        }
    }

    @Test
    fun `export includes manifest entry only for populated tables`() {
        val sessions = listOf(WorkoutSessionEntity(
            id = 1,
            date = 20000L,
            startTime = 1000000L,
            status = "COMPLETED",
        ))
        val entities = emptyEntityLists().copy(workoutSessions = sessions)

        val result = BackupExporter.export(
            appVersion = "1.0",
            dbVersion = 7,
            allEntities = entities,
            mediaRecords = emptyList(),
            appMediaStorage = appMediaStorage,
            outputDir = tempDir,
        )

        assertEquals("Total rows should be 1", 1, result.manifest.totalRows)
        assertEquals("workout_sessions row count should be 1", 1, result.manifest.rowCounts["workout_sessions"])
        assertEquals("exercise_categories should not appear", null, result.manifest.rowCounts["exercise_categories"])
    }

    // ── Media in backup ──────────────────────────────────────────────────────

    @Test
    fun `export with media includes media prefix entries`() {
        // Create a media file on disk that can be packed
        val pending = appMediaStorage.createPendingPhoto("image/jpeg")
        pending.pendingFile.writeBytes(byteArrayOf(1, 2, 3, 4))
        val relativePath = appMediaStorage.commitPendingMedia(pending)

        try {
            val mediaRecords = listOf(MediaRecordEntity(
                id = 1,
                mediaType = com.example.fitlog.domain.media.MediaType.PHOTO,
                relativePath = relativePath,
                mimeType = "image/jpeg",
                capturedAt = System.currentTimeMillis(),
                date = 20000L,
                sizeBytes = 4,
                category = com.example.fitlog.domain.media.MediaCategory.GENERAL,
            ))

            val result = BackupExporter.export(
                appVersion = "1.0",
                dbVersion = 7,
                allEntities = emptyEntityLists(),
                mediaRecords = mediaRecords,
                appMediaStorage = appMediaStorage,
                outputDir = tempDir,
            )

            ZipFile(result.backupFile).use { zip ->
                val mediaEntryName = "media/$relativePath"
                val mediaEntry = zip.getEntry(mediaEntryName)
                assertNotNull("Media entry should exist in ZIP", mediaEntry)
                assertEquals("Media file content should match", 4L, mediaEntry.size)
            }
        } finally {
            appMediaStorage.deleteFile(relativePath)
        }
    }

    @Test
    fun `export skips missing media files gracefully`() {
        val mediaRecords = listOf(MediaRecordEntity(
            id = 1,
            mediaType = com.example.fitlog.domain.media.MediaType.PHOTO,
            relativePath = "Pictures/FitLog/nonexistent.jpg",
            mimeType = "image/jpeg",
            capturedAt = System.currentTimeMillis(),
            date = 20000L,
            sizeBytes = 0,
            category = com.example.fitlog.domain.media.MediaCategory.GENERAL,
        ))

        val result = BackupExporter.export(
            appVersion = "1.0",
            dbVersion = 7,
            allEntities = emptyEntityLists(),
            mediaRecords = mediaRecords,
            appMediaStorage = appMediaStorage,
            outputDir = tempDir,
        )

        // Should still produce a valid ZIP with 2 entries (no media/ entry)
        ZipFile(result.backupFile).use { zip ->
            assertEquals("Should have 2 entries (no media)", 2, zip.size())
        }
    }

    // ── Manifest validation ───────────────────────────────────────────────────

    @Test
    fun `manifest has all required fields`() {
        val result = BackupExporter.export(
            appVersion = "2.0.0",
            dbVersion = 7,
            allEntities = emptyEntityLists(),
            mediaRecords = emptyList(),
            appMediaStorage = appMediaStorage,
            outputDir = tempDir,
        )

        ZipFile(result.backupFile).use { zip ->
            val entry = zip.getEntry("manifest.json")
            val jsonStr = zip.getInputStream(entry).readBytes().toString(Charsets.UTF_8)
            val manifestJson = JSONObject(jsonStr)

            assertTrue("manifest should have backupVersion", manifestJson.has("backupVersion"))
            assertTrue("manifest should have appVersion", manifestJson.has("appVersion"))
            assertTrue("manifest should have dbVersion", manifestJson.has("dbVersion"))
            assertTrue("manifest should have exportedAt", manifestJson.has("exportedAt"))
            assertTrue("manifest should have totalRows", manifestJson.has("totalRows"))
            assertTrue("manifest should have rowCounts", manifestJson.has("rowCounts"))
            assertTrue("manifest should have mediaCount", manifestJson.has("mediaCount"))
            assertTrue("manifest should have dbChecksum", manifestJson.has("dbChecksum"))

            assertEquals("appVersion should match", "2.0.0", manifestJson.getString("appVersion"))
        }
    }

    @Test
    fun `manifest dbChecksum is a non-empty hex string`() {
        val result = BackupExporter.export(
            appVersion = "1.0",
            dbVersion = 7,
            allEntities = emptyEntityLists(),
            mediaRecords = emptyList(),
            appMediaStorage = appMediaStorage,
            outputDir = tempDir,
        )

        val checksum = result.manifest.dbChecksum
        assertTrue("dbChecksum should not be blank", checksum.isNotBlank())
        assertTrue("dbChecksum should be a hex string (64 chars for SHA-256)", checksum.length == 64)
        assertTrue("dbChecksum should only contain hex chars", checksum.all { it in "0123456789abcdef" })
    }

    @Test
    fun `full backup round-trip manifest consistency`() {
        val allEntities = emptyEntityLists().copy(
            exerciseCategories = listOf(ExerciseCategoryEntity(id = 1, name = "Legs")),
            exercises = listOf(ExerciseEntity(id = 1, name = "Squat", primaryMuscleGroup = "Legs")),
            workoutTemplates = listOf(WorkoutTemplateEntity(id = 1, name = "Leg Day")),
        )

        val result = BackupExporter.export(
            appVersion = "1.0.0",
            dbVersion = 7,
            allEntities = allEntities,
            mediaRecords = emptyList(),
            appMediaStorage = appMediaStorage,
            outputDir = tempDir,
        )

        assertEquals(3, result.manifest.totalRows)
        assertEquals(1, result.manifest.rowCounts["exercise_categories"])
        assertEquals(1, result.manifest.rowCounts["exercises"])
        assertEquals(1, result.manifest.rowCounts["workout_templates"])

        // Verify checksum matches actual db.json content
        ZipFile(result.backupFile).use { zip ->
            val dbEntry = zip.getEntry("db.json")
            val dbBytes = zip.getInputStream(dbEntry).readBytes()
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val expectedChecksum = digest.digest(dbBytes).joinToString("") { "%02x".format(it) }
            assertEquals("Manifest checksum should match db.json content", expectedChecksum, result.manifest.dbChecksum)
        }
    }

    // ── Output file naming ───────────────────────────────────────────────────

    @Test
    fun `export creates file with fitlog_backup prefix and timestamp`() {
        val result = BackupExporter.export(
            appVersion = "1.0",
            dbVersion = 7,
            allEntities = emptyEntityLists(),
            mediaRecords = emptyList(),
            appMediaStorage = appMediaStorage,
            outputDir = tempDir,
        )

        assertTrue("Filename should start with fitlog_backup", result.backupFile.name.startsWith("fitlog_backup"))
        assertTrue("Filename should end with .zip", result.backupFile.name.endsWith(".zip"))
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun emptyEntityLists() = EntityLists(
        exerciseCategories = emptyList(),
        exercises = emptyList(),
        workoutTemplates = emptyList(),
        workoutTemplateExercises = emptyList(),
        workoutSchedules = emptyList(),
        workoutSessions = emptyList(),
        exerciseSessions = emptyList(),
        setRecords = emptyList(),
        workoutPlanOverrides = emptyList(),
        reminders = emptyList(),
        checkIns = emptyList(),
        userProfiles = emptyList(),
        bodyMeasurements = emptyList(),
        foodRecords = emptyList(),
    )
}
