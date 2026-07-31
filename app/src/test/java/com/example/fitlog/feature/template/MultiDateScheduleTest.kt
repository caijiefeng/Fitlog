package com.example.fitlog.feature.template

import com.example.fitlog.core.database.dao.PlannedWorkoutDao
import com.example.fitlog.core.database.entity.PlannedWorkoutEntity
import com.example.fitlog.core.model.WorkoutTemplate
import com.example.fitlog.core.time.CurrentDateProvider
import com.example.fitlog.data.repository.BatchScheduleResult
import com.example.fitlog.data.repository.PlannedWorkoutRepository
import com.example.fitlog.data.repository.WorkoutScheduleRepository
import com.example.fitlog.data.repository.WorkoutTemplateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Covers multi-day and multi-weekday scheduling: batch creation through
 * [PlannedWorkoutRepository.createMany] (with unique-index dedup) and the
 * [TemplateListViewModel] schedule dialog state machine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MultiDateScheduleTest {

    // ── PlannedWorkoutRepository.createMany ──────────────────────────────────

    private val today = LocalDate.of(2026, 7, 29) // Wednesday

    @Test
    fun `createMany splits rows into created and skipped`() = runTest {
        val dao = mockk<PlannedWorkoutDao>()
        val repo = PlannedWorkoutRepository(dao)

        val dates = listOf(
            LocalDate.of(2026, 8, 3),
            LocalDate.of(2026, 8, 5),
            LocalDate.of(2026, 8, 7),
        )
        coEvery { dao.insertAllIgnore(any()) } returns longArrayOf(11L, -1L, 13L)

        val result = repo.createMany(templateId = 9L, plannedDates = dates)

        assertEquals(listOf(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 7)), result.created.map { it.plannedDate })
        assertEquals(listOf(LocalDate.of(2026, 8, 5)), result.skipped)
        assertEquals(2, result.createdCount)
        assertEquals(1, result.skippedCount)
        assertTrue(result.created.all { it.templateId == 9L })
        coVerify(exactly = 1) {
            dao.insertAllIgnore(match { entities ->
                entities.map { it.plannedDate } == dates.map { it.toEpochDay() }
            })
        }
    }

    @Test
    fun `createMany collapses duplicate dates and returns empty result for empty input`() = runTest {
        val dao = mockk<PlannedWorkoutDao>()
        val repo = PlannedWorkoutRepository(dao)

        val empty = repo.createMany(templateId = 1L, plannedDates = emptyList())
        assertEquals(0, empty.created.size)
        assertEquals(0, empty.skipped.size)
        coVerify(exactly = 0) { dao.insertAllIgnore(any()) }

        val date = LocalDate.of(2026, 8, 10)
        coEvery { dao.insertAllIgnore(any()) } returns longArrayOf(-1L)
        val dup = repo.createMany(templateId = 1L, plannedDates = listOf(date, date))
        assertEquals(1, dup.skipped.size)
        coVerify {
            dao.insertAllIgnore(match { it.size == 1 }) // duplicates collapsed to one row
        }
    }

    // ── TemplateListViewModel: multi-date one-time scheduling ────────────────

    private val templateRepo = mockk<WorkoutTemplateRepository>(relaxed = true)
    private val plannedRepo = mockk<PlannedWorkoutRepository>(relaxed = true)
    private val scheduleRepo = mockk<WorkoutScheduleRepository>(relaxed = true)
    private val dateProvider = mockk<CurrentDateProvider>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { templateRepo.getAllActive() } returns flowOf(
            listOf(WorkoutTemplate(id = 10L, name = "Push Day"))
        )
        coEvery { dateProvider.today() } returns today
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TemplateListViewModel {
        val vm = TemplateListViewModel(templateRepo, plannedRepo, scheduleRepo, dateProvider)
        testDispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    @Test
    fun `one-time scheduling calls createMany with every selected date`() = runTest(testDispatcher) {
        coEvery { plannedRepo.createMany(any(), any(), any()) } returns
            BatchScheduleResult(created = emptyList(), skipped = emptyList())

        val vm = createViewModel()
        vm.onScheduleClick(templateId = 10L, templateName = "Push Day")
        vm.toggleScheduleDate(LocalDate.of(2026, 8, 3))
        vm.toggleScheduleDate(LocalDate.of(2026, 8, 5))
        vm.toggleScheduleDate(LocalDate.of(2026, 8, 7))

        assertTrue(vm.uiState.value.canConfirmSchedule)
        vm.confirmSchedule()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            plannedRepo.createMany(
                templateId = 10L,
                plannedDates = listOf(
                    LocalDate.of(2026, 8, 3),
                    LocalDate.of(2026, 8, 5),
                    LocalDate.of(2026, 8, 7),
                ),
                note = null,
            )
        }
        coVerify(exactly = 0) { scheduleRepo.createRecurringForWeekdays(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `toggling a date twice removes it`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onScheduleClick(templateId = 10L, templateName = "Push Day")

        val date = LocalDate.of(2026, 8, 3)
        vm.toggleScheduleDate(date)
        assertEquals(setOf(date), vm.uiState.value.selectedDates)
        vm.toggleScheduleDate(date)
        assertTrue(vm.uiState.value.selectedDates.isEmpty())
        assertFalse(vm.uiState.value.canConfirmSchedule)
    }

    @Test
    fun `past dates cannot be selected and confirm is disabled without selection`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onScheduleClick(templateId = 10L, templateName = "Push Day")

        vm.toggleScheduleDate(today.minusDays(1))
        assertTrue(vm.uiState.value.selectedDates.isEmpty())

        vm.confirmSchedule()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { plannedRepo.createMany(any(), any(), any()) }
    }

    @Test
    fun `selectThisWeek only picks the remaining days of the current week`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onScheduleClick(templateId = 10L, templateName = "Push Day")

        vm.selectThisWeek()
        assertEquals(
            setOf(
                LocalDate.of(2026, 7, 29),
                LocalDate.of(2026, 7, 30),
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
            ),
            vm.uiState.value.selectedDates,
        )
    }

    @Test
    fun `selectNextWeek picks all seven days of next week`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onScheduleClick(templateId = 10L, templateName = "Push Day")

        vm.selectNextWeek()
        assertEquals(
            (3L..9L).map { LocalDate.of(2026, 8, it.toInt()) }.toSet(),
            vm.uiState.value.selectedDates,
        )
    }

    @Test
    fun `clearSelectedDates empties the selection`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onScheduleClick(templateId = 10L, templateName = "Push Day")

        vm.selectNextWeek()
        vm.clearSelectedDates()
        assertTrue(vm.uiState.value.selectedDates.isEmpty())
    }

    @Test
    fun `skipped dates are reported back in the snackbar`() = runTest(testDispatcher) {
        coEvery { plannedRepo.createMany(any(), any(), any()) } returns BatchScheduleResult(
            created = listOf(
                com.example.fitlog.data.repository.PlannedWorkout(
                    id = 1, templateId = 10L,
                    plannedDate = LocalDate.of(2026, 8, 3), note = null, createdAt = 0,
                )
            ),
            skipped = listOf(LocalDate.of(2026, 8, 5)),
        )

        val vm = createViewModel()
        vm.onScheduleClick(templateId = 10L, templateName = "Push Day")
        vm.toggleScheduleDate(LocalDate.of(2026, 8, 3))
        vm.toggleScheduleDate(LocalDate.of(2026, 8, 5))
        vm.confirmSchedule()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.showScheduleDialog)
    }

    // ── TemplateListViewModel: multi-weekday recurring scheduling ────────────

    @Test
    fun `recurring scheduling calls createRecurringForWeekdays with all weekdays`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onScheduleClick(templateId = 10L, templateName = "Push Day")

        vm.setOneTime(false)
        vm.toggleWeekday(1)
        vm.toggleWeekday(3)
        vm.toggleWeekday(5)
        val start = LocalDate.of(2026, 8, 3)
        val end = LocalDate.of(2026, 9, 30)
        vm.setStartDate(start)
        vm.setEndDate(end)
        vm.setRepeatIntervalWeeks(2)

        assertTrue(vm.uiState.value.canConfirmSchedule)
        vm.confirmSchedule()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            scheduleRepo.createRecurringForWeekdays(
                dayOfWeeks = listOf(1, 3, 5),
                templateId = 10L,
                startDate = start,
                endDate = end,
                repeatIntervalWeeks = 2,
            )
        }
        coVerify(exactly = 0) { plannedRepo.createMany(any(), any(), any()) }
    }

    @Test
    fun `toggling a weekday twice removes it`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onScheduleClick(templateId = 10L, templateName = "Push Day")
        vm.setOneTime(false)

        vm.toggleWeekday(2)
        vm.toggleWeekday(2)
        assertTrue(vm.uiState.value.selectedWeekdays.isEmpty())
        assertFalse(vm.uiState.value.canConfirmSchedule)
    }

    @Test
    fun `endDate before startDate is rejected`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onScheduleClick(templateId = 10L, templateName = "Push Day")
        vm.setOneTime(false)

        vm.setStartDate(LocalDate.of(2026, 8, 10))
        vm.setEndDate(LocalDate.of(2026, 8, 3))
        assertEquals(null, vm.uiState.value.endDate)

        vm.setEndDate(LocalDate.of(2026, 8, 20))
        assertEquals(LocalDate.of(2026, 8, 20), vm.uiState.value.endDate)
    }

    @Test
    fun `clearEndDate makes the schedule indefinite`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onScheduleClick(templateId = 10L, templateName = "Push Day")
        vm.setOneTime(false)

        vm.setEndDate(LocalDate.of(2026, 9, 30))
        vm.clearEndDate()
        assertEquals(null, vm.uiState.value.endDate)
    }

    @Test
    fun `startDate is initialized to today when dialog opens`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onScheduleClick(templateId = 10L, templateName = "Push Day")
        assertEquals(today, vm.uiState.value.startDate)
    }
}
