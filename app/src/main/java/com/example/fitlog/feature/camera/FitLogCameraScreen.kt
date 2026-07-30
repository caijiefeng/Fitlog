package com.example.fitlog.feature.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.core.media.AppMediaStorage
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Full-screen camera with CameraX — photo capture, video recording,
 * tap-to-focus, pinch-zoom, exposure compensation, timer, grid overlay.
 */
@Composable
fun FitLogCameraScreen(
    onNavigateBack: () -> Unit,
    onMediaSaved: (mediaId: Long) -> Unit = {},
    onNavigateToMediaLibrary: () -> Unit = {},
    viewModel: FitLogCameraViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // CameraX references held across recompositions
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val mediaStorage = remember { AppMediaStorage(context) }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            recording?.stop()
            cameraExecutor.shutdown()
        }
    }

    // Start/restart camera when facing or flash mode changes
    LaunchedEffect(uiState.permissionState, uiState.isFrontCamera, uiState.flashMode) {
        if (uiState.permissionState != CameraPermissionState.GRANTED) return@LaunchedEffect
        val provider = ProcessCameraProvider.getInstance(context).get()

        provider.unbindAll()

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

        val imageCap = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(androidx.camera.core.AspectRatio.RATIO_16_9)
            .apply {
                when (uiState.flashMode) {
                    FlashMode.ON -> setFlashMode(ImageCapture.FLASH_MODE_ON)
                    FlashMode.AUTO -> setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                    FlashMode.OFF -> setFlashMode(ImageCapture.FLASH_MODE_OFF)
                }
            }
            .build()

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(Quality.FHD, FallbackStrategy.lowerQualityOrHigher(Quality.SD))
            )
            .build()
        val videoCap = VideoCapture.Builder(recorder).build()

        val selector = if (uiState.isFrontCamera) {
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
        } else {
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        }

        try {
            val cam = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCap, videoCap)
            camera = cam
            imageCapture = imageCap
            videoCapture = videoCap

            // Set zoom range
            val zs = cam.cameraInfo.zoomState.value
            if (zs != null) {
                viewModel.setZoomRange(zs.minZoomRatio, zs.maxZoomRatio)
            }
            // Set exposure range
            val es = cam.cameraInfo.exposureState.value
            if (es != null) {
                viewModel.setExposureRange(
                    es.exposureCompensationRange.lower,
                    es.exposureCompensationRange.upper,
                )
            }
            viewModel.setCameraReady()
        } catch (_: Exception) {
            // Fallback: preview only
            try {
                val cam = provider.bindToLifecycle(lifecycleOwner, selector, preview)
                camera = cam
                viewModel.setCameraReady()
            } catch (_: Exception) { }
        }
    }

    // Apply zoom
    LaunchedEffect(uiState.zoomRatio) {
        camera?.cameraControl?.setZoomRatio(uiState.zoomRatio)
    }

    // Apply exposure
    LaunchedEffect(uiState.exposureCompensation) {
        camera?.cameraControl?.setExposureCompensationIndex(uiState.exposureCompensation)
    }

    // Apply torch
    LaunchedEffect(uiState.isTorchOn) {
        try { camera?.cameraControl?.enableTorch(uiState.isTorchOn) } catch (_: Exception) { }
    }

    // Navigate after save
    LaunchedEffect(uiState.savedMediaId) {
        uiState.savedMediaId?.let { id ->
            onMediaSaved(id)
            viewModel.clearSavedMediaId()
        }
    }

    // ── Permission denied dialogs ─────────────────────────────────────────
    if (uiState.permissionState == CameraPermissionState.DENIED) {
        AlertDialog(
            onDismissRequest = onNavigateBack,
            title = { Text("需要相机权限") },
            text = { Text("请授予相机权限以使用拍照和录像功能") },
            confirmButton = { TextButton(onClick = onNavigateBack) { Text("返回") } },
        )
    }
    if (uiState.permissionState == CameraPermissionState.PERMANENTLY_DENIED) {
        AlertDialog(
            onDismissRequest = onNavigateBack,
            title = { Text("无法访问相机") },
            text = { Text("请在系统设置中授予相机权限") },
            confirmButton = { TextButton(onClick = onNavigateBack) { Text("返回") } },
        )
    }

    // ── Main UI ───────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Camera preview
        if (uiState.permissionState == CameraPermissionState.GRANTED) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).also {
                        it.scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewView = it
                    }
                },
            )

            // Grid overlay
            if (uiState.showGrid) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val lineColor = Color.White.copy(alpha = 0.4f)
                    drawLine(lineColor, Offset(w / 3, 0f), Offset(w / 3, h), strokeWidth = 1f)
                    drawLine(lineColor, Offset(2 * w / 3, 0f), Offset(2 * w / 3, h), strokeWidth = 1f)
                    drawLine(lineColor, Offset(0f, h / 3), Offset(w, h / 3), strokeWidth = 1f)
                    drawLine(lineColor, Offset(0f, 2 * h / 3), Offset(w, 2 * h / 3), strokeWidth = 1f)
                }
            }

            // Pinch-zoom + tap-to-focus overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1.0f) {
                                val newZoom = uiState.zoomRatio * zoom
                                viewModel.setZoom(newZoom)
                            }
                        }
                    }
                    .clickable {
                        // Tap-to-focus: use center metering point as default
                        try {
                            val meteringPointFactory = androidx.camera.core.SurfaceOrientedMeteringPointFactory(1f, 1f)
                            val point = meteringPointFactory.createPoint(0.5f, 0.5f)
                            val action = FocusMeteringAction.Builder(point).build()
                            camera?.cameraControl?.startFocusAndMetering(action)
                        } catch (_: Exception) { }
                    },
            )
        }

        // Countdown overlay
        if (uiState.countdownValue > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.countdownValue.toString(),
                    color = Color.White,
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Photo preview overlay
        if (uiState.showPreview && uiState.capturedPhotoUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        // Retake
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { viewModel.retakePhoto() },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            ) {
                                Icon(Icons.Filled.Replay, "重拍", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Text("重拍", color = Color.White, fontSize = 12.sp)
                        }

                        // Save
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(56.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp,
                                )
                            } else {
                                IconButton(
                                    onClick = { viewModel.savePhoto(uiState.capturedPhotoUri!!) },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color(0xFF2196F3), CircleShape),
                                ) {
                                    Icon(Icons.Filled.Check, "保存", tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }
                            Text("保存", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Top bar (always visible unless preview is showing)
        if (!uiState.showPreview) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { viewModel.setCameraMode(CameraMode.PHOTO) }) {
                        Text(
                            "拍照",
                            color = if (uiState.cameraMode == CameraMode.PHOTO) Color.White
                            else Color.White.copy(alpha = 0.5f),
                            fontWeight = if (uiState.cameraMode == CameraMode.PHOTO) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                    TextButton(onClick = { viewModel.setCameraMode(CameraMode.VIDEO) }) {
                        Text(
                            "录像",
                            color = if (uiState.cameraMode == CameraMode.VIDEO) Color.White
                            else Color.White.copy(alpha = 0.5f),
                            fontWeight = if (uiState.cameraMode == CameraMode.VIDEO) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
                Spacer(Modifier.width(48.dp))
            }
        }

        // Bottom controls
        if (!uiState.showPreview) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Exposure slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("-", color = Color.White, fontSize = 12.sp)
                    Slider(
                        modifier = Modifier.weight(1f),
                        value = uiState.exposureCompensation.toFloat(),
                        onValueChange = { viewModel.setExposureCompensation(it.toInt()) },
                        valueRange = uiState.minExposure.toFloat()..uiState.maxExposure.toFloat(),
                        steps = (uiState.maxExposure - uiState.minExposure) - 1,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                        ),
                    )
                    Text("+", color = Color.White, fontSize = 12.sp)
                }

                Spacer(Modifier.height(12.dp))

                // Side controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Flash
                    IconButton(onClick = { viewModel.cycleFlashMode() }) {
                        Icon(
                            when {
                                uiState.flashMode == FlashMode.ON || uiState.isTorchOn -> Icons.Filled.FlashOn
                                uiState.flashMode == FlashMode.AUTO -> Icons.Filled.FlashAuto
                                else -> Icons.Filled.FlashOff
                            },
                            contentDescription = null,
                            tint = if (uiState.flashMode != FlashMode.OFF) Color.Yellow else Color.White,
                        )
                    }
                    // Timer
                    IconButton(onClick = { viewModel.cycleTimer() }) {
                        Icon(
                            when (uiState.timerSeconds) {
                                3 -> Icons.Filled.Timer3
                                10 -> Icons.Filled.Timer10
                                else -> Icons.Filled.TimerOff
                            },
                            contentDescription = null,
                            tint = if (uiState.timerSeconds > 0) Color.Yellow else Color.White,
                        )
                    }
                    // Grid
                    IconButton(onClick = { viewModel.toggleGrid() }) {
                        Icon(
                            if (uiState.showGrid) Icons.Filled.GridOn else Icons.Filled.GridOff,
                            contentDescription = null,
                            tint = if (uiState.showGrid) Color.Yellow else Color.White,
                        )
                    }
                    // Mic (video only)
                    if (uiState.cameraMode == CameraMode.VIDEO) {
                        IconButton(onClick = { viewModel.setMicEnabled(!uiState.isMicEnabled) }) {
                            Icon(
                                if (uiState.isMicEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                                contentDescription = null,
                                tint = if (uiState.isMicEnabled) Color.White else Color.Red,
                            )
                        }
                    }
                    // Flip
                    IconButton(onClick = { viewModel.toggleCameraFacing() }) {
                        Icon(
                            if (uiState.isFrontCamera) Icons.Filled.CameraRear else Icons.Filled.CameraFront,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Recording duration
                if (uiState.isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(Modifier.size(8.dp).background(Color.Red, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = formatDuration(uiState.recordDurationMs),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Capture button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Gallery
                    IconButton(onClick = onNavigateToMediaLibrary) {
                        Icon(Icons.Filled.PhotoLibrary, "媒体库", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
                    }

                    // Capture button
                    if (uiState.isRecording) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                                .clickable { recording?.stop(); viewModel.setCurrentRecording(null) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(28.dp).background(Color.White, RoundedCornerShape(4.dp)))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .border(3.dp, Color.White, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f))
                                .clickable {
                                    if (uiState.timerSeconds > 0) {
                                        viewModel.startCountdown {
                                            performCaptureAction(
                                                context = context,
                                                mediaStorage = mediaStorage,
                                                imageCapture = imageCapture,
                                                videoCapture = videoCapture,
                                                cameraExecutor = cameraExecutor,
                                                cameraMode = uiState.cameraMode,
                                                isMicEnabled = uiState.isMicEnabled,
                                                viewModel = viewModel,
                                                onRecording = { recording = it },
                                            )
                                        }
                                    } else {
                                        performCaptureAction(
                                            context = context,
                                            mediaStorage = mediaStorage,
                                            imageCapture = imageCapture,
                                            videoCapture = videoCapture,
                                            cameraExecutor = cameraExecutor,
                                            cameraMode = uiState.cameraMode,
                                            isMicEnabled = uiState.isMicEnabled,
                                            viewModel = viewModel,
                                            onRecording = { recording = it },
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (uiState.cameraMode == CameraMode.PHOTO) {
                                Box(Modifier.size(56.dp).background(Color.White, CircleShape))
                            } else {
                                Box(Modifier.size(56.dp).background(Color.Red, CircleShape))
                            }
                        }
                    }

                    // Spacer for symmetry
                    Spacer(Modifier.size(48.dp))
                }
            }
        }

        // Error indicator
        if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(uiState.error ?: "", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ── Capture logic ─────────────────────────────────────────────────────────────────

private fun performCaptureAction(
    context: android.content.Context,
    mediaStorage: AppMediaStorage,
    imageCapture: ImageCapture?,
    videoCapture: VideoCapture<Recorder>?,
    cameraExecutor: ExecutorService,
    cameraMode: CameraMode,
    isMicEnabled: Boolean,
    viewModel: FitLogCameraViewModel,
    onRecording: (Recording) -> Unit,
) {
    when (cameraMode) {
        CameraMode.PHOTO -> {
            if (imageCapture == null) return
            val pending = mediaStorage.createPendingPhoto("image/jpeg")

            // CameraX writes directly to final file path; remove .pending suffix
            val outputFile = mediaStorage.resolveFile(pending.relativePath)
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

            imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        // Clean up the .pending file created by createPendingPhoto
                        try {
                            mediaStorage.resolveFile(pending.relativePath).parentFile?.let { dir ->
                                File(dir, outputFile.name + ".pending")?.delete()
                            }
                        } catch (_: Exception) { }
                        viewModel.setCapturedPhoto(pending.relativePath)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        viewModel.onCaptureError(exception)
                        // Discard pending
                        try { mediaStorage.discardPendingMedia(pending) } catch (_: Exception) { }
                    }
                },
            )
        }

        CameraMode.VIDEO -> {
            if (videoCapture == null) return
            val pending = mediaStorage.createPendingVideo("video/mp4")
            val outputFile = mediaStorage.resolveFile(pending.relativePath)
            val outputOptions = FileOutputOptions.Builder(outputFile).build()

            val recording = videoCapture.output.prepareRecording(context, outputOptions)
                .apply { if (isMicEnabled) withAudioEnabled() }
                .start(androidx.core.content.ContextCompat.getMainExecutor(context)) { event ->
                    viewModel.onVideoRecordEvent(event)
                }
            onRecording(recording)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}
