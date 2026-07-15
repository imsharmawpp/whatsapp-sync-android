package com.whatsappsync.app.data.service

import android.content.Context
import com.whatsappsync.app.data.models.Message
import com.whatsappsync.app.data.repository.SharedPreferencesManager
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Client for interacting with Google Sheets API via REST
 * Uses direct HTTP calls instead of Google client library to avoid dependency issues
 */
class GoogleSheetsClient(private val context: Context) {
    
    private val client = OkHttpClient()
    private val preferencesManager = SharedPreferencesManager(context)
    private val sheetsApiUrl = "https://sheets.googleapis.com/v4/spreadsheets"
    
    suspend fun appendMessagesToSheet(messages: List<Message>): Result<Int> = runCatching {
        val accessToken = preferencesManager.getGoogleAccessToken()
            ?: throw IllegalStateException("Not authenticated with Google")
        
        val spreadsheetId = preferencesManager.getSpreadsheetId()
            ?: throw IllegalStateException("Spreadsheet ID not configured")
        
        val syncedIds = preferencesManager.getSyncedMessageIds()
        val newMessages = messages.filter { it.uniqueId !in syncedIds }
        
        if (newMessages.isEmpty()) {
            return@runCatching 0
        }
        
        // Build JSON request body
        val values = newMessages.map { message ->
            listOf(
                formatTimestamp(message.timestamp),
                message.phoneNumber,
                message.senderName,
                message.messageText,
                getCurrentDate()
            )
        }
        
        val requestBody = buildJsonObject {
            put("values", JsonArray(values.map { row ->
                JsonArray(row.map { JsonPrimitive(it) })
            }))
        }
        
        val request = Request.Builder()
            .url("$sheetsApiUrl/$spreadsheetId/values/Sheet1!A:E:append?valueInputOption=USER_ENTERED")
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("API Error: ${response.code} - ${response.message}")
        }
        
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
