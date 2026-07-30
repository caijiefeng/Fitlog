package com.example.fitlog.feature.camera

import android.net.Uri
import androidx.annotation.FloatRange
import androidx.camera.view.PreviewView
import java.io.File

/**
 * Abstraction over the device camera, allowing the UI layer to remain
 * decoupled from the concrete CameraX implementation.
 *
 * Implementations must handle their own lifecycle — callers invoke
 * [bindPreview] to attach the camera preview, and [release] to tear down
 * all resources.
 */
interface CameraEngine {

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
     * Sets the zoom ratio. Values outside the camera's supported range
     * are silently clamped.
     */
    fun setZoom(@FloatRange(from = 0.0) ratio: Float)

    /**
     * Sets the exposure compensation index. Negative values darken the image,
     * positive values brighten it.
     */
    fun setExposure(index: Int)

    /** Triggers autofocus at the given normalized viewport coordinates. */
    fun focus(x: Float, y: Float)

    /**
     * Toggles the flash / torch. When flash is ON in photo mode the flash
     * fires on capture; in video / preview mode this controls the torch.
     */
    fun toggleFlash()
}
