package com.taskora.home.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.taskora.home.data.HomeProfile
import com.taskora.home.data.HomeType
import com.taskora.home.data.TaskStatus
import com.taskora.home.ui.components.ConfirmDialog
import com.taskora.home.ui.components.DropdownField
import com.taskora.home.ui.components.StatusPill
import com.taskora.home.ui.components.TaskoraScaffold
import com.taskora.home.ui.navigation.Routes
import com.taskora.home.ui.theme.GoodGreen
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.homeTypeLabel

@Composable
fun HomeProfilesScreen(vm: TaskoraViewModel, nav: NavHostController) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val activeId = vm.activeHome(data)?.id

    var editing by remember { mutableStateOf<HomeProfile?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<HomeProfile?>(null) }

    TaskoraScaffold(
        title = "Homes",
        onBack = { nav.popBackStack() },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(Routes.HOME_SETUP) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add home")
            }
        }
    ) { padding ->
        if (data.homes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No homes yet.", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tap + to create your first home.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@TaskoraScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(data.homes, key = { it.id }) { profile ->
                val roomCount = data.rooms.count { it.homeId == profile.id }
                val computed = vm.computedForHome(profile.id, data)
                val activeCount = computed.count { it.isActiveForStatus }
                val overdueCount = computed.count { it.task.enabled && it.status == TaskStatus.Overdue }
                val isActive = profile.id == activeId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            if (isActive) 2.dp else 1.dp,
                            if (isActive) GoodGreen else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { vm.setActiveHome(profile.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.name.ifBlank { "Home" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isActive) {
                                Spacer(Modifier.height(0.dp))
                                Text("  ")
                                StatusPill("Active", GoodGreen)
                            }
                        }
                        Text(
                            text = homeTypeLabel(profile.homeType),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$roomCount rooms · $activeCount active · $overdueCount overdue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { editing = profile; showEditor = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit home")
                    }
                    IconButton(onClick = { pendingDelete = profile }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete home")
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { nav.navigate(Routes.ROOM_MANAGEMENT) }) {
                    Text("Manage rooms for the active home")
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showEditor && editing != null) {
        HomeEditorDialog(
            initial = editing!!,
            onDismiss = { showEditor = false },
            onSave = { name, type, description ->
                vm.updateHome(editing!!.copy(name = name.trim(), homeType = type, description = description.trim()))
                showEditor = false
            }
        )
    }

    pendingDelete?.let { profile ->
        ConfirmDialog(
            title = "Delete this home?",
            text = "This will also remove its rooms, maintenance tasks, completion history, notes, and linked shopping items.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                vm.deleteHome(profile.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun HomeEditorDialog(
    initial: HomeProfile,
    onDismiss: () -> Unit,
    onSave: (String, HomeType, String) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var type by remember { mutableStateOf(initial.homeType) }
    var description by remember { mutableStateOf(initial.description) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit home") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showError = false },
                    label = { Text("Home name (required)") },
                    singleLine = true,
                    isError = showError && name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                DropdownField(
                    label = "Home type",
                    options = HomeType.entries,
                    selected = type,
                    optionLabel = { homeTypeLabel(it) },
                    onSelected = { type = it }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) { showError = true; return@TextButton }
                onSave(name, type, description)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
