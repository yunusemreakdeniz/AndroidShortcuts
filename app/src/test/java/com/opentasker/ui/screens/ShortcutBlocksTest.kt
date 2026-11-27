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

    @Test
    fun aMenuGetsOneBranchPerItem() {
        val menu = ActionSpec(type = "menu.choose", args = mapOf("items" to "Kahve\nÇay\n\nA == B"))
        val result = appendWithBlockCloser(emptyList(), menu)
        assertEquals(
            listOf("menu.choose", "flow.if", "flow.endif", "flow.if", "flow.endif", "flow.if", "flow.endif"),
            result.map { it.type },
        )
        assertEquals("{{ Choice }} == Kahve", result[1].args["condition"])
        assertEquals("{{ Choice }} == Çay", result[3].args["condition"])
        assertEquals("{{ Choice_index }} == 3", result[5].args["condition"])
    }

    @Test
    fun aRenamedMenuOutputIsUsed() {
        val menu = ActionSpec(type = "menu.choose", args = mapOf("items" to "Evet", "var" to "Answer"))
        assertEquals("{{ Answer }} == Evet", appendWithBlockCloser(emptyList(), menu)[1].args["condition"])
    }

    @Test
    fun stepsInsideBlocksAreIndented() {
        val types = listOf("input.ask", "flow.if", "log", "flow.foreach", "log", "flow.endfor", "flow.else", "log", "flow.endif", "log")
        assertEquals(listOf(0, 0, 1, 1, 2, 1, 0, 1, 0, 0), blockDepths(types.map { ActionSpec(type = it) }))
    }

    @Test
    fun anUnbalancedEndNeverGoesNegative() {
        assertEquals(listOf(0, 0), blockDepths(listOf(ActionSpec(type = "flow.endif"), ActionSpec(type = "log"))))
    }
}
