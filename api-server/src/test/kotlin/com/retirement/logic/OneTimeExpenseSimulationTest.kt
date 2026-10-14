package com.retirement.logic

import com.retirement.model.*
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OneTimeExpenseSimulationTest {

    private val inflation = 0.03
    private val currentYear = 2025

    private fun config(expenses: List<OneTimeExpense>) = SimulationConfig(
        id = "ote-test",
        name = "One-Time Expense Test",
        currentYear = currentYear,
        currentAge = 60,
        retirementAge = 65,
        salary = 120000.0,
        portfolio = Portfolio(sb = 200000.0, cbb = 1000000.0, tba = 500000.0, tda = 1000000.0, tfa = 0.0),
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
        contributions = ContributionConfig(annual401k = 23000.0, annualTba = 12000.0),
        rates = RateConfig(
            inflation = inflation,
            preRetirementGrowth = 0.07,
            postRetirementGrowth = 0.05,
            bondYield = 0.04,
            hysaRate = 0.04,
            incomeTax = 0.20
        ),
        strategy = StrategyConfig(initialTdaWithdrawal = 40000.0, rothConversionAmount = 10000.0),
        oneTimeExpenses = expenses
    )

    private fun run(vararg expenses: OneTimeExpense) =
        SimulationEngine.runSimulation(config(expenses.toList()), List(40) { 0.05 }, List(40) { inflation })

    private fun factor(year: Int) = (1.0 + inflation).pow(year - currentYear)

    private fun quarterly(result: SimulationResult, year: Int) =
        result.quarterlyResults.filter { it.year == year }.sortedBy { it.quarter }.map { it.cashFlow.oneTimeExpenses }

    private val loan = LoanExpense(
        id = "loan-1",
        name = "Car Loan",
        principal = 40000.0,
        aprPercent = 5.0,
        termYears = 5,
        startYearOrAge = YearOrAge.Year(2027),
        quarterlyPayment = 999999.0 // stale/tampered client value must be ignored
    )

    private val expectedQ = LoanExpense.calculateQuarterlyPayment(40000.0 * (1.03).pow(2), 5.0, 5)

    @Test
    fun `engine computes loan payment server side and ignores client value`() {
        val result = run(loan)
        val entry = result.yearlyResults.first { it.year == 2028 }.oneTimeExpensesBreakdown!!.single()
        assertEquals(ExpenseType.LOAN_PAYMENT, entry.type)
        assertEquals(expectedQ * 4, entry.amount, 0.01)
    }

    @Test
    fun `loan payment is fixed nominal across the whole term`() {
        val result = run(loan)
        (2027..2031).forEach { year ->
            val yr = result.yearlyResults.first { it.year == year }
            assertEquals(expectedQ * 4, yr.cashFlow.oneTimeExpenses, 0.01, "annual loan total in $year")
        }
        val after = result.yearlyResults.first { it.year == 2032 }
        assertEquals(0.0, after.cashFlow.oneTimeExpenses, 1e-9)
        assertNull(after.oneTimeExpensesBreakdown)
    }

    @Test
    fun `loan payments are deducted every quarter, not as a Q1 lump`() {
        val result = run(loan)
        quarterly(result, 2028).forEach { assertEquals(expectedQ, it, 0.01) }
    }

    @Test
    fun `down payment is an inflated Q1 lump sum on top of the first quarterly payment`() {
        val withDown = loan.copy(principal = 50000.0, downPayment = 10000.0)
        val result = run(withDown)
        val down = 10000.0 * factor(2027)
        val qs = quarterly(result, 2027)
        assertEquals(down + expectedQ, qs[0], 0.01)
        qs.drop(1).forEach { assertEquals(expectedQ, it, 0.01) }

        val breakdown = result.yearlyResults.first { it.year == 2027 }.oneTimeExpensesBreakdown!!
        assertEquals(listOf("Car Loan (Down Payment)", "Car Loan"), breakdown.map { it.name })
        assertEquals(down, breakdown[0].amount, 0.01)
    }

    @Test
    fun `one year term pays exactly four quarters in the start year`() {
        val result = run(loan.copy(termYears = 1))
        val q = LoanExpense.calculateQuarterlyPayment(40000.0 * factor(2027), 5.0, 1)
        quarterly(result, 2027).forEach { assertEquals(q, it, 0.01) }
        assertEquals(0.0, result.yearlyResults.first { it.year == 2028 }.cashFlow.oneTimeExpenses, 1e-9)
    }

    @Test
    fun `cash expense remains an inflation adjusted Q1 lump sum`() {
        val cash = CashExpense(id = "cash-1", name = "Roof", amount = 50000.0, yearOrAge = YearOrAge.Age(65))
        val result = run(cash)
        val qs = quarterly(result, 2030)
        assertEquals(50000.0 * factor(2030), qs[0], 0.01)
        qs.drop(1).forEach { assertEquals(0.0, it, 1e-9) }
    }

    @Test
    fun `quarterly one-time amounts sum to the yearly total`() {
        val cash = CashExpense(id = "cash-1", name = "Roof", amount = 20000.0, yearOrAge = YearOrAge.Year(2028))
        val result = run(cash, loan)
        result.yearlyResults.forEach { yr ->
            assertEquals(yr.cashFlow.oneTimeExpenses, quarterly(result, yr.year).sum(), 0.01, "year ${yr.year}")
        }
    }

    @Test
    fun `spend bucket never goes negative when expense exceeds balance`() {
        val big = CashExpense(id = "cash-big", name = "House", amount = 500000.0, yearOrAge = YearOrAge.Year(2027))
        val result = run(big)
        result.quarterlyResults.filter { it.year == 2027 }.forEach {
            assertTrue(it.balances.sb >= -0.01, "SB negative in ${it.year} Q${it.quarter}: ${it.balances.sb}")
        }
    }

    @Test
    fun `simulation with expenses is deterministic`() {
        val cash = CashExpense(id = "cash-1", name = "Roof", amount = 20000.0, yearOrAge = YearOrAge.Year(2028))
        assertEquals(run(cash, loan).yearlyResults, run(cash, loan).yearlyResults)
    }
}
