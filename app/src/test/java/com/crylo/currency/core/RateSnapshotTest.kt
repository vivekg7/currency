package com.crylo.currency.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RateSnapshotTest {
    // EUR-based, like both sources.
    private val snap = RateSnapshot(
        source = RateSource.FRANKFURTER,
        date = LocalDate.of(2026, 11, 10),
        fetchedAt = 0,
        base = "EUR",
        rates = mapOf("USD" to 1.12, "INR" to 105.0, "JPY" to 177.0, "AED" to 4.1132),
    )

    @Test fun baseToQuoteIsTheRate() {
        assertEquals(105.0, snap.convert(1.0, "EUR", "INR")!!, 1e-12)
    }

    @Test fun quoteToBaseIsTheInverse() {
        assertEquals(100.0, snap.convert(112.0, "USD", "EUR")!!, 1e-9)
    }

    @Test fun crossRateGoesThroughTheBase() {
        // 1 USD = 105 / 1.12 INR
        assertEquals(93.75, snap.convert(1.0, "USD", "INR")!!, 1e-9)
        assertEquals(3.6725, snap.rate("USD", "AED")!!, 1e-9)
    }

    @Test fun roundTripIsIdentity() {
        val inr = snap.convert(250.0, "JPY", "INR")!!
        assertEquals(250.0, snap.convert(inr, "INR", "JPY")!!, 1e-9)
    }

    @Test fun sameCurrencyIsOne() {
        assertEquals(1.0, snap.rate("INR", "INR")!!, 0.0)
        assertEquals(1.0, snap.rate("EUR", "EUR")!!, 0.0)
    }

    @Test fun unknownCurrencyIsNull() {
        assertNull(snap.convert(1.0, "USD", "XYZ"))
        assertNull(snap.rate("XYZ", "EUR"))
    }

    @Test fun seriesStats() {
        val pts = listOf(2.0, 1.0, 4.0, 2.5).mapIndexed { i, r ->
            RatePoint(LocalDate.of(2026, 1, 1).plusDays(i.toLong()), r)
        }
        val s = SeriesStats.of(pts)!!
        assertEquals(1.0, s.min, 0.0)
        assertEquals(4.0, s.max, 0.0)
        assertEquals(25.0, s.changePercent, 1e-9)
        assertNull(SeriesStats.of(emptyList()))
    }
}
