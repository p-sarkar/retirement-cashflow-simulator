package com.retirement.logic

import com.retirement.data.HistoricalDataService
import com.retirement.guardrailsConfig
import com.retirement.model.MarketPresets
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * SC006-005: a 30-year guardrails retirement runs interactively in under 3 s, and a 1,000-run Monte Carlo in
 * under 60 s. Tagged "perf": excluded from a plain `./gradlew test` so CI timing noise can't fail a build;
 * run with `./gradlew test -Pperf --tests '*GuardrailsPerformanceTest*'` (research R006-10).
 */
@Tag("perf")
class GuardrailsPerformanceTest {

    private val history = HistoricalDataService.getHistoricalYears()
    // Retire at 64, simulate 65 through 95: 31 yearly reviews
    private val config = guardrailsConfig().let { it.copy(currentAge = 64, retirementAge = 64, endAge = 95, spousal = it.spousal.copy(spouseAge = 62)) }

    @Test
    fun `an interactive 30-year guardrails run takes under 3 seconds`() {
        SimulationEngine.runSimulation(config, MarketPresets.equityReturns(config), emptyList(), history = history) // warm up
        val ms = measureTimeMillis {
            SimulationEngine.runSimulation(config, MarketPresets.equityReturns(config), emptyList(), history = history)
        }
        println("PERF interactive guardrails run: $ms ms")
        assertTrue(ms < 3000, "interactive run took $ms ms")
    }

    @Test
    fun `a 1,000-run guardrails Monte Carlo takes under 60 seconds`() {
        val ms = measureTimeMillis { runBlocking { MonteCarloEngine.run(config, runs = 1000, seed = 42, history = history) } }
        println("PERF 1,000-run guardrails Monte Carlo: $ms ms on ${Runtime.getRuntime().availableProcessors()} processors")
        assertTrue(ms < 60000, "Monte Carlo took $ms ms")
    }
}
