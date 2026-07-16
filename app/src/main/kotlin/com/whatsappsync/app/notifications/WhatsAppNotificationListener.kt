package com.whatsappsync.app.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.whatsappsync.app.data.models.Message
import com.whatsappsync.app.data.repository.SharedPreferencesManager
import com.whatsappsync.app.permissions.PermissionManager

class WhatsAppNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.packageName !in PermissionManager.WHATSAPP_PACKAGES) return

        val store = SharedPreferencesManager(applicationContext)
        NotificationMessageParser.parse(notification)?.let { message ->
            val mappedPhone = store.getPhoneMapping(message.conversationName)
            store.addPendingMessage(message.copy(phoneNumber = mappedPhone))
        }
    }
}

object NotificationMessageParser {
    private val summaryPattern = Regex("\\d+ new messages?", RegexOption.IGNORE_CASE)

    fun parse(sbn: StatusBarNotification): Message? {
        val extras = sbn.notification.extras
        val conversation = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?.toString()
            ?.trim()
            .orEmpty()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()
            ?.trim()
            .orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?.toString()
            ?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()

        val chat = conversation.ifBlank { title }
        if (chat.isBlank() || text.isBlank() || text.matches(summaryPattern)) return null

        return Message(
            phoneNumber = "",
            senderName = title.ifBlank { chat },
            messageText = text,
            timestamp = sbn.postTime,
            conversationName = chat,
            source = "notification",
            direction = "incoming",
        )
    }
}
