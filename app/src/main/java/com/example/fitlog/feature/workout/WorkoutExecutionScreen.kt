package com.example.fitlog.feature.workout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogSurface
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.model.ExerciseSession
import com.example.fitlog.core.model.SetRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExecutionScreen(
    viewModel: WorkoutExecutionViewModel = hiltViewModel(),
    onNavigateToSummary: (Long) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WorkoutExecutionEvent.NavigateToSummary -> onNavigateToSummary(event.sessionId)
                is WorkoutExecutionEvent.NavigateBack -> onNavigateBack()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.sessionDetail?.session?.templateNameSnapshot ?: "训练",
                        color = FitLogTextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.showCancelDialog() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "退出", tint = FitLogTextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.showCompleteDialog() }) {
                        Text("完成", color = FitLogAccent)
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
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!, color = FitLogTextSecondary)
                }
            }
            uiState.sessionDetail != null -> {
                val detail = uiState.sessionDetail!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                ) {
                    // Rest timer
                    if (uiState.restTimerState.isRunning || uiState.restTimerState.isFinished) {
                        item {
                            RestTimerBar(
                                state = uiState.restTimerState,
                                onTick = { viewModel.tickRest() },
                                onSkip = { viewModel.skipRest() },
                                onAdd15 = { viewModel.add15Seconds() },
                                onSubtract15 = { viewModel.subtract15Seconds() },
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    // Exercises
                    itemsIndexed(detail.exercises) { idx, pair ->
                        val (exercise, sets) = pair
                        ExerciseSetCard(
                            exercise = exercise,
                            sets = sets,
                            onCompleteSet = { setId, reps, weight, rpe, rir ->
                                viewModel.completeSet(exercise.id, setId, reps, weight, rpe, rir)
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Complete dialog
    if (uiState.showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCompleteDialog() },
            title = { Text("完成训练", color = FitLogTextPrimary) },
            text = { Text("确认完成本次训练？", color = FitLogTextSecondary) },
            confirmButton = {
                Button(
                    onClick = { viewModel.completeWorkout() },
                    enabled = !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                ) { Text("确认完成") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCompleteDialog() }) {
                    Text("继续训练", color = FitLogAccent)
                }
            },
        )
    }

    // Cancel dialog
    if (uiState.showCancelDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCancelDialog() },
            title = { Text("退出训练", color = FitLogTextPrimary) },
            text = { Text("退出训练将取消本次训练记录。", color = FitLogTextSecondary) },
            confirmButton = {
                Button(
                    onClick = { viewModel.cancelWorkout() },
                    colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                ) { Text("取消训练") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCancelDialog() }) {
                    Text("继续训练", color = FitLogAccent)
                }
            },
        )
    }
}

@Composable
private fun RestTimerBar(
    state: RestTimerState,
    onTick: () -> Unit,
    onSkip: () -> Unit,
    onAdd15: () -> Unit,
    onSubtract15: () -> Unit,
) {
    FitLogCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (state.isFinished) "休息结束" else "休息中",
                style = MaterialTheme.typography.titleMedium,
                color = FitLogAccent,
            )
            Text(
                "${state.remainingSeconds}s",
                style = MaterialTheme.typography.headlineLarge,
                color = FitLogTextPrimary,
            )
            Row {
                TextButton(onClick = onSubtract15) { Text("-15s", color = FitLogAccent) }
                TextButton(onClick = onSkip) { Text("跳过", color = FitLogAccent) }
                TextButton(onClick = onAdd15) { Text("+15s", color = FitLogAccent) }
            }
        }
    }
}

@Composable
private fun ExerciseSetCard(
    exercise: ExerciseSession,
    sets: List<SetRecord>,
    onCompleteSet: (Long, Int?, Double?, Double?, Int?) -> Unit,
) {
    FitLogCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(exercise.exerciseNameSnapshot, style = MaterialTheme.typography.titleSmall, color = FitLogTextPrimary)
            Text("目标: ${exercise.targetSets}组", style = MaterialTheme.typography.bodySmall, color = FitLogTextSecondary)
            Spacer(Modifier.height(8.dp))

            sets.forEach { set ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("#${set.setNumber}", color = if (set.completed) FitLogAccent else FitLogTextSecondary, modifier = Modifier.width(28.dp))
                    Text(
                        if (set.completed) "${set.reps ?: "-"}次 ${set.weightKg ?: "-"}kg" else "未完成",
                        color = FitLogTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
