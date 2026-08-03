package com.example.fitlog.feature.exercise

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogCardStyle
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.MetricRow
import com.example.fitlog.core.designsystem.component.SectionTitle
import com.example.fitlog.core.designsystem.component.StatusPill
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import com.example.fitlog.core.designsystem.theme.FitLogType
import com.example.fitlog.core.model.Exercise

/**
 * 动作详情页：起始/结束姿势（可滑动、自动交替、缩放）+ 肌群/器械/记录类型
 * + 动作步骤 + 加入模板/加入当前训练。
 */
@Composable
fun ExerciseDetailScreen(
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {},
    onNavigateToTemplate: (Long) -> Unit = {},
    onNavigateToExecution: (Long) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExerciseDetailEvent.NavigateToTemplate -> onNavigateToTemplate(event.templateId)
                is ExerciseDetailEvent.NavigateToExecution -> onNavigateToExecution(event.sessionId)
                is ExerciseDetailEvent.ShowError -> { /* shown via state */ }
            }
        }
    }

    Scaffold(
        topBar = {
            FitLogTopAppBar(
                title = stringResource(R.string.exercise_detail_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = FitLogTextPrimary,
                        )
                    }
                },
                actions = {
                    val exercise = uiState.exercise
                    if (exercise?.isCustom == true) {
                        IconButton(onClick = { onNavigateToEdit(exercise.id) }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.exercise_detail_edit),
                                tint = FitLogAccent,
                            )
                        }
                    }
                },
            )
        },
        containerColor = FitLogBackground,
    ) { padding ->
        val exercise = uiState.exercise
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FitLogAccent)
                }
            }
            exercise == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.error ?: "动作不存在",
                        color = FitLogTextSecondary,
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    // 名称 + 状态
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = exercise.name,
                            style = FitLogType.pageTitle,
                            color = FitLogTextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        if (exercise.isCustom) {
                            StatusPill(
                                text = stringResource(R.string.exercise_scope_custom),
                                color = FitLogTextSecondary,
                                containerColor = FitLogTextSecondary.copy(alpha = 0.12f),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // 起始/结束姿势
                    ExerciseStartEndImages(
                        builtInKey = exercise.builtInKey,
                        contentDescription = exercise.name,
                        fallbackText = stringResource(R.string.exercise_detail_no_illustration),
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 基本信息
                    FitLogCard(style = FitLogCardStyle.TONAL) {
                        MetricRow(
                            label = stringResource(R.string.exercise_detail_primary_muscle),
                            value = muscleGroupFullLabel(exercise.primaryMuscleGroup),
                            valueColor = FitLogAccent,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MetricRow(
                            label = stringResource(R.string.exercise_detail_secondary_muscle),
                            value = exercise.secondaryMuscleGroup
                                ?.let { muscleGroupFullLabel(it) }
                                ?: "—",
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MetricRow(
                            label = stringResource(R.string.exercise_detail_equipment),
                            value = equipmentLabel(exercise.equipmentType),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MetricRow(
                            label = stringResource(R.string.exercise_detail_tracking),
                            value = trackingLabel(exercise.trackingType),
                        )
                    }

                    // 动作步骤
                    val instructions = uiState.asset?.instructionsZh.orEmpty()
                    if (instructions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionTitle(title = stringResource(R.string.exercise_detail_steps))
                        Spacer(modifier = Modifier.height(8.dp))
                        FitLogCard {
                            instructions.forEachIndexed { index, step ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        style = FitLogType.body,
                                        color = FitLogAccent,
                                        modifier = Modifier.width(28.dp),
                                    )
                                    Text(
                                        text = step,
                                        style = FitLogType.body,
                                        color = FitLogTextPrimary,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    // 备注（自定义动作）
                    val notes = exercise.notes
                    if (!notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionTitle(title = stringResource(R.string.exercise_detail_notes))
                        Spacer(modifier = Modifier.height(8.dp))
                        FitLogCard {
                            Text(
                                text = notes,
                                style = FitLogType.body,
                                color = FitLogTextPrimary,
                            )
                        }
                    }

                    // 操作
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onAddToTemplate() },
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.exercise_detail_add_to_template),
                                color = FitLogAccent,
                            )
                        }
                        Button(
                            onClick = { viewModel.onAddToWorkout() },
                            enabled = !uiState.isAddingToSession,
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) {
                            if (uiState.isAddingToSession) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = stringResource(R.string.exercise_detail_add_to_workout),
                            )
                        }
                    }
                    uiState.addedMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = FitLogType.caption,
                            color = FitLogAccent,
                        )
                    }
                    uiState.error?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = FitLogType.caption,
                            color = com.example.fitlog.core.designsystem.theme.FitLogError,
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
