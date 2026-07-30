package com.example.fitlog.feature.camera

import android.app.Application
import android.content.pm.PackageManager
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.media.AppMediaStorage
import com.example.fitlog.data.repository.MediaRecord
import com.example.fitlog.data.repository.MediaRepository
import com.example.fitlog.domain.media.MediaCategory
import com.example.fitlog.domain.media.MediaType
import com.example.fitlog.domain.media.ProgressPose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ── Data classes ─────────────────────────────────────────────────────────────────

/** Context for a media capture — which category and associated entity IDs. */
data class CaptureContext(
    val category: MediaCategory = MediaCategory.GENERAL,
    val workoutSessionId: Long? = null,
    val bodyMeasurementId: Long? = null,
    val checkInId: Long? = null,
    val exerciseSessionId: Long? = null,
    val foodRecordId: Long? = null,
    val poseTag: ProgressPose? = null,
)

/** Permissions state for camera and audio. */
enum class CameraPermissionState {
    UNKNOWN,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
}

enum class CameraMode { PHOTO, VIDEO }
enum class FlashMode { OFF, ON, AUTO }

data class CameraUiState(
    val permissionState: CameraPermissionState = CameraPermissionState.UNKNOWN,
    val audioPermissionState: CameraPermissionState = CameraPermissionState.UNKNOWN,
    val cameraMode: CameraMode = CameraMode.PHOTO,
    val flashMode: FlashMode = FlashMode.OFF,
    val isTorchOn: Boolean = false,
    val isFrontCamera: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val minZoom: Float = 1.0f,
    val maxZoom: Float = 10.0f,
    val exposureCompensation: Int = 0,
    val minExposure: Int = -10,
    val maxExposure: Int = 10,
    val timerSeconds: Int = 0,
    val showGrid: Boolean = false,
    val isRecording: Boolean = false,
    val isMicEnabled: Boolean = true,
    val recordDurationMs: Long = 0L,
    val capturedPhotoUri: String? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showPreview: Boolean = false,
    val isCameraReady: Boolean = false,
    val captureContext: CaptureContext = CaptureContext(),
    val savedMediaId: Long? = null,
    val countdownValue: Int = 0,
)

@HiltViewModel
class FitLogCameraViewModel @Inject constructor(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val mediaStorage: AppMediaStorage,
    private val mediaRepository: MediaRepository,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var recordDurationJob: Job? = null
    private var countdownJob: Job? = null
    private var currentRecording: Recording? = null

    init {
        // Parse capture context from route query parameters
        val categoryStr = savedStateHandle.get<String>("category")
        val category = try {
            categoryStr?.let { MediaCategory.valueOf(it) } ?: MediaCategory.GENERAL
        } catch (_: IllegalArgumentException) {
            MediaCategory.GENERAL
        }
        val workoutSessionId = savedStateHandle.get<Long>("workoutSessionId")
        val bodyMeasurementId = savedStateHandle.get<Long>("bodyMeasurementId")
        val checkInId = savedStateHandle.get<Long>("checkInId")

        _uiState.value = _uiState.value.copy(
            captureContext = CaptureContext(
                category = category,
                workoutSessionId = workoutSessionId,
                bodyMeasurementId = bodyMeasurementId,
                checkInId = checkInId,
            ),
        )

        checkPermissions()
    }

    // ── Permissions ──────────────────────────────────────────────────────────────

    fun checkPermissions() {
        val context = getApplication<Application>()
        val cameraGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        _uiState.value = _uiState.value.copy(
            permissionState = if (cameraGranted) CameraPermissionState.GRANTED
            else CameraPermissionState.DENIED,
            audioPermissionState = if (audioGranted) CameraPermissionState.GRANTED
            else CameraPermissionState.DENIED,
        )
    }

    fun setPermissionGranted() {
        _uiState.value = _uiState.value.copy(permissionState = CameraPermissionState.GRANTED)
    }

    fun setPermissionDenied(permanently: Boolean) {
        _uiState.value = _uiState.value.copy(
            permissionState = if (permanently) CameraPermissionState.PERMANENTLY_DENIED
            else CameraPermissionState.DENIED
        )
    }

    fun setAudioPermissionGranted() {
        _uiState.value = _uiState.value.copy(audioPermissionState = CameraPermissionState.GRANTED)
    }

    // ── Camera state ─────────────────────────────────────────────────────────────

    fun setCameraReady() {
        _uiState.value = _uiState.value.copy(isCameraReady = true)
    }

    fun setCameraMode(mode: CameraMode) {
        _uiState.value = _uiState.value.copy(cameraMode = mode, error = null)
    }

    fun toggleCameraFacing() {
        _uiState.value = _uiState.value.copy(
            isFrontCamera = !_uiState.value.isFrontCamera,
        )
    }

    fun setZoom(ratio: Float) {
        _uiState.value = _uiState.value.copy(zoomRatio = ratio.coerceIn(
            _uiState.value.minZoom, _uiState.value.maxZoom
        ))
    }

    fun setZoomRange(min: Float, max: Float) {
        _uiState.value = _uiState.value.copy(minZoom = min, maxZoom = max)
    }

    fun setExposureCompensation(value: Int) {
        _uiState.value = _uiState.value.copy(
            exposureCompensation = value.coerceIn(
                _uiState.value.minExposure, _uiState.value.maxExposure
            )
        )
    }

    fun setExposureRange(min: Int, max: Int) {
        _uiState.value = _uiState.value.copy(minExposure = min, maxExposure = max)
    }

    fun cycleFlashMode() {
        val current = _uiState.value.flashMode
        val next = when (current) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
        _uiState.value = _uiState.value.copy(
            flashMode = next,
            isTorchOn = next == FlashMode.ON,
        )
    }

    fun setTorch(on: Boolean) {
        _uiState.value = _uiState.value.copy(isTorchOn = on)
    }

    fun toggleGrid() {
        _uiState.value = _uiState.value.copy(showGrid = !_uiState.value.showGrid)
    }

    fun cycleTimer() {
        val current = _uiState.value.timerSeconds
        val next = when (current) {
            0 -> 3
            3 -> 10
            10 -> 0
            else -> 0
        }
        _uiState.value = _uiState.value.copy(timerSeconds = next)
    }

    fun setMicEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isMicEnabled = enabled)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ── Timer countdown ──────────────────────────────────────────────────────────

    fun startCountdown(onFinished: () -> Unit) {
        val seconds = _uiState.value.timerSeconds
        if (seconds <= 0) {
            onFinished()
            return
        }
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(countdownValue = seconds)
            for (i in seconds downTo 1) {
                _uiState.value = _uiState.value.copy(countdownValue = i)
                delay(1000L)
            }
            _uiState.value = _uiState.value.copy(countdownValue = 0)
            onFinished()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _uiState.value = _uiState.value.copy(countdownValue = 0)
    }

    // ── Photo capture ────────────────────────────────────────────────────────────

    fun setCapturedPhoto(relativePath: String) {
        _uiState.value = _uiState.value.copy(
            capturedPhotoUri = relativePath,
            showPreview = true,
        )
    }

    fun savePhoto(uri: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                val now = System.currentTimeMillis()
                val ctx = _uiState.value.captureContext
                val record = mediaRepository.save(
                    MediaRecord(
                        mediaType = MediaType.PHOTO,
                        relativePath = uri,
                        mimeType = "image/jpeg",
                        capturedAt = now,
                        date = LocalDate.now().toEpochDay(),
                        sizeBytes = mediaStorage.calculateSize(uri),
                        category = ctx.category,
                        workoutSessionId = ctx.workoutSessionId,
                        bodyMeasurementId = ctx.bodyMeasurementId,
                        checkInId = ctx.checkInId,
                        exerciseSessionId = ctx.exerciseSessionId,
                        foodRecordId = ctx.foodRecordId,
                        poseTag = ctx.poseTag,
                    )
                )
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    showPreview = false,
                    capturedPhotoUri = null,
                    savedMediaId = record.id,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save photo",
                )
            }
        }
    }

    fun retakePhoto() {
        _uiState.value = _uiState.value.copy(
            showPreview = false,
            capturedPhotoUri = null,
        )
    }

    fun discardPreview() {
        val uri = _uiState.value.capturedPhotoUri
        if (uri != null) {
            try { mediaStorage.deleteFile(uri) } catch (_: Exception) { }
        }
        _uiState.value = _uiState.value.copy(
            showPreview = false,
            capturedPhotoUri = null,
        )
    }

    // ── Video recording ──────────────────────────────────────────────────────────

    fun onVideoRecordEvent(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Start -> {
                _uiState.value = _uiState.value.copy(isRecording = true, recordDurationMs = 0L)
                recordDurationJob?.cancel()
                recordDurationJob = viewModelScope.launch {
                    while (true) {
                        delay(100L)
                        _uiState.value = _uiState.value.copy(
                            recordDurationMs = _uiState.value.recordDurationMs + 100L
                        )
                    }
                }
            }
            is VideoRecordEvent.Finalize -> {
                recordDurationJob?.cancel()
                currentRecording = null
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    recordDurationMs = event.outputResults.durationMillis ?: _uiState.value.recordDurationMs,
                )
                viewModelScope.launch {
                    try {
                        val ctx = _uiState.value.captureContext
                        val now = System.currentTimeMillis()
                        val uri = event.outputResults.outputUri
                        // For FileOutputOptions, the URI is a file URI
                        val filePath = uri.path ?: return@launch
                        val record = mediaRepository.save(
                            MediaRecord(
                                mediaType = MediaType.VIDEO,
                                relativePath = filePath,
                                mimeType = "video/mp4",
                                capturedAt = now,
                                date = LocalDate.now().toEpochDay(),
                                sizeBytes = 0L,
                                durationMillis = _uiState.value.recordDurationMs,
                                category = ctx.category,
                                workoutSessionId = ctx.workoutSessionId,
                                bodyMeasurementId = ctx.bodyMeasurementId,
                                checkInId = ctx.checkInId,
                                exerciseSessionId = ctx.exerciseSessionId,
                                foodRecordId = ctx.foodRecordId,
                                poseTag = ctx.poseTag,
                            )
                        )
                        _uiState.value = _uiState.value.copy(savedMediaId = record.id)
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            error = e.message ?: "Failed to save video",
                        )
                    }
                }
            }
            is VideoRecordEvent.Status -> {
                _uiState.value = _uiState.value.copy(
                    recordDurationMs = event.recordingStats.recordedDurationNanos / 1_000_000
                )
            }
            else -> {}
        }
    }

    fun setCurrentRecording(recording: Recording?) {
        currentRecording = recording
    }

    fun stopRecording() {
        currentRecording?.stop()
    }

    // ── Error handling ──────────────────────────────────────────────────────────

    fun onCaptureError(exception: Exception) {
        _uiState.value = _uiState.value.copy(
            error = exception.message ?: "Capture failed",
        )
    }

    fun clearSavedMediaId() {
        _uiState.value = _uiState.value.copy(savedMediaId = null)
    }

    fun setCaptureContextCategory(category: MediaCategory) {
        _uiState.value = _uiState.value.copy(
            captureContext = _uiState.value.captureContext.copy(category = category)
        )
    }
}
