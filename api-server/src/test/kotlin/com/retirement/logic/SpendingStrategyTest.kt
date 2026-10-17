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

    private fun execute(
        sb: Double, aig: Double, capAig: Double, annualExpenses: Double, marketPerformance: Double = 1.0,
        cbb: Double = 500000.0, tba: Double = 1000000.0, tda: Double = 1000000.0
    ) =
        SpendingStrategy.executeQuarterly(
            quarter = 0,
            config = config,
            currentBalances = Portfolio(sb = sb, cbb = cbb, tba = tba, tda = tda, tfa = 0.0),
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

    // --- T004-004: rule, limit and routes reported for the breakdown (FR004-052) ---

    private fun SpendingResult.assertRoutesSumToTotals() {
        assertEquals(tdaWithdrawal, tdaToSb + tdaToCbb, 0.01)
        assertEquals(tbaWithdrawal, tbaToSb + tbaToCbb, 0.01)
        assertEquals(cbbWithdrawal, cbbToSb, 0.01)
    }

    @Test
    fun `market up with the CBB full refills the SB, limited by the SB cap`() {
        val result = execute(sb = 90000.0, aig = 60000.0, capAig = 50000.0, annualExpenses = 100000.0)

        assertEquals(StrategyRule.MARKET_UP_CBB_FULL, result.rule)
        assertEquals(RefillLimit.SB_CAP, result.limitedBy)
        assertEquals(10000.0, result.tdaToSb + result.tbaToSb, 0.01)
        assertEquals(0.0, result.tdaToCbb + result.tbaToCbb, 0.01)
        result.assertRoutesSumToTotals()
    }

    @Test
    fun `a refill of the quarterly share of the gap is limited by that share`() {
        // Cap 100,000, floor 10,000: refill = 25% × 20,000
        val result = execute(sb = 90000.0, aig = 20000.0, capAig = 50000.0, annualExpenses = 40000.0)

        assertEquals(RefillLimit.QUARTERLY_SHARE, result.limitedBy)
        assertEquals(5000.0, result.tdaToSb + result.tbaToSb, 0.01)
    }

    @Test
    fun `a refill that repays the SB floor deficit reports it`() {
        // Floor 25,000, deficit 20,000: refill = 25% × 10,000 + 20,000, well under the 100,000 cap
        val result = execute(sb = 5000.0, aig = 10000.0, capAig = 50000.0, annualExpenses = 100000.0)

        assertEquals(RefillLimit.SB_FLOOR_DEFICIT, result.limitedBy)
        assertEquals(22500.0, result.tdaToSb + result.tbaToSb, 0.01)
    }

    @Test
    fun `market up with the CBB below its cap also refills the CBB`() {
        val result = execute(sb = 90000.0, aig = 20000.0, capAig = 50000.0, annualExpenses = 40000.0, cbb = 400000.0)

        assertEquals(StrategyRule.MARKET_UP_CBB_REFILL, result.rule)
        assertEquals(2500.0, result.tdaToCbb + result.tbaToCbb, 0.01) // 12.5% × 20,000
        result.assertRoutesSumToTotals()
    }

    @Test
    fun `a refill larger than the equity balances is limited by the available balance`() {
        val result = execute(sb = 5000.0, aig = 10000.0, capAig = 50000.0, annualExpenses = 100000.0, tba = 1000.0, tda = 1000.0)

        assertEquals(RefillLimit.AVAILABLE_BALANCE, result.limitedBy)
        assertEquals(2000.0, result.tdaToSb + result.tbaToSb, 0.01)
    }

    @Test
    fun `market down with the SB above its floor moves nothing`() {
        val result = execute(sb = 90000.0, aig = 20000.0, capAig = 50000.0, annualExpenses = 40000.0, marketPerformance = 0.90)

        assertEquals(StrategyRule.MARKET_DOWN_HOLD, result.rule)
        assertEquals(RefillLimit.NONE, result.limitedBy)
        assertEquals(90000.0, result.portfolio.sb, 0.01)
    }

    @Test
    fun `market down with the SB below its floor draws from the CBB`() {
        val result = execute(sb = -5000.0, aig = 20000.0, capAig = -10000.0, annualExpenses = 20000.0, marketPerformance = 0.90)

        assertEquals(StrategyRule.MARKET_DOWN_FROM_CBB, result.rule)
        assertEquals(15000.0, result.cbbToSb, 0.01)
        result.assertRoutesSumToTotals()
    }

    @Test
    fun `shortfall cover reports what reached the SB from each account`() {
        val before = Portfolio(sb = -30000.0, cbb = 10000.0, tba = 100000.0, tda = 100000.0, tfa = 0.0)
        val result = SpendingStrategy.coverShortfall(before, 30000.0, 0.5, 500000.0)

        assertEquals(10000.0, result.cbbWithdrawal, 0.01)
        assertEquals(30000.0, result.cbbWithdrawal + result.tdaWithdrawal + result.tbaWithdrawal, 0.01)
        assertEquals(0.0, result.balances.sb, 0.01)
    }

    // --- T004-045: what limited the CBB refill (U004-1, FR004-052) ---

    @Test
    fun `a CBB refill of the quarter's share is limited by that share`() {
        // Room under the cap 100,000 > 12.5% × 20,000
        val result = execute(sb = 90000.0, aig = 20000.0, capAig = 50000.0, annualExpenses = 40000.0, cbb = 400000.0)

        assertEquals(RefillLimit.QUARTERLY_SHARE, result.cbbRefillLimitedBy)
        assertEquals(2500.0, result.tdaToCbb + result.tbaToCbb, 0.01)
    }

    @Test
    fun `a CBB refill close to the cap is limited by the CBB cap`() {
        // Room under the cap 1,000 < 12.5% × 20,000
        val result = execute(sb = 90000.0, aig = 20000.0, capAig = 50000.0, annualExpenses = 40000.0, cbb = 499000.0)

        assertEquals(RefillLimit.CBB_CAP, result.cbbRefillLimitedBy)
        assertEquals(1000.0, result.tdaToCbb + result.tbaToCbb, 0.01)
    }

    @Test
    fun `a CBB refill larger than the equity balances is limited by the available balance`() {
        // SB above its cap, so only the CBB refill runs; TDA and TBA hold 1,000 between them
        val result = execute(sb = 200000.0, aig = 20000.0, capAig = 50000.0, annualExpenses = 40000.0,
            cbb = 400000.0, tba = 500.0, tda = 500.0)

        assertEquals(RefillLimit.AVAILABLE_BALANCE, result.cbbRefillLimitedBy)
        assertEquals(1000.0, result.tdaToCbb + result.tbaToCbb, 0.01)
    }

    @Test
    fun `there is no CBB refill limit when the CBB is full or the market is down`() {
        assertEquals(null, execute(sb = 90000.0, aig = 20000.0, capAig = 50000.0, annualExpenses = 40000.0).cbbRefillLimitedBy)
        assertEquals(null, execute(sb = 90000.0, aig = 20000.0, capAig = 50000.0, annualExpenses = 40000.0,
            cbb = 400000.0, marketPerformance = 0.90).cbbRefillLimitedBy)
    }
}
