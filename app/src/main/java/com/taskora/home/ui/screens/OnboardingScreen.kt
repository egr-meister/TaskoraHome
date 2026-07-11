package com.taskora.home.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.taskora.home.ui.components.DisclaimerBox
import com.taskora.home.ui.components.HouseIllustration
import com.taskora.home.ui.navigation.Routes
import com.taskora.home.ui.viewmodel.TaskoraViewModel
import com.taskora.home.util.Disclaimers

@Composable
fun OnboardingScreen(vm: TaskoraViewModel, nav: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        HouseIllustration(sizeDp = 132)

        Text(
            text = "Taskora Home",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Organize recurring home maintenance tasks and keep a clear local history.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))
        OnboardingPoint("Map maintenance tasks by room on a simple home layout.")
        OnboardingPoint("Track filters, lighting, appliances, cleaning, and custom household routines.")
        OnboardingPoint("Statuses are Good, Soon, and Overdue — shown as text and color.")
        OnboardingPoint("Mark tasks complete manually and review what is due next.")
        OnboardingPoint("Keep a local completion history and an in-app monthly calendar.")
        OnboardingPoint("Maintain a household shopping list for supplies you need.")
        OnboardingPoint("Reminders appear inside the app only — no push notifications.")
        OnboardingPoint("Your home data stays on this device. No internet, camera, or smart-home connection.")

        Spacer(Modifier.height(4.dp))
        DisclaimerBox(text = Disclaimers.SAFETY)
        DisclaimerBox(text = Disclaimers.MAP_NOT_ARCHITECTURAL)

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                vm.completeOnboarding()
                nav.navigate(Routes.HOME_SETUP) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Set Up Home") }

        OutlinedButton(
            onClick = {
                vm.completeOnboarding()
                nav.navigate(Routes.HOME_MAP) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Explore First") }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OnboardingPoint(text: String) {
    Text(
        text = "•  $text",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
}
