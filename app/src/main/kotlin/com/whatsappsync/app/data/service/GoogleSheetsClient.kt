package com.whatsappsync.app.data.service

import android.content.Context
import com.whatsappsync.app.data.models.Message
import com.whatsappsync.app.data.repository.SharedPreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GoogleSheetsClient(context: Context) {
    class AuthenticationExpiredException : Exception("Google authorization expired. Sign in again.")

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()
    private val preferencesManager = SharedPreferencesManager(context.applicationContext)
    private val sheetsApiUrl = "https://sheets.googleapis.com/v4/spreadsheets"

    suspend fun appendMessagesToSheet(
        messages: List<Message>,
        onProgress: (uploaded: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Int> = runCatching {
        val accessToken = preferencesManager.getGoogleAccessToken()
            ?: throw AuthenticationExpiredException()
        val spreadsheetId = preferencesManager.getSpreadsheetId()
            ?: throw IllegalStateException("Spreadsheet ID is not configured")
        val syncedIds = preferencesManager.getSyncedMessageIds()
        val newMessages = messages.distinctBy { it.uniqueId }.filter { it.uniqueId !in syncedIds }
        if (newMessages.isEmpty()) return@runCatching 0

        var uploaded = 0
        newMessages.chunked(BATCH_SIZE).forEach { batch ->
            appendBatch(spreadsheetId, accessToken, batch)
            val completedIds = preferencesManager.getSyncedMessageIds().toMutableSet()
            completedIds.addAll(batch.map { it.uniqueId })
            preferencesManager.saveSyncedMessageIds(completedIds)
            preferencesManager.removePendingMessages(batch.map { it.uniqueId }.toSet())
            uploaded += batch.size
            onProgress(uploaded, newMessages.size)
        }
        preferencesManager.saveLastSyncTime(System.currentTimeMillis())
        uploaded
    }

    private fun appendBatch(spreadsheetId: String, accessToken: String, messages: List<Message>) {
        val values = messages.map { message ->
            listOf(
                formatTimestamp(message.timestamp), message.phoneNumber, message.senderName,
                message.messageText, getCurrentDate()
            )
        }
        val body = buildJsonObject {
            put("values", JsonArray(values.map { row -> JsonArray(row.map(::JsonPrimitive)) }))
        }.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$sheetsApiUrl/$spreadsheetId/values/Sheet1!A:E:append?valueInputOption=USER_ENTERED")
            .header("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401 || response.code == 403) throw AuthenticationExpiredException()
            if (!response.isSuccessful) {
                val detail = response.body?.string()?.take(500).orEmpty()
                throw IllegalStateException("Google Sheets error ${response.code}: ${detail.ifBlank { response.message }}")
            }
        }
    }

    fun isAuthenticated() = preferencesManager.getGoogleAccessToken() != null
    fun clearAuthentication() = preferencesManager.clearGoogleAuth()
    fun saveAccessToken(token: String) = preferencesManager.saveGoogleAccessToken(token)
    fun saveRefreshToken(token: String) = preferencesManager.saveGoogleRefreshToken(token)
    fun saveSpreadsheetId(id: String) = preferencesManager.saveSpreadsheetId(id)

    private fun formatTimestamp(timestamp: Long) =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    private fun getCurrentDate() =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    companion object { private const val BATCH_SIZE = 200 }
}
