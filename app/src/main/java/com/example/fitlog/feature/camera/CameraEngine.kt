package com.example.fitlog.feature.camera

import android.net.Uri
import androidx.camera.view.PreviewView
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Snapshot of the camera's current hardware capabilities.
 *
 * Consumers read these values from [CameraEngine.capabilities] and treat
 * them as the source of truth for UI ranges, labels, and toggles.
 */
data class CameraCapabilities(
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 1.0f,
    val currentZoomRatio: Float = 1.0f,
    val minExposureIndex: Int = 0,
    val maxExposureIndex: Int = 0,
    val currentExposureIndex: Int = 0,
    val exposureStepEv: Double = 0.0,
    val hasFlashUnit: Boolean = false,
    val supportsVideo: Boolean = false,
    val supportsManualExposure: Boolean = false,
)

/**
 * Abstraction over the device camera, allowing the UI layer to remain
 * decoupled from the concrete CameraX implementation.
 *
 * Implementations must handle their own lifecycle — callers invoke
 * [bindPreview] to attach the camera preview, and [release] to tear down
 * all resources.
 *
 * ## Capabilities
 * The [capabilities] flow emits the current hardware capabilities whenever
 * the preview is bound or a setting (zoom, exposure) changes.
 */
interface CameraEngine {

    // ── Capabilities ────────────────────────────────────────────────────────────

    /** Emits the camera's current hardware capabilities. */
    val capabilities: StateFlow<CameraCapabilities>

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Binds the camera preview to [previewView] and begins displaying frames. */
    suspend fun bindPreview(previewView: PreviewView)

    /** Releases all camera resources. After this call the engine is unusable. */
    fun release()

    // ── Photo capture ─────────────────────────────────────────────────────────

    /**
     * Captures a single photo and writes it to [outputFile].
     * Returns the same [outputFile] on success, or `null` if the capture failed.
     */
    suspend fun capturePhoto(outputFile: File): File?

    // ── Video mode ──────────────────────────────────────────────────────────

    /** When true, [bindPreview] will use VideoCapture instead of ImageCapture. */
    var isVideoMode: Boolean

    /** Whether audio is enabled for video recording. */
    var micEnabled: Boolean

    // ── Video recording ───────────────────────────────────────────────────────

    /** Starts recording video to [outputFile]. */
    suspend fun startVideo(outputFile: File)

    /**
     * Stops an active recording and returns the file it was written to,
     * or `null` if no recording was in progress.
     */
    suspend fun stopVideo(): File?

    // ── Camera controls ───────────────────────────────────────────────────────

    /** Switches between front-facing and back-facing lenses. */
    fun switchLens()

    /**
     * Sets the zoom ratio (e.g. 1.0x, 2.0x). Values outside the camera's
     * supported range are silently clamped. Returns [Result.success] on
     * success or [Result.failure] if the camera is not bound or the
     * underlying CameraX API throws.
     */
    suspend fun setZoomRatio(ratio: Float): Result<Unit>

    /**
     * Sets the exposure compensation index. Negative values darken the image,
     * positive values brighten it. Values outside the camera's supported range
     * are silently clamped.
     */
    suspend fun setExposure(index: Int): Result<Unit>

    /**
     * Triggers autofocus at the given normalized viewport coordinates (0..1).
     * Returns [Result.success] when focus and metering complete successfully.
     */
    suspend fun focus(x: Float, y: Float): Result<Unit>

    /**
     * Toggles the flash / torch. When flash is ON in photo mode the flash
     * fires on capture; in video / preview mode this controls the torch.
     */
    fun toggleFlash()
}
