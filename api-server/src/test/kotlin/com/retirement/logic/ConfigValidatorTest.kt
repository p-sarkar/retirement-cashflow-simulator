package com.retirement.logic

import com.retirement.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigValidatorTest {

    private val rates = RateConfig(
        inflation = 0.03,
        preRetirementGrowth = 0.08,
        postRetirementGrowth = 0.08,
        bondYield = 0.05,
        hysaRate = 0.03,
        incomeTax = 0.18
    )

    private fun config(
        rates: RateConfig = this.rates,
        tdaWithdrawalPercentage: Double = 0.6,
        expenses: List<OneTimeExpense> = emptyList()
    ) = SimulationConfig(
        name = "Config Validator Test",
        currentYear = 2025,
        currentAge = 60,
        retirementAge = 65,
        salary = 0.0,
        portfolio = Portfolio(0.0, 0.0, 0.0, 0.0, 0.0),
        spousal = SpousalDetails(58, SocialSecurityDetails(67, 0.0), SocialSecurityDetails(70, 0.0)),
        expenses = ExpenseConfig(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        contributions = ContributionConfig(annual401k = 0.0, annualTba = 0.0),
        rates = rates,
        strategy = StrategyConfig(initialTdaWithdrawal = 0.0, tdaWithdrawalPercentage = tdaWithdrawalPercentage),
        oneTimeExpenses = expenses
    )

    private fun fields(c: SimulationConfig) = ConfigValidator.validate(c).errors.map { it.field }

    @Test
    fun `typical decimal rates are valid`() {
        assertTrue(ConfigValidator.validate(config()).isValid)
    }

    @Test
    fun `boundary values are valid`() {
        val edges = RateConfig(
            inflation = -0.10, preRetirementGrowth = -0.50, postRetirementGrowth = 0.50,
            bondYield = 0.0, hysaRate = 0.25, incomeTax = 0.90
        )
        assertTrue(ConfigValidator.validate(config(rates = edges, tdaWithdrawalPercentage = 1.0)).isValid)
        assertTrue(ConfigValidator.validate(config(tdaWithdrawalPercentage = 0.0)).isValid)
    }

    @Test
    fun `whole-number percentages sent by mistake are rejected`() {
        val wholeNumbers = RateConfig(
            inflation = 3.0, preRetirementGrowth = 8.0, postRetirementGrowth = 8.0,
            bondYield = 5.0, hysaRate = 3.0, incomeTax = 18.0
        )
        assertEquals(
            listOf(
                "rates.inflation", "rates.preRetirementGrowth", "rates.postRetirementGrowth",
                "rates.bondYield", "rates.hysaRate", "rates.incomeTax", "strategy.tdaWithdrawalPercentage"
            ),
            fields(config(rates = wholeNumbers, tdaWithdrawalPercentage = 60.0))
        )
    }

    @Test
    fun `out of range values just past the bounds are rejected`() {
        assertEquals(listOf("rates.inflation"), fields(config(rates = rates.copy(inflation = 0.26))))
        assertEquals(listOf("rates.inflation"), fields(config(rates = rates.copy(inflation = -0.11))))
        assertEquals(listOf("rates.hysaRate"), fields(config(rates = rates.copy(hysaRate = -0.01))))
        assertEquals(listOf("rates.incomeTax"), fields(config(rates = rates.copy(incomeTax = 0.91))))
        assertEquals(listOf("strategy.tdaWithdrawalPercentage"), fields(config(tdaWithdrawalPercentage = 1.01)))
    }

    @Test
    fun `error message states the expected unit`() {
        val message = ConfigValidator.validate(config(rates = rates.copy(inflation = 3.0))).errors.single().message
        assertEquals("Inflation must be between -10% and 25% (as a decimal, e.g. 0.03 for 3%)", message)
    }

    @Test
    fun `one-time expense errors are included`() {
        val badCash = CashExpense(id = "c", name = "Roof", amount = -1.0, yearOrAge = YearOrAge.Year(2027))
        assertEquals(
            listOf("rates.inflation", "oneTimeExpenses[0].amount"),
            fields(config(rates = rates.copy(inflation = 3.0), expenses = listOf(badCash)))
        )
    }

    @Test
    fun `current age must be below the end age`() {
        assertEquals(emptyList(), fields(config().copy(currentAge = 84)))
        assertEquals(listOf("endAge"), fields(config().copy(currentAge = 85)))
        assertEquals(emptyList(), fields(config().copy(currentAge = 85, endAge = 90)))
    }
}
