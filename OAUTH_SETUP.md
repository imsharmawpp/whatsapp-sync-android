# Google OAuth 2.0 Setup Guide

Your OAuth 2.0 Client ID has been successfully added to the project!

## ✅ What We've Done

- Added your OAuth Client ID: `613814281399-v1r1k4uoggvdrrup7egtng71r02s1jrd.apps.googleusercontent.com`
- Created `GoogleOAuthConfig.kt` with your credentials
- Configured Google Sheets API scopes

## 📋 Next Steps

### 1. Add Your Google Sheet ID

You now need to provide your Google Sheet ID where messages will be synced.

**To find your Google Sheet ID:**
1. Open your Google Sheet at sheets.google.com
2. Look at the URL: `https://docs.google.com/spreadsheets/d/[SHEET_ID]/edit`
3. Copy the `[SHEET_ID]` part (long string between `/d/` and `/edit`)

**Two ways to add it:**

**Option A: Add it directly in code (for testing)**
- Open `app/src/main/kotlin/com/whatsappsync/app/config/GoogleOAuthConfig.kt`
- Replace `const val DEFAULT_SHEET_ID = ""` with your Sheet ID
- Example: `const val DEFAULT_SHEET_ID = "1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o"`

**Option B: Users enter it in app settings (recommended)**
- Keep `DEFAULT_SHEET_ID = ""` empty
- When app launches, users go to Settings tab
- Enter their Sheet ID in the "Spreadsheet ID" field
- Click Save

### 2. Configure Your Google Sheet Structure

Your Google Sheet should have these columns (in order):
- Column A: `Timestamp` - When the message was received
- Column B: `Phone Number` - Customer's phone number
- Column C: `Customer Name` - Name of the person
- Column D: `Message` - The message text
- Column E: `Synced Date` - When it was synced to the sheet

**Example first row:**
| Timestamp | Phone Number | Customer Name | Message | Synced Date |
|-----------|--------------|---------------|---------|------------|
| 2026-07-16 14:30:45 | +1234567890 | John Doe | Hi, I'm interested in your product | 2026-07-16 |

### 3. Share Your Sheet (Important!)

Make sure your Google Sheet is accessible:
1. Open your Google Sheet
2. Click "Share" (top right)
3. Add the email address associated with your Google account
4. Grant "Editor" access
5. Make sure the email matches the one you'll use in the app

### 4. Test the App

1. **Build and run the app** on your Android device
2. **Go to Settings tab**
3. **Click "Connect to Google Sheets"**
4. **Sign in with your Google account** (the one that owns the Sheet)
5. **Enter your Spreadsheet ID**
6. **Click Save**

### 5. Enable Accessibility Service (Critical!)

For the app to read WhatsApp messages:
1. Open app → Go to Permissions tab
2. You'll see "Enable Accessibility Service" option
3. Android will take you to Settings
4. Find "WhatsApp Sync" in the accessibility apps list
5. Toggle it ON
6. Go back to the app

Without this, the app cannot read WhatsApp messages!

### 6. Start Syncing!

1. **Open WhatsApp Business app** and receive/send some messages
2. **Open WhatsApp Sync app**
3. **Go to Sync tab**
4. **Tap "Sync Now"** button
5. The app will:
   - Read messages from WhatsApp Business
   - Extract Name, Number, Message, Timestamp
   - Remove duplicates
   - Upload to Google Sheet
   - Show "Success: X messages synced"

6. **Check your Google Sheet** - New rows should appear!

## 🔐 Security Notes

- Your Google OAuth token is stored securely in Android Keystore
- Tokens are never stored in plain text
- The app only accesses Google Sheets API (nothing else)
- Refresh tokens are used automatically to keep you logged in

## 🐛 Troubleshooting

**"Unresolved reference: GOOGLE_OAUTH_CLIENT_ID"**
- Make sure the `GoogleOAuthConfig.kt` file exists in the project
- Rebuild the project: Build → Rebuild Project

**"Not connected to Google Sheets"**
- Go to Settings → Click "Connect to Google Sheets"
- Make sure you're signing in with the correct Google account

**"Spreadsheet ID not found"**
- Go to Settings → Enter your Sheet ID
- Make sure you copied the ID correctly from the URL

**"Permission Denied" errors**
- Make sure your Sheet is shared with your Google account
- Make sure the Accessibility Service is enabled (needed for reading messages)

**App crashes on Sync**
- Enable Accessibility Service first (required!)
- Make sure WhatsApp Business app is installed
- Check that your Sheet ID is correct

## 📝 Important Reminders

✅ Your OAuth Client ID is already configured
✅ Your Google OAuth config file is created
⚠️ Still needed: Your Google Sheet ID
⚠️ Still needed: Enable Accessibility Service in your Android settings
⚠️ Still needed: Share your Google Sheet with your account

## 📞 Support

If you encounter any issues:
1. Check the troubleshooting section above
2. Make sure all sheets columns are named correctly
3. Verify your Sheet is shared with the right account
4. Try rebuilding the project in Android Studio

---

**You're almost done!** Once you add your Sheet ID and enable accessibility service, the app will be ready to use.
