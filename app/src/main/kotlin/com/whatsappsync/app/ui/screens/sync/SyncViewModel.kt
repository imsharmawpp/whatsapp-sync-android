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

    enum class Phase { Idle, Working, Preview, Success, Empty, Error }

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
                if (uris.isEmpty()) {
                    _state.value = queueState("The WhatsApp share contained no readable TXT or ZIP file.")
                        .copy(phase = Phase.Error)
                    ShareImportCoordinator.consume(uris)
                } else {
                    selectExports(uris, consumeShareAfterStart = true)
                }
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

    fun selectExports(uris: List<Uri>, consumeShareAfterStart: Boolean = false) {
        if (uris.isEmpty()) return
        retryAction = { selectExports(uris) }
        viewModelScope.launch {
            _state.value = queueState("Reading WhatsApp export…").copy(phase = Phase.Working)
            if (consumeShareAfterStart) ShareImportCoordinator.consume(uris)
            val myWhatsAppName = store.getWhatsAppName().trim()
            if (myWhatsAppName.isBlank()) {
                _state.value = queueState("Set your exact WhatsApp display name in Settings before importing chats.")
                    .copy(phase = Phase.Error)
                return@launch
            }
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
                val messages = parsed
                    .flatMap { it.first.messages }
                    .filterNot { it.senderName.trim().equals(myWhatsAppName, ignoreCase = true) }
                    .groupBy { it.conversationName }
                    .mapNotNull { (_, chatMessages) -> chatMessages.minByOrNull(Message::timestamp) }
                    .distinctBy { it.conversationName.trim().lowercase() }
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
                        "No client messages were found. Check your WhatsApp display name in Settings."
                    } else {
                        "Review the first client lead before adding it to the queue."
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
            _state.value = queueState("No pending leads to sync.").copy(phase = Phase.Empty)
            return
        }
        val mappedPending = pending.map { message ->
            val entered = _state.value.phoneNumbers[message.conversationName].orEmpty()
            val phone = message.phoneNumber.ifBlank { entered.ifBlank { store.getPhoneMapping(message.conversationName) } }
            message.copy(phoneNumber = phone)
        }
        val missing = mappedPending.filter { it.phoneNumber.isBlank() }.map { it.conversationName }.toSet()
        if (missing.isNotEmpty()) {
            _state.update { it.copy(unresolvedChats = missing, status = "Enter a mobile number for each pending lead.") }
            return
        }
        mappedPending.forEach { store.savePhoneMapping(it.conversationName, it.phoneNumber) }
        store.savePendingMessages(mappedPending)
        viewModelScope.launch {
            _state.value = queueState("Uploading ${mappedPending.size} leads…")
                .copy(phase = Phase.Working, progress = 0f)
            val result = withContext(Dispatchers.IO) {
                sheets.appendMessagesToSheet(mappedPending) { uploaded, total ->
                    _state.update {
                        it.copy(
                            progress = uploaded.toFloat() / total.coerceAtLeast(1),
                            status = "Uploaded $uploaded of $total",
                        )
                    }
                }
            }
            result.onSuccess {
                _state.value = queueState("Processed $it lead candidates in Google Sheets.")
                    .copy(phase = Phase.Success)
            }.onFailure(::showError)
        }
    }

    fun retry() {
        retryAction?.invoke()
    }

    private fun queueState(status: String = "Ready to sync pending messages."): UiState {
        val pending = store.getPendingMessages()
        val unresolved = pending.filter { it.phoneNumber.isBlank() }.map { it.conversationName }.toSet()
        return UiState(
            pendingCount = pending.size,
            notificationCount = pending.count { it.source == "notification" },
            exportCount = pending.count { it.source == "export" },
            status = status,
            phoneNumbers = unresolved.associateWith { store.getPhoneMapping(it) },
            unresolvedChats = unresolved,
        )
    }

    private fun showError(error: Throwable) {
        _state.value = queueState(error.message ?: "Operation failed. Please retry.")
            .copy(phase = Phase.Error)
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
