package com.whatsappsync.app.ui.screens.sync

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onBackClicked: () -> Unit,
    viewModel: SyncViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::selectExport)
    }
    val working = state.phase == SyncViewModel.Phase.Working

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Sync Messages") },
            navigationIcon = {
                IconButton(onClick = onBackClicked, enabled = !working) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("New message capture", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${state.pendingCount} messages waiting to sync",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Captures future WhatsApp Business events after Accessibility access is enabled. It cannot read WhatsApp's private database.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = viewModel::syncCapturedMessages,
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sync new captured messages") }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Previous 90 days", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "In WhatsApp Business, export a chat without media, then select its TXT or ZIP file here.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = { picker.launch(arrayOf("text/plain", "application/zip", "application/octet-stream")) },
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Choose exported chat") }
                }
            }

            StatusCard(state)

            if (state.phase == SyncViewModel.Phase.PermissionRequired) {
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Accessibility settings") }
            }

            state.preview?.let { preview ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Import preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(preview.fileName)
                        Text("${preview.messages.size} messages in the last 90 days")
                        Text("${preview.outsideWindow} older and ${preview.malformed} unrecognized records skipped")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = viewModel::cancelImport, modifier = Modifier.weight(1f)) { Text("Cancel") }
                            Button(onClick = viewModel::confirmImport, modifier = Modifier.weight(1f)) { Text("Upload") }
                        }
                    }
                }
            }

            if (state.phase == SyncViewModel.Phase.Error) {
                OutlinedButton(onClick = viewModel::retry, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
            }
        }
    }
}

@Composable
private fun StatusCard(state: SyncViewModel.UiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.phase == SyncViewModel.Phase.Working) {
                if (state.progress == null) CircularProgressIndicator()
                else LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
            }
            Text(state.status, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
