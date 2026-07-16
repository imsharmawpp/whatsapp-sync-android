package com.whatsappsync.app.ui.screens.permissions

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.whatsappsync.app.permissions.PermissionManager
import com.whatsappsync.app.permissions.PermissionStatus

@Composable
fun PermissionsScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val manager = remember(context) { PermissionManager(context) }
    var status by remember { mutableStateOf(readStatus(manager)) }

    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { status = readStatus(manager) }

    DisposableEffect(lifecycleOwner, manager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) status = readStatus(manager)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Enable message capture",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Enable each item below. Status refreshes automatically when you return to the app.",
            style = MaterialTheme.typography.bodyLarge,
        )

        PermissionItem(
            title = "Accessibility service",
            description = "Allows capture while you use WhatsApp or WhatsApp Business.",
            enabled = status.accessibilityService,
            action = "Open accessibility settings",
            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
        )
        PermissionItem(
            title = "Notification access",
            description = "Queues future visible WhatsApp notifications for one-tap sync.",
            enabled = status.notificationListener,
            action = "Open notification access",
            onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
        )
        PermissionItem(
            title = "Contacts",
            description = "Resolves saved names and phone numbers where Android permits it.",
            enabled = status.contactsPermission,
            action = "Allow contacts",
            onClick = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) },
        )
        PermissionItem(
            title = "WhatsApp installed",
            description = "Standard WhatsApp and WhatsApp Business are both supported.",
            enabled = status.whatsAppInstalled,
            action = null,
            onClick = {},
        )

        Button(
            onClick = onPermissionsGranted,
            enabled = status.allGranted,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue")
        }
        if (!status.allGranted) {
            Text(
                text = "Complete the three permission steps to continue.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    enabled: Boolean,
    action: String?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (enabled) "Enabled" else "Not enabled",
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(description, style = MaterialTheme.typography.bodyMedium)
        if (!enabled && action != null) {
            OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(action)
            }
        }
    }
}

private fun readStatus(manager: PermissionManager) = PermissionStatus(
    contactsPermission = manager.contactsPermissionGranted(),
    accessibilityService = manager.accessibilityServiceEnabled(),
    notificationListener = manager.notificationListenerEnabled(),
    whatsAppInstalled = manager.whatsAppInstalled(),
)
