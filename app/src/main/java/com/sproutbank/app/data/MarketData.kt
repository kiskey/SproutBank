package com.sproutbank.app.data

import android.content.Context
import com.sproutbank.app.rust.DailyPrice
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

object MarketData {
    private val tracked = listOf("VOO", "SCHG", "VT", "VTI")
    private val cache = mutableMapOf<String, List<DailyPrice>>()
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    private lateinit var diskCacheDir: File

    fun init(context: Context) {
        diskCacheDir = File(context.cacheDir, "market").also { it.mkdirs() }
    }

    fun trackedEtfs(): List<String> = tracked

    fun pricesFor(symbol: String, days: Int = 365): List<DailyPrice> {
        cache[symbol]?.let { if (it.isNotEmpty()) return it }
        val disk = readDisk(symbol)
        if (disk.isNotEmpty()) { cache[symbol] = disk; return disk }
        // Deterministic seed only if nothing else is available yet.
        return com.sproutbank.app.rust.seedOfflinePrices(
            symbol,
            LocalDate.now().minusDays(days.toLong()).toString(),
            LocalDate.now().toString(),
        ).also { cache[symbol] = it }
    }

    suspend fun refresh(symbol: String) = withContext(Dispatchers.IO) {
        val end = Instant.now().epochSecond
        val start = Instant.now().minusSeconds(365L * 86_400).epochSecond
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol" +
                  "?period1=$start&period2=$end&interval=1d&events=history"
        runCatching {
            val resp: ChartResponse = client.get(url).body()
            val r = resp.chart.result?.firstOrNull() ?: return@runCatching
            val ts = r.timestamp ?: return@runCatching
            val closes = r.indicators.quote.firstOrNull()?.close ?: return@runCatching
            val rows = ts.mapIndexedNotNull { i, t ->
                val c = closes.getOrNull(i) ?: return@mapIndexedNotNull null
                DailyPrice(
                    symbol = symbol,
                    date = Instant.ofEpochSecond(t).atZone(ZoneOffset.UTC).toLocalDate().toString(),
                    closeCents = (c * 100).toLong(),
                )
            }.sortedBy { it.date }
            if (rows.isNotEmpty()) {
                cache[symbol] = rows
                writeDisk(symbol, rows)
            }
        }
    }

    private fun cacheFile(symbol: String) = File(diskCacheDir, "$symbol.json")
    private fun readDisk(symbol: String): List<DailyPrice> = runCatching {
        val f = cacheFile(symbol)
        if (!f.exists()) return emptyList()
        Json.decodeFromString<List<DailyPrice>>(f.readText())
    }.getOrDefault(emptyList())

    private fun writeDisk(symbol: String, rows: List<DailyPrice>) {
        runCatching { cacheFile(symbol).writeText(Json.encodeToString(rows)) }
    }

    // ---- Yahoo response shapes -----------------------------------------
    @Serializable private data class ChartResponse(val chart: Chart)
    @Serializable private data class Chart(val result: List<Result>? = null)
    @Serializable private data class Result(
        val timestamp: List<Long>? = null,
        val indicators: Indicators,
    )
    @Serializable private data class Indicators(
        val quote: List<Quote> = emptyList(),
    )
    @Serializable private data class Quote(
        val close: List<Double?> = emptyList(),
    )
}
