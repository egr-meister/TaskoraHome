package com.taskora.home.util

import com.taskora.home.data.MaintenanceCompletion
import com.taskora.home.data.WeekDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** One cell in the month grid. */
data class CalendarCell(
    val date: LocalDate,
    val inCurrentMonth: Boolean
)

/** Accessible marker information for a single calendar day. */
data class DayMarkers(
    val date: LocalDate,
    val dueSoon: Boolean,
    val overdue: Boolean,
    val hasCompletion: Boolean,
    val dueCount: Int,
    val completionCount: Int
) {
    val hasAnyMarker: Boolean get() = dueSoon || overdue || hasCompletion

    /** Screen-reader friendly summary. */
    fun accessibilityLabel(): String {
        val parts = mutableListOf<String>()
        if (overdue) parts += "overdue task"
        if (dueSoon) parts += "task due"
        if (hasCompletion) parts += "$completionCount completion recorded"
        return if (parts.isEmpty()) "no maintenance" else parts.joinToString(", ")
    }
}

object CalendarUtils {

    /**
     * Builds a stable 6-row (42 cell) month grid beginning on [firstDayOfWeek].
     * Dates outside the month are included and flagged, so the grid never
     * changes height between months.
     */
    fun monthGrid(yearMonth: YearMonth, firstDayOfWeek: WeekDay): List<CalendarCell> {
        val firstDow = if (firstDayOfWeek == WeekDay.Monday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
        val firstOfMonth = yearMonth.atDay(1)

        // Number of days to step back so the grid starts on firstDow.
        val shift = ((firstOfMonth.dayOfWeek.value - firstDow.value) + 7) % 7
        val gridStart = firstOfMonth.minusDays(shift.toLong())

        return (0 until 42).map { offset ->
            val d = gridStart.plusDays(offset.toLong())
            CalendarCell(date = d, inCurrentMonth = YearMonth.from(d) == yearMonth)
        }
    }

    /** Header labels (Mon..Sun or Sun..Sat) matching [firstDayOfWeek]. */
    fun weekdayHeaders(firstDayOfWeek: WeekDay): List<String> {
        val base = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return if (firstDayOfWeek == WeekDay.Monday) base else listOf("Sun") + base.dropLast(1)
    }

    /**
     * Computes markers for every day in [yearMonth]. A task contributes a
     * "due" marker on its computed next-due date; completions contribute a
     * completion marker on their completed date.
     */
    fun markersForMonth(
        yearMonth: YearMonth,
        computed: List<TaskComputed>,
        completions: List<MaintenanceCompletion>,
        todayDate: LocalDate = today()
    ): Map<LocalDate, DayMarkers> {
        val dueByDate = HashMap<LocalDate, MutableList<TaskComputed>>()
        computed.forEach { tc ->
            val d = tc.nextDue ?: return@forEach
            if (YearMonth.from(d) == yearMonth) {
                dueByDate.getOrPut(d) { mutableListOf() }.add(tc)
            }
        }

        val completionByDate = HashMap<LocalDate, Int>()
        completions.forEach { c ->
            val d = parseDate(c.completedDate) ?: return@forEach
            if (YearMonth.from(d) == yearMonth) {
                completionByDate[d] = (completionByDate[d] ?: 0) + 1
            }
        }

        val allDates = dueByDate.keys + completionByDate.keys
        return allDates.associateWith { date ->
            val dueList = dueByDate[date].orEmpty()
            val overdue = dueList.any { it.nextDue != null && it.nextDue.isBefore(todayDate) }
            val dueSoon = dueList.any { it.nextDue != null && !it.nextDue.isBefore(todayDate) }
            DayMarkers(
                date = date,
                dueSoon = dueSoon,
                overdue = overdue,
                hasCompletion = (completionByDate[date] ?: 0) > 0,
                dueCount = dueList.size,
                completionCount = completionByDate[date] ?: 0
            )
        }
    }

    /** Tasks whose next due date is exactly [date]. */
    fun tasksDueOn(date: LocalDate, computed: List<TaskComputed>): List<TaskComputed> =
        computed.filter { it.nextDue == date }

    /** Completions recorded on [date]. */
    fun completionsOn(date: LocalDate, completions: List<MaintenanceCompletion>): List<MaintenanceCompletion> =
        completions.filter { parseDate(it.completedDate) == date }
}
