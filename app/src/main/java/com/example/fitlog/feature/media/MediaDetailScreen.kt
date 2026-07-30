package com.example.fitlog.feature.media

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.data.repository.MediaRecord
import com.example.fitlog.data.repository.MediaRepository
import com.example.fitlog.domain.media.MediaCategory
import com.example.fitlog.domain.media.MediaType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    mediaId: Long,
    onNavigateBack: () -> Unit,
    viewModel: MediaDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(mediaId) {
        viewModel.load(mediaId)
    }

    // Handle delete navigation
    LaunchedEffect(uiState.deleteCompleted) {
        if (uiState.deleteCompleted) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("媒体详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // Favorite
                    uiState.record?.let { record ->
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                if (record.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "收藏",
                                tint = if (record.isFavorite) Color(0xFFFF4081) else Color.White,
                            )
                        }
                    }
                    // Share
                    uiState.record?.let { record ->
                        IconButton(onClick = {
                            shareMedia(context, record)
                        }) {
                            Icon(Icons.Filled.Share, "分享", tint = Color.White)
                        }
                    }
                    // Delete
                    IconButton(onClick = { viewModel.showDeleteConfirmation() }) {
                        Icon(Icons.Filled.Delete, "删除", tint = Color(0xFFEF5350))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = Color(0xFF1A1A2E),
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(uiState.error!!)
            uiState.record == null -> EmptyState(
                icon = Icons.Filled.Image,
                title = "媒体不存在",
                subtitle = "该媒体文件已被删除或无法访问",
            )
            else -> androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
                MediaDetailContent(
                    record = uiState.record,
                    isEditingNote = uiState.isEditingNote,
                    editNoteText = uiState.editNoteText,
                    onStartEditNote = { viewModel.startEditingNote() },
                    onNoteTextChange = { viewModel.updateEditNoteText(it) },
                    onSaveNote = { viewModel.saveNote() },
                    onCancelEditNote = { viewModel.cancelEditingNote() },
                )
            }
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirmation() },
            title = { Text("删除确认") },
            text = { Text("确定要删除此媒体文件吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                ) {
                    Text("删除", color = Color(0xFFEF5350))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirmation() }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
private fun ErrorState(message: String) {
    EmptyState(
        icon = Icons.Filled.Image,
        title = "加载失败",
        subtitle = message,
    )
}

@Composable
private fun MediaDetailContent(
    record: MediaRecord,
    isEditingNote: Boolean,
    editNoteText: String,
    onStartEditNote: () -> Unit,
    onNoteTextChange: (String) -> Unit,
    onSaveNote: () -> Unit,
    onCancelEditNote: () -> Unit,
) {
    val context = LocalContext.current
    val storageRoot = remember { context.getExternalFilesDir(null) }
    val file = storageRoot?.resolve(record.relativePath)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding),
    ) {
        // Media preview
        MediaPreview(
            record = record,
            file = file,
        )

        Spacer(Modifier.height(16.dp))

        // Info card
        FitLogCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Date
                InfoRow(
                    icon = Icons.Filled.DateRange,
                    label = "日期",
                    value = formatDate(record.capturedAt),
                )
                Spacer(Modifier.height(8.dp))

                // Category
                InfoRow(
                    icon = Icons.Filled.Info,
                    label = "分类",
                    value = categoryLabel(record.category),
                )
                Spacer(Modifier.height(8.dp))

                // Type
                InfoRow(
                    icon = if (record.mediaType == MediaType.PHOTO) Icons.Filled.Image else Icons.Filled.Videocam,
                    label = "类型",
                    value = if (record.mediaType == MediaType.PHOTO) "照片" else "视频",
                )
                if (record.mediaType == MediaType.VIDEO && record.durationMillis != null) {
                    Spacer(Modifier.height(8.dp))
                    InfoRow(
                        icon = Icons.Filled.PlayArrow,
                        label = "时长",
                        value = formatDurationDetail(record.durationMillis),
                    )
                }
                Spacer(Modifier.height(8.dp))

                // Size
                InfoRow(
                    icon = null,
                    label = "文件大小",
                    value = formatFileSize(record.sizeBytes),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Note editor
        FitLogCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.EditNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "备注",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(8.dp))

                if (isEditingNote) {
                    OutlinedTextField(
                        value = editNoteText,
                        onValueChange = onNoteTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("添加备注...") },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3A3A5C),
                            unfocusedBorderColor = Color(0xFF2A2A4A),
                            cursorColor = Color.White,
                        ),
                        minLines = 2,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onCancelEditNote) {
                            Text("取消", color = Color.White.copy(alpha = 0.7f))
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onSaveNote) {
                            Text("保存", color = Color(0xFF2196F3))
                        }
                    }
                } else {
                    Text(
                        text = record.note ?: "暂无备注",
                        color = if (record.note != null) Color.White else Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onStartEditNote,
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2196F3),
                        ),
                    ) {
                        Text(if (record.note != null) "编辑备注" else "添加备注")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MediaPreview(
    record: MediaRecord,
    file: File?,
) {
    if (file?.exists() != true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color(0xFF2A2A4A)),
            contentAlignment = Alignment.Center,
        ) {
            Text("媒体文件不可用", color = Color.Gray)
        }
        return
    }

    if (record.mediaType == MediaType.PHOTO) {
        // Photo with pinch-to-zoom
        var scale by remember { mutableFloatStateOf(1f) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(RoundedCornerShape(0.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                ),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = "照片预览",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        // Video — show thumbnail with play overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = "视频缩略图",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            // Play button overlay
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "播放",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }

            // Duration badge
            record.durationMillis?.let { ms ->
                Text(
                    text = formatDurationDetail(ms),
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            modifier = Modifier.width(60.dp),
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────────

private fun shareMedia(context: android.content.Context, record: MediaRecord) {
    try {
        val storageRoot = context.getExternalFilesDir(null) ?: return
        val file = storageRoot.resolve(record.relativePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val type = if (record.mediaType == MediaType.PHOTO) "image/*" else "video/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            setDataAndType(uri, type)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享媒体"))
    } catch (_: Exception) { }
}

private fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun categoryLabel(category: MediaCategory): String = when (category) {
    MediaCategory.BODY_PROGRESS -> "身体进度"
    MediaCategory.WORKOUT_FORM -> "动作姿势"
    MediaCategory.MEAL -> "饮食"
    MediaCategory.GENERAL -> "通用"
}

private fun formatDurationDetail(ms: Long): String {
    val totalSec = ms / 1000
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "%02d:%02d".format(mins, secs)
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}
