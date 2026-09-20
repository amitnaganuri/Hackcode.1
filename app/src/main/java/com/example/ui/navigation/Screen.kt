package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Blocks : Screen("blocks", "Blocks")
    object Insights : Screen("insights", "Insights")
    object Settings : Screen("settings", "Settings")
    object Chamber : Screen("chamber", "Focus Chamber")
    object InterventionEditor : Screen("intervention_editor", "Intervention Editor")
}
