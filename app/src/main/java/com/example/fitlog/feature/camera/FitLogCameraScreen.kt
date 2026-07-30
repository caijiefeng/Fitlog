@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.fitlog.feature.camera

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import kotlin.math.roundToInt

/**
 * Camera screen supporting both photo and video modes.
 *
 * @param onNavigateBack Called when the user wants to go back.
 * @param onMediaSaved Called with the new media record ID after a photo or
 *        video has been saved to the database.
 */
@Composable
fun FitLogCameraScreen(
    viewModel: FitLogCameraViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onMediaSaved: (Long) -> Unit = {},
    onNavigateToMediaDetail: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val hasCameraPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    // Create CameraXEngine once
    val cameraEngine = remember {
        CameraXEngine(context, lifecycleOwner).also { viewModel.cameraEngine = it }
    }

    // Bind preview when permission granted or mode toggled
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(hasCameraPermission.value, uiState.isVideoMode, uiState.isFrontCamera) {
        if (hasCameraPermission.value && previewView != null) {
            viewModel.bindPreview(previewView!!)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.releaseCamera()
        }
    }

    // ── Back confirmation when recording ────────────────────────────────────
    var showExitConfirm by remember { mutableStateOf(false) }

    fun handleBack() {
        if (uiState.recordingState is RecordingState.Recording ||
            uiState.recordingState is RecordingState.Starting
        ) {
            showExitConfirm = true
        } else {
            onNavigateBack()
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text(stringResource(R.string.media_delete_title)) },
            text = { Text(stringResource(R.string.camera_video_discard_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirm = false
                        viewModel.stopVideoRecording()
                        viewModel.retakePhoto()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FitLogError),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        containerColor = FitLogBackground,
    ) { innerPadding ->
        if (!hasCameraPermission.value) {
            CameraPermissionPlaceholder(
                onPermissionGranted = { hasCameraPermission.value = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else if (uiState.pictureTaken && uiState.photoUri != null) {
            // Preview / retake flow (photo or video)
            MediaPreviewScreen(
                photoUri = uiState.photoUri!!,
                isVideo = uiState.recordingState is RecordingState.Preview,
                onRetake = {
                    viewModel.retakePhoto()
                    previewView?.let { viewModel.bindPreview(it) }
                },
                onConfirm = {
                    val mediaId = viewModel.confirmPhoto()
                    if (mediaId != null) {
                        onMediaSaved(mediaId)
                    } else {
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            CameraViewfinder(
                previewView = previewView,
                onPreviewViewCreated = { pv ->
                    previewView = pv
                    if (hasCameraPermission.value) {
                        viewModel.bindPreview(pv)
                    }
                },
                uiState = uiState,
                viewModel = viewModel,
                onNavigateBack = { handleBack() },
                onNavigateToDetail = { onNavigateToMediaDetail(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

// ── Camera Viewfinder ─────────────────────────────────────────────────────────

@Composable
private fun CameraViewfinder(
    previewView: PreviewView?,
    onPreviewViewCreated: (PreviewView) -> Unit,
    uiState: CameraUiState,
    viewModel: FitLogCameraViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var lastPinchTime by remember { mutableStateOf(0L) }

    // Focus indicator fade animation
    val focusAlpha by animateFloatAsState(
        targetValue = if (uiState.focusState.isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "focusAlpha",
    )

    val isRecording = uiState.recordingState is RecordingState.Recording ||
            uiState.recordingState is RecordingState.Starting

    Box(
        modifier = modifier.background(Color.Black),
    ) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    onPreviewViewCreated(this)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Grid overlay
        if (uiState.showGrid) {
            CameraGridOverlay(modifier = Modifier.fillMaxSize())
        }

        // ── Gesture handlers ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        lastPinchTime = System.currentTimeMillis()
                        val newRatio = uiState.zoomRatio * zoom
                        viewModel.setZoomRatio(newRatio)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            lastPinchTime = System.currentTimeMillis()
                            val target = if (uiState.zoomRatio > 1.5f) 1.0f else 2.0f
                            viewModel.setZoomRatio(
                                target.coerceIn(
                                    uiState.minZoomRatio,
                                    uiState.maxZoomRatio,
                                ),
                            )
                        },
                        onTap = { offset ->
                            val now = System.currentTimeMillis()
                            if (now - lastPinchTime > 300) {
                                val x = offset.x / size.width.toFloat()
                                val y = offset.y / size.height.toFloat()
                                viewModel.focus(x, y)
                            }
                        },
                    )
                },
        )

        // ── Focus indicator (simplified) ────────────────────────────────────
        if (focusAlpha > 0.01f) {
            Text(
                text = if (uiState.focusState.isSuccess) "对焦成功" else "对焦失败",
                color = if (uiState.focusState.isSuccess) Color.Green else Color.Red,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.labelSmall,
            )
        }

        // Recording indicator (red dot + duration)
        if (isRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 84.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Red),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.formatDuration(uiState.recordingDurationMillis),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    )
                }
            }
        }

        // Error overlay
        uiState.error?.let { error ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 84.dp)
                    .background(
                        color = FitLogError.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = error,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // Capturing / finalizing indicator
        if (uiState.isCapturing || uiState.recordingState is RecordingState.Finalizing) {
            CircularProgressIndicator(
                color = FitLogAccent,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
            )
        }

        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_back),
                    tint = Color.White,
                )
            }

            // Mode toggle tabs
            ModeToggleTabs(
                isVideoMode = uiState.isVideoMode,
                enabled = !isRecording,
                onModeChange = { viewModel.toggleMode() },
            )

            IconButton(onClick = { viewModel.toggleGrid() }) {
                Icon(
                    imageVector = if (uiState.showGrid) Icons.Filled.GridOn
                    else Icons.Filled.GridOff,
                    contentDescription = if (uiState.showGrid) {
                        stringResource(R.string.camera_grid_off)
                    } else {
                        stringResource(R.string.camera_grid_on)
                    },
                    tint = Color.White,
                )
            }
        }

        // Recent media thumbnail (bottom-right corner)
        if (uiState.recentMediaPath != null && !uiState.pictureTaken && !isRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 120.dp)
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable {
                        uiState.recentMediaId?.let { onNavigateToDetail(it) }
                    },
            ) {
                AsyncImage(
                    model = uiState.recentMediaPath,
                    contentDescription = "Recent",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Zoom controls ───────────────────────────────────────────────
            AnimatedVisibility(visible = !isRecording) {
                ZoomControls(
                    zoomRatio = uiState.zoomRatio,
                    minZoomRatio = uiState.minZoomRatio,
                    maxZoomRatio = uiState.maxZoomRatio,
                    onZoomChange = { viewModel.setZoomRatio(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            if (!isRecording) {
                Spacer(modifier = Modifier.height(12.dp))

                // ── Exposure slider ─────────────────────────────────────────
                ExposureSlider(
                    exposure = uiState.exposureIndex,
                    minExposure = uiState.minExposureIndex,
                    maxExposure = uiState.maxExposureIndex,
                    exposureStepEv = uiState.exposureStepEv,
                    onExposureChange = { viewModel.setExposure(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Bottom action row ───────────────────────────────────────────
            if (uiState.isVideoMode) {
                VideoBottomControls(
                    uiState = uiState,
                    viewModel = viewModel,
                    onPreviewViewCreated = { onPreviewViewCreated(previewView ?: return@VideoBottomControls) },
                    isRecording = isRecording,
                )
            } else {
                PhotoBottomControls(
                    uiState = uiState,
                    viewModel = viewModel,
                    isRecording = isRecording,
                )
            }
        }
    }
}

// ── Mode Toggle Tabs ──────────────────────────────────────────────────────────

@Composable
private fun ModeToggleTabs(
    isVideoMode: Boolean,
    enabled: Boolean,
    onModeChange: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(2.dp),
    ) {
        // Photo tab
        Text(
            text = stringResource(R.string.camera_photo_mode),
            color = if (!isVideoMode) Color.Black else Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = if (!isVideoMode) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (!isVideoMode) Color.White else Color.Transparent)
                .clickable(enabled = enabled && isVideoMode) { onModeChange() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )

        // Video tab
        Text(
            text = stringResource(R.string.camera_video_mode),
            color = if (isVideoMode) Color.Black else Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = if (isVideoMode) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (isVideoMode) Color.White else Color.Transparent)
                .clickable(enabled = enabled && !isVideoMode) { onModeChange() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

// ── Photo Bottom Controls ──────────────────────────────────────────────────────

@Composable
private fun PhotoBottomControls(
    uiState: CameraUiState,
    viewModel: FitLogCameraViewModel,
    isRecording: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Timer toggle
        CameraControlButton(
            icon = {
                Icon(
                    Icons.Filled.Timer,
                    contentDescription = stringResource(R.string.camera_timer_off),
                    tint = if (uiState.timerOption != TimerOption.OFF) FitLogAccent
                    else Color.White,
                )
            },
            label = if (uiState.timerOption != TimerOption.OFF) {
                "${uiState.timerOption.seconds}s"
            } else "",
            onClick = { viewModel.cycleTimer() },
        )

        // Flash toggle
        CameraControlButton(
            icon = {
                Icon(
                    imageVector = if (uiState.flashMode == FlashMode.ON) {
                        Icons.Filled.FlashOn
                    } else {
                        Icons.Filled.FlashOff
                    },
                    contentDescription = if (uiState.flashMode == FlashMode.ON) {
                        stringResource(R.string.camera_flash_off)
                    } else {
                        stringResource(R.string.camera_flash_on)
                    },
                    tint = if (uiState.flashMode == FlashMode.ON) FitLogAccent
                    else Color.White,
                )
            },
            label = "",
            onClick = { viewModel.toggleFlash() },
        )

        // Capture button
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(enabled = !uiState.isCapturing && !isRecording) {
                    viewModel.capturePhoto()
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color.Black, CircleShape),
            )
        }

        // Lens switch
        CameraControlButton(
            icon = {
                Icon(
                    Icons.Filled.Cameraswitch,
                    contentDescription = stringResource(R.string.camera_flip),
                    tint = Color.White,
                )
            },
            label = "",
            onClick = {
                viewModel.switchLens()
                // Rebind will be triggered by LaunchedEffect on remount
            },
        )

        // Spacer for symmetry
        Spacer(modifier = Modifier.size(48.dp))
    }
}

// ── Video Bottom Controls ──────────────────────────────────────────────────────

@Composable
private fun VideoBottomControls(
    uiState: CameraUiState,
    viewModel: FitLogCameraViewModel,
    onPreviewViewCreated: () -> Unit,
    isRecording: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Mic toggle
        CameraControlButton(
            icon = {
                Icon(
                    imageVector = if (uiState.micEnabled) Icons.Filled.Mic
                    else Icons.Filled.MicOff,
                    contentDescription = if (uiState.micEnabled) {
                        stringResource(R.string.camera_mic_off)
                    } else {
                        stringResource(R.string.camera_mic_on)
                    },
                    tint = if (uiState.micEnabled) FitLogAccent else Color.White.copy(alpha = 0.5f),
                )
            },
            label = "",
            onClick = { viewModel.toggleMic() },
        )

        // Flash toggle (torch in video mode)
        CameraControlButton(
            icon = {
                Icon(
                    imageVector = if (uiState.flashMode == FlashMode.ON) {
                        Icons.Filled.FlashOn
                    } else {
                        Icons.Filled.FlashOff
                    },
                    contentDescription = if (uiState.flashMode == FlashMode.ON) {
                        stringResource(R.string.camera_flash_off)
                    } else {
                        stringResource(R.string.camera_flash_on)
                    },
                    tint = if (uiState.flashMode == FlashMode.ON) FitLogAccent
                    else Color.White,
                )
            },
            label = "",
            onClick = {
                viewModel.toggleFlash()
                onPreviewViewCreated()
            },
        )

        // Record button (red)
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.Red)
                .clickable(enabled = !uiState.isCapturing) {
                    if (isRecording) {
                        viewModel.stopVideoRecording()
                    } else {
                        viewModel.startVideoRecording()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isRecording) {
                // Stop button: white square
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White),
                )
            } else {
                // Record button: inner circle
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                )
            }
        }

        // Lens switch (disabled during recording)
        CameraControlButton(
            icon = {
                Icon(
                    Icons.Filled.Cameraswitch,
                    contentDescription = stringResource(R.string.camera_flip),
                    tint = if (isRecording) Color.White.copy(alpha = 0.3f) else Color.White,
                )
            },
            label = "",
            onClick = {
                if (!isRecording) {
                    viewModel.switchLens()
                    onPreviewViewCreated()
                }
            },
        )

        // Quality label
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "FHD",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ── Zoom Controls ──────────────────────────────────────────────────────────────

@Composable
private fun ZoomControls(
    zoomRatio: Float,
    minZoomRatio: Float,
    maxZoomRatio: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Quick zoom preset buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            listOf(
                "1x" to 1.0f,
                "2x" to 2.0f,
                "max" to maxZoomRatio,
            ).forEach { (label, target) ->
                val enabled = target in minZoomRatio..maxZoomRatio
                TextButton(
                    onClick = { onZoomChange(target) },
                    enabled = enabled,
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Text(
                        text = label,
                        color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // Zoom ratio slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = String.format("%.1fx", zoomRatio),
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.width(40.dp),
            )
            Slider(
                value = zoomRatio,
                valueRange = minZoomRatio..maxZoomRatio,
                onValueChange = onZoomChange,
                colors = SliderDefaults.colors(
                    thumbColor = FitLogAccent,
                    activeTrackColor = FitLogAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = String.format("%.1fx", maxZoomRatio),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                modifier = Modifier.width(40.dp),
            )
        }
    }
}

// ── Camera Control Button ─────────────────────────────────────────────────────

@Composable
private fun CameraControlButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onClick) {
            icon()
        }
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 10.sp,
            )
        }
    }
}

// ── Grid Overlay ──────────────────────────────────────────────────────────────

@Composable
private fun CameraGridOverlay(modifier: Modifier = Modifier) {
    val lineColor = Color.White.copy(alpha = 0.3f)
    BoxWithConstraints(modifier = modifier) {
        val thirdWidth = maxWidth / 3f
        val thirdHeight = maxHeight / 3f

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .offset(x = thirdWidth)
                .background(lineColor),
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .offset(x = thirdWidth * 2f)
                .background(lineColor),
        )
        Box(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .offset(y = thirdHeight)
                .background(lineColor),
        )
        Box(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .offset(y = thirdHeight * 2f)
                .background(lineColor),
        )
    }
}

// ── Exposure Slider ───────────────────────────────────────────────────────────

@Composable
private fun ExposureSlider(
    exposure: Int,
    minExposure: Int,
    maxExposure: Int,
    exposureStepEv: Double,
    onExposureChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rangeSize = (maxExposure - minExposure).coerceAtLeast(1)
    val evValue = if (exposureStepEv != 0.0) exposure * exposureStepEv else 0.0

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "EV",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            modifier = Modifier.padding(end = 4.dp),
        )
        Slider(
            value = (exposure - minExposure).toFloat() / rangeSize.toFloat(),
            onValueChange = { value ->
                val index = (value * rangeSize + minExposure).roundToInt()
                onExposureChange(index.coerceIn(minExposure, maxExposure))
            },
            colors = SliderDefaults.colors(
                thumbColor = FitLogAccent,
                activeTrackColor = FitLogAccent,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "%.1f".format(evValue),
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

// ── Media Preview Screen (photo & video) ─────────────────────────────────────

@Composable
private fun MediaPreviewScreen(
    photoUri: android.net.Uri,
    isVideo: Boolean,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black),
    ) {
        coil.compose.AsyncImage(
            model = photoUri,
            contentDescription = if (isVideo) "Video preview" else "Photo preview",
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(3f / 4f),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )

        // Video play icon overlay
        if (isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(3f / 4f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Videocam,
                    contentDescription = stringResource(R.string.media_play_video),
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(64.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onRetake,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.3f),
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.camera_retake),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.camera_retake),
                    color = Color.White,
                )
            }

            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FitLogAccent,
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = stringResource(R.string.action_save),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.camera_save),
                    color = Color.White,
                )
            }
        }
    }
}

// ── Camera Permission Placeholder ─────────────────────────────────────────────

@Composable
private fun CameraPermissionPlaceholder(
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.camera_permission_denied),
            color = FitLogTextPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.camera_permission_denied_desc),
            color = FitLogTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
