package com.example.fitlog.feature.body

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.data.repository.MediaRecord
import com.example.fitlog.domain.media.MediaType
import com.example.fitlog.domain.media.ProgressPose
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Progress photo screen — lists BODY_PROGRESS photos grouped by date.
 * Allows selecting two photos for a side-by-side comparison with
 * measurement diffs (days, weight, body fat, waist changes).
 *
 * Uses a plain [LazyColumn] (no nested LazyVerticalGrid) to avoid
 * Compose nested-scroll issues.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProgressPhotoScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit = {},
    viewModel: ProgressPhotoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("身体进度照片") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (uiState.selectedPhotoIds.size == 2) {
                        IconButton(onClick = {
                            viewModel.togglePhotoSelection(uiState.selectedPhotoIds.first())
                        }) {
                            Icon(Icons.Filled.Clear, "清除选择")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        containerColor = Color(0xFF1A1A2E),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Pose filter chips
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { viewModel.setPoseFilter(null) },
                    label = { Text("全部", fontSize = 13.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (uiState.selectedPose == null) Color(0xFF3A3A5C) else Color(0xFF2A2A4A),
                        labelColor = Color.White,
                    ),
                )
                ProgressPose.entries.forEach { pose ->
                    AssistChip(
                        onClick = { viewModel.setPoseFilter(pose) },
                        label = { Text(poseLabel(pose), fontSize = 13.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (uiState.selectedPose == pose) Color(0xFF3A3A5C) else Color(0xFF2A2A4A),
                            labelColor = Color.White,
                        ),
                    )
                }
            }

            // Compare mode hint
            if (uiState.selectedPhotoIds.size == 1) {
                Text(
                    text = "请再选择一张照片进行对比",
                    color = Color(0xFFFFA726),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            // Comparison view
            if (uiState.isCompareMode && uiState.comparison != null) {
                ComparisonView(
                    comparison = uiState.comparison!!,
                    onClear = { viewModel.clearSelection() },
                )
            } else {
                // Content
                when {
                    uiState.isLoading -> LoadingState()
                    uiState.error != null -> ErrorState(uiState.error!!)
                    uiState.groups.isEmpty() -> EmptyProgressPhotoState(onNavigateToCamera)
                    else -> ProgressPhotoList(
                        groups = uiState.groups,
                        selectedPhotoIds = uiState.selectedPhotoIds,
                        onPhotoClick = { viewModel.togglePhotoSelection(it) },
                    )
                }
            }
        }
    }
}

// ── States ─────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
private fun ErrorState(message: String) {
    EmptyState(
        icon = Icons.Filled.PhotoCamera,
        title = "加载失败",
        subtitle = message,
    )
}

@Composable
private fun EmptyProgressPhotoState(onNavigateToCamera: () -> Unit) {
    EmptyState(
        icon = Icons.Filled.PhotoCamera,
        title = "还没有身体进度照片",
        subtitle = "在记录身体测量时拍照，照片将按日期分组展示",
        actionLabel = "拍照",
        onAction = onNavigateToCamera,
    )
}

// ── Photo list (LazyColumn, no nested grids) ──────────────────────────────────────

@Composable
private fun ProgressPhotoList(
    groups: List<ProgressPhotoGroup>,
    selectedPhotoIds: Set<Long>,
    onPhotoClick: (Long) -> Unit,
) {
    val context = LocalContext.current
    val storageRoot = remember { context.getExternalFilesDir(null) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        groups.forEach { group ->
            item(key = "header_${group.date}") {
                Text(
                    text = group.label,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }

            // Render items in rows of 2 using a fixed Column (no nested LazyVerticalGrid)
            item(key = "grid_${group.date}") {
                PhotoGridChunk(
                    items = group.items,
                    selectedPhotoIds = selectedPhotoIds,
                    storageRoot = storageRoot,
                    onPhotoClick = onPhotoClick,
                )
            }
        }
    }
}

/**
 * Renders photos in a 2-column grid using [Row] composables.
 * This avoids the nested-LazyVerticalGrid problem entirely.
 */
@Composable
private fun PhotoGridChunk(
    items: List<MediaRecord>,
    selectedPhotoIds: Set<Long>,
    storageRoot: File?,
    onPhotoClick: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        var i = 0
        while (i < items.size) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // First item
                ProgressPhotoItem(
                    record = items[i],
                    isSelected = selectedPhotoIds.contains(items[i].id),
                    storageRoot = storageRoot,
                    onClick = { onPhotoClick(items[i].id) },
                    modifier = Modifier.weight(1f),
                )
                // Second item (if exists)
                if (i + 1 < items.size) {
                    ProgressPhotoItem(
                        record = items[i + 1],
                        isSelected = selectedPhotoIds.contains(items[i + 1].id),
                        storageRoot = storageRoot,
                        onClick = { onPhotoClick(items[i + 1].id) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    // Empty spacer to maintain alignment
                    Spacer(Modifier.weight(1f))
                }
            }
            i += 2
        }
    }
}

@Composable
private fun ProgressPhotoItem(
    record: MediaRecord,
    isSelected: Boolean,
    storageRoot: File?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val file = storageRoot?.resolve(record.relativePath)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSelected) Modifier.border(3.dp, Color(0xFFFFA726), RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
    ) {
        // Placeholder — replace with AsyncImage for real device testing
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2A2A4A)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatDateShort(record.capturedAt),
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }

        // Pose badge
        record.poseTag?.let { pose ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    poseLabel(pose),
                    color = Color.White,
                    fontSize = 11.sp,
                )
            }
        }

        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(Color(0xFFFFA726), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "已选择",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ── Comparison view ────────────────────────────────────────────────────────────────

@Composable
private fun ComparisonView(
    comparison: PhotoComparisonData,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CompareArrows,
                    contentDescription = null,
                    tint = Color(0xFFFFA726),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("对比视图", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Clear, "关闭对比", tint = Color.White.copy(alpha = 0.7f))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Side-by-side photos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ComparisonPhoto(
                record = comparison.photo1,
                label = "之前",
                date = formatDateShort(comparison.photo1.capturedAt),
                modifier = Modifier.weight(1f),
            )
            ComparisonPhoto(
                record = comparison.photo2,
                label = "之后",
                date = formatDateShort(comparison.photo2.capturedAt),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Stats card
        FitLogCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Days between
                ComparisonStatRow(
                    label = "间隔天数",
                    value = "${comparison.daysBetween} 天",
                    delta = null,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.White.copy(alpha = 0.1f),
                )

                // Weight change
                comparison.weightChange?.let { delta ->
                    ComparisonStatRow(
                        label = "体重变化",
                        value = "%.1f kg".format(delta),
                        delta = delta,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color.White.copy(alpha = 0.1f),
                    )
                }

                // Body fat change
                comparison.bodyFatChange?.let { delta ->
                    ComparisonStatRow(
                        label = "体脂变化",
                        value = "%.1f%%".format(delta),
                        delta = delta,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color.White.copy(alpha = 0.1f),
                    )
                }

                // Waist change
                comparison.waistChange?.let { delta ->
                    ComparisonStatRow(
                        label = "腰围变化",
                        value = "%.1f cm".format(delta),
                        delta = delta,
                    )
                }

                if (comparison.weightChange == null &&
                    comparison.bodyFatChange == null &&
                    comparison.waistChange == null
                ) {
                    Text(
                        "所选日期无身体测量数据",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonPhoto(
    record: MediaRecord,
    label: String,
    date: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A2A4A)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "照片预览\n需真机测试",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )

            // Label badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Date
            Text(
                text = date,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun ComparisonStatRow(
    label: String,
    value: String,
    delta: Double?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
        )
        Text(
            text = value,
            color = if (delta != null) {
                if (delta < 0) Color(0xFF66BB6A)
                else if (delta > 0) Color(0xFFEF5350)
                else Color.White
            } else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────────

private fun poseLabel(pose: ProgressPose): String = when (pose) {
    ProgressPose.FRONT -> "正面"
    ProgressPose.SIDE_LEFT -> "左侧"
    ProgressPose.SIDE_RIGHT -> "右侧"
    ProgressPose.BACK -> "背面"
    ProgressPose.OTHER -> "其他"
}

private fun formatDateShort(millis: Long): String {
    val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
    return sdf.format(Date(millis))
}
