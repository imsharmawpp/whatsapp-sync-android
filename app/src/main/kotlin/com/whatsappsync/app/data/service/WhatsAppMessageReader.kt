package com.whatsappsync.app.data.service

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.whatsappsync.app.data.models.Message
import com.whatsappsync.app.data.repository.SharedPreferencesManager
import com.whatsappsync.app.permissions.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Uses visible WhatsApp chat identity only to enrich unresolved leads; notifications supply message content. */
object WhatsAppMessageReader {
    private var appContext: Context? = null
    private var store: SharedPreferencesManager? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (store == null) store = SharedPreferencesManager(context.applicationContext)
        refresh()
    }

    fun processAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName !in PermissionManager.WHATSAPP_PACKAGES) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val visible = linkedSetOf<String>()
        collectText(event.source, visible)
        event.text.mapTo(visible) { it.toString().trim() }
        val phone = visible.firstNotNullOfOrNull(ContactResolver::normalizePhone) ?: return
        val pending = store?.getPendingMessages().orEmpty()
        val matchingChat = pending.map { it.conversationName }.firstOrNull { chat ->
            visible.any { it.equals(chat, ignoreCase = true) }
        } ?: return

        store?.savePhoneMapping(matchingChat, phone)
        val context = appContext ?: return
        scope.launch { AutomaticLeadSync(context).retryPending() }
    }

    fun addMessage(message: Message) {
        store?.addPendingMessage(message)
        refresh()
    }

    fun getMessages(): List<Message> = store?.getPendingMessages().orEmpty()

    fun removeMessages(ids: Set<String>) {
        store?.removePendingMessages(ids)
        refresh()
    }

    private fun collectText(node: AccessibilityNodeInfo?, destination: MutableSet<String>) {
        node ?: return
        node.text?.toString()?.trim()?.takeIf { it.isUseful() }?.let(destination::add)
        node.contentDescription?.toString()?.trim()?.takeIf { it.isUseful() }?.let(destination::add)
        repeat(node.childCount) { collectText(node.getChild(it), destination) }
    }

    private fun String.isUseful(): Boolean = isNotBlank() && length <= 256

    private fun refresh() {
        _messages.value = store?.getPendingMessages().orEmpty()
    }
}
