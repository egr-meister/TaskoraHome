package com.taskora.home.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.taskora.home.data.Room
import com.taskora.home.ui.components.ConfirmDialog
import com.taskora.home.ui.components.EmptyState
import com.taskora.home.ui.components.TaskoraScaffold
import com.taskora.home.ui.navigation.Routes
import com.taskora.home.ui.theme.roomAccent
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.roomTypeLabel

@Composable
fun RoomManagementScreen(vm: TaskoraViewModel, nav: NavHostController) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val home = vm.activeHome(data)
    val rooms = if (home != null) vm.roomsForHome(home.id, data) else emptyList()
    var pendingDelete by remember { mutableStateOf<Room?>(null) }

    TaskoraScaffold(
        title = "Manage Rooms",
        onBack = { nav.popBackStack() },
        floatingActionButton = {
            if (home != null) {
                FloatingActionButton(onClick = { nav.navigate(Routes.addEditRoom()) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add room")
                }
            }
        }
    ) { padding ->
        if (home == null) {
            EmptyState(
                title = "No active home.",
                message = "Add a home to manage rooms.",
                modifier = Modifier.padding(padding)
            )
            return@TaskoraScaffold
        }
        if (rooms.isEmpty()) {
            EmptyState(
                title = "No rooms yet.",
                message = "Tap + to add your first room.",
                modifier = Modifier.padding(padding)
            )
            return@TaskoraScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(rooms, key = { _, r -> r.id }) { index, room ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(roomAccent(room.colorKey))
                    )
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = room.name.ifBlank { "Room" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = roomTypeLabel(room.roomType),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { vm.moveRoom(home.id, room.id, up = true) },
                        enabled = index > 0
                    ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up") }
                    IconButton(
                        onClick = { vm.moveRoom(home.id, room.id, up = false) },
                        enabled = index < rooms.lastIndex
                    ) { Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down") }
                    IconButton(onClick = { nav.navigate(Routes.addEditRoom(room.id)) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit room")
                    }
                    IconButton(onClick = { pendingDelete = room }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete room")
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    pendingDelete?.let { room ->
        ConfirmDialog(
            title = "Delete this room?",
            text = "This will also remove maintenance tasks and history assigned to this room.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                vm.deleteRoom(room.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}
