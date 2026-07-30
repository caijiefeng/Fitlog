package com.example.fitlog.feature.camera

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.media.AppMediaStorage
import com.example.fitlog.domain.media.MediaCategory
import com.example.fitlog.data.repository.MediaRecord
import com.example.fitlog.data.repository.MediaRepository
import com.example.fitlog.domain.media.MediaType
import com.example.fitlog.domain.media.ProgressPose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Flash modes for the camera UI.
 */
enum class FlashMode {
    OFF,
    ON,
}

/**
 * Timer countdown options in seconds. 0 means no timer.
 */
enum class TimerOption(val seconds: Long) {
    OFF(0),
    S_3(3),
    S_10(10),
}

/**
 * UI state for the camera screen.
 */
data class CameraUiState(
    val isLoading: Boolean = false,
    val photoUri: Uri? = null,
    val capturedFilePending: AppMediaStorage.PendingMedia? = null,
    val flashMode: FlashMode = FlashMode.OFF,
    val timerOption: TimerOption = TimerOption.OFF,
    val zoom: Float = 0f, // linear zoom 0..1
    val exposure: Int = 0,
    val isFrontCamera: Boolean = false,
    val showGrid: Boolean = false,
    val isCapturing: Boolean = false,
    val pictureTaken: Boolean = false,
    val error: String? = null,
    val category: MediaCategory = MediaCategory.GENERAL,
    val poseTag: ProgressPose? = null,
    val workoutSessionId: Long? = null,
    val bodyMeasurementId: Long? = null,
    val checkInId: Long? = null,
)

@HiltViewModel
class FitLogCameraViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val mediaStorage: AppMediaStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    /** The CameraEngine is set externally by the screen (Hilt cannot inject it). */
    var cameraEngine: CameraEngine? = null

    // Parse navigation arguments from SavedStateHandle
    private val categoryParam: String? = savedStateHandle.get<String>("category")
    private val workoutSessionIdParam: Long? = savedStateHandle.get<Long>("workoutSessionId")?.let {
        if (it == -1L) null else it
    }
    private val bodyMeasurementIdParam: Long? = savedStateHandle.get<Long>("bodyMeasurementId")?.let {
        if (it == -1L) null else it
    }
    private val checkInIdParam: Long? = savedStateHandle.get<Long>("checkInId")?.let {
        if (it == -1L) null else it
    }

    init {
        val category = categoryParam?.let { safeCategory ->
            try {
                MediaCategory.valueOf(safeCategory)
            } catch (_: IllegalArgumentException) {
                MediaCategory.GENERAL
            }
        } ?: MediaCategory.GENERAL

        _uiState.value = _uiState.value.copy(
            category = category,
            workoutSessionId = workoutSessionIdParam,
            bodyMeasurementId = bodyMeasurementIdParam,
            checkInId = checkInIdParam,
        )
    }

    // ── Camera engine delegation ──────────────────────────────────────────────

    fun bindPreview(previewView: androidx.camera.view.PreviewView) {
        viewModelScope.launch {
            try {
                cameraEngine?.bindPreview(previewView)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to bind camera preview",
                )
            }
        }
    }

    fun releaseCamera() {
        cameraEngine?.release()
    }

    // ── Flash ─────────────────────────────────────────────────────────────────

    fun toggleFlash() {
        val newMode = if (_uiState.value.flashMode == FlashMode.OFF) {
            cameraEngine?.toggleFlash()
            FlashMode.ON
        } else {
            cameraEngine?.toggleFlash()
            FlashMode.OFF
        }
        _uiState.value = _uiState.value.copy(flashMode = newMode)
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    fun cycleTimer() {
        val current = _uiState.value.timerOption
        val next = when (current) {
            TimerOption.OFF -> TimerOption.S_3
            TimerOption.S_3 -> TimerOption.S_10
            TimerOption.S_10 -> TimerOption.OFF
        }
        _uiState.value = _uiState.value.copy(timerOption = next)
    }

    // ── Grid ──────────────────────────────────────────────────────────────────

    fun toggleGrid() {
        _uiState.value = _uiState.value.copy(showGrid = !_uiState.value.showGrid)
    }

    // ── Lens ──────────────────────────────────────────────────────────────────

    fun switchLens() {
        cameraEngine?.switchLens()
        _uiState.value = _uiState.value.copy(
            isFrontCamera = !_uiState.value.isFrontCamera,
        )
    }

    // ── Zoom ──────────────────────────────────────────────────────────────────

    fun setZoom(ratio: Float) {
        cameraEngine?.setZoom(ratio)
        _uiState.value = _uiState.value.copy(zoom = ratio)
    }

    // ── Exposure ──────────────────────────────────────────────────────────────

    fun setExposure(index: Int) {
        cameraEngine?.setExposure(index)
        _uiState.value = _uiState.value.copy(exposure = index)
    }

    // ── Focus ─────────────────────────────────────────────────────────────────

    fun focus(x: Float, y: Float) {
        cameraEngine?.focus(x, y)
    }

    // ── Photo capture ─────────────────────────────────────────────────────────

    /**
     * Initiates photo capture. If a timer is active, the caller must wait
     * for the countdown before calling [capturePhotoNow].
     */
    fun capturePhoto() {
        val state = _uiState.value
        if (state.isCapturing || state.pictureTaken) return

        if (state.timerOption.seconds > 0) {
            startTimerAndCapture(state.timerOption.seconds)
        } else {
            capturePhotoNow()
        }
    }

    private fun startTimerAndCapture(seconds: Long) {
        _uiState.value = _uiState.value.copy(isCapturing = true)
        viewModelScope.launch {
            // Simple countdown: wait for the specified seconds
            kotlinx.coroutines.delay(seconds * 1000)
            capturePhotoNow()
        }
    }

    private fun capturePhotoNow() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(isCapturing = true, error = null)

        viewModelScope.launch {
            try {
                // Create pending file
                val pending = mediaStorage.createPendingPhoto("image/jpeg")

                // Capture
                val result = cameraEngine?.capturePhoto(pending.pendingFile)

                if (result != null) {
                    // Commit the pending file
                    val relativePath = mediaStorage.commitPendingMedia(pending)
                    val uri = Uri.fromFile(pending.finalFile)

                    _uiState.value = _uiState.value.copy(
                        isCapturing = false,
                        pictureTaken = true,
                        photoUri = uri,
                        capturedFilePending = null,
                    )

                    // Save to repository
                    val record = MediaRecord(
                        mediaType = MediaType.PHOTO,
                        relativePath = relativePath,
                        mimeType = "image/jpeg",
                        capturedAt = System.currentTimeMillis(),
                        date = java.time.LocalDate.now().toEpochDay(),
                        sizeBytes = mediaStorage.calculateSize(relativePath),
                        category = currentState.category,
                        poseTag = currentState.poseTag,
                        workoutSessionId = currentState.workoutSessionId,
                        bodyMeasurementId = currentState.bodyMeasurementId,
                        checkInId = currentState.checkInId,
                    )
                    mediaRepository.save(record)
                } else {
                    // Capture failed, discard pending file
                    mediaStorage.discardPendingMedia(pending)
                    _uiState.value = _uiState.value.copy(
                        isCapturing = false,
                        error = "Capture failed",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCapturing = false,
                    error = e.message ?: "Capture failed",
                )
            }
        }
    }

    // ── Retake / Confirm ──────────────────────────────────────────────────────

    fun retakePhoto() {
        _uiState.value = _uiState.value.copy(
            pictureTaken = false,
            photoUri = null,
            capturedFilePending = null,
        )
    }

    fun confirmPhoto(): MediaRecord? {
        // The record was already saved in capturePhotoNow.
        // Return the last captured record URI for navigation.
        return null
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        cameraEngine?.release()
    }
}
