package com.example.fitlog.feature.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.fitlog.domain.media.MediaType
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaLibraryScreen(
    viewModel: MediaLibraryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            FitLogTopAppBar(
                title = stringResource(R.string.media_library_title),
            )
        },
        containerColor = FitLogBackground,
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
                                        onClick = { onNavigateToDetail(record.id) },
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

@Composable
private fun MediaRowItem(
    record: MediaRecord,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    FitLogCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FitLogSurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (record.mediaType == MediaType.VIDEO) {
                    Icon(
                        Icons.Filled.Videocam,
                        contentDescription = stringResource(R.string.media_type_video),
                        tint = FitLogAccent,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = stringResource(R.string.media_type_photo),
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
                        text = record.category.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = FitLogTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Favorite toggle
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

private val dateFormat = DateTimeFormatter.ofPattern("MM-dd HH:mm")

private fun formatMediaDate(timestamp: Long): String {
    val instant = java.time.Instant.ofEpochMilli(timestamp)
    val localDateTime = java.time.LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return localDateTime.format(dateFormat)
}
