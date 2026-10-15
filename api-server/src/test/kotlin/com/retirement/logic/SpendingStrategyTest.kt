package com.retirement.logic

import com.retirement.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * SB cap floor: the SB cap is never below the larger of six months of the full AIG and one quarter of
 * total expenses, and a refill also repays whatever the SB has fallen below that floor.
 */
class SpendingStrategyTest {

    private val config = SimulationConfig(
        name = "Strategy",
        currentYear = 2026,
        currentAge = 66,
        retirementAge = 65,
        salary = 0.0,
        portfolio = Portfolio(0.0, 0.0, 0.0, 0.0, 0.0),
        spousal = SpousalDetails(64, SocialSecurityDetails(67, 0.0), SocialSecurityDetails(70, 0.0)),
        expenses = ExpenseConfig(60000.0, 20000.0, 10000.0, 0.0, 10000.0, 5000.0),
        contributions = ContributionConfig(annual401k = 0.0, annualTba = 0.0),
        rates = RateConfig(0.03, 0.07, 0.05, 0.04, 0.04, 0.20),
        strategy = StrategyConfig(initialTdaWithdrawal = 0.0)
    )

    private fun execute(sb: Double, aig: Double, capAig: Double, annualExpenses: Double, marketPerformance: Double = 1.0) =
        SpendingStrategy.executeQuarterly(
            quarter = 0,
            config = config,
            currentBalances = Portfolio(sb = sb, cbb = 500000.0, tba = 1000000.0, tda = 1000000.0, tfa = 0.0),
            aig = aig,
            capAig = capAig,
            annualExpenses = annualExpenses,
            cbbCap = 500000.0, // CBB full: no CBB refill
            marketPerformance = marketPerformance,
            athPerformance = 1.0,
            cbbPerformance = 1.0,
            inflationAdjustment = 1.0
        )

    @Test
    fun `equity refill restores the SB to its floor when the cap AIG is negative`() {
        // Floor = max(0.5 × 20,000, 0.25 × 20,000) = 10,000; the SB is 15,000 below it
        val result = execute(sb = -5000.0, aig = 20000.0, capAig = -10000.0, annualExpenses = 20000.0)

        assertEquals(10000.0, result.portfolio.sb, 0.01)
        assertEquals(15000.0, result.tdaWithdrawal + result.tbaWithdrawal, 0.01)
    }

    @Test
    fun `CBB refill in a down market restores the SB to its floor`() {
        val result = execute(sb = -5000.0, aig = 20000.0, capAig = -10000.0, annualExpenses = 20000.0, marketPerformance = 0.90)

        assertEquals(10000.0, result.portfolio.sb, 0.01)
        assertEquals(15000.0, result.cbbWithdrawal, 0.01)
    }

    @Test
    fun `floor holds a quarter of expenses when passive income covers most of them`() {
        // Floor = max(0.5 × 10,000, 0.25 × 100,000) = 25,000; refill = min(25% × 10,000 + 20,000, 25,000 − 5,000)
        val result = execute(sb = 5000.0, aig = 10000.0, capAig = -1000.0, annualExpenses = 100000.0)

        assertEquals(25000.0, result.portfolio.sb, 0.01)
    }

    @Test
    fun `cap and refill size are unchanged when the SB is above its floor`() {
        // Cap = 2 × 50,000 = 100,000; refill = min(25% × 60,000, 100,000 − 90,000)
        val result = execute(sb = 90000.0, aig = 60000.0, capAig = 50000.0, annualExpenses = 100000.0)

        assertEquals(100000.0, result.portfolio.sb, 0.01)
    }
}
