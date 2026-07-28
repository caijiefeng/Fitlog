package com.example.fitlog.feature.today

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitlog.R
import com.example.fitlog.core.designsystem.component.EmptyState
import com.example.fitlog.core.designsystem.component.FitLogCard
import com.example.fitlog.core.designsystem.component.FitLogTopAppBar
import com.example.fitlog.core.designsystem.component.PageContainer
import com.example.fitlog.core.designsystem.component.SectionHeader
import com.example.fitlog.core.designsystem.theme.FitLogAccent
import com.example.fitlog.core.designsystem.theme.FitLogBackground
import com.example.fitlog.core.designsystem.theme.FitLogTextPrimary
import com.example.fitlog.core.designsystem.theme.FitLogTextSecondary
import java.util.Calendar

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val snackbarMessage = stringResource(R.string.snackbar_v2_feature)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TodayEvent.QuickStartNotAvailable -> {
                    snackbarHostState.showSnackbar(message = snackbarMessage)
                }
            }
        }
    }

    // Greeting based on time of day
    val greeting = greetingForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

    Scaffold(
        topBar = {
            FitLogTopAppBar(title = stringResource(R.string.nav_today))
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FitLogBackground,
    ) { innerPadding ->
        PageContainer(
            modifier = Modifier.padding(innerPadding),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Greeting
            Text(
                text = stringResource(greeting),
                style = MaterialTheme.typography.headlineSmall,
                color = FitLogTextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.today_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = FitLogTextSecondary,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Today's Workout
            SectionHeader(title = stringResource(R.string.section_todays_workout))

            if (uiState.hasWorkoutToday) {
                FitLogCard {
                    Text(
                        text = uiState.todayWorkoutName ?: stringResource(R.string.section_todays_workout),
                        style = MaterialTheme.typography.titleMedium,
                        color = FitLogTextPrimary,
                    )
                }
            } else {
                EmptyState(
                    icon = Icons.Filled.FitnessCenter,
                    title = stringResource(R.string.empty_today_title),
                    subtitle = stringResource(R.string.empty_today_subtitle),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions
            SectionHeader(title = stringResource(R.string.section_quick_actions))

            FitLogCard(
                onClick = { viewModel.onQuickStart() },
            ) {
                Text(
                    text = stringResource(R.string.today_quick_start),
                    style = MaterialTheme.typography.titleMedium,
                    color = FitLogAccent,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.today_quick_start_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = FitLogTextSecondary,
                )
            }
        }
    }
}

private fun greetingForHour(hour: Int): Int = when (hour) {
    in 5..11 -> R.string.today_greeting_morning
    in 12..17 -> R.string.today_greeting_afternoon
    else -> R.string.today_greeting_evening
}
