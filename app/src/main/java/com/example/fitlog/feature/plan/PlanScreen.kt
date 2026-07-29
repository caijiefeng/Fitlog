package com.example.fitlog.feature.plan

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fitlog.feature.calendar.CalendarScreen

@Composable
fun PlanScreen(
    viewModel: PlanViewModel = hiltViewModel(),
    onNavigateToExercises: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToSession: (Long) -> Unit = {},
) {
    CalendarScreen(
        onNavigateToExercises = onNavigateToExercises,
        onNavigateToTemplates = onNavigateToTemplates,
        onNavigateToSession = onNavigateToSession,
    )
}
