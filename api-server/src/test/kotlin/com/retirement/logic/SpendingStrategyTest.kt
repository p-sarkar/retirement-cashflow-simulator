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
        cbb: Double = 500000.0, tba: Double = 1000000.0, tda: Double = 1000000.0,
        athPerformance: Double = 1.0, cbbPerformance: Double = 1.0, inflationAdjustment: Double = 1.0, tfa: Double = 0.0,
        wantsCutInDownMarket: Boolean = true, cbbCap: Double = 500000.0
    ) =
        SpendingStrategy.executeQuarterly(
            quarter = 0,
            config = config,
            currentBalances = Portfolio(sb = sb, cbb = cbb, tba = tba, tda = tda, tfa = tfa),
            aig = aig,
            capAig = capAig,
            annualExpenses = annualExpenses,
            cbbCap = cbbCap, // default: the CBB is full, so no CBB refill
            marketPerformance = marketPerformance,
            athPerformance = athPerformance,
            cbbPerformance = cbbPerformance,
            inflationAdjustment = inflationAdjustment,
            wantsCutInDownMarket = wantsCutInDownMarket
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
        val result = execute(sb = 5000.0, aig = 10000.0, capAig = 50000.0, annualExpenses = 100000.0, tba = 1000.0, tda = 1000.0, cbb = 0.0, cbbCap = 0.0)

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

    // --- #21: in a downturn, the CBB or the equities, by the CBB's 12-month change and the losses (FR002-005) ---

    @Test
    fun `a CBB at 90 percent of its balance 12 months earlier refills the SB`() {
        // CBB loss 400,000 is far above the equities' 12-month market loss 222,222, but the CBB check passes first
        val result = execute(sb = -5000.0, aig = 20000.0, capAig = -10000.0, annualExpenses = 20000.0,
            marketPerformance = 0.90, cbb = 100000.0, cbbPerformance = 0.90)

        assertEquals(StrategyRule.MARKET_DOWN_FROM_CBB, result.rule)
        assertEquals(15000.0, result.cbbToSb, 0.01)
        assertEquals(0.90, result.cbbVs12MonthsPrior!!, 1e-9)
        assertEquals(null, result.cbbLoss) // the loss comparison wasn't needed
    }

    @Test
    fun `a CBB that lost less than the equities refills the SB`() {
        // CBB loss = cap 500,000 − 450,000 = 50,000; equities' loss = 2,000,000 × (1 / 0.80 − 1) = 500,000
        val result = execute(sb = -5000.0, aig = 20000.0, capAig = -10000.0, annualExpenses = 20000.0,
            marketPerformance = 0.80, cbb = 450000.0, cbbPerformance = 0.85)

        assertEquals(StrategyRule.MARKET_DOWN_FROM_CBB, result.rule)
        assertEquals(15000.0, result.cbbToSb, 0.01)
        assertEquals(50000.0, result.cbbLoss!!, 0.01)
        assertEquals(500000.0, result.equityLoss!!, 0.01)
    }

    @Test
    fun `a CBB that lost more than the equities leaves the CBB and refills the SB from equities`() {
        // CBB loss = 500,000 − 100,000 = 400,000; equities' loss = 2,000,000 × (1 / 0.90 − 1) = 222,222
        val result = execute(sb = -5000.0, aig = 20000.0, capAig = -10000.0, annualExpenses = 20000.0,
            marketPerformance = 0.90, cbb = 100000.0, cbbPerformance = 0.50)

        assertEquals(StrategyRule.MARKET_DOWN_FROM_EQUITIES, result.rule)
        assertEquals(0.0, result.cbbToSb, 0.01)
        assertEquals(100000.0, result.portfolio.cbb, 0.01)
        assertEquals(15000.0, result.tdaToSb + result.tbaToSb, 0.01)
        assertEquals(10000.0, result.portfolio.sb, 0.01)
        assertEquals(RefillLimit.SB_CAP, result.limitedBy) // (20,000 − 2,000) / 4 + 15,000 due, 15,000 to the cap
        assertEquals(222222.22, result.equityLoss!!, 0.01)
        result.assertRoutesSumToTotals()
    }

    @Test
    fun `equities that rose over 12 months have no loss, so a drawn-down CBB is spared`() {
        // Below 85% of the all-time high (market down), but up on 12 months earlier: the equities' loss is 0
        val result = execute(sb = -5000.0, aig = 20000.0, capAig = -10000.0, annualExpenses = 20000.0,
            marketPerformance = 1.02, athPerformance = 0.80, cbb = 400000.0, cbbPerformance = 0.80)

        assertEquals(StrategyRule.MARKET_DOWN_FROM_EQUITIES, result.rule)
        assertEquals(0.0, result.equityLoss!!, 0.01)
        assertEquals(15000.0, result.tdaToSb + result.tbaToSb, 0.01)
    }

    @Test
    fun `a downturn refill from equities is limited by the equity balances`() {
        val result = execute(sb = -5000.0, aig = 20000.0, capAig = -10000.0, annualExpenses = 20000.0,
            marketPerformance = 0.50, cbb = 0.0, tba = 1000.0, tda = 1000.0, cbbPerformance = 0.0)

        assertEquals(StrategyRule.MARKET_DOWN_FROM_EQUITIES, result.rule)
        assertEquals(RefillLimit.AVAILABLE_BALANCE, result.limitedBy)
        assertEquals(2000.0, result.tdaToSb + result.tbaToSb, 0.01)
        assertEquals(13000.0, result.shortfall, 0.01)
    }

    // --- #22: the downturn Wants cut is 10% of the year's Wants, not of the configured amount (FR002-005) ---

    @Test
    fun `the downturn Wants cut is 10 percent of the year's inflation-adjusted Wants`() {
        // Configured Wants 20,000 × 1.8 cumulative inflation = 36,000 this year; cut 3,600 (not 2,000).
        // SB at its floor (max(0.5 × 40,000, 0.25 × 40,000) = 20,000), so no deficit; far below its cap
        val result = execute(sb = 20000.0, aig = 40000.0, capAig = 50000.0, annualExpenses = 40000.0,
            marketPerformance = 0.90, inflationAdjustment = 1.8)

        assertEquals(StrategyRule.MARKET_DOWN_FROM_CBB, result.rule)
        assertEquals((40000.0 - 3600.0) / 4.0, result.cbbToSb, 0.01)
        assertEquals(RefillLimit.QUARTERLY_SHARE, result.limitedBy)
    }

    @Test
    fun `without the downturn Wants cut the refill is the full quarterly share (FR006-015)`() {
        // Guardrails set Wants themselves, so the 10% cut is off: 40,000 / 4, not (40,000 − 3,600) / 4
        val result = execute(sb = 20000.0, aig = 40000.0, capAig = 50000.0, annualExpenses = 40000.0,
            marketPerformance = 0.90, inflationAdjustment = 1.8, wantsCutInDownMarket = false)

        assertEquals(StrategyRule.MARKET_DOWN_FROM_CBB, result.rule)
        assertEquals(40000.0 / 4.0, result.cbbToSb, 0.01)
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

    // --- The CBB pays what the equities can't, in any market, before the TFA (FR002-005, 2026-10-19) ---

    @Test
    fun `in a rising market a refill the equities can't cover comes from the CBB`() {
        // Floor 25,000, SB 20,000 below it: 0.25 × 10,000 + 20,000 = 22,500 due; equities hold 2,000
        val result = execute(sb = 5000.0, aig = 10000.0, capAig = 50000.0, annualExpenses = 100000.0,
            tba = 1000.0, tda = 1000.0, tfa = 50000.0)

        assertEquals(StrategyRule.MARKET_UP_CBB_FULL, result.rule)
        assertEquals(2000.0, result.tdaToSb + result.tbaToSb, 0.01)
        assertEquals(20500.0, result.cbbToSb, 0.01)
        assertEquals(20500.0, result.cbbWithdrawal, 0.01)
        assertEquals(500000.0 - 20500.0, result.portfolio.cbb, 0.01)
        assertEquals(0.0, result.tfaWithdrawal, 0.01) // the TFA stays the last resort
        assertEquals(27500.0, result.portfolio.sb, 0.01)
        assertEquals(0.0, result.shortfall, 0.01)
        assertEquals(RefillLimit.SB_FLOOR_DEFICIT, result.limitedBy)
        result.assertRoutesSumToTotals()
    }

    @Test
    fun `a refill beyond the equities and the CBB is completed from the TFA`() {
        val result = execute(sb = 5000.0, aig = 10000.0, capAig = 50000.0, annualExpenses = 100000.0,
            tba = 1000.0, tda = 1000.0, cbb = 500.0, tfa = 50000.0, cbbCap = 500.0)

        assertEquals(500.0, result.cbbToSb, 0.01)
        assertEquals(20000.0, result.tfaWithdrawal, 0.01)
        assertEquals(0.0, result.shortfall, 0.01)
    }

    // --- A downturn refill the CBB can't cover comes from the equities, then the TFA (FR002-005, 2026-10-19) ---

    @Test
    fun `a downturn refill from a CBB that can't cover it is completed from the equities`() {
        // CBB held up (>= 90% of a year earlier), so the SB is refilled from it, but it holds only 5,000 of 15,000
        val result = execute(sb = -5000.0, aig = 20000.0, capAig = -10000.0, annualExpenses = 20000.0,
            marketPerformance = 0.90, cbb = 5000.0, cbbPerformance = 1.0)

        assertEquals(StrategyRule.MARKET_DOWN_FROM_CBB, result.rule)
        assertEquals(5000.0, result.cbbToSb, 0.01)
        assertEquals(10000.0, result.tdaToSb + result.tbaToSb, 0.01)
        assertEquals(10000.0, result.portfolio.sb, 0.01)
        assertEquals(0.0, result.shortfall, 0.01)
        result.assertRoutesSumToTotals()
    }

    @Test
    fun `an empty CBB chosen by the loss comparison leaves the refill to the equities`() {
        // Empty CBB, empty a year ago too: its "loss" (cap − balance) is below the equities', so the CBB is chosen
        val result = execute(sb = -5000.0, aig = 20000.0, capAig = -10000.0, annualExpenses = 20000.0,
            marketPerformance = 0.50, cbb = 0.0, cbbPerformance = 0.0, cbbCap = 100.0)

        assertEquals(StrategyRule.MARKET_DOWN_FROM_CBB, result.rule)
        assertEquals(15000.0, result.tdaToSb + result.tbaToSb, 0.01)
        assertEquals(10000.0, result.portfolio.sb, 0.01)
        assertEquals(0.0, result.shortfall, 0.01)
    }

    @Test
    fun `a downturn refill beyond the CBB and the equities is completed from the TFA`() {
        val result = execute(sb = -5000.0, aig = 20000.0, capAig = -10000.0, annualExpenses = 20000.0,
            marketPerformance = 0.90, cbb = 5000.0, cbbPerformance = 1.0, tba = 1000.0, tda = 1000.0, tfa = 100000.0)

        assertEquals(5000.0, result.cbbToSb, 0.01)
        assertEquals(2000.0, result.tdaToSb + result.tbaToSb, 0.01)
        assertEquals(8000.0, result.tfaWithdrawal, 0.01)
        assertEquals(0.0, result.shortfall, 0.01)
    }

    // --- #23: the Tax Free account is the last resort for spending (FR002-005) ---

    @Test
    fun `a refill TDA and TBA can't cover is completed from the TFA`() {
        // Floor 25,000, SB 20,000 below it: 0.25 × 10,000 + 20,000 = 22,500 due; equities hold 2,000
        val result = execute(sb = 5000.0, aig = 10000.0, capAig = 50000.0, annualExpenses = 100000.0,
            tba = 1000.0, tda = 1000.0, tfa = 50000.0, cbb = 0.0, cbbCap = 0.0)

        assertEquals(2000.0, result.tdaToSb + result.tbaToSb, 0.01)
        assertEquals(20500.0, result.tfaWithdrawal, 0.01)
        assertEquals(29500.0, result.portfolio.tfa, 0.01)
        assertEquals(27500.0, result.portfolio.sb, 0.01)
        assertEquals(0.0, result.shortfall, 0.01)
        assertEquals(RefillLimit.SB_FLOOR_DEFICIT, result.limitedBy) // the full refill was made
    }

    @Test
    fun `a refill beyond the TFA too is limited by the available balance`() {
        val result = execute(sb = 5000.0, aig = 10000.0, capAig = 50000.0, annualExpenses = 100000.0,
            tba = 1000.0, tda = 1000.0, tfa = 500.0, cbb = 0.0, cbbCap = 0.0)

        assertEquals(500.0, result.tfaWithdrawal, 0.01)
        assertEquals(0.0, result.portfolio.tfa, 0.01)
        assertEquals(20000.0, result.shortfall, 0.01)
        assertEquals(RefillLimit.AVAILABLE_BALANCE, result.limitedBy)
    }

    @Test
    fun `the TFA pays for spending but never refills the CBB`() {
        // Market up, an empty CBB below its cap, no equities: the SB's 2,500 share comes from the TFA, the CBB's doesn't
        val result = execute(sb = 90000.0, aig = 20000.0, capAig = 50000.0, annualExpenses = 40000.0,
            cbb = 0.0, tba = 0.0, tda = 0.0, tfa = 1000000.0)

        assertEquals(2500.0, result.tfaWithdrawal, 0.01)
        assertEquals(0.0, result.portfolio.cbb, 0.01)
        assertEquals(RefillLimit.AVAILABLE_BALANCE, result.cbbRefillLimitedBy)
    }

    @Test
    fun `a downturn refill from equities falls back to the CBB, then the TFA`() {
        // CBB 5,000 below 90% of a year earlier and lost more than the (empty) equities: equities, then CBB, then TFA
        val result = execute(sb = -5000.0, aig = 20000.0, capAig = -10000.0, annualExpenses = 20000.0,
            marketPerformance = 0.90, cbb = 5000.0, tba = 0.0, tda = 0.0, cbbPerformance = 0.50, tfa = 100000.0)

        assertEquals(StrategyRule.MARKET_DOWN_FROM_EQUITIES, result.rule)
        assertEquals(5000.0, result.cbbToSb, 0.01)
        assertEquals(0.0, result.portfolio.cbb, 0.01)
        assertEquals(10000.0, result.tfaWithdrawal, 0.01)
        assertEquals(10000.0, result.portfolio.sb, 0.01)
        assertEquals(0.0, result.shortfall, 0.01)
    }

    @Test
    fun `shortfall cover takes the CBB, then equities, then the TFA`() {
        val before = Portfolio(sb = -30000.0, cbb = 10000.0, tba = 5000.0, tda = 5000.0, tfa = 100000.0)
        val result = SpendingStrategy.coverShortfall(before, 30000.0, 0.5, 500000.0)

        assertEquals(10000.0, result.cbbWithdrawal, 0.01)
        assertEquals(10000.0, result.tdaWithdrawal + result.tbaWithdrawal, 0.01)
        assertEquals(10000.0, result.tfaWithdrawal, 0.01)
        assertEquals(90000.0, result.balances.tfa, 0.01)
        assertEquals(0.0, result.balances.sb, 0.01)
    }
}
