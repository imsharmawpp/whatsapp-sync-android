package com.whatsappsync.app.data.models

data class Message(
    val phoneNumber: String,
    val senderName: String,
    val messageText: String,
    val timestamp: Long,
    val uniqueId: String = generateUniqueId(phoneNumber, messageText, timestamp)
)

fun generateUniqueId(phoneNumber: String, messageText: String, timestamp: Long): String {
    return "$phoneNumber|$messageText|$timestamp".hashCode().toString()
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
