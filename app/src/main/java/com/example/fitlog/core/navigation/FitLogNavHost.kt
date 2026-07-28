package com.example.fitlog.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.fitlog.feature.exercise.ExerciseFormScreen
import com.example.fitlog.feature.exercise.ExerciseListScreen
import com.example.fitlog.feature.plan.PlanScreen
import com.example.fitlog.feature.profile.ProfileScreen
import com.example.fitlog.feature.progress.ProgressScreen
import com.example.fitlog.feature.record.RecordScreen
import com.example.fitlog.feature.template.TemplateEditScreen
import com.example.fitlog.feature.template.TemplateListScreen
import com.example.fitlog.feature.today.TodayScreen

object Routes {
    const val TODAY = "today"
    const val PLAN = "plan"
    const val RECORD = "record"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val EXERCISE_LIST = "exercise/list"
    const val EXERCISE_CREATE = "exercise/create"
    const val EXERCISE_EDIT = "exercise/edit/{exerciseId}"
    const val TEMPLATE_LIST = "template/list"
    const val TEMPLATE_CREATE = "template/create"
    const val TEMPLATE_EDIT = "template/edit/{templateId}"

    fun exerciseEdit(id: Long) = "exercise/edit/$id"
    fun templateEdit(id: Long) = "template/edit/$id"
}

@Composable
fun FitLogNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Today.route,
        modifier = modifier,
    ) {
        // ── Top-level tabs ──────────────────────────────────────────────────
        composable(BottomNavItem.Today.route) { TodayScreen() }
        composable(BottomNavItem.Plan.route) {
            PlanScreen(
                onNavigateToExercises = { navController.navigate(Routes.EXERCISE_LIST) },
            )
        }
        composable(BottomNavItem.Record.route) { RecordScreen() }
        composable(BottomNavItem.Progress.route) { ProgressScreen() }
        composable(BottomNavItem.Profile.route) { ProfileScreen() }

        // ── Exercise routes ─────────────────────────────────────────────────
        composable(Routes.EXERCISE_LIST) {
            ExerciseListScreen(
                onNavigateToCreate = { navController.navigate(Routes.EXERCISE_CREATE) },
                onNavigateToEdit = { id -> navController.navigate(Routes.exerciseEdit(id)) },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Routes.EXERCISE_CREATE) {
            ExerciseFormScreen(
                onSaved = { navController.popBackStack() },
                onCancelled = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.EXERCISE_EDIT,
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType }),
        ) {
            ExerciseFormScreen(
                onSaved = { navController.popBackStack() },
                onCancelled = { navController.popBackStack() },
            )
        }

        // ── Template routes ─────────────────────────────────────────────────
        composable(Routes.TEMPLATE_LIST) {
            TemplateListScreen(
                onNavigateToCreate = { navController.navigate(Routes.TEMPLATE_CREATE) },
                onNavigateToEdit = { id -> navController.navigate(Routes.templateEdit(id)) },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Routes.TEMPLATE_CREATE) {
            TemplateEditScreen(
                onSaved = { navController.popBackStack() },
                onCancelled = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.TEMPLATE_EDIT,
            arguments = listOf(navArgument("templateId") { type = NavType.LongType }),
        ) {
            TemplateEditScreen(
                onSaved = { navController.popBackStack() },
                onCancelled = { navController.popBackStack() },
            )
        }
    }
}
