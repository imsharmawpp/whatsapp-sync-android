package com.whatsappsync.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whatsappsync.app.BuildConfig
import com.whatsappsync.app.data.repository.SharedPreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClicked: () -> Unit) {
    val connected = BuildConfig.SYNC_API_URL.startsWith("https://") && BuildConfig.SYNC_API_TOKEN.isNotBlank()
    val store = remember { SharedPreferencesManager(LocalContext.current.applicationContext) }
    var whatsappName by remember { mutableStateOf(store.getWhatsAppName()) }
    var savedName by remember { mutableStateOf(store.getWhatsAppName()) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Lead identification", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your WhatsApp display name", style = MaterialTheme.typography.titleMedium)
                    Text("Enter your name exactly as it appears as a sender in exported chats. Your messages will be excluded so only the client's first message becomes a lead.")
                    OutlinedTextField(
                        value = whatsappName,
                        onValueChange = { whatsappName = it },
                        label = { Text("My WhatsApp name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { store.saveWhatsAppName(whatsappName); savedName = whatsappName.trim() },
                        enabled = whatsappName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save name") }
                    if (savedName.isNotBlank()) Text("Saved: $savedName", color = MaterialTheme.colorScheme.primary)
                }
            }

            Text("Private Google Sheets sync", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (connected) "Connected" else "Not configured",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Text(if (connected) "Leads are sent through the private sync service." else "Install an APK built with the private sync URL and token.")
                }
            }

            Text("App info", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            SettingItem("Version", BuildConfig.VERSION_NAME)
            SettingItem("Build", BuildConfig.VERSION_CODE.toString())
        }
    }
}

@Composable
private fun SettingItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
