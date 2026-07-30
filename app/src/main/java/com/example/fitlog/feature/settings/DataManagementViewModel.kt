package com.example.fitlog.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.fitlog.core.media.MediaCleanupManager
import com.example.fitlog.data.backup.BackupExporter
import com.example.fitlog.data.backup.BackupImporter
import com.example.fitlog.data.backup.EntityLists
import com.example.fitlog.data.backup.ImportResult
import com.example.fitlog.data.export.ExportManager
import com.example.fitlog.data.export.exportBodyMeasurements
import com.example.fitlog.data.export.exportCheckIns
import com.example.fitlog.data.export.exportNutrition
import com.example.fitlog.data.export.exportWorkouts
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.time.ZoneId
import javax.inject.Inject

/**
 * UI state for the Data Management screen.
 *
 * @property isLoading Whether a long-running operation is in progress.
 * @property storageStats Current media storage statistics (nullable while loading).
 * @property exportResult Message from the last export operation (null if idle).
 * @property importResult Result from the last backup import (null if idle).
 * @property backupExportResult Result from the last backup export (null if idle).
 * @property cleanupResult Message from the last cleanup operation (null if idle).
 * @property deleteAllResult Message from the last delete-all operation (null if idle).
 * @property error Error message if the last operation failed.
 */
data class DataManagementUiState(
    val isLoading: Boolean = false,
    val storageStats: MediaCleanupManager.StorageStats? = null,
    val exportResult: String? = null,
    val importResult: ImportResult? = null,
    val backupExportResult: String? = null,
    val cleanupResult: String? = null,
    val deleteAllResult: String? = null,
    val error: String? = null,
)

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: FitLogDatabase,
    private val exportManager: ExportManager,
    private val mediaCleanupManager: MediaCleanupManager,
    private val appMediaStorage: AppMediaStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState: StateFlow<DataManagementUiState> = _uiState.asStateFlow()

    // BackupExporter and BackupImporter are created as-needed.

    init {
        loadStorageStats()
    }

    // ── Storage stats ──────────────────────────────────────────────────────────

    fun loadStorageStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val stats = mediaCleanupManager.getStorageStats()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    storageStats = stats,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load storage stats: ${e.message}",
                )
            }
        }
    }

    // ── Media cleanup ──────────────────────────────────────────────────────────

    fun runCleanup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, cleanupResult = null, error = null)
            try {
                val result = mediaCleanupManager.cleanupAll()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    cleanupResult = "Cleaned: ${result.recordsRemoved} records, " +
                        "${result.filesRemoved} files, ${result.pendingRemoved} temp files",
                )
                // Refresh stats
                loadStorageStats()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Cleanup failed: ${e.message}",
                )
            }
        }
    }

    // ── CSV export ─────────────────────────────────────────────────────────────

    /**
     * Exports workout sessions as CSV to the given SAF URI.
     */
    fun exportWorkoutsToUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, exportResult = null, error = null)
            try {
                val sessions = db.workoutSessionDao().getAll()
                val exerciseSessions = db.exerciseSessionDao().getAll()
                val setRecords = db.setRecordDao().getAll()
                val csv = exportWorkouts(sessions, exerciseSessions, setRecords, ZoneId.systemDefault())
                exportManager.writeToUriOrThrow(uri, csv)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    exportResult = "Workouts exported successfully",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Export workouts failed: ${e.message}",
                )
            }
        }
    }

    /**
     * Exports body measurements as CSV to the given SAF URI.
     */
    fun exportBodyMeasurementsToUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, exportResult = null, error = null)
            try {
                val measurements = db.bodyMeasurementDao().getAll()
                val csv = exportBodyMeasurements(measurements)
                exportManager.writeToUriOrThrow(uri, csv)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    exportResult = "Body measurements exported successfully",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Export body measurements failed: ${e.message}",
                )
            }
        }
    }

    /**
     * Exports nutrition records as CSV to the given SAF URI.
     */
    fun exportNutritionToUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, exportResult = null, error = null)
            try {
                val records = db.foodRecordDao().getAll()
                val csv = exportNutrition(records)
                exportManager.writeToUriOrThrow(uri, csv)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    exportResult = "Nutrition records exported successfully",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Export nutrition failed: ${e.message}",
                )
            }
        }
    }

    /**
     * Exports check-in records as CSV to the given SAF URI.
     */
    fun exportCheckInsToUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, exportResult = null, error = null)
            try {
                val checkIns = db.checkInDao().getAll()
                val csv = exportCheckIns(checkIns)
                exportManager.writeToUriOrThrow(uri, csv)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    exportResult = "Check-ins exported successfully",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Export check-ins failed: ${e.message}",
                )
            }
        }
    }

    // ── Backup export ──────────────────────────────────────────────────────────

    /**
     * Exports a full backup to the given SAF URI.
     *
     * The backup is written to a temporary file first (via [BackupExporter]),
     * then copied to the SAF URI.
     */
    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                backupExportResult = null,
                error = null,
            )
            try {
                val exporter = BackupExporter
                val allEntities = loadAllEntities()
                val mediaRecords = db.mediaRecordDao().getAllRecords()

                val exportResult = exporter.export(
                    appVersion = "${context.packageManager.getPackageInfo(context.packageName, 0).versionName}",
                    dbVersion = 7,
                    allEntities = allEntities,
                    mediaRecords = mediaRecords,
                    appMediaStorage = appMediaStorage,
                )

                // Read the temp file and write to SAF URI
                val backupBytes = exportResult.backupFile.readBytes()
                exportManager.writeToUriOrThrow(uri, backupBytes)

                // Clean up temp file
                exportResult.backupFile.delete()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    backupExportResult = "Backup exported successfully " +
                        "(${exportResult.manifest.totalRows} rows, " +
                        "${exportResult.manifest.mediaCount} media files)",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Backup export failed: ${e.message}",
                )
            }
        }
    }

    // ── Backup import ──────────────────────────────────────────────────────────

    /**
     * Imports a backup from the given [inputStream].
     */
    fun importBackup(inputStream: InputStream) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                importResult = null,
                error = null,
            )
            try {
                val importer = BackupImporter(context, db, appMediaStorage)
                val result = importer.importBackup(inputStream)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    importResult = result,
                )
                // Refresh stats
                loadStorageStats()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    importResult = ImportResult.Error(
                        message = e.message ?: "Import failed",
                        cause = e,
                    ),
                )
            }
        }
    }

    // ── Delete all data ────────────────────────────────────────────────────────

    /**
     * Deletes ALL data from all tables, including media files on disk.
     * This is irreversible — call only after user confirmation.
     */
    fun deleteAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                deleteAllResult = null,
                error = null,
            )
            try {
                // Delete media files first
                val allMedia = db.mediaRecordDao().getAllRecords()
                for (media in allMedia) {
                    try {
                        appMediaStorage.deleteFile(media.relativePath)
                    } catch (_: Exception) {
                        // Continue deleting other files
                    }
                }

                // Delete database rows in FK-safe order via raw SQL
                val writableDb = db.openHelper.writableDatabase
                writableDb.beginTransaction()
                try {
                    writableDb.execSQL("PRAGMA foreign_keys = OFF")
                    writableDb.delete("media_records", null, null)
                    writableDb.delete("food_records", null, null)
                    writableDb.delete("body_measurements", null, null)
                    writableDb.delete("user_profiles", null, null)
                    writableDb.delete("check_ins", null, null)
                    writableDb.delete("reminders", null, null)
                    writableDb.delete("workout_plan_overrides", null, null)
                    writableDb.delete("set_records", null, null)
                    writableDb.delete("exercise_sessions", null, null)
                    writableDb.delete("workout_sessions", null, null)
                    writableDb.delete("workout_schedules", null, null)
                    writableDb.delete("workout_template_exercises", null, null)
                    writableDb.delete("workout_templates", null, null)
                    writableDb.delete("exercises", null, null)
                    writableDb.delete("exercise_categories", null, null)
                    writableDb.execSQL("PRAGMA foreign_keys = ON")
                    writableDb.setTransactionSuccessful()
                } finally {
                    writableDb.endTransaction()
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    deleteAllResult = "All data has been deleted",
                )
                // Refresh stats
                loadStorageStats()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Delete all data failed: ${e.message}",
                )
            }
        }
    }

    // ── Dismiss result messages ────────────────────────────────────────────────

    fun dismissExportResult() {
        _uiState.value = _uiState.value.copy(exportResult = null)
    }

    fun dismissBackupExportResult() {
        _uiState.value = _uiState.value.copy(backupExportResult = null)
    }

    fun dismissImportResult() {
        _uiState.value = _uiState.value.copy(importResult = null)
    }

    fun dismissCleanupResult() {
        _uiState.value = _uiState.value.copy(cleanupResult = null)
    }

    fun dismissDeleteAllResult() {
        _uiState.value = _uiState.value.copy(deleteAllResult = null)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private suspend fun loadAllEntities(): EntityLists {
        return EntityLists(
            exerciseCategories = db.exerciseCategoryDao().getAll().first(),
            exercises = db.exerciseDao().getAll(),
            workoutTemplates = db.workoutTemplateDao().getAll(),
            workoutTemplateExercises = db.workoutTemplateDao().getAllTemplateExercises(),
            workoutSchedules = db.workoutScheduleDao().getAll(),
            workoutSessions = db.workoutSessionDao().getAll(),
            exerciseSessions = db.exerciseSessionDao().getAll(),
            setRecords = db.setRecordDao().getAll(),
            workoutPlanOverrides = db.workoutPlanOverrideDao().getAll(),
            reminders = db.reminderDao().getAll(),
            checkIns = db.checkInDao().getAll(),
            userProfiles = db.userProfileDao().getAll(),
            bodyMeasurements = db.bodyMeasurementDao().getAll(),
            foodRecords = db.foodRecordDao().getAll(),
        )
    }
}
