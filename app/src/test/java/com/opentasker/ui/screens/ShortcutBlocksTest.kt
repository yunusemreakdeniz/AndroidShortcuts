package com.opentasker.ui.screens

import com.opentasker.core.model.ActionSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutBlocksTest {
    private val start = listOf(ActionSpec(type = "input.ask"))

    @Test
    fun aRepeatComesWithItsEnd() {
        val result = appendWithBlockCloser(start, ActionSpec(type = "flow.foreach", args = mapOf("values" to "1:5")))
        assertEquals(listOf("input.ask", "flow.foreach", "flow.endfor"), result.map { it.type })
    }

    @Test
    fun ifAndTryAreClosedToo() {
        assertEquals("flow.endif", appendWithBlockCloser(start, ActionSpec(type = "flow.if")).last().type)
        assertEquals("flow.endtry", appendWithBlockCloser(start, ActionSpec(type = "flow.try")).last().type)
    }

    @Test
    fun anOrdinaryStepIsJustAppended() {
        assertEquals(listOf("input.ask", "log"), appendWithBlockCloser(start, ActionSpec(type = "log")).map { it.type })
    }
}
