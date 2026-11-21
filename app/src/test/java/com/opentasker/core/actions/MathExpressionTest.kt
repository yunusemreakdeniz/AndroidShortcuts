package com.opentasker.core.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MathExpressionTest {
    private fun calc(expression: String) = MathExpression.format(MathExpression.evaluate(expression))

    @Test
    fun followsOperatorPrecedence() {
        assertEquals("14", calc("2 + 3 * 4"))
        assertEquals("20", calc("(2 + 3) * 4"))
        assertEquals("512", calc("2 ^ 3 ^ 2"))
        assertEquals("-9", calc("-3 * 3"))
        assertEquals("1", calc("10 % 3"))
    }

    @Test
    fun keepsDecimalsAndAcceptsCommaAndSymbols() {
        assertEquals("2.5", calc("5 / 2"))
        assertEquals("3.75", calc("1,5 × 2,5"))
        assertEquals("4", calc("8 ÷ 2"))
        assertEquals("0.1", calc("0.3 - 0.2"))
    }

    @Test
    fun rejectsBadInput() {
        assertThrows(IllegalArgumentException::class.java) { MathExpression.evaluate("1 / 0") }
        assertThrows(IllegalArgumentException::class.java) { MathExpression.evaluate("2 +") }
        assertThrows(IllegalArgumentException::class.java) { MathExpression.evaluate("(1 + 2") }
        assertThrows(IllegalArgumentException::class.java) { MathExpression.evaluate("rm -rf") }
        assertThrows(IllegalArgumentException::class.java) { MathExpression.evaluate("1 2") }
    }
}
