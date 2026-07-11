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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.taskora.home.data.NextDateCalculationMode
import com.taskora.home.data.WeekDay
import com.taskora.home.ui.components.ConfirmDialog
import com.taskora.home.ui.components.DisclaimerBox
import com.taskora.home.ui.components.DropdownField
import com.taskora.home.ui.components.SectionLabel
import com.taskora.home.ui.components.TaskoraScaffold
import com.taskora.home.ui.navigation.Routes
import com.taskora.home.ui.theme.OverdueRed
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.Disclaimers
import com.taskora.home.util.calcModeLabel

@Composable
fun SettingsScreen(vm: TaskoraViewModel, nav: NavHostController) {
    val data by vm.appData.collectAsStateWithLifecycle()
    val settings = data.settings
    val activeHome = vm.activeHome(data)

    var confirmClearShopping by remember { mutableStateOf(false) }
    var confirmDeleteHistory by remember { mutableStateOf(false) }
    var confirmDeleteHome by remember { mutableStateOf(false) }
    var confirmResetAll by remember { mutableStateOf(false) }

    TaskoraScaffold(title = "Settings") { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Active home
            SectionLabel("Active home")
            if (data.homes.isEmpty()) {
                Text(
                    "No homes yet. Add one from the Home tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                DropdownField(
                    label = "Active home",
                    options = data.homes.map { it.id },
                    selected = activeHome?.id ?: data.homes.first().id,
                    optionLabel = { id -> data.homes.firstOrNull { it.id == id }?.name?.ifBlank { "Home" } ?: "Home" },
                    onSelected = { vm.setActiveHome(it) }
                )
            }

            // Scheduling
            SectionLabel("Scheduling")
            DropdownField(
                label = "Soon threshold (days)",
                options = listOf(3, 7, 14, 30),
                selected = settings.soonThresholdDays.takeIf { it in listOf(3, 7, 14, 30) } ?: 7,
                optionLabel = { "$it days" },
                onSelected = { vm.updateSettings(settings.copy(soonThresholdDays = it)) }
            )
            Spacer(Modifier.height(4.dp))
            DropdownField(
                label = "First day of week",
                options = WeekDay.entries,
                selected = settings.firstDayOfWeek,
                optionLabel = { if (it == WeekDay.Monday) "Monday" else "Sunday" },
                onSelected = { vm.updateSettings(settings.copy(firstDayOfWeek = it)) }
            )
            Spacer(Modifier.height(4.dp))
            DropdownField(
                label = "Default next-date calculation",
                options = NextDateCalculationMode.entries,
                selected = settings.defaultCalculationMode,
                optionLabel = { calcModeLabel(it) },
                onSelected = { vm.updateSettings(settings.copy(defaultCalculationMode = it)) }
            )

            // Reminders
            SectionLabel("In-app reminders")
            SwitchRow("Enable in-app reminders", settings.reminderSettings.enabled) {
                vm.updateSettings(settings.copy(reminderSettings = settings.reminderSettings.copy(enabled = it)))
            }
            SwitchRow("Overdue reminders", settings.reminderSettings.showOverdue) {
                vm.updateSettings(settings.copy(reminderSettings = settings.reminderSettings.copy(showOverdue = it)))
            }
            SwitchRow("Due-today reminders", settings.reminderSettings.showDueToday) {
                vm.updateSettings(settings.copy(reminderSettings = settings.reminderSettings.copy(showDueToday = it)))
            }
            SwitchRow("Shopping reminders", settings.reminderSettings.showShoppingReminder) {
                vm.updateSettings(settings.copy(reminderSettings = settings.reminderSettings.copy(showShoppingReminder = it)))
            }
            DisclaimerBox(text = Disclaimers.REMINDERS_NOTE)

            // General
            SectionLabel("General")
            OutlinedButton(
                onClick = {
                    vm.showOnboardingAgain()
                    nav.navigate(Routes.ONBOARDING)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Show onboarding again") }
            OutlinedButton(
                onClick = { confirmClearShopping = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Clear checked shopping items") }

            // Destructive
            SectionLabel("Data")
            DestructiveButton("Delete history for active home") { confirmDeleteHistory = true }
            DestructiveButton("Delete active home") { confirmDeleteHome = true }
            DestructiveButton("Reset all local data") { confirmResetAll = true }

            // Info
            SectionLabel("About")
            InfoBlock("Taskora Home", "Version 1.0.0 — a manual, offline home maintenance organizer.")
            DisclaimerBox(text = Disclaimers.SAFETY)
            DisclaimerBox(text = Disclaimers.PROFESSIONAL_WORK)
            DisclaimerBox(text = Disclaimers.PRIVACY)

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmClearShopping) {
        ConfirmDialog(
            title = "Clear checked items?",
            text = "This removes all purchased items from the active home's shopping list.",
            confirmLabel = "Clear",
            destructive = true,
            onConfirm = {
                activeHome?.let { vm.clearCheckedShopping(it.id) }
                confirmClearShopping = false
            },
            onDismiss = { confirmClearShopping = false }
        )
    }
    if (confirmDeleteHistory) {
        ConfirmDialog(
            title = "Delete history for this home?",
            text = "This permanently removes all completion records for the active home. Tasks and rooms remain.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                activeHome?.let { vm.resetHistoryForHome(it.id) }
                confirmDeleteHistory = false
            },
            onDismiss = { confirmDeleteHistory = false }
        )
    }
    if (confirmDeleteHome) {
        ConfirmDialog(
            title = "Delete this home?",
            text = "This will also remove its rooms, maintenance tasks, completion history, notes, and linked shopping items.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                activeHome?.let { vm.deleteHome(it.id) }
                confirmDeleteHome = false
            },
            onDismiss = { confirmDeleteHome = false }
        )
    }
    if (confirmResetAll) {
        ConfirmDialog(
            title = "Reset all local data?",
            text = "This will permanently remove every home, room, task, completion record, note, shopping item, and setting stored by Taskora Home on this device.",
            confirmLabel = "Reset everything",
            destructive = true,
            onConfirm = {
                vm.resetAllData()
                confirmResetAll = false
            },
            onDismiss = { confirmResetAll = false }
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun DestructiveButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) { Text(label, color = OverdueRed) }
}

@Composable
private fun InfoBlock(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
