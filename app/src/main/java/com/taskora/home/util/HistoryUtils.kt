package com.taskora.home.util

import com.taskora.home.data.MaintenanceCategory
import com.taskora.home.data.MaintenanceCompletion
import com.taskora.home.data.MaintenanceTask
import com.taskora.home.data.Room
import java.time.LocalDate
import java.time.YearMonth

enum class HistoryGrouping { Date, Room, Task, Category }

/** A titled section of completion records. */
data class HistorySection(
    val title: String,
    val items: List<MaintenanceCompletion>
)

object HistoryUtils {

    /** Reverse-chronological order by completed date then created timestamp. */
    fun sortNewestFirst(completions: List<MaintenanceCompletion>): List<MaintenanceCompletion> =
        completions.sortedWith(
            compareByDescending<MaintenanceCompletion> { parseDate(it.completedDate) ?: LocalDate.MIN }
                .thenByDescending { it.completedTime }
                .thenByDescending { it.createdAt }
        )

    fun filter(
        completions: List<MaintenanceCompletion>,
        homeId: String,
        roomId: String? = null,
        category: MaintenanceCategory? = null,
        tasks: List<MaintenanceTask> = emptyList(),
        month: YearMonth? = null,
        year: Int? = null
    ): List<MaintenanceCompletion> {
        val taskCategory: Map<String, MaintenanceCategory> = tasks.associate { it.id to it.category }
        return completions.filter { c ->
            if (c.homeId != homeId) return@filter false
            if (roomId != null && c.roomId != roomId) return@filter false
            if (category != null && taskCategory[c.taskId] != category) return@filter false
            val d = parseDate(c.completedDate)
            if (month != null && (d == null || YearMonth.from(d) != month)) return@filter false
            if (year != null && (d == null || d.year != year)) return@filter false
            true
        }
    }

    fun group(
        completions: List<MaintenanceCompletion>,
        grouping: HistoryGrouping,
        tasks: List<MaintenanceTask>,
        rooms: List<Room>
    ): List<HistorySection> {
        val sorted = sortNewestFirst(completions)
        val taskById = tasks.associateBy { it.id }
        val roomById = rooms.associateBy { it.id }

        return when (grouping) {
            HistoryGrouping.Date -> sorted
                .groupBy { it.completedDate }
                .toSortedMap(compareByDescending { parseDate(it) ?: LocalDate.MIN })
                .map { (date, items) -> HistorySection(displayDate(date), items) }

            HistoryGrouping.Room -> sorted
                .groupBy { it.roomId }
                .map { (roomId, items) ->
                    val name = when {
                        roomId == null -> "Whole Home"
                        roomById[roomId] != null -> roomById[roomId]!!.name
                        else -> "Deleted Room"
                    }
                    HistorySection(name, items)
                }
                .sortedBy { it.title.lowercase() }

            HistoryGrouping.Task -> sorted
                .groupBy { it.taskId }
                .map { (taskId, items) ->
                    val name = taskById[taskId]?.title?.ifBlank { "Untitled Task" }
                        ?: "Deleted Maintenance Task"
                    HistorySection(name, items)
                }
                .sortedBy { it.title.lowercase() }

            HistoryGrouping.Category -> sorted
                .groupBy { taskById[it.taskId]?.category }
                .map { (category, items) ->
                    HistorySection(category?.let { categoryLabel(it) } ?: "Uncategorized", items)
                }
                .sortedBy { it.title.lowercase() }
        }
    }
}
