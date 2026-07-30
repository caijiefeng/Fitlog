package com.example.fitlog.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.fitlog.feature.body.BodyMeasurementScreen
import com.example.fitlog.feature.body.BodyProfileScreen
import com.example.fitlog.feature.exercise.ExerciseFormScreen
import com.example.fitlog.feature.exercise.ExerciseListScreen
import com.example.fitlog.feature.goal.GoalScreen
import com.example.fitlog.feature.nutrition.NutritionScreen
import com.example.fitlog.feature.plan.PlanScreen
import com.example.fitlog.feature.profile.ProfileScreen
import com.example.fitlog.feature.progress.ProgressScreen
import com.example.fitlog.feature.record.RecordScreen
import com.example.fitlog.feature.reminder.ReminderEditScreen
import com.example.fitlog.feature.reminder.ReminderListScreen
import com.example.fitlog.feature.template.TemplateEditScreen
import com.example.fitlog.feature.template.TemplateListScreen
import com.example.fitlog.feature.today.TodayScreen
import com.example.fitlog.feature.workout.ExercisePickerScreen
import com.example.fitlog.feature.workout.WorkoutDetailScreen
import com.example.fitlog.feature.workout.WorkoutExecutionScreen
import com.example.fitlog.feature.workout.WorkoutSummaryScreen

object Routes {
    const val TODAY = "today"
    const val PLAN = "plan"
    const val RECORD = "record"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val CALENDAR_DAY = "calendar/day/{epochDay}"
    const val EXERCISE_LIST = "exercise/list"
    const val EXERCISE_CREATE = "exercise/create"
    const val EXERCISE_EDIT = "exercise/edit/{exerciseId}"
    const val TEMPLATE_LIST = "template/list"
    const val TEMPLATE_CREATE = "template/create"
    const val TEMPLATE_EDIT = "template/edit/{templateId}"
    const val WORKOUT_EXECUTION = "workout/session/{sessionId}"
    const val WORKOUT_SUMMARY = "workout/summary/{sessionId}"
    const val WORKOUT_DETAIL = "workout/detail/{sessionId}"
    const val EXERCISE_PICKER = "workout/exercise-picker/{sessionId}"
    const val REMINDER_LIST = "reminder/list"
    const val REMINDER_CREATE = "reminder/create"
    const val REMINDER_EDIT = "reminder/edit/{reminderId}"

    fun exerciseEdit(id: Long) = "exercise/edit/$id"
    fun templateEdit(id: Long) = "template/edit/$id"
    fun workoutExecution(id: Long) = "workout/session/$id"
    fun workoutSummary(id: Long) = "workout/summary/$id"
    fun workoutDetail(id: Long) = "workout/detail/$id"
    fun exercisePicker(id: Long) = "workout/exercise-picker/$id"
    fun calendarDay(epochDay: Long) = "calendar/day/$epochDay"
    fun reminderEdit(id: Long) = "reminder/edit/$id"

    // Body & Nutrition routes
    const val BODY_PROFILE = "body/profile"
    const val BODY_MEASUREMENT = "body/measurement"
    const val BODY_PROGRESS_PHOTO = "body/progress-photo"
    const val NUTRITION = "nutrition"
    const val GOAL = "goal"

    // Media routes
    const val MEDIA_LIBRARY = "media/library"
    const val MEDIA_DETAIL = "media/detail/{mediaId}"
    const val CAMERA = "camera"

    fun mediaDetail(id: Long) = "media/detail/$id"

    /** Builds a camera route with optional query parameters. */
    fun camera(
        category: String? = null,
        workoutSessionId: Long? = null,
        bodyMeasurementId: Long? = null,
        checkInId: Long? = null,
    ): String = buildString {
        append(CAMERA)
        val params = mutableListOf<String>()
        category?.let { params.add("category=$it") }
        workoutSessionId?.let { params.add("workoutSessionId=$it") }
        bodyMeasurementId?.let { params.add("bodyMeasurementId=$it") }
        checkInId?.let { params.add("checkInId=$it") }
        if (params.isNotEmpty()) {
            append("?")
            append(params.joinToString("&"))
        }
    }
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
        composable(BottomNavItem.Today.route) {
            TodayScreen(
                onStartWorkout = { id -> navController.navigate(Routes.workoutExecution(id)) },
                onResumeWorkout = { id -> navController.navigate(Routes.workoutExecution(id)) },
                onNavigateToWorkoutDetail = { id -> navController.navigate(Routes.workoutDetail(id)) },
            )
        }
        composable(BottomNavItem.Plan.route) {
            PlanScreen(
                onNavigateToExercises = { navController.navigate(Routes.EXERCISE_LIST) },
                onNavigateToTemplates = { navController.navigate(Routes.TEMPLATE_LIST) },
                onNavigateToSession = { id -> navController.navigate(Routes.workoutExecution(id)) },
                onNavigateToDayDetail = { epochDay -> navController.navigate(Routes.calendarDay(epochDay)) },
                onNavigateToReminders = { navController.navigate(Routes.REMINDER_LIST) },
            )
        }
        composable(BottomNavItem.Record.route) {
            RecordScreen(
                onNavigateToWorkoutDetail = { id -> navController.navigate(Routes.workoutDetail(id)) },
            )
        }
        composable(BottomNavItem.Progress.route) { ProgressScreen() }
        composable(BottomNavItem.Profile.route) {
            ProfileScreen(
                onNavigateToBodyProfile = { navController.navigate(Routes.BODY_PROFILE) },
            )
        }

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

        // ── Reminder routes ─────────────────────────────────────────────────
        composable(Routes.REMINDER_LIST) {
            ReminderListScreen(
                onNavigateToCreate = { navController.navigate(Routes.REMINDER_CREATE) },
                onNavigateToEdit = { id -> navController.navigate(Routes.reminderEdit(id)) },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Routes.REMINDER_CREATE) {
            ReminderEditScreen(
                onSaved = { navController.popBackStack() },
                onCancelled = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.REMINDER_EDIT,
            arguments = listOf(navArgument("reminderId") { type = NavType.LongType }),
        ) {
            ReminderEditScreen(
                onSaved = { navController.popBackStack() },
                onCancelled = { navController.popBackStack() },
            )
        }

        // ── Calendar day detail (full-screen, no bottom nav) ────────────────
        composable(
            route = Routes.CALENDAR_DAY,
            arguments = listOf(navArgument("epochDay") { type = NavType.LongType }),
        ) {
            com.example.fitlog.feature.calendar.CalendarDayDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExecution = { id -> navController.navigate(Routes.workoutExecution(id)) },
                onNavigateToWorkoutDetail = { id -> navController.navigate(Routes.workoutDetail(id)) },
            )
        }

        // ── Workout routes (full-screen, no bottom nav) ────────────────────
        composable(
            route = Routes.WORKOUT_EXECUTION,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
        ) {
            WorkoutExecutionScreen(
                onNavigateToSummary = { id -> navController.navigate(Routes.workoutSummary(id)) },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExercisePicker = { sessionId -> navController.navigate(Routes.exercisePicker(sessionId)) },
            )
        }
        composable(
            route = Routes.EXERCISE_PICKER,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
        ) {
            ExercisePickerScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Workout Summary ────────────────────────────────────────────────
        composable(
            route = Routes.WORKOUT_SUMMARY,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
        ) {
            WorkoutSummaryScreen(
                onNavigateBack = {
                    navController.navigate(Routes.TODAY) {
                        popUpTo(Routes.WORKOUT_EXECUTION) { inclusive = true }
                    }
                },
            )
        }

        // ── Workout Detail ─────────────────────────────────────────────────
        composable(
            route = Routes.WORKOUT_DETAIL,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
        ) {
            WorkoutDetailScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Body & Nutrition routes ─────────────────────────────────────────
        composable(Routes.BODY_PROFILE) {
            BodyProfileScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Routes.BODY_MEASUREMENT) {
            BodyMeasurementScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        // Progress photo, camera, media library, media detail
        // UNVERIFIED_DEVICE: routes registered, UI pending CameraX API fix
        composable(Routes.NUTRITION) {
            NutritionScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Routes.GOAL) {
            GoalScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
