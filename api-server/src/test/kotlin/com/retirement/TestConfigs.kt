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

/**
 * The form's default plan (frontend SimulationForm `defaultConfig`), with a fixed start year so results don't
 * move with the calendar: age 51 in 2026, retiring at 55, seven one-time expenses.
 */
fun defaultFormConfig() = SimulationConfig(
    name = "Default form plan",
    currentYear = 2026,
    currentAge = 51,
    retirementAge = 55,
    endAge = 85,
    salary = 230000.0,
    portfolio = Portfolio(sb = 200000.0, cbb = 400000.0, tba = 1215507.0, tda = 1735185.0, tfa = 428470.0),
    spousal = SpousalDetails(49, SocialSecurityDetails(62, 20000.0), SocialSecurityDetails(70, 40000.0)),
    expenses = ExpenseConfig(50000.0, 90000.0, 12000.0, 5000.0, 24000.0, 3000.0),
    contributions = ContributionConfig(annual401k = 0.0, annualRoth = 5000.0, annualTba = 5000.0),
    rates = RateConfig(
        inflation = 0.03, preRetirementGrowth = 0.08, postRetirementGrowth = 0.08,
        bondYield = 0.05, hysaRate = 0.04, incomeTax = 0.12
    ),
    strategy = StrategyConfig(
        initialTdaWithdrawal = 40000.0, tdaWithdrawalPercentage = 0.6,
        rothConversionPreRetirement = 10000.0, rothConversionPostRetirement = 20000.0
    ),
    oneTimeExpenses = listOf(
        LoanExpense("partha-car-loan-1", "Partha's car 1", 50000.0, 5.0, 5, YearOrAge.Age(55), downPayment = 10000.0),
        LoanExpense("mou-car-loan-1", "Mou's car 1", 50000.0, 5.0, 5, YearOrAge.Age(56), downPayment = 10000.0),
        LoanExpense("home-maintenance-loan-2", "New HVAC", 20000.0, 5.0, 5, YearOrAge.Year(2032)),
        CashExpense("default-cash-1", "Daughter's wedding", 100000.0, YearOrAge.Age(60)),
        LoanExpense("home-maintenance-loan-1", "New roof", 25000.0, 5.0, 5, YearOrAge.Year(2035)),
        LoanExpense("partha-car-loan-2", "Partha's car 2", 50000.0, 5.0, 5, YearOrAge.Age(65), downPayment = 10000.0),
        LoanExpense("mou-car-loan-2", "Mou's car 2", 50000.0, 5.0, 5, YearOrAge.Age(68), downPayment = 10000.0)
    )
)

/** `defaultFormConfig()` under each market preset, rates and preset id applied as the form does. */
fun presetConfigs(): List<SimulationConfig> = defaultFormConfig().let { base ->
    MarketPresets.all.map { preset ->
        base.copy(
            name = preset.name,
            rates = base.rates.copy(
                inflation = preset.inflation, preRetirementGrowth = preset.preRetirementGrowth,
                postRetirementGrowth = preset.postRetirementGrowth, bondYield = preset.bondYield
            ),
            marketPresetId = preset.id
        )
    }
}

/** Runs an interactive simulation (preset return sequence included) with a trace, as the breakdown route does. */
fun runTraced(config: SimulationConfig): Pair<SimulationResult, com.retirement.logic.SimulationTrace> {
    val trace = com.retirement.logic.SimulationTrace()
    val result = com.retirement.logic.SimulationEngine.runSimulation(
        config, MarketPresets.equityReturns(config), emptyList(), trace = trace
    )
    return result to trace
}
