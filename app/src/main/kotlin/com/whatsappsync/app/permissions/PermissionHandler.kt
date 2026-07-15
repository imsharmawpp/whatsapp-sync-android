package com.whatsappsync.app.permissions

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Composable hook for requesting permissions
 */
@Composable
fun rememberPermissionRequester(
    onPermissionsResult: (granted: Boolean) -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        onPermissionsResult(allGranted)
    }
    
    remember(permissionLauncher) {
        {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.QUERY_ALL_PACKAGES
                )
            )
        }
    }
}

/**
 * Get the required permissions for the app
 */
fun getRequiredPermissions(): Array<String> {
    return arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.INTERNET,
        Manifest.permission.QUERY_ALL_PACKAGES
    )
}

/**
 * Get accessibility service permissions (these are requested via settings, not ActivityResultContracts)
 */
fun getAccessibilityServicePermissions(): List<String> {
    return listOf(
        "android.accessibilityservice.AccessibilityService"
    )
}
