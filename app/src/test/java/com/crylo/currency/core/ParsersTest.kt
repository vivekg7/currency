package com.crylo.currency.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ParsersTest {
    private val allowed = setOf("EUR", "USD", "INR", "AED", "SAR", "JPY")

    @Test fun frankfurterLatest() {
        val body = """
            [{"date":"2026-11-09","base":"EUR","quote":"BTC","rate":0.00001},
             {"date":"2026-11-10","base":"EUR","quote":"AED","rate":4.1171},
             {"date":"2026-11-10","base":"EUR","quote":"INR","rate":105.16},
             {"date":"2026-11-09","base":"EUR","quote":"SAR","rate":4.2},
             {"date":"2026-11-10","base":"EUR","quote":"USD","rate":1.1211},
             {"date":"2026-11-10","base":"EUR","quote":"JPY","rate":0},
             {"date":"2026-11-10","base":"EUR","quote":"EUR","rate":1}]
        """.trimIndent()
        val s = RateParsers.frankfurterLatest(body, allowed, 42L)
        assertEquals(RateSource.FRANKFURTER, s.source)
        assertEquals("EUR", s.base)
        assertEquals(LocalDate.of(2026, 11, 10), s.date)
        assertEquals(42L, s.fetchedAt)
        assertEquals(setOf("AED", "INR", "SAR", "USD"), s.rates.keys) // no crypto, no zero, no base
        assertEquals(105.16, s.rates["INR"]!!, 0.0)
    }

    @Test fun exchangeApiLatest() {
        val body = """
            {"date": "2026-11-10",
             "eur": {"1inch": 14.5, "aed": 4.12337952, "inr": 105.3, "usd": 1.1221, "eur": 1, "sar": "4.2", "jpy": null}}
        """.trimIndent()
        val s = RateParsers.exchangeApiLatest(body, allowed, 7L)
        assertEquals(RateSource.EXCHANGE_API, s.source)
        assertEquals("EUR", s.base)
        assertEquals(LocalDate.of(2026, 11, 10), s.date)
        assertEquals(setOf("AED", "INR", "USD", "SAR"), s.rates.keys)
        assertEquals(4.12337952, s.rates["AED"]!!, 0.0)
    }

    @Test fun series() {
        val body = """
            [{"date":"2021-11-12","base":"USD","quote":"INR","rate":74.284},
             {"date":"2021-11-05","base":"USD","quote":"INR","rate":74.633},
             {"date":"2021-11-19","base":"USD","quote":"INR","rate":-1}]
        """.trimIndent()
        val pts = RateParsers.frankfurterSeries(body)
        assertEquals(listOf(LocalDate.of(2021, 11, 5), LocalDate.of(2021, 11, 12)), pts.map { it.date })
        assertEquals(74.633, pts[0].rate, 0.0)
    }

    @Test fun cacheRoundTrip() {
        val s = RateSnapshot(RateSource.EXCHANGE_API, LocalDate.of(2026, 1, 2), 123456789L, "EUR", mapOf("USD" to 1.1, "INR" to 99.5))
        assertEquals(s, RateParsers.fromCache(RateParsers.toCache(s)))
    }

    @Test fun garbageIsRejected() {
        assertThrows(Exception::class.java) { RateParsers.frankfurterLatest("[]", allowed, 0) }
        assertThrows(Exception::class.java) { RateParsers.frankfurterLatest("{\"status\":422}", allowed, 0) }
        assertThrows(Exception::class.java) { RateParsers.frankfurterLatest("<html>", allowed, 0) }
        assertThrows(Exception::class.java) { RateParsers.exchangeApiLatest("{\"date\":\"2026-01-01\"}", allowed, 0) }
        assertThrows(Exception::class.java) {
            RateParsers.exchangeApiLatest("{\"date\":\"2026-01-01\",\"eur\":{\"btc\":1}}", allowed, 0)
        }
    }

    @Test fun seriesUrl() {
        val today = LocalDate.of(2026, 11, 10)
        assertEquals(
            "https://api.frankfurter.dev/v2/rates?base=USD&quotes=INR&from=2026-10-10",
            Endpoints.frankfurterSeries("USD", "INR", HistoryRange.M1, today),
        )
        assertEquals(
            "https://api.frankfurter.dev/v2/rates?base=USD&quotes=INR&from=2021-11-10&group=week",
            Endpoints.frankfurterSeries("USD", "INR", HistoryRange.Y5, today),
        )
    }

    @Test fun bundledCurrencyTableCoversTheDefaults() {
        for (c in listOf("INR", "USD", "EUR", "GBP", "AED", "SAR", "JPY")) assertTrue(c, c in Currencies.codes)
        assertTrue(Currencies.codes.size >= 150)
        assertFalse("BTC" in Currencies.codes)
    }
}
