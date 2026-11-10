package com.crylo.currency.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class StalenessTest {
    private fun snap(date: String, fetched: String) = RateSnapshot(
        RateSource.FRANKFURTER, LocalDate.parse(date), Instant.parse(fetched).toEpochMilli(), "EUR", mapOf("USD" to 1.1),
    )

    private fun at(s: String) = Instant.parse(s)

    @Test fun nothingCachedNeedsRefresh() = assertTrue(Staleness.needsRefresh(null, at("2026-11-10T12:00:00Z")))

    @Test fun todaysRatesAreFresh() {
        val s = snap("2026-11-10", "2026-11-10T09:00:00Z")
        assertFalse(Staleness.needsRefresh(s, at("2026-11-10T20:00:00Z")))
    }

    @Test fun newDayMeansRefresh() {
        val s = snap("2026-11-10", "2026-11-10T09:00:00Z")
        assertTrue(Staleness.needsRefresh(s, at("2026-11-11T03:00:00Z")))
    }

    @Test fun retriesAreThrottledToHourly() {
        // Fetched after midnight but the source still had yesterday's rates.
        val s = snap("2026-11-10", "2026-11-11T01:00:00Z")
        assertFalse(Staleness.needsRefresh(s, at("2026-11-11T01:30:00Z")))
        assertTrue(Staleness.needsRefresh(s, at("2026-11-11T02:00:00Z")))
    }

    @Test fun fullCycleAlwaysRefreshes() {
        // Dated "tomorrow" in UTC (a provider ahead of us) still refreshes after 24h.
        val s = snap("2026-11-12", "2026-11-10T12:00:00Z")
        assertFalse(Staleness.needsRefresh(s, at("2026-11-11T11:00:00Z")))
        assertTrue(Staleness.needsRefresh(s, at("2026-11-11T12:00:00Z")))
    }

    @Test fun clockMovedBackwardsRefreshes() {
        val s = snap("2026-11-10", "2026-11-10T12:00:00Z")
        assertTrue(Staleness.needsRefresh(s, at("2026-11-09T12:00:00Z")))
    }

    @Test fun outdatedWarning() {
        val s = snap("2026-11-07", "2026-11-07T12:00:00Z")
        assertFalse(Staleness.isOutdated(s, at("2026-11-09T12:00:00Z")))
        assertTrue(Staleness.isOutdated(s, at("2026-11-10T12:00:00Z")))
    }
}
