package com.crylo.currency.core

import java.time.LocalDate

/**
 * Every URL the app ever requests. Neither service needs an API key or an account.
 *
 * Frankfurter (v2) is primary: it blends ~40 central-bank feeds into 160+ fiat
 * currencies and has decades of daily history, which the chart needs. fawazahmed0's
 * exchange-api is the fallback for latest rates: different infrastructure (a static
 * file on a CDN), similar coverage, but history only as one file per day, so it is not
 * used for the chart.
 */
object Endpoints {
    const val USER_AGENT = "Currency/1.0"
    private const val FRANKFURTER = "https://api.frankfurter.dev/v2"

    const val FRANKFURTER_LATEST = "$FRANKFURTER/rates?base=EUR"

    /** jsDelivr first, then the project's documented Cloudflare Pages mirror. */
    val EXCHANGE_API_LATEST = listOf(
        "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/eur.json",
        "https://latest.currency-api.pages.dev/v1/currencies/eur.json",
    )

    fun frankfurterSeries(base: String, quote: String, range: HistoryRange, today: LocalDate): String {
        val group = range.group?.let { "&group=$it" }.orEmpty()
        return "$FRANKFURTER/rates?base=$base&quotes=$quote&from=${range.from(today)}$group"
    }
}
