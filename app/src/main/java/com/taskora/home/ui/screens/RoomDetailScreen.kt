package com.taskora.home.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.taskora.home.data.TaskStatus
import com.taskora.home.ui.components.EmptyState
import com.taskora.home.ui.components.MaintenanceTaskRow
import com.taskora.home.ui.components.SectionLabel
import com.taskora.home.ui.components.TaskoraScaffold
import com.taskora.home.ui.components.ZoneStatusPill
import com.taskora.home.ui.navigation.Routes
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.StatusCalculator
import com.taskora.home.util.TaskSort
import com.taskora.home.util.displayDate
import com.taskora.home.util.today

@Composable
fun RoomDetailScreen(vm: TaskoraViewModel, nav: NavHostController, roomId: String?) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val room = vm.roomById(roomId, data)

    if (room == null) {
        TaskoraScaffold(title = "Room", onBack = { nav.popBackStack() }) { p ->
            EmptyState(
                title = "Deleted Room",
                message = "This room is no longer available.",
                modifier = Modifier.padding(p)
            )
        }
        return
    }

    val todayDate = today()
    val computed = vm.computedForHome(room.homeId, data, todayDate)
    val roomComputed = computed.filter { it.task.roomId == room.id }
    val status = StatusCalculator.roomStatus(room.id, computed)
    val counts = StatusCalculator.counts(roomComputed)

    val activeTasks = StatusCalculator.sort(
        roomComputed.filter { it.task.enabled },
        TaskSort.OverdueFirst
    )
    val disabledTasks = roomComputed.filter { !it.task.enabled }
    val nextTask = roomComputed
        .filter { it.isActiveForStatus && it.nextDue != null }
        .minByOrNull { it.nextDue!! }

    val recentHistory = data.completions
        .filter { it.roomId == room.id }
        .sortedByDescending { it.completedDate }
        .take(5)

    TaskoraScaffold(
        title = room.name.ifBlank { "Room" },
        onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = { nav.navigate(Routes.addEditRoom(room.id)) }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit room")
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.addEditTask(roomId = room.id)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Task") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                ZoneStatusPill(status)
            }
            Text(
                text = "${counts.active} active · ${counts.overdue} overdue · ${counts.soon} due soon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionLabel("Next task")
            if (nextTask != null) {
                MaintenanceTaskRow(
                    computed = nextTask,
                    roomName = room.name.ifBlank { "Room" },
                    onClick = { nav.navigate(Routes.taskDetail(nextTask.task.id)) }
                )
            } else {
                Text(
                    "No scheduled task.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionLabel("Active tasks")
            if (activeTasks.isEmpty()) {
                Text(
                    "No active tasks in this room yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                activeTasks.forEach { tc ->
                    MaintenanceTaskRow(
                        computed = tc,
                        roomName = room.name.ifBlank { "Room" },
                        onClick = { nav.navigate(Routes.taskDetail(tc.task.id)) }
                    )
                }
            }

            if (disabledTasks.isNotEmpty()) {
                SectionLabel("Disabled tasks")
                disabledTasks.forEach { tc ->
                    MaintenanceTaskRow(
                        computed = tc,
                        roomName = room.name.ifBlank { "Room" },
                        onClick = { nav.navigate(Routes.taskDetail(tc.task.id)) }
                    )
                }
            }

            SectionLabel("Recent history")
            if (recentHistory.isEmpty()) {
                Text(
                    "No maintenance history yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                recentHistory.forEach { c ->
                    val task = vm.taskById(c.taskId, data)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(10.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = task?.title?.ifBlank { "Task" } ?: "Deleted Maintenance Task",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = displayDate(c.completedDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (room.notes.isNotBlank()) {
                SectionLabel("Room notes")
                Text(
                    text = room.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(10.dp)
                )
            }

            Spacer(Modifier.height(90.dp))
        }
    }
}
