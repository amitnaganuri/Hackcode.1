package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.FocusGuardBottomNav
import com.example.ui.screens.blocks.BlocksScreen
import com.example.ui.screens.chamber.ActiveFocusChamberScreen
import com.example.ui.screens.editor.InterventionEditorScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.insights.InsightsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.FocusSurface
import com.example.viewmodel.FocusGuardViewModel

@Composable
fun FocusGuardApp(
    viewModel: FocusGuardViewModel = viewModel(factory = FocusGuardViewModel.Factory)
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.Blocks.route,
        Screen.Insights.route,
        Screen.Settings.route
    )
    val shouldShowBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        containerColor = FocusSurface,
        bottomBar = {
            if (shouldShowBottomBar) {
                FocusGuardBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToChamber = { navController.navigate(Screen.Chamber.route) },
                    onNavigateToBlocks = { navController.navigate(Screen.Blocks.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Blocks.route) {
                BlocksScreen(
                    viewModel = viewModel,
                    onNavigateToChamber = { navController.navigate(Screen.Chamber.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Insights.route) {
                InsightsScreen(
                    viewModel = viewModel,
                    onNavigateToChamber = { navController.navigate(Screen.Chamber.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToChamber = { navController.navigate(Screen.Chamber.route) },
                    onNavigateToEditor = { navController.navigate(Screen.InterventionEditor.route) }
                )
            }

            composable(Screen.Chamber.route) {
                ActiveFocusChamberScreen(
                    viewModel = viewModel,
                    onReturnToFocus = { navController.popBackStack() },
                    onNavigateToEditor = { navController.navigate(Screen.InterventionEditor.route) }
                )
            }

            composable(Screen.InterventionEditor.route) {
                InterventionEditorScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onPreviewChamber = { navController.navigate(Screen.Chamber.route) }
                )
            }
        }
    }
}
