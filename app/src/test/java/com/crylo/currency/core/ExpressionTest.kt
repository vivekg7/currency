package com.crylo.currency.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionTest {
    private val en = Expression('.')
    private val de = Expression(',')

    private fun eval(s: String, e: Expression = en) = e.evaluate(s)!!

    @Test fun plainNumbers() {
        assertEquals(1200.0, eval("1200"), 0.0)
        assertEquals(0.5, eval(".5"), 0.0)
        assertEquals(12.75, eval(" 12.75 "), 0.0)
    }

    @Test fun precedenceAndAssociativity() {
        assertEquals(3650.0, eval("1200*3+50"), 1e-9)
        assertEquals(1250.0, eval("50+1200"), 1e-9)
        assertEquals(2.0, eval("8/2/2"), 1e-9)
        assertEquals(5.0, eval("10-3-2"), 1e-9)
        assertEquals(14.0, eval("2+3*4"), 1e-9)
    }

    @Test fun parenthesesAndUnaryMinus() {
        assertEquals(20.0, eval("(2+3)*4"), 1e-9)
        assertEquals(-5.0, eval("-5"), 1e-9)
        assertEquals(5.0, eval("--5"), 1e-9)
        assertEquals(-20.0, eval("-(2+3)*4"), 1e-9)
        assertEquals(6.0, eval("3*-(-2)"), 1e-9)
    }

    @Test fun keypadSymbolsAndPercent() {
        assertEquals(3650.0, eval("1200×3+50"), 1e-9)
        assertEquals(4.0, eval("12÷3"), 1e-9)
        assertEquals(7.0, eval("10−3"), 1e-9)
        assertEquals(15.0, eval("10%*150"), 1e-9)
    }

    @Test fun groupingSeparatorsAreIgnored() {
        assertEquals(1200.5, eval("1,200.50"), 1e-9)
        assertEquals(1200.5, eval("1.200,50", de), 1e-9)
        assertEquals(2.5, eval("2,5", de), 1e-9)
    }

    @Test fun malformedInputIsNull() {
        for (bad in listOf("", "   ", "1+", "*2", "(1+2", "1+2)", "1..2", ".", "abc", "1/0", "2(3)", "()")) {
            assertNull(bad, en.evaluate(bad))
        }
    }

    @Test fun lenientEvaluationWhileTyping() {
        assertEquals(1200.0, en.evaluateLenient("1200*")!!, 1e-9)
        assertEquals(14.0, en.evaluateLenient("2*(3+4")!!, 1e-9)
        assertEquals(2.0, en.evaluateLenient("2*(")!!, 1e-9)
        assertEquals(5.0, en.evaluateLenient("5 + ")!!, 1e-9)
        assertNull(en.evaluateLenient(""))
        assertNull(en.evaluateLenient("-"))
    }

    @Test fun operatorDetection() {
        assertFalse(en.hasOperator("1200"))
        assertFalse(en.hasOperator("-12"))
        assertTrue(en.hasOperator("1200*3"))
        assertTrue(en.hasOperator("(5)"))
    }
}
