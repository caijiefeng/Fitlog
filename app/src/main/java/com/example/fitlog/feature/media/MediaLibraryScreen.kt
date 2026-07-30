package com.example.fitlog.feature.media

import android.net.Uri
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sort
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.data.repository.MediaRecord
import com.example.fitlog.domain.media.MediaType
import java.io.File

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
                else -> MediaGrid(
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

@Composable
private fun categoryFilterLabel(filter: MediaCategoryFilter): String = when (filter) {
    MediaCategoryFilter.ALL -> "全部"
    MediaCategoryFilter.BODY_PROGRESS -> "身体进度"
    MediaCategoryFilter.WORKOUT_FORM -> "动作姿势"
    MediaCategoryFilter.MEAL -> "饮食"
    MediaCategoryFilter.GENERAL -> "通用"
}

// ── Media grid ────────────────────────────────────────────────────────────────────

@Composable
private fun MediaGrid(
    groups: List<MediaDateGroup>,
    onItemClick: (Long) -> Unit,
) {
    val context = LocalContext.current
    val storageRoot = remember {
        context.getExternalFilesDir(null)
    }

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

            // 3-column grid for each group
            item(key = "grid_${group.date}") {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(((group.items.size + 2) / 3) * 130.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    userScrollEnabled = false,
                ) {
                    items(group.items, key = { it.id }) { record ->
                        MediaThumbnail(
                            record = record,
                            storageRoot = storageRoot,
                            onClick = { onItemClick(record.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnail(
    record: MediaRecord,
    storageRoot: File?,
    onClick: () -> Unit,
) {
    val file = storageRoot?.let { root ->
        try { root.resolve(record.relativePath) } catch (_: Exception) { null }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
    ) {
        if (file?.exists() == true) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2A2A4A)),
                contentAlignment = Alignment.Center,
            ) {
                Text("?", color = Color.Gray, fontSize = 24.sp)
            }
        }

        // Video badge
        if (record.mediaType == MediaType.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                    record.durationMillis?.let { ms ->
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = formatDurationBadge(ms),
                            color = Color.White,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }

        // Favorite indicator
        if (record.isFavorite) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF4081),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(14.dp),
            )
        }
    }
}

private fun formatDurationBadge(ms: Long): String {
    val totalSec = ms / 1000
    if (totalSec < 60) return "${totalSec}s"
    return "${totalSec / 60}m"
}
