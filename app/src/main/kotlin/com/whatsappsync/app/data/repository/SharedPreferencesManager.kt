package com.whatsappsync.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.json.Json

class SharedPreferencesManager(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedSharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "whatsapp_sync_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveGoogleAccessToken(token: String) {
        encryptedSharedPreferences.edit().putString("google_access_token", token).apply()
    }
    
    fun getGoogleAccessToken(): String? {
        return encryptedSharedPreferences.getString("google_access_token", null)
    }
    
    fun saveGoogleRefreshToken(token: String) {
        encryptedSharedPreferences.edit().putString("google_refresh_token", token).apply()
    }
    
    fun getGoogleRefreshToken(): String? {
        return encryptedSharedPreferences.getString("google_refresh_token", null)
    }
    
    fun saveSpreadsheetId(id: String) {
        encryptedSharedPreferences.edit().putString("spreadsheet_id", id).apply()
    }
    
    fun getSpreadsheetId(): String? {
        return encryptedSharedPreferences.getString("spreadsheet_id", null)
    }
    
    fun saveSyncedMessageIds(ids: Set<String>) {
        encryptedSharedPreferences.edit().putStringSet("synced_message_ids", ids).apply()
    }
    
    fun getSyncedMessageIds(): Set<String> {
        return encryptedSharedPreferences.getStringSet("synced_message_ids", emptySet()) ?: emptySet()
    }
    
    fun saveLastSyncTime(time: Long) {
        encryptedSharedPreferences.edit().putLong("last_sync_time", time).apply()
    }
    
    fun getLastSyncTime(): Long {
        return encryptedSharedPreferences.getLong("last_sync_time", 0L)
    }
    
    fun clearGoogleAuth() {
        encryptedSharedPreferences.edit().apply {
            remove("google_access_token")
            remove("google_refresh_token")
            remove("spreadsheet_id")
        }.apply()
    }
}
