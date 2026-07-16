package com.whatsappsync.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.whatsappsync.app.share.ShareImportCoordinator
import com.whatsappsync.app.ui.screens.home.HomeScreen
import com.whatsappsync.app.ui.screens.permissions.PermissionsScreen
import com.whatsappsync.app.ui.screens.settings.SettingsScreen
import com.whatsappsync.app.ui.screens.sync.SyncScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val incomingShare by ShareImportCoordinator.incoming.collectAsState()

    LaunchedEffect(incomingShare) {
        if (!incomingShare.isNullOrEmpty() && navController.currentDestination?.route != "sync") {
            navController.navigate("sync") { launchSingleTop = true }
        }
    }

    NavHost(navController = navController, startDestination = "permissions") {
        composable("permissions") {
            PermissionsScreen(
                onPermissionsGranted = {
                    navController.navigate("home") {
                        popUpTo("permissions") { inclusive = true }
                    }
                },
            )
        }
        composable("home") {
            HomeScreen(
                onSyncClicked = { navController.navigate("sync") },
                onSettingsClicked = { navController.navigate("settings") },
            )
        }
        composable("sync") {
            SyncScreen(onBackClicked = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onBackClicked = { navController.popBackStack() })
        }
    }
}
