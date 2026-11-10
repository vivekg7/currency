package com.crylo.currency.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MoneyFormatTest {
    private val us = Locale.US
    private val de = Locale.GERMANY

    @Test fun twoDecimalCurrency() {
        assertEquals("1,234.50", MoneyFormat.format(1234.5, "USD", us))
        assertEquals("1,234.57", MoneyFormat.format(1234.567, "USD", us))
        assertEquals("0.00", MoneyFormat.format(0.0, "USD", us))
    }

    @Test fun jpyHasNoDecimals() {
        assertEquals("1,235", MoneyFormat.format(1234.5, "JPY", us))
        assertEquals("177", MoneyFormat.format(177.38, "JPY", us))
    }

    @Test fun threeDecimalCurrency() {
        assertEquals("1.235", MoneyFormat.format(1.23456, "KWD", us))
    }

    @Test fun roundsHalfUp() {
        assertEquals("1.13", MoneyFormat.format(1.125, "USD", us))
        assertEquals("3", MoneyFormat.format(2.5, "JPY", us))
    }

    @Test fun smallAmountsKeepThreeSignificantDigits() {
        assertEquals("0.0000597", MoneyFormat.format(0.00005971, "USD", us))
        assertEquals("0.0123", MoneyFormat.format(0.012345, "USD", us))
        assertEquals("0.50", MoneyFormat.format(0.5, "USD", us))
        assertEquals("0.5", MoneyFormat.format(0.5, "JPY", us))
        assertEquals(8, MoneyFormat.fractionDigits(1e-12, 2))
    }

    @Test fun followsLocaleSeparators() {
        assertEquals("1.234,50", MoneyFormat.format(1234.5, "EUR", de))
    }

    @Test fun plainFormIsUngroupedAndReparses() {
        val s = MoneyFormat.plain(1234567.891, "INR", us)
        assertEquals("1234567.89", s)
        assertEquals(1234567.89, Expression('.').evaluate(s)!!, 1e-9)
        assertEquals("1235", MoneyFormat.plain(1234.5, "JPY", de))
        assertEquals("1234,50", MoneyFormat.plain(1234.5, "EUR", de))
    }

    @Test fun unknownCodeFallsBackToTwoDecimals() {
        assertEquals("1.50", MoneyFormat.format(1.5, "ZZZ", us))
    }

    @Test fun rateUsesFourDecimals() {
        assertEquals("88.6800", MoneyFormat.rate(88.68, us))
    }
}
