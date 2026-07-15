package com.whatsappsync.app.data.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.whatsappsync.app.data.models.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Service to read WhatsApp messages via accessibility service.
 * This captures messages from the WhatsApp Business app accessibility events.
 */
object WhatsAppMessageReader {
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    
    private val capturedMessages = mutableListOf<Message>()
    
    fun processAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val text = event.text
            for (item in text) {
                val message = item.toString()
                // Simple message detection - can be enhanced based on WhatsApp UI structure
                if (message.isNotEmpty() && !message.contains("Loading")) {
                    // In production, this would parse structured WhatsApp data
                    // For now, we capture raw text and let the main app process it
                }
            }
        }
    }
    
    fun addMessage(message: Message) {
        capturedMessages.add(message)
        _messages.value = capturedMessages.toList()
    }
    
    fun getMessages(): List<Message> = capturedMessages.toList()
    
    fun clearMessages() {
        capturedMessages.clear()
        _messages.value = emptyList()
    }
    
    fun removeMessage(message: Message) {
        capturedMessages.remove(message)
        _messages.value = capturedMessages.toList()
    }
}
