package com.example.fitlog.feature.body

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.fitlog.core.designsystem.theme.FitLogSuccess
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.data.repository.MediaRecord
import com.example.fitlog.domain.media.ProgressPose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressPhotoScreen(
    viewModel: ProgressPhotoViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            FitLogTopAppBar(
                title = stringResource(R.string.progress_photo_title),
            )
        },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.isCompareMode && uiState.comparison != null) {
                // Comparison view
                ComparisonView(
                    comparison = uiState.comparison!!,
                    onClose = { viewModel.clearSelection() },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
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
                            title = stringResource(R.string.progress_photo_empty_title),
                            subtitle = stringResource(R.string.progress_photo_empty_subtitle),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        PageContainer {
                            // Pose filter chips
                            PoseFilterRow(
                                selectedPose = uiState.selectedPose,
                                onPoseSelected = { viewModel.setPoseFilter(it) },
                            )

                            // Selection hint
                            if (uiState.selectedPhotoIds.isNotEmpty()) {
                                SelectionHint(
                                    count = uiState.selectedPhotoIds.size,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                            ) {
                                items(
                                    items = uiState.groups,
                                    key = { it.date.toEpochDay() },
                                ) { group ->
                                    DateHeader(label = group.label)
                                    group.items.forEach { record ->
                                        ProgressPhotoItem(
                                            record = record,
                                            isSelected = uiState.selectedPhotoIds.contains(record.id),
                                            selectionOrder = uiState.selectedPhotoIds.indexOf(record.id) + 1,
                                            onToggle = { viewModel.togglePhotoSelection(record.id) },
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
}

@Composable
private fun PoseFilterRow(
    selectedPose: ProgressPose?,
    onPoseSelected: (ProgressPose?) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = selectedPose == null,
                    onClick = { onPoseSelected(null) },
                    label = {
                        Text(
                            stringResource(R.string.media_category_all),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FitLogAccent,
                        selectedLabelColor = FitLogBackground,
                    ),
                )
            }
            items(ProgressPose.entries.toList()) { pose ->
                FilterChip(
                    selected = selectedPose == pose,
                    onClick = { onPoseSelected(if (selectedPose == pose) null else pose) },
                    label = {
                        Text(
                            text = poseLabel(pose),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FitLogAccent,
                        selectedLabelColor = FitLogBackground,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SelectionHint(count: Int, modifier: Modifier = Modifier) {
    Text(
        text = if (count < 2) {
            stringResource(R.string.progress_photo_select_hint)
        } else {
            stringResource(R.string.progress_photo_compare)
        },
        style = MaterialTheme.typography.bodySmall,
        color = FitLogAccent,
        modifier = modifier,
    )
}

@Composable
private fun DateHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = FitLogTextSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun ProgressPhotoItem(
    record: MediaRecord,
    isSelected: Boolean,
    selectionOrder: Int,
    onToggle: () -> Unit,
) {
    FitLogCard(
        onClick = onToggle,
        modifier = Modifier.then(
            if (isSelected) {
                Modifier.border(
                    width = 2.dp,
                    color = FitLogAccent,
                    shape = RoundedCornerShape(12.dp),
                )
            } else {
                Modifier
            }
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FitLogSurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Image,
                    contentDescription = null,
                    tint = FitLogTextSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.poseTag?.let { poseLabel(it) }
                        ?: stringResource(R.string.progress_photo_no_pose),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitLogTextPrimary,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = record.note ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                    maxLines = 1,
                )
            }

            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(FitLogAccent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$selectionOrder",
                        color = FitLogBackground,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ── Comparison View ───────────────────────────────────────────────────────────

@Composable
private fun ComparisonView(
    comparison: PhotoComparisonData,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.progress_photo_comparison),
                style = MaterialTheme.typography.titleMedium,
                color = FitLogTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.progress_photo_close_comparison),
                    tint = FitLogTextSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Side-by-side photos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Before (older)
            ComparisonPhotoCard(
                label = stringResource(R.string.progress_photo_before),
                record = comparison.photo1,
                measurement = comparison.measurement1,
                modifier = Modifier.weight(1f),
            )

            // After (newer)
            ComparisonPhotoCard(
                label = stringResource(R.string.progress_photo_after),
                record = comparison.photo2,
                measurement = comparison.measurement2,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Comparison stats
        FitLogCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.progress_photo_days_between) + ": " +
                        stringResource(R.string.progress_photo_days_format)
                            .replace("%d", comparison.daysBetween.toString()),
                style = MaterialTheme.typography.bodyMedium,
                color = FitLogTextPrimary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            comparison.weightChange?.let { change ->
                ComparisonStatRow(
                    label = stringResource(R.string.progress_photo_weight_change),
                    value = change,
                    unit = "kg",
                )
            }
            comparison.bodyFatChange?.let { change ->
                ComparisonStatRow(
                    label = stringResource(R.string.progress_photo_bf_change),
                    value = change,
                    unit = "%",
                )
            }
        }
    }
}

@Composable
private fun ComparisonPhotoCard(
    label: String,
    record: MediaRecord,
    measurement: com.example.fitlog.domain.body.BodyMeasurement?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Photo placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(12.dp))
                .background(FitLogSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Image,
                    contentDescription = null,
                    tint = FitLogTextSecondary,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = FitLogAccent,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.poseTag?.let { poseLabel(it) }
                        ?: stringResource(R.string.progress_photo_no_pose),
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Measurement data
        measurement?.let { meas ->
            meas.weightKg?.let {
                StatLine(label = stringResource(R.string.body_measurement_weight), value = "$it kg")
            }
            meas.bodyFatPercent?.let {
                StatLine(label = stringResource(R.string.body_measurement_bf), value = "$it%")
            }
            meas.waistCm?.let {
                StatLine(label = stringResource(R.string.body_measurement_waist), value = "$it cm")
            }
        } ?: Text(
            text = stringResource(R.string.media_no_measurement_data),
            style = MaterialTheme.typography.bodySmall,
            color = FitLogTextSecondary,
        )
    }
}

@Composable
private fun ComparisonStatRow(
    label: String,
    value: Double,
    unit: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = FitLogTextSecondary,
            modifier = Modifier.weight(1f),
        )
        val (sign, color) = if (value >= 0) Pair("+", FitLogSuccess) else Pair("", FitLogError)
        Text(
            text = "$sign%.1f $unit".format(value),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = FitLogTextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = FitLogTextPrimary,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun poseLabel(pose: ProgressPose): String {
    return when (pose) {
        ProgressPose.FRONT -> "正面"
        ProgressPose.SIDE_LEFT -> "左侧"
        ProgressPose.SIDE_RIGHT -> "右侧"
        ProgressPose.BACK -> "背面"
        ProgressPose.OTHER -> "其他"
    }
}
