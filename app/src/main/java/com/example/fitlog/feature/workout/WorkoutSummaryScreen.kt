package com.example.fitlog.feature.workout

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogDivider
import com.example.fitlog.core.designsystem.theme.FitLogSurface
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTextTertiary
import com.example.fitlog.core.model.WorkoutStatus
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    viewModel: WorkoutSummaryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.workout_summary_title), color = FitLogTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.workout_summary_back),
                            tint = FitLogTextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FitLogSurface),
            )
        },
        containerColor = FitLogBackground,
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FitLogAccent)
                }
            }
            uiState.error != null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(uiState.error ?: "", color = FitLogTextSecondary)
                }
            }
            uiState.session != null -> {
                SummaryContent(
                    uiState = uiState,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun SummaryContent(
    uiState: WorkoutSummaryUiState,
    modifier: Modifier = Modifier,
) {
    val session = uiState.session ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Template name
        Text(
            text = session.templateNameSnapshot ?: stringResource(R.string.workout_execution_title),
            style = MaterialTheme.typography.headlineSmall,
            color = FitLogTextPrimary,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(4.dp))

        // Date and status row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatSessionDate(session),
                style = MaterialTheme.typography.bodyMedium,
                color = FitLogTextSecondary,
            )
            Spacer(Modifier.width(12.dp))
            StatusBadge(session.status)
        }

        Spacer(Modifier.height(16.dp))

        // Duration
        val durationSeconds = session.endTime?.let { end ->
            Duration.between(session.startTime, end).seconds
        } ?: Duration.between(session.startTime, java.time.Instant.now()).seconds

        StatRow(
            label = stringResource(R.string.workout_summary_duration),
            value = formatDurationText(durationSeconds),
        )

        Spacer(Modifier.height(12.dp))

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label = stringResource(R.string.workout_summary_exercise_count),
                value = uiState.exerciseCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.workout_summary_completed_sets),
                value = uiState.completedSetCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.workout_summary_total_volume),
                value = stringResource(R.string.workout_summary_volume_format, uiState.totalVolume),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = FitLogDivider)
        Spacer(Modifier.height(4.dp))

        // Per-exercise summary
        SectionHeader(title = stringResource(R.string.workout_summary_per_exercise))

        uiState.exerciseSummaries.forEach { summary ->
            ExerciseSummaryRow(summary)
            Spacer(Modifier.height(6.dp))
        }

        // Bottom padding
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun StatusBadge(status: WorkoutStatus) {
    val (label, color) = when (status) {
        WorkoutStatus.COMPLETED -> stringResource(R.string.workout_summary_status_completed) to FitLogAccent
        WorkoutStatus.PARTIALLY_COMPLETED -> stringResource(R.string.workout_summary_status_partial) to FitLogAccent
        WorkoutStatus.CANCELLED -> stringResource(R.string.workout_summary_status_cancelled) to com.example.fitlog.core.designsystem.theme.FitLogError
        else -> "" to FitLogTextTertiary
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FitLogTextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = FitLogTextPrimary)
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    FitLogCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = FitLogAccent,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = FitLogTextSecondary,
            )
        }
    }
}

@Composable
private fun ExerciseSummaryRow(summary: ExerciseCompletionSummary) {
    FitLogCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = FitLogTextPrimary,
                )
                if (summary.isSkipped) {
                    Text(
                        text = stringResource(R.string.workout_summary_skipped),
                        style = MaterialTheme.typography.labelSmall,
                        color = FitLogTextTertiary,
                    )
                } else {
                    Text(
                        text = stringResource(
                            R.string.workout_summary_set_progress,
                            summary.completedSets,
                            summary.targetSets,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = FitLogTextSecondary,
                    )
                }
            }
        }
    }
}

private fun formatSessionDate(session: com.example.fitlog.core.model.WorkoutSession): String {
    val zdt = session.startTime.atZone(ZoneId.systemDefault())
    return zdt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
}

private fun formatDurationText(totalSeconds: Long): String {
    val minutes = (totalSeconds / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()
    return if (minutes > 0) {
        "%d分%d秒".format(minutes, seconds)
    } else {
        "%d秒".format(seconds)
    }
}
