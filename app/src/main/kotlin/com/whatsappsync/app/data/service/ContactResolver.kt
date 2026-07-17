package com.whatsappsync.app.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

class ContactResolver(private val context: Context) {
    sealed interface Result {
        data class Resolved(val phoneNumber: String, val displayName: String) : Result
        data object Ambiguous : Result
        data object Unresolved : Result
    }

    fun resolve(identity: String): Result {
        val cleanedIdentity = identity.trim()
            .substringBeforeLast('.')
            .replace(Regex("^(?:WhatsApp Chat with|Chat with)\\s+", RegexOption.IGNORE_CASE), "")
            .substringBefore(" (")
            .trim()
        normalizePhone(cleanedIdentity)?.let {
            return Result.Resolved(it, cleanedIdentity.takeUnless(::looksLikePhone).orEmpty())
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return Result.Unresolved
        }

        val matches = linkedMapOf<String, String>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ? COLLATE NOCASE",
            arrayOf(cleanedIdentity),
            null,
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                normalizePhone(cursor.getString(phoneIndex))?.let { phone ->
                    matches[phone] = cursor.getString(nameIndex).orEmpty().ifBlank { cleanedIdentity }
                }
            }
        }
        return when (matches.size) {
            0 -> Result.Unresolved
            1 -> matches.entries.first().let { Result.Resolved(it.key, it.value) }
            else -> Result.Ambiguous
        }
    }

    companion object {
        private val phoneCandidate = Regex("(?:\\+?\\d[\\d\\s().-]{5,}\\d)")

        fun normalizePhone(value: String): String? {
            val candidate = phoneCandidate.find(value)?.value ?: return null
            val digits = candidate.filter(Char::isDigit)
            if (digits.length !in 7..15) return null
            return if (candidate.trim().startsWith("+")) "+$digits" else digits
        }

        fun looksLikePhone(value: String): Boolean =
            normalizePhone(value) != null && value.count(Char::isLetter) == 0
    }
}
