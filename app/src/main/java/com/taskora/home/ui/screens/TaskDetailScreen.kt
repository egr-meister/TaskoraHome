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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.taskora.home.ui.components.ConfirmDialog
import com.taskora.home.ui.components.DateField
import com.taskora.home.ui.components.DisclaimerBox
import com.taskora.home.ui.components.EmptyState
import com.taskora.home.ui.components.SectionLabel
import com.taskora.home.ui.components.TaskStatusPill
import com.taskora.home.ui.components.TaskoraScaffold
import com.taskora.home.ui.components.defaultDateString
import com.taskora.home.ui.navigation.Routes
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.Disclaimers
import com.taskora.home.util.NoteLimits
import com.taskora.home.util.StatusCalculator
import com.taskora.home.util.Validation
import com.taskora.home.util.categoryLabel
import com.taskora.home.util.displayDate
import com.taskora.home.util.recurrenceLabel
import com.taskora.home.util.today

@Composable
fun TaskDetailScreen(vm: TaskoraViewModel, nav: NavHostController, taskId: String?) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val task = vm.taskById(taskId, data)

    if (task == null) {
        TaskoraScaffold(title = "Task", onBack = { nav.popBackStack() }) { p ->
            EmptyState(
                title = "Deleted Maintenance Task",
                message = "This task is no longer available.",
                modifier = Modifier.padding(p)
            )
        }
        return
    }

    val todayDate = today()
    val computed = StatusCalculator.compute(task, data.completions, data.settings.soonThresholdDays, todayDate)
    val roomLabel = roomName(vm, data, task.roomId)
    val history = data.completions
        .filter { it.taskId == task.id }
        .sortedByDescending { it.completedDate }

    var showComplete by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var completeDate by remember { mutableStateOf(defaultDateString()) }
    var completeNote by remember { mutableStateOf("") }

    TaskoraScaffold(
        title = task.title.ifBlank { "Task" },
        onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = { nav.navigate(Routes.addEditTask(taskId = task.id)) }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit task")
            }
            IconButton(onClick = { showDelete = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete task")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TaskStatusPill(computed.status)

            InfoRow("Home", vm.activeHome(data)?.name?.ifBlank { "Home" } ?: "Home")
            InfoRow("Room", roomLabel)
            InfoRow("Category", categoryLabel(task.category))
            InfoRow("Priority", if (task.priority == com.taskora.home.data.TaskPriority.High) "High" else "Normal")
            InfoRow(
                "Next due",
                computed.nextDue?.let { displayDate(it) }
                    ?: if (computed.status == com.taskora.home.data.TaskStatus.InvalidSchedule) "Schedule unavailable" else "—"
            )
            InfoRow(
                "Recurrence",
                recurrenceLabel(task.scheduleType, task.intervalValue, task.selectedMonths, task.yearlyMonth, task.yearlyDay)
            )
            InfoRow("Start date", if (task.startDate.isBlank()) "—" else displayDate(task.startDate))
            InfoRow("Latest completion", computed.latestCompletion?.let { displayDate(it) } ?: "None")
            InfoRow("Completion count", computed.completionCount.toString())
            if (task.shoppingItemLabel.isNotBlank()) InfoRow("Shopping label", task.shoppingItemLabel)

            if (task.notes.isNotBlank()) {
                SectionLabel("Notes")
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(10.dp)
                )
            }

            DisclaimerBox(text = Disclaimers.MANUAL_ENTRY)

            SectionLabel("Actions")
            Button(
                onClick = {
                    completeDate = defaultDateString()
                    completeNote = ""
                    showComplete = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Mark Complete")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { vm.setTaskEnabled(task.id, !task.enabled) },
                    modifier = Modifier.weight(1f)
                ) { Text(if (task.enabled) "Disable" else "Enable") }
                OutlinedButton(
                    onClick = { nav.navigate(Routes.HISTORY) },
                    modifier = Modifier.weight(1f)
                ) { Text("View History") }
            }

            SectionLabel("Completion history")
            if (history.isEmpty()) {
                Text(
                    "No completions recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                history.forEach { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(displayDate(c.completedDate), style = MaterialTheme.typography.bodyMedium)
                            if (c.note.isNotBlank()) {
                                Text(
                                    c.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { nav.navigate(Routes.completionDetail(c.id)) }) {
                            Text("Open")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showComplete) {
        AlertDialog(
            onDismissRequest = { showComplete = false },
            title = { Text("Mark this task complete?") },
            text = {
                Column {
                    Text("Taskora Home will save the completion date and calculate the next due date.")
                    Spacer(Modifier.height(12.dp))
                    DateField(
                        label = "Completion date",
                        value = completeDate,
                        onValueChange = { completeDate = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = completeNote,
                        onValueChange = { if (it.length <= NoteLimits.SHORT) completeNote = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.recordCompletion(task, completeDate, completeNote)
                    showComplete = false
                }) { Text("Recorded as complete") }
            },
            dismissButton = {
                TextButton(onClick = { showComplete = false }) { Text("Cancel") }
            }
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete this task?",
            text = "This will also remove its completion history.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                vm.deleteTask(task.id)
                showDelete = false
                nav.popBackStack()
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(140.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}
