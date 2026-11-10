package com.crylo.currency.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CurrenciesTest {
    private val all = Currencies.all(Locale.US)

    @Test fun decimalsFollowIso4217() {
        assertEquals(0, Currencies.decimals("JPY"))
        assertEquals(2, Currencies.decimals("INR"))
        assertEquals(3, Currencies.decimals("KWD"))
    }

    @Test fun searchByCodeRanksExactCodeFirst() {
        assertEquals("USD", Currencies.search(all, "usd").first().code)
        assertEquals("USD", Currencies.search(all, "us").first().code)
    }

    @Test fun searchByName() {
        assertTrue(Currencies.search(all, "rupee").any { it.code == "INR" })
        assertTrue(Currencies.search(all, "dirham").any { it.code == "AED" })
    }

    @Test fun searchByCountry() {
        assertEquals("INR", Currencies.search(all, "India").first().code)
        assertTrue(Currencies.search(all, "germany").any { it.code == "EUR" })
        assertTrue(Currencies.search(all, "Saudi").any { it.code == "SAR" })
    }

    @Test fun defaultsPutTheLocaleCurrencyFirst() {
        assertEquals(listOf("INR", "USD", "EUR", "GBP", "AED"), Currencies.defaults(Locale.forLanguageTag("en-IN")))
        assertEquals(listOf("JPY", "INR", "USD", "EUR", "GBP", "AED"), Currencies.defaults(Locale.JAPAN))
        assertEquals(listOf("INR", "USD", "EUR", "GBP", "AED"), Currencies.defaults(Locale.ENGLISH))
    }
}
