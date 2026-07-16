# WhatsApp Sync Android App - Complete Specification

**Project Name:** WhatsApp Sync  
**Application ID:** com.whatsappsync.app  
**Version:** 1.0.0  
**Min SDK:** 26 (Android 8.0)  
**Target SDK:** 35 (Android 15)  
**Compiled SDK:** 35

---

## 📋 Project Overview

WhatsApp Sync is an Android application that automatically captures WhatsApp Business messages and synchronizes them to a Google Sheet in real-time. The app uses accessibility services to monitor incoming/outgoing messages and uploads them to Google Sheets via REST API integration.

**Key Features:**
- Real-time message capture from WhatsApp Business
- Automatic Google Sheets synchronization
- OAuth 2.0 authentication with Google
- Accessibility service integration
- Contact synchronization
- Settings management for Google Sheets configuration
- Permission handling for Android 8.0+

---

## 🏗️ Architecture & Tech Stack

### Build Configuration
- **Build Tool:** Gradle 8.2.0
- **Language:** Kotlin 1.9.24
- **UI Framework:** Jetpack Compose (latest stable)
- **Compose BOM:** 2024.06.00
- **Serialization:** kotlinx-serialization-json 1.6.3

### Core Dependencies
```kotlin
// AndroidX Core
androidx-core-ktx = 1.13.1
androidx-lifecycle-runtime-ktx = 2.8.4
androidx-activity-compose = 1.9.1
androidx-navigation-compose = 2.7.7

// Compose UI
compose-bom = 2024.06.00 (handles all Compose versions)
compose-ui
compose-material3
compose-foundation

// Networking
okhttp = 4.12.0

// Serialization
kotlinx-serialization-json = 1.6.3
```

### Architecture Pattern
- **MVVM** (Model-View-ViewModel) with Jetpack Compose
- **Navigation:** Jetpack Navigation Compose
- **State Management:** ViewModel + State
- **Data Persistence:** SharedPreferences (Android native)
- **External APIs:** Google Sheets REST API v4, Google OAuth 2.0

---

## 📁 Project Structure

```
whatsapp-sync-android/
├── app/
│   └── src/main/
│       ├── kotlin/com/whatsappsync/app/
│       │   ├── MainActivity.kt                          # Entry point, Navigation setup
│       │   ├── accessibility/
│       │   │   ├── WhatsAppAccessibilityService.kt     # Captures WhatsApp messages
│       │   │   └── AccessibilityServiceManager.kt      # Service management
│       │   ├── config/
│       │   │   └── GoogleOAuthConfig.kt                # OAuth configuration
│       │   ├── data/
│       │   │   ├── models/
│       │   │   │   ├── Message.kt                      # Message data class
│       │   │   │   └── GoogleSheetConfig.kt            # Sheet configuration
│       │   │   ├── repository/
│       │   │   │   └── SharedPreferencesManager.kt     # Local storage
│       │   │   └── service/
│       │   │       ├── GoogleSheetsClient.kt           # REST API client
│       │   │       └── WhatsAppMessageReader.kt        # Message extraction
│       │   ├── permissions/
│       │   │   ├── PermissionManager.kt                # Runtime permissions
│       │   │   └── PermissionHandler.kt                # Permission UI logic
│       │   └── ui/
│       │       ├── navigation/
│       │       │   └── AppNavigation.kt                # Navigation graph
│       │       ├── screens/
│       │       │   ├── home/HomeScreen.kt              # Main home page
│       │       │   ├── permissions/PermissionsScreen.kt # Permission requests
│       │       │   ├── settings/SettingsScreen.kt      # Settings & Google config
│       │       │   └── sync/SyncScreen.kt              # Sync status & logs
│       │       └── theme/
│       │           ├── Color.kt                        # Color scheme
│       │           ├── Theme.kt                        # Compose theme
│       │           └── Type.kt                         # Typography
│       └── res/
│           ├── values/
│           │   ├── strings.xml                         # String resources
│           │   ├── colors.xml                          # Color resources
│           │   └── themes.xml                          # Material themes
│           ├── mipmap-*/
│           │   └── ic_launcher.png                     # App icons (all densities)
│           └── xml/
│               ├── accessibility_service_config.xml    # Accessibility config
│               ├── backup_schemes.xml                  # Backup config
│               └── data_extraction_rules.xml           # Data extraction rules
├── gradle/
│   └── libs.versions.toml                              # Dependency version catalog
├── gradle.properties                                   # Gradle configuration
├── AndroidManifest.xml                                 # App manifest (see below)
└── build.gradle.kts                                    # App build configuration
```

---

## 🔐 Android Manifest Configuration

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Required Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_CONTACTS" />

    <!-- Query WhatsApp Business app -->
    <queries>
        <package android:name="com.whatsapp.w4b" />
    </queries>

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_schemes"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.WhatsAppSync"
        android:usesCleartextTraffic="false">

        <!-- Main Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.WhatsAppSync">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Accessibility Service for message capture -->
        <service
            android:name=".accessibility.WhatsAppAccessibilityService"
            android:enabled="true"
            android:exported="false"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

    </application>

</manifest>
```

---

## 🔑 Key Components & Data Flow

### 1. Message Capture (Accessibility Service)
**File:** `accessibility/WhatsAppAccessibilityService.kt`

Monitors WhatsApp Business app for:
- Incoming messages
- Outgoing messages
- Message metadata (timestamp, sender, content)

**Flow:**
```
WhatsApp → AccessibilityService → Message extraction → Local storage
```

### 2. Google Sheets Integration
**File:** `data/service/GoogleSheetsClient.kt`

**REST API Usage:**
```
Endpoint: https://sheets.googleapis.com/v4/spreadsheets/{spreadsheetId}/values/{range}:append
Method: POST
Auth: Bearer {accessToken}
Content-Type: application/json
```

**Data Format:**
```json
{
  "values": [
    ["2024-07-16 14:30:00", "+1234567890", "John Doe", "Hello", "2024-07-16"]
  ]
}
```

### 3. Data Models

**Message.kt**
```kotlin
data class Message(
    val uniqueId: String,           // Unique identifier
    val phoneNumber: String,        // Contact number
    val senderName: String,         // Contact name
    val messageText: String,        // Message content
    val timestamp: Long             // Unix timestamp
)
```

**GoogleSheetConfig.kt**
```kotlin
data class GoogleSheetConfig(
    val spreadsheetId: String,      // Google Sheet ID
    val accessToken: String,        // OAuth access token
    val refreshToken: String,       // OAuth refresh token
    val lastSyncTime: Long          // Last sync timestamp
)
```

### 4. Local Storage (SharedPreferences)
**File:** `data/repository/SharedPreferencesManager.kt`

**Keys Stored:**
- `google_access_token`: OAuth access token
- `google_refresh_token`: OAuth refresh token
- `spreadsheet_id`: Google Sheet ID
- `synced_message_ids`: Set of synced message IDs (deduplication)
- `last_sync_time`: Last successful sync timestamp

---

## 🔐 Google OAuth 2.0 Integration

**File:** `config/GoogleOAuthConfig.kt`

### OAuth Flow:
1. User clicks "Connect to Google" in Settings
2. App opens browser with Google OAuth consent screen
3. User authorizes app
4. OAuth code returned to app via deep link
5. App exchanges code for access + refresh tokens
6. Tokens stored securely in SharedPreferences

### Required Google Cloud Setup:
```
Client ID: [Generated in Google Cloud Console]
Client Secret: [Generated in Google Cloud Console]
Redirect URI: com.whatsappsync.app://callback
Scopes: https://www.googleapis.com/auth/spreadsheets
```

---

## 🧭 Navigation Flow

**File:** `ui/navigation/AppNavigation.kt`

```
MainActivity
├── HomeScreen (Initial route)
│   └── Options: Settings, Sync, Permissions
├── PermissionsScreen
│   └── Request: Accessibility, Contacts, Notifications
├── SettingsScreen
│   ├── Connect to Google (OAuth flow)
│   └── Enter Google Sheet ID
└── SyncScreen
    ├── Manual sync button
    └── Sync logs/status
```

---

## 🎨 UI Components (Jetpack Compose)

### Screens:
1. **HomeScreen** - Dashboard with quick actions
2. **SettingsScreen** - Google authentication & Sheet configuration
3. **PermissionsScreen** - Runtime permission requests
4. **SyncScreen** - Sync status, logs, manual sync trigger

### Theme:
- **Color Scheme:** Material 3 (Light/Dark support)
- **Typography:** Roboto font
- **Component Library:** Material3 Compose

---

## ⚙️ Gradle Build Configuration

### Android Configuration:
```kotlin
android {
    namespace = "com.whatsappsync.app"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "com.whatsappsync.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
}
```

### Compose Configuration:
```kotlin
buildFeatures {
    compose = true
}
composeOptions {
    kotlinCompilerExtensionVersion = "1.5.11"
}
```

### Gradle Properties (gradle.properties):
```properties
android.useAndroidX=true
android.enableJetifier=true
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
kotlin.code.style=official
```

---

## 🚀 Building the APK

### Prerequisites:
1. Android SDK 35 installed
2. Gradle 8.2.0 or higher
3. Java 8+ (OpenJDK 8)
4. Git for version control

### Build Steps:
```bash
# Clone repository
git clone https://github.com/imsharmawpp/whatsapp-sync-android.git
cd whatsapp-sync-android

# Sync Gradle
./gradlew assemble

# Build Debug APK
./gradlew assembleDebug

# Output location:
# app/build/outputs/apk/debug/app-debug.apk
```

### Build APK in Android Studio:
1. Open project
2. File → Sync with Gradle Files
3. Build → Generate APK(s)
4. APK located in: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📦 Installation

### On Android Device:
```bash
# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Or manually transfer APK and tap to install
```

### First Launch Setup:
1. Grant Accessibility Service permission
2. Grant Contact reading permission
3. Tap "Settings" → "Connect to Google"
4. Authorize with Google account
5. Enter Google Sheet ID
6. Go to "Sync" → "Sync Now"

---

## 🔄 Sync Process

```
1. User taps "Sync Now" in SyncScreen
   ↓
2. GoogleSheetsClient reads stored accessToken & spreadsheetId
   ↓
3. WhatsAppMessageReader extracts unsynced messages
   ↓
4. Filter out already-synced messages (by uniqueId)
   ↓
5. Format messages: [Timestamp, PhoneNumber, SenderName, MessageText, Date]
   ↓
6. POST to Google Sheets API:
   https://sheets.googleapis.com/v4/spreadsheets/{id}/values/Sheet1!A:E:append
   ↓
7. Update SharedPreferences with synced message IDs
   ↓
8. Update lastSyncTime
   ↓
9. Display success message with count of synced messages
```

---

## 🛡️ Security & Permissions

### Required Permissions:
- `INTERNET` - API calls to Google Sheets
- `READ_CONTACTS` - Extract contact names
- `BIND_ACCESSIBILITY_SERVICE` - Capture messages

### Security Practices:
- OAuth tokens stored in SharedPreferences (on Android 12+, use EncryptedSharedPreferences)
- HTTPS-only for API calls (`usesCleartextTraffic="false"`)
- No hardcoded credentials
- Accessibility service runs with minimal permissions
- Message IDs deduplicated to prevent duplicates

---

## 📊 Google Sheets Column Structure

The app appends data to Google Sheet with these columns:

| Column | Format | Example |
|--------|--------|---------|
| A | Timestamp | 2024-07-16 14:30:00 |
| B | Phone Number | +1234567890 |
| C | Sender Name | John Doe |
| D | Message Text | Hello, how are you? |
| E | Sync Date | 2024-07-16 |

---

## 🐛 Troubleshooting & Common Issues

### Issue: APK Build Fails
**Solution:**
- Ensure `gradle.properties` has AndroidX enabled:
  ```properties
  android.useAndroidX=true
  android.enableJetifier=true
  ```
- Run: `./gradlew clean assembleDebug`

### Issue: Messages Not Syncing
**Solution:**
- Check Accessibility Service is enabled in Settings
- Verify Google Sheet ID is correct
- Check OAuth token validity
- Ensure internet connection active

### Issue: "Resource linking failed"
**Solution:**
- Icon files must exist in `res/mipmap-{density}/`
- Densities: mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi
- Theme resource must be defined in `res/values/themes.xml`

---

## 📋 Dependency Version Catalog (gradle/libs.versions.toml)

```toml
[versions]
agp = "8.2.0"
kotlin = "1.9.24"
kotlin-serialization = "1.6.3"
androidx-core = "1.13.1"
androidx-lifecycle = "2.8.4"
androidx-activityCompose = "1.9.1"
compose-bom = "2024.06.00"
androidx-navigation = "2.7.7"
okhttp = "4.12.0"

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

---

## 🎯 Future Enhancements

- Multi-language support
- Message encryption in transit
- Backup to cloud
- Scheduled automatic sync
- Message filtering by sender
- Database integration (Room)
- Unit & Integration tests
- Release APK signing

---

## 📝 Notes for AI Studio

- **Repository URL:** https://github.com/imsharmawpp/whatsapp-sync-android.git
- **Main Branch:** main
- **Language:** 100% Kotlin
- **UI Framework:** Jetpack Compose (no XML layouts except themes)
- **API Integration:** Google Sheets v4 REST API
- **Authentication:** OAuth 2.0 with Google
- **Target Users:** WhatsApp Business users wanting to log messages to Google Sheets

---

**Generated:** 2024-07-16  
**Version:** 1.0.0  
**Status:** Production-Ready APK Build Configured
