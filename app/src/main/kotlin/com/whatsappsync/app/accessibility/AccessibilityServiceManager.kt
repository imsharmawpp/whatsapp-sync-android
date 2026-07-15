package com.whatsappsync.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.whatsappsync.app.data.service.WhatsAppMessageReader

/**
 * Manager for accessibility service lifecycle and state
 */
object AccessibilityServiceManager {
    
    private var currentService: WhatsAppAccessibilityService? = null
    
    fun onServiceConnected(service: WhatsAppAccessibilityService) {
        currentService = service
    }
    
    fun onServiceDisconnected() {
        currentService = null
    }
    
    fun isServiceRunning(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        accessibilityManager?.let {
            val services = it.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
            return services.any { service ->
                service.id.contains("WhatsAppAccessibilityService")
            }
        }
        return false
    }
    
    fun getConnectedService(): WhatsAppAccessibilityService? {
        return currentService
    }
}

/**
 * Extended accessibility service with message handling
 */
class MessageCapturingAccessibilityService : AccessibilityService() {
    
    private val TAG = "MessageCapturingService"
    
    override fun onCreate() {
        super.onCreate()
        AccessibilityServiceManager.onServiceConnected(
            WhatsAppAccessibilityService()
        )
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            if (it.packageName?.toString()?.contains("whatsapp") == true) {
                when (it.eventType) {
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                        // Message content may have changed
                        processMessageEvent(it)
                    }
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                        // Text input or message field changed
                        processTextEvent(it)
                    }
                }
            }
        }
    }
    
    override fun onInterrupt() {
        // Service was interrupted
    }
    
    private fun processMessageEvent(event: AccessibilityEvent) {
        // Extract message data from accessibility event
        val text = event.text
        for (item in text) {
            val content = item.toString().trim()
            if (content.isNotEmpty() && shouldProcessContent(content)) {
                // In production, parse the content and extract:
                // - Sender name
                // - Phone number
                // - Message text
                // - Timestamp
                // Then add to WhatsAppMessageReader
            }
        }
    }
    
    private fun processTextEvent(event: AccessibilityEvent) {
        // Handle text changed events
        val text = event.text
        for (item in text) {
            val content = item.toString().trim()
            if (content.isNotEmpty() && shouldProcessContent(content)) {
                // Process similar to processMessageEvent
            }
        }
    }
    
    private fun shouldProcessContent(content: String): Boolean {
        // Filter out non-message content
        return content.isNotEmpty() &&
               !content.contains("Loading", ignoreCase = true) &&
               !content.contains("Search", ignoreCase = true) &&
               !content.contains("Settings", ignoreCase = true) &&
               content.length > 2
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityServiceManager.onServiceConnected(
            WhatsAppAccessibilityService()
        )
    }
}
