package com.opentasker.core.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DictionariesTest {
    @Test
    fun startsANewDictionaryAndKeepsTypes() {
        var d = Dictionaries.set(null, "ad", "Ayşe")!!
        d = Dictionaries.set(d, "yaş", "30")!!
        d = Dictionaries.set(d, "aktif", "true")!!
        d = Dictionaries.set(d, "etiketler", "[\"a\",\"b\"]")!!
        assertEquals("{\"ad\":\"Ayşe\",\"yaş\":30,\"aktif\":true,\"etiketler\":[\"a\",\"b\"]}", d)
    }

    @Test
    fun anEmptyValueRemovesTheKey() {
        assertEquals("{\"b\":2}", Dictionaries.set("{\"a\":1,\"b\":2}", "a", ""))
    }

    @Test
    fun refusesTextThatIsNotADictionary() {
        assertNull(Dictionaries.set("merhaba", "a", "1"))
        assertNull(Dictionaries.set("[1,2]", "a", "1"))
    }
}
