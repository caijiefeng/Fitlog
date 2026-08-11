package com.example.fitlog.feature.record

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogCardStyle
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.component.StarPageSceneBackground
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogType
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.StarScenePlacement
import com.example.fitlog.core.model.WorkoutSession
import com.example.fitlog.core.model.WorkoutStatus
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecordScreen(
    viewModel: RecordViewModel = hiltViewModel(),
    onNavigateToWorkoutDetail: (Long) -> Unit = {},
    onNavigateToNutrition: () -> Unit = {},
    onNavigateToBodyMeasurement: () -> Unit = {},
    onNavigateToMedia: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { FitLogTopAppBar(title = stringResource(R.string.nav_record)) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            StarPageSceneBackground(placement = StarScenePlacement.RECORD)
            PageContainer(modifier = Modifier.padding(innerPadding)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // 顶部分类 2×2：每个分类有图标和摘要，不再只是相同卡片
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RecordCategoryTile(
                                icon = Icons.Filled.FitnessCenter,
                                label = stringResource(R.string.record_entry_workout),
                                summary = stringResource(
                                    R.string.record_category_workout_summary,
                                    uiState.sessions.size,
                                ),
                                onClick = { /* Already on Record screen showing workouts */ },
                                modifier = Modifier.weight(1f),
                            )
                            RecordCategoryTile(
                                icon = Icons.Filled.Restaurant,
                                label = stringResource(R.string.record_entry_nutrition),
                                summary = stringResource(R.string.record_category_nutrition_summary),
                                onClick = onNavigateToNutrition,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RecordCategoryTile(
                                icon = Icons.Filled.MonitorWeight,
                                label = stringResource(R.string.record_entry_body),
                                summary = stringResource(R.string.record_category_body_summary),
                                onClick = onNavigateToBodyMeasurement,
                                modifier = Modifier.weight(1f),
                            )
                            RecordCategoryTile(
                                icon = Icons.Filled.PhotoLibrary,
                                label = stringResource(R.string.record_entry_media),
                                summary = stringResource(R.string.record_category_media_summary),
                                onClick = onNavigateToMedia,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(title = stringResource(R.string.section_recent_training))
                }

                when {
                    uiState.isLoading -> {
                        item {
                            CircularProgressIndicator(
                                color = FitLogAccent,
                                modifier = Modifier.padding(32.dp),
                            )
                        }
                    }
                    uiState.isEmpty -> {
                        item {
                            EmptyState(
                                icon = Icons.Filled.EditNote,
                                title = stringResource(R.string.empty_record_title),
                                subtitle = stringResource(R.string.empty_record_subtitle),
                            )
                        }
                    }
                    else -> {
                        items(uiState.sessions, key = { it.session.id }) { item ->
                            HistoryCard(
                                session = item.session,
                                volume = item.volume,
                                completedSetCount = item.completedSetCount,
                                exerciseCount = item.exerciseCount,
                                onClick = { onNavigateToWorkoutDetail(item.session.id) },
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader(title = stringResource(R.string.section_body_data))
                    Text(
                        stringResource(R.string.record_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FitLogTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    session: WorkoutSession,
    volume: Double,
    completedSetCount: Int,
    exerciseCount: Int,
    onClick: () -> Unit,
) {
    FitLogCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column {
            // Template name
            Text(
                text = session.templateNameSnapshot ?: "训练",
                style = MaterialTheme.typography.bodyLarge,
                color = FitLogTextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Date/time and status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDate(session),
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusLabel(session.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor(session.status),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem(
                    label = stringResource(R.string.record_duration),
                    value = formatDuration(session),
                )
                StatItem(
                    label = stringResource(R.string.record_exercise_count),
                    value = exerciseCount.toString(),
                )
                StatItem(
                    label = stringResource(R.string.record_completed_sets),
                    value = completedSetCount.toString(),
                )
                StatItem(
                    label = stringResource(R.string.record_volume),
                    value = formatVolume(volume),
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = FitLogTextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = FitLogTextSecondary,
        )
    }
}

private fun formatDate(session: WorkoutSession): String {
    val zdt = session.startTime.atZone(ZoneId.systemDefault())
    return zdt.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
}

private fun statusLabel(status: WorkoutStatus): String = when (status) {
    WorkoutStatus.COMPLETED -> "已完成"
    WorkoutStatus.PARTIALLY_COMPLETED -> "部分完成"
    WorkoutStatus.CANCELLED -> "已取消"
    else -> ""
}

@Composable
private fun statusColor(status: WorkoutStatus): androidx.compose.ui.graphics.Color = when (status) {
    WorkoutStatus.COMPLETED -> FitLogAccent
    WorkoutStatus.PARTIALLY_COMPLETED -> FitLogAccent
    WorkoutStatus.CANCELLED -> FitLogTextSecondary
    else -> FitLogTextSecondary
}

private fun formatDuration(session: WorkoutSession): String {
    val endTime = session.endTime ?: java.time.Instant.now()
    val seconds = Duration.between(session.startTime, endTime).seconds
    val minutes = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return if (minutes > 0) {
        "%d分%d秒".format(minutes, secs)
    } else {
        "%d秒".format(secs)
    }
}

private fun formatVolume(volume: Double): String {
    return if (volume >= 1000) {
        "%.0f".format(volume)
    } else if (volume > 0) {
        "%.1f".format(volume)
    } else {
        "-"
    }
}

@Composable
private fun RecordCategoryTile(
    icon: ImageVector,
    label: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FitLogCard(
        modifier = modifier,
        style = FitLogCardStyle.TONAL,
        onClick = onClick,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = FitLogAccent,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            style = FitLogType.body,
            color = FitLogTextPrimary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = summary,
            style = FitLogType.caption,
            color = FitLogTextSecondary,
            maxLines = 2,
        )
    }
}
