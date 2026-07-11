package com.taskora.home

import com.taskora.home.data.AppData
import com.taskora.home.data.AppSettings
import com.taskora.home.data.MaintenanceCompletion
import com.taskora.home.data.MaintenanceScheduleType
import com.taskora.home.data.MaintenanceTask
import com.taskora.home.data.ReminderSettings
import com.taskora.home.data.Room
import com.taskora.home.data.ShoppingItem
import com.taskora.home.data.ShoppingPriority
import com.taskora.home.util.CalendarUtils
import com.taskora.home.util.HistoryGrouping
import com.taskora.home.util.HistoryUtils
import com.taskora.home.util.ReminderEvaluator
import com.taskora.home.util.StatusCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class UtilsTest {

    private val today = LocalDate.of(2026, 6, 15)

    private fun oneTime(id: String, date: String, roomId: String? = null) = MaintenanceTask(
        id = id, homeId = "h1", roomId = roomId, title = id,
        scheduleType = MaintenanceScheduleType.OneTime, specificDate = date
    )

    private fun completion(id: String, taskId: String, date: String, roomId: String? = null) =
        MaintenanceCompletion(id = id, taskId = taskId, homeId = "h1", roomId = roomId, completedDate = date, note = "")

    // ---- calendar markers -------------------------------------------------

    @Test
    fun calendarMarkersDetectOverdueSoonAndCompletion() {
        val tasks = listOf(
            oneTime("overdue", "2026-06-10"),
            oneTime("soon", "2026-06-20")
        )
        val computed = tasks.map { StatusCalculator.compute(it, emptyList(), 7, today) }
        val completions = listOf(completion("c1", "x", "2026-06-05"))
        val markers = CalendarUtils.markersForMonth(YearMonth.of(2026, 6), computed, completions, today)

        assertTrue(markers[LocalDate.of(2026, 6, 10)]?.overdue == true)
        assertTrue(markers[LocalDate.of(2026, 6, 20)]?.dueSoon == true)
        assertTrue(markers[LocalDate.of(2026, 6, 5)]?.hasCompletion == true)
        assertNull(markers[LocalDate.of(2026, 6, 1)])
    }

    @Test
    fun monthGridAlwaysHas42Cells() {
        val grid = CalendarUtils.monthGrid(YearMonth.of(2026, 2), com.taskora.home.data.WeekDay.Monday)
        assertEquals(42, grid.size)
    }

    // ---- history grouping -------------------------------------------------

    @Test
    fun historyGroupByDateIsReverseChronological() {
        val completions = listOf(
            completion("c1", "t1", "2026-01-01"),
            completion("c2", "t1", "2026-03-01"),
            completion("c3", "t1", "2026-02-01")
        )
        val sections = HistoryUtils.group(completions, HistoryGrouping.Date, emptyList(), emptyList())
        // First section should be the most recent date.
        assertEquals("Mar 1, 2026", sections.first().title)
    }

    @Test
    fun historyGroupByTaskUsesDeletedFallback() {
        val completions = listOf(completion("c1", "ghost", "2026-01-01"))
        val sections = HistoryUtils.group(completions, HistoryGrouping.Task, emptyList(), emptyList())
        assertEquals("Deleted Maintenance Task", sections.first().title)
    }

    @Test
    fun historyFilterByRoom() {
        val completions = listOf(
            completion("c1", "t1", "2026-01-01", roomId = "r1"),
            completion("c2", "t1", "2026-01-02", roomId = "r2")
        )
        val filtered = HistoryUtils.filter(completions, homeId = "h1", roomId = "r1")
        assertEquals(1, filtered.size)
        assertEquals("c1", filtered.first().id)
    }

    // ---- reminders --------------------------------------------------------

    @Test
    fun reminderReportsOverdue() {
        val computed = listOf(StatusCalculator.compute(oneTime("a", "2026-05-01"), emptyList(), 7, today))
        val reminder = ReminderEvaluator.evaluate(computed, emptyList(), AppSettings())
        assertNotNull(reminder)
        assertTrue(reminder!!.title.contains("overdue"))
    }

    @Test
    fun reminderNullWhenDisabled() {
        val computed = listOf(StatusCalculator.compute(oneTime("a", "2026-05-01"), emptyList(), 7, today))
        val settings = AppSettings(reminderSettings = ReminderSettings(enabled = false))
        assertNull(ReminderEvaluator.evaluate(computed, emptyList(), settings))
    }

    @Test
    fun reminderReportsHighPriorityShopping() {
        val shopping = listOf(
            ShoppingItem(id = "s1", homeId = "h1", title = "Bulb", priority = ShoppingPriority.High, checked = false)
        )
        val reminder = ReminderEvaluator.evaluate(emptyList(), shopping, AppSettings())
        assertNotNull(reminder)
    }

    // ---- room grouping helper --------------------------------------------

    @Test
    fun computeForHomeFiltersByHome() {
        val tasks = listOf(
            oneTime("a", "2026-07-01").copy(homeId = "h1"),
            oneTime("b", "2026-07-01").copy(homeId = "h2")
        )
        val data = AppData(maintenanceTasks = tasks)
        val computed = StatusCalculator.computeForHome("h1", data.maintenanceTasks, emptyList(), 7, today)
        assertEquals(1, computed.size)
        assertEquals("a", computed.first().task.id)
    }

    @Test
    fun roomLookupHandlesDeletedRoom() {
        val rooms = listOf(Room(id = "r1", homeId = "h1", name = "Kitchen"))
        assertNull(rooms.firstOrNull { it.id == "ghost" })
        assertEquals("Kitchen", rooms.first { it.id == "r1" }.name)
    }

    @Test
    fun emptyDataProducesNoComputedTasks() {
        val data = AppData()
        assertTrue(StatusCalculator.computeForHome("h1", data.maintenanceTasks, data.completions, 7, today).isEmpty())
        assertFalse(data.homes.isNotEmpty())
    }
}
