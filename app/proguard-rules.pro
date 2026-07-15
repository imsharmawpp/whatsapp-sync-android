# ProGuard rules for WhatsApp Sync app

# Google Sheets API
-keep class com.google.api.services.sheets.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.auth.** { *; }

# Kotlin
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Keep data classes
-keep class com.whatsappsync.app.data.models.** { *; }

# Keep services
-keep class com.whatsappsync.app.accessibility.** { *; }

# Keep our app classes
-keep class com.whatsappsync.app.** { *; }

# Prevent class name obfuscation for reflection
-keepnames class com.google.api.services.sheets.v4.Sheets$Spreadsheets$Values
-keepnames class com.google.api.services.sheets.v4.model.ValueRange

# Preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Jackson (if used)
-keep @com.fasterxml.jackson.annotation.JsonIgnoreProperties class * { *; }
-keep @com.fasterxml.jackson.annotation.JsonProperty class * { *; }

# Jetpack Compose
-keep class androidx.compose.** { *; }
