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
}
