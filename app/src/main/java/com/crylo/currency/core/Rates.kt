package com.crylo.currency.core

import java.time.LocalDate

/** Where a set of rates came from. [label] is what the "Rates as of" line shows. */
enum class RateSource(val label: String) {
    FRANKFURTER("Frankfurter"),
    EXCHANGE_API("exchange-api"),
}

/**
 * One day's rates, all quoted against [base]: `rates[X]` is how many X one unit of
 * [base] buys. Any pair converts through the base (a cross rate), so one download of
 * ~160 numbers covers every combination the user can pick.
 */
data class RateSnapshot(
    val source: RateSource,
    val date: LocalDate,
    /** Wall-clock millis when this was downloaded; drives the staleness check. */
    val fetchedAt: Long,
    val base: String,
    val rates: Map<String, Double>,
) {
    fun has(code: String) = code == base || rates.containsKey(code)

    /** Units of [to] per one unit of [from], or null if either is not in this snapshot. */
    fun rate(from: String, to: String): Double? {
        val f = if (from == base) 1.0 else rates[from] ?: return null
        val t = if (to == base) 1.0 else rates[to] ?: return null
        return t / f
    }

    fun convert(amount: Double, from: String, to: String): Double? =
        rate(from, to)?.let { amount * it }
}

data class RatePoint(val date: LocalDate, val rate: Double)

/** Summary numbers shown above the history chart. */
data class SeriesStats(val first: Double, val last: Double, val min: Double, val max: Double) {
    val changePercent: Double get() = if (first == 0.0) 0.0 else (last - first) / first * 100.0

    companion object {
        fun of(points: List<RatePoint>): SeriesStats? {
            if (points.isEmpty()) return null
            return SeriesStats(
                first = points.first().rate,
                last = points.last().rate,
                min = points.minOf { it.rate },
                max = points.maxOf { it.rate },
            )
        }
    }
}

/** Chart ranges. Frankfurter has decades of history, so every range is available. */
enum class HistoryRange(val months: Long, val group: String?) {
    M1(1, null),
    M6(6, null),
    Y1(12, null),
    // ~1800 daily points would be more than the screen has pixels; weekly is plenty.
    Y5(60, "week");

    fun from(today: LocalDate): LocalDate = today.minusMonths(months)
}
