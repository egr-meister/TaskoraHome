package com.taskora.home.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.taskora.home.data.MapPosition
import com.taskora.home.data.Room
import com.taskora.home.data.RoomType
import com.taskora.home.ui.components.DropdownField
import com.taskora.home.ui.components.EmptyState
import com.taskora.home.ui.components.SectionLabel
import com.taskora.home.ui.components.TaskoraScaffold
import com.taskora.home.ui.theme.roomColorChoices
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.NoteLimits
import com.taskora.home.util.Validation
import com.taskora.home.util.roomTypeLabel

@Composable
fun AddEditRoomScreen(vm: TaskoraViewModel, nav: NavHostController, roomId: String?) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val home = vm.activeHome(data)
    val existing = vm.roomById(roomId, data)

    if (home == null) {
        TaskoraScaffold(title = "Room", onBack = { nav.popBackStack() }) { p ->
            EmptyState(
                title = "No active home.",
                message = "Add a home before creating rooms.",
                modifier = Modifier.padding(p)
            )
        }
        return
    }
    if (roomId != null && existing == null) {
        TaskoraScaffold(title = "Room", onBack = { nav.popBackStack() }) { p ->
            EmptyState(
                title = "Deleted Room",
                message = "This room is no longer available.",
                modifier = Modifier.padding(p)
            )
        }
        return
    }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var roomType by remember { mutableStateOf(existing?.roomType ?: RoomType.Custom) }
    var colorKey by remember { mutableStateOf(existing?.colorKey ?: "utility") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var showError by remember { mutableStateOf(false) }

    TaskoraScaffold(
        title = if (existing == null) "Add Room" else "Edit Room",
        onBack = { nav.popBackStack() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp)
        ) {
            SectionLabel("Room")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; showError = false },
                label = { Text("Room name (required)") },
                singleLine = true,
                isError = showError && Validation.validateRoom(name).error("name") != null,
                supportingText = {
                    val err = if (showError) Validation.validateRoom(name).error("name") else null
                    if (err != null) Text(err)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            DropdownField(
                label = "Room type",
                options = RoomType.entries,
                selected = roomType,
                optionLabel = { roomTypeLabel(it) },
                onSelected = { roomType = it }
            )

            Spacer(Modifier.height(16.dp))
            SectionLabel("Zone color")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                roomColorChoices.forEach { (key, color) ->
                    ColorSwatch(
                        color = color,
                        selected = colorKey == key,
                        onClick = { colorKey = key }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Notes")
            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= NoteLimits.DETAILED) notes = it },
                label = { Text("Room notes (optional)") },
                supportingText = { Text("${notes.length}/${NoteLimits.DETAILED}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (!Validation.validateRoom(name).isValid) {
                        showError = true
                        return@Button
                    }
                    if (existing == null) {
                        vm.addRoom(
                            Room(
                                homeId = home.id,
                                name = name.trim(),
                                roomType = roomType,
                                colorKey = colorKey,
                                mapPosition = MapPosition(),
                                notes = Validation.trimNote(notes, NoteLimits.DETAILED)
                            )
                        ) { nav.popBackStack() }
                    } else {
                        vm.updateRoom(
                            existing.copy(
                                name = name.trim(),
                                roomType = roomType,
                                colorKey = colorKey,
                                notes = Validation.trimNote(notes, NoteLimits.DETAILED)
                            )
                        )
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Room") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White)
        }
    }
}
