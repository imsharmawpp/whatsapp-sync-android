package com.whatsappsync.app.data.service

import android.content.Context
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.oauth2.OAuth2Credentials
import com.google.auth.transport.http.HttpTransportFactory
import com.whatsappsync.app.data.models.Message
import com.whatsappsync.app.data.repository.SharedPreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Client for interacting with Google Sheets API
 */
class GoogleSheetsClient(private val context: Context) {
    
    private val preferencesManager = SharedPreferencesManager(context)
    private var sheetsService: Sheets? = null
    
    private fun getAuthenticatedSheetsService(): Sheets? {
        val accessToken = preferencesManager.getGoogleAccessToken() ?: return null
        
        val credentials = OAuth2Credentials.newBuilder()
            .setAccessToken(com.google.auth.oauth2.AccessToken(accessToken, null))
            .build()
        
        return Sheets.Builder(
            HttpTransportFactory.DEFAULT.create(),
            com.google.api.client.json.jackson2.JacksonFactory.getDefaultInstance(),
            credentials
        )
            .setApplicationName("WhatsApp Sync")
            .build()
    }
    
    suspend fun appendMessagesToSheet(messages: List<Message>): Result<Int> = runCatching {
        val spreadsheetId = preferencesManager.getSpreadsheetId()
            ?: throw IllegalStateException("Spreadsheet ID not configured")
        
        val service = getAuthenticatedSheetsService()
            ?: throw IllegalStateException("Not authenticated with Google")
        
        val syncedIds = preferencesManager.getSyncedMessageIds()
        val newMessages = messages.filter { it.uniqueId !in syncedIds }
        
        if (newMessages.isEmpty()) {
            return@runCatching 0
        }
        
        val values = newMessages.map { message ->
            listOf(
                formatTimestamp(message.timestamp),
                message.phoneNumber,
                message.senderName,
                message.messageText,
                getCurrentDate()
            )
        }
        
        val body = ValueRange()
            .setValues(values)
        
        val result = service.spreadsheets().values()
            .append(spreadsheetId, "Sheet1", body)
            .setValueInputOption("USER_ENTERED")
            .execute()
        
        // Update synced message IDs
        val updatedSyncedIds = syncedIds.toMutableSet()
        updatedSyncedIds.addAll(newMessages.map { it.uniqueId })
        preferencesManager.saveSyncedMessageIds(updatedSyncedIds)
        preferencesManager.saveLastSyncTime(System.currentTimeMillis())
        
        newMessages.size
    }
    
    fun saveAccessToken(token: String) {
        preferencesManager.saveGoogleAccessToken(token)
    }
    
    fun saveRefreshToken(token: String) {
        preferencesManager.saveGoogleRefreshToken(token)
    }
    
    fun saveSpreadsheetId(id: String) {
        preferencesManager.saveSpreadsheetId(id)
    }
    
    fun isAuthenticated(): Boolean {
        return preferencesManager.getGoogleAccessToken() != null
    }
    
    fun clearAuthentication() {
        preferencesManager.clearGoogleAuth()
        sheetsService = null
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
