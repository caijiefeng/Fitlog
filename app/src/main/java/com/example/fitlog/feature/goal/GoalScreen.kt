package com.example.fitlog.feature.goal

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.FlagCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.domain.body.GoalType
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary

@Composable
fun GoalScreen(
    viewModel: GoalViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            FitLogTopAppBar(title = stringResource(R.string.goal_title))
        },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = FitLogAccent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Goal type header
                FitLogCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.FlagCircle,
                            contentDescription = null,
                            tint = FitLogAccent,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val goalLabel = when (uiState.goalType) {
                                GoalType.FAT_LOSS -> stringResource(R.string.goal_fat_loss)
                                GoalType.MAINTAIN -> stringResource(R.string.goal_maintain)
                                GoalType.LEAN_GAIN -> stringResource(R.string.goal_lean_gain)
                                GoalType.MUSCLE_GAIN -> stringResource(R.string.goal_muscle_gain)
                            }
                            Text(
                                text = goalLabel,
                                style = MaterialTheme.typography.titleLarge,
                                color = FitLogTextPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.goal_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = FitLogTextSecondary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current state
                val plan = uiState.goalPlan
                if (plan != null) {
                    SectionHeader(title = stringResource(R.string.goal_current_state))
                    FitLogCard {
                        StatRow(
                            label = stringResource(R.string.goal_current_weight),
                            value = "%.1f kg".format(plan.currentWeightKg),
                        )
                        uiState.currentBodyFatPercent?.let {
                            StatRow(
                                label = stringResource(R.string.goal_current_bf),
                                value = "%.1f%%".format(it),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Target state
                    SectionHeader(title = stringResource(R.string.goal_target_state))
                    FitLogCard {
                        StatRow(
                            label = stringResource(R.string.goal_target_weight),
                            value = "%.1f kg".format(plan.targetWeightKg),
                        )
                        uiState.targetBodyFatPercent?.let {
                            StatRow(
                                label = stringResource(R.string.goal_target_bf),
                                value = "%.1f%%".format(it),
                            )
                        }
                        StatRow(
                            label = stringResource(R.string.goal_weight_diff),
                            value = "%.1f kg".format(plan.weightDifferenceKg),
                        )
                        StatRow(
                            label = stringResource(R.string.goal_fat_to_lose),
                            value = "%.1f kg".format(plan.fatToLoseKg),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Timeline
                    SectionHeader(title = stringResource(R.string.goal_timeline))
                    FitLogCard {
                        StatRow(
                            label = stringResource(R.string.goal_estimated_weeks),
                            value = stringResource(R.string.goal_weeks_format, plan.estimatedWeeks),
                        )
                        StatRow(
                            label = stringResource(R.string.goal_recommended_calories),
                            value = stringResource(R.string.goal_calories_format, plan.recommendedDailyCalories),
                        )
                    }
                } else {
                    // No plan available
                    Spacer(modifier = Modifier.height(16.dp))
                    FitLogCard {
                        Text(
                            text = stringResource(R.string.goal_no_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = FitLogTextSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = FitLogTextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = FitLogTextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
