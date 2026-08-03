package com.example.fitlog.feature.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.fitlog.core.designsystem.component.EmptyStateIllustration
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogCardStyle
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.LoadingSkeleton
import com.example.fitlog.core.designsystem.component.SegmentedFilter
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogDimensions
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogType
import com.example.fitlog.core.model.EquipmentType
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.MuscleGroup

@Composable
fun ExerciseListScreen(
    viewModel: ExerciseListViewModel = hiltViewModel(),
    onNavigateToCreate: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExerciseListEvent.NavigateToCreate -> onNavigateToCreate()
                is ExerciseListEvent.NavigateToDetail -> onNavigateToDetail(event.exerciseId)
                is ExerciseListEvent.ShowError -> { /* handled via state */ }
            }
        }
    }

    Scaffold(
        topBar = {
            FitLogTopAppBar(
                title = stringResource(R.string.exercise_library_title),
                navigationIcon = {
                    androidx.compose.material3.TextButton(onClick = onNavigateBack) {
                        Text(stringResource(R.string.action_back_to_plan), color = FitLogAccent)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onCreateNew() }) {
                        Icon(
                            Icons.Filled.Add,
                            stringResource(R.string.exercise_create),
                            tint = FitLogAccent,
                        )
                    }
                },
            )
        },
        containerColor = FitLogBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ── 顶部筛选区（固定在列表上方，不随列表滚动消失）──────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            stringResource(R.string.exercise_search_placeholder),
                            color = FitLogTextSecondary,
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, null, tint = FitLogTextSecondary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FitLogTextPrimary,
                        unfocusedTextColor = FitLogTextPrimary,
                        focusedBorderColor = FitLogAccent,
                        unfocusedBorderColor = FitLogTextSecondary.copy(alpha = 0.3f),
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                val allLabel = stringResource(R.string.exercise_scope_all)
                val customLabel = stringResource(R.string.exercise_scope_custom)
                SegmentedFilter(
                    options = listOf(ExerciseScopeFilter.ALL, ExerciseScopeFilter.CUSTOM),
                    selected = uiState.scopeFilter,
                    onSelect = { viewModel.onScopeFilterSelected(it) },
                    label = {
                        when (it) {
                            ExerciseScopeFilter.ALL -> allLabel
                            ExerciseScopeFilter.CUSTOM -> customLabel
                        }
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 肌群筛选
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    listOf(null) + MuscleGroup.entries,
                    key = { it?.name ?: "all" },
                ) { group ->
                    FilterChip(
                        selected = uiState.selectedMuscleGroup == group,
                        onClick = { viewModel.onMuscleGroupSelected(group) },
                        label = {
                            Text(
                                group?.let { muscleGroupLabel(it) }
                                    ?: stringResource(R.string.exercise_filter_all),
                            )
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 器械筛选
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    listOf(null) + EquipmentType.entries,
                    key = { it?.name ?: "all" },
                ) { equipment ->
                    FilterChip(
                        selected = uiState.selectedEquipment == equipment,
                        onClick = { viewModel.onEquipmentSelected(equipment) },
                        label = {
                            Text(
                                equipment?.let { equipmentLabel(it) }
                                    ?: stringResource(R.string.exercise_filter_all),
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── 内容 ─────────────────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LoadingSkeleton(
                            showThumbnail = true,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                uiState.isEmpty -> {
                    EmptyStateIllustration(
                        icon = Icons.Filled.Search,
                        title = stringResource(R.string.exercise_library_empty_title),
                        subtitle = stringResource(R.string.exercise_library_empty_subtitle),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val wide = maxWidth >= 600.dp
                        if (wide) {
                            // 平板/横屏双列网格
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(uiState.exercises, key = { it.id }) { exercise ->
                                    ExerciseRowCard(
                                        exercise = exercise,
                                        onClick = { viewModel.onExerciseClicked(exercise) },
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 4.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(uiState.exercises, key = { it.id }) { exercise ->
                                    ExerciseRowCard(
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
    }
}

/** 带 80×80 缩略图的动作行卡片（列表与双列网格共用）。 */
@Composable
internal fun ExerciseRowCard(
    exercise: Exercise,
    onClick: () -> Unit,
    selected: Boolean = false,
    showChevron: Boolean = true,
) {
    FitLogCard(
        onClick = onClick,
        selected = selected,
        style = if (selected) FitLogCardStyle.OUTLINED else FitLogCardStyle.STANDARD,
        leadingContent = {
            ExerciseThumbnailByKey(
                builtInKey = exercise.builtInKey,
                contentDescription = exercise.name,
                modifier = Modifier.size(FitLogDimensions.listThumbnail),
            )
        },
        trailingContent = {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = FitLogAccent,
                    modifier = Modifier.size(24.dp),
                )
            } else if (showChevron) {
                Text("›", color = FitLogTextSecondary)
            }
        },
    ) {
        Text(
            text = exercise.name,
            style = FitLogType.body,
            color = FitLogTextPrimary,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = exerciseSubtitle(exercise),
            style = FitLogType.caption,
            color = FitLogTextSecondary,
            maxLines = 2,
        )
    }
}
