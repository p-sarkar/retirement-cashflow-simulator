package com.retirement.logic

import com.retirement.data.HistoricalDataService
import com.retirement.data.HistoricalYear
import com.retirement.model.MonteCarloPath
import com.retirement.testConfig
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MonteCarloEngineTest {

    private val config = testConfig()
    private val history = HistoricalDataService.getHistoricalYears()

    @Test
    fun `historical data is available for bootstrapping`() {
        assertTrue(history.size > 80, "expected years since 1928, got ${history.size}")
        val year2008 = history.single { it.year == 2008 }
        assertEquals(-0.385, year2008.equityReturn, 0.01) // S&P 500 price index, Dec 2007 to Dec 2008
    }

    @Test
    fun `the same seed always gives the same analysis`() = runBlocking {
        val a = MonteCarloEngine.run(config, runs = 50, seed = 42)
        val b = MonteCarloEngine.run(config, runs = 50, seed = 42)
        assertEquals(a.paths, b.paths)
        assertEquals(a.statistics, b.statistics)
    }

    @Test
    fun `different seeds give different runs`() = runBlocking {
        val a = MonteCarloEngine.run(config, runs = 20, seed = 1)
        val b = MonteCarloEngine.run(config, runs = 20, seed = 2)
        assertNotEquals(a.paths.map { it.endingBalance }, b.paths.map { it.endingBalance })
    }

    @Test
    fun `1000 runs, one path per run, each through End Age or its failure year`() = runBlocking {
        val result = MonteCarloEngine.run(config, runs = 1000, seed = 7)

        assertEquals(1000, result.paths.size)
        assertEquals((0 until 1000).toList(), result.paths.map { it.runIndex })
        assertEquals((2025..2050).toList(), result.years)
        result.paths.forEach { path ->
            if (path.isSuccess) assertEquals(26, path.totalBalances.size)
            else assertEquals(path.failureYear!! - 2025 + 1, path.totalBalances.size)
        }
        // Runs vary: not every run ends at the same balance
        assertTrue(result.paths.map { it.endingBalance }.toSet().size > 900)
    }

    @Test
    fun `1000 runs finish within the 8 second budget (SC002-002)`() = runBlocking {
        MonteCarloEngine.run(config, runs = 100, seed = 1) // warm up the JVM
        val started = System.nanoTime()
        MonteCarloEngine.run(config, runs = 1000, seed = 3)
        val seconds = (System.nanoTime() - started) / 1e9
        assertTrue(seconds < 8.0, "took $seconds s")
    }

    @Test
    fun `the confidence lines are ordered median, 75 percent, 90 percent`() = runBlocking {
        val stats = MonteCarloEngine.run(config, runs = 300, seed = 11).statistics
        assertEquals(26, stats.medianPath.size)
        for (i in stats.medianPath.indices) {
            assertTrue(stats.medianPath[i] >= stats.percentile75Path[i])
            assertTrue(stats.percentile75Path[i] >= stats.percentile90Path[i])
        }
        assertTrue(stats.successRate in 0.0..1.0)
    }

    @Test
    fun `scenarios are re-centred on the configured growth and inflation`() {
        val returns = mutableListOf<Double>()
        val inflation = mutableListOf<Double>()
        repeat(400) { i ->
            val scenario = MonteCarloEngine.generateScenario(config, seed = 5, runIndex = i, history = history)
            returns += scenario.equityReturns.drop(1)
            inflation += scenario.inflationRates.drop(1)
        }
        assertEquals(0.10, returns.average(), 0.01)
        assertEquals(0.03, inflation.average(), 0.005)
        assertTrue(returns.min() < -0.2 && returns.max() > 0.3, "keeps the historical spread")
    }

    @Test
    fun `scenario variables line up with the simulated years`() {
        val scenario = MonteCarloEngine.generateScenario(config, seed = 5, runIndex = 0, history = history)
        assertEquals((2025..2050).toList(), scenario.years)
        listOf(scenario.equityReturns, scenario.inflationRates, scenario.bondYields).forEach { assertEquals(26, it.size) }
        assertEquals(config.rates.bondYield, scenario.bondYields[0]) // start year holds the config rates
        assertTrue(scenario.bondYields.all { it >= 0.0 })
    }

    @Test
    fun `without historical data scenarios are still generated`() {
        val scenario = MonteCarloEngine.generateScenario(config, seed = 5, runIndex = 0, history = emptyList<HistoricalYear>())
        assertEquals(26, scenario.equityReturns.size)
        assertTrue(MonteCarloEngine.describeMethod(emptyList()).contains("historical data was not available"))
    }

    @Test
    fun `run details rebuild exactly the run from the analysis`() = runBlocking {
        val analysis = MonteCarloEngine.run(config, runs = 30, seed = 99)
        val detail = MonteCarloEngine.runDetail(config, seed = 99, runIndex = 17)

        assertEquals(analysis.paths[17], detail.path)
        assertEquals(detail.path.totalBalances, detail.result.yearlyResults.map { it.balances.total() })
        assertEquals(detail.scenario.equityReturns.drop(1).average(), detail.path.averageReturn, 1e-12)
    }

    @Test
    fun `percentile interpolates between ranks`() {
        val values = listOf(40.0, 10.0, 30.0, 20.0, 50.0)
        assertEquals(10.0, MonteCarloEngine.percentile(values, 0.0))
        assertEquals(30.0, MonteCarloEngine.percentile(values, 0.5))
        assertEquals(50.0, MonteCarloEngine.percentile(values, 1.0))
        assertEquals(15.0, MonteCarloEngine.percentile(values, 0.125))
    }

    @Test
    fun `a failed run counts as zero after it fails`() {
        fun path(i: Int, balances: List<Double>, success: Boolean) =
            MonteCarloPath(i, balances, success, if (success) null else 2026, balances.last(), 0.0, 0.0, 0.0)
        val stats = MonteCarloEngine.statistics(
            listOf(path(0, listOf(100.0, 200.0, 300.0), true), path(1, listOf(100.0, 50.0), false)),
            yearCount = 3
        )
        assertEquals(150.0, stats.medianPath[2]) // median of 300 and 0
        assertEquals(0.5, stats.successRate)
    }

    // --- Risk-Based Guardrails (spec 006, FR006-021) ---

    @Test
    fun `a guardrails analysis adds spending percentiles and the share of runs with a cut`() = runBlocking {
        val g = com.retirement.guardrailsConfig()
        val a = MonteCarloEngine.run(g, runs = 30, seed = 7)
        assertTrue(a.paths.all { it.hadSpendingCut != null })
        val spending = kotlin.test.assertNotNull(a.statistics.spending)
        listOf(spending.percentile10Path, spending.medianPath, spending.percentile90Path).forEach { assertEquals(a.years.size, it.size) }
        a.years.forEachIndexed { i, year ->
            val age = g.currentAge + (year - g.currentYear)
            if (age <= g.retirementAge) {
                assertEquals(null, spending.medianPath[i], "working year $year")
            } else if (spending.medianPath[i] != null) {
                assertTrue(spending.percentile10Path[i]!! <= spending.medianPath[i]!! && spending.medianPath[i]!! <= spending.percentile90Path[i]!!)
            }
        }
        assertTrue(spending.medianPath[a.years.indexOf(g.currentYear + g.retirementAge - g.currentAge + 1)] != null)
        assertEquals(a.paths.count { it.hadSpendingCut == true }.toDouble() / a.paths.size, spending.shareOfRunsWithCut)

        val again = MonteCarloEngine.run(g, runs = 30, seed = 7)
        assertEquals(a.paths, again.paths)
        assertEquals(a.statistics, again.statistics)
        assertEquals(a.paths[3], MonteCarloEngine.runDetail(g, seed = 7, runIndex = 3).path)
    }

    @Test
    fun `the existing strategy has no spending statistics`() = runBlocking {
        val a = MonteCarloEngine.run(config, runs = 10, seed = 7)
        assertEquals(null, a.statistics.spending)
        assertTrue(a.paths.all { it.hadSpendingCut == null })
    }

    @Test
    fun `no run fails with more than a year's expenses left in its accounts (FR002-005)`() = runBlocking {
        // Before 2026-10-19 a downturn could pick an empty CBB and move nothing, failing runs that still held millions
        val base = com.retirement.defaultFormConfig().let {
            it.copy(marketPresetId = "crash-2008", rates = it.rates.copy(preRetirementGrowth = 0.10, postRetirementGrowth = 0.10))
        }
        for (type in listOf("PARTHA_V0_01_20250105", "RISK_BASED_GUARDRAILS_V1")) {
            val config = base.copy(strategy = base.strategy.copy(type = type))
            val result = MonteCarloEngine.run(config, runs = 300, seed = 42, history = history)
            result.paths.filter { !it.isSuccess }.forEach { path ->
                val detail = MonteCarloEngine.runDetail(config, seed = 42, runIndex = path.runIndex, history = history)
                val failed = detail.result.yearlyResults.last()
                val left = failed.balances.let { it.cbb + it.tba + it.tda + it.tfa }
                assertTrue(left <= failed.cashFlow.totalExpenses,
                    "$type run ${path.runIndex} failed in ${failed.year} with ${"%,.0f".format(left)} left (a year's expenses: ${"%,.0f".format(failed.cashFlow.totalExpenses)})")
            }
        }
    }
}

