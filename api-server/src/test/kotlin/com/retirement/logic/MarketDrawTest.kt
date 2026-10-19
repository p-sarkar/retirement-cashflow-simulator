package com.retirement.logic

import com.retirement.data.HistoricalDataService
import com.retirement.defaultFormConfig
import com.retirement.model.MarketScenario
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Moving the per-year draw into MarketDraw (research R006-3) must not change any Monte Carlo scenario:
 * the snapshot was taken from MonteCarloEngine before the move. GOLDEN_UPDATE=1 rewrites it.
 */
class MarketDrawTest {

    private val file = File("src/test/resources/golden/006/scenarios.json")
    private val serializer = ListSerializer(MarketScenario.serializer())

    private fun scenarios(): List<MarketScenario> {
        val config = defaultFormConfig()
        val history = HistoricalDataService.getHistoricalYears()
        assertTrue(history.isNotEmpty(), "historical data must load for this test")
        return listOf(history, emptyList()).flatMap { h ->
            listOf(1L, 42L, 20261019L).flatMap { seed ->
                (0..4).map { runIndex -> MonteCarloEngine.generateScenario(config, seed, runIndex, h) }
            }
        }
    }

    @Test
    fun `Monte Carlo scenarios are unchanged by the MarketDraw extraction`() {
        val actual = scenarios()
        if (System.getenv("GOLDEN_UPDATE") == "1") {
            file.parentFile.mkdirs()
            file.writeText(Json.encodeToString(serializer, actual))
            return
        }
        assertTrue(file.exists(), "snapshot ${file.path} is missing (run with GOLDEN_UPDATE=1 on unchanged code)")
        assertEquals(Json.decodeFromString(serializer, file.readText()), actual)
    }

    @Test
    fun `drawYear is repeatable, floors the return and keeps bond yields non-negative`() {
        val rates = defaultFormConfig().rates.copy(bondYield = 0.0)
        val history = HistoricalDataService.getHistoricalYears()
        val meanReturn = history.map { it.equityReturn }.average()
        val meanInflation = history.map { it.inflation }.average()
        fun draws(seed: Long, growth: Double) = java.util.Random(seed).let { r ->
            List(200) { MarketDraw.drawYear(r, rates, growth, history, meanReturn, meanInflation) }
        }
        assertEquals(draws(7, 0.05), draws(7, 0.05))
        assertTrue(draws(7, -5.0).all { it.equityReturn == MarketDraw.MIN_RETURN })
        assertTrue(draws(7, 0.05).all { it.bondYield >= 0.0 })
    }
}
