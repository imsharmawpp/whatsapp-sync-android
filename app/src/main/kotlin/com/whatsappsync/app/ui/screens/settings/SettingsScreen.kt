package com.whatsappsync.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    onBackClicked: () -> Unit
) {
    val spreadsheetId = remember { mutableStateOf("") }
    val isAuthenticated = remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Google Sheets",
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (!isAuthenticated.value) {
                Text(
                    text = "Not connected to Google Sheets yet",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Button(
                    onClick = {
                        isAuthenticated.value = true
                        // TODO: Implement Google OAuth flow
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text("Connect to Google Sheets")
                }
            } else {
                Text(
                    text = "Connected",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = spreadsheetId.value,
                    onValueChange = { spreadsheetId.value = it },
                    label = { Text("Spreadsheet ID") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                
                Text(
                    text = "Find your Spreadsheet ID in the URL: docs.google.com/spreadsheets/d/[ID]/edit",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedButton(
                    onClick = {
                        isAuthenticated.value = false
                        spreadsheetId.value = ""
                        // TODO: Implement logout
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disconnect")
                }
            }
            
            Text(
                text = "App Info",
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
            )
            
            SettingItem(label = "Version", value = "1.0.0")
            SettingItem(label = "Build", value = "1")
        }
    }
}

@Composable
fun SettingItem(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text = label, fontSize = 12.sp)
        Text(text = value, fontSize = 14.sp)
    }
}
