package com.whatsappsync.app.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactResolverTest {
    @Test
    fun extractsInternationalNumberFromWhatsAppIdentity() {
        assertEquals("+919876543210", ContactResolver.normalizePhone("+91 98765 43210"))
    }

    @Test
    fun extractsUnsavedLocalNumber() {
        assertEquals("9876543210", ContactResolver.normalizePhone("98765-43210"))
    }

    @Test
    fun rejectsNamesAndShortCodes() {
        assertNull(ContactResolver.normalizePhone("Rahul Sharma"))
        assertNull(ContactResolver.normalizePhone("12345"))
    }

    @Test
    fun recognizesRawPhoneIdentity() {
        assertTrue(ContactResolver.looksLikePhone("+91 98765 43210"))
    }
}
