package com.whatsappsync.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.whatsappsync.app.data.service.WhatsAppMessageReader

/**
 * Accessibility service that monitors WhatsApp for incoming messages.
 * This service captures accessibility events from WhatsApp Business app.
 */
class WhatsAppAccessibilityService : AccessibilityService() {
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName.toString().contains("whatsapp")) {
            WhatsAppMessageReader.processAccessibilityEvent(event!!)
        }
    }
    
    override fun onInterrupt() {
        // Called when service is interrupted
    }
    
    override fun onCreate() {
        super.onCreate()
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
    }
}
