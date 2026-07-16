package com.whatsappsync.app.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.whatsappsync.app.data.models.Message
import com.whatsappsync.app.data.repository.SharedPreferencesManager

class WhatsAppNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.packageName != WHATSAPP_BUSINESS_PACKAGE) return
        NotificationMessageParser.parse(notification)?.let {
            SharedPreferencesManager(applicationContext).addPendingMessage(it)
        }
    }

    companion object { const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b" }
}

object NotificationMessageParser {
    fun parse(sbn: StatusBarNotification): Message? {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: return null
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: return null
        if (title.isBlank() || text.isBlank() || text.matches(Regex("\\d+ new messages?", RegexOption.IGNORE_CASE))) return null
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.ifBlank { title } ?: title
        return Message(
            phoneNumber = "",
            senderName = sender,
            messageText = text,
            timestamp = sbn.postTime,
            conversationName = title,
            source = "notification",
            direction = "incoming"
        )
    }
}
