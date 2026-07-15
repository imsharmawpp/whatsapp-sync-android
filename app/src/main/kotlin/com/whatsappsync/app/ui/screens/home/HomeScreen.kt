package com.whatsappsync.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onSyncClicked: () -> Unit,
    onSettingsClicked: () -> Unit
) {
    val syncedToday = remember { mutableStateOf("0") }
    val lastSyncTime = remember { mutableStateOf("Never") }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("WhatsApp Sync") },
            actions = {
                IconButton(onClick = onSettingsClicked) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Dashboard",
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            StatCard(
                label = "Messages Synced Today",
                value = syncedToday.value
            )
            
            StatCard(
                label = "Last Sync",
                value = lastSyncTime.value
            )
            
            Button(
                onClick = onSyncClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 8.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Sync", modifier = Modifier.padding(end = 8.dp))
                Text("Sync Now")
            }
            
            OutlinedButton(
                onClick = { /* TODO: View history */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Sync History")
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = value,
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
