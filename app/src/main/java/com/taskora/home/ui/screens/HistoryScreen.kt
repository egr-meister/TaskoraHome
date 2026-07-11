package com.taskora.home.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.taskora.home.data.MaintenanceCategory
import com.taskora.home.ui.components.DropdownField
import com.taskora.home.ui.components.EmptyState
import com.taskora.home.ui.components.SectionLabel
import com.taskora.home.ui.components.TaskoraScaffold
import com.taskora.home.ui.navigation.Routes
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.HistoryGrouping
import com.taskora.home.util.HistoryUtils
import com.taskora.home.util.categoryLabel
import com.taskora.home.util.displayDate

@Composable
fun HistoryScreen(vm: TaskoraViewModel, nav: NavHostController) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val home = vm.activeHome(data)

    var grouping by remember { mutableStateOf(HistoryGrouping.Date) }
    var roomFilter by remember { mutableStateOf<String?>(null) }
    var categoryFilter by remember { mutableStateOf<MaintenanceCategory?>(null) }

    TaskoraScaffold(title = "History", onBack = { nav.popBackStack() }) { padding ->
        if (home == null) {
            EmptyState(
                title = "No maintenance history yet.",
                message = "Add a home to begin.",
                modifier = Modifier.padding(padding)
            )
            return@TaskoraScaffold
        }

        val rooms = vm.roomsForHome(home.id, data)
        val filtered = HistoryUtils.filter(
            completions = data.completions,
            homeId = home.id,
            roomId = roomFilter,
            category = categoryFilter,
            tasks = data.maintenanceTasks
        )
        val sections = HistoryUtils.group(filtered, grouping, data.maintenanceTasks, rooms)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                DropdownField(
                    label = "Group by",
                    options = HistoryGrouping.entries,
                    selected = grouping,
                    optionLabel = { it.name },
                    onSelected = { grouping = it }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        onSelected = { roomFilter = it },
                        modifier = Modifier.weight(1f)
                    )
                    val catOptions = listOf<MaintenanceCategory?>(null) + MaintenanceCategory.entries
                    DropdownField(
                        label = "Category",
                        options = catOptions,
                        selected = categoryFilter,
                        optionLabel = { if (it == null) "All" else categoryLabel(it) },
                        onSelected = { categoryFilter = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (sections.isEmpty()) {
                EmptyState(title = "No maintenance history yet.")
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sections.forEachIndexed { sIndex, section ->
                        item(key = "h_$sIndex") {
                            SectionLabel(section.title)
                        }
                        items(
                            count = section.items.size,
                            key = { idx -> section.items[idx].id }
                        ) { idx ->
                            val c = section.items[idx]
                            val task = vm.taskById(c.taskId, data)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { nav.navigate(Routes.completionDetail(c.id)) }
                                    .padding(10.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = task?.title?.ifBlank { "Task" } ?: "Deleted Maintenance Task",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${displayDate(c.completedDate)} · ${roomName(vm, data, c.roomId)}" +
                                            (task?.let { " · ${categoryLabel(it.category)}" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (c.note.isNotBlank()) {
                                        Text(
                                            text = c.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}
