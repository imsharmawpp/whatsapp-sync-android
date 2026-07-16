package com.whatsappsync.app.accessibility

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/** Tracks the connected service instance and can query its persisted Android state. */
object AccessibilityServiceManager {
    private var currentService: WhatsAppAccessibilityService? = null

    fun onServiceConnected(service: WhatsAppAccessibilityService) {
        currentService = service
    }

    fun onServiceDisconnected(service: WhatsAppAccessibilityService) {
        if (currentService === service) currentService = null
    }

    fun isServiceRunning(context: Context): Boolean {
        val expected = ComponentName(context, WhatsAppAccessibilityService::class.java)
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
            .split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == expected }
    }

    fun getConnectedService(): WhatsAppAccessibilityService? = currentService
}
