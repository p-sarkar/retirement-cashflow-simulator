package com.retirement.logic

import com.retirement.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimulationEngineTest {

    @Test
    fun `test simulation run basic`() {
        val config = SimulationConfig(
            id = "test-1",
            name = "Test Simulation",
            currentYear = 2025,
            currentAge = 60,
            retirementAge = 65,
            salary = 120000.0,
            portfolio = Portfolio(
                sb = 200000.0,
                cbb = 1000000.0,
                tba = 500000.0,
                tda = 1000000.0,
                tfa = 0.0
            ),
            spousal = SpousalDetails(
                spouseAge = 58,
                lowerEarner = SocialSecurityDetails(67, 20000.0),
                higherEarner = SocialSecurityDetails(70, 40000.0)
            ),
            expenses = ExpenseConfig(
                needs = 60000.0,
                wants = 30000.0,
                propertyTax = 10000.0,
                healthcarePreRetirement = 5000.0,
                healthcarePostRetirementPreMedicare = 5000.0,
                healthcareMedicare = 3000.0
            ),
            contributions = ContributionConfig(
                annual401k = 23000.0,
                annualTba = 12000.0
            ),
            rates = RateConfig(
                inflation = 0.03,
                preRetirementGrowth = 0.07,
                postRetirementGrowth = 0.05,
                bondYield = 0.04,
                hysaRate = 0.04,
                incomeTax = 0.20
            ),
            strategy = StrategyConfig(
                initialTdaWithdrawal = 40000.0,
                rothConversionAmount = 10000.0
            )
        )

        // Mock 40 years of data
        val marketReturns = List(40) { 0.05 }
        val inflationRates = List(40) { 0.03 }

        val result = SimulationEngine.runSimulation(config, marketReturns, inflationRates)
        
        // 60 to 85 is 26 years (inclusive)
        assertEquals(26, result.yearlyResults.size)
        
        // Check final result
        assertTrue(result.summary.isSuccess)
        assertTrue(result.summary.finalTotalBalance > 0)
    }

    @Test
    fun `working-year interest estimate uses the SB balance`() {
        assertEquals(8000.0, SimulationEngine.estimatedAnnualInterest(true, sb = 200000.0, hysaRate = 0.04, annualExpenses = 100000.0), 0.01)
    }

    @Test
    fun `retirement interest estimate assumes the SB holds half a year of expenses`() {
        // The SB is refilled through the year, so its average balance is about half the annual expenses
        val small = SimulationEngine.estimatedAnnualInterest(false, sb = 10000.0, hysaRate = 0.04, annualExpenses = 100000.0)
        val large = SimulationEngine.estimatedAnnualInterest(false, sb = 2000000.0, hysaRate = 0.04, annualExpenses = 100000.0)

        assertEquals(2000.0, small, 0.01)
        assertEquals(2000.0, large, 0.01)
    }

    @Test
    fun `a plan that runs out of equities in a rising market spends its Crash Buffer before failing (FR002-005)`() {
        // At a steady 10% the market is never down, so before 2026-10-19 the CBB was never drawn: this plan
        // failed in 2039 with about $414k still in it
        val config = com.retirement.testConfig(portfolio = Portfolio(sb = 50000.0, cbb = 100000.0, tba = 150000.0, tda = 250000.0, tfa = 0.0))
        val result = SimulationEngine.runSimulation(config, emptyList(), emptyList())
        val failureYear = kotlin.test.assertNotNull(result.summary.failureYear)
        assertTrue(failureYear > 2039, "fails in $failureYear")
        val failed = result.yearlyResults.last()
        assertTrue(failed.balances.cbb < 1.0, "CBB left at failure: ${failed.balances.cbb}")
    }
}

