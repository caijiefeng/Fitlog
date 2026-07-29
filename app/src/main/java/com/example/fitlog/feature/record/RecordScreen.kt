package com.example.fitlog.feature.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.model.WorkoutSession
import com.example.fitlog.core.model.WorkoutStatus
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecordScreen(
    viewModel: RecordViewModel = hiltViewModel(),
    onNavigateToWorkoutDetail: (Long) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { FitLogTopAppBar(title = stringResource(R.string.nav_record)) },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
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
