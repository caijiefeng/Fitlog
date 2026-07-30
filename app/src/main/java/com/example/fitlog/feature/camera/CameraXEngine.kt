package com.example.fitlog.feature.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Real CameraX implementation of [CameraEngine] using CameraX v1.3.4 APIs.
 *
 * **Photo only** — video recording is not yet implemented because the
 * VideoCapture builder API in CameraX 1.3.4 behaves unreliably across
 * device manufacturers. Follow-up when CameraX 1.4+ stabilises.
 *
 * ## Lifecycle
 * The engine binds to [lifecycleOwner] on every [bindPreview] call.
 * Call [release] to unbind. After [release] the engine is unusable.
 */
class CameraXEngine(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) : CameraEngine {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var isPreviewBound: Boolean = false
    private var flashEnabled: Boolean = false
    private var currentFacing: Int = CameraSelector.LENS_FACING_BACK
    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private var released: Boolean = false

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override suspend fun bindPreview(previewView: PreviewView) {
        checkNotReleased()

        val provider = getOrCreateCameraProvider()

        // Build Preview use case
        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        // Build ImageCapture use case with current flash mode
        val flashMode = if (flashEnabled) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
        val imgCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .build()

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

        // Bind to lifecycle
        camera = provider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imgCapture,
        )
        imageCapture = imgCapture
        isPreviewBound = true
    }

    override fun release() {
        if (released) return
        released = true
        cameraProvider?.let { provider ->
            if (isPreviewBound) {
                provider.unbindAll()
            }
        }
        camera = null
        imageCapture = null
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
    // Not implemented — VideoCapture API in CameraX 1.3.4 is unreliable across
    // devices. These methods are no-ops returning null / doing nothing.

    override suspend fun startVideo(outputFile: File) {
        // No-op: video recording deferred until CameraX 1.4+.
    }

    override suspend fun stopVideo(): File? {
        // No-op: video recording deferred.
        return null
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

    override fun setZoom(ratio: Float) {
        camera?.cameraControl?.setLinearZoom(ratio.coerceIn(0f, 1f))
    }

    override fun setExposure(index: Int) {
        val control: CameraControl = camera?.cameraControl ?: return
        val range = camera?.cameraInfo?.exposureState?.exposureCompensationRange
        if (range != null) {
            control.setExposureCompensationIndex(index.coerceIn(range.lower, range.upper))
        } else {
            control.setExposureCompensationIndex(index)
        }
    }

    override fun focus(x: Float, y: Float) {
        val cam: Camera = camera ?: return
        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point).build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    override fun toggleFlash() {
        flashEnabled = !flashEnabled
        camera?.cameraControl?.enableTorch(flashEnabled)
        // Note: Flash mode will be applied on next bindPreview / image capture
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

    private fun checkNotReleased() {
        check(!released) { "CameraXEngine has been released" }
    }

    /** Returns the current lens facing for tests / debugging. */
    internal fun currentFacing(): Int = currentFacing

    /** Whether flash is enabled. */
    internal fun isFlashEnabled(): Boolean = flashEnabled
}
