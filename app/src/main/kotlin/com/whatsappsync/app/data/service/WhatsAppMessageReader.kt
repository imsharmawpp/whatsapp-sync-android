package com.whatsappsync.app.data.service

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import com.whatsappsync.app.data.models.Message
import com.whatsappsync.app.data.repository.SharedPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Best-effort capture of future WhatsApp Business accessibility events. */
object WhatsAppMessageReader {
    private var store: SharedPreferencesManager? = null
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    fun initialize(context: Context) {
        if (store == null) store = SharedPreferencesManager(context.applicationContext)
        refresh()
    }

    fun processAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName?.toString() != WHATSAPP_BUSINESS_PACKAGE) return
        if (event.eventType !in SUPPORTED_EVENT_TYPES) return

        val rawParts = buildList {
            event.text.mapTo(this) { it.toString().trim() }
            event.contentDescription?.toString()?.trim()?.let(::add)
        }.filter { it.isUsefulWhatsAppText() }.distinct()

        if (rawParts.isEmpty()) return
        val source = event.source
        val conversation = source?.packageName?.toString().orEmpty()
            .takeUnless { it == WHATSAPP_BUSINESS_PACKAGE }
            ?: "WhatsApp Business"
        val sender = rawParts.first()
        val body = rawParts.drop(1).joinToString(" ").ifBlank { rawParts.first() }

        addMessage(
            Message(
                phoneNumber = sender.takeIf { PHONE_PATTERN.containsMatchIn(it) }.orEmpty(),
                senderName = sender,
                messageText = body,
                timestamp = event.eventTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
                conversationName = conversation
            )
        )
    }

    fun addMessage(message: Message) {
        val storage = store ?: return
        storage.addPendingMessage(message)
        refresh()
    }

    fun getMessages(): List<Message> = store?.getPendingMessages().orEmpty()

    fun removeMessages(ids: Set<String>) {
        store?.removePendingMessages(ids)
        refresh()
    }

    private fun refresh() {
        _messages.value = store?.getPendingMessages().orEmpty()
    }

    private fun String.isUsefulWhatsAppText(): Boolean {
        if (isBlank() || length > 8_000) return false
        val normalized = trim().lowercase()
        return normalized !in IGNORED_TEXT && !normalized.startsWith("loading")
    }

    private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    private val PHONE_PATTERN = Regex("\\+?[0-9][0-9 ()-]{6,}")
    private val SUPPORTED_EVENT_TYPES = setOf(
        AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED,
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
    )
    private val IGNORED_TEXT = setOf(
        "whatsapp business", "search", "new chat", "archived", "chats", "updates", "calls"
    )
}
