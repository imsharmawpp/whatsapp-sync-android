package com.whatsappsync.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.whatsappsync.app.data.service.WhatsAppMessageReader
import com.whatsappsync.app.permissions.PermissionManager

/** Monitors accessibility events from both supported WhatsApp packages. */
class WhatsAppAccessibilityService : AccessibilityService() {

    override fun onCreate() {
        super.onCreate()
        WhatsAppMessageReader.initialize(applicationContext)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityServiceManager.onServiceConnected(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName in PermissionManager.WHATSAPP_PACKAGES) {
            WhatsAppMessageReader.processAccessibilityEvent(event)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        AccessibilityServiceManager.onServiceDisconnected(this)
        super.onDestroy()
    }
}
