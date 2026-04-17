package com.hunterxdk.stayconnected.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.hunterxdk.stayconnected.ui.addedit.AddEditContactScreen
import com.hunterxdk.stayconnected.ui.contacts.ContactsScreen
import com.hunterxdk.stayconnected.ui.dashboard.DashboardScreen
import com.hunterxdk.stayconnected.ui.detail.ContactDetailScreen
import com.hunterxdk.stayconnected.ui.schedule.ScheduleScreen
import com.hunterxdk.stayconnected.ui.settings.SettingsScreen

private val bottomNavRoutes = setOf(
    Screen.Dashboard.route,
    Screen.Contacts.route,
    Screen.Schedule.route,
    Screen.Settings.route
)

@Composable
fun NavGraph(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                NavigationBar(
                    containerColor = colorScheme.surfaceContainer.copy(alpha = 0.9f)
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentRoute == Screen.Dashboard.route,
                        onClick = { navigateToTab(navController, Screen.Dashboard.route) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = colorScheme.primaryContainer
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.People, contentDescription = "Contacts") },
                        label = { Text("Contacts") },
                        selected = currentRoute == Screen.Contacts.route,
                        onClick = { navigateToTab(navController, Screen.Contacts.route) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = colorScheme.primaryContainer
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Schedule") },
                        label = { Text("Schedule") },
                        selected = currentRoute == Screen.Schedule.route,
                        onClick = { navigateToTab(navController, Screen.Schedule.route) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = colorScheme.primaryContainer
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = currentRoute == Screen.Settings.route,
                        onClick = { navigateToTab(navController, Screen.Settings.route) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onAddContactClick = {
                        navController.navigate(Screen.AddEditContact.createRoute())
                    },
                    onContactClick = { contactId ->
                        navController.navigate(Screen.ContactDetail.createRoute(contactId))
                    },
                    onSettingsClick = {
                        navigateToTab(navController, Screen.Settings.route)
                    }
                )
            }

            composable(Screen.Contacts.route) {
                ContactsScreen(
                    onContactClick = { contactId ->
                        navController.navigate(Screen.ContactDetail.createRoute(contactId))
                    },
                    onAddContactClick = {
                        navController.navigate(Screen.AddEditContact.createRoute())
                    }
                )
            }

            composable(Screen.Schedule.route) {
                ScheduleScreen(
                    onContactClick = { contactId ->
                        navController.navigate(Screen.ContactDetail.createRoute(contactId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(onBackClick = { navController.popBackStack() })
            }

            composable(
                route = Screen.ContactDetail.route,
                arguments = listOf(navArgument("contactId") { type = NavType.LongType })
            ) { backStackEntry ->
                val contactId = backStackEntry.arguments?.getLong("contactId") ?: -1L
                ContactDetailScreen(
                    contactId = contactId,
                    onBackClick = { navController.popBackStack() },
                    onEditClick = {
                        navController.navigate(Screen.AddEditContact.createRoute(contactId))
                    }
                )
            }

            composable(
                route = Screen.AddEditContact.route,
                arguments = listOf(
                    navArgument("contactId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) {
                AddEditContactScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(Screen.Dashboard.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
