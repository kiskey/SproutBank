package com.sproutbank.app.data

import com.sproutbank.app.rust.DailyPrice
import java.time.LocalDate

/**
 * Bundled / offline market data source. In production this would fetch
 * Yahoo Finance histories; here we delegate to the Rust seed for a
 * deterministic, today-aware baseline so new credits settle immediately.
 */
object MarketData {
    private val tracked = listOf("VOO", "SCHG", "VT", "VTI")

    fun trackedEtfs(): List<String> = tracked

    fun pricesFor(symbol: String, days: Int = 365): List<DailyPrice> {
        val end = LocalDate.now()
        val start = end.minusDays(days.toLong())
        return com.sproutbank.app.rust.seedOfflinePrices(
            symbol, start.toString(), end.toString()
        )
    }

    /** Live fetch is stubbed; the UI's manual "Sync" calls into this later. */
    suspend fun refresh(_symbol: String) { /* no-op */ }
}
