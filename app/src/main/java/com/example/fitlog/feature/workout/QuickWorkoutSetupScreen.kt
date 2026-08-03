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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogDivider
import com.example.fitlog.core.designsystem.theme.FitLogSurface
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.MuscleGroup
import com.example.fitlog.feature.exercise.muscleGroupLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickWorkoutSetupScreen(
    viewModel: QuickWorkoutSetupViewModel = hiltViewModel(),
    onNavigateToExecution: (Long) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is QuickWorkoutSetupEvent.NavigateToExecution -> onNavigateToExecution(event.sessionId)
                is QuickWorkoutSetupEvent.ShowError -> { /* shown via state */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自由训练", color = FitLogTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = FitLogTextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FitLogSurface),
            )
        },
        containerColor = FitLogBackground,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // Search field
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索动作...", color = FitLogTextSecondary) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = FitLogTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FitLogTextPrimary,
                        unfocusedTextColor = FitLogTextPrimary,
                        focusedBorderColor = FitLogAccent,
                        unfocusedBorderColor = FitLogDivider,
                        focusedContainerColor = FitLogSurface,
                        unfocusedContainerColor = FitLogSurface,
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Muscle group filter chips
            item {
                LazyRow {
                    items(
                        listOf(null) + MuscleGroup.entries,
                        key = { it?.name ?: "all" },
                    ) { group ->
                        FilterChip(
                            selected = uiState.selectedMuscleGroup == group,
                            onClick = { viewModel.onMuscleGroupSelected(group) },
                            label = {
                                Text(group?.let { muscleGroupLabel(it) } ?: "全部")
                            },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Hint text
            item {
                Text(
                    "选择要训练的动作，点击 + 添加到下方列表",
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Available exercises
            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FitLogAccent)
                    }
                }
            } else if (uiState.allExercises.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "没有动作",
                        subtitle = "请在动作库中先创建动作",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(uiState.allExercises, key = { "lib_${it.id}" }) { exercise ->
                    val isSelected = uiState.selectedExercises.any { it.exercise.id == exercise.id }
                    FitLogCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        onClick = { viewModel.toggleExercise(exercise) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exercise.name, style = MaterialTheme.typography.bodyLarge, color = FitLogTextPrimary)
                                Text(
                                    muscleGroupLabel(exercise.primaryMuscleGroup),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FitLogTextSecondary,
                                )
                            }
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.Add,
                                contentDescription = null,
                                tint = if (isSelected) FitLogAccent else FitLogTextSecondary,
                            )
                        }
                    }
                }
            }

            // Selected exercises section
            if (uiState.selectedExercises.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "已选动作 (${uiState.selectedExercises.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = FitLogTextPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                itemsIndexed(uiState.selectedExercises, key = { _, item -> "sel_${item.exercise.id}" }) { index, item ->
                    FitLogCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${index + 1}. ${item.exercise.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = FitLogTextPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { viewModel.removeExercise(index) }) {
                                    Icon(Icons.Filled.Delete, "移除", tint = FitLogTextSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberField("组", item.targetSets, 70.dp) {
                                    viewModel.updateExerciseField(index, "sets", it)
                                }
                                NumberField("次", item.targetReps, 70.dp) {
                                    viewModel.updateExerciseField(index, "reps", it)
                                }
                                NumberField("kg", item.targetWeightKg, 80.dp) {
                                    viewModel.updateExerciseField(index, "weight", it)
                                }
                            }
                        }
                    }
                }

                // Start button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    uiState.error?.let { error ->
                        Text(error, color = FitLogTextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(
                        onClick = { viewModel.onCreateSession() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isCreating,
                        colors = ButtonDefaults.buttonColors(containerColor = FitLogAccent),
                    ) {
                        if (uiState.isCreating) {
                            CircularProgressIndicator(color = FitLogBackground, modifier = Modifier.height(20.dp))
                        } else {
                            Text("开始训练")
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, width: androidx.compose.ui.unit.Dp, onChanged: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChanged,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = Modifier.width(width),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = FitLogTextPrimary,
            unfocusedTextColor = FitLogTextPrimary,
            focusedBorderColor = FitLogAccent,
            unfocusedBorderColor = FitLogTextSecondary.copy(alpha = 0.3f),
            focusedLabelColor = FitLogAccent,
            unfocusedLabelColor = FitLogTextSecondary,
        ),
    )
}

