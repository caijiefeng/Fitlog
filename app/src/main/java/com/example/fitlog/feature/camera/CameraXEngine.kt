package com.example.fitlog.feature.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Real CameraX implementation of [CameraEngine] using CameraX v1.3.4 APIs.
 *
 * Supports both photo and video modes. Mode switching is done by setting
 * [isVideoMode] and then calling [bindPreview] to rebind the use cases.
 *
 * ## Lifecycle
 * The engine binds to [lifecycleOwner] on every [bindPreview] call.
 * Call [release] to unbind. After [release] the engine is unusable.
 *
 * ## Capabilities
 * After every [bindPreview] the engine reads the real hardware capabilities
 * from [Camera.CameraInfo] and emits them through [capabilities].
 */
class CameraXEngine(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) : CameraEngine {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var isPreviewBound: Boolean = false
    private var flashEnabled: Boolean = false
    private var currentFacing: Int = CameraSelector.LENS_FACING_BACK
    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private var released: Boolean = false
    private var videoModeEnabled: Boolean = false
    private var audioEnabled: Boolean = false

    // ── Recording state ───────────────────────────────────────────────────────

    private var currentRecording: Recording? = null
    private var currentVideoFile: File? = null
    private var finalizeDeferred: CompletableDeferred<VideoRecordEvent.Finalize>? = null

    // ── Video mode control (set by ViewModel before bindPreview) ──────────────

    /** Whether the engine binds VideoCapture (true) or ImageCapture (false). */
    override var isVideoMode: Boolean
        get() = videoModeEnabled
        set(value) { videoModeEnabled = value }

    /** Whether audio is enabled for video recording. Requires RECORD_AUDIO permission. */
    override var micEnabled: Boolean
        get() = audioEnabled
        set(value) { audioEnabled = value }

    // ── Capabilities ─────────────────────────────────────────────────────────────

    private val _capabilities = MutableStateFlow(CameraCapabilities())
    override val capabilities: StateFlow<CameraCapabilities> = _capabilities.asStateFlow()

    /**
     * Refreshes [capabilities] from the currently bound camera.
     * Safe to call when [camera] is null (becomes a no-op).
     */
    private fun refreshCapabilities() {
        val cam = camera ?: return
        val zoomInfo = cam.cameraInfo.zoomState.value
        val expState = cam.cameraInfo.exposureState
        _capabilities.value = CameraCapabilities(
            minZoomRatio = zoomInfo?.minZoomRatio ?: 1.0f,
            maxZoomRatio = zoomInfo?.maxZoomRatio ?: 1.0f,
            currentZoomRatio = zoomInfo?.zoomRatio ?: _capabilities.value.currentZoomRatio,
            minExposureIndex = expState.exposureCompensationRange.lower,
            maxExposureIndex = expState.exposureCompensationRange.upper,
            currentExposureIndex = expState.exposureCompensationIndex,
            exposureStepEv = expState.exposureCompensationStep.toDouble(),
            hasFlashUnit = cam.cameraInfo.hasFlashUnit(),
            supportsVideo = true,
            supportsManualExposure = expState.isExposureCompensationSupported(),
        )
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override suspend fun bindPreview(previewView: PreviewView) {
        checkNotReleased()

        val provider = getOrCreateCameraProvider()

        // Build Preview use case
        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        // Select camera lens
        val cameraSelector = if (currentFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // Unbind any previous use cases before rebinding
        if (isPreviewBound) {
            provider.unbindAll()
        }

        if (videoModeEnabled) {
            // ── Video mode: Preview + VideoCapture ───────────────────────────
            val qualitySelector = QualitySelector.from(
                Quality.FHD,
                FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
            )
            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
            val vidCapture = VideoCapture.withOutput(recorder)

            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                vidCapture,
            )
            videoCapture = vidCapture
            imageCapture = null
        } else {
            // ── Photo mode: Preview + ImageCapture ───────────────────────────
            val flashMode = if (flashEnabled) {
                ImageCapture.FLASH_MODE_ON
            } else {
                ImageCapture.FLASH_MODE_OFF
            }
            val imgCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flashMode)
                .build()

            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imgCapture,
            )
            imageCapture = imgCapture
            videoCapture = null
        }

        isPreviewBound = true

        // Read real hardware capabilities after binding
        refreshCapabilities()
    }

    override fun release() {
        if (released) return
        released = true

        // Stop any active recording
        currentRecording?.stop()
        currentRecording = null

        cameraProvider?.let { provider ->
            if (isPreviewBound) {
                provider.unbindAll()
            }
        }
        camera = null
        imageCapture = null
        videoCapture = null
        cameraProvider = null
        isPreviewBound = false
    }

    // ── Photo capture ─────────────────────────────────────────────────────────

    override suspend fun capturePhoto(outputFile: File): File? {
        checkNotReleased()
        val imgCapture = imageCapture ?: return null
        outputFile.parentFile?.mkdirs()

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile)
            .build()

        return suspendCoroutine { continuation ->
            imgCapture.takePicture(
                outputOptions,
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        continuation.resume(outputFile)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resume(null)
                    }
                },
            )
        }
    }

    // ── Video recording ───────────────────────────────────────────────────────

    override suspend fun startVideo(outputFile: File) {
        checkNotReleased()
        val recorder = videoCapture?.output
            ?: throw IllegalStateException("VideoCapture not bound. Call bindPreview with isVideoMode=true first.")

        // Ensure parent directory exists
        outputFile.parentFile?.mkdirs()
        currentVideoFile = outputFile

        val outputOptions = FileOutputOptions.Builder(outputFile).build()
        val pendingRecording = recorder.prepareRecording(context, outputOptions)

        if (audioEnabled) {
            pendingRecording.withAudioEnabled()
        }

        // Create a deferred for the Finalize event
        finalizeDeferred = CompletableDeferred()

        // Start recording. start() returns the Recording object and registers
        // an event listener that receives Finalize (and other) events.
        currentRecording = pendingRecording.start(executor) { event ->
            when (event) {
                is VideoRecordEvent.Finalize -> {
                    // Recording has finished (either by stop() call or error)
                    finalizeDeferred?.complete(event)
                }
                else -> { /* Start, Status, Error — informational */ }
            }
        }
    }

    override suspend fun stopVideo(): File? {
        val rec = currentRecording ?: return null
        currentRecording = null

        val file = currentVideoFile

        // Stop the recording — this triggers a Finalize event
        rec.stop()

        return try {
            // Wait for the Finalize event (with a generous timeout)
            val event = withTimeout(VIDEO_STOP_TIMEOUT_MS) {
                val deferred = finalizeDeferred
                if (deferred != null) {
                    deferred.await()
                } else {
                    throw IOException("No finalize listener registered")
                }
            }
            finalizeDeferred = null

            // Validate: no error, file exists, size > 0
            if (event.error == VideoRecordEvent.Finalize.ERROR_NONE &&
                file != null && file.exists() && file.length() > 0L
            ) {
                currentVideoFile = null
                file
            } else {
                // Recording produced an error or invalid file — clean up
                file?.delete()
                currentVideoFile = null
                null
            }
        } catch (e: Exception) {
            // Timeout or cancellation — clean up
            file?.delete()
            currentVideoFile = null
            finalizeDeferred = null
            null
        }
    }

    // ── Camera controls ───────────────────────────────────────────────────────

    override fun switchLens() {
        currentFacing = if (currentFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        // The caller should call bindPreview again to apply the change.
    }

    override suspend fun setZoomRatio(ratio: Float): Result<Unit> = runCatching {
        val cam = requireNotNull(camera) { "Camera not bound" }
        val zoomState = cam.cameraInfo.zoomState.value
        val range = if (zoomState != null) {
            zoomState.minZoomRatio..zoomState.maxZoomRatio
        } else {
            1.0f..1.0f
        }
        val clamped = ratio.coerceIn(range)
        awaitFuture(cam.cameraControl.setZoomRatio(clamped))
        refreshCapabilities()
    }

    override suspend fun setExposure(index: Int): Result<Unit> = runCatching {
        val cam = requireNotNull(camera) { "Camera not bound" }
        val expState = cam.cameraInfo.exposureState
        val clamped = index.coerceIn(
            expState.exposureCompensationRange.lower,
            expState.exposureCompensationRange.upper,
        )
        awaitFuture(cam.cameraControl.setExposureCompensationIndex(clamped))
        refreshCapabilities()
    }

    override suspend fun focus(x: Float, y: Float): Result<Unit> = runCatching {
        val cam = requireNotNull(camera) { "Camera not bound" }
        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point).build()
        awaitFuture(cam.cameraControl.startFocusAndMetering(action))
        Unit
    }

    override fun toggleFlash() {
        flashEnabled = !flashEnabled
        camera?.cameraControl?.enableTorch(flashEnabled)
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Returns the existing [ProcessCameraProvider] or creates a new one by
     * awaiting the CameraX async initialisation.
     */
    private suspend fun getOrCreateCameraProvider(): ProcessCameraProvider {
        val existing = cameraProvider
        if (existing != null) return existing

        return suspendCoroutine { continuation ->
            val future: ListenableFuture<ProcessCameraProvider> =
                ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    try {
                        val provider = future.get()
                        cameraProvider = provider
                        continuation.resume(provider)
                    } catch (e: ExecutionException) {
                        continuation.resumeWithException(e.cause ?: e)
                    } catch (e: CancellationException) {
                        continuation.resumeWithException(e)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
    }

    /**
     * Awaits a [ListenableFuture] and returns its result.
     * Re-throws [ExecutionException]'s cause and [CancellationException]
     * through the coroutine.
     */
    private suspend fun <T> awaitFuture(future: ListenableFuture<T>): T =
        suspendCoroutine { continuation ->
            future.addListener(
                {
                    try {
                        continuation.resume(future.get())
                    } catch (e: ExecutionException) {
                        continuation.resumeWithException(e.cause ?: e)
                    } catch (e: CancellationException) {
                        continuation.resumeWithException(e)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }

    private fun checkNotReleased() {
        check(!released) { "CameraXEngine has been released" }
    }

    /** Returns the current lens facing for tests / debugging. */
    internal fun currentFacing(): Int = currentFacing

    /** Whether flash is enabled. */
    internal fun isFlashEnabled(): Boolean = flashEnabled

    companion object {
        /** Maximum time to wait for a video recording Finalize event. */
        private const val VIDEO_STOP_TIMEOUT_MS = 10_000L
    }
}
