package com.retirement.logic

import com.retirement.defaultFormConfig
import com.retirement.model.*
import com.retirement.presetConfigs
import com.retirement.runTraced
import com.retirement.testConfig
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** T004-014: the Money Movement section (FR004-050–FR004-055). */
class BreakdownMoneyMovementTest {

    private val accounts = listOf(Endpoint.SB, Endpoint.CBB, Endpoint.TBA, Endpoint.TDA, Endpoint.TFA)

    private fun Portfolio.of(account: Endpoint) = when (account) {
        Endpoint.SB -> sb; Endpoint.CBB -> cbb; Endpoint.TBA -> tba; Endpoint.TDA -> tda; Endpoint.TFA -> tfa
        else -> error("$account is not an account")
    }

    private fun assertMoney(expected: Double, actual: Double, message: String) =
        assertTrue(abs(expected - actual) < 0.01, "$message: expected $expected, got $actual")

    /** Every simulated year's breakdown, for the default plan under every preset */
    private fun breakdowns(): List<Triple<SimulationConfig, YearlyResult, YearBreakdown>> =
        (listOf(defaultFormConfig()) + presetConfigs()).flatMap { config ->
            val (result, trace) = runTraced(config)
            result.yearlyResults.drop(1).map { row -> Triple(config, row, BreakdownGenerator.generate(config, result, trace, row.age)) }
        }

    @Test
    fun `ledgers reconcile every quarter and end at the results-table balances`() {
        breakdowns().forEach { (config, row, b) ->
            val mm = assertNotNull(b.moneyMovement, "${config.name} ${row.year}")
            assertEquals(accounts, mm.ledgers.map { it.account })
            mm.ledgers.forEach { ledger ->
                val where = "${config.name} ${row.year} ${ledger.account}"
                assertMoney(b.startingPosition.openingBalances.of(ledger.account), ledger.opening[0], "$where opening")
                (0..3).forEach { q ->
                    assertMoney(ledger.opening[q] + ledger.rows.sumOf { it.amounts[q] }, ledger.closing[q], "$where Q${q + 1}")
                    if (q < 3) assertMoney(ledger.closing[q], ledger.opening[q + 1], "$where Q${q + 2} opening")
                }
                assertMoney(row.balances.of(ledger.account), ledger.closing[3], "$where year end")
                ledger.rows.forEach { r -> assertMoney(r.amounts.sum(), r.yearTotal, "$where ${r.type} year total") }
            }
        }
    }

    @Test
    fun `flows are positive and add up to each account's ledger`() {
        breakdowns().forEach { (config, row, b) ->
            val mm = b.moneyMovement!!
            mm.flows.forEach { assertTrue(it.amount > 0.0 && it.from != it.to, "${config.name} ${row.year} $it") }
            mm.ledgers.forEach { ledger ->
                val net = mm.flows.filter { it.to == ledger.account }.sumOf { it.amount } -
                    mm.flows.filter { it.from == ledger.account }.sumOf { it.amount }
                assertMoney(ledger.rows.sumOf { it.yearTotal }, net, "${config.name} ${row.year} ${ledger.account} flows")
            }
        }
    }

    @Test
    fun `retirement years explain each quarter's strategy, working years have none`() {
        breakdowns().forEach { (config, row, b) ->
            val strategy = b.moneyMovement!!.quarterStrategy
            if (b.phase == YearPhase.RETIREMENT) {
                assertEquals(listOf(1, 2, 3, 4), strategy.map { it.quarter }, "${config.name} ${row.year}")
                assertTrue(strategy.all { it.explanation.isNotBlank() })
            } else {
                assertTrue(strategy.isEmpty(), "${config.name} ${row.year}")
            }
        }
    }

    @Test
    fun `a failing year marks the quarter and account but still shows four quarters`() {
        val config = testConfig(portfolio = Portfolio(0.0, 0.0, 0.0, 0.0, 0.0)).copy(salary = 150000.0)
        val (result, trace) = runTraced(config)
        val failing = result.yearlyResults.last()
        val mm = BreakdownGenerator.generate(config, result, trace, failing.age).moneyMovement!!
        val failure = assertNotNull(mm.failure)
        assertTrue(failure.quarter in 1..4 && failure.condition.isNotBlank())
        assertTrue(mm.ledgers.all { it.closing.size == 4 })
        assertNull(BreakdownGenerator.generate(config, result, trace, failing.age - 1).moneyMovement!!.failure)
    }

    @Test
    fun `the start-year snapshot has no money movement`() {
        val config = defaultFormConfig()
        val (result, trace) = runTraced(config)
        assertNull(BreakdownGenerator.generate(config, result, trace, config.currentAge).moneyMovement)
    }
}
