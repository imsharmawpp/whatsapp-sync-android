package com.whatsappsync.app.data.service

import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppExportParserTest {
    private val parser = WhatsAppExportParser()
    private val dateFormat = SimpleDateFormat("M/d/yy h:mm a", Locale.US)

    @Test
    fun parsesAndroidExportAndMultilineMessages() {
        val cutoff = dateFormat.parse("6/1/26 12:00 AM")!!.time
        val text = """
            7/10/26, 9:15 AM - Alice: First line
            second line
            7/10/26, 9:16 AM - +1 555 123 4567: Hello
        """.trimIndent()

        val result = parser.parseText(text, "Customer chat", cutoff)

        assertEquals(2, result.messages.size)
        assertEquals("First line\nsecond line", result.messages[0].messageText)
        assertEquals("+1 555 123 4567", result.messages[1].phoneNumber)
        assertEquals(0, result.malformedRecords)
    }

    @Test
    fun excludesRecordsBeforeNinetyDayCutoff() {
        val cutoff = dateFormat.parse("6/1/26 12:00 AM")!!.time
        val text = """
            5/31/26, 11:59 PM - Alice: Too old
            6/1/26, 12:00 AM - Alice: Included
        """.trimIndent()

        val result = parser.parseText(text, "Customer chat", cutoff)

        assertEquals(1, result.messages.size)
        assertEquals("Included", result.messages.single().messageText)
        assertEquals(1, result.recordsOutsideWindow)
    }

    @Test
    fun deduplicatesRepeatedExportRecords() {
        val cutoff = dateFormat.parse("6/1/26 12:00 AM")!!.time
        val line = "7/10/26, 9:15 AM - Alice: Same message"

        val result = parser.parseText("$line\n$line", "Customer chat", cutoff)

        assertEquals(1, result.messages.size)
        assertTrue(result.messages.single().uniqueId.isNotBlank())
    }
}
