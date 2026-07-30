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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogSurface
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTextTertiary
import com.example.fitlog.core.model.ExerciseSession
import com.example.fitlog.core.model.SetRecord
import com.example.fitlog.core.model.SetType
import com.example.fitlog.core.model.WorkoutSession
import com.example.fitlog.core.model.WorkoutStatus
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    viewModel: WorkoutDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }

    // Navigate back when deleted
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.workout_detail_title), color = FitLogTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.workout_detail_back),
                            tint = FitLogTextPrimary,
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "更多操作",
                                tint = FitLogTextPrimary,
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("删除记录", color = FitLogError) },
                                onClick = {
                                    showMenu = false
                                    viewModel.showDeleteDialog()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = FitLogError)
                                },
                            )
                        }
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
            uiState.detail != null -> {
                DetailContent(
                    session = uiState.detail!!.session,
                    exercises = uiState.detail!!.exercises,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("删除训练记录") },
            text = {
                Text("删除这条训练记录？动作、组数和容量记录将一起删除，此操作无法撤销。")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSession() }) {
                    Text("删除", color = FitLogError)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun DetailContent(
    session: WorkoutSession,
    exercises: List<Pair<ExerciseSession, List<SetRecord>>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // ── Session Info ──────────────────────────────────────────────────
        SectionHeader(title = stringResource(R.string.workout_detail_session_info))

        FitLogCard {
            Text(
                text = session.templateNameSnapshot ?: stringResource(R.string.workout_execution_title),
                style = MaterialTheme.typography.titleMedium,
                color = FitLogTextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))

            DetailInfoRow(
                label = stringResource(R.string.workout_summary_duration),
                value = formatDuration(session),
            )
            DetailInfoRow(
                label = stringResource(R.string.workout_summary_completed_sets),
                value = exercises.sumOf { (_, sets) -> sets.count { it.completed } }.toString(),
            )
            DetailInfoRow(
                label = stringResource(R.string.workout_summary_total_volume),
                value = stringResource(
                    R.string.workout_summary_volume_format,
                    exercises.sumOf { (_, sets) ->
                        sets.filter { it.completed && it.weightKg != null && it.reps != null }
                            .sumOf { it.weightKg!! * it.reps!! }
                    },
                ),
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = FitLogDivider)
        Spacer(Modifier.height(4.dp))

        // ── Exercises ─────────────────────────────────────────────────────
        if (exercises.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.workout_detail_no_exercises),
                style = MaterialTheme.typography.bodyLarge,
                color = FitLogTextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            exercises.forEach { (exercise, sets) ->
                ExerciseDetailCard(exercise = exercise, sets = sets)
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ExerciseDetailCard(
    exercise: ExerciseSession,
    sets: List<SetRecord>,
) {
    FitLogCard {
        // Exercise header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.exerciseNameSnapshot,
                    style = MaterialTheme.typography.titleSmall,
                    color = FitLogTextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                if (exercise.isSkipped) {
                    Text(
                        text = stringResource(R.string.workout_detail_skipped),
                        style = MaterialTheme.typography.labelSmall,
                        color = FitLogTextTertiary,
                    )
                }
            }
        }

        if (exercise.isSkipped) return@FitLogCard

        Spacer(Modifier.height(8.dp))

        if (sets.isEmpty()) {
            Text(
                text = stringResource(R.string.workout_detail_no_exercises),
                style = MaterialTheme.typography.bodySmall,
                color = FitLogTextSecondary,
            )
            return@FitLogCard
        }

        // Set headers row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "",
                style = MaterialTheme.typography.labelSmall,
                color = FitLogTextTertiary,
                modifier = Modifier.width(36.dp),
            )
            Text(
                text = stringResource(R.string.set_type_warmup),
                style = MaterialTheme.typography.labelSmall,
                color = FitLogTextTertiary,
                modifier = Modifier.width(48.dp),
            )
            Text(
                text = stringResource(R.string.workout_execution_weight),
                style = MaterialTheme.typography.labelSmall,
                color = FitLogTextTertiary,
                modifier = Modifier.width(60.dp),
            )
            Text(
                text = stringResource(R.string.workout_execution_reps),
                style = MaterialTheme.typography.labelSmall,
                color = FitLogTextTertiary,
                modifier = Modifier.width(36.dp),
            )
            Text(
                text = stringResource(R.string.workout_execution_rpe),
                style = MaterialTheme.typography.labelSmall,
                color = FitLogTextTertiary,
                modifier = Modifier.width(36.dp),
            )
            Text(
                text = stringResource(R.string.workout_execution_rir),
                style = MaterialTheme.typography.labelSmall,
                color = FitLogTextTertiary,
                modifier = Modifier.width(28.dp),
            )
        }

        HorizontalDivider(color = FitLogDivider)

        // Set rows
        sets.forEach { setRecord ->
            SetDetailRow(setRecord)
            HorizontalDivider(color = FitLogDivider.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun SetDetailRow(setRecord: SetRecord) {
    Surface(
        color = if (setRecord.completed) FitLogSurface else FitLogSurfaceVariant,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Set number
            Text(
                text = stringResource(R.string.workout_execution_set_number, setRecord.setNumber),
                style = MaterialTheme.typography.bodySmall,
                color = FitLogAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp),
            )

            // Type
            Text(
                text = setTypeShortLabel(setRecord.setType),
                style = MaterialTheme.typography.bodySmall,
                color = FitLogTextSecondary,
                modifier = Modifier.width(48.dp),
            )

            // Weight
            Text(
                text = setRecord.weightKg?.let { formatWeight(it) } ?: "-",
                style = MaterialTheme.typography.bodySmall,
                color = FitLogTextPrimary,
                modifier = Modifier.width(60.dp),
            )

            // Reps
            Text(
                text = setRecord.reps?.toString() ?: "-",
                style = MaterialTheme.typography.bodySmall,
                color = FitLogTextPrimary,
                modifier = Modifier.width(36.dp),
            )

            // RPE
            Text(
                text = setRecord.rpe?.let { formatDecimal(it) } ?: "-",
                style = MaterialTheme.typography.bodySmall,
                color = FitLogTextSecondary,
                modifier = Modifier.width(36.dp),
            )

            // RIR
            Text(
                text = setRecord.rir?.toString() ?: "-",
                style = MaterialTheme.typography.bodySmall,
                color = FitLogTextSecondary,
                modifier = Modifier.width(28.dp),
            )
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = FitLogTextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = FitLogTextPrimary,
        )
    }
}

private fun setTypeShortLabel(type: SetType): String = when (type) {
    SetType.WARMUP -> "WU"
    SetType.WORKING -> "WK"
    SetType.DROP -> "DR"
    SetType.FAILURE -> "FL"
}

private fun formatWeight(weightKg: Double): String {
    return if (weightKg == weightKg.toLong().toDouble()) {
        weightKg.toLong().toString()
    } else {
        "%.1f".format(weightKg)
    }
}

private fun formatDecimal(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        "%.1f".format(value)
    }
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
