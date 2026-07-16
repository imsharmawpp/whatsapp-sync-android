package com.whatsappsync.app.ui.screens.sync

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whatsappsync.app.data.models.Message
import com.whatsappsync.app.data.repository.SharedPreferencesManager
import com.whatsappsync.app.data.service.GoogleSheetsClient
import com.whatsappsync.app.data.service.WhatsAppExportParser
import com.whatsappsync.app.share.ShareImportCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncViewModel(application: Application) : AndroidViewModel(application) {
    data class UiState(
        val phase: Phase = Phase.Idle,
        val pendingCount: Int = 0,
        val notificationCount: Int = 0,
        val exportCount: Int = 0,
        val status: String = "Export a chat or enable notification access to begin.",
        val progress: Float? = null,
        val preview: ImportPreview? = null,
        val phoneNumbers: Map<String, String> = emptyMap(),
        val unresolvedChats: Set<String> = emptySet(),
    )

    data class ImportPreview(
        val fileName: String,
        val messages: List<Message>,
        val malformed: Int,
        val outsideWindow: Int,
    )

    enum class Phase { Idle, Working, Preview, Success, Empty, AuthExpired, Error }

    private val app = application
    private val store = SharedPreferencesManager(application)
    private val sheets = GoogleSheetsClient(application)
    private val parser = WhatsAppExportParser()
    private val _state = MutableStateFlow(queueState())
    val state = _state.asStateFlow()
    private var retryAction: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            ShareImportCoordinator.incoming.filterNotNull().collect { uris ->
                ShareImportCoordinator.consume(uris)
                selectExports(uris)
            }
        }
    }

    fun refresh() {
        _state.value = queueState(_state.value.status)
    }

    fun setPhoneNumber(chat: String, value: String) {
        _state.update { it.copy(phoneNumbers = it.phoneNumbers + (chat to value)) }
    }

    fun selectExport(uri: Uri) = selectExports(listOf(uri))

    fun selectExports(uris: List<Uri>) {
        if (uris.isEmpty()) return
        retryAction = { selectExports(uris) }
        viewModelScope.launch {
            _state.value = queueState("Reading WhatsApp export…").copy(phase = Phase.Working)
            runCatching {
                withContext(Dispatchers.IO) {
                    uris.map { uri ->
                        val name = queryFileName(uri)
                        require(name.endsWith(".txt", true) || name.endsWith(".zip", true)) {
                            "$name is not a TXT or ZIP WhatsApp export."
                        }
                        val input = app.contentResolver.openInputStream(uri)
                            ?: error("Unable to open $name")
                        input.use { parser.parse(it, name) } to name
                    }
                }
            }.onSuccess { parsed ->
                val messages = parsed.flatMap { it.first.messages }.distinctBy(Message::uniqueId)
                val name = if (parsed.size == 1) parsed.first().second else "${parsed.size} exported chats"
                val preview = ImportPreview(
                    fileName = name,
                    messages = messages,
                    malformed = parsed.sumOf { it.first.malformedRecords },
                    outsideWindow = parsed.sumOf { it.first.recordsOutsideWindow },
                )
                val chats = messages.map { it.conversationName }.toSortedSet()
                val numbers = chats.associateWith { store.getPhoneMapping(it) }
                val unresolved = chats.filterTo(mutableSetOf()) { chat ->
                    numbers[chat].isNullOrBlank() && messages.none {
                        it.conversationName == chat && it.phoneNumber.isNotBlank()
                    }
                }
                _state.value = queueState().copy(
                    phase = if (messages.isEmpty()) Phase.Empty else Phase.Preview,
                    preview = preview.takeIf { messages.isNotEmpty() },
                    phoneNumbers = numbers,
                    unresolvedChats = unresolved,
                    status = if (messages.isEmpty()) {
                        "No messages from the previous 90 days were found."
                    } else {
                        "Review the export before adding it to the queue."
                    },
                )
            }.onFailure(::showError)
        }
    }

    fun confirmImport() {
        val current = _state.value
        val preview = current.preview ?: return
        val missing = current.unresolvedChats.filter { current.phoneNumbers[it].isNullOrBlank() }
        if (missing.isNotEmpty()) {
            _state.update { it.copy(status = "Enter a mobile number for each chat before continuing.") }
            return
        }

        current.phoneNumbers.forEach { (chat, phone) ->
            if (phone.isNotBlank()) store.savePhoneMapping(chat, phone)
        }
        preview.messages.forEach { message ->
            val mapped = current.phoneNumbers[message.conversationName].orEmpty()
            store.addPendingMessage(
                message.copy(
                    phoneNumber = message.phoneNumber.ifBlank { mapped },
                    source = "export",
                ),
            )
        }
        _state.value = queueState("Export queued. Tap Sync pending messages when ready.")
            .copy(phase = Phase.Success)
    }

    fun cancelImport() {
        _state.value = queueState("Import cancelled.")
    }

    fun syncPendingMessages() {
        retryAction = ::syncPendingMessages
        val pending = store.getPendingMessages()
        if (pending.isEmpty()) {
            _state.value = queueState("No pending messages to sync.").copy(phase = Phase.Empty)
            return
        }
        viewModelScope.launch {
            _state.value = queueState("Uploading ${pending.size} messages…")
                .copy(phase = Phase.Working, progress = 0f)
            val result = withContext(Dispatchers.IO) {
                sheets.appendMessagesToSheet(pending) { uploaded, total ->
                    _state.update {
                        it.copy(
                            progress = uploaded.toFloat() / total.coerceAtLeast(1),
                            status = "Uploaded $uploaded of $total",
                        )
                    }
                }
            }
            result.onSuccess {
                _state.value = queueState("Synced $it messages to Google Sheets.")
                    .copy(phase = Phase.Success)
            }.onFailure(::showError)
        }
    }

    fun retry() {
        retryAction?.invoke()
    }

    private fun queueState(status: String = "Ready to sync pending messages."): UiState {
        val pending = store.getPendingMessages()
        return UiState(
            pendingCount = pending.size,
            notificationCount = pending.count { it.source == "notification" },
            exportCount = pending.count { it.source == "export" },
            status = status,
        )
    }

    private fun showError(error: Throwable) {
        val expired = error is GoogleSheetsClient.AuthenticationExpiredException
        if (expired) sheets.clearAuthentication()
        _state.value = queueState(error.message ?: "Operation failed. Please retry.")
            .copy(phase = if (expired) Phase.AuthExpired else Phase.Error)
    }

    private fun queryFileName(uri: Uri): String {
        app.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return uri.lastPathSegment ?: "WhatsApp export.txt"
    }
}
