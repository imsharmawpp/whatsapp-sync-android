# WhatsApp Sync - Quick Start Checklist

Get your WhatsApp Sync app running in 15 minutes!

## ✅ Pre-Flight Checklist (Before You Start)

- [ ] Android Studio installed (latest version)
- [ ] Android SDK 26+ installed
- [ ] Project downloaded/cloned
- [ ] Test device/emulator ready (Android 8.0+)
- [ ] Google account ready for OAuth

---

## ✅ Google Cloud Setup (5 minutes)

### Create Google Project
- [ ] Visit https://console.cloud.google.com
- [ ] Create new project (or select existing)
- [ ] Name it "WhatsApp Sync"

### Enable Google Sheets API
- [ ] Search for "Google Sheets API"
- [ ] Click "Enable"

### Create OAuth Credentials
- [ ] Go to APIs & Services > Credentials
- [ ] Click "+ Create Credentials"
- [ ] Configure OAuth Consent Screen:
  - [ ] Choose "External" user type
  - [ ] Fill app name: "WhatsApp Sync"
  - [ ] Add email for support
  - [ ] Click "Scopes" → Search "sheets" → Select `https://www.googleapis.com/auth/spreadsheets`
  - [ ] Save

### Create Android OAuth Credential
- [ ] Back to Credentials, click Create Credentials again
- [ ] Select "OAuth 2.0 Client ID"
- [ ] Choose "Android"
- [ ] Fill in:
  - Package name: `com.whatsappsync.app`
  - [ ] Get SHA-1 from Android Studio (see note below)
- [ ] Click "Create"
- [ ] **Copy your Client ID** (you'll need this)

**Getting SHA-1:**
1. Open Android Studio
2. Build menu → Generate Signed Bundle/APK
3. Select "Debug" configuration
4. The SHA-1 will be shown in the dialog
5. Or run: `./gradlew signingReport` in terminal

### Create Google Sheet
- [ ] Go to https://sheets.google.com
- [ ] Create new sheet: "WhatsApp Leads"
- [ ] Add column headers (Row 1):
  - A1: `Timestamp`
  - B1: `Phone Number`
  - C1: `Customer Name`
  - D1: `Message`
  - E1: `Synced Date`
- [ ] **Copy Spreadsheet ID** from URL: `docs.google.com/spreadsheets/d/[THIS_ID]/edit`

---

## ✅ Android Studio Setup (3 minutes)

### Open Project
- [ ] File → Open → Select project folder
- [ ] Wait for Gradle sync (2-3 minutes)
- [ ] Click "Sync Now" if prompted

### Add OAuth Client ID
1. Open: `app/src/main/kotlin/com/whatsappsync/app/`
2. Create new file: `GoogleAuthConfig.kt`
3. Paste this code:
```kotlin
package com.whatsappsync.app

object GoogleAuthConfig {
    const val CLIENT_ID = "YOUR_OAUTH_CLIENT_ID_HERE"
    const val SCOPES = "https://www.googleapis.com/auth/spreadsheets"
}
```
4. Replace `YOUR_OAUTH_CLIENT_ID_HERE` with your actual Client ID from Google Cloud

---

## ✅ Build & Deploy (5 minutes)

### Choose Your Target
- [ ] Connect Android device via USB, OR
- [ ] Open Device Manager and create/start emulator

### Deploy App
- [ ] Click "Run" (green play button) in Android Studio
- [ ] Select device if prompted
- [ ] Wait for build to complete
- [ ] App should launch on device

### First Launch
- [ ] Grant accessibility permission when prompted
- [ ] Grant contacts permission when prompted
- [ ] Tap "Connect to Google Sheets"
- [ ] Sign in with your Google account
- [ ] Enter your Spreadsheet ID in Settings
- [ ] Done! Ready to sync

---

## ✅ Test the App

### Test on Home Screen
- [ ] See "Dashboard" with 0 messages synced
- [ ] See "Last sync: Never"

### Test Sync Flow
1. Send a test message on WhatsApp Business
2. Tap "Sync Now" button in app
3. Wait for sync to complete
4. Check Google Sheet - message should appear!

### Test Deduplication
1. Tap "Sync Now" again WITHOUT sending a new message
2. Sheet should NOT get duplicate entries
3. App should show "Synced: 0 messages"

---

## ✅ Troubleshooting

| Issue | Solution |
|-------|----------|
| Gradle sync fails | Ensure Android SDK 26+ is installed via SDK Manager |
| Build error about Compose | Check Kotlin version matches in `gradle/libs.versions.toml` |
| App crashes on launch | Check AndroidManifest.xml for permission declarations |
| OAuth login fails | Verify Client ID is correct, check SHA-1 matches Google Cloud |
| No messages synced | Enable accessibility service in device Settings > Accessibility |
| Sheet not found | Verify Spreadsheet ID is correct in app Settings |

---

## 📋 Next Steps (When Working)

1. **Send real messages** on WhatsApp Business and sync them
2. **Customize columns** in Google Sheet as needed
3. **Test auto-deduplication** by syncing same messages twice
4. **Share sheet** with sales team to start calling leads
5. **Add more features** (auto-sync, filtering, etc.)

---

## 🚀 You're Ready!

Your WhatsApp Sync app is now set up and ready to use. Here's what it does:

1. **One-tap sync** of WhatsApp messages to Google Sheets
2. **Automatic deduplication** (no duplicate rows)
3. **Secure OAuth** connection to Google
4. **Device-only** (no backend server, no hidden costs)
5. **Privacy-first** (tokens encrypted in device storage)

---

## 📚 Documentation

- **SETUP.md** - Detailed setup instructions
- **DEVELOPMENT.md** - Development guide for making changes
- **PROJECT_SUMMARY.md** - Complete project structure and architecture

---

**Happy syncing!** 🎉
