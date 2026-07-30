package com.example.fitlog.feature.exercise

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogSurface
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.model.EquipmentType
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.MuscleGroup
import com.example.fitlog.core.model.TrackingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    viewModel: ExerciseListViewModel = hiltViewModel(),
    onNavigateToCreate: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExerciseListEvent.NavigateToCreate -> onNavigateToCreate()
                is ExerciseListEvent.NavigateToEdit -> onNavigateToEdit(event.exerciseId)
                is ExerciseListEvent.ShowError -> { /* handled via state */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("动作库", color = FitLogTextPrimary) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← 计划", color = FitLogAccent)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onCreateNew() }) {
                        Icon(Icons.Filled.Add, "新增动作", tint = FitLogAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FitLogSurface),
            )
        },
        containerColor = FitLogBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // Search
            androidx.compose.material3.OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索动作...", color = FitLogTextSecondary) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = FitLogTextSecondary) },
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FitLogTextPrimary,
                    unfocusedTextColor = FitLogTextPrimary,
                    focusedBorderColor = FitLogAccent,
                    unfocusedBorderColor = FitLogTextSecondary.copy(alpha = 0.3f),
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Muscle group filter chips
            LazyRow {
                items(
                    listOf(null) + MuscleGroup.entries,
                    key = { it?.name ?: "all" }
                ) { group ->
                    FilterChip(
                        selected = uiState.selectedMuscleGroup == group,
                        onClick = { viewModel.onMuscleGroupSelected(group) },
                        label = {
                            Text(
                                group?.let { muscleGroupLabel(it) } ?: "全部",
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FitLogAccent)
                    }
                }
                uiState.isEmpty -> {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "没有动作",
                        subtitle = "点击右上角 + 创建自定义动作",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    LazyColumn {
                        items(uiState.exercises, key = { it.id }) { exercise ->
                            ExerciseRow(
                                exercise = exercise,
                                onClick = { viewModel.onExerciseClicked(exercise) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    FitLogCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.bodyLarge, color = FitLogTextPrimary)
                Text(
                    trackingTypeDescription(exercise),
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                )
            }
            Text("›", color = FitLogTextSecondary)
        }
    }
}

private fun trackingTypeDescription(exercise: Exercise): String {
    val muscleLabel = muscleGroupLabel(exercise.primaryMuscleGroup)
    val customTag = if (exercise.isCustom) " · 自定义" else ""
    return when (exercise.equipmentType) {
        EquipmentType.BARBELL, EquipmentType.DUMBBELL,
        EquipmentType.MACHINE, EquipmentType.CABLE,
        EquipmentType.KETTLEBELL -> {
            "${equipmentLabel(exercise.equipmentType)} · $muscleLabel · 重量×次数$customTag"
        }
        EquipmentType.BODYWEIGHT -> {
            when (exercise.trackingType) {
                TrackingType.BODYWEIGHT_REPS -> "自重次数 + 附加重量(可选)$customTag"
                TrackingType.DURATION -> "计时 · 保持姿势$customTag"
                else -> "自重 · $muscleLabel$customTag"
            }
        }
        EquipmentType.CARDIO_MACHINE -> {
            when (exercise.trackingType) {
                TrackingType.DISTANCE_DURATION -> "有氧 · 距离/时长$customTag"
                TrackingType.DURATION -> "有氧 · 计时$customTag"
                else -> "有氧 · $muscleLabel$customTag"
            }
        }
        EquipmentType.OTHER -> {
            "$muscleLabel$customTag"
        }
    }
}

private fun equipmentLabel(type: EquipmentType): String = when (type) {
    EquipmentType.BARBELL -> "杠铃"
    EquipmentType.DUMBBELL -> "哑铃"
    EquipmentType.MACHINE -> "器械"
    EquipmentType.CABLE -> "绳索"
    EquipmentType.BODYWEIGHT -> "自重"
    EquipmentType.KETTLEBELL -> "壶铃"
    EquipmentType.CARDIO_MACHINE -> "有氧"
    EquipmentType.OTHER -> "其他"
}

private fun muscleGroupLabel(group: MuscleGroup): String = when (group) {
    MuscleGroup.CHEST -> "胸"
    MuscleGroup.BACK -> "背"
    MuscleGroup.SHOULDERS -> "肩"
    MuscleGroup.BICEPS -> "肱二头肌"
    MuscleGroup.TRICEPS -> "肱三头肌"
    MuscleGroup.FOREARMS -> "前臂"
    MuscleGroup.QUADRICEPS -> "股四头肌"
    MuscleGroup.HAMSTRINGS -> "腘绳肌"
    MuscleGroup.GLUTES -> "臀"
    MuscleGroup.CALVES -> "小腿"
    MuscleGroup.CORE -> "核心"
    MuscleGroup.CARDIO -> "有氧"
    MuscleGroup.FULL_BODY -> "全身"
}

@Composable
private fun TextButton(onClick: () -> Unit, text: @Composable () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { text() }
}
