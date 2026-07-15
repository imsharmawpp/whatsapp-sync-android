package com.whatsappsync.app.data.models

data class GoogleSheetConfig(
    val spreadsheetId: String = "",
    val isConfigured: Boolean = false,
    val lastError: String? = null
)

data class GoogleAuthToken(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresIn: Long,
    val tokenType: String = "Bearer"
)
