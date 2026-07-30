@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.fitlog.feature.camera

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary

@Composable
fun FitLogCameraScreen(
    viewModel: FitLogCameraViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onPhotoSaved: (String) -> Unit = {},
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

    // Bind preview when permission granted
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(hasCameraPermission.value) {
        if (hasCameraPermission.value && previewView != null) {
            viewModel.bindPreview(previewView!!)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.releaseCamera()
        }
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
            // Preview / retake flow
            PhotoPreviewScreen(
                photoUri = uiState.photoUri!!,
                onRetake = {
                    viewModel.retakePhoto()
                    previewView?.let { viewModel.bindPreview(it) }
                },
                onConfirm = {
                    viewModel.confirmPhoto()
                    onNavigateBack()
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
                onNavigateBack = onNavigateBack,
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
    modifier: Modifier = Modifier,
) {
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

        // Tap to focus
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.firstOrNull()?.position
                            if (position != null) {
                                val x = position.x / size.width.toFloat()
                                val y = position.y / size.height.toFloat()
                                viewModel.focus(x, y)
                            }
                        }
                    }
                }
        )

        // Error overlay
        uiState.error?.let { error ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
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

        // Capturing indicator
        if (uiState.isCapturing) {
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

            Text(
                text = stringResource(R.string.camera_title),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
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

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Exposure slider
            ExposureSlider(
                exposure = uiState.exposure,
                onExposureChange = { viewModel.setExposure(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                        .clickable(enabled = !uiState.isCapturing) {
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
                        previewView?.let { viewModel.bindPreview(it) }
                    },
                )

                // Spacer for symmetry (exposure slider takes more room)
                Spacer(modifier = Modifier.size(48.dp))
            }
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
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val thirdWidth = maxWidth / 3f
        val thirdHeight = maxHeight / 3f

        // Left vertical line (at 1/3 width)
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .offset(x = thirdWidth)
                .background(lineColor),
        )
        // Right vertical line (at 2/3 width)
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .offset(x = thirdWidth * 2f)
                .background(lineColor),
        )
        // Top horizontal line (at 1/3 height)
        Box(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .offset(y = thirdHeight)
                .background(lineColor),
        )
        // Bottom horizontal line (at 2/3 height)
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
    onExposureChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            value = (exposure + 10).toFloat() / 20f, // map -10..10 to 0..1
            onValueChange = { value ->
                onExposureChange((value * 20 - 10).toInt())
            },
            colors = SliderDefaults.colors(
                thumbColor = FitLogAccent,
                activeTrackColor = FitLogAccent,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$exposure",
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

// ── Photo Preview Screen ──────────────────────────────────────────────────────

@Composable
private fun PhotoPreviewScreen(
    photoUri: android.net.Uri,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black),
    ) {
        // Image preview using Coil
        coil.compose.AsyncImage(
            model = photoUri,
            contentDescription = "Photo preview",
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(3f / 4f),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )

        // Bottom controls
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
    // In a real app, this would request permissions via
    // rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
    // For now, show a placeholder message.
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

