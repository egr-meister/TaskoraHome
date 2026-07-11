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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.taskora.home.ui.components.ConfirmDialog
import com.taskora.home.ui.components.EmptyState
import com.taskora.home.ui.components.SectionLabel
import com.taskora.home.ui.components.TaskoraScaffold
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.NoteLimits
import com.taskora.home.util.StatusCalculator
import com.taskora.home.util.Validation
import com.taskora.home.util.displayDate
import com.taskora.home.util.today

@Composable
fun CompletionDetailScreen(vm: TaskoraViewModel, nav: NavHostController, completionId: String?) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val completion = vm.completionById(completionId, data)

    if (completion == null) {
        TaskoraScaffold(title = "Completion", onBack = { nav.popBackStack() }) { p ->
            EmptyState(
                title = "Completion not found",
                message = "This completion record is no longer available.",
                modifier = Modifier.padding(p)
            )
        }
        return
    }

    val task = vm.taskById(completion.taskId, data)
    val nextDue = task?.let {
        StatusCalculator.compute(it, data.completions, data.settings.soonThresholdDays, today()).nextDue
    }

    var noteText by remember { mutableStateOf(completion.note) }
    var showDelete by remember { mutableStateOf(false) }

    TaskoraScaffold(title = "Completion", onBack = { nav.popBackStack() }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InfoRow2("Task", task?.title?.ifBlank { "Task" } ?: "Deleted Maintenance Task")
            InfoRow2("Room", roomName(vm, data, completion.roomId))
            InfoRow2("Completed date", displayDate(completion.completedDate))
            InfoRow2("Completed time", completion.completedTime.ifBlank { "—" })
            InfoRow2("Next due after task schedule", nextDue?.let { displayDate(it) } ?: "—")

            SectionLabel("Note")
            OutlinedTextField(
                value = noteText,
                onValueChange = { if (it.length <= NoteLimits.DETAILED) noteText = it },
                label = { Text("Completion note") },
                supportingText = { Text("${noteText.length}/${NoteLimits.DETAILED}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Button(
                onClick = {
                    vm.updateCompletionNote(completion.id, Validation.trimNote(noteText, NoteLimits.DETAILED))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Note") }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDelete = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete Completion") }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete this completion record?",
            text = "The task schedule will recalculate from the previous completion or start date.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                vm.deleteCompletion(completion.id)
                showDelete = false
                nav.popBackStack()
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun InfoRow2(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(150.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}
