package com.example.fitlog.feature.camera

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import androidx.camera.view.PreviewView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

/**
 * Fake implementation of [CameraEngine] for tests.
 *
 * - Photo capture writes a 640x480 black [Bitmap] to the requested file.
 * - Video recording creates an empty file.
 * - Every method call is recorded in [callLog] for test verification.
 * - Capabilities are fully configurable via public mutable properties.
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

    // ── Configurable capabilities ───────────────────────────────────────────────

    var minZoomRatio: Float = 1.0f
    var maxZoomRatio: Float = 10.0f
    var currentZoomRatio: Float = 1.0f
    var minExposureIndex: Int = -10
    var maxExposureIndex: Int = 10
    var currentExposureIndex: Int = 0
    var exposureStepEv: Double = 0.5
    var hasFlashUnit: Boolean = true
    var supportsVideo: Boolean = false
    var supportsManualExposure: Boolean = true

    private val _capabilities = MutableStateFlow(
        CameraCapabilities(
            minZoomRatio = 1.0f,
            maxZoomRatio = 10.0f,
            currentZoomRatio = 1.0f,
            minExposureIndex = -10,
            maxExposureIndex = 10,
            currentExposureIndex = 0,
            exposureStepEv = 0.5,
            hasFlashUnit = true,
            supportsVideo = false,
            supportsManualExposure = true,
        ),
    )
    override val capabilities: StateFlow<CameraCapabilities> = _capabilities.asStateFlow()

    /** Rebuilds [_capabilities] from the current mutable properties. */
    private fun updateCapabilities() {
        _capabilities.value = CameraCapabilities(
            minZoomRatio = minZoomRatio,
            maxZoomRatio = maxZoomRatio,
            currentZoomRatio = currentZoomRatio,
            minExposureIndex = minExposureIndex,
            maxExposureIndex = maxExposureIndex,
            currentExposureIndex = currentExposureIndex,
            exposureStepEv = exposureStepEv,
            hasFlashUnit = hasFlashUnit,
            supportsVideo = supportsVideo,
            supportsManualExposure = supportsManualExposure,
        )
    }

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
        updateCapabilities()
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

    // ── Video mode settings ───────────────────────────────────────────────────

    override var isVideoMode: Boolean = false
    override var micEnabled: Boolean = false

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

    override suspend fun setZoomRatio(ratio: Float): Result<Unit> = runCatching {
        currentZoomRatio = ratio.coerceIn(minZoomRatio, maxZoomRatio)
        currentZoom = currentZoomRatio
        updateCapabilities()
        callLog.add("setZoomRatio($ratio)")
        Unit
    }

    override suspend fun setExposure(index: Int): Result<Unit> = runCatching {
        currentExposureIndex = index.coerceIn(minExposureIndex, maxExposureIndex)
        currentExposure = currentExposureIndex
        updateCapabilities()
        callLog.add("setExposure($index)")
        Unit
    }

    override suspend fun focus(x: Float, y: Float): Result<Unit> = runCatching {
        lastFocusX = x
        lastFocusY = y
        callLog.add("focus($x, $y)")
        Unit
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
        minZoomRatio = 1.0f
        maxZoomRatio = 10.0f
        currentZoomRatio = 1.0f
        minExposureIndex = -10
        maxExposureIndex = 10
        currentExposureIndex = 0
        exposureStepEv = 0.5
        hasFlashUnit = true
        supportsVideo = false
        supportsManualExposure = true
        currentZoom = 1.0f
        currentExposure = 0
        isFrontLens = false
        isFlashOn = false
        isRecording = false
        isVideoMode = false
        micEnabled = false
        lastPreviewView = null
        lastFocusX = 0f
        lastFocusY = 0f
        videoFile = null
        isReleased = false
        updateCapabilities()
    }
}
