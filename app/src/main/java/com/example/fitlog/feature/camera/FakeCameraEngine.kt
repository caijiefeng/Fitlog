package com.example.fitlog.feature.camera

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import androidx.camera.view.PreviewView
import java.io.File
import java.io.FileOutputStream

/**
 * Fake implementation of [CameraEngine] for tests.
 *
 * - Photo capture writes a 640×480 black [Bitmap] to the requested file.
 * - Video recording creates an empty file.
 * - Every method call is recorded in [callLog] for test verification.
 * - No CameraX classes are imported (PreviewView is the only CameraX
 *   dependency, and it exists on the interface only; this class merely
 *   receives it and does nothing with it).
 */
class FakeCameraEngine : CameraEngine {

    /** Ordered list of all method invocations since the last reset. */
    val callLog: MutableList<String> = mutableListOf()

    /** Whether photo capture should simulate a failure (return null). */
    var captureFails: Boolean = false

    /** Whether video recording should simulate a failure. */
    var videoFails: Boolean = false

    // State tracking, readable by tests
    var currentZoom: Float = 1.0f
        private set
    var currentExposure: Int = 0
        private set
    var isFrontLens: Boolean = false
        private set
    var isFlashOn: Boolean = false
        private set
    var isRecording: Boolean = false
        private set
    var lastPreviewView: PreviewView? = null
        private set
    var lastFocusX: Float = 0f
        private set
    var lastFocusY: Float = 0f
        private set
    var videoFile: File? = null
        private set
    var isReleased: Boolean = false
        private set

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override suspend fun bindPreview(previewView: PreviewView) {
        lastPreviewView = previewView
        callLog.add("bindPreview")
    }

    override fun release() {
        isReleased = true
        callLog.add("release")
    }

    // ── Photo capture ─────────────────────────────────────────────────────────

    override suspend fun capturePhoto(outputFile: File): File? {
        callLog.add("capturePhoto(${outputFile.absolutePath})")
        if (captureFails) return null

        // Write a 640x480 black bitmap
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()

        return outputFile
    }

    // ── Video recording ───────────────────────────────────────────────────────

    override suspend fun startVideo(outputFile: File) {
        callLog.add("startVideo(${outputFile.absolutePath})")
        if (videoFails) return
        isRecording = true
        videoFile = outputFile
        outputFile.parentFile?.mkdirs()
        outputFile.createNewFile()
    }

    override suspend fun stopVideo(): File? {
        callLog.add("stopVideo()")
        if (!isRecording || videoFails) return null
        isRecording = false
        val file = videoFile
        videoFile = null
        return file
    }

    // ── Camera controls ───────────────────────────────────────────────────────

    override fun switchLens() {
        isFrontLens = !isFrontLens
        callLog.add("switchLens() → isFront=$isFrontLens")
    }

    override fun setZoom(ratio: Float) {
        currentZoom = ratio.coerceIn(0.1f, 10f)
        callLog.add("setZoom($ratio)")
    }

    override fun setExposure(index: Int) {
        currentExposure = index.coerceIn(-10, 10)
        callLog.add("setExposure($index)")
    }

    override fun focus(x: Float, y: Float) {
        lastFocusX = x
        lastFocusY = y
        callLog.add("focus($x, $y)")
    }

    override fun toggleFlash() {
        isFlashOn = !isFlashOn
        callLog.add("toggleFlash() → on=$isFlashOn")
    }

    // ── Test helpers ──────────────────────────────────────────────────────────

    /** Clears the [callLog] and resets all tracked state. */
    fun reset() {
        callLog.clear()
        captureFails = false
        videoFails = false
        currentZoom = 1.0f
        currentExposure = 0
        isFrontLens = false
        isFlashOn = false
        isRecording = false
        lastPreviewView = null
        lastFocusX = 0f
        lastFocusY = 0f
        videoFile = null
        isReleased = false
    }
}
