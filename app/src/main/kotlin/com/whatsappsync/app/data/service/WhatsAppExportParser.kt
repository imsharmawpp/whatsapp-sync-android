package com.whatsappsync.app.data.service

import com.whatsappsync.app.data.models.Message
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipInputStream

class WhatsAppExportParser {
    data class ParseResult(
        val messages: List<Message>,
        val malformedRecords: Int,
        val recordsOutsideWindow: Int
    )

    fun parse(
        input: InputStream,
        fileName: String,
        conversationName: String = fileName.substringBeforeLast('.'),
    ): ParseResult {
        val text = if (fileName.endsWith(".zip", ignoreCase = true)) {
            readTextFromZip(input)
        } else {
            input.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        }
        return parseText(text, conversationName)
    }

    fun parseText(text: String, conversationName: String): ParseResult {
        val parsed = mutableListOf<Message>()
        var malformed = 0
        var current: PendingRecord? = null

        fun commit() {
            val record = current ?: return
            val timestamp = parseTimestamp(record.date, record.time)
            if (timestamp == null) {
                malformed++
            } else if (record.body.isNotBlank()) {
                parsed += Message(
                    phoneNumber = record.sender.takeIf { PHONE_PATTERN.containsMatchIn(it) }.orEmpty(),
                    senderName = record.sender.ifBlank { conversationName },
                    messageText = record.body.trim(),
                    timestamp = timestamp,
                    conversationName = conversationName
                )
            }
            current = null
        }

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.removePrefix("\u200e").removePrefix("[")
            val match = RECORD_PATTERN.matchEntire(line)
            if (match != null) {
                commit()
                val content = match.groupValues[3].removeSuffix("]").trimStart(' ', '-')
                val separator = content.indexOf(": ")
                val sender = if (separator > 0) content.substring(0, separator) else conversationName
                val body = if (separator > 0) content.substring(separator + 2) else content
                current = PendingRecord(match.groupValues[1], match.groupValues[2], sender, body)
            } else if (current != null) {
                current = current?.copy(body = current!!.body + "\n" + rawLine)
            } else if (rawLine.isNotBlank()) {
                malformed++
            }
        }
        commit()

        return ParseResult(parsed.distinctBy { it.uniqueId }, malformed, 0)
    }

    private fun readTextFromZip(input: InputStream): String {
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".txt", ignoreCase = true)) {
                    return InputStreamReader(zip, Charsets.UTF_8).readText()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        throw IllegalArgumentException("This ZIP does not contain a WhatsApp .txt export")
    }

    private fun parseTimestamp(date: String, time: String): Long? {
        val normalized = "$date ${time.replace(" ", " ").replace(" ", " ")}".trim()
        return DATE_FORMATS.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    timeZone = TimeZone.getDefault()
                }.parse(normalized)?.time
            }.getOrNull()
        }
    }

    private data class PendingRecord(
        val date: String,
        val time: String,
        val sender: String,
        val body: String
    )

    companion object {
        private val PHONE_PATTERN = Regex("\\+?[0-9][0-9 ()-]{6,}")
        private val RECORD_PATTERN = Regex(
            "^(?:\\[)?(\\d{1,4}[/.\\-]\\d{1,2}[/.\\-]\\d{1,4}),?\\s+" +
                "(\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*[AaPp][Mm])?)" +
                "(?:\\])?\\s*(?:-|–)?\\s*(.+)$"
        )
        private val DATE_FORMATS = listOf(
            "M/d/yy h:mm a", "M/d/yyyy h:mm a", "MM/dd/yy, h:mm a",
            "d/M/yy H:mm", "d/M/yyyy H:mm", "dd/MM/yy, HH:mm",
            "yyyy/M/d H:mm", "yyyy-MM-dd HH:mm", "M/d/yy H:mm"
        )
    }
}
