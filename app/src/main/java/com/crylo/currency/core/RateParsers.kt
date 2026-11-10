package com.crylo.currency.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.time.LocalDate

/**
 * Turns source responses (and our own cache file) into [RateSnapshot]s. Every function
 * throws on input it cannot make sense of; callers treat that like a network failure.
 *
 * Rates that are missing, zero, negative or not finite are dropped rather than trusted,
 * and codes outside [allowed] (crypto, metals, retired currencies) are ignored.
 */
object RateParsers {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Frankfurter v2 `GET /v2/rates?base=EUR`:
     * `[{"date":"2026-11-10","base":"EUR","quote":"INR","rate":105.16}, ...]`.
     * Different providers can be a day apart; the snapshot is dated by the newest one.
     */
    fun frankfurterLatest(body: String, allowed: Set<String>, fetchedAt: Long): RateSnapshot {
        val items = json.parseToJsonElement(body).jsonArray
        require(items.isNotEmpty()) { "empty response" }
        val base = items.first().jsonObject.string("base").uppercase()
        val rates = HashMap<String, Double>()
        var date: LocalDate? = null
        for (item in items) {
            val o = item.jsonObject
            if (o.string("base").uppercase() != base) continue
            val quote = o.string("quote").uppercase()
            if (quote !in allowed) continue
            val rate = o["rate"]?.jsonPrimitive?.double ?: continue
            if (!rate.isFinite() || rate <= 0.0) continue
            rates[quote] = rate
            val d = LocalDate.parse(o.string("date"))
            if (date == null || d.isAfter(date)) date = d
        }
        return snapshot(RateSource.FRANKFURTER, date, fetchedAt, base, rates)
    }

    /**
     * fawazahmed0 exchange-api `GET /v1/currencies/eur.json`:
     * `{"date":"2026-11-10","eur":{"inr":105.1,"usd":1.12,...}}`. Codes are lowercase.
     */
    fun exchangeApiLatest(body: String, allowed: Set<String>, fetchedAt: Long): RateSnapshot {
        val root = json.parseToJsonElement(body).jsonObject
        val date = LocalDate.parse(root.string("date"))
        val (baseKey, table) = root.entries.firstOrNull { it.value is JsonObject }
            ?: throw IllegalArgumentException("no rate table")
        val rates = HashMap<String, Double>()
        for ((code, v) in table.jsonObject) {
            val c = code.uppercase()
            if (c !in allowed) continue
            val rate = runCatching { v.jsonPrimitive.double }.getOrNull() ?: continue
            if (rate.isFinite() && rate > 0.0) rates[c] = rate
        }
        return snapshot(RateSource.EXCHANGE_API, date, fetchedAt, baseKey.uppercase(), rates)
    }

    /** Frankfurter v2 time series: same shape as latest, one entry per date. */
    fun frankfurterSeries(body: String): List<RatePoint> =
        json.parseToJsonElement(body).jsonArray.mapNotNull { item ->
            val o = item.jsonObject
            val rate = o["rate"]?.jsonPrimitive?.double ?: return@mapNotNull null
            if (!rate.isFinite() || rate <= 0.0) return@mapNotNull null
            RatePoint(LocalDate.parse(o.string("date")), rate)
        }.sortedBy { it.date }

    /** The on-device cache format: our own, so it survives a change of source format. */
    fun toCache(s: RateSnapshot): String = buildJsonObject {
        put("version", 1)
        put("source", s.source.name)
        put("date", s.date.toString())
        put("fetchedAt", s.fetchedAt)
        put("base", s.base)
        putJsonObject("rates") { s.rates.toSortedMap().forEach { (k, v) -> put(k, v) } }
    }.toString()

    fun fromCache(body: String): RateSnapshot {
        val o = json.parseToJsonElement(body).jsonObject
        val rates = o["rates"]!!.jsonObject.mapValues { it.value.jsonPrimitive.double }
        return RateSnapshot(
            source = RateSource.valueOf(o.string("source")),
            date = LocalDate.parse(o.string("date")),
            fetchedAt = o["fetchedAt"]!!.jsonPrimitive.long,
            base = o.string("base"),
            rates = rates,
        )
    }

    private fun snapshot(
        source: RateSource,
        date: LocalDate?,
        fetchedAt: Long,
        base: String,
        rates: Map<String, Double>,
    ): RateSnapshot {
        require(rates.isNotEmpty() && date != null) { "no usable rates" }
        return RateSnapshot(source, date, fetchedAt, base, rates - base)
    }

    private fun JsonObject.string(key: String): String =
        this[key]?.jsonPrimitive?.content ?: throw IllegalArgumentException("missing $key")
}
