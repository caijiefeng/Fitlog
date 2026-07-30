package com.example.fitlog.feature.camera

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Full-screen camera with CameraEngine — photo capture, video recording,
 * tap-to-focus, pinch-zoom, exposure compensation, timer, grid overlay.
 *
 * The [CameraEngine] is injected via Hilt and accessible through the ViewModel.
 */
@Composable
fun FitLogCameraScreen(
    onNavigateBack: () -> Unit,
    onMediaSaved: (mediaId: Long) -> Unit = {},
    onNavigateToMediaLibrary: () -> Unit = {},
    viewModel: FitLogCameraViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Bind the camera engine to the PreviewView when it becomes available
    LaunchedEffect(previewView, uiState.isFrontCamera) {
        val pv = previewView ?: return@LaunchedEffect
        if (uiState.permissionState != CameraPermissionState.GRANTED) return@LaunchedEffect
        viewModel.cameraEngine.bindPreview(pv)
        viewModel.setCameraReady()
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
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }) {
                    Text("打开设置")
                }
            },
            dismissButton = {
                TextButton(onClick = onNavigateBack) { Text("返回") }
            },
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
                    PreviewView(ctx).also { pv ->
                        pv.scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewView = pv
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
                        viewModel.performFocus(0.5f, 0.5f)
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
                                .clickable { viewModel.stopVideoCapture() },
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
                                                cameraMode = uiState.cameraMode,
                                                viewModel = viewModel,
                                            )
                                        }
                                    } else {
                                        performCaptureAction(
                                            cameraMode = uiState.cameraMode,
                                            viewModel = viewModel,
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
    cameraMode: CameraMode,
    viewModel: FitLogCameraViewModel,
) {
    when (cameraMode) {
        CameraMode.PHOTO -> viewModel.capturePhotoFromEngine()
        CameraMode.VIDEO -> viewModel.startVideoCaptureFromEngine()
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}
