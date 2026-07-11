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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.taskora.home.ui.components.CalendarView
import com.taskora.home.ui.components.EmptyState
import com.taskora.home.ui.components.SectionLabel
import com.taskora.home.ui.components.StatusLegend
import com.taskora.home.ui.components.TaskoraScaffold
import com.taskora.home.ui.navigation.Routes
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.CalendarUtils
import com.taskora.home.util.displayDate
import com.taskora.home.util.today
import java.time.YearMonth

@Composable
fun CalendarScreen(vm: TaskoraViewModel, nav: NavHostController) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val home = vm.activeHome(data)

    val todayDate = today()
    var visibleMonth by remember { mutableStateOf(YearMonth.from(todayDate)) }
    var selectedDate by remember { mutableStateOf(todayDate) }

    TaskoraScaffold(title = "Calendar") { padding ->
        if (home == null) {
            EmptyState(
                title = "No home yet.",
                message = "Add a home to see its maintenance calendar.",
                modifier = Modifier.padding(padding)
            )
            return@TaskoraScaffold
        }

        val computed = vm.computedForHome(home.id, data, todayDate)
        val completions = data.completions.filter { it.homeId == home.id }
        val markers = CalendarUtils.markersForMonth(visibleMonth, computed, completions, todayDate)

        val dueOnSelected = CalendarUtils.tasksDueOn(selectedDate, computed)
        val completedOnSelected = CalendarUtils.completionsOn(selectedDate, completions)
        val roomsInvolved = (dueOnSelected.map { it.task.roomId } +
            completedOnSelected.map { it.roomId }).distinct()
        val notesCount = completedOnSelected.count { it.note.isNotBlank() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalendarView(
                yearMonth = visibleMonth,
                firstDayOfWeek = data.settings.firstDayOfWeek,
                markers = markers,
                selectedDate = selectedDate,
                todayDate = todayDate,
                onPrevMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                onSelectDate = { selectedDate = it }
            )

            StatusLegend()

            SectionLabel(displayDate(selectedDate))
            Text(
                text = "${dueOnSelected.size} due · ${completedOnSelected.size} completed · " +
                    "${roomsInvolved.size} room(s) · $notesCount note(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (dueOnSelected.isNotEmpty()) {
                SectionLabel("Tasks due")
                dueOnSelected.forEach { tc ->
                    DayRow(
                        title = tc.task.title.ifBlank { "Task" },
                        subtitle = roomName(vm, data, tc.task.roomId),
                        onClick = { nav.navigate(Routes.taskDetail(tc.task.id)) }
                    )
                }
            }
            if (completedOnSelected.isNotEmpty()) {
                SectionLabel("Completed")
                completedOnSelected.forEach { c ->
                    val task = vm.taskById(c.taskId, data)
                    DayRow(
                        title = task?.title?.ifBlank { "Task" } ?: "Deleted Maintenance Task",
                        subtitle = roomName(vm, data, c.roomId),
                        onClick = { nav.navigate(Routes.completionDetail(c.id)) }
                    )
                }
            }
            if (dueOnSelected.isEmpty() && completedOnSelected.isEmpty()) {
                Text(
                    "Nothing on this date.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DayRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
