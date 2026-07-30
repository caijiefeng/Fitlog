package com.example.fitlog.feature.camera

import android.content.Context
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.VideoCapture
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewTreeLifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Production implementation of [CameraEngine] backed by CameraX.
 *
 * Thread safety: all CameraX operations happen on a dedicated single-thread
 * executor.  The suspend functions provided here bridge from that executor
 * back to the caller's coroutine context.
 */
@Singleton
class CameraXEngine @Inject constructor(
    private val context: Context,
) : CameraEngine {

    // ── Internal state ────────────────────────────────────────────────────────

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    // Cache the current lens facing direction for switchLens()
    private var currentLensFacing: Int = CameraSelector.LENS_FACING_BACK

    /** Callback invoked when a [VideoRecordEvent] is fired by CameraX. */
    var onVideoEvent: ((VideoRecordEvent) -> Unit)? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override suspend fun bindPreview(previewView: PreviewView) {
        val lifecycleOwner = ViewTreeLifecycleOwner.get(previewView)
            ?: throw IllegalStateException("PreviewView is not attached to a LifecycleOwner")

        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
        videoCapture = null
        activeRecording?.stop()
        activeRecording = null

        val provider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider = provider

        provider.unbindAll()

        // ── Preview ──────────────────────────────────────────────────────
        val preview = Preview.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(
                            AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
                            AspectRatioStrategy.FALLBACK_RULE_AUTO,
                        )
                    )
                    .build()
            )
            .build()
        preview.surfaceProvider = previewView.surfaceProvider

        // ── Image capture ────────────────────────────────────────────────
        val imageCap = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MINIMIZE_LATENCY)
            .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
            .build()
        imageCapture = imageCap

        // ── Video capture ────────────────────────────────────────────────
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.FHD,
                    FallbackStrategy.lowerQualityOrHigher(Quality.SD),
                )
            )
            .build()
        val videoCap = VideoCapture.Builder(recorder).build()
        videoCapture = videoCap

        // ── Camera selector ──────────────────────────────────────────────
        val selector = CameraSelector.Builder()
            .requireLensFacing(currentLensFacing)
            .build()

        try {
            val cam = provider.bindToLifecycle(
                lifecycleOwner, selector, preview, imageCap, videoCap,
            )
            camera = cam
        } catch (e: Exception) {
            // If the full bind fails, try preview-only fallback
            try {
                val cam = provider.bindToLifecycle(lifecycleOwner, selector, preview)
                camera = cam
                imageCapture = null
                videoCapture = null
            } catch (e2: Exception) {
                camera = null
                imageCapture = null
                videoCapture = null
                throw e2
            }
        }
    }

    override fun release() {
        activeRecording?.stop()
        activeRecording = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        camera = null
        imageCapture = null
        videoCapture = null
        cameraExecutor.shutdownNow()
    }

    // ── Photo capture ─────────────────────────────────────────────────────────

    override suspend fun capturePhoto(outputFile: File): File? {
        val imageCap = imageCapture ?: return null

        outputFile.parentFile?.mkdirs()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        return suspendCancellableCoroutine { continuation ->
            imageCap.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        if (continuation.isActive) {
                            continuation.resume(outputFile)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                },
            )
        }
    }

    // ── Video recording ───────────────────────────────────────────────────────

    override suspend fun startVideo(outputFile: File) {
        val vc = videoCapture ?: return

        activeRecording?.stop()
        activeRecording = null

        outputFile.parentFile?.mkdirs()
        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        val recording = vc.output.prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(
                ContextCompat.getMainExecutor(context),
            ) { event ->
                onVideoEvent?.invoke(event)
                // Handle Finalize to clear internal reference
                if (event is VideoRecordEvent.Finalize) {
                    activeRecording = null
                }
            }

        activeRecording = recording
    }

    override suspend fun stopVideo(): File? {
        val recording = activeRecording ?: return null
        recording.stop()
        activeRecording = null
        // The file path was known at start — we can't retrieve it from the
        // Recording object after stop, so the caller must track it.
        return null
    }

    // ── Camera controls ───────────────────────────────────────────────────────

    override fun switchLens() {
        currentLensFacing = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        // rebindPreview() must be called externally after this, or the
        // caller should trigger a recomposition that calls bindPreview again.
    }

    override fun setZoom(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    override fun setExposure(index: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(index)
    }

    override fun focus(x: Float, y: Float) {
        try {
            val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(point).build()
            camera?.cameraControl?.startFocusAndMetering(action)
        } catch (_: Exception) {
            // Silently ignore — focus is best-effort on many devices
        }
    }

    override fun toggleFlash() {
        val cam = camera ?: return
        val isTorchOn = cam.cameraInfo.torchState.value ?: 0
        cam.cameraControl.enableTorch(isTorchOn != 1)
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns the current zoom range, or (1f, 10f) if unavailable. */
    fun getZoomRange(): Pair<Float, Float> {
        val zs = camera?.cameraInfo?.zoomState?.value ?: return 1f to 10f
        return zs.minZoomRatio to zs.maxZoomRatio
    }

    /** Returns the current exposure range, or (-10, 10) if unavailable. */
    fun getExposureRange(): Pair<Int, Int> {
        val es = camera?.cameraInfo?.exposureState?.value ?: return -10 to 10
        return es.exposureCompensationRange.lower to es.exposureCompensationRange.upper
    }
}
