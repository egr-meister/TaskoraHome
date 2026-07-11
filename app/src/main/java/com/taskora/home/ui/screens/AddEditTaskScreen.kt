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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.taskora.home.data.IntervalUnit
import com.taskora.home.data.MaintenanceCategory
import com.taskora.home.data.MaintenanceScheduleType
import com.taskora.home.data.MaintenanceTask
import com.taskora.home.data.NextDateCalculationMode
import com.taskora.home.data.TaskPriority
import com.taskora.home.ui.components.DateField
import com.taskora.home.ui.components.DisclaimerBox
import com.taskora.home.ui.components.DropdownField
import com.taskora.home.ui.components.EmptyState
import com.taskora.home.ui.components.SectionLabel
import com.taskora.home.ui.components.TaskoraScaffold
import com.taskora.home.ui.components.defaultDateString
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.Disclaimers
import com.taskora.home.util.NoteLimits
import com.taskora.home.util.Validation
import com.taskora.home.util.calcModeLabel
import com.taskora.home.util.categoryLabel
import com.taskora.home.util.monthShort
import com.taskora.home.util.scheduleTypeLabel

@Composable
fun AddEditTaskScreen(
    vm: TaskoraViewModel,
    nav: NavHostController,
    taskId: String?,
    prefillRoomId: String?
) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val home = vm.activeHome(data)
    val existing = vm.taskById(taskId, data)

    if (home == null) {
        TaskoraScaffold(title = "Task", onBack = { nav.popBackStack() }) { p ->
            EmptyState(
                title = "No active home.",
                message = "Add a home before creating tasks.",
                modifier = Modifier.padding(p)
            )
        }
        return
    }
    if (taskId != null && existing == null) {
        TaskoraScaffold(title = "Task", onBack = { nav.popBackStack() }) { p ->
            EmptyState(
                title = "Deleted Maintenance Task",
                message = "This task is no longer available.",
                modifier = Modifier.padding(p)
            )
        }
        return
    }

    val rooms = vm.roomsForHome(home.id, data)

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var roomId by remember { mutableStateOf(existing?.roomId ?: prefillRoomId) }
    var category by remember { mutableStateOf(existing?.category ?: MaintenanceCategory.Other) }
    var priority by remember { mutableStateOf(existing?.priority ?: TaskPriority.Normal) }
    var scheduleType by remember {
        mutableStateOf(existing?.scheduleType ?: MaintenanceScheduleType.EveryNumberOfMonths)
    }
    var intervalText by remember { mutableStateOf(existing?.intervalValue?.toString() ?: "1") }
    var startDate by remember { mutableStateOf(existing?.startDate?.ifBlank { defaultDateString() } ?: defaultDateString()) }
    var specificDate by remember { mutableStateOf(existing?.specificDate ?: "") }
    var selectedMonths by remember { mutableStateOf(existing?.selectedMonths ?: emptyList()) }
    var yearlyMonth by remember { mutableStateOf(existing?.yearlyMonth ?: 1) }
    var yearlyDay by remember { mutableStateOf(existing?.yearlyDay ?: 1) }
    var calcMode by remember {
        mutableStateOf(existing?.calculationMode ?: data.settings.defaultCalculationMode)
    }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var shoppingLabel by remember { mutableStateOf(existing?.shoppingItemLabel ?: "") }
    var enabled by remember { mutableStateOf(existing?.enabled ?: true) }
    var showErrors by remember { mutableStateOf(false) }

    fun validation() = Validation.validateTask(
        title = title,
        scheduleType = scheduleType,
        intervalText = intervalText,
        intervalUnit = intervalUnitFor(scheduleType),
        startDate = startDate,
        specificDate = specificDate,
        selectedMonths = selectedMonths,
        yearlyMonth = yearlyMonth,
        yearlyDay = yearlyDay
    )

    TaskoraScaffold(
        title = if (existing == null) "Add Task" else "Edit Task",
        onBack = { nav.popBackStack() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DisclaimerBox(text = Disclaimers.TASK_CREATE_NOTE)

            SectionLabel("Task")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; showErrors = false },
                label = { Text("Title (required)") },
                singleLine = true,
                isError = showErrors && validation().error(com.taskora.home.util.ValidationResult.FIELD_TITLE) != null,
                supportingText = {
                    val e = if (showErrors) validation().error(com.taskora.home.util.ValidationResult.FIELD_TITLE) else null
                    if (e != null) Text(e)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Home: ${home.name.ifBlank { "Home" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val roomOptions = listOf<String?>(null) + rooms.map { it.id }
            DropdownField(
                label = "Room",
                options = roomOptions,
                selected = if (roomId != null && rooms.any { it.id == roomId }) roomId else null,
                optionLabel = {
                    if (it == null) "Whole Home"
                    else rooms.firstOrNull { r -> r.id == it }?.name?.ifBlank { "Room" } ?: "Room"
                },
                onSelected = { roomId = it }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = "Category",
                    options = MaintenanceCategory.entries,
                    selected = category,
                    optionLabel = { categoryLabel(it) },
                    onSelected = { category = it },
                    modifier = Modifier.weight(1f)
                )
                DropdownField(
                    label = "Priority",
                    options = TaskPriority.entries,
                    selected = priority,
                    optionLabel = { if (it == TaskPriority.High) "High" else "Normal" },
                    onSelected = { priority = it },
                    modifier = Modifier.weight(1f)
                )
            }

            SectionLabel("Schedule")
            DropdownField(
                label = "Schedule type",
                options = MaintenanceScheduleType.entries,
                selected = scheduleType,
                optionLabel = { scheduleTypeLabel(it) },
                onSelected = { scheduleType = it; showErrors = false }
            )

            when (scheduleType) {
                MaintenanceScheduleType.EveryNumberOfDays,
                MaintenanceScheduleType.EveryNumberOfWeeks,
                MaintenanceScheduleType.EveryNumberOfMonths -> {
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { input ->
                            // Allow empty while typing; otherwise digits only.
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                intervalText = input.take(5)
                                showErrors = false
                            }
                        },
                        label = { Text("Every N ${unitWord(scheduleType)}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = showErrors && validation().error(com.taskora.home.util.ValidationResult.FIELD_INTERVAL) != null,
                        supportingText = {
                            val e = if (showErrors) validation().error(com.taskora.home.util.ValidationResult.FIELD_INTERVAL) else null
                            if (e != null) Text(e)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DateField(
                        label = "Start date",
                        value = startDate,
                        onValueChange = { startDate = it; showErrors = false },
                        isError = showErrors && validation().error(com.taskora.home.util.ValidationResult.FIELD_START) != null,
                        supportingText = if (showErrors) validation().error(com.taskora.home.util.ValidationResult.FIELD_START) else null
                    )
                }

                MaintenanceScheduleType.OneTime -> {
                    DateField(
                        label = "Due date",
                        value = specificDate,
                        onValueChange = { specificDate = it; showErrors = false },
                        isError = showErrors && validation().error(com.taskora.home.util.ValidationResult.FIELD_DUE) != null,
                        supportingText = if (showErrors) validation().error(com.taskora.home.util.ValidationResult.FIELD_DUE) else null
                    )
                }

                MaintenanceScheduleType.SelectedMonths -> {
                    Text("Select months", style = MaterialTheme.typography.bodyMedium)
                    MonthPicker(
                        selected = selectedMonths,
                        onToggle = { m ->
                            selectedMonths = if (selectedMonths.contains(m)) selectedMonths - m
                            else selectedMonths + m
                            showErrors = false
                        }
                    )
                    if (showErrors) validation().error(com.taskora.home.util.ValidationResult.FIELD_MONTHS)?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    DateField(
                        label = "Start date",
                        value = startDate,
                        onValueChange = { startDate = it; showErrors = false }
                    )
                }

                MaintenanceScheduleType.Yearly -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DropdownField(
                            label = "Month",
                            options = (1..12).toList(),
                            selected = yearlyMonth,
                            optionLabel = { monthShort(it) },
                            onSelected = { yearlyMonth = it; showErrors = false },
                            modifier = Modifier.weight(1f)
                        )
                        DropdownField(
                            label = "Day",
                            options = (1..31).toList(),
                            selected = yearlyDay,
                            optionLabel = { it.toString() },
                            onSelected = { yearlyDay = it; showErrors = false },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (showErrors) validation().error(com.taskora.home.util.ValidationResult.FIELD_YEARLY)?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    DateField(
                        label = "Start date",
                        value = startDate,
                        onValueChange = { startDate = it; showErrors = false }
                    )
                }

                MaintenanceScheduleType.ManualOnly -> {
                    Text(
                        "Manual-only tasks have no calculated due date. Mark them complete whenever you do them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (scheduleType != MaintenanceScheduleType.OneTime &&
                scheduleType != MaintenanceScheduleType.ManualOnly
            ) {
                DropdownField(
                    label = "Next-date calculation",
                    options = NextDateCalculationMode.entries,
                    selected = calcMode,
                    optionLabel = { calcModeLabel(it) },
                    onSelected = { calcMode = it }
                )
            }

            SectionLabel("Extras")
            OutlinedTextField(
                value = shoppingLabel,
                onValueChange = { shoppingLabel = it },
                label = { Text("Shopping label (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= NoteLimits.DETAILED) notes = it },
                label = { Text("Notes (optional)") },
                supportingText = { Text("${notes.length}/${NoteLimits.DETAILED}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enabled", modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            Button(
                onClick = {
                    val result = validation()
                    if (!result.isValid) {
                        showErrors = true
                        return@Button
                    }
                    val interval = Validation.parseIntervalInput(intervalText)
                    val base = existing ?: MaintenanceTask(homeId = home.id)
                    val toSave = base.copy(
                        homeId = home.id,
                        roomId = if (roomId != null && rooms.any { it.id == roomId }) roomId else null,
                        title = title.trim(),
                        category = category,
                        scheduleType = scheduleType,
                        intervalValue = interval,
                        intervalUnit = intervalUnitFor(scheduleType),
                        selectedMonths = selectedMonths.filter { it in 1..12 }.sorted(),
                        specificDate = specificDate,
                        startDate = startDate,
                        yearlyMonth = if (scheduleType == MaintenanceScheduleType.Yearly) yearlyMonth else existing?.yearlyMonth,
                        yearlyDay = if (scheduleType == MaintenanceScheduleType.Yearly) yearlyDay else existing?.yearlyDay,
                        enabled = enabled,
                        priority = priority,
                        calculationMode = calcMode,
                        notes = Validation.trimNote(notes, NoteLimits.DETAILED),
                        shoppingItemLabel = shoppingLabel.trim()
                    )
                    if (existing == null) {
                        vm.addTask(toSave) { nav.popBackStack() }
                    } else {
                        vm.updateTask(toSave)
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Task") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun intervalUnitFor(type: MaintenanceScheduleType): IntervalUnit = when (type) {
    MaintenanceScheduleType.EveryNumberOfDays -> IntervalUnit.Days
    MaintenanceScheduleType.EveryNumberOfWeeks -> IntervalUnit.Weeks
    MaintenanceScheduleType.EveryNumberOfMonths -> IntervalUnit.Months
    MaintenanceScheduleType.Yearly -> IntervalUnit.Years
    else -> IntervalUnit.Days
}

private fun unitWord(type: MaintenanceScheduleType): String = when (type) {
    MaintenanceScheduleType.EveryNumberOfDays -> "days"
    MaintenanceScheduleType.EveryNumberOfWeeks -> "weeks"
    MaintenanceScheduleType.EveryNumberOfMonths -> "months"
    else -> "units"
}

@Composable
private fun MonthPicker(selected: List<Int>, onToggle: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        (0 until 3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                (1..4).forEach { col ->
                    val month = row * 4 + col
                    val isSel = selected.contains(month)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.dp,
                                if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onToggle(month) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = monthShort(month),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
