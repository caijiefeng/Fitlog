package com.example.fitlog.feature.media

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.data.repository.MediaRecord
import com.example.fitlog.domain.media.MediaCategory
import com.example.fitlog.domain.media.MediaType
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ── Category label helpers ───────────────────────────────────────────────────

private fun categoryLabelRes(category: MediaCategory?): Int = when (category) {
    MediaCategory.BODY_PROGRESS -> R.string.media_category_body_progress
    MediaCategory.WORKOUT_FORM -> R.string.media_category_workout_form
    MediaCategory.MEAL -> R.string.media_category_meal
    MediaCategory.GENERAL -> R.string.media_category_general
    null -> R.string.media_category_all
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MediaLibraryScreen(
    viewModel: MediaLibraryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show export success snackbar
    LaunchedEffect(uiState.showBatchExportSuccess) {
        if (uiState.showBatchExportSuccess) {
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.media_export_success),
            )
            viewModel.dismissBatchExportSuccess()
        }
    }

    Scaffold(
        topBar = {
            if (uiState.isSelectMode) {
                // Selection mode top bar
                FitLogTopAppBar(
                    title = stringResource(R.string.media_select_count, uiState.selectedIds.size),
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val intent = viewModel.createBatchShareIntent(context)
                            if (intent != null) {
                                context.startActivity(
                                    Intent.createChooser(intent, null)
                                )
                            }
                        }) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = stringResource(R.string.media_batch_share),
                                tint = FitLogTextSecondary,
                            )
                        }
                        IconButton(onClick = {
                            viewModel.batchExportToGallery(context)
                        }) {
                            Icon(
                                Icons.Filled.SaveAlt,
                                contentDescription = stringResource(R.string.media_batch_export),
                                tint = FitLogTextSecondary,
                            )
                        }
                        IconButton(onClick = { viewModel.showBatchDeleteConfirmation() }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = FitLogError,
                            )
                        }
                    },
                )
            } else {
                FitLogTopAppBar(
                    title = stringResource(R.string.media_library_title),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                )
            }
        },
        containerColor = FitLogBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Filter chips row
            CategoryFilterRow(
                selectedCategory = uiState.selectedCategory,
                favoritesOnly = uiState.favoritesOnly,
                onCategorySelected = { viewModel.setCategory(it) },
                onFavoritesToggle = { viewModel.setFavoritesOnly(!uiState.favoritesOnly) },
            )

            // Content area
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = FitLogAccent)
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error ?: "",
                                color = FitLogError,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.action_retry),
                                color = FitLogAccent,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.clickable { viewModel.loadAll() },
                            )
                        }
                    }
                }
                uiState.groups.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.PhotoLibrary,
                        title = stringResource(R.string.media_empty_title),
                        subtitle = stringResource(R.string.media_empty_subtitle),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    PageContainer {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                        ) {
                            items(
                                items = uiState.groups,
                                key = { it.date.toEpochDay() },
                            ) { group ->
                                DateGroupHeader(label = group.label)
                                group.items.forEach { record ->
                                    MediaRowItem(
                                        record = record,
                                        viewModel = viewModel,
                                        isSelectMode = uiState.isSelectMode,
                                        isSelected = record.id in uiState.selectedIds,
                                        onClick = {
                                            if (uiState.isSelectMode) {
                                                viewModel.toggleSelection(record.id)
                                            } else {
                                                onNavigateToDetail(record.id)
                                            }
                                        },
                                        onLongClick = {
                                            if (!uiState.isSelectMode) {
                                                viewModel.enterSelectionMode(record.id)
                                            }
                                        },
                                        onFavoriteToggle = { viewModel.toggleFavorite(record) },
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }

    // Batch delete confirmation dialog
    if (uiState.showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBatchDeleteConfirmation() },
            title = { Text(stringResource(R.string.media_batch_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.media_batch_delete_message,
                        uiState.selectedIds.size,
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmBatchDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = FitLogError),
                    enabled = !uiState.batchDeleteInProgress,
                ) {
                    if (uiState.batchDeleteInProgress) {
                        CircularProgressIndicator(
                            color = FitLogBackground,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBatchDeleteConfirmation() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: MediaCategoryFilter,
    favoritesOnly: Boolean,
    onCategorySelected: (MediaCategoryFilter) -> Unit,
    onFavoritesToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MediaCategoryFilter.entries.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = when (category) {
                            MediaCategoryFilter.ALL -> stringResource(R.string.media_category_all)
                            MediaCategoryFilter.BODY_PROGRESS -> stringResource(R.string.media_category_body_progress)
                            MediaCategoryFilter.WORKOUT_FORM -> stringResource(R.string.media_category_workout_form)
                            MediaCategoryFilter.MEAL -> stringResource(R.string.media_category_meal)
                            MediaCategoryFilter.GENERAL -> stringResource(R.string.media_category_general)
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FitLogAccent,
                    selectedLabelColor = FitLogBackground,
                ),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onFavoritesToggle) {
            Icon(
                imageVector = if (favoritesOnly) Icons.Filled.Favorite
                else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(R.string.media_favorites_only),
                tint = if (favoritesOnly) FitLogError else FitLogTextSecondary,
            )
        }
    }
}

@Composable
private fun DateGroupHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = FitLogTextSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaRowItem(
    record: MediaRecord,
    viewModel: MediaLibraryViewModel,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    val file = remember(record.relativePath) {
        try {
            viewModel.resolveFile(record.relativePath)
        } catch (_: Exception) { null }
    }

    FitLogCard(
        onClick = null,
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Checkbox in select mode
            if (isSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = FitLogAccent,
                        uncheckedColor = FitLogTextSecondary,
                    ),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            // Thumbnail with Coil
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FitLogSurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (file != null && file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = if (record.mediaType == MediaType.VIDEO) {
                            stringResource(R.string.media_type_video)
                        } else {
                            stringResource(R.string.media_type_photo)
                        },
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    // Video overlay: play icon + duration
                    if (record.mediaType == MediaType.VIDEO) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.media_play_video),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }

                        // Duration badge
                        record.durationMillis?.let { duration ->
                            if (duration > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(
                                            Color.Black.copy(alpha = 0.6f),
                                            RoundedCornerShape(4.dp),
                                        )
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                ) {
                                    Text(
                                        text = formatDurationShort(duration),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Fallback icon when file not available
                    Icon(
                        imageVector = if (record.mediaType == MediaType.VIDEO) {
                            Icons.Filled.PlayArrow
                        } else {
                            Icons.Filled.PhotoLibrary
                        },
                        contentDescription = if (record.mediaType == MediaType.VIDEO) {
                            stringResource(R.string.media_type_video)
                        } else {
                            stringResource(R.string.media_type_photo)
                        },
                        tint = FitLogTextSecondary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatMediaDate(record.capturedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitLogTextPrimary,
                    fontWeight = FontWeight.Medium,
                )
                Row {
                    TypeBadge(
                        text = if (record.mediaType == MediaType.VIDEO) {
                            stringResource(R.string.media_type_video)
                        } else {
                            stringResource(R.string.media_type_photo)
                        },
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(categoryLabelRes(record.category)),
                        style = MaterialTheme.typography.bodySmall,
                        color = FitLogTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Favorite toggle (hidden in select mode)
            if (!isSelectMode) {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (record.isFavorite) Icons.Filled.Favorite
                        else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(R.string.action_favorite),
                        tint = if (record.isFavorite) FitLogError else FitLogTextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(FitLogAccent.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = FitLogAccent,
        )
    }
}

private fun formatDurationShort(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private val dateFormat = DateTimeFormatter.ofPattern("MM-dd HH:mm")

private fun formatMediaDate(timestamp: Long): String {
    val instant = java.time.Instant.ofEpochMilli(timestamp)
    val localDateTime = java.time.LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return localDateTime.format(dateFormat)
}
