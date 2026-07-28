package com.example.fitlog.feature.plan

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary

@Composable
fun PlanScreen(
    viewModel: PlanViewModel = hiltViewModel(),
    onNavigateToExercises: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            FitLogTopAppBar(title = stringResource(R.string.nav_plan))
        },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        PageContainer(
            modifier = Modifier.padding(innerPadding),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = stringResource(R.string.section_weekly_plan))

            EmptyState(
                icon = Icons.Filled.DateRange,
                title = stringResource(R.string.empty_plan_title),
                subtitle = stringResource(R.string.empty_plan_subtitle),
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(title = stringResource(R.string.section_training_days))

            Text(
                text = stringResource(R.string.plan_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = FitLogTextSecondary,
            )
        }
    }
}
