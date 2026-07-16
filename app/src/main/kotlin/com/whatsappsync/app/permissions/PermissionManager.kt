package com.whatsappsync.app.permissions

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.whatsappsync.app.accessibility.WhatsAppAccessibilityService
import com.whatsappsync.app.notifications.WhatsAppNotificationListener

/** Checks the live Android permission and service state used by capture setup. */
class PermissionManager(private val context: Context) {

    fun allPermissionsGranted(): Boolean =
        contactsPermissionGranted() &&
            accessibilityServiceEnabled() &&
            notificationListenerEnabled()

    fun contactsPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun accessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(context, WhatsAppAccessibilityService::class.java)
        return enabledComponents(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).any {
            it == expected
        }
    }

    fun notificationListenerEnabled(): Boolean {
        val expected = ComponentName(context, WhatsAppNotificationListener::class.java)
        return enabledComponents(Settings.Secure.ENABLED_NOTIFICATION_LISTENERS).any {
            it == expected
        }
    }

    fun whatsAppBusinessInstalled(): Boolean = isPackageInstalled(WHATSAPP_BUSINESS_PACKAGE)

    fun regularWhatsAppInstalled(): Boolean = isPackageInstalled(WHATSAPP_PACKAGE)

    fun whatsAppInstalled(): Boolean = whatsAppBusinessInstalled() || regularWhatsAppInstalled()

    fun getMissingPermissions(): List<String> = buildList {
        if (!contactsPermissionGranted()) add(Manifest.permission.READ_CONTACTS)
    }

    private fun enabledComponents(setting: String): List<ComponentName> =
        Settings.Secure.getString(context.contentResolver, setting)
            .orEmpty()
            .split(':')
            .mapNotNull(ComponentName::unflattenFromString)

    private fun isPackageInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    companion object {
        const val WHATSAPP_PACKAGE = "com.whatsapp"
        const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
        val WHATSAPP_PACKAGES = setOf(WHATSAPP_PACKAGE, WHATSAPP_BUSINESS_PACKAGE)
    }
}

data class PermissionStatus(
    val contactsPermission: Boolean,
    val accessibilityService: Boolean,
    val notificationListener: Boolean,
    val whatsAppInstalled: Boolean,
) {
    val allGranted: Boolean
        get() = contactsPermission && accessibilityService && notificationListener
}
