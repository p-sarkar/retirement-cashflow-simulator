package com.retirement.logic

import com.retirement.defaultFormConfig
import com.retirement.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** FR002-009: every results row carries the market rates applied in its year, for the table and the CSV (FR002-010a). */
class MarketRatesTest {

    private fun assertRate(expected: Double, actual: Double, message: String) =
        assertTrue(abs(expected - actual) < 1e-9, "$message: expected $expected, got $actual")

    private fun assertRates(expected: MarketRates, actual: MarketRates?, where: String) {
        val rates = assertNotNull(actual, where)
        assertRate(expected.equityReturn, rates.equityReturn, "$where equity return")
        assertRate(expected.inflation, rates.inflation, "$where inflation")
        assertRate(expected.bondYield, rates.bondYield, "$where bond yield")
    }

    /** Each simulated year's quarters carry the same rates as its yearly row. */
    private fun assertQuartersMatchYears(result: SimulationResult) {
        result.quarterlyResults.filter { it.year > result.config.currentYear }.forEach { q ->
            val year = result.yearlyResults.first { it.year == q.year }
            assertEquals(year.market, q.market, "${q.year} Q${q.quarter + 1}")
        }
    }

    @Test
    fun `an interactive run's rows carry the configured rates, pre- or post-retirement growth by year`() {
        val config = defaultFormConfig().let { it.copy(rates = it.rates.copy(preRetirementGrowth = 0.07, postRetirementGrowth = 0.05)) }
        val result = SimulationEngine.runSimulation(config, emptyList(), emptyList(), logExpenses = false)

        // The start year is a balance snapshot: no rates were applied
        assertNull(result.yearlyResults.first().market)
        assertNull(result.quarterlyResults.first().market)

        result.yearlyResults.drop(1).forEach { row ->
            val growth = if (row.age <= config.retirementAge) 0.07 else 0.05
            assertRates(MarketRates(growth, config.rates.inflation, config.rates.bondYield), row.market, "${row.year}")
        }
        assertQuartersMatchYears(result)
    }

    @Test
    fun `rows carry the rates of a supplied sequence, year by year`() {
        val config = defaultFormConfig()
        val years = config.endAge - config.currentAge
        val equity = List(years + 1) { 0.01 * (it % 7) - 0.02 }
        val inflation = List(years + 1) { 0.02 + 0.001 * it }
        val bonds = List(years + 1) { 0.03 + 0.0005 * it }
        val result = SimulationEngine.runSimulation(config, equity, inflation, bonds, logExpenses = false)

        result.yearlyResults.drop(1).forEach { row ->
            val i = row.year - config.currentYear
            assertRates(MarketRates(equity[i], inflation[i], bonds[i]), row.market, "${row.year}")
        }
        assertQuartersMatchYears(result)
    }

    @Test
    fun `a Monte Carlo run's rows carry that run's scenario`() {
        val config = defaultFormConfig()
        val detail = MonteCarloEngine.runDetail(config, seed = 20261017L, runIndex = 3)
        detail.result.yearlyResults.drop(1).forEach { row ->
            val i = row.year - config.currentYear
            with(detail.scenario) {
                assertRates(MarketRates(equityReturns[i], inflationRates[i], bondYields[i]), row.market, "${row.year}")
            }
        }
        assertQuartersMatchYears(detail.result)
    }

    @Test
    fun `a result saved before rows had market rates still loads`() {
        val json = Json { ignoreUnknownKeys = true }
        val result = SimulationEngine.runSimulation(defaultFormConfig(), emptyList(), emptyList(), logExpenses = false)
        val row = json.encodeToJsonElement(YearlyResult.serializer(), result.yearlyResults[1]).jsonObject
        val saved = JsonObject(row - "market")

        assertNull(json.decodeFromJsonElement(YearlyResult.serializer(), saved).market)
    }
}
