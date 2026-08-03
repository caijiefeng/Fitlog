package com.example.fitlog.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.SavedStateHandle
import com.example.fitlog.core.designsystem.theme.FitLogTheme
import com.example.fitlog.data.repository.DailyNutritionSummary
import com.example.fitlog.data.repository.TrendPoint
import com.example.fitlog.data.repository.TrendRange
import com.example.fitlog.feature.calendar.CalendarViewModel
import com.example.fitlog.feature.checkin.CheckInViewModel
import com.example.fitlog.feature.exercise.ExerciseDetailViewModel
import com.example.fitlog.feature.exercise.ExerciseListViewModel
import com.example.fitlog.feature.nutrition.NutritionViewModel
import com.example.fitlog.feature.profile.ProfileViewModel
import com.example.fitlog.feature.progress.ProgressViewModel
import com.example.fitlog.feature.template.TemplateListScreen
import com.example.fitlog.feature.template.TemplateListViewModel
import com.example.fitlog.feature.today.TodayScreen
import com.example.fitlog.feature.today.TodayViewModel
import com.example.fitlog.feature.workout.ExercisePickerScreen
import com.example.fitlog.feature.workout.ExercisePickerViewModel
import com.example.fitlog.feature.workout.WorkoutExecutionScreen
import com.example.fitlog.feature.workout.WorkoutExecutionViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * 核心页面截图回归测试（Roborazzi）。
 *
 * 覆盖：Today / ExerciseList / ExerciseDetail / ExercisePicker /
 * WorkoutExecution / TemplateList / Calendar / Nutrition / Progress / Profile。
 * 每个页面至少浅色 + 深色；关键页面覆盖 360×800 小屏与空状态。
 *
 * 运行：./gradlew recordRoborazziDebug（录制）/ verifyRoborazziDebug（比对）
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class CoreScreensScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val fixedDate = LocalDate.of(2026, 7, 31)

    // ── Today ────────────────────────────────────────────────────────────

    private fun todayViewModel(
        withPlan: Boolean,
        inProgress: Boolean,
        weekVolume: Double = 1250.0,
        weekDays: Int = 3,
    ): TodayViewModel {
        val calendarRepo = mockk<com.example.fitlog.data.repository.CalendarRepository>(relaxed = true)
        val sessionRepo = mockk<com.example.fitlog.data.repository.WorkoutSessionRepository>(relaxed = true)
        val progressRepo = mockk<com.example.fitlog.data.repository.ProgressRepository>(relaxed = true)
        val foodRepo = mockk<com.example.fitlog.data.repository.FoodRecordRepository>(relaxed = true)
        val dateProvider = mockk<com.example.fitlog.core.time.CurrentDateProvider>(relaxed = true)
        every { dateProvider.today() } returns fixedDate
        coEvery { progressRepo.getTrendPoints(TrendRange.WEEK_7) } returns listOf(
            TrendPoint(fixedDate.minusDays(6), weight = 80.5, volume = 400.0),
            TrendPoint(fixedDate.minusDays(4), volume = 500.0),
            TrendPoint(fixedDate.minusDays(2), volume = 350.0),
        )
        coEvery { foodRepo.observeDailySummary(fixedDate) } returns flowOf(
            DailyNutritionSummary(
                calories = 1650.0,
                protein = 95.0,
                targetCalories = 2300,
                targetProtein = 140,
            ),
        )
        coEvery { calendarRepo.getDayDetail(fixedDate.toEpochDay()) } returns
            if (withPlan) listOf(
                com.example.fitlog.domain.calendar.CalendarDay(
                    epochDay = fixedDate.toEpochDay(),
                    date = fixedDate,
                    dayOfMonth = fixedDate.dayOfMonth,
                    dayOfWeek = fixedDate.dayOfWeek.value,
                    occurrences = listOf(
                        com.example.fitlog.domain.calendar.CalendarWorkoutOccurrence(
                            key = "1:${fixedDate.toEpochDay()}",
                            scheduleId = 1,
                            templateId = 1,
                            templateName = "Push A",
                            occurrenceDate = fixedDate,
                            plannedDate = fixedDate,
                            sessionId = null,
                            status = com.example.fitlog.domain.calendar.CalendarWorkoutStatus.SCHEDULED,
                            isQuickWorkout = false,
                        ),
                    ),
                ),
            ) else emptyList()
        coEvery { sessionRepo.observeInProgress() } returns flowOf(
            if (inProgress) {
                com.example.fitlog.core.model.WorkoutSession(
                    id = 1,
                    templateNameSnapshot = "Push A",
                    status = com.example.fitlog.core.model.WorkoutStatus.IN_PROGRESS,
                )
            } else null,
        )
        if (inProgress) {
            coEvery { sessionRepo.getInProgress() } returns
                com.example.fitlog.core.model.WorkoutSession(
                    id = 1,
                    templateNameSnapshot = "Push A",
                    status = com.example.fitlog.core.model.WorkoutStatus.IN_PROGRESS,
                )
            coEvery { sessionRepo.getDetail(1) } returns sessionDetail()
        }
        return TodayViewModel(calendarRepo, sessionRepo, progressRepo, foodRepo, dateProvider)
    }

    private fun checkInViewModel(): CheckInViewModel {
        val repo = mockk<com.example.fitlog.data.repository.CheckInRepository>(relaxed = true)
        val dateProvider = mockk<com.example.fitlog.core.time.CurrentDateProvider>(relaxed = true)
        every { dateProvider.today() } returns fixedDate
        coEvery { repo.observeByDate(fixedDate) } returns flowOf(null)
        return CheckInViewModel(repo, dateProvider)
    }

    @Test
    fun today_light_withPlan() = capture("today_light_plan_412") {
        FitLogTheme {
            TodayScreen(
                viewModel = todayViewModel(withPlan = true, inProgress = false),
                checkInViewModel = checkInViewModel(),
            )
        }
    }

    @Test
    fun today_dark_withPlan() = capture("today_dark_plan_412") {
        FitLogTheme(darkTheme = true) {
            TodayScreen(
                viewModel = todayViewModel(withPlan = true, inProgress = false),
                checkInViewModel = checkInViewModel(),
            )
        }
    }

    @Test
    fun today_dark_inProgress() = capture("today_dark_inprogress_412") {
        FitLogTheme(darkTheme = true) {
            TodayScreen(
                viewModel = todayViewModel(withPlan = true, inProgress = true),
                checkInViewModel = checkInViewModel(),
            )
        }
    }

    @Config(qualifiers = "w360dp-h800dp")
    @Test
    fun today_light_noPlan_smallScreen() = capture("today_light_noplan_360") {
        FitLogTheme {
            TodayScreen(
                viewModel = todayViewModel(withPlan = false, inProgress = false),
                checkInViewModel = checkInViewModel(),
            )
        }
    }

    // ── ExerciseList ──────────────────────────────────────────────────────

    private fun exerciseListViewModel(): ExerciseListViewModel {
        val repo = mockk<com.example.fitlog.data.repository.ExerciseRepository>(relaxed = true)
        coEvery { repo.getAllActive() } returns flowOf(exerciseList())
        return ExerciseListViewModel(repo)
    }

    @Test
    fun exerciseList_light() = capture("exercise_list_light_412") {
        FitLogTheme {
            com.example.fitlog.feature.exercise.ExerciseListScreen(
                viewModel = exerciseListViewModel(),
            )
        }
    }

    @Test
    fun exerciseList_dark() = capture("exercise_list_dark_412") {
        FitLogTheme(darkTheme = true) {
            com.example.fitlog.feature.exercise.ExerciseListScreen(
                viewModel = exerciseListViewModel(),
            )
        }
    }

    @Config(qualifiers = "w360dp-h800dp")
    @Test
    fun exerciseList_light_smallScreen() = capture("exercise_list_light_360") {
        FitLogTheme {
            com.example.fitlog.feature.exercise.ExerciseListScreen(
                viewModel = exerciseListViewModel(),
            )
        }
    }

    // ── ExerciseDetail ────────────────────────────────────────────────────

    private fun exerciseDetailViewModel(): ExerciseDetailViewModel {
        val exerciseRepo = mockk<com.example.fitlog.data.repository.ExerciseRepository>(relaxed = true)
        val assetRepo = mockk<com.example.fitlog.data.repository.ExerciseAssetRepository>(relaxed = true)
        val sessionRepo = mockk<com.example.fitlog.data.repository.WorkoutSessionRepository>(relaxed = true)
        val templateRepo = mockk<com.example.fitlog.data.repository.WorkoutTemplateRepository>(relaxed = true)
        coEvery { exerciseRepo.getById(1) } returns exercise(1, "杠铃卧推", builtInKey = "barbell_bench_press")
        coEvery { assetRepo.getByBuiltInKey("barbell_bench_press") } returns null
        return ExerciseDetailViewModel(
            SavedStateHandle(mapOf("exerciseId" to 1L)),
            exerciseRepo,
            assetRepo,
            sessionRepo,
            templateRepo,
        )
    }

    @Test
    fun exerciseDetail_light() = capture("exercise_detail_light_412") {
        FitLogTheme {
            com.example.fitlog.feature.exercise.ExerciseDetailScreen(
                viewModel = exerciseDetailViewModel(),
            )
        }
    }

    @Test
    fun exerciseDetail_dark() = capture("exercise_detail_dark_412") {
        FitLogTheme(darkTheme = true) {
            com.example.fitlog.feature.exercise.ExerciseDetailScreen(
                viewModel = exerciseDetailViewModel(),
            )
        }
    }

    // ── ExercisePicker ────────────────────────────────────────────────────

    private fun exercisePickerViewModel(): ExercisePickerViewModel {
        val exerciseRepo = mockk<com.example.fitlog.data.repository.ExerciseRepository>(relaxed = true)
        val sessionRepo = mockk<com.example.fitlog.data.repository.WorkoutSessionRepository>(relaxed = true)
        coEvery { exerciseRepo.getAllActive() } returns flowOf(exerciseList())
        return ExercisePickerViewModel(
            SavedStateHandle(mapOf("sessionId" to 1L)),
            exerciseRepo,
            sessionRepo,
        )
    }

    @Test
    fun exercisePicker_light() = capture("exercise_picker_light_412") {
        FitLogTheme {
            ExercisePickerScreen(viewModel = exercisePickerViewModel())
        }
    }

    @Test
    fun exercisePicker_dark_selected() = capture("exercise_picker_dark_selected_412") {
        FitLogTheme(darkTheme = true) {
            val vm = exercisePickerViewModel()
            vm.toggleSelection(1)
            vm.toggleSelection(2)
            ExercisePickerScreen(viewModel = vm)
        }
    }

    // ── WorkoutExecution ──────────────────────────────────────────────────

    private fun executionViewModel(): WorkoutExecutionViewModel {
        val sessionRepo = mockk<com.example.fitlog.data.repository.WorkoutSessionRepository>(relaxed = true)
        val exerciseRepo = mockk<com.example.fitlog.data.repository.ExerciseRepository>(relaxed = true)
        coEvery { sessionRepo.getDetail(1) } returns sessionDetail()
        coEvery { sessionRepo.getRestState(1) } returns Triple(null, null, null)
        coEvery { exerciseRepo.getAllActive() } returns flowOf(exerciseList())
        return WorkoutExecutionViewModel(
            SavedStateHandle(mapOf("sessionId" to 1L)),
            sessionRepo,
            exerciseRepo,
        )
    }

    @Test
    fun execution_light() = capture("execution_light_412") {
        FitLogTheme {
            WorkoutExecutionScreen(viewModel = executionViewModel())
        }
    }

    @Test
    fun execution_dark() = capture("execution_dark_412") {
        FitLogTheme(darkTheme = true) {
            WorkoutExecutionScreen(viewModel = executionViewModel())
        }
    }

    // ── TemplateList ──────────────────────────────────────────────────────

    private fun templateListViewModel(): TemplateListViewModel {
        val repo = mockk<com.example.fitlog.data.repository.WorkoutTemplateRepository>(relaxed = true)
        coEvery { repo.getAllActive() } returns flowOf(
            listOf(
                template(1, "Push A", "卧推与推举综合日"),
                template(2, "Pull B", "背部与二头训练日（长名称模板示例）"),
                template(3, "Legs C"),
            ),
        )
        coEvery { repo.getDetail(any()) } returns null
        val plannedRepo = mockk<com.example.fitlog.data.repository.PlannedWorkoutRepository>(relaxed = true)
        val scheduleRepo = mockk<com.example.fitlog.data.repository.WorkoutScheduleRepository>(relaxed = true)
        val dateProvider = mockk<com.example.fitlog.core.time.CurrentDateProvider>(relaxed = true)
        every { dateProvider.today() } returns fixedDate
        return TemplateListViewModel(repo, plannedRepo, scheduleRepo, dateProvider)
    }

    @Test
    fun templateList_light() = capture("template_list_light_412") {
        FitLogTheme {
            TemplateListScreen(viewModel = templateListViewModel(), isPickerMode = false)
        }
    }

    @Test
    fun templateList_dark() = capture("template_list_dark_412") {
        FitLogTheme(darkTheme = true) {
            TemplateListScreen(viewModel = templateListViewModel(), isPickerMode = false)
        }
    }

    // ── Calendar (Plan) ───────────────────────────────────────────────────

    private fun calendarViewModel(): CalendarViewModel {
        val calendarRepo = mockk<com.example.fitlog.data.repository.CalendarRepository>(relaxed = true)
        val plannedRepo = mockk<com.example.fitlog.data.repository.PlannedWorkoutRepository>(relaxed = true)
        val scheduleRepo = mockk<com.example.fitlog.data.repository.WorkoutScheduleRepository>(relaxed = true)
        val templateRepo = mockk<com.example.fitlog.data.repository.WorkoutTemplateRepository>(relaxed = true)
        val dateProvider = mockk<com.example.fitlog.core.time.CurrentDateProvider>(relaxed = true)
        every { dateProvider.today() } returns fixedDate
        coEvery { calendarRepo.getMonth(any()) } returns emptyList()
        coEvery { calendarRepo.getDayDetail(any()) } returns emptyList()
        coEvery { templateRepo.getAllActive() } returns flowOf(emptyList())
        return CalendarViewModel(calendarRepo, plannedRepo, scheduleRepo, templateRepo, dateProvider)
    }

    @Test
    fun calendar_light() = capture("calendar_light_412") {
        FitLogTheme {
            com.example.fitlog.feature.calendar.CalendarScreen(
                viewModel = calendarViewModel(),
            )
        }
    }

    @Test
    fun calendar_dark() = capture("calendar_dark_412") {
        FitLogTheme(darkTheme = true) {
            com.example.fitlog.feature.calendar.CalendarScreen(
                viewModel = calendarViewModel(),
            )
        }
    }

    // ── Nutrition ─────────────────────────────────────────────────────────

    private fun nutritionViewModel(): NutritionViewModel {
        val foodRepo = mockk<com.example.fitlog.data.repository.FoodRecordRepository>(relaxed = true)
        val profileRepo = mockk<com.example.fitlog.data.repository.UserProfileRepository>(relaxed = true)
        val dateProvider = mockk<com.example.fitlog.core.time.CurrentDateProvider>(relaxed = true)
        val advisor = mockk<com.example.fitlog.domain.nutrition.NutritionAdvisor>(relaxed = true)
        val foodProvider = mockk<com.example.fitlog.domain.nutrition.FoodDataProvider>(relaxed = true)
        every { dateProvider.today() } returns fixedDate
        coEvery { foodRepo.observeByDate(fixedDate) } returns flowOf(emptyList())
        coEvery { profileRepo.observe() } returns flowOf(null)
        return NutritionViewModel(foodRepo, profileRepo, dateProvider, advisor, foodProvider)
    }

    @Test
    fun nutrition_light() = capture("nutrition_light_412") {
        FitLogTheme {
            com.example.fitlog.feature.nutrition.NutritionScreen(viewModel = nutritionViewModel())
        }
    }

    @Test
    fun nutrition_dark() = capture("nutrition_dark_412") {
        FitLogTheme(darkTheme = true) {
            com.example.fitlog.feature.nutrition.NutritionScreen(viewModel = nutritionViewModel())
        }
    }

    // ── Progress ──────────────────────────────────────────────────────────

    private fun progressViewModel(): ProgressViewModel {
        val calculator = mockk<com.example.fitlog.domain.stats.WorkoutStreakCalculator>(relaxed = true)
        val sessionRepo = mockk<com.example.fitlog.data.repository.WorkoutSessionRepository>(relaxed = true)
        val overrideRepo = mockk<com.example.fitlog.data.repository.WorkoutPlanOverrideRepository>(relaxed = true)
        val progressRepo = mockk<com.example.fitlog.data.repository.ProgressRepository>(relaxed = true)
        coEvery { sessionRepo.getSessionsInRange(any(), any()) } returns emptyList()
        coEvery { overrideRepo.observeAll() } returns flowOf(emptyList())
        coEvery { progressRepo.getTrendPoints(any()) } returns listOf(
            TrendPoint(fixedDate.minusDays(2), weight = 80.5, volume = 500.0),
            TrendPoint(fixedDate.minusDays(1), volume = 400.0),
            TrendPoint(fixedDate, volume = 350.0),
        )
        return ProgressViewModel(calculator, sessionRepo, overrideRepo, progressRepo)
    }

    @Test
    fun progress_light() = capture("progress_light_412") {
        FitLogTheme {
            com.example.fitlog.feature.progress.ProgressScreen(viewModel = progressViewModel())
        }
    }

    @Test
    fun progress_dark() = capture("progress_dark_412") {
        FitLogTheme(darkTheme = true) {
            com.example.fitlog.feature.progress.ProgressScreen(viewModel = progressViewModel())
        }
    }

    // ── Profile ───────────────────────────────────────────────────────────

    private fun profileViewModel(): ProfileViewModel {
        val prefsRepo = mockk<com.example.fitlog.core.datastore.UserPreferencesRepository>(relaxed = true)
        val profileRepo = mockk<com.example.fitlog.data.repository.UserProfileRepository>(relaxed = true)
        val bodyRepo = mockk<com.example.fitlog.data.repository.BodyMeasurementRepository>(relaxed = true)
        coEvery { prefsRepo.preferences } returns flowOf(
            com.example.fitlog.core.datastore.UserPreferences(),
        )
        coEvery { profileRepo.observe() } returns flowOf(null)
        coEvery { bodyRepo.getLatestOnOrBefore(any()) } returns null
        return ProfileViewModel(prefsRepo, profileRepo, bodyRepo)
    }

    @Test
    fun profile_light() = capture("profile_light_412") {
        FitLogTheme {
            com.example.fitlog.feature.profile.ProfileScreen(viewModel = profileViewModel())
        }
    }

    @Test
    fun profile_dark() = capture("profile_dark_412") {
        FitLogTheme(darkTheme = true) {
            com.example.fitlog.feature.profile.ProfileScreen(viewModel = profileViewModel())
        }
    }

    // ── 捕获助手 ─────────────────────────────────────────────────────────

    private fun capture(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent { content() }
        // 无路径调用：Roborazzi 按测试名生成基准图（src/test/snapshots/images/），随仓库提交
        composeRule.onRoot().captureRoboImage()
    }
}
