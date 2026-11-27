package com.opentasker.core.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlPartsTest {
    @Test
    fun splitsAWebAddress() {
        val parts = UrlParts.parse("https://example.com:8080/a/b?q=kedi%20resmi&x=1#top", "q")!!
        assertEquals("example.com", parts[""])
        assertEquals("https", parts["_scheme"])
        assertEquals("8080", parts["_port"])
        assertEquals("/a/b", parts["_path"])
        assertEquals("q=kedi resmi&x=1", parts["_query"])
        assertEquals("top", parts["_fragment"])
        assertEquals("kedi resmi", parts["_param"])
    }

    @Test
    fun aMissingValueIsEmpty() {
        assertEquals("", UrlParts.parse("https://example.com/", "nope")!!["_param"])
    }

    @Test
    fun rejectsTextThatIsNotAUrl() {
        assertNull(UrlParts.parse("merhaba dünya", ""))
        assertNull(UrlParts.parse("/only/a/path", ""))
    }
}
