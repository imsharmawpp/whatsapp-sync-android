# WhatsApp Sync - Android App Setup Guide

This is a complete Android app for syncing WhatsApp Business messages to Google Sheets.

## Prerequisites

1. **Android Studio** (latest version) - [Download](https://developer.android.com/studio)
2. **Android SDK 26+** 
3. **Java/Kotlin** (included with Android Studio)
4. **Google Cloud Project** with Google Sheets API enabled
5. **WhatsApp Business app** installed on your test device/emulator

---

## Step 1: Setup in Android Studio

1. **Clone/Open this project** in Android Studio
2. **Wait for Gradle sync** to complete
3. **Connect an Android device** or start an emulator (API 26+)
4. **Get your SHA-1 certificate fingerprint**:
   - In Android Studio: `Build` → `Generate Signed Bundle/APK`
   - Select "Debug" configuration
   - Copy the SHA-1 value from the dialog
   - Or run: `./gradlew signingReport` in terminal

---

## Step 2: Google Cloud Configuration

### 2a. Enable Google Sheets API
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your project
3. Search for "Google Sheets API"
4. Click **Enable**

### 2b. Create OAuth 2.0 Credentials
1. Go to **APIs & Services** → **Credentials**
2. Click **"+ Create Credentials"** → **"OAuth 2.0 Client ID"**
3. First time? Click **"Configure Consent Screen"**:
   - Choose **External** user type
   - Fill in:
     - App name: `WhatsApp Sync`
     - User support email: `your-email@gmail.com`
   - Click **Scopes** → Search `sheets` → Check `https://www.googleapis.com/auth/spreadsheets`
   - Save, then go back to Credentials

4. Click **Create Credentials** → **OAuth 2.0 Client ID** again
5. Select **Android** as application type
6. Fill in:
   - Package name: `com.whatsappsync.app`
   - SHA-1 certificate fingerprint: (paste your SHA-1 from Step 1)
   - Click **Create**
7. **Save** the Client ID (you'll need it later)

### 2c. Create Google Sheet
1. Go to [Google Sheets](https://sheets.google.com)
2. Create a new sheet named "WhatsApp Leads"
3. Add columns (in row 1): 
   - A: `Timestamp`
   - B: `Phone Number`
   - C: `Customer Name`
   - D: `Message`
   - E: `Synced Date`
4. Copy the Sheet ID from the URL: `docs.google.com/spreadsheets/d/[THIS_IS_THE_ID]/edit`

---

## Step 3: Configure the App

### 3a. Add Google OAuth Credentials
1. In Android Studio, open: `app/src/main/kotlin/com/whatsappsync/app/`
2. Create a new file: `GoogleAuthConfig.kt`
3. Add your OAuth Client ID:
```kotlin
package com.whatsappsync.app

object GoogleAuthConfig {
    const val CLIENT_ID = "YOUR_OAUTH_CLIENT_ID_HERE"
    const val SCOPES = "https://www.googleapis.com/auth/spreadsheets"
}
```

### 3b. Add Your Spreadsheet ID
When the app runs, you'll be prompted to enter your Spreadsheet ID (from Step 2c).

---

## Step 4: Install Required Dependencies

All dependencies are defined in `gradle/libs.versions.toml`. When you first open the project, Android Studio will automatically download them. If not:

1. Click **Sync Now** in Android Studio
2. Or run: `./gradlew build` in terminal

**Key dependencies**:
- `androidx-compose` - UI framework
- `google-auth-client` - OAuth authentication
- `google-sheets-api` - Google Sheets API client
- `androidx-navigation` - App navigation

---

## Step 5: Build & Run

1. **Connect Android device or start emulator**
2. **Click "Run"** (green play button) in Android Studio
3. **Select device** if prompted
4. App will build and install

On first launch:
1. Grant accessibility permission (needed to read WhatsApp messages)
2. Grant contacts permission (optional, for better data enrichment)
3. Connect to Google Sheets (OAuth login)
4. Enter your Spreadsheet ID
5. Done! You can now sync messages

---

## Step 6: Using the App

### Home Screen
- Shows total messages synced today
- Shows last sync time
- Quick "Sync Now" button

### Sync Screen
1. Tap "Sync Now"
2. App queries WhatsApp Business for recent messages
3. Deduplicates messages (avoids duplicates)
4. Appends new messages to Google Sheet
5. Shows success/error message

### Settings Screen
- Change Google Sheets connection
- View app version
- Disconnect Google account

---

## Troubleshooting

### "WhatsApp Business app not detected"
- Ensure WhatsApp Business app is installed on device
- Try restarting the accessibility service

### "OAuth login fails"
- Verify your Client ID is correct in `GoogleAuthConfig.kt`
- Check that your SHA-1 is registered in Google Cloud
- Ensure Google Sheets API is enabled in Google Cloud Console

### "Messages not syncing"
- Enable accessibility service: Settings → Accessibility → WhatsApp Sync
- Ensure contacts permission is granted
- Check internet connection
- Verify Spreadsheet ID is correct in Settings

### "No permission to access spreadsheet"
- Share your Google Sheet with the same Google account used for OAuth
- Or, sign in with the account that owns the sheet

---

## Project Structure

```
app/
├── src/main/kotlin/com/whatsappsync/app/
│   ├── MainActivity.kt                 # App entry point
│   ├── GoogleAuthConfig.kt             # OAuth configuration
│   ├── accessibility/
│   │   └── WhatsAppAccessibilityService.kt
│   ├── data/
│   │   ├── models/                     # Data classes
│   │   ├── repository/                 # Data persistence
│   │   └── service/                    # API clients & business logic
│   └── ui/
│       ├── navigation/                 # Navigation graph
│       ├── screens/                    # UI screens
│       └── theme/                      # Theming
├── AndroidManifest.xml                 # App permissions & services
└── res/
    ├── values/                         # Strings, colors, themes
    └── xml/                            # Accessibility service config
```

---

## Security Notes

- **Tokens are encrypted** using Android Keystore
- **No data stored on servers** - everything stays on your device
- **OAuth credentials are device-locked** to your app's SHA-1
- **SharedPreferences are encrypted** with master key

---

## Next Steps

Once the app is working:

1. **Install on your phone** via USB cable or Google Play Store (after publishing)
2. **Enable accessibility service** in device settings
3. **Grant permissions** when prompted
4. **Start syncing** WhatsApp messages to your Google Sheet

---

## Support

If you encounter issues:
1. Check the troubleshooting section above
2. Verify all permissions are granted in device settings
3. Ensure Google Cloud configuration matches the guide
4. Check logcat output in Android Studio for error details

---

## License

This project is provided as-is for your personal/business use.
