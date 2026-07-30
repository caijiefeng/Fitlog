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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
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
 * Visual state for the on-screen focus indicator ring.
 *
 * @property x Normalised horizontal coordinate (0..1) of the tap point.
 * @property y Normalised vertical coordinate (0..1) of the tap point.
 * @property isVisible Whether the ring should be drawn.
 * @property isSuccess `true` when the autofocus operation succeeded.
 */
data class FocusIndicatorState(
    val x: Float = 0f,
    val y: Float = 0f,
    val isVisible: Boolean = false,
    val isSuccess: Boolean = true,
)

/**
 * Recording state for video mode.
 */
sealed interface RecordingState {
    /** No recording is active or pending. */
    data object Idle : RecordingState

    /** Recording is starting (engine is being prepared). */
    data object Starting : RecordingState

    /** Video is actively being recorded. */
    data object Recording : RecordingState

    /** Recording has stopped, file is being finalized. */
    data object Finalizing : RecordingState

    /** Recording is complete and ready for preview. */
    data class Preview(
        val file: File,
        val uri: Uri,
        val pending: AppMediaStorage.PendingMedia,
    ) : RecordingState

    /** An error occurred. */
    data class Error(val message: String) : RecordingState
}

/**
 * UI state for the camera screen.
 *
 * Zoom and exposure fields are driven by the engine's [CameraCapabilities]
 * flow so the UI always reflects the real hardware range.
 */
data class CameraUiState(
    val isLoading: Boolean = false,
    val photoUri: Uri? = null,
    val capturedFilePending: AppMediaStorage.PendingMedia? = null,
    val flashMode: FlashMode = FlashMode.OFF,
    val timerOption: TimerOption = TimerOption.OFF,
    // --- Zoom (from capabilities) ---
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 1.0f,
    // --- Exposure (from capabilities) ---
    val exposureIndex: Int = 0,
    val minExposureIndex: Int = 0,
    val maxExposureIndex: Int = 0,
    val exposureStepEv: Double = 0.0,
    // --- Other ---
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
    val focusState: FocusIndicatorState = FocusIndicatorState(),
    // --- Video mode ---
    val isVideoMode: Boolean = false,
    val recordingState: RecordingState = RecordingState.Idle,
    val recordingDurationMillis: Long = 0L,
    val micEnabled: Boolean = false,
    // --- Recent media thumbnail ---
    val recentMediaId: Long? = null,
    val recentMediaPath: String? = null,
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
        set(value) {
            field = value
            // Start observing capabilities as soon as the engine is provided
            value?.let { observeCapabilities(it) }
        }

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
        loadRecentMedia()
    }

    private fun loadRecentMedia() {
        viewModelScope.launch {
            try {
                val all = mediaRepository.getAll()
                val mostRecent = all.maxByOrNull { it.capturedAt }
                if (mostRecent != null) {
                    val file = mediaRepository.resolveFile(mostRecent.relativePath)
                    if (file.exists()) {
                        _uiState.value = _uiState.value.copy(
                            recentMediaId = mostRecent.id,
                            recentMediaPath = file.absolutePath,
                        )
                    }
                }
            } catch (_: Exception) {
                // Silently ignore — thumbnail is decorative
            }
        }
    }

    // ── Capabilities observation ────────────────────────────────────────────────

    /**
     * Collects the engine's [CameraCapabilities] flow and applies every
     * emission to [CameraUiState].
     */
    private fun observeCapabilities(engine: CameraEngine) {
        viewModelScope.launch {
            engine.capabilities.collect { caps ->
                _uiState.value = _uiState.value.copy(
                    zoomRatio = caps.currentZoomRatio,
                    minZoomRatio = caps.minZoomRatio,
                    maxZoomRatio = caps.maxZoomRatio,
                    exposureIndex = caps.currentExposureIndex,
                    minExposureIndex = caps.minExposureIndex,
                    maxExposureIndex = caps.maxExposureIndex,
                    exposureStepEv = caps.exposureStepEv,
                )
            }
        }
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

    // ── Mode switching ────────────────────────────────────────────────────────

    /**
     * Toggles between photo and video mode. Updates the engine's mode flag
     * so the next [bindPreview] uses the correct use cases.
     */
    fun toggleMode() {
        val current = _uiState.value.isVideoMode
        _uiState.value = _uiState.value.copy(
            isVideoMode = !current,
            recordingState = RecordingState.Idle,
            recordingDurationMillis = 0L,
        )
        cameraEngine?.isVideoMode = !current
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
        // Capabilities will refresh when bindPreview is called by the screen.
    }

    // ── Mic toggle (video mode) ───────────────────────────────────────────────

    fun toggleMic() {
        val newState = !_uiState.value.micEnabled
        _uiState.value = _uiState.value.copy(micEnabled = newState)
        cameraEngine?.micEnabled = newState
    }

    // ── Zoom ──────────────────────────────────────────────────────────────────

    /**
     * Sets the zoom ratio on the engine and updates [CameraUiState.zoomRatio].
     * The value is clamped to the current hardware range before being sent.
     */
    fun setZoomRatio(ratio: Float) {
        val state = _uiState.value
        val clamped = ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio)
        _uiState.value = state.copy(zoomRatio = clamped)
        viewModelScope.launch {
            cameraEngine?.setZoomRatio(clamped)
        }
    }

    // ── Exposure ──────────────────────────────────────────────────────────────

    /**
     * Sets the exposure compensation index on the engine.
     */
    fun setExposure(index: Int) {
        val state = _uiState.value
        _uiState.value = state.copy(exposureIndex = index)
        viewModelScope.launch {
            cameraEngine?.setExposure(index)
        }
    }

    /**
     * Returns the exposure value (EV) as a formatted string.
     * EV = exposureIndex * exposureStepEv.
     */
    fun evDisplay(): String {
        val state = _uiState.value
        if (state.exposureStepEv == 0.0) return "0.0"
        val ev = state.exposureIndex * state.exposureStepEv
        return "%.1f".format(ev)
    }

    // ── Focus ─────────────────────────────────────────────────────────────────

    /**
     * Triggers autofocus at the given normalised viewport coordinates.
     * Updates [FocusIndicatorState] with the result, then auto-clears after
     * 1.5 seconds so the UI ring fades out.
     */
    fun focus(x: Float, y: Float) {
        _uiState.value = _uiState.value.copy(
            focusState = FocusIndicatorState(x = x, y = y, isVisible = true),
        )
        viewModelScope.launch {
            val result = cameraEngine?.focus(x, y)
            _uiState.value = _uiState.value.copy(
                focusState = FocusIndicatorState(
                    x = x,
                    y = y,
                    isVisible = true,
                    isSuccess = result?.isSuccess == true,
                ),
            )
            delay(1500)
            _uiState.value = _uiState.value.copy(
                focusState = FocusIndicatorState(),
            )
        }
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
            delay(seconds * 1000)
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

                // Capture to the pending file
                val result = cameraEngine?.capturePhoto(pending.pendingFile)

                if (result != null) {
                    // Do NOT commit yet — wait for user confirmation.
                    // Show preview using the pending file directly.
                    val uri = Uri.fromFile(pending.pendingFile)

                    _uiState.value = _uiState.value.copy(
                        isCapturing = false,
                        pictureTaken = true,
                        photoUri = uri,
                        capturedFilePending = pending,
                    )
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

    // ── Video recording ───────────────────────────────────────────────────────

    private var recordingTimerJob: Job? = null
    private var videoPendingMedia: AppMediaStorage.PendingMedia? = null

    /**
     * Starts video recording. Creates a pending video file, sets up the engine,
     * and begins the recording duration timer.
     */
    fun startVideoRecording() {
        if (_uiState.value.recordingState is RecordingState.Recording) return
        if (_uiState.value.recordingState is RecordingState.Starting) return

        _uiState.value = _uiState.value.copy(
            recordingState = RecordingState.Starting,
            error = null,
        )

        viewModelScope.launch {
            try {
                val pending = mediaStorage.createPendingVideo("video/mp4")
                videoPendingMedia = pending
                cameraEngine?.startVideo(pending.pendingFile)

                _uiState.value = _uiState.value.copy(
                    recordingState = RecordingState.Recording,
                    recordingDurationMillis = 0L,
                )

                // Start the duration timer
                startRecordingTimer()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    recordingState = RecordingState.Error(
                        e.message ?: "Failed to start recording"
                    ),
                    error = e.message ?: "Failed to start recording",
                )
            }
        }
    }

    /**
     * Stops video recording, waits for the engine to finalize the file,
     * validates the result, and transitions to preview state.
     */
    fun stopVideoRecording() {
        if (_uiState.value.recordingState !is RecordingState.Recording) return

        _uiState.value = _uiState.value.copy(
            recordingState = RecordingState.Finalizing,
        )

        // Cancel the timer
        recordingTimerJob?.cancel()
        recordingTimerJob = null

        viewModelScope.launch {
            try {
                val pending = videoPendingMedia
                val file = cameraEngine?.stopVideo()

                if (file != null && file.exists() && file.length() > 0L && pending != null) {
                    // Video was recorded successfully — show preview
                    val uri = Uri.fromFile(file)

                    _uiState.value = _uiState.value.copy(
                        photoUri = uri,
                        pictureTaken = true,
                        recordingState = RecordingState.Preview(
                            file = file,
                            uri = uri,
                            pending = pending,
                        ),
                        recordingDurationMillis = _uiState.value.recordingDurationMillis,
                    )
                } else {
                    // Recording produced no valid file
                    pending?.let { mediaStorage.discardPendingMedia(it) }
                    videoPendingMedia = null
                    _uiState.value = _uiState.value.copy(
                        recordingState = RecordingState.Error("Recording produced no file"),
                        error = "Recording produced no file",
                    )
                }
            } catch (e: Exception) {
                videoPendingMedia?.let { mediaStorage.discardPendingMedia(it) }
                videoPendingMedia = null
                _uiState.value = _uiState.value.copy(
                    recordingState = RecordingState.Error(
                        e.message ?: "Failed to stop recording"
                    ),
                    error = e.message ?: "Failed to stop recording",
                )
            }
        }
    }

    /**
     * Starts a coroutine that updates [CameraUiState.recordingDurationMillis]
     * every second while recording is active.
     */
    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            var elapsed = 0L
            while (true) {
                delay(1000)
                elapsed += 1000L
                _uiState.value = _uiState.value.copy(
                    recordingDurationMillis = elapsed,
                )
            }
        }
    }

    // ── Retake / Confirm (photo + video) ─────────────────────────────────────

    /**
     * Discards the current pending media and returns to the viewfinder.
     */
    fun retakePhoto() {
        // Discard any pending media
        val photoPending = _uiState.value.capturedFilePending
        if (photoPending != null) {
            mediaStorage.discardPendingMedia(photoPending)
        }

        // Discard video pending if in video preview
        val recordingState = _uiState.value.recordingState
        if (recordingState is RecordingState.Preview) {
            try {
                recordingState.pending.pendingFile.delete()
            } catch (_: Exception) {}
        }

        // Clear any stored video pending
        val vPending = videoPendingMedia
        if (vPending != null) {
            try {
                mediaStorage.discardPendingMedia(vPending)
            } catch (_: Exception) {}
            videoPendingMedia = null
        }

        _uiState.value = _uiState.value.copy(
            pictureTaken = false,
            photoUri = null,
            capturedFilePending = null,
            recordingState = RecordingState.Idle,
            recordingDurationMillis = 0L,
        )
    }

    /**
     * Commits the pending media to permanent storage and saves the record to
     * the database.
     *
     * @return The [MediaRecord]'s ID after saving, or `null` if there is
     *         nothing to confirm.
     */
    fun confirmPhoto(): Long? {
        val currentState = _uiState.value

        // Handle photo confirmation
        val photoPending = currentState.capturedFilePending
        if (photoPending != null) {
            return commitPendingAndSave(
                pending = photoPending,
                mediaType = MediaType.PHOTO,
                mimeType = "image/jpeg",
            )
        }

        // Handle video confirmation
        val recordingState = currentState.recordingState
        if (recordingState is RecordingState.Preview) {
            return commitPendingAndSave(
                pending = recordingState.pending,
                mediaType = MediaType.VIDEO,
                mimeType = "video/mp4",
            )
        }

        return null
    }

    /**
     * Commits a pending file, saves a [MediaRecord] to the database, and
     * returns the new record's ID.
     */
    private fun commitPendingAndSave(
        pending: AppMediaStorage.PendingMedia,
        mediaType: MediaType,
        mimeType: String,
    ): Long? {
        val currentState = _uiState.value

        return try {
            val relativePath = mediaStorage.commitPendingMedia(pending)

            // Clear the stored video pending reference
            if (mediaType == MediaType.VIDEO) {
                videoPendingMedia = null
            }

            val record = MediaRecord(
                mediaType = mediaType,
                relativePath = relativePath,
                mimeType = mimeType,
                capturedAt = System.currentTimeMillis(),
                date = java.time.LocalDate.now().toEpochDay(),
                sizeBytes = mediaStorage.calculateSize(relativePath),
                category = currentState.category,
                poseTag = currentState.poseTag,
                workoutSessionId = currentState.workoutSessionId,
                bodyMeasurementId = currentState.bodyMeasurementId,
                checkInId = currentState.checkInId,
            )

            val savedRecord = kotlinx.coroutines.runBlocking { mediaRepository.save(record) }

            // Reset UI state
            _uiState.value = _uiState.value.copy(
                pictureTaken = false,
                photoUri = null,
                capturedFilePending = null,
                recordingState = RecordingState.Idle,
                recordingDurationMillis = 0L,
            )

            savedRecord.id
        } catch (e: Exception) {
            // Commit or save failed — try to clean up
            try {
                mediaStorage.discardPendingMedia(pending)
            } catch (_: Exception) {}
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Failed to save media",
            )
            null
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Formats the recording duration as MM:SS.
     */
    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()

        // Stop recording if active
        recordingTimerJob?.cancel()
        recordingTimerJob = null

        // Stop video recording in background
        viewModelScope.launch {
            try {
                cameraEngine?.stopVideo()
            } catch (_: Exception) {}
        }

        // Discard any pending media
        val photoPending = _uiState.value.capturedFilePending
        if (photoPending != null) {
            try {
                mediaStorage.discardPendingMedia(photoPending)
            } catch (_: Exception) {}
        }

        // Discard video pending if in preview
        val recordingState = _uiState.value.recordingState
        if (recordingState is RecordingState.Preview) {
            try {
                recordingState.pending.pendingFile.delete()
            } catch (_: Exception) {}
        }

        // Discard stored video pending media
        val vPending = videoPendingMedia
        if (vPending != null) {
            try {
                mediaStorage.discardPendingMedia(vPending)
            } catch (_: Exception) {}
        }

        cameraEngine?.release()
    }
}
