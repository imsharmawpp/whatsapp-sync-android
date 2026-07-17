package com.whatsappsync.app.notifications

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.whatsappsync.app.data.models.Message
import com.whatsappsync.app.data.service.AutomaticLeadSync
import com.whatsappsync.app.permissions.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WhatsAppNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.packageName !in PermissionManager.WHATSAPP_PACKAGES) return
        val message = NotificationMessageParser.parse(notification) ?: return
        scope.launch { AutomaticLeadSync(applicationContext).resolveAndSync(message) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

object NotificationMessageParser {
    private val summaryPattern = Regex("(?:\\d+ new messages?|new messages?|checking for new messages)", RegexOption.IGNORE_CASE)

    fun parse(sbn: StatusBarNotification): Message? {
        val notification = sbn.notification
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        val extras = notification.extras
        val conversation = value(extras, Notification.EXTRA_CONVERSATION_TITLE)
        val title = value(extras, Notification.EXTRA_TITLE)
        val messaging = latestMessagingEntry(extras)
        val sender = messaging?.first.orEmpty().ifBlank { title }
        val text = messaging?.second.orEmpty()
            .ifBlank { value(extras, Notification.EXTRA_BIG_TEXT) }
            .ifBlank { value(extras, Notification.EXTRA_TEXT) }
        val chat = conversation.ifBlank { title }.substringBefore(": ").trim()

        if (chat.isBlank() || sender.isBlank() || text.isBlank()) return null
        if (conversation.isNotBlank() && sender.isNotBlank() && sender != conversation) return null // group notification
        if (text.matches(summaryPattern) || title.matches(summaryPattern)) return null

        return Message(
            phoneNumber = "",
            senderName = sender,
            messageText = text,
            timestamp = messaging?.third ?: sbn.postTime,
            conversationName = chat,
            source = "notification",
            direction = "incoming",
        )
    }

    private fun latestMessagingEntry(extras: Bundle): Triple<String, String, Long>? {
        @Suppress("DEPRECATION")
        val bundles = extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.filterIsInstance<Bundle>().orEmpty()
        val latest = bundles.maxByOrNull { it.getLong("time") } ?: return null
        val sender = latest.getCharSequence("sender")?.toString()?.trim().orEmpty()
        val text = latest.getCharSequence("text")?.toString()?.trim().orEmpty()
        return Triple(sender, text, latest.getLong("time"))
    }

    private fun value(extras: Bundle, key: String): String =
        extras.getCharSequence(key)?.toString()?.trim().orEmpty()
}
