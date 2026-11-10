package com.crylo.currency.data

import android.content.Context
import com.crylo.currency.core.Currencies
import com.crylo.currency.core.Endpoints
import com.crylo.currency.core.HistoryRange
import com.crylo.currency.core.RateParsers
import com.crylo.currency.core.RatePoint
import com.crylo.currency.core.RateSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * Downloads and caches rates. The cache is one small JSON file in app-private storage;
 * after the first successful sync the app never needs the network to convert.
 */
class RateRepository(context: Context) {
    private val cacheFile = File(context.filesDir, "rates.json")
    private val history = ConcurrentHashMap<String, List<RatePoint>>()

    suspend fun cached(): RateSnapshot? = withContext(Dispatchers.IO) {
        runCatching { RateParsers.fromCache(cacheFile.readText()) }.getOrNull()
    }

    /** Primary source, then each fallback URL in turn. Saves whatever succeeds. */
    suspend fun refresh(): RateSnapshot = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val allowed = Currencies.codes
        val attempts: List<() -> RateSnapshot> = listOf<() -> RateSnapshot>(
            { RateParsers.frankfurterLatest(get(Endpoints.FRANKFURTER_LATEST), allowed, now) },
        ) + Endpoints.EXCHANGE_API_LATEST.map { url ->
            { RateParsers.exchangeApiLatest(get(url), allowed, now) }
        }
        var last: Exception? = null
        for (attempt in attempts) {
            try {
                val snapshot = attempt()
                val tmp = File(cacheFile.parentFile, "rates.json.tmp")
                tmp.writeText(RateParsers.toCache(snapshot))
                tmp.renameTo(cacheFile)
                return@withContext snapshot
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IOException("no source")
    }

    /** Daily (weekly for 5Y) history; kept in memory for the session only. */
    suspend fun history(base: String, quote: String, range: HistoryRange): List<RatePoint> =
        withContext(Dispatchers.IO) {
            val today = LocalDate.now()
            val key = "$base/$quote/$range/$today"
            history[key] ?: RateParsers.frankfurterSeries(
                get(Endpoints.frankfurterSeries(base, quote, range, today)),
            ).also { if (it.isNotEmpty()) history[key] = it }
        }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("User-Agent", Endpoints.USER_AGENT)
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code for $url")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
