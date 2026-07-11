package com.taskora.home.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.taskora.home.data.HomeType
import com.taskora.home.ui.components.DisclaimerBox
import com.taskora.home.ui.components.DropdownField
import com.taskora.home.ui.components.SectionLabel
import com.taskora.home.ui.components.TaskoraScaffold
import com.taskora.home.ui.navigation.Routes
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.Disclaimers
import com.taskora.home.util.Templates
import com.taskora.home.util.Validation
import com.taskora.home.util.homeTypeLabel

@Composable
fun HomeSetupScreen(vm: TaskoraViewModel, nav: NavHostController) {
    var name by remember { mutableStateOf("") }
    var homeType by remember { mutableStateOf(HomeType.House) }
    var description by remember { mutableStateOf("") }
    var layoutKey by remember { mutableStateOf(Templates.NONE_KEY) }
    var showError by remember { mutableStateOf(false) }

    val canGoBack = nav.previousBackStackEntry != null

    TaskoraScaffold(
        title = "Set Up Home",
        onBack = if (canGoBack) ({ nav.popBackStack() }) else null
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp)
        ) {
            SectionLabel("Home details")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; showError = false },
                label = { Text("Home name (required)") },
                singleLine = true,
                isError = showError && Validation.validateHome(name).error("name") != null,
                supportingText = {
                    val err = if (showError) Validation.validateHome(name).error("name") else null
                    if (err != null) Text(err)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            DropdownField(
                label = "Home type",
                options = HomeType.entries,
                selected = homeType,
                optionLabel = { homeTypeLabel(it) },
                onSelected = { homeType = it }
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("Optional layout template")
            Text(
                text = "Templates create only room placeholders you can rename or remove.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            DropdownField(
                label = "Layout",
                options = Templates.layouts,
                selected = Templates.layoutByKey(layoutKey) ?: Templates.layouts.last(),
                optionLabel = { "${it.name} — ${it.description}" },
                onSelected = { layoutKey = it.key }
            )

            Spacer(Modifier.height(16.dp))
            DisclaimerBox(text = Disclaimers.MAP_NOT_ARCHITECTURAL)

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val result = Validation.validateHome(name)
                    if (!result.isValid) {
                        showError = true
                        return@Button
                    }
                    val rooms = Templates.layoutByKey(layoutKey)?.rooms ?: emptyList()
                    vm.addHomeWithLayout(name, homeType, description, rooms) {
                        nav.navigate(Routes.HOME_MAP) {
                            popUpTo(Routes.HOME_SETUP) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Home") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
