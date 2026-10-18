package com.retirement.logic

import com.retirement.model.*
import com.retirement.testConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** #23: once TDA and TBA can't pay for spending, the Tax Free account does, before the plan fails (FR002-005, FR002-008). */
class TfaLastResortTest {

    // Retired, with little in TDA and TBA and most of the savings in the Roth account
    private fun config(tfa: Double) = testConfig(
        currentAge = 66, retirementAge = 65,
        portfolio = Portfolio(sb = 20000.0, cbb = 100000.0, tba = 50000.0, tda = 50000.0, tfa = tfa)
    )

    private fun run(config: SimulationConfig, trace: SimulationTrace? = null) =
        SimulationEngine.runSimulation(config, emptyList(), emptyList(), logExpenses = false, trace = trace)

    @Test
    fun `a plan with its savings in the TFA survives on it`() {
        assertFalse(run(config(tfa = 0.0)).summary.isSuccess, "without the TFA the plan fails")

        val result = run(config(tfa = 2000000.0))
        assertTrue(result.summary.isSuccess)
        assertTrue(result.yearlyResults.sumOf { it.cashFlow.tfaWithdrawal } > 0.0)
    }

    @Test
    fun `TFA withdrawals are income, untaxed, and the quarters add up to the year`() {
        val result = run(config(tfa = 2000000.0))
        result.yearlyResults.drop(1).forEach { row ->
            val cf = row.cashFlow
            assertEquals(cf.salary + cf.interest + cf.dividends + cf.socialSecurity + cf.tbaWithdrawal +
                cf.tdaWithdrawalSpend + cf.tfaWithdrawal, cf.totalIncome, 0.01, "${row.year} total income")
            val quarters = result.quarterlyResults.filter { it.year == row.year }
            assertEquals(cf.tfaWithdrawal, quarters.sumOf { it.cashFlow.tfaWithdrawal }, 0.01, "${row.year} quarters")
        }
    }

    @Test
    fun `the breakdown lists TFA withdrawals as untaxed income`() {
        val config = config(tfa = 2000000.0)
        val trace = SimulationTrace()
        val result = run(config, trace)
        val row = result.yearlyResults.first { it.cashFlow.tfaWithdrawal > 0.0 }
        val b = BreakdownGenerator.generate(config, result, trace, row.age)

        val tfa = b.income!!.withdrawals.single { "TFA" in it.label }
        assertEquals(row.cashFlow.tfaWithdrawal, tfa.amount, 0.01)
        assertEquals(row.cashFlow.totalIncome, b.income!!.totalIncome, 0.01)
        val taxed = b.taxes!!.generatedThisYear.components.single { "TFA" in it.name }
        assertEquals(0.0, taxed.taxable, 0.01)
        assertTrue(b.moneyMovement!!.flows.any { it.from == Endpoint.TFA && it.to == Endpoint.SB })
    }
}
