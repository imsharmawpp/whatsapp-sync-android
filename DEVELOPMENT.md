# WhatsApp Sync - Development Guide

This guide covers common development tasks and workflows for the WhatsApp Sync Android app.

## Development Environment

### Requirements
- Android Studio (Giraffe or newer)
- Android SDK 35 (installed via SDK Manager)
- Java 11+ (included with Android Studio)
- Git (optional, for version control)

### First-Time Setup
1. Open the project in Android Studio
2. Wait for Gradle sync to complete (may take 2-3 minutes)
3. Accept SDK installation prompts
4. Click "Run" to build and install on device/emulator

## Project Structure & Where to Make Changes

### Adding a New Screen
1. Create file: `app/src/main/kotlin/com/whatsappsync/app/ui/screens/[feature]/[Feature]Screen.kt`
2. Create `@Composable` function
3. Add route to `ui/navigation/AppNavigation.kt`
4. Example:
```kotlin
@Composable
fun MyNewScreen(onBackClicked: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Your UI here
    }
}
```

### Adding a New Data Model
1. Create file: `app/src/main/kotlin/com/whatsappsync/app/data/models/[Model].kt`
2. Define `data class` with properties
3. Example:
```kotlin
data class Contact(
    val name: String,
    val phoneNumber: String,
    val lastMessageTime: Long
)
```

### Adding a New Service/API Integration
1. Create file: `app/src/main/kotlin/com/whatsappsync/app/data/service/[Service].kt`
2. Implement async operations using coroutines
3. Return `Result<T>` for error handling
4. Example:
```kotlin
class MyApiClient(private val context: Context) {
    suspend fun fetchData(): Result<String> = runCatching {
        // API call here
        "success"
    }
}
```

## Common Development Tasks

### Testing on Device
```bash
# Install debug APK
./gradlew installDebug

# Run specific activity
adb shell am start -n com.whatsappsync.app/.MainActivity

# View logs in real-time
adb logcat | grep WhatsAppSync

# Clear app data
adb shell pm clear com.whatsappsync.app
```

### Testing on Emulator
1. Open Device Manager in Android Studio
2. Create emulator (recommend API 30+)
3. Click run/play button to start emulator
4. Click "Run" in Android Studio to deploy app

### Debugging
1. Set breakpoints by clicking line numbers
2. Click "Debug" instead of "Run"
3. App will pause at breakpoints
4. Use Debug panel to inspect variables

### Building Release APK
```bash
# Requires signing key setup in keystore
# After first setup:
./gradlew assembleRelease
# APK will be at: app/build/outputs/apk/release/app-release.apk
```

## Code Patterns Used in This Project

### 1. Composable UI Pattern
```kotlin
@Composable
fun MyScreen(
    onActionClick: () -> Unit,  // Callbacks instead of events
    someState: String = "default"
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Hello")
        Button(onClick = onActionClick) {
            Text("Click Me")
        }
    }
}
```

### 2. Data Flow Pattern
```
User Action → ViewModel/State → UI Update
```
No direct API calls from UI; use services + state management.

### 3. Error Handling Pattern
```kotlin
suspend fun operation(): Result<String> = runCatching {
    // do work
    "success"
}

// Usage:
operation()
    .onSuccess { result -> /* handle success */ }
    .onFailure { error -> /* handle error */ }
```

### 4. Async/Await Pattern
```kotlin
// Don't use:
val data = GlobalScope.launch { ... }

// Do use:
LaunchedEffect(Unit) {
    val result = asyncOperation()
}
```

## File Organization

### Kotlin Package Structure
- `accessibility/` - Accessibility service implementation
- `data/` - Data layer (models, repos, services)
  - `models/` - Data classes
  - `repository/` - Local storage
  - `service/` - API clients
- `permissions/` - Permission management
- `ui/` - User interface
  - `screens/` - UI screens
  - `theme/` - Theming
  - `navigation/` - Navigation logic

### Resource Organization
- `res/values/` - Colors, strings, dimensions
- `res/xml/` - Configuration files
- `res/drawable/` - Images and icons

## Working with Dependencies

### Adding a New Dependency
1. Edit `gradle/libs.versions.toml`
2. Add version in `[versions]` section
3. Add library in `[libraries]` section
4. Import in `app/build.gradle.kts` under `dependencies`
5. Gradle sync will download the library

Example:
```toml
# In gradle/libs.versions.toml
[versions]
my-new-library = "1.2.3"

[libraries]
my-new-library = { group = "com.example", name = "library", version.ref = "my-new-library" }
```

Then in `app/build.gradle.kts`:
```kotlin
implementation(libs.my.new.library)
```

## Useful Android Studio Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+Shift+A` | Find action by name |
| `Ctrl+N` | New file/class |
| `Ctrl+O` | Override method |
| `Ctrl+Alt+L` | Format code |
| `Ctrl+/` | Toggle comment |
| `Shift+F10` | Run app |
| `Shift+F9` | Debug app |
| `Ctrl+D` | Duplicate line |
| `Ctrl+Shift+X` | Delete line |

## Testing

### Manual Testing Checklist
- [ ] App starts without crashes
- [ ] Permissions screen appears
- [ ] Accessibility service enables
- [ ] Home screen shows sync button
- [ ] Sync button opens sync screen
- [ ] Settings button opens settings
- [ ] Navigation back works
- [ ] Theme switches (light/dark mode)

### Debug Logging
Add to code for debugging:
```kotlin
Log.d("WhatsAppSync", "Message: $value")
```
View with:
```bash
adb logcat | grep WhatsAppSync
```

## Performance Optimization

### Do's
- Use Compose remember for state
- Use LaunchedEffect for side effects
- Batch database operations
- Cache results where appropriate
- Use `@Stable` for data classes

### Don'ts
- Don't call composables in loops
- Don't use GlobalScope for coroutines
- Don't do heavy operations in composables
- Don't recompose unnecessarily
- Don't use Thread.sleep()

## Troubleshooting Development Issues

### Gradle Sync Fails
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

### App Crashes on Launch
1. Check Logcat for stack trace
2. Search error message
3. Verify all permissions in AndroidManifest.xml
4. Ensure all resources exist

### Slow Emulator
- Use Android Emulator with API 30 (not latest)
- Disable animations (Settings > Developer Options)
- Allocate more RAM (Device Manager > Settings)

### Changes Not Appearing
- Rebuild project: `Build > Clean Project`
- Restart emulator or clear device
- Invalidate cache: `File > Invalidate Caches`

## Code Style Guidelines

### Naming Conventions
- Classes: PascalCase (e.g., `MainActivity`)
- Functions: camelCase (e.g., `fetchMessages()`)
- Constants: UPPER_SNAKE_CASE (e.g., `MAX_RETRIES`)
- Private vars: leading underscore (e.g., `_uiState`)

### Format Guidelines
- Max line length: 100 characters
- 4 spaces for indentation
- One class per file (except sealed classes)
- Group imports alphabetically

### Kotlin Best Practices
- Use `val` instead of `var`
- Use extension functions for utility
- Use scope functions (`let`, `run`, `apply`)
- Avoid null; use optional types

## Version Control Tips

### Good Commit Messages
```
Add: Sync button to home screen
Fix: Accessibility service crashes on denied permission
Update: Google Sheets API to latest version
```

### .gitignore (already included)
- `/build/` - Build artifacts
- `.gradle/` - Gradle cache
- `*.iml` - IDE files
- `local.properties` - SDK path (don't share)

## Publishing to Play Store

When ready to release:
1. Create signed APK (or AAB)
2. Test on real devices (multiple versions)
3. Create Google Play Console account
4. Upload APK/AAB
5. Complete store listing
6. Submit for review

See Android docs: https://developer.android.com/studio/publish

## Getting Help

- **Android Docs**: https://developer.android.com
- **Kotlin Docs**: https://kotlinlang.org/docs
- **Compose Docs**: https://developer.android.com/jetpack/compose
- **Stack Overflow**: Tag `android`, `kotlin`, `jetpack-compose`

---

Happy developing! 🚀
