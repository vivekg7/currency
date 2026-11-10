package com.crylo.currency.core

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10

/**
 * Formats amounts the way each currency is actually written: JPY has no decimals, KWD
 * has three, most have two (ISO 4217 minor units).
 *
 * One exception: an amount below 1 keeps three significant digits even if that means
 * more decimals than the currency has, because "1 IDR = 0.00 USD" is useless while
 * "0.0000597" is the answer the user wanted. Trailing zeros beyond the currency's own
 * decimals are trimmed.
 */
object MoneyFormat {
    private const val MAX_DIGITS = 8

    /** Max fraction digits for [value] in a currency with [decimals] minor units. */
    fun fractionDigits(value: Double, decimals: Int): Int {
        val a = abs(value)
        if (a == 0.0 || a >= 1.0 || !a.isFinite()) return decimals
        val leadingZeros = -floor(log10(a)).toInt() - 1
        return (leadingZeros + 3).coerceAtLeast(decimals).coerceAtMost(MAX_DIGITS)
    }

    /** Grouped, locale-aware display form, e.g. `1,234.50` or `1.234,50`. */
    fun format(value: Double, code: String, locale: Locale): String =
        formatter(value, Currencies.decimals(code), locale, grouping = true).format(value)

    /**
     * Ungrouped form used when a converted row becomes the one being edited, so the
     * expression field starts from a value the parser reads back exactly.
     */
    fun plain(value: Double, code: String, locale: Locale): String =
        formatter(value, Currencies.decimals(code), locale, grouping = false).format(value)

    /** A rate like `1 USD = 88.6800 INR`: four decimals, more for small rates. */
    fun rate(value: Double, locale: Locale): String =
        formatter(value, 4, locale, grouping = true).format(value)

    private fun formatter(value: Double, decimals: Int, locale: Locale, grouping: Boolean): DecimalFormat {
        val f = NumberFormat.getNumberInstance(locale) as DecimalFormat
        f.decimalFormatSymbols = DecimalFormatSymbols.getInstance(locale)
        f.roundingMode = RoundingMode.HALF_UP
        f.isGroupingUsed = grouping
        f.minimumFractionDigits = decimals
        f.maximumFractionDigits = fractionDigits(value, decimals)
        return f
    }
}
