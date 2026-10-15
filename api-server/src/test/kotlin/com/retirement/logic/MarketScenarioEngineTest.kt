package com.retirement.logic

import com.retirement.model.Portfolio
import com.retirement.testConfig
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** SimulationEngine with per-year rates (presets, Monte Carlo) and the point-to-point market check. */
class MarketScenarioEngineTest {

    @Test
    fun `cumulative inflation is the product of each year's rate`() {
        val factors = SimulationEngine.cumulativeInflation(0.03, listOf(0.03, 0.10, -0.05), 3)
        assertEquals(1.0, factors[0])
        assertEquals(1.10, factors[1], 1e-12)
        assertEquals(1.10 * 0.95, factors[2], 1e-12)
        assertEquals(1.10 * 0.95 * 1.03, factors[3], 1e-12) // missing years use the config rate
    }

    @Test
    fun `without per-year rates inflation compounds the config rate`() {
        val factors = SimulationEngine.cumulativeInflation(0.03, emptyList(), 2)
        assertEquals(1.03.pow(2), factors[2])
    }

    @Test
    fun `per-year inflation drives expenses`() {
        val config = testConfig()
        val inflation = List(26) { if (it == 1) 0.10 else 0.0 }
        val result = SimulationEngine.runSimulation(config, emptyList(), inflation)

        assertEquals(config.expenses.needs * 1.10, result.yearlyResults[1].cashFlow.needs, 1e-6)
        assertEquals(config.expenses.needs * 1.10, result.yearlyResults[2].cashFlow.needs, 1e-6)
    }

    @Test
    fun `per-year bond yields drive dividends`() {
        val config = testConfig()
        val low = SimulationEngine.runSimulation(config, emptyList(), emptyList(), bondYields = List(26) { 0.01 })
        val high = SimulationEngine.runSimulation(config, emptyList(), emptyList(), bondYields = List(26) { 0.08 })

        // Year 2 dividends are all accrued at that year's yield (year 1 Q1 credits the start year's accrual)
        assertTrue(high.yearlyResults[2].cashFlow.dividends > low.yearlyResults[2].cashFlow.dividends * 4)
    }

    @Test
    fun `a market down more than 5 percent on 12 months ago stops equity sales`() {
        // Retired, with the SB between its floor and cap (so up markets refill it): steady 6% yearly declines stay
        // above the all-time-high check (85%) in the second year, so only the point-to-point 12-month check (95%)
        // can move the strategy to the down market path
        val config = testConfig(
            currentAge = 70, retirementAge = 65,
            portfolio = Portfolio(sb = 80000.0, cbb = 300000.0, tba = 1000000.0, tda = 1000000.0, tfa = 0.0),
            rates = testConfig().rates.copy(postRetirementGrowth = -0.06)
        )
        val result = SimulationEngine.runSimulation(config, emptyList(), emptyList())
        val year2 = result.quarterlyResults.filter { it.year == 2027 }
        val equitySales = { q: com.retirement.model.QuarterlyResult -> q.cashFlow.tbaWithdrawal + q.cashFlow.tdaWithdrawalSpend }

        assertTrue(result.quarterlyResults.filter { it.year == 2026 }.any { equitySales(it) > 0.0 }, "year 1 still sells equities")
        assertEquals(0.0, year2.sumOf(equitySales), "year 2 is a down market on the 12-month check")
    }
}
