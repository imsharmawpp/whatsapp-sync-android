package com.whatsappsync.app.data.service

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppExportParserTest {
    private val parser = WhatsAppExportParser()

    @Test
    fun parsesAndroidExportAndMultilineMessages() {
        val text = """
            7/10/26, 9:15 AM - Alice: First line
            second line
            7/10/26, 9:16 AM - +1 555 123 4567: Hello
        """.trimIndent()

        val result = parser.parseText(text, "Customer chat")

        assertEquals(2, result.messages.size)
        assertEquals("First line\nsecond line", result.messages[0].messageText)
        assertEquals("+1 555 123 4567", result.messages[1].phoneNumber)
        assertEquals(0, result.malformedRecords)
    }

    @Test
    fun includesCompleteHistoryWithoutDateCutoff() {
        val text = """
            1/1/20, 8:00 AM - Alice: Original first message
            7/10/26, 9:15 AM - Alice: Recent message
        """.trimIndent()

        val result = parser.parseText(text, "Customer chat")

        assertEquals(2, result.messages.size)
        assertEquals("Original first message", result.messages.first().messageText)
        assertEquals(0, result.recordsOutsideWindow)
    }

    @Test
    fun deduplicatesRepeatedExportRecords() {
        val line = "7/10/26, 9:15 AM - Alice: Same message"
        val result = parser.parseText("$line\n$line", "Customer chat")
        assertEquals(1, result.messages.size)
        assertTrue(result.messages.single().uniqueId.isNotBlank())
    }

    @Test
    fun parsesIosBracketedTimestamp() {
        val result = parser.parseText("[7/10/26, 9:15:00 AM] Alice: From iPhone", "Alice")
        assertEquals(1, result.messages.size)
        assertEquals("From iPhone", result.messages.single().messageText)
    }

    @Test
    fun readsTxtEntryFromZipExport() {
        val bytes = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("WhatsApp Chat with Alice.txt"))
                zip.write("7/10/26, 9:15 AM - Alice: From ZIP".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()

        val result = parser.parse(ByteArrayInputStream(bytes), "Alice.zip")
        assertEquals(1, result.messages.size)
        assertEquals("From ZIP", result.messages.single().messageText)
    }
}
