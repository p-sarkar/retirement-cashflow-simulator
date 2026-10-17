package com.retirement.logic

import com.retirement.defaultFormConfig
import com.retirement.model.*
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** T004-054: the breakdown of one Monte Carlo run explains that run (US004-6, FR004-081, FR004-082, SC004-008). */
class BreakdownMonteCarloTest {

    private val seed = 20261017L

    private fun assertMoney(expected: Double, actual: Double, message: String) =
        assertTrue(abs(expected - actual) < 0.01, "$message: expected $expected, got $actual")

    private fun Portfolio.of(account: Endpoint) = when (account) {
        Endpoint.SB -> sb; Endpoint.CBB -> cbb; Endpoint.TBA -> tba; Endpoint.TDA -> tda; else -> tfa
    }

    private fun assertExplainsRun(config: SimulationConfig, runIndex: Int) {
        val detail = MonteCarloEngine.runDetail(config, seed, runIndex)
        detail.result.yearlyResults.drop(1).forEach { row ->
            val where = "run ${runIndex + 1} ${row.year}"
            val b = assertNotNull(MonteCarloEngine.runBreakdown(config, seed, runIndex, row.age), where)
            val yearIdx = row.year - config.currentYear

            // Amounts shared with the run's results row
            assertMoney(row.cashFlow.totalExpenses, b.expenses!!.total, "$where total expenses")
            assertMoney(row.cashFlow.totalIncome, b.income!!.totalIncome, "$where total income")
            assertMoney(row.cashFlow.incomeTax, b.taxes!!.billThisYear.tax, "$where tax")
            row.metrics.annualIncomeGap?.let { assertMoney(it, b.incomeGap!!.actual!!.gap, "$where gap") }
            b.moneyMovement!!.ledgers.forEach { l ->
                (0..3).forEach { q -> assertMoney(l.opening[q] + l.rows.sumOf { it.amounts[q] }, l.closing[q], "$where ${l.account} Q${q + 1}") }
                assertMoney(row.balances.of(l.account), l.closing[3], "$where ${l.account} year end")
            }

            // The run's own market for this year
            val c = b.startingPosition.conditions
            assertEquals(EquityReturnSource.MONTE_CARLO_SCENARIO, c.equityReturnSource, where)
            assertEquals(runIndex + 1, c.monteCarloRun, where)
            assertMoney(detail.scenario.equityReturns[yearIdx], c.equityReturn, "$where equity return")
            assertTrue(abs(detail.scenario.inflationRates[yearIdx] - c.inflationRate) < 1e-9, "$where inflation")
            assertTrue(abs(detail.scenario.bondYields[yearIdx] - c.bondYield) < 1e-9, "$where bond yield")
        }
    }

    @Test
    fun `a run's breakdown matches that run's results and scenario, year by year`() {
        (0..2).forEach { assertExplainsRun(defaultFormConfig(), it) }
    }

    @Test
    fun `a failing run's breakdown marks the quarter it failed in`() {
        // Half the default plan's balances, so some runs fail
        val config = defaultFormConfig().let { c ->
            c.copy(portfolio = with(c.portfolio) { Portfolio(sb / 2, cbb / 2, tba / 2, tda / 2, tfa / 2) })
        }
        val failing = (0 until 200).first { !MonteCarloEngine.runDetail(config, seed, it).result.summary.isSuccess }
        assertExplainsRun(config, failing)
        val last = MonteCarloEngine.runDetail(config, seed, failing).result.yearlyResults.last()
        val failure = assertNotNull(MonteCarloEngine.runBreakdown(config, seed, failing, last.age)!!.moneyMovement!!.failure)
        assertTrue(failure.quarter in 1..4 && failure.condition.isNotBlank())
    }
}
