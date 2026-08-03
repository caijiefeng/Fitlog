package com.example.fitlog.feature.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.EmptyStateIllustration
import com.example.fitlog.core.designsystem.component.PrimaryBottomAction
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogDivider
import com.example.fitlog.core.designsystem.theme.FitLogSurface
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.model.EquipmentType
import com.example.fitlog.core.model.Exercise
import com.example.fitlog.core.model.MuscleGroup
import com.example.fitlog.feature.exercise.ExerciseRowCard
import com.example.fitlog.feature.exercise.equipmentLabel
import com.example.fitlog.feature.exercise.muscleGroupLabel

/**
 * 动作选择器（多选）：勾选多个动作，底部固定「已选择 N 个动作 / 加入训练」。
 * 不再点击一条就立即退出。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    viewModel: ExercisePickerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExercisePickerEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.exercise_picker_title), color = FitLogTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.exercise_picker_back),
                            tint = FitLogTextPrimary,
                        )
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
                .padding(padding),
        ) {
            // 搜索
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.exercise_picker_search_placeholder),
                        color = FitLogTextSecondary,
                    )
                },
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
                                    ?: stringResource(R.string.exercise_picker_all_groups),
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
                                    ?: stringResource(R.string.exercise_picker_all_groups),
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 动作列表
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FitLogAccent)
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
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(uiState.exercises, key = { it.id }) { exercise ->
                            ExerciseRowCard(
                                exercise = exercise,
                                selected = exercise.id in uiState.selectedIds,
                                showChevron = false,
                                onClick = { viewModel.toggleSelection(exercise.id) },
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // 底部固定操作条
            PrimaryBottomAction(
                text = if (uiState.isAdding) {
                    stringResource(R.string.exercise_picker_confirm_adding)
                } else {
                    stringResource(R.string.exercise_picker_add_all)
                },
                onClick = { viewModel.confirmSelection() },
                enabled = uiState.selectedIds.isNotEmpty() && !uiState.isAdding,
                loading = uiState.isAdding,
                badge = stringResource(
                    R.string.exercise_picker_selected_count,
                    uiState.selectedIds.size,
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}
