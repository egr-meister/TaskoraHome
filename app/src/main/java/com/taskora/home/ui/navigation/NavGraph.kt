package com.taskora.home.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taskora.home.ui.screens.AddEditRoomScreen
import com.taskora.home.ui.screens.AddEditTaskScreen
import com.taskora.home.ui.screens.AllTasksScreen
import com.taskora.home.ui.screens.CalendarScreen
import com.taskora.home.ui.screens.CompletionDetailScreen
import com.taskora.home.ui.screens.HistoryScreen
import com.taskora.home.ui.screens.HomeMapScreen
import com.taskora.home.ui.screens.HomeProfilesScreen
import com.taskora.home.ui.screens.HomeSetupScreen
import com.taskora.home.ui.screens.OnboardingScreen
import com.taskora.home.ui.screens.RoomDetailScreen
import com.taskora.home.ui.screens.RoomManagementScreen
import com.taskora.home.ui.screens.SettingsScreen
import com.taskora.home.ui.screens.ShoppingScreen
import com.taskora.home.ui.screens.TaskDetailScreen
import com.taskora.home.ui.viewmodel.TaskoraViewModel

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomDestinations = listOf(
    BottomDestination(Routes.HOME_MAP, "Home", Icons.Filled.Home),
    BottomDestination(Routes.ALL_TASKS, "Tasks", Icons.Filled.Checklist),
    BottomDestination(Routes.CALENDAR, "Calendar", Icons.Filled.CalendarMonth),
    BottomDestination(Routes.SHOPPING, "Shopping", Icons.Filled.ShoppingCart),
    BottomDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

/** Root composable: gates on bootstrap, hosts the bottom bar and NavHost. */
@Composable
fun TaskoraApp(vm: TaskoraViewModel) {
    val bootstrapped by vm.bootstrapped.collectAsStateWithLifecycle()
    val data by vm.appData.collectAsStateWithLifecycle()

    if (!bootstrapped) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = bottomDestinations.any { dest ->
        currentRoute == dest.route || currentRoute?.startsWith("${dest.route}?") == true
    }

    val startDestination = if (data.settings.onboardingCompleted) {
        Routes.HOME_MAP
    } else {
        Routes.ONBOARDING
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route ||
                            currentRoute?.startsWith("${dest.route}?") == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.ONBOARDING) { OnboardingScreen(vm, navController) }
            composable(Routes.HOME_SETUP) { HomeSetupScreen(vm, navController) }

            composable(Routes.HOME_MAP) { HomeMapScreen(vm, navController) }

            composable(
                route = "${Routes.ALL_TASKS}?${Routes.ARG_FILTER}={${Routes.ARG_FILTER}}",
                arguments = listOf(navArgument(Routes.ARG_FILTER) {
                    type = NavType.StringType; nullable = true; defaultValue = null
                })
            ) { entry ->
                AllTasksScreen(vm, navController, entry.arguments?.getString(Routes.ARG_FILTER))
            }
            // Also register the bare route for bottom-nav navigation.
            composable(Routes.ALL_TASKS) { AllTasksScreen(vm, navController, null) }

            composable(Routes.CALENDAR) { CalendarScreen(vm, navController) }
            composable(Routes.SHOPPING) { ShoppingScreen(vm, navController) }
            composable(Routes.SETTINGS) { SettingsScreen(vm, navController) }

            composable(
                route = "${Routes.ROOM_DETAIL}/{${Routes.ARG_ROOM_ID}}",
                arguments = listOf(navArgument(Routes.ARG_ROOM_ID) { type = NavType.StringType })
            ) { entry ->
                RoomDetailScreen(vm, navController, entry.arguments?.getString(Routes.ARG_ROOM_ID))
            }

            composable(Routes.ROOM_MANAGEMENT) { RoomManagementScreen(vm, navController) }

            composable(
                route = "${Routes.ADD_EDIT_ROOM}?${Routes.ARG_ROOM_ID}={${Routes.ARG_ROOM_ID}}",
                arguments = listOf(navArgument(Routes.ARG_ROOM_ID) {
                    type = NavType.StringType; nullable = true; defaultValue = null
                })
            ) { entry ->
                AddEditRoomScreen(vm, navController, entry.arguments?.getString(Routes.ARG_ROOM_ID))
            }
            composable(Routes.ADD_EDIT_ROOM) { AddEditRoomScreen(vm, navController, null) }

            composable(
                route = "${Routes.ADD_EDIT_TASK}?${Routes.ARG_TASK_ID}={${Routes.ARG_TASK_ID}}&${Routes.ARG_ROOM_ID}={${Routes.ARG_ROOM_ID}}",
                arguments = listOf(
                    navArgument(Routes.ARG_TASK_ID) {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    },
                    navArgument(Routes.ARG_ROOM_ID) {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    }
                )
            ) { entry ->
                AddEditTaskScreen(
                    vm,
                    navController,
                    entry.arguments?.getString(Routes.ARG_TASK_ID),
                    entry.arguments?.getString(Routes.ARG_ROOM_ID)
                )
            }
            composable(Routes.ADD_EDIT_TASK) { AddEditTaskScreen(vm, navController, null, null) }

            composable(
                route = "${Routes.TASK_DETAIL}/{${Routes.ARG_TASK_ID}}",
                arguments = listOf(navArgument(Routes.ARG_TASK_ID) { type = NavType.StringType })
            ) { entry ->
                TaskDetailScreen(vm, navController, entry.arguments?.getString(Routes.ARG_TASK_ID))
            }

            composable(Routes.HISTORY) { HistoryScreen(vm, navController) }

            composable(
                route = "${Routes.COMPLETION_DETAIL}/{${Routes.ARG_COMPLETION_ID}}",
                arguments = listOf(navArgument(Routes.ARG_COMPLETION_ID) { type = NavType.StringType })
            ) { entry ->
                CompletionDetailScreen(
                    vm,
                    navController,
                    entry.arguments?.getString(Routes.ARG_COMPLETION_ID)
                )
            }

            composable(Routes.HOME_PROFILES) { HomeProfilesScreen(vm, navController) }
        }
    }
}
