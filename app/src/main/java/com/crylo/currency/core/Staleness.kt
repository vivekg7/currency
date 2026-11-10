package com.crylo.currency.core

import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Decides whether cached rates are worth re-downloading when the app opens.
 *
 * Both sources publish once a day, so a snapshot dated D is superseded once day D+1
 * starts (UTC). After that the app retries at most hourly until the source actually
 * has the new day, and unconditionally after a full cycle. That keeps rates current
 * without a request on every open, and never hammers the source on days (weekends,
 * holidays) when nothing new is published.
 */
object Staleness {
    val CYCLE: Duration = Duration.ofHours(24)
    val MIN_RETRY: Duration = Duration.ofHours(1)

    fun needsRefresh(snapshot: RateSnapshot?, now: Instant): Boolean {
        if (snapshot == null) return true
        val sinceFetch = Duration.between(Instant.ofEpochMilli(snapshot.fetchedAt), now)
        // A fetch "in the future" means the clock was changed; trust nothing.
        if (sinceFetch.isNegative) return true
        if (sinceFetch >= CYCLE) return true
        if (sinceFetch < MIN_RETRY) return false
        val nextPublication = snapshot.date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        return !now.isBefore(nextPublication)
    }

    /** Old enough to warn about in the UI: more than two cycles behind today. */
    fun isOutdated(snapshot: RateSnapshot, now: Instant): Boolean {
        val today = now.atZone(ZoneOffset.UTC).toLocalDate()
        return snapshot.date.plusDays(2).isBefore(today)
    }
}
