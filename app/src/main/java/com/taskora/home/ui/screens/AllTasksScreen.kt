package com.taskora.home.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.taskora.home.data.TaskPriority
import com.taskora.home.data.TaskStatus
import com.taskora.home.ui.components.DropdownField
import com.taskora.home.ui.components.EmptyState
import com.taskora.home.ui.components.MaintenanceTaskRow
import com.taskora.home.ui.components.TaskoraScaffold
import com.taskora.home.ui.navigation.Routes
import com.taskora.home.ui.navigation.TaskFilterKeys
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.StatusCalculator
import com.taskora.home.util.TaskSort
import com.taskora.home.util.today

private data class TabDef(val key: String, val label: String)

private val tabs = listOf(
    TabDef(TaskFilterKeys.ALL, "All"),
    TabDef(TaskFilterKeys.OVERDUE, "Overdue"),
    TabDef(TaskFilterKeys.DUE_SOON, "Due Soon"),
    TabDef(TaskFilterKeys.GOOD, "Good"),
    TabDef(TaskFilterKeys.UNSCHEDULED, "Unscheduled"),
    TabDef(TaskFilterKeys.DISABLED, "Disabled")
)

@Composable
fun AllTasksScreen(vm: TaskoraViewModel, nav: NavHostController, initialFilter: String?) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val home = vm.activeHome(data)

    val initialIndex = tabs.indexOfFirst { it.key == initialFilter }.let { if (it < 0) 0 else it }
    var tabIndex by remember { mutableIntStateOf(initialIndex) }

    // Filter selections. null sentinels represent "All".
    var roomFilter by remember { mutableStateOf<String?>(null) }   // null=all, ""=whole home, id=room
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var priorityFilter by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf(TaskSort.OverdueFirst) }

    TaskoraScaffold(
        title = "Tasks",
        floatingActionButton = {
            if (home != null) {
                FloatingActionButton(onClick = { nav.navigate(Routes.addEditTask()) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add task")
                }
            }
        }
    ) { padding ->
        if (home == null) {
            EmptyState(
                title = "No maintenance tasks yet.",
                message = "Add a home first to begin your home maintenance map.",
                modifier = Modifier.padding(padding)
            )
            return@TaskoraScaffold
        }

        val todayDate = today()
        val computed = vm.computedForHome(home.id, data, todayDate)
        val rooms = vm.roomsForHome(home.id, data)

        val currentTab = tabs[tabIndex.coerceIn(0, tabs.lastIndex)]

        val byTab = computed.filter { tc ->
            when (currentTab.key) {
                TaskFilterKeys.OVERDUE -> tc.task.enabled && tc.status == TaskStatus.Overdue
                TaskFilterKeys.DUE_SOON -> tc.task.enabled && tc.status == TaskStatus.Soon
                TaskFilterKeys.GOOD -> tc.task.enabled && tc.status == TaskStatus.Good
                TaskFilterKeys.UNSCHEDULED -> tc.task.enabled &&
                    (tc.status == TaskStatus.Unscheduled || tc.status == TaskStatus.InvalidSchedule)
                TaskFilterKeys.DISABLED -> !tc.task.enabled
                else -> true
            }
        }

        val filtered = byTab.filter { tc ->
            val roomOk = when (roomFilter) {
                null -> true
                "" -> tc.task.roomId == null
                else -> tc.task.roomId == roomFilter
            }
            val catOk = categoryFilter == null || tc.task.category.name == categoryFilter
            val prioOk = priorityFilter == null || tc.task.priority.name == priorityFilter
            roomOk && catOk && prioOk
        }

        val sorted = StatusCalculator.sort(filtered, sort) { rid -> roomName(vm, data, rid) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = tabIndex.coerceIn(0, tabs.lastIndex),
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = index == tabIndex,
                        onClick = { tabIndex = index },
                        text = { Text(tab.label) }
                    )
                }
            }

            // Filters
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                val roomOptions = listOf<String?>(null, "") + rooms.map { it.id }
                DropdownField(
                    label = "Room",
                    options = roomOptions,
                    selected = roomFilter,
                    optionLabel = {
                        when (it) {
                            null -> "All rooms"
                            "" -> "Whole Home"
                            else -> rooms.firstOrNull { r -> r.id == it }?.name ?: "Room"
                        }
                    },
                    onSelected = { roomFilter = it }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val catOptions = listOf<String?>(null) +
                        com.taskora.home.data.MaintenanceCategory.entries.map { it.name }
                    DropdownField(
                        label = "Category",
                        options = catOptions,
                        selected = categoryFilter,
                        optionLabel = {
                            if (it == null) "All" else com.taskora.home.util.categoryLabel(
                                com.taskora.home.data.MaintenanceCategory.valueOf(it)
                            )
                        },
                        onSelected = { categoryFilter = it },
                        modifier = Modifier.weight(1f)
                    )
                    val prioOptions = listOf<String?>(null, TaskPriority.High.name, TaskPriority.Normal.name)
                    DropdownField(
                        label = "Priority",
                        options = prioOptions,
                        selected = priorityFilter,
                        optionLabel = {
                            when (it) {
                                null -> "All"
                                TaskPriority.High.name -> "High"
                                else -> "Normal"
                            }
                        },
                        onSelected = { priorityFilter = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                DropdownField(
                    label = "Sort",
                    options = TaskSort.entries.toList(),
                    selected = sort,
                    optionLabel = {
                        when (it) {
                            TaskSort.OverdueFirst -> "Overdue first"
                            TaskSort.NearestDue -> "Nearest due date"
                            TaskSort.Room -> "Room"
                            TaskSort.Category -> "Category"
                            TaskSort.Title -> "Title"
                        }
                    },
                    onSelected = { sort = it }
                )
            }

            if (sorted.isEmpty()) {
                EmptyState(
                    title = emptyTitleFor(currentTab.key),
                    message = "Add a task to begin your home maintenance map."
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sorted, key = { it.task.id }) { tc ->
                        MaintenanceTaskRow(
                            computed = tc,
                            roomName = roomName(vm, data, tc.task.roomId),
                            onClick = { nav.navigate(Routes.taskDetail(tc.task.id)) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

private fun emptyTitleFor(key: String): String = when (key) {
    TaskFilterKeys.OVERDUE -> "No overdue tasks."
    TaskFilterKeys.DUE_SOON -> "No tasks due soon."
    TaskFilterKeys.GOOD -> "No tasks in good standing."
    TaskFilterKeys.UNSCHEDULED -> "No unscheduled tasks."
    TaskFilterKeys.DISABLED -> "No disabled tasks."
    else -> "No maintenance tasks yet."
}
