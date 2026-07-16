package com.whatsappsync.app.data.models

import java.security.MessageDigest
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val phoneNumber: String,
    val senderName: String,
    val messageText: String,
    val timestamp: Long,
    val conversationName: String = senderName,
    val source: String = "export",
    val direction: String = "unknown",
    val uniqueId: String = generateUniqueId(conversationName, senderName, messageText, timestamp)
)

fun generateUniqueId(
    conversationName: String,
    senderName: String,
    messageText: String,
    timestamp: Long
): String {
    val normalized = listOf(conversationName, senderName, messageText)
        .joinToString("|") { it.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ") }
    // Accessibility events can repeat within the same minute with slightly different event times.
    val minuteBucket = timestamp / 60_000L
    return MessageDigest.getInstance("SHA-256")
        .digest("$normalized|$minuteBucket".toByteArray())
        .joinToString("") { "%02x".format(it) }
}

data class SyncResult(
    val successCount: Int,
    val failedCount: Int,
    val totalMessages: Int,
    val errorMessage: String? = null
)

data class SyncStatus(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val lastSyncStatus: String = "Never synced",
    val totalSyncedMessages: Int = 0
)
