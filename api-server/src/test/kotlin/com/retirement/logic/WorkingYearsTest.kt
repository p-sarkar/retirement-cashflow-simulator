package com.retirement.logic

import com.retirement.model.*
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Working years (age <= retirementAge): salary covers expenses, so there is no income gap,
 * and the Spend Bucket must reconcile: start + deposits - withdrawals = end.
 */
class WorkingYearsTest {

    private fun config(
        rothConversionPreRetirement: Double = 0.0,
        oneTimeExpenses: List<OneTimeExpense> = emptyList()
    ) = SimulationConfig(
        name = "Working years",
        currentYear = 2026,
        currentAge = 55,
        retirementAge = 60,
        endAge = 75,
        salary = 200000.0,
        portfolio = Portfolio(sb = 100000.0, cbb = 300000.0, tba = 500000.0, tda = 800000.0, tfa = 100000.0),
        spousal = SpousalDetails(
            spouseAge = 53,
            lowerEarner = SocialSecurityDetails(67, 20000.0),
            higherEarner = SocialSecurityDetails(70, 40000.0)
        ),
        expenses = ExpenseConfig(
            needs = 60000.0,
            wants = 40000.0,
            propertyTax = 10000.0,
            healthcarePreRetirement = 6000.0,
            healthcarePostRetirementPreMedicare = 18000.0,
            healthcareMedicare = 8000.0
        ),
        contributions = ContributionConfig(annual401k = 23000.0, annualRoth = 7000.0, annualTba = 10000.0),
        rates = RateConfig(
            inflation = 0.03,
            preRetirementGrowth = 0.07,
            postRetirementGrowth = 0.05,
            bondYield = 0.04,
            hysaRate = 0.04,
            incomeTax = 0.20
        ),
        strategy = StrategyConfig(
            initialTdaWithdrawal = 0.0,
            rothConversionPreRetirement = rothConversionPreRetirement,
            rothConversionPostRetirement = 20000.0
        ),
        oneTimeExpenses = oneTimeExpenses
    )

    private fun run(config: SimulationConfig) = SimulationEngine.runSimulation(config, emptyList(), emptyList())

    private fun assertMoney(expected: Double, actual: Double, message: String) =
        assertTrue(abs(expected - actual) < 0.01, "$message: expected $expected, got $actual")

    @Test
    fun `start year and working years have no income gap`() {
        val result = run(config())

        result.yearlyResults.filter { it.age <= 60 }.forEach {
            assertNull(it.metrics.annualIncomeGap, "Yearly AIG at age ${it.age}")
        }
        result.quarterlyResults.filter { it.age <= 60 }.forEach {
            assertNull(it.metrics.annualIncomeGap, "Quarterly AIG at ${it.year} Q${it.quarter}")
        }
        result.yearlyResults.filter { it.age > 60 }.forEach {
            assertNotNull(it.metrics.annualIncomeGap, "Yearly AIG at age ${it.age}")
        }
    }

    @Test
    fun `start year and working years have no SB cap`() {
        val result = run(config())

        result.yearlyResults.filter { it.age <= 60 }.forEach {
            assertNull(it.metrics.sbCap, "Yearly SB cap at age ${it.age}")
        }
        result.quarterlyResults.filter { it.age <= 60 }.forEach {
            assertNull(it.metrics.sbCap, "Quarterly SB cap at ${it.year} Q${it.quarter}")
        }
        result.yearlyResults.filter { it.age > 60 }.forEach {
            assertNotNull(it.metrics.sbCap, "Yearly SB cap at age ${it.age}")
        }
    }

    @Test
    fun `retirement-age year is a full working year`() {
        val config = config()
        val year = run(config).yearlyResults.single { it.age == config.retirementAge }
        val inflation = Math.pow(1.03, 5.0)

        assertMoney(200000.0 * inflation, year.cashFlow.salary, "Salary")
        assertMoney(6000.0 * inflation, year.cashFlow.healthcare, "Pre-retirement healthcare")
        assertMoney(0.0, year.cashFlow.tbaWithdrawal + year.cashFlow.tdaWithdrawalSpend, "Spending-strategy withdrawals")
        // Wants absorb what salary has left, so take-home salary covers exactly the expenses
        val takeHome = year.cashFlow.salary - year.cashFlow.contribution401k - year.cashFlow.contributionRoth - year.cashFlow.contributionTba
        assertMoney(takeHome, year.cashFlow.totalExpenses, "Total expenses")
    }

    @Test
    fun `spend bucket reconciles every year`() {
        val loan = LoanExpense(
            id = "car", name = "Car", startYearOrAge = YearOrAge.Year(2028), principal = 60000.0,
            downPayment = 10000.0, aprPercent = 6.0, termYears = 5
        )
        val roof = CashExpense(id = "roof", name = "Roof", yearOrAge = YearOrAge.Age(64), amount = 400000.0)
        val result = run(config(rothConversionPreRetirement = 30000.0, oneTimeExpenses = listOf(loan, roof)))

        result.yearlyResults.zipWithNext().forEach { (prior, year) ->
            assertMoney(
                prior.balances.sb + year.cashFlow.sbDeposit - year.cashFlow.sbWithdrawal,
                year.balances.sb,
                "SB at age ${year.age}"
            )
        }
    }

    @Test
    fun `working years save exactly the interest and dividends`() {
        val result = run(config(rothConversionPreRetirement = 30000.0))

        result.yearlyResults.zipWithNext().filter { (_, year) -> year.age <= 60 }.forEach { (prior, year) ->
            assertTrue(year.cashFlow.wants > 0.0, "Wants must not be clamped at age ${year.age}")
            assertMoney(
                year.cashFlow.interest + year.cashFlow.dividends,
                year.balances.sb - prior.balances.sb,
                "SB growth at age ${year.age}"
            )
        }
    }

    @Test
    fun `roth conversion does not reduce working-year wants`() {
        // Conversions start in simulation year 2, and are taxed the year after, so year 2 isolates the effect
        val withoutConversion = run(config()).yearlyResults.single { it.age == 57 }
        val withConversion = run(config(rothConversionPreRetirement = 30000.0)).yearlyResults.single { it.age == 57 }

        assertTrue(withConversion.cashFlow.rothConversion > 0.0)
        assertMoney(withoutConversion.cashFlow.wants, withConversion.cashFlow.wants, "Wants")
    }

    @Test
    fun `retirement years withdraw full expenses from the spend bucket`() {
        val result = run(config())

        result.yearlyResults.filter { it.age > 60 }.forEach {
            assertMoney(it.cashFlow.totalExpenses, it.cashFlow.sbWithdrawal, "SB withdrawal at age ${it.age}")
        }
    }
}
