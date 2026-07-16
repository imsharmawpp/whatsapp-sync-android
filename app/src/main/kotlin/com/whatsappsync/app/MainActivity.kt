package com.whatsappsync.app

import android.content.Intent
import android.os.Bundle
import com.whatsappsync.app.share.ShareImportCoordinator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.whatsappsync.app.ui.theme.WhatsAppSyncTheme
import com.whatsappsync.app.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingShare(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingShare(intent)
        setContent {
            WhatsAppSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun handleIncomingShare(incomingIntent: Intent) {
        ShareImportCoordinator.accept(incomingIntent)
        if (incomingIntent.action == Intent.ACTION_SEND || incomingIntent.action == Intent.ACTION_SEND_MULTIPLE) {
            setIntent(Intent(incomingIntent).setAction(null))
        } else {
            setIntent(incomingIntent)
        }
    }
}
