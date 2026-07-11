package com.taskora.home

import com.taskora.home.data.IntervalUnit
import com.taskora.home.data.MaintenanceCompletion
import com.taskora.home.data.MaintenanceScheduleType
import com.taskora.home.data.MaintenanceTask
import com.taskora.home.data.NextDateCalculationMode
import com.taskora.home.util.DueResult
import com.taskora.home.util.ScheduleCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleCalculatorTest {

    private fun task(
        id: String = "t1",
        type: MaintenanceScheduleType,
        interval: Int? = null,
        start: String = "",
        specific: String = "",
        months: List<Int> = emptyList(),
        yearlyMonth: Int? = null,
        yearlyDay: Int? = null,
        mode: NextDateCalculationMode = NextDateCalculationMode.FromCompletionDate
    ) = MaintenanceTask(
        id = id,
        homeId = "h1",
        title = "task",
        scheduleType = type,
        intervalValue = interval,
        intervalUnit = IntervalUnit.Days,
        selectedMonths = months,
        specificDate = specific,
        startDate = start,
        yearlyMonth = yearlyMonth,
        yearlyDay = yearlyDay,
        calculationMode = mode
    )

    private fun completion(taskId: String, date: String) =
        MaintenanceCompletion(id = "c_$date", taskId = taskId, homeId = "h1", completedDate = date)

    @Test
    fun oneTimeReturnsSpecificDate() {
        val t = task(type = MaintenanceScheduleType.OneTime, specific = "2026-05-10")
        val r = ScheduleCalculator.nextDue(t, emptyList())
        assertEquals(DueResult.Due(LocalDate.of(2026, 5, 10)), r)
    }

    @Test
    fun oneTimeCompletedHasNoNext() {
        val t = task(type = MaintenanceScheduleType.OneTime, specific = "2026-05-10")
        val r = ScheduleCalculator.nextDue(t, listOf(completion("t1", "2026-05-10")))
        assertEquals(DueResult.CompletedOneTime, r)
    }

    @Test
    fun dailyNoCompletionUsesStart() {
        val t = task(type = MaintenanceScheduleType.EveryNumberOfDays, interval = 5, start = "2026-01-01")
        val r = ScheduleCalculator.nextDue(t, emptyList())
        assertEquals(DueResult.Due(LocalDate.of(2026, 1, 1)), r)
    }

    @Test
    fun dailyFromCompletion() {
        val t = task(type = MaintenanceScheduleType.EveryNumberOfDays, interval = 7, start = "2026-01-01")
        val r = ScheduleCalculator.nextDue(t, listOf(completion("t1", "2026-02-01")))
        assertEquals(DueResult.Due(LocalDate.of(2026, 2, 8)), r)
    }

    @Test
    fun weeklyFromCompletion() {
        val t = task(type = MaintenanceScheduleType.EveryNumberOfWeeks, interval = 2, start = "2026-01-01")
        val r = ScheduleCalculator.nextDue(t, listOf(completion("t1", "2026-03-01")))
        assertEquals(DueResult.Due(LocalDate.of(2026, 3, 15)), r)
    }

    @Test
    fun monthlyFromCompletionCrossesMonthLengths() {
        val t = task(type = MaintenanceScheduleType.EveryNumberOfMonths, interval = 1, start = "2026-01-31")
        val r = ScheduleCalculator.nextDue(t, listOf(completion("t1", "2026-01-31")))
        // Jan 31 + 1 month -> Feb 28 (2026 is not a leap year)
        assertEquals(DueResult.Due(LocalDate.of(2026, 2, 28)), r)
    }

    @Test
    fun yearlyNoCompletion() {
        val t = task(
            type = MaintenanceScheduleType.Yearly,
            start = "2026-01-01",
            yearlyMonth = 3,
            yearlyDay = 10
        )
        val r = ScheduleCalculator.nextDue(t, emptyList())
        assertEquals(DueResult.Due(LocalDate.of(2026, 3, 10)), r)
    }

    @Test
    fun yearlyAfterCompletionRollsToNextYear() {
        val t = task(
            type = MaintenanceScheduleType.Yearly,
            start = "2026-01-01",
            yearlyMonth = 3,
            yearlyDay = 10
        )
        val r = ScheduleCalculator.nextDue(t, listOf(completion("t1", "2026-03-10")))
        assertEquals(DueResult.Due(LocalDate.of(2027, 3, 10)), r)
    }

    @Test
    fun selectedMonthsPicksEarliestOnOrAfterStart() {
        val t = task(
            type = MaintenanceScheduleType.SelectedMonths,
            start = "2026-01-05",
            months = listOf(9, 3)
        )
        val r = ScheduleCalculator.nextDue(t, emptyList())
        assertEquals(DueResult.Due(LocalDate.of(2026, 3, 5)), r)
    }

    @Test
    fun selectedMonthsAfterCompletion() {
        val t = task(
            type = MaintenanceScheduleType.SelectedMonths,
            start = "2026-01-05",
            months = listOf(3, 9)
        )
        val r = ScheduleCalculator.nextDue(t, listOf(completion("t1", "2026-03-05")))
        assertEquals(DueResult.Due(LocalDate.of(2026, 9, 5)), r)
    }

    @Test
    fun fromScheduledKeepsCadenceAnchoredToStart() {
        val t = task(
            type = MaintenanceScheduleType.EveryNumberOfDays,
            interval = 10,
            start = "2026-01-01",
            mode = NextDateCalculationMode.FromScheduledDate
        )
        // Completed early on Jan 5 -> next scheduled slot after Jan 5 is Jan 11.
        val r = ScheduleCalculator.nextDue(t, listOf(completion("t1", "2026-01-05")))
        assertEquals(DueResult.Due(LocalDate.of(2026, 1, 11)), r)
    }

    @Test
    fun manualOnlyIsManual() {
        val t = task(type = MaintenanceScheduleType.ManualOnly)
        assertEquals(DueResult.Manual, ScheduleCalculator.nextDue(t, emptyList()))
    }

    @Test
    fun zeroIntervalIsInvalid() {
        val t = task(type = MaintenanceScheduleType.EveryNumberOfDays, interval = 0, start = "2026-01-01")
        assertEquals(DueResult.Invalid, ScheduleCalculator.nextDue(t, emptyList()))
    }

    @Test
    fun invalidStartDateIsInvalid() {
        val t = task(type = MaintenanceScheduleType.EveryNumberOfDays, interval = 3, start = "not-a-date")
        assertEquals(DueResult.Invalid, ScheduleCalculator.nextDue(t, emptyList()))
    }

    @Test
    fun latestCompletionIsSelected() {
        val completions = listOf(
            completion("t1", "2026-01-01"),
            completion("t1", "2026-03-15"),
            completion("t1", "2026-02-10")
        )
        assertEquals(LocalDate.of(2026, 3, 15), ScheduleCalculator.latestCompletionDate("t1", completions))
    }

    @Test
    fun deletingLatestCompletionRecalculatesFromPrevious() {
        val t = task(type = MaintenanceScheduleType.EveryNumberOfDays, interval = 30, start = "2026-01-01")
        val all = listOf(completion("t1", "2026-02-01"), completion("t1", "2026-03-01"))
        val afterDelete = all.filterNot { it.completedDate == "2026-03-01" }
        val r = ScheduleCalculator.nextDue(t, afterDelete)
        assertTrue(r is DueResult.Due)
        assertEquals(LocalDate.of(2026, 3, 3), (r as DueResult.Due).date)
    }
}
