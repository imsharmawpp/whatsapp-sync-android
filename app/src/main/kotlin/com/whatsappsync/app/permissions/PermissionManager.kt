package com.whatsappsync.app.permissions

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat

/**
 * Manager for checking and requesting various app permissions
 */
class PermissionManager(private val context: Context) {
    
    /**
     * Check if all required permissions are granted
     */
    fun allPermissionsGranted(): Boolean {
        return contactsPermissionGranted() && 
               accessibilityServiceEnabled()
    }
    
    /**
     * Check if contacts permission is granted
     */
    fun contactsPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if accessibility service is enabled for our app
     */
    fun accessibilityServiceEnabled(): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        accessibilityManager?.let {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            
            val serviceName = "com.whatsappsync.app/.accessibility.WhatsAppAccessibilityService"
            return enabledServices.contains(serviceName)
        }
        return false
    }
    
    /**
     * Check if WhatsApp Business app is installed
     */
    fun whatsAppBusinessInstalled(): Boolean {
        return isPackageInstalled("com.whatsapp.w4b")
    }
    
    /**
     * Check if regular WhatsApp app is installed (alternative)
     */
    fun regularWhatsAppInstalled(): Boolean {
        return isPackageInstalled("com.whatsapp")
    }
    
    /**
     * Check if any WhatsApp version is installed
     */
    fun whatsAppInstalled(): Boolean {
        return whatsAppBusinessInstalled() || regularWhatsAppInstalled()
    }
    
    /**
     * Get list of permissions that need to be requested
     */
    fun getMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()
        
        if (!contactsPermissionGranted()) {
            missing.add(Manifest.permission.READ_CONTACTS)
        }
        
        return missing
    }
    
    /**
     * Get permission description for UI display
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_CONTACTS -> "Access contacts to enrich message data with contact names"
            Manifest.permission.INTERNET -> "Internet access to sync data to Google Sheets"
            else -> "This permission is required for the app to function"
        }
    }
    
    /**
     * Helper function to check if a package is installed
     */
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

/**
 * Data class representing permission status
 */
data class PermissionStatus(
    val contactsPermission: Boolean,
    val accessibilityService: Boolean,
    val whatsAppInstalled: Boolean,
    val allGranted: Boolean
)
