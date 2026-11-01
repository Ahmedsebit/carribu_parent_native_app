package com.carribu.parentnative.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.carribu.parentnative.AppContainer
import com.carribu.parentnative.data.session.Session
import com.carribu.parentnative.ui.*

private data class MainTab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    MainTab("track", "Track", Icons.Default.LocationOn),
    MainTab("trips", "Trips", Icons.Default.History),
    MainTab("alerts", "Alerts", Icons.Default.Notifications),
    MainTab("chat", "Chat", Icons.Default.Chat),
    MainTab("profile", "Profile", Icons.Default.Person)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(session: Session, container: AppContainer) {
    val navController = rememberNavController()
    val track: TrackViewModel = viewModel(factory = SimpleFactory { TrackViewModel(container.parentRepository) })
    val trips: TripsViewModel = viewModel(factory = SimpleFactory { TripsViewModel(container.parentRepository) })
    val alerts: AlertsViewModel = viewModel(factory = SimpleFactory { AlertsViewModel(container.parentRepository) })
    val chat: ChatViewModel = viewModel(factory = SimpleFactory { ChatViewModel(container.parentRepository) })
    val profile: ProfileViewModel = viewModel(
        factory = SimpleFactory { ProfileViewModel(container.parentRepository, container.authRepository) }
    )
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "track"
    val title = tabs.firstOrNull { it.route == route }?.label ?: "Carribu"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "track") {
            composable("track") {
                TrackScreen(track, container.realtime, padding)
            }
            composable("trips") {
                TripsScreen(trips, padding)
            }
            composable("alerts") {
                AlertsScreen(alerts, container.realtime, padding)
            }
            composable("chat") {
                ChatScreen(chat, session.user, container.realtime, padding)
            }
            composable("profile") {
                ProfileScreen(profile, session.user, padding)
            }
        }
    }
}
