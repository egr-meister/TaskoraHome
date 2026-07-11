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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.taskora.home.data.ShoppingCategory
import com.taskora.home.data.ShoppingItem
import com.taskora.home.data.ShoppingPriority
import com.taskora.home.ui.components.ConfirmDialog
import com.taskora.home.ui.components.DropdownField
import com.taskora.home.ui.components.EmptyState
import com.taskora.home.ui.components.SectionLabel
import com.taskora.home.ui.components.StatusPill
import com.taskora.home.ui.components.TaskoraScaffold
import com.taskora.home.ui.theme.OverdueRed
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.NoteLimits
import com.taskora.home.util.shoppingCategoryLabel

@Composable
fun ShoppingScreen(vm: TaskoraViewModel, nav: NavHostController) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val home = vm.activeHome(data)

    var roomFilter by remember { mutableStateOf<String?>(null) }
    var categoryFilter by remember { mutableStateOf<ShoppingCategory?>(null) }
    var priorityFilter by remember { mutableStateOf<ShoppingPriority?>(null) }
    var editing by remember { mutableStateOf<ShoppingItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }

    TaskoraScaffold(
        title = "Shopping",
        actions = {
            if (home != null) {
                IconButton(onClick = { showClear = true }) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear checked items")
                }
            }
        },
        floatingActionButton = {
            if (home != null) {
                FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add item")
                }
            }
        }
    ) { padding ->
        if (home == null) {
            EmptyState(
                title = "No home yet.",
                message = "Add a home to keep a shopping list.",
                modifier = Modifier.padding(padding)
            )
            return@TaskoraScaffold
        }

        val rooms = vm.roomsForHome(home.id, data)
        val all = data.shoppingItems.filter { it.homeId == home.id }.filter { item ->
            val roomOk = when (roomFilter) {
                null -> true
                "" -> item.roomId == null
                else -> item.roomId == roomFilter
            }
            val catOk = categoryFilter == null || item.category == categoryFilter
            val prioOk = priorityFilter == null || item.priority == priorityFilter
            roomOk && catOk && prioOk
        }
        // High priority first, then title.
        val unchecked = all.filter { !it.checked }
            .sortedWith(compareByDescending<ShoppingItem> { it.priority == ShoppingPriority.High }
                .thenBy { it.title.lowercase() })
        val checked = all.filter { it.checked }.sortedBy { it.title.lowercase() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val roomOptions = listOf<String?>(null, "") + rooms.map { it.id }
                    DropdownField(
                        label = "Room",
                        options = roomOptions,
                        selected = roomFilter,
                        optionLabel = {
                            when (it) {
                                null -> "All"
                                "" -> "Whole Home"
                                else -> rooms.firstOrNull { r -> r.id == it }?.name ?: "Room"
                            }
                        },
                        onSelected = { roomFilter = it },
                        modifier = Modifier.weight(1f)
                    )
                    val catOptions = listOf<ShoppingCategory?>(null) + ShoppingCategory.entries
                    DropdownField(
                        label = "Category",
                        options = catOptions,
                        selected = categoryFilter,
                        optionLabel = { if (it == null) "All" else shoppingCategoryLabel(it) },
                        onSelected = { categoryFilter = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                val prioOptions = listOf<ShoppingPriority?>(null, ShoppingPriority.High, ShoppingPriority.Normal)
                DropdownField(
                    label = "Priority",
                    options = prioOptions,
                    selected = priorityFilter,
                    optionLabel = {
                        when (it) {
                            null -> "All"
                            ShoppingPriority.High -> "High"
                            else -> "Normal"
                        }
                    },
                    onSelected = { priorityFilter = it }
                )
            }

            if (all.isEmpty()) {
                EmptyState(
                    title = "Your shopping list is empty.",
                    message = "Tap + to add supplies you need."
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (unchecked.isNotEmpty()) {
                        item(key = "unchecked_header") { SectionLabel("To buy") }
                        items(count = unchecked.size, key = { unchecked[it].id }) { idx ->
                            ShoppingRow(
                                item = unchecked[idx],
                                roomLabel = roomName(vm, data, unchecked[idx].roomId),
                                onToggle = { vm.setShoppingChecked(unchecked[idx].id, true) },
                                onEdit = { editing = unchecked[idx]; showEditor = true },
                                onDelete = { vm.deleteShoppingItem(unchecked[idx].id) }
                            )
                        }
                    }
                    if (checked.isNotEmpty()) {
                        item(key = "checked_header") { SectionLabel("Purchased") }
                        items(count = checked.size, key = { checked[it].id }) { idx ->
                            ShoppingRow(
                                item = checked[idx],
                                roomLabel = roomName(vm, data, checked[idx].roomId),
                                onToggle = { vm.setShoppingChecked(checked[idx].id, false) },
                                onEdit = { editing = checked[idx]; showEditor = true },
                                onDelete = { vm.deleteShoppingItem(checked[idx].id) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showEditor) {
        ShoppingEditorDialog(
            initial = editing,
            rooms = vm.roomsForHome(home?.id ?: "", data),
            onDismiss = { showEditor = false },
            onSave = { title, category, quantity, priority, roomId, note ->
                if (home != null) {
                    val base = editing
                    if (base == null) {
                        vm.addShoppingItem(
                            ShoppingItem(
                                homeId = home.id,
                                roomId = roomId,
                                title = title.trim(),
                                category = category,
                                quantityLabel = quantity.trim(),
                                priority = priority,
                                note = note.trim()
                            )
                        )
                    } else {
                        vm.updateShoppingItem(
                            base.copy(
                                roomId = roomId,
                                title = title.trim(),
                                category = category,
                                quantityLabel = quantity.trim(),
                                priority = priority,
                                note = note.trim()
                            )
                        )
                    }
                }
                showEditor = false
            }
        )
    }

    if (showClear) {
        ConfirmDialog(
            title = "Clear checked items?",
            text = "This removes all purchased items from this home's shopping list.",
            confirmLabel = "Clear",
            destructive = true,
            onConfirm = {
                home?.let { vm.clearCheckedShopping(it.id) }
                showClear = false
            },
            onDismiss = { showClear = false }
        )
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    roomLabel: String,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.checked, onCheckedChange = { onToggle() })
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title.ifBlank { "Item" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null
                )
                if (item.priority == ShoppingPriority.High && !item.checked) {
                    Spacer(Modifier.height(0.dp))
                    Text("  ")
                    StatusPill("High", OverdueRed)
                }
            }
            val meta = buildString {
                append(shoppingCategoryLabel(item.category))
                if (item.quantityLabel.isNotBlank()) append(" · ${item.quantityLabel}")
                append(" · $roomLabel")
            }
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.note.isNotBlank()) {
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(onClick = onEdit) { Text("Edit") }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete item")
        }
    }
}

@Composable
private fun ShoppingEditorDialog(
    initial: ShoppingItem?,
    rooms: List<com.taskora.home.data.Room>,
    onDismiss: () -> Unit,
    onSave: (String, ShoppingCategory, String, ShoppingPriority, String?, String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: ShoppingCategory.General) }
    var quantity by remember { mutableStateOf(initial?.quantityLabel ?: "") }
    var priority by remember { mutableStateOf(initial?.priority ?: ShoppingPriority.Normal) }
    var roomId by remember { mutableStateOf(initial?.roomId) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add item" else "Edit item") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; showError = false },
                    label = { Text("Title (required)") },
                    singleLine = true,
                    isError = showError && title.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                DropdownField(
                    label = "Category",
                    options = ShoppingCategory.entries,
                    selected = category,
                    optionLabel = { shoppingCategoryLabel(it) },
                    onSelected = { category = it }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                DropdownField(
                    label = "Priority",
                    options = ShoppingPriority.entries,
                    selected = priority,
                    optionLabel = { if (it == ShoppingPriority.High) "High" else "Normal" },
                    onSelected = { priority = it }
                )
                Spacer(Modifier.height(8.dp))
                val roomOptions = listOf<String?>(null) + rooms.map { it.id }
                DropdownField(
                    label = "Room (optional)",
                    options = roomOptions,
                    selected = if (roomId != null && rooms.any { it.id == roomId }) roomId else null,
                    optionLabel = {
                        if (it == null) "None" else rooms.firstOrNull { r -> r.id == it }?.name ?: "Room"
                    },
                    onSelected = { roomId = it }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= NoteLimits.SHORT) note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) { showError = true; return@TextButton }
                onSave(title, category, quantity, priority, roomId, note)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
