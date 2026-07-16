package com.whatsappsync.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.whatsappsync.app.data.models.Message
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SharedPreferencesManager(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedSharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "whatsapp_sync_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveSyncedMessageIds(ids: Set<String>) {
        encryptedSharedPreferences.edit().putStringSet("synced_message_ids", ids).apply()
    }
    
    fun getSyncedMessageIds(): Set<String> {
        return encryptedSharedPreferences.getStringSet("synced_message_ids", emptySet()) ?: emptySet()
    }
    
    fun savePendingMessages(messages: List<Message>) {
        encryptedSharedPreferences.edit()
            .putString("pending_messages", Json.encodeToString(messages.distinctBy { it.uniqueId }))
            .apply()
    }

    fun getPendingMessages(): List<Message> {
        val value = encryptedSharedPreferences.getString("pending_messages", null) ?: return emptyList()
        return runCatching { Json.decodeFromString<List<Message>>(value) }.getOrDefault(emptyList())
    }

    fun addPendingMessage(message: Message) {
        val messages = getPendingMessages().toMutableList()
        if (messages.none { it.uniqueId == message.uniqueId } &&
            message.uniqueId !in getSyncedMessageIds()
        ) {
            messages += message
            savePendingMessages(messages)
        }
    }

    fun removePendingMessages(ids: Set<String>) {
        savePendingMessages(getPendingMessages().filterNot { it.uniqueId in ids })
    }

    fun savePhoneMapping(chatName: String, phoneNumber: String) {
        encryptedSharedPreferences.edit()
            .putString("phone_${chatName.trim().lowercase()}", phoneNumber.trim())
            .apply()
    }

    fun getPhoneMapping(chatName: String): String =
        encryptedSharedPreferences.getString("phone_${chatName.trim().lowercase()}", "").orEmpty()

    fun saveLastSyncTime(time: Long) {
        encryptedSharedPreferences.edit().putLong("last_sync_time", time).apply()
    }
    
    fun getLastSyncTime(): Long {
        return encryptedSharedPreferences.getLong("last_sync_time", 0L)
    }

    fun recordSuccessfulSync(messageCount: Int, time: Long = System.currentTimeMillis()) {
        val dayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(time))
        val existingDay = encryptedSharedPreferences.getString("sync_count_day", "")
        val currentCount = if (existingDay == dayKey) {
            encryptedSharedPreferences.getInt("sync_count_today", 0)
        } else {
            0
        }
        encryptedSharedPreferences.edit()
            .putLong("last_sync_time", time)
            .putInt("last_sync_count", messageCount)
            .putString("sync_count_day", dayKey)
            .putInt("sync_count_today", currentCount + messageCount)
            .apply()
    }

    fun getSyncedToday(now: Long = System.currentTimeMillis()): Int {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(now))
        return if (encryptedSharedPreferences.getString("sync_count_day", "") == today) {
            encryptedSharedPreferences.getInt("sync_count_today", 0)
        } else {
            0
        }
    }

    fun getLastSyncCount(): Int = encryptedSharedPreferences.getInt("last_sync_count", 0)
}
