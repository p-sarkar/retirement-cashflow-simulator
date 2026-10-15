package com.retirement

import com.retirement.model.*

/** A typical couple: age 60 in 2025, retiring at 65, with enough saved to last to 85 at steady markets. */
fun testConfig(
    currentAge: Int = 60,
    retirementAge: Int = 65,
    portfolio: Portfolio = Portfolio(sb = 200000.0, cbb = 1000000.0, tba = 500000.0, tda = 1000000.0, tfa = 0.0),
    rates: RateConfig = RateConfig(
        inflation = 0.03, preRetirementGrowth = 0.10, postRetirementGrowth = 0.10,
        bondYield = 0.05, hysaRate = 0.04, incomeTax = 0.20
    ),
    marketPresetId: String? = null
) = SimulationConfig(
    name = "Test Plan",
    currentYear = 2025,
    currentAge = currentAge,
    retirementAge = retirementAge,
    salary = 120000.0,
    portfolio = portfolio,
    spousal = SpousalDetails(58, SocialSecurityDetails(67, 20000.0), SocialSecurityDetails(70, 40000.0)),
    expenses = ExpenseConfig(60000.0, 30000.0, 10000.0, 5000.0, 5000.0, 3000.0),
    contributions = ContributionConfig(annual401k = 23000.0, annualTba = 12000.0),
    rates = rates,
    strategy = StrategyConfig(initialTdaWithdrawal = 40000.0),
    marketPresetId = marketPresetId
)
