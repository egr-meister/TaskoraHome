package com.taskora.home

import com.taskora.home.data.MaintenanceScheduleType
import com.taskora.home.data.MaintenanceTask
import com.taskora.home.data.TaskStatus
import com.taskora.home.data.ZoneStatus
import com.taskora.home.util.StatusCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatusCalculatorTest {

    private val today = LocalDate.of(2026, 6, 1)

    private fun oneTime(id: String, date: String, roomId: String? = null, enabled: Boolean = true) =
        MaintenanceTask(
            id = id,
            homeId = "h1",
            roomId = roomId,
            title = id,
            scheduleType = MaintenanceScheduleType.OneTime,
            specificDate = date,
            enabled = enabled
        )

    private fun manual(id: String, roomId: String? = null) = MaintenanceTask(
        id = id, homeId = "h1", roomId = roomId, title = id,
        scheduleType = MaintenanceScheduleType.ManualOnly
    )

    @Test
    fun goodStatus() {
        val tc = StatusCalculator.compute(oneTime("a", "2026-07-01"), emptyList(), 7, today)
        assertEquals(TaskStatus.Good, tc.status)
    }

    @Test
    fun soonStatusWithinThreshold() {
        val tc = StatusCalculator.compute(oneTime("a", "2026-06-05"), emptyList(), 7, today)
        assertEquals(TaskStatus.Soon, tc.status)
    }

    @Test
    fun soonStatusToday() {
        val tc = StatusCalculator.compute(oneTime("a", "2026-06-01"), emptyList(), 7, today)
        assertEquals(TaskStatus.Soon, tc.status)
    }

    @Test
    fun overdueStatus() {
        val tc = StatusCalculator.compute(oneTime("a", "2026-05-01"), emptyList(), 7, today)
        assertEquals(TaskStatus.Overdue, tc.status)
    }

    @Test
    fun disabledStatus() {
        val tc = StatusCalculator.compute(oneTime("a", "2026-05-01", enabled = false), emptyList(), 7, today)
        assertEquals(TaskStatus.Disabled, tc.status)
    }

    @Test
    fun unscheduledStatusForManual() {
        val tc = StatusCalculator.compute(manual("a"), emptyList(), 7, today)
        assertEquals(TaskStatus.Unscheduled, tc.status)
    }

    @Test
    fun roomStatusAggregatesOverdue() {
        val tasks = listOf(
            oneTime("a", "2026-07-01", roomId = "r1"),
            oneTime("b", "2026-05-01", roomId = "r1")
        )
        val computed = tasks.map { StatusCalculator.compute(it, emptyList(), 7, today) }
        assertEquals(ZoneStatus.Overdue, StatusCalculator.roomStatus("r1", computed))
    }

    @Test
    fun roomStatusSoonWhenNoOverdue() {
        val tasks = listOf(
            oneTime("a", "2026-07-01", roomId = "r1"),
            oneTime("b", "2026-06-03", roomId = "r1")
        )
        val computed = tasks.map { StatusCalculator.compute(it, emptyList(), 7, today) }
        assertEquals(ZoneStatus.Soon, StatusCalculator.roomStatus("r1", computed))
    }

    @Test
    fun roomStatusNoTasksWhenEmpty() {
        assertEquals(ZoneStatus.NoTasks, StatusCalculator.roomStatus("rX", emptyList()))
    }

    @Test
    fun wholeHomeStatusUsesNullRoomTasks() {
        val tasks = listOf(
            oneTime("a", "2026-05-01", roomId = null),
            oneTime("b", "2026-07-01", roomId = "r1")
        )
        val computed = tasks.map { StatusCalculator.compute(it, emptyList(), 7, today) }
        assertEquals(ZoneStatus.Overdue, StatusCalculator.wholeHomeStatus(computed))
    }

    @Test
    fun deletedRoomReferenceDoesNotCrashAggregation() {
        // Task references a room id that no longer exists; roomStatus for the
        // missing id simply yields NoTasks, and the task's own status is fine.
        val tasks = listOf(oneTime("a", "2026-07-01", roomId = "ghost"))
        val computed = tasks.map { StatusCalculator.compute(it, emptyList(), 7, today) }
        assertEquals(ZoneStatus.NoTasks, StatusCalculator.roomStatus("r1", computed))
        assertEquals(ZoneStatus.Good, StatusCalculator.roomStatus("ghost", computed))
    }

    @Test
    fun sortingPutsOverdueFirst() {
        val tasks = listOf(
            oneTime("good", "2026-07-01"),
            oneTime("overdue", "2026-05-01"),
            oneTime("soon", "2026-06-03")
        )
        val computed = tasks.map { StatusCalculator.compute(it, emptyList(), 7, today) }
        val sorted = StatusCalculator.sort(computed, com.taskora.home.util.TaskSort.OverdueFirst)
        assertEquals("overdue", sorted.first().task.id)
    }
}
