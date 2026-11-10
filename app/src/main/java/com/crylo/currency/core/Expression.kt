package com.crylo.currency.core

/**
 * A tiny calculator for the amount field: numbers, `+ - * /` (also `× ÷ −`), parentheses,
 * unary minus and a postfix `%` that divides by 100.
 *
 * Hand-written recursive descent rather than a library: the grammar is five lines, and a
 * scripting engine would be a large dependency (and an injection surface) for `1200*3+50`.
 *
 * Grammar:
 * ```
 * expr   := term (('+' | '-') term)*
 * term   := unary (('*' | '/') unary)*
 * unary  := '-' unary | '+' unary | postfix
 * postfix:= atom '%'*
 * atom   := number | '(' expr ')'
 * ```
 *
 * [decimalSeparator] is the locale's; the other of `.`/`,` is treated as a grouping
 * separator and ignored, so `1,200.50` (en) and `1.200,50` (de) both read as 1200.5.
 */
class Expression(private val decimalSeparator: Char = '.') {

    /** Evaluates [input] strictly. Returns null for empty, malformed or non-finite input. */
    fun evaluate(input: String): Double? {
        val tokens = tokenize(input) ?: return null
        if (tokens.isEmpty()) return null
        val parser = Parser(tokens)
        val value = parser.parseExpr() ?: return null
        if (!parser.atEnd()) return null
        return value.takeIf { it.isFinite() }
    }

    /**
     * Evaluates an expression that is still being typed: trailing operators are dropped
     * and unclosed parentheses are closed, so `1200*` shows 1200 and `2*(3+4` shows 14
     * instead of the other rows flickering to nothing mid-keystroke.
     */
    fun evaluateLenient(input: String): Double? {
        var s = input.trim()
        while (s.isNotEmpty() && (s.last() in TRAILING_JUNK || s.last().isWhitespace())) {
            s = s.dropLast(1)
        }
        val open = s.count { it == '(' } - s.count { it == ')' }
        if (open > 0) s += ")".repeat(open)
        return evaluate(s)
    }

    /** True when [input] is more than a plain number, i.e. worth showing "= result" for. */
    fun hasOperator(input: String): Boolean {
        val t = input.trim().removePrefix("-")
        return t.any { it in "+-*/×÷−()%" }
    }

    private sealed interface Token {
        data class Num(val value: Double) : Token
        data class Op(val c: Char) : Token
    }

    private fun tokenize(input: String): List<Token>? {
        val out = mutableListOf<Token>()
        val grouping = if (decimalSeparator == ',') '.' else ','
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c.isWhitespace() || c == ' ' || c == ' ' -> i++
                c.isDigit() || c == decimalSeparator -> {
                    val sb = StringBuilder()
                    var seenDot = false
                    while (i < input.length) {
                        val d = input[i]
                        when {
                            d.isDigit() -> sb.append(d)
                            d == decimalSeparator -> {
                                if (seenDot) return null
                                seenDot = true
                                sb.append('.')
                            }
                            d == grouping -> Unit
                            else -> break
                        }
                        i++
                    }
                    if (sb.toString() == ".") return null
                    out += Token.Num(sb.toString().toDoubleOrNull() ?: return null)
                }
                else -> {
                    val op = when (c) {
                        '+' -> '+'
                        '-', '−', '–' -> '-'
                        '*', '×', 'x', 'X' -> '*'
                        '/', '÷' -> '/'
                        '(', ')', '%' -> c
                        else -> return null
                    }
                    out += Token.Op(op)
                    i++
                }
            }
        }
        return out
    }

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0

        fun atEnd() = pos == tokens.size

        private fun peekOp(): Char? = (tokens.getOrNull(pos) as? Token.Op)?.c

        fun parseExpr(): Double? {
            var left = parseTerm() ?: return null
            while (true) {
                when (peekOp()) {
                    '+' -> { pos++; left += parseTerm() ?: return null }
                    '-' -> { pos++; left -= parseTerm() ?: return null }
                    else -> return left
                }
            }
        }

        private fun parseTerm(): Double? {
            var left = parseUnary() ?: return null
            while (true) {
                when (peekOp()) {
                    '*' -> { pos++; left *= parseUnary() ?: return null }
                    '/' -> {
                        pos++
                        val right = parseUnary() ?: return null
                        if (right == 0.0) return null
                        left /= right
                    }
                    else -> return left
                }
            }
        }

        private fun parseUnary(): Double? = when (peekOp()) {
            '-' -> { pos++; parseUnary()?.let { -it } }
            '+' -> { pos++; parseUnary() }
            else -> parsePostfix()
        }

        private fun parsePostfix(): Double? {
            var v = parseAtom() ?: return null
            while (peekOp() == '%') { pos++; v /= 100.0 }
            return v
        }

        private fun parseAtom(): Double? {
            val t = tokens.getOrNull(pos) ?: return null
            if (t is Token.Num) { pos++; return t.value }
            if (t == Token.Op('(')) {
                pos++
                val v = parseExpr() ?: return null
                if (peekOp() != ')') return null
                pos++
                return v
            }
            return null
        }
    }

    private companion object {
        const val TRAILING_JUNK = "+-*/×÷−–(xX"
    }
}
