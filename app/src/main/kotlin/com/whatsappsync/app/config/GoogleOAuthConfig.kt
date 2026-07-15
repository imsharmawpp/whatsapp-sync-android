package com.whatsappsync.app.config

/**
 * Google OAuth Configuration
 * 
 * TODO: Replace these values with your actual Google Cloud credentials
 */
object GoogleOAuthConfig {
    
    /**
     * Your OAuth 2.0 Client ID from Google Cloud Console
     * Format: xxxxx-xxxxxxx.apps.googleusercontent.com
     * 
     * Get this from:
     * 1. Go to https://console.cloud.google.com
     * 2. Select your project
     * 3. APIs & Services → Credentials
     * 4. Click on your Android OAuth 2.0 Client ID
     * 5. Copy the Client ID value
     */
    const val GOOGLE_OAUTH_CLIENT_ID = "613814281399-v1r1k4uoggvdrrup7egtng71r02s1jrd.apps.googleusercontent.com"
    
    /**
     * Google Sheets API Scopes
     * This allows the app to read and write to Google Sheets
     */
    val SCOPES = listOf(
        "https://www.googleapis.com/auth/spreadsheets"
    )
    
    /**
     * Your default Google Sheet ID (optional - can be set in settings later)
     * You can leave this empty and let users enter it in the app settings
     * 
     * To get your Sheet ID:
     * 1. Open your Google Sheet
     * 2. Copy the ID from the URL: docs.google.com/spreadsheets/d/[SHEET_ID]/edit
     */
    const val DEFAULT_SHEET_ID = "i0qFk_OpEP_foWfyt8F11IM340h5Fu7Aqo4rUJHqmaM8"
    
    /**
     * WhatsApp Business Package Name
     * This is the package name of the WhatsApp Business app on your device
     */
    const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
}
