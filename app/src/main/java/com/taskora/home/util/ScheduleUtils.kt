package com.taskora.home.util

import com.taskora.home.data.IntervalUnit
import com.taskora.home.data.MaintenanceCompletion
import com.taskora.home.data.MaintenanceScheduleType
import com.taskora.home.data.MaintenanceTask
import com.taskora.home.data.NextDateCalculationMode
import java.time.LocalDate
import java.time.YearMonth

/** Result of computing a task's next due date. */
sealed class DueResult {
    data class Due(val date: LocalDate) : DueResult()

    /** Task has no scheduled date on purpose (ManualOnly). */
    data object Manual : DueResult()

    /** A one-time task that has already been completed. */
    data object CompletedOneTime : DueResult()

    /** Schedule data is present but unusable (bad interval / date). */
    data object Invalid : DueResult()
}

/**
 * Pure, side-effect-free schedule calculations. All arithmetic is calendar
 * based via [LocalDate]; no millisecond math is used, so month/year intervals
 * remain correct across differing month lengths and leap years.
 */
object ScheduleCalculator {

    /** The most recent valid completion for [taskId], by completed date. */
    fun latestCompletionDate(taskId: String, completions: List<MaintenanceCompletion>): LocalDate? =
        completions.asSequence()
            .filter { it.taskId == taskId }
            .mapNotNull { parseDate(it.completedDate) }
            .maxOrNull()

    /** How many completions exist for a task. */
    fun completionCount(taskId: String, completions: List<MaintenanceCompletion>): Int =
        completions.count { it.taskId == taskId }

    /** Computes the next due date for [task] given its completion history. */
    fun nextDue(task: MaintenanceTask, completions: List<MaintenanceCompletion>): DueResult {
        return when (task.scheduleType) {
            MaintenanceScheduleType.ManualOnly -> DueResult.Manual

            MaintenanceScheduleType.OneTime -> {
                val done = completions.any { it.taskId == task.id }
                if (done) return DueResult.CompletedOneTime
                val d = parseDate(task.specificDate) ?: return DueResult.Invalid
                DueResult.Due(d)
            }

            MaintenanceScheduleType.EveryNumberOfDays,
            MaintenanceScheduleType.EveryNumberOfWeeks,
            MaintenanceScheduleType.EveryNumberOfMonths -> intervalNextDue(task, completions)

            MaintenanceScheduleType.SelectedMonths -> selectedMonthsNextDue(task, completions)

            MaintenanceScheduleType.Yearly -> yearlyNextDue(task, completions)
        }
    }

    // ---- interval-based ---------------------------------------------------

    private fun unitFor(task: MaintenanceTask): IntervalUnit = when (task.scheduleType) {
        MaintenanceScheduleType.EveryNumberOfDays -> IntervalUnit.Days
        MaintenanceScheduleType.EveryNumberOfWeeks -> IntervalUnit.Weeks
        MaintenanceScheduleType.EveryNumberOfMonths -> IntervalUnit.Months
        else -> task.intervalUnit ?: IntervalUnit.Days
    }

    private fun step(date: LocalDate, unit: IntervalUnit, n: Int): LocalDate = when (unit) {
        IntervalUnit.Days -> date.plusDays(n.toLong())
        IntervalUnit.Weeks -> date.plusWeeks(n.toLong())
        IntervalUnit.Months -> date.plusMonths(n.toLong())
        IntervalUnit.Years -> date.plusYears(n.toLong())
    }

    private fun intervalNextDue(
        task: MaintenanceTask,
        completions: List<MaintenanceCompletion>
    ): DueResult {
        val n = task.intervalValue ?: return DueResult.Invalid
        if (n <= 0) return DueResult.Invalid
        val start = parseDate(task.startDate) ?: return DueResult.Invalid
        val unit = unitFor(task)
        val latest = latestCompletionDate(task.id, completions)
            ?: return DueResult.Due(start) // never completed -> first due is the start date

        return when (task.calculationMode) {
            NextDateCalculationMode.FromCompletionDate -> DueResult.Due(step(latest, unit, n))
            NextDateCalculationMode.FromScheduledDate -> {
                // Keep the fixed cadence anchored to startDate: advance in whole
                // steps until strictly after the latest completion.
                var d = start
                var guard = 0
                while (!d.isAfter(latest) && guard < 200_000) {
                    d = step(d, unit, n)
                    guard++
                }
                DueResult.Due(d)
            }
        }
    }

    // ---- selected months --------------------------------------------------

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate? {
        if (month !in 1..12) return null
        return try {
            val ym = YearMonth.of(year, month)
            LocalDate.of(year, month, day.coerceIn(1, ym.lengthOfMonth()))
        } catch (_: Exception) {
            null
        }
    }

    private fun firstOccurrence(
        months: List<Int>,
        day: Int,
        threshold: LocalDate,
        inclusive: Boolean
    ): LocalDate? {
        val sorted = months.filter { it in 1..12 }.sorted()
        if (sorted.isEmpty()) return null
        var year = threshold.year
        repeat(8) {
            for (m in sorted) {
                val d = safeDate(year, m, day) ?: continue
                val ok = if (inclusive) !d.isBefore(threshold) else d.isAfter(threshold)
                if (ok) return d
            }
            year++
        }
        return null
    }

    private fun selectedMonthsNextDue(
        task: MaintenanceTask,
        completions: List<MaintenanceCompletion>
    ): DueResult {
        val months = task.selectedMonths.filter { it in 1..12 }
        if (months.isEmpty()) return DueResult.Invalid
        val start = parseDate(task.startDate) ?: return DueResult.Invalid
        val day = start.dayOfMonth
        val latest = latestCompletionDate(task.id, completions)
        val result = if (latest == null) {
            firstOccurrence(months, day, start, inclusive = true)
        } else {
            firstOccurrence(months, day, latest, inclusive = false)
        }
        return result?.let { DueResult.Due(it) } ?: DueResult.Invalid
    }

    // ---- yearly -----------------------------------------------------------

    private fun yearlyNextDue(
        task: MaintenanceTask,
        completions: List<MaintenanceCompletion>
    ): DueResult {
        val month = task.yearlyMonth ?: return DueResult.Invalid
        val day = task.yearlyDay ?: return DueResult.Invalid
        if (month !in 1..12 || day !in 1..31) return DueResult.Invalid
        val start = parseDate(task.startDate) ?: return DueResult.Invalid
        val latest = latestCompletionDate(task.id, completions)
        val result = if (latest == null) {
            firstOccurrence(listOf(month), day, start, inclusive = true)
        } else {
            firstOccurrence(listOf(month), day, latest, inclusive = false)
        }
        return result?.let { DueResult.Due(it) } ?: DueResult.Invalid
    }
}
