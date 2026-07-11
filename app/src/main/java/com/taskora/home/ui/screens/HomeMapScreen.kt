package com.taskora.home.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.taskora.home.data.TaskStatus
import com.taskora.home.ui.components.EmptyState
import com.taskora.home.ui.components.HomeMap
import com.taskora.home.ui.components.MaintenanceTaskRow
import com.taskora.home.ui.components.ReminderBanner
import com.taskora.home.ui.components.RoomZoneData
import com.taskora.home.ui.components.SectionLabel
import com.taskora.home.ui.components.StatusLegend
import com.taskora.home.ui.components.TaskoraScaffold
import com.taskora.home.ui.navigation.Routes
import com.taskora.home.ui.navigation.TaskFilterKeys
import com.taskora.home.ui.theme.roomAccent
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.ReminderEvaluator
import com.taskora.home.util.StatusCalculator
import com.taskora.home.util.categoryLabel
import com.taskora.home.util.displayDate
import com.taskora.home.util.displayDateShort
import com.taskora.home.util.homeTypeLabel
import com.taskora.home.util.today

@Composable
fun HomeMapScreen(vm: TaskoraViewModel, nav: NavHostController) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val dismissedKey by vm.dismissedReminderKey.collectAsStateWithLifecycle()
    val home = vm.activeHome(data)

    TaskoraScaffold(
        title = "Taskora Home",
        floatingActionButton = {
            if (home != null) {
                ExtendedFloatingActionButton(
                    onClick = { nav.navigate(Routes.addEditTask()) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add Task") }
                )
            }
        }
    ) { padding ->
        if (home == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmptyState(
                    title = "No home has been added.",
                    message = "Create a home profile to start organizing maintenance."
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Button(onClick = { nav.navigate(Routes.HOME_SETUP) }) {
                    Text("Add Your Home")
                }
            }
            return@TaskoraScaffold
        }

        val todayDate = today()
        val computed = vm.computedForHome(home.id, data, todayDate)
        val rooms = vm.roomsForHome(home.id, data)
        val shoppingForHome = data.shoppingItems.filter { it.homeId == home.id }

        val reminder = ReminderEvaluator.evaluate(computed, shoppingForHome, data.settings)
        val reminderKey = reminder?.let { "${it.title}|${it.detail}" }

        val roomZones = rooms.map { room ->
            val roomComputed = computed.filter { it.task.roomId == room.id }
            val counts = StatusCalculator.counts(roomComputed)
            RoomZoneData(
                roomId = room.id,
                name = room.name.ifBlank { "Room" },
                accent = roomAccent(room.colorKey),
                status = StatusCalculator.roomStatus(room.id, computed),
                activeCount = counts.active,
                overdueCount = counts.overdue,
                soonCount = counts.soon,
                nearestDue = roomComputed.mapNotNull { it.nextDue }.minOrNull()?.let { displayDateShort(it) }
            )
        }

        val wholeHomeComputed = computed.filter { it.task.roomId == null }
        val wholeCounts = StatusCalculator.counts(wholeHomeComputed)
        val wholeNext = wholeHomeComputed
            .filter { it.isActiveForStatus }
            .minByOrNull { it.nextDue ?: java.time.LocalDate.MAX }

        val mostUrgentRoomId = roomZones
            .filter { it.overdueCount > 0 }
            .maxByOrNull { it.overdueCount }?.roomId
            ?: roomZones.firstOrNull { it.soonCount > 0 }?.roomId

        val nextMaintenance = computed
            .filter { it.isActiveForStatus && it.nextDue != null }
            .minByOrNull { it.nextDue!! }

        val upcoming = computed
            .filter { it.task.enabled && (it.status == TaskStatus.Overdue || it.status == TaskStatus.Soon) }
            .sortedBy { it.nextDue ?: java.time.LocalDate.MAX }
            .take(10)

        val recentCompletions = data.completions
            .filter { it.homeId == home.id }
            .sortedByDescending { it.completedDate }
            .take(8)

        val shoppingPreview = shoppingForHome.filter { !it.checked }.take(4)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // 1. Home selector + date strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { nav.navigate(Routes.HOME_PROFILES) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = home.name.ifBlank { "Home" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${homeTypeLabel(home.homeType)}  ·  ${displayDate(todayDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Filled.SwapHoriz, contentDescription = "Switch home")
            }

            // Reminder banner
            if (reminder != null && reminderKey != dismissedKey) {
                ReminderBanner(
                    reminder = reminder,
                    onViewTasks = { nav.navigate(Routes.allTasks(TaskFilterKeys.OVERDUE)) },
                    onNotNow = { vm.dismissReminder(reminderKey!!) }
                )
            }

            // 2. House map
            HomeMap(
                wholeHomeStatus = StatusCalculator.wholeHomeStatus(computed),
                wholeHomeActive = wholeCounts.active,
                wholeHomeOverdue = wholeCounts.overdue,
                wholeHomeNextTitle = wholeNext?.task?.title,
                rooms = roomZones,
                highlightRoomId = mostUrgentRoomId,
                onWholeHomeClick = { nav.navigate(Routes.allTasks(TaskFilterKeys.ALL)) },
                onRoomClick = { roomId -> nav.navigate(Routes.roomDetail(roomId)) }
            )

            // 3. Next maintenance shelf
            SectionLabel("Next maintenance")
            if (nextMaintenance != null) {
                MaintenanceTaskRow(
                    computed = nextMaintenance,
                    roomName = roomName(vm, data, nextMaintenance.task.roomId),
                    onClick = { nav.navigate(Routes.taskDetail(nextMaintenance.task.id)) }
                )
            } else {
                Text(
                    text = if (computed.isEmpty()) "No maintenance tasks yet. Add a task to a room or the whole home."
                    else "Nothing scheduled soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 4. Legend
            SectionLabel("Status legend")
            StatusLegend()

            // 5. Upcoming rail
            if (upcoming.isNotEmpty()) {
                SectionLabel("Upcoming & overdue")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(upcoming, key = { it.task.id }) { tc ->
                        Box(Modifier.width(260.dp)) {
                            MaintenanceTaskRow(
                                computed = tc,
                                roomName = roomName(vm, data, tc.task.roomId),
                                onClick = { nav.navigate(Routes.taskDetail(tc.task.id)) }
                            )
                        }
                    }
                }
            }

            // 6. Recent completions strip
            if (recentCompletions.isNotEmpty()) {
                SectionLabel("Recently completed")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recentCompletions, key = { it.id }) { c ->
                        val task = vm.taskById(c.taskId, data)
                        Column(
                            modifier = Modifier
                                .width(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { nav.navigate(Routes.completionDetail(c.id)) }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = task?.title?.ifBlank { "Task" } ?: "Deleted Maintenance Task",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = displayDate(c.completedDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            task?.let {
                                Text(
                                    text = categoryLabel(it.category),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 7. Shopping preview
            SectionLabel("Shopping list")
            if (shoppingPreview.isEmpty()) {
                Text(
                    text = "No unchecked shopping items.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                shoppingPreview.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { nav.navigate(Routes.SHOPPING) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("•  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = item.title.ifBlank { "Item" },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.quantityLabel.isNotBlank()) {
                            Text(
                                text = item.quantityLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(90.dp)) // space for FAB
        }
    }
}
