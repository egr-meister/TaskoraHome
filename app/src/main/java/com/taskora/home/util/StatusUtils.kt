package com.taskora.home.util

import com.taskora.home.data.MaintenanceCompletion
import com.taskora.home.data.MaintenanceTask
import com.taskora.home.data.TaskPriority
import com.taskora.home.data.TaskStatus
import com.taskora.home.data.ZoneStatus
import java.time.LocalDate

/** A task combined with its locally-computed schedule + status. */
data class TaskComputed(
    val task: MaintenanceTask,
    val status: TaskStatus,
    val nextDue: LocalDate?,
    val dueResult: DueResult,
    val latestCompletion: LocalDate?,
    val completionCount: Int
) {
    val isActiveForStatus: Boolean
        get() = task.enabled &&
            (status == TaskStatus.Overdue || status == TaskStatus.Soon || status == TaskStatus.Good)
}

/** Sort options exposed in task lists. */
enum class TaskSort { OverdueFirst, NearestDue, Room, Category, Title }

object StatusCalculator {

    /** Computes the full [TaskComputed] view for a task. */
    fun compute(
        task: MaintenanceTask,
        completions: List<MaintenanceCompletion>,
        soonThresholdDays: Int,
        todayDate: LocalDate = today()
    ): TaskComputed {
        val due = ScheduleCalculator.nextDue(task, completions)
        val latest = ScheduleCalculator.latestCompletionDate(task.id, completions)
        val count = ScheduleCalculator.completionCount(task.id, completions)

        val status: TaskStatus = when {
            !task.enabled -> TaskStatus.Disabled
            due is DueResult.Manual -> TaskStatus.Unscheduled
            due is DueResult.Invalid -> TaskStatus.InvalidSchedule
            due is DueResult.CompletedOneTime -> TaskStatus.Completed
            due is DueResult.Due -> {
                val date = due.date
                when {
                    date.isBefore(todayDate) -> TaskStatus.Overdue
                    !date.isAfter(todayDate.plusDays(soonThresholdDays.toLong())) -> TaskStatus.Soon
                    else -> TaskStatus.Good
                }
            }
            else -> TaskStatus.Unscheduled
        }

        val nextDue = (due as? DueResult.Due)?.date
        return TaskComputed(task, status, nextDue, due, latest, count)
    }

    /** Computes all tasks for a home into [TaskComputed] views. */
    fun computeForHome(
        homeId: String,
        tasks: List<MaintenanceTask>,
        completions: List<MaintenanceCompletion>,
        soonThresholdDays: Int,
        todayDate: LocalDate = today()
    ): List<TaskComputed> =
        tasks.filter { it.homeId == homeId }
            .map { compute(it, completions, soonThresholdDays, todayDate) }

    // ---- aggregation ------------------------------------------------------

    /** Aggregates a set of computed tasks into a zone (room / whole-home) status. */
    fun zoneStatus(computed: List<TaskComputed>): ZoneStatus {
        val enabled = computed.filter { it.task.enabled }
        if (enabled.isEmpty()) return ZoneStatus.NoTasks
        val active = enabled.filter { it.isActiveForStatus }
        return when {
            active.any { it.status == TaskStatus.Overdue } -> ZoneStatus.Overdue
            active.any { it.status == TaskStatus.Soon } -> ZoneStatus.Soon
            else -> ZoneStatus.Good
        }
    }

    /** Zone status for a specific room. */
    fun roomStatus(
        roomId: String,
        computed: List<TaskComputed>
    ): ZoneStatus = zoneStatus(computed.filter { it.task.roomId == roomId })

    /** Zone status for the Whole-Home zone (tasks with no room). */
    fun wholeHomeStatus(computed: List<TaskComputed>): ZoneStatus =
        zoneStatus(computed.filter { it.task.roomId == null })

    // ---- counts -----------------------------------------------------------

    data class ZoneCounts(
        val active: Int,
        val overdue: Int,
        val soon: Int,
        val good: Int
    )

    fun counts(computed: List<TaskComputed>): ZoneCounts {
        val enabled = computed.filter { it.task.enabled }
        return ZoneCounts(
            active = enabled.count { it.isActiveForStatus },
            overdue = enabled.count { it.status == TaskStatus.Overdue },
            soon = enabled.count { it.status == TaskStatus.Soon },
            good = enabled.count { it.status == TaskStatus.Good }
        )
    }

    // ---- sorting ----------------------------------------------------------

    private fun statusRank(s: TaskStatus): Int = when (s) {
        TaskStatus.Overdue -> 0
        TaskStatus.Soon -> 1
        TaskStatus.Good -> 2
        TaskStatus.Unscheduled -> 3
        TaskStatus.Completed -> 4
        TaskStatus.InvalidSchedule -> 5
        TaskStatus.Disabled -> 6
    }

    private fun priorityRank(p: TaskPriority): Int = if (p == TaskPriority.High) 0 else 1

    fun sort(
        list: List<TaskComputed>,
        sort: TaskSort,
        roomNameLookup: (String?) -> String = { it ?: "" }
    ): List<TaskComputed> {
        val farFuture = LocalDate.MAX
        return when (sort) {
            TaskSort.OverdueFirst -> list.sortedWith(
                compareBy<TaskComputed> { statusRank(it.status) }
                    .thenBy { priorityRank(it.task.priority) }
                    .thenBy { it.nextDue ?: farFuture }
                    .thenBy { it.task.title.lowercase() }
            )
            TaskSort.NearestDue -> list.sortedWith(
                compareBy<TaskComputed> { it.nextDue ?: farFuture }
                    .thenBy { priorityRank(it.task.priority) }
                    .thenBy { it.task.title.lowercase() }
            )
            TaskSort.Room -> list.sortedWith(
                compareBy<TaskComputed> { roomNameLookup(it.task.roomId).lowercase() }
                    .thenBy { statusRank(it.status) }
                    .thenBy { it.task.title.lowercase() }
            )
            TaskSort.Category -> list.sortedWith(
                compareBy<TaskComputed> { it.task.category.name }
                    .thenBy { statusRank(it.status) }
                    .thenBy { it.task.title.lowercase() }
            )
            TaskSort.Title -> list.sortedBy { it.task.title.lowercase() }
        }
    }
}
