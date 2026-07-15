# GitHub Setup Guide - WhatsApp Sync Android

## Quick Setup (3 Steps)

### Step 1: Create GitHub Repository
1. Go to https://github.com/new
2. Fill in:
   - **Repository name**: `whatsapp-sync-android`
   - **Description**: WhatsApp Business to Google Sheets Sync App
   - **Visibility**: Public (or Private if you prefer)
3. Click **"Create repository"**

### Step 2: Download Project from v0
1. In v0.app, click the three dots (⋯) in the top right
2. Select **"Download ZIP"**
3. Extract the ZIP to your computer

### Step 3: Push to GitHub

**For Windows:**
1. Open the extracted folder
2. Right-click → **"Open in Terminal"** (or open Command Prompt in that folder)
3. Run:
   ```bash
   PUSH_TO_GITHUB.bat imsharmawpp
   ```

**For Mac/Linux:**
1. Open Terminal
2. Navigate to the project folder:
   ```bash
   cd path/to/whatsapp-sync-android
   ```
3. Run:
   ```bash
   chmod +x PUSH_TO_GITHUB.sh
   ./PUSH_TO_GITHUB.sh imsharmawpp
   ```

4. When prompted, enter your GitHub credentials:
   - Username: `imsharmawpp` (or your GitHub username)
   - Password: Enter your GitHub personal access token OR your GitHub password

### Step 4: Verify on GitHub
- Go to https://github.com/imsharmawpp/whatsapp-sync-android
- You should see all your project files there!

---

## What's in the Repository?

✅ Complete Android app source code (Kotlin)  
✅ All UI screens and navigation  
✅ Google Sheets API integration  
✅ Accessibility service for WhatsApp  
✅ Permission management  
✅ Build configuration (Gradle)  
✅ Documentation and setup guides  

---

## Next Steps

### Build APK from GitHub Repository
1. Clone the repository to your computer:
   ```bash
   git clone https://github.com/imsharmawpp/whatsapp-sync-android.git
   ```

2. Open in Android Studio:
   - File → Open → Select the cloned folder

3. Build APK:
   - Build → Build APK(s)
   - Wait for the build to complete
   - Find `app-debug.apk` in `app/build/outputs/apk/debug/`

4. Install on Android device:
   - Transfer the APK to your phone
   - Tap to install

---

## Troubleshooting

**"fatal: not a git repository" error:**
- Make sure you're running the script in the downloaded project folder

**"Permission denied" error (Mac/Linux):**
- Run: `chmod +x PUSH_TO_GITHUB.sh` first

**"Authentication failed" error:**
- Use a GitHub Personal Access Token instead of your password
- Create one at: https://github.com/settings/tokens
- Permissions needed: `repo` (full control of private repositories)

**Want to Clone It Later?**
```bash
git clone https://github.com/imsharmawpp/whatsapp-sync-android.git
cd whatsapp-sync-android
```

---

## Questions?

The project includes:
- `QUICKSTART.md` - Get running in 15 minutes
- `SETUP.md` - Detailed setup instructions
- `OAUTH_SETUP.md` - Google OAuth configuration guide
- `DEVELOPMENT.md` - Development and customization guide

Good luck! 🚀
