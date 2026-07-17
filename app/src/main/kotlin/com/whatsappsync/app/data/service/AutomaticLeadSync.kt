package com.whatsappsync.app.data.service

import android.content.Context
import com.whatsappsync.app.data.models.Message
import com.whatsappsync.app.data.repository.SharedPreferencesManager

class AutomaticLeadSync(context: Context) {
    private val appContext = context.applicationContext
    private val store = SharedPreferencesManager(appContext)
    private val sheets = GoogleSheetsClient(appContext)
    private val contacts = ContactResolver(appContext)

    suspend fun resolveAndSync(message: Message): Result<Int> {
        val knownPhone = ContactResolver.normalizePhone(message.phoneNumber)
            ?: ContactResolver.normalizePhone(message.conversationName)
            ?: ContactResolver.normalizePhone(message.senderName)
            ?: ContactResolver.normalizePhone(store.getPhoneMapping(message.conversationName))

        val resolved = if (knownPhone != null) {
            ContactResolver.Result.Resolved(knownPhone, message.senderName)
        } else {
            contacts.resolve(message.conversationName.ifBlank { message.senderName })
        }

        return when (resolved) {
            ContactResolver.Result.Ambiguous, ContactResolver.Result.Unresolved -> {
                store.addPendingMessage(message.copy(phoneNumber = ""))
                Result.success(0)
            }
            is ContactResolver.Result.Resolved -> {
                store.savePhoneMapping(message.conversationName, resolved.phoneNumber)
                val matchingPending = store.getPendingMessages().filter {
                    it.conversationName.equals(message.conversationName, ignoreCase = true)
                }
                val earliest = (matchingPending + message).minByOrNull { it.timestamp } ?: message
                val lead = earliest.copy(
                        phoneNumber = resolved.phoneNumber,
                        senderName = resolved.displayName.ifBlank {
                            message.senderName.takeUnless(ContactResolver::looksLikePhone).orEmpty()
                        },
                        direction = "incoming",
                    )
                store.addPendingMessage(lead)
                sheets.appendMessagesToSheet(listOf(lead))
            }
        }
    }

    suspend fun retryPending(): Result<Int> {
        var synced = 0
        for (message in store.getPendingMessages().sortedBy { it.timestamp }) {
            resolveAndSync(message).getOrElse { return Result.failure(it) }.also { synced += it }
        }
        return Result.success(synced)
    }
}
