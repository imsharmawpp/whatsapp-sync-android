package com.whatsappsync.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.whatsappsync.app.ui.screens.home.HomeScreen
import com.whatsappsync.app.ui.screens.sync.SyncScreen
import com.whatsappsync.app.ui.screens.settings.SettingsScreen
import com.whatsappsync.app.ui.screens.permissions.PermissionsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "permissions"
    ) {
        composable("permissions") {
            PermissionsScreen(
                onPermissionsGranted = {
                    navController.navigate("home") {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }
        
        composable("home") {
            HomeScreen(
                onSyncClicked = { navController.navigate("sync") },
                onSettingsClicked = { navController.navigate("settings") }
            )
        }
        
        composable("sync") {
            SyncScreen(
                onBackClicked = { navController.popBackStack() }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBackClicked = { navController.popBackStack() }
            )
        }
    }
}
