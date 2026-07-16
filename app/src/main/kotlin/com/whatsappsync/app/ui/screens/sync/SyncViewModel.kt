package com.whatsappsync.app.ui.screens.sync

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappsync.app.accessibility.WhatsAppAccessibilityService
import com.whatsappsync.app.data.models.Message
import com.whatsappsync.app.data.repository.SharedPreferencesManager
import com.whatsappsync.app.data.service.GoogleSheetsClient
import com.whatsappsync.app.data.service.WhatsAppExportParser
import com.whatsappsync.app.data.service.WhatsAppMessageReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncViewModel(application: Application) : AndroidViewModel(application) {
    data class UiState(
        val phase: Phase = Phase.Idle,
        val pendingCount: Int = 0,
        val status: String = "Capture begins after Accessibility access is enabled.",
        val progress: Float? = null,
        val preview: ImportPreview? = null
    )

    data class ImportPreview(
        val fileName: String,
        val messages: List<Message>,
        val malformed: Int,
        val outsideWindow: Int
    )

    enum class Phase { Idle, Working, Preview, Success, Empty, PermissionRequired, AuthExpired, Error }

    private val app = application
    private val store = SharedPreferencesManager(application)
    private val sheets = GoogleSheetsClient(application)
    private val parser = WhatsAppExportParser()
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        WhatsAppMessageReader.initialize(application)
        refresh()
    }

    fun refresh() {
        val pending = store.getPendingMessages().size
        _state.value = _state.value.copy(pendingCount = pending)
    }

    fun syncCapturedMessages() {
        if (!isAccessibilityEnabled()) {
            _state.value = UiState(
                phase = Phase.PermissionRequired,
                pendingCount = store.getPendingMessages().size,
                status = "Enable WhatsApp Sync under Accessibility, then return and retry."
            )
            return
        }
        upload(store.getPendingMessages(), "captured messages")
    }

    fun selectExport(uri: Uri) {
        viewModelScope.launch {
            _state.value = UiState(Phase.Working, store.getPendingMessages().size, "Reading export…")
            runCatching {
                withContext(Dispatchers.IO) {
                    val name = queryFileName(uri)
                    val input = app.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Unable to open the selected export")
                    input.use { parser.parse(it, name) } to name
                }
            }.onSuccess { (result, name) ->
                if (result.messages.isEmpty()) {
                    _state.value = UiState(
                        Phase.Empty, store.getPendingMessages().size,
                        "No messages from the previous 90 days were found. ${result.recordsOutsideWindow} older records were skipped."
                    )
                } else {
                    _state.value = UiState(
                        phase = Phase.Preview,
                        pendingCount = store.getPendingMessages().size,
                        status = "Review the import before uploading.",
                        preview = ImportPreview(name, result.messages, result.malformedRecords, result.recordsOutsideWindow)
                    )
                }
            }.onFailure(::showError)
        }
    }

    fun confirmImport() {
        val preview = _state.value.preview ?: return
        upload(preview.messages, "historical messages")
    }

    fun cancelImport() {
        _state.value = UiState(pendingCount = store.getPendingMessages().size)
    }

    fun retry() {
        _state.value = UiState(pendingCount = store.getPendingMessages().size)
    }

    private fun upload(messages: List<Message>, label: String) {
        if (messages.isEmpty()) {
            _state.value = UiState(Phase.Empty, 0, "There are no new $label to upload.")
            return
        }
        viewModelScope.launch {
            _state.value = UiState(Phase.Working, store.getPendingMessages().size, "Uploading $label…", 0f)
            val result = withContext(Dispatchers.IO) {
                sheets.appendMessagesToSheet(messages) { uploaded, total ->
                    _state.value = _state.value.copy(progress = uploaded.toFloat() / total, status = "Uploaded $uploaded of $total")
                }
            }
            result.onSuccess { count ->
                _state.value = if (count == 0) {
                    UiState(Phase.Empty, store.getPendingMessages().size, "Everything selected was already synced.")
                } else {
                    UiState(Phase.Success, store.getPendingMessages().size, "Uploaded $count $label successfully.")
                }
            }.onFailure(::showError)
        }
    }

    private fun showError(error: Throwable) {
        val authExpired = error is GoogleSheetsClient.AuthenticationExpiredException
        if (authExpired) sheets.clearAuthentication()
        _state.value = UiState(
            phase = if (authExpired) Phase.AuthExpired else Phase.Error,
            pendingCount = store.getPendingMessages().size,
            status = error.message ?: "Sync failed. Check your connection and retry."
        )
    }

    private fun queryFileName(uri: Uri): String {
        app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return uri.lastPathSegment ?: "WhatsApp export.txt"
    }

    private fun isAccessibilityEnabled(): Boolean {
        val component = ComponentName(app, WhatsAppAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(app.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabled?.split(':')?.any { it.equals(component, ignoreCase = true) } == true
    }
}
