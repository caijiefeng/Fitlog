package com.example.fitlog.feature.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogDivider
import com.example.fitlog.core.designsystem.theme.FitLogError
import com.example.fitlog.core.designsystem.theme.FitLogSuccess
import com.example.fitlog.core.designsystem.theme.FitLogSurface
import com.example.fitlog.core.designsystem.theme.FitLogSurfaceVariant
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogTextTertiary
import com.example.fitlog.core.model.ExerciseSession
import com.example.fitlog.core.model.SetRecord
import com.example.fitlog.core.model.SetType
import kotlinx.coroutines.delay

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
                        uiState.sessionDetail?.session?.templateNameSnapshot
                            ?: stringResource(R.string.workout_execution_title),
                        color = FitLogTextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.showCancelDialog() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.workout_execution_exit),
                            tint = FitLogTextPrimary,
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.showCompleteDialog() },
                        enabled = !uiState.isSaving,
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = FitLogAccent,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                stringResource(R.string.workout_execution_complete),
                                color = FitLogAccent,
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
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        uiState.error ?: "",
                        color = FitLogTextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            uiState.sessionDetail != null -> {
                WorkoutContent(
                    detail = uiState.sessionDetail!!,
                    restTimerState = uiState.restTimerState,
                    onTickRest = viewModel::tickRest,
                    onSkipRest = viewModel::skipRest,
                    onAdd15Seconds = viewModel::add15Seconds,
                    onSubtract15Seconds = viewModel::subtract15Seconds,
                    onCompleteSet = { exerciseSessionId, setRecordId, reps, weightKg, rpe, rir, setType ->
                        viewModel.completeSet(
                            sessionId = uiState.sessionDetail!!.session.id,
                            exerciseSessionId = exerciseSessionId,
                            setRecordId = setRecordId,
                            reps = reps,
                            weightKg = weightKg,
                            rpe = rpe,
                            rir = rir,
                            setType = setType,
                        )
                    },
                    onAddSet = viewModel::addSet,
                    onDeleteSet = viewModel::deleteSet,
                    onUpdateSetType = viewModel::updateSetType,
                    onSkipExercise = viewModel::skipExercise,
                    onUpdateNotes = viewModel::updateNotes,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }

    // Complete Workout confirmation dialog
    if (uiState.showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCompleteDialog() },
            title = {
                Text(
                    stringResource(R.string.workout_execution_complete_title),
                    color = FitLogTextPrimary,
                )
            },
            text = {
                Text(
                    stringResource(R.string.workout_execution_complete_message),
                    color = FitLogTextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.completeWorkout() },
                    enabled = !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                ) {
                    Text(stringResource(R.string.workout_execution_confirm_complete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCompleteDialog() }) {
                    Text(
                        stringResource(R.string.workout_execution_dismiss),
                        color = FitLogAccent,
                    )
                }
            },
        )
    }

    // Cancel Workout confirmation dialog
    if (uiState.showCancelDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCancelDialog() },
            title = {
                Text(
                    stringResource(R.string.workout_execution_cancel_title),
                    color = FitLogTextPrimary,
                )
            },
            text = {
                Text(
                    stringResource(R.string.workout_execution_cancel_message),
                    color = FitLogTextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.cancelWorkout() },
                    enabled = !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = FitLogError),
                ) {
                    Text(stringResource(R.string.workout_execution_confirm_cancel))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCancelDialog() }) {
                    Text(
                        stringResource(R.string.workout_execution_dismiss),
                        color = FitLogAccent,
                    )
                }
            },
        )
    }
}

// ─── Content ───────────────────────────────────────────────────────────────────

@Composable
private fun WorkoutContent(
    detail: com.example.fitlog.core.model.WorkoutSessionDetail,
    restTimerState: RestTimerState,
    onTickRest: () -> Unit,
    onSkipRest: () -> Unit,
    onAdd15Seconds: () -> Unit,
    onSubtract15Seconds: () -> Unit,
    onCompleteSet: (Long, Long, Int?, Double?, Double?, Int?, SetType) -> Unit,
    onAddSet: (Long) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onUpdateSetType: (Long, SetType) -> Unit,
    onSkipExercise: (Long) -> Unit,
    onUpdateNotes: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Track which completed sets are in edit mode (keyed by setRecordId)
    val editingSets = remember { mutableStateMapOf<Long, Boolean>() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        // Rest timer bar (shown when active)
        if (restTimerState.isRunning || restTimerState.isFinished) {
            item(key = "rest_timer") {
                RestTimerBar(
                    state = restTimerState,
                    onTick = onTickRest,
                    onSkip = onSkipRest,
                    onAdd15 = onAdd15Seconds,
                    onSubtract15 = onSubtract15Seconds,
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        // Exercise cards
        itemsIndexed(
            items = detail.exercises,
            key = { _, pair -> pair.first.id },
        ) { _, (exercise, sets) ->
            ExerciseCard(
                exercise = exercise,
                sets = sets,
                editingSets = editingSets,
                onCompleteSet = onCompleteSet,
                onAddSet = onAddSet,
                onDeleteSet = onDeleteSet,
                onUpdateSetType = onUpdateSetType,
                onSkipExercise = onSkipExercise,
                onUpdateNotes = onUpdateNotes,
                onToggleEdit = { setId ->
                    editingSets[setId] = !(editingSets[setId] ?: false)
                },
            )
            Spacer(Modifier.height(8.dp))
        }

        // Bottom padding for navigation bar
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─── Rest Timer Bar ────────────────────────────────────────────────────────────

@Composable
private fun RestTimerBar(
    state: RestTimerState,
    onTick: () -> Unit,
    onSkip: () -> Unit,
    onAdd15: () -> Unit,
    onSubtract15: () -> Unit,
) {
    // Auto-tick every second while running
    LaunchedEffect(state.isRunning, state.isFinished) {
        if (state.isRunning && !state.isFinished) {
            while (true) {
                onTick()
                delay(1000)
            }
        }
    }

    FitLogCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (state.isFinished) {
                    stringResource(R.string.workout_execution_rest_finished)
                } else {
                    stringResource(R.string.workout_execution_rest_timer)
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (state.isFinished) FitLogSuccess else FitLogAccent,
            )

            Text(
                text = formatSeconds(state.remainingSeconds),
                style = MaterialTheme.typography.headlineLarge,
                color = FitLogTextPrimary,
            )

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSubtract15,
                    enabled = state.isRunning && !state.isFinished,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FitLogAccent),
                    border = BorderStroke(1.dp, FitLogAccent.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        stringResource(R.string.workout_execution_rest_subtract),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                OutlinedButton(
                    onClick = onSkip,
                    enabled = state.isRunning && !state.isFinished,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FitLogAccent),
                    border = BorderStroke(1.dp, FitLogAccent.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        stringResource(R.string.workout_execution_skip_rest),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                OutlinedButton(
                    onClick = onAdd15,
                    enabled = state.isRunning && !state.isFinished,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FitLogAccent),
                    border = BorderStroke(1.dp, FitLogAccent.copy(alpha = 0.3f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        stringResource(R.string.workout_execution_rest_add),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

// ─── Exercise Card ─────────────────────────────────────────────────────────────

@Composable
private fun ExerciseCard(
    exercise: ExerciseSession,
    sets: List<SetRecord>,
    editingSets: MutableMap<Long, Boolean>,
    onCompleteSet: (Long, Long, Int?, Double?, Double?, Int?, SetType) -> Unit,
    onAddSet: (Long) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onUpdateSetType: (Long, SetType) -> Unit,
    onSkipExercise: (Long) -> Unit,
    onUpdateNotes: (Long, String) -> Unit,
    onToggleEdit: (Long) -> Unit,
) {
    var notesText by remember(exercise.id, exercise.notes) {
        mutableStateOf(exercise.notes ?: "")
    }

    FitLogCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Header ────────────────────────────────────────────────────────
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
                    TargetInfoLine(exercise)
                }

                if (exercise.isSkipped) {
                    Surface(
                        color = FitLogSurfaceVariant,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            text = stringResource(R.string.workout_execution_exercise_skipped),
                            style = MaterialTheme.typography.labelSmall,
                            color = FitLogTextTertiary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            if (exercise.isSkipped) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onSkipExercise(exercise.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FitLogAccent),
                ) {
                    Text(stringResource(R.string.workout_execution_unskip_exercise))
                }
                return@Column
            }

            Spacer(Modifier.height(12.dp))

            // ── Set Rows ──────────────────────────────────────────────────────
            sets.forEach { setRecord ->
                val isEditing = !setRecord.completed || (editingSets[setRecord.id] ?: false)

                SetRow(
                    setRecord = setRecord,
                    isEditing = isEditing,
                    onCompleteSet = { reps, weightKg, rpe, rir, setType ->
                        onCompleteSet(
                            exercise.id,
                            setRecord.id,
                            reps,
                            weightKg,
                            rpe,
                            rir,
                            setType,
                        )
                        // Exit edit mode after saving a previously completed set
                        if (setRecord.completed) {
                            editingSets.remove(setRecord.id)
                        }
                    },
                    onDelete = if (!setRecord.completed) {
                        { onDeleteSet(setRecord.id) }
                    } else {
                        null
                    },
                    onToggleEdit = if (setRecord.completed) {
                        { onToggleEdit(setRecord.id) }
                    } else {
                        null
                    },
                    onUpdateSetType = { type -> onUpdateSetType(setRecord.id, type) },
                )
                Spacer(Modifier.height(6.dp))
            }

            // ── Add Set button ────────────────────────────────────────────────
            TextButton(
                onClick = { onAddSet(exercise.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = FitLogAccent,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.workout_execution_add_set),
                    color = FitLogAccent,
                )
            }

            // ── Skip Exercise button ──────────────────────────────────────────
            TextButton(
                onClick = { onSkipExercise(exercise.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = FitLogTextSecondary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.workout_execution_skip_exercise),
                    color = FitLogTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // ── Notes Field ───────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notesText,
                onValueChange = { newValue ->
                    notesText = newValue
                    onUpdateNotes(exercise.id, newValue)
                },
                label = {
                    Text(
                        stringResource(R.string.workout_execution_notes),
                        color = FitLogTextSecondary,
                    )
                },
                placeholder = {
                    Text(
                        stringResource(R.string.workout_execution_notes_placeholder),
                        color = FitLogTextTertiary,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                textStyle = MaterialTheme.typography.bodySmall,
                minLines = 1,
                maxLines = 3,
                shape = MaterialTheme.shapes.small,
            )
        }
    }
}

// ─── Target Info Line ──────────────────────────────────────────────────────────

@Composable
private fun TargetInfoLine(exercise: ExerciseSession) {
    val parts = mutableListOf<String>()

    if (exercise.targetSets > 0) {
        parts.add(stringResource(R.string.workout_execution_target_sets, exercise.targetSets))
    }
    if (exercise.targetRepsMin != null && exercise.targetRepsMax != null) {
        parts.add(
            stringResource(
                R.string.workout_execution_target_reps_range,
                exercise.targetRepsMin,
                exercise.targetRepsMax,
            )
        )
    } else if (exercise.targetRepsMin != null) {
        parts.add(stringResource(R.string.workout_execution_target_reps_min, exercise.targetRepsMin))
    }
    if (exercise.targetWeightKg != null) {
        parts.add(stringResource(R.string.workout_execution_target_weight, exercise.targetWeightKg))
    }
    if (exercise.targetRpe != null) {
        val rpeLabel = stringResource(R.string.workout_execution_target_rpe_label)
        parts.add("$rpeLabel ${exercise.targetRpe}")
    }
    if (exercise.targetRir != null) {
        val rirLabel = stringResource(R.string.workout_execution_target_rir_label)
        parts.add("$rirLabel ${exercise.targetRir}")
    }
    if (exercise.plannedRestSeconds > 0) {
        val restLabel = stringResource(R.string.workout_execution_target_rest_label)
        parts.add("$restLabel ${exercise.plannedRestSeconds}s")
    }

    if (parts.isNotEmpty()) {
        Text(
            text = parts.joinToString(" | "),
            style = MaterialTheme.typography.bodySmall,
            color = FitLogTextSecondary,
        )
    }
}

// ─── Set Row (delegates to editable or read-only) ──────────────────────────────

@Composable
private fun SetRow(
    setRecord: SetRecord,
    isEditing: Boolean,
    onCompleteSet: (Int?, Double?, Double?, Int?, SetType) -> Unit,
    onDelete: (() -> Unit)?,
    onToggleEdit: (() -> Unit)?,
    onUpdateSetType: (SetType) -> Unit,
) {
    if (isEditing) {
        EditableSetRow(
            setRecord = setRecord,
            onCompleteSet = onCompleteSet,
            onDelete = onDelete,
            onUpdateSetType = onUpdateSetType,
        )
    } else {
        CompletedSetRow(
            setRecord = setRecord,
            onToggleEdit = onToggleEdit,
        )
    }
}

// ─── Editable Set Row ──────────────────────────────────────────────────────────

@Composable
private fun EditableSetRow(
    setRecord: SetRecord,
    onCompleteSet: (Int?, Double?, Double?, Int?, SetType) -> Unit,
    onDelete: (() -> Unit)?,
    onUpdateSetType: (SetType) -> Unit,
) {
    var weightText by remember(setRecord.id, setRecord.weightKg) {
        mutableStateOf(setRecord.weightKg?.formatDecimal() ?: "")
    }
    var repsText by remember(setRecord.id, setRecord.reps) {
        mutableStateOf(setRecord.reps?.toString() ?: "")
    }
    var rpeText by remember(setRecord.id, setRecord.rpe) {
        mutableStateOf(setRecord.rpe?.formatDecimal() ?: "")
    }
    var rirText by remember(setRecord.id, setRecord.rir) {
        mutableStateOf(setRecord.rir?.toString() ?: "")
    }

    var selectedType by remember(setRecord.id, setRecord.setType) {
        mutableStateOf(setRecord.setType)
    }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    // Validation
    val weightError = validateWeight(weightText)
    val repsError = validateReps(repsText)
    val rpeError = validateRpe(rpeText)
    val rirError = validateRir(rirText)
    val hasError = weightError != null || repsError != null || rpeError != null || rirError != null

    Surface(
        color = FitLogSurfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Row 1: Set number, Type dropdown, Delete button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Set number
                Text(
                    text = stringResource(R.string.workout_execution_set_number, setRecord.setNumber),
                    style = MaterialTheme.typography.labelLarge,
                    color = FitLogAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                )

                // Set type dropdown
                Box {
                    OutlinedButton(
                        onClick = { typeMenuExpanded = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = FitLogTextPrimary,
                        ),
                        border = BorderStroke(1.dp, FitLogDivider),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = getSetTypeDisplayName(selectedType),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                    ) {
                        SetType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = getSetTypeDisplayName(type),
                                        color = FitLogTextPrimary,
                                    )
                                },
                                onClick = {
                                    selectedType = type
                                    typeMenuExpanded = false
                                    onUpdateSetType(type)
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Delete button (only for incomplete extra sets)
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.workout_execution_delete_set),
                            tint = FitLogError,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Row 2: Weight, Reps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NumericField(
                    value = weightText,
                    onValueChange = { weightText = filterDecimalInput(it) },
                    label = stringResource(R.string.workout_execution_weight),
                    placeholder = "0",
                    isError = weightError != null,
                    errorMessage = weightError,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                NumericField(
                    value = repsText,
                    onValueChange = { repsText = filterDigitInput(it) },
                    label = stringResource(R.string.workout_execution_reps),
                    placeholder = "0",
                    isError = repsError != null,
                    errorMessage = repsError,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(4.dp))

            // Row 3: RPE, RIR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NumericField(
                    value = rpeText,
                    onValueChange = { rpeText = filterDecimalInput(it) },
                    label = stringResource(R.string.workout_execution_rpe),
                    placeholder = stringResource(R.string.workout_execution_rpe_hint),
                    isError = rpeError != null,
                    errorMessage = rpeError,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                NumericField(
                    value = rirText,
                    onValueChange = { rirText = filterDigitInput(it) },
                    label = stringResource(R.string.workout_execution_rir),
                    placeholder = stringResource(R.string.workout_execution_rir_hint),
                    isError = rirError != null,
                    errorMessage = rirError,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Action button: Complete Set / Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = {
                        onCompleteSet(
                            repsText.toIntOrNull(),
                            weightText.toDoubleOrNull(),
                            rpeText.toDoubleOrNull(),
                            rirText.toIntOrNull(),
                            selectedType,
                        )
                    },
                    enabled = !hasError,
                    colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (setRecord.completed) {
                            stringResource(R.string.workout_execution_save)
                        } else {
                            stringResource(R.string.workout_execution_complete_set)
                        },
                    )
                }
            }
        }
    }
}

// ─── Completed Set Row (read-only) ─────────────────────────────────────────────

@Composable
private fun CompletedSetRow(
    setRecord: SetRecord,
    onToggleEdit: (() -> Unit)?,
) {
    Surface(
        color = FitLogSurfaceVariant,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, FitLogAccent.copy(alpha = 0.25f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            // Set number
            Text(
                text = stringResource(R.string.workout_execution_set_number, setRecord.setNumber),
                style = MaterialTheme.typography.labelLarge,
                color = FitLogAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                // Type + summary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getSetTypeDisplayName(setRecord.setType),
                        style = MaterialTheme.typography.labelSmall,
                        color = FitLogAccent.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = buildCompletedSummary(setRecord),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FitLogTextPrimary,
                    )
                }
                // RPE / RIR line
                if (setRecord.rpe != null || setRecord.rir != null) {
                    Spacer(Modifier.height(2.dp))
                    Row {
                        setRecord.rpe?.let {
                            Text(
                                text = stringResource(R.string.workout_execution_rpe_value, it),
                                style = MaterialTheme.typography.labelSmall,
                                color = FitLogTextSecondary,
                            )
                        }
                        setRecord.rir?.let {
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.workout_execution_rir_value, it),
                                style = MaterialTheme.typography.labelSmall,
                                color = FitLogTextSecondary,
                            )
                        }
                    }
                }
            }

            // Edit button
            if (onToggleEdit != null) {
                TextButton(onClick = onToggleEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = FitLogAccent,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.workout_execution_edit),
                        color = FitLogAccent,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

// ─── Reusable Numeric Field ────────────────────────────────────────────────────

@Composable
private fun NumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean,
    errorMessage: String?,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.labelSmall,
                color = FitLogTextTertiary,
            )
        },
        isError = isError,
        supportingText = errorMessage?.let {
            { Text(text = it, color = FitLogError, style = MaterialTheme.typography.labelSmall) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = modifier,
        colors = fieldColors(),
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}

// ─── Validation Helpers ────────────────────────────────────────────────────────

private fun validateWeight(text: String): String? {
    if (text.isEmpty()) return null
    val v = text.toDoubleOrNull() ?: return null
    return if (v < 0) ">=0" else null
}

private fun validateReps(text: String): String? {
    if (text.isEmpty()) return null
    val v = text.toIntOrNull() ?: return null
    return if (v < 0) ">=0" else null
}

private fun validateRpe(text: String): String? {
    if (text.isEmpty()) return null
    val v = text.toDoubleOrNull() ?: return null
    return if (v < 1 || v > 10) "1-10" else null
}

private fun validateRir(text: String): String? {
    if (text.isEmpty()) return null
    val v = text.toIntOrNull() ?: return null
    return if (v < 0 || v > 5) "0-5" else null
}

// ─── Input Filters ─────────────────────────────────────────────────────────────

private fun filterDecimalInput(value: String): String {
    // Allow digits and at most one decimal point
    val filtered = value.filter { c -> c.isDigit() || c == '.' }
    val dotCount = filtered.count { it == '.' }
    if (dotCount > 1) {
        val firstDot = filtered.indexOf('.')
        return filtered.substring(0, firstDot + 1) +
            filtered.substring(firstDot + 1).filter { it.isDigit() }
    }
    return filtered
}

private fun filterDigitInput(value: String): String {
    return value.filter { it.isDigit() }
}

// ─── Display Helpers ───────────────────────────────────────────────────────────

@Composable
private fun getSetTypeDisplayName(type: SetType): String {
    return when (type) {
        SetType.WARMUP -> stringResource(R.string.set_type_warmup)
        SetType.WORKING -> stringResource(R.string.set_type_working)
        SetType.DROP -> stringResource(R.string.set_type_drop)
        SetType.FAILURE -> stringResource(R.string.set_type_failure)
    }
}

@Composable
private fun buildCompletedSummary(setRecord: SetRecord): String {
    val weightStr = setRecord.weightKg?.let { "${it}kg" } ?: "-"
    val repsStr = setRecord.reps?.let { "x${it}" } ?: "-"
    return "$weightStr $repsStr"
}

private fun formatSeconds(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return if (mins > 0) {
        String.format("%d:%02d", mins, secs)
    } else {
        "${secs}"
    }
}

private fun Double.formatDecimal(): String {
    return if (this == this.toLong().toDouble()) {
        this.toLong().toString()
    } else {
        this.toString()
    }
}

// ─── Shared Colors ─────────────────────────────────────────────────────────────

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = FitLogTextPrimary,
    unfocusedTextColor = FitLogTextPrimary,
    focusedBorderColor = FitLogAccent,
    unfocusedBorderColor = FitLogDivider,
    focusedLabelColor = FitLogAccent,
    unfocusedLabelColor = FitLogTextSecondary,
    cursorColor = FitLogAccent,
    errorBorderColor = FitLogError,
    errorLabelColor = FitLogError,
    errorTextColor = FitLogTextPrimary,
    focusedContainerColor = FitLogSurfaceVariant,
    unfocusedContainerColor = FitLogSurfaceVariant,
)
