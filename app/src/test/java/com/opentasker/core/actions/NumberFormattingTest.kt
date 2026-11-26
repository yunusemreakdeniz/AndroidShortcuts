package com.opentasker.core.actions

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberFormattingTest {
    private val tr = Locale.forLanguageTag("tr-TR")

    @Test
    fun turkishUsesDotGroupingAndCommaDecimals() {
        assertEquals("1.234,50", NumberFormatting.format("1234.5", 2, true, tr))
    }

    @Test
    fun englishAndNoGrouping() {
        assertEquals("1,234.57", NumberFormatting.format("1234.567", 2, true, Locale.US))
        assertEquals("1234", NumberFormatting.format("1234.4", 0, false, Locale.US))
    }

    @Test
    fun acceptsCommaDecimalInput() {
        assertEquals("3,14", NumberFormatting.format("3,14159", 2, false, tr))
    }

    @Test
    fun rejectsBadInput() {
        assertNull(NumberFormatting.format("abc", 2, true, tr))
        assertNull(NumberFormatting.format("1", 11, true, tr))
    }
}
