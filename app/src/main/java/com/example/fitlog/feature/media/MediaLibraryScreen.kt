package com.example.fitlog.feature.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.data.repository.MediaRecord
import com.example.fitlog.domain.media.MediaCategory
import com.example.fitlog.domain.media.MediaType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simplified media library — uses a plain [LazyColumn] (no nested grids,
 * no Coil) to avoid issues with image loading composables.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MediaLibraryScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCamera: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: MediaLibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("媒体库") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setFavoritesOnly(!uiState.favoritesOnly) }) {
                        Icon(
                            if (uiState.favoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "仅收藏",
                            tint = if (uiState.favoritesOnly) Color(0xFFFF4081) else Color.Gray,
                        )
                    }
                    IconButton(onClick = { viewModel.setSortNewestFirst(!uiState.sortNewestFirst) }) {
                        Icon(Icons.Filled.Sort, "排序")
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
            // Category filter chips
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MediaCategoryFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.selectedCategory == filter,
                        onClick = { viewModel.setCategory(filter) },
                        label = { Text(categoryFilterLabel(filter)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3A3A5C),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF2A2A4A),
                            labelColor = Color.White.copy(alpha = 0.7f),
                        ),
                    )
                }
            }

            // Content
            when {
                uiState.isLoading -> LoadingState()
                uiState.error != null -> ErrorState(uiState.error!!, onRetry = { viewModel.loadAll() })
                uiState.groups.isEmpty() -> EmptyMediaState()
                else -> MediaItemList(
                    groups = uiState.groups,
                    onItemClick = onNavigateToDetail,
                )
            }
        }
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
private fun ErrorState(message: String, onRetry: () -> Unit) {
    EmptyState(
        icon = Icons.Filled.PhotoLibrary,
        title = "加载失败",
        subtitle = message,
        actionLabel = "重试",
        onAction = onRetry,
    )
}

@Composable
private fun EmptyMediaState() {
    EmptyState(
        icon = Icons.Filled.PhotoLibrary,
        title = "还没有媒体文件",
        subtitle = "拍照或录像后，媒体文件将在这里显示",
    )
}

private fun categoryFilterLabel(filter: MediaCategoryFilter): String = when (filter) {
    MediaCategoryFilter.ALL -> "全部"
    MediaCategoryFilter.BODY_PROGRESS -> "身体进度"
    MediaCategoryFilter.WORKOUT_FORM -> "动作姿势"
    MediaCategoryFilter.MEAL -> "饮食"
    MediaCategoryFilter.GENERAL -> "通用"
}

// ── Media list (plain LazyColumn, no nested grids) ────────────────────────────────

@Composable
private fun MediaItemList(
    groups: List<MediaDateGroup>,
    onItemClick: (Long) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        groups.forEach { group ->
            item(key = "header_${group.date}") {
                Text(
                    text = group.label,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            items(group.items, key = { it.id }) { record ->
                MediaItemRow(
                    record = record,
                    onClick = { onItemClick(record.id) },
                )
            }
        }
    }
}

@Composable
private fun MediaItemRow(
    record: MediaRecord,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A4A)),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail placeholder icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (record.mediaType == MediaType.VIDEO) Color(0xFF3A3A5C)
                        else Color(0xFF2A2A4A),
                        RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (record.mediaType == MediaType.VIDEO)
                        Icons.Filled.Videocam else Icons.Filled.Image,
                    contentDescription = null,
                    tint = if (record.mediaType == MediaType.VIDEO)
                        Color(0xFF4FC3F7) else Color(0xFF81C784),
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.relativePath.substringAfterLast('/'),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatDate(record.capturedAt),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    // Category badge
                    Text(
                        text = categoryBadgeLabel(record.category),
                        color = Color(0xFF4FC3F7),
                        fontSize = 11.sp,
                    )
                }
            }

            // Type badge
            Box(
                modifier = Modifier
                    .background(
                        if (record.mediaType == MediaType.VIDEO) Color(0xFF3A3A5C)
                        else Color(0xFF2E7D32).copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = if (record.mediaType == MediaType.VIDEO) "视频" else "照片",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                )
            }

            // Duration for video
            if (record.mediaType == MediaType.VIDEO && record.durationMillis != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = formatDurationShort(record.durationMillis),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                )
            }

            // Favorite indicator
            if (record.isFavorite) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF4081),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────────

private fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatDurationShort(ms: Long): String {
    val totalSec = ms / 1000
    if (totalSec < 60) return "${totalSec}s"
    return "${totalSec / 60}m"
}

private fun categoryBadgeLabel(category: MediaCategory): String = when (category) {
    MediaCategory.BODY_PROGRESS -> "身体进度"
    MediaCategory.WORKOUT_FORM -> "动作姿势"
    MediaCategory.MEAL -> "饮食"
    MediaCategory.GENERAL -> "通用"
}
