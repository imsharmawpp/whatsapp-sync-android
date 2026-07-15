package com.whatsappsync.app.ui.screens.permissions

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionsScreen(
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val allPermissionsGranted = remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Check if all permissions are granted
        // In production, use proper permission checking with ActivityResultContracts
        allPermissionsGranted.value = true
    }
    
    if (allPermissionsGranted.value) {
        LaunchedEffect(Unit) {
            onPermissionsGranted()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Permissions Required",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "WhatsApp Sync needs a few permissions to work properly:",
            modifier = Modifier.padding(bottom = 24.dp),
            textAlign = TextAlign.Center
        )
        
        PermissionItem(
            title = "Accessibility Service",
            description = "Needed to read messages from WhatsApp Business app"
        )
        
        PermissionItem(
            title = "Contacts",
            description = "Needed to enrich message data with contact names"
        )
        
        PermissionItem(
            title = "Internet",
            description = "Needed to sync data to Google Sheets"
        )
        
        Button(
            onClick = {
                // Open accessibility settings
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 8.dp)
        ) {
            Text("Open Accessibility Settings")
        }
        
        OutlinedButton(
            onClick = { onPermissionsGranted() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Permissions Already Granted")
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = description,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
