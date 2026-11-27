package com.opentasker.core.actions

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageLinksTest {
    @Test
    fun phoneNumbersKeepOnlyDigits() {
        assertEquals("905550000000", MessageLinks.digits("+90 555 000 00 00"))
        assertEquals("", MessageLinks.digits(""))
    }
}
