package com.whatsappsync.app.data.service

import android.content.Context
import com.whatsappsync.app.BuildConfig
import com.whatsappsync.app.data.models.Message
import com.whatsappsync.app.data.repository.SharedPreferencesManager
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
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()
    private val preferencesManager = SharedPreferencesManager(context.applicationContext)

    suspend fun appendMessagesToSheet(
        messages: List<Message>,
        onProgress: (uploaded: Int, total: Int) -> Unit = { _, _ -> },
    ): Result<Int> = runCatching {
        checkConfiguration()
        val syncedIds = preferencesManager.getSyncedMessageIds()
        val newMessages = messages.distinctBy { it.uniqueId }.filter { it.uniqueId !in syncedIds }
        if (newMessages.isEmpty()) return@runCatching 0

        var uploaded = 0
        newMessages.chunked(BATCH_SIZE).forEach { batch ->
            appendBatch(batch)
            val completedIds = preferencesManager.getSyncedMessageIds().toMutableSet()
            completedIds.addAll(batch.map { it.uniqueId })
            preferencesManager.saveSyncedMessageIds(completedIds)
            preferencesManager.removePendingMessages(batch.map { it.uniqueId }.toSet())
            uploaded += batch.size
            onProgress(uploaded, newMessages.size)
        }
        preferencesManager.recordSuccessfulSync(uploaded)
        uploaded
    }

    private fun appendBatch(messages: List<Message>) {
        val payload = buildJsonObject {
            put("messages", JsonArray(messages.map { message ->
                buildJsonObject {
                    put("timestamp", message.timestamp)
                    put("phoneNumber", message.phoneNumber)
                    put("senderName", message.senderName)
                    put("messageText", message.messageText)
                    put("conversationName", message.conversationName)
                    put("source", message.source)
                    put("direction", message.direction)
                    put("uniqueId", message.uniqueId)
                }
            }))
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(BuildConfig.SYNC_API_URL.trimEnd('/'))
            .header("Authorization", "Bearer ${BuildConfig.SYNC_API_TOKEN}")
            .post(payload)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401 || response.code == 403) {
                throw IllegalStateException("Private sync authorization failed. Update the app connection token.")
            }
            if (!response.isSuccessful) {
                val detail = response.body?.string()?.take(500).orEmpty()
                throw IllegalStateException("Sync service error ${response.code}: ${detail.ifBlank { response.message }}")
            }
        }
    }

    fun isConfigured(): Boolean =
        BuildConfig.SYNC_API_URL.startsWith("https://") && BuildConfig.SYNC_API_TOKEN.isNotBlank()

    private fun checkConfiguration() {
        check(isConfigured()) { "Private Sheets sync is not configured in this APK." }
    }

    companion object {
        private const val BATCH_SIZE = 200
    }
}
