package com.retirement.logic

import com.retirement.defaultFormConfig
import com.retirement.model.*
import com.retirement.presetConfigs
import com.retirement.runTraced
import kotlin.math.abs
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

/** T004-040: breakdown amounts match the results table (SC004-002); a breakdown is fast enough (SC004-005). */
class BreakdownConsistencyTest {

    private fun assertMoney(expected: Double, actual: Double, message: String) =
        assertTrue(abs(expected - actual) < 0.01, "$message: expected $expected, got $actual")

    @Test
    fun `every amount shared with the results row matches, for every year and preset`() {
        (listOf(defaultFormConfig()) + presetConfigs()).forEach { config ->
            val (result, trace) = runTraced(config)
            result.yearlyResults.drop(1).forEach { row ->
                val where = "${config.name} ${row.year}"
                val b = BreakdownGenerator.generate(config, result, trace, row.age)
                val cf = row.cashFlow
                val e = b.expenses!!
                listOf("Needs" to cf.needs, "Wants" to cf.wants, "Healthcare" to cf.healthcare,
                    "Property Tax" to cf.propertyTax, "Income Tax" to cf.incomeTax).forEach { (label, value) ->
                    assertMoney(value, e.items.single { it.label == label }.amount, "$where $label")
                }
                assertMoney(cf.totalExpenses, e.total, "$where total expenses")
                assertMoney(cf.oneTimeExpenses, e.oneTimeExpenses.sumOf { it.amount }, "$where one-time")
                val i = b.income!!
                assertMoney(cf.totalIncome, i.totalIncome, "$where total income")
                assertMoney(cf.interest, i.interest.amount, "$where interest")
                assertMoney(cf.dividends, i.dividends.amount, "$where dividends")
                row.metrics.annualIncomeGap?.let { assertMoney(it, b.incomeGap!!.actual!!.gap, "$where gap") }
                row.metrics.sbCap?.let { assertMoney(it, b.incomeGap!!.sbCap!!.amount, "$where SB cap") }
                assertMoney(cf.incomeTax, b.taxes!!.billThisYear.tax, "$where tax")
                b.moneyMovement!!.ledgers.forEach { l ->
                    val end = when (l.account) {
                        Endpoint.SB -> row.balances.sb; Endpoint.CBB -> row.balances.cbb; Endpoint.TBA -> row.balances.tba
                        Endpoint.TDA -> row.balances.tda; else -> row.balances.tfa
                    }
                    assertMoney(end, l.closing[3], "$where ${l.account} year end")
                }
            }
        }
    }

    @Test
    fun `a breakdown for a 60-year plan takes well under a second`() {
        val config = defaultFormConfig().copy(currentAge = 50, endAge = 110, retirementAge = 55)
        repeat(3) { runTraced(config) } // warm up
        // Same work as POST /api/simulate/breakdown: a traced simulation, then one year's breakdown
        val elapsed = measureTimeMillis {
            val (result, trace) = runTraced(config)
            BreakdownGenerator.generate(config, result, trace, 80)
        }
        assertTrue(elapsed < 1000, "breakdown took ${elapsed}ms (SC004-005 budget is 1s including the browser)")
    }
}
