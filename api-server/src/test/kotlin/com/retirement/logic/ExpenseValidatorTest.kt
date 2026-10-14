package com.retirement.logic

import com.retirement.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpenseValidatorTest {

    private fun config(vararg expenses: OneTimeExpense) = SimulationConfig(
        name = "Validator Test",
        currentYear = 2025,
        currentAge = 60,
        retirementAge = 65,
        salary = 0.0,
        portfolio = Portfolio(sb = 0.0, cbb = 0.0, tba = 0.0, tda = 0.0, tfa = 0.0),
        spousal = SpousalDetails(58, SocialSecurityDetails(67, 0.0), SocialSecurityDetails(70, 0.0)),
        expenses = ExpenseConfig(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        contributions = ContributionConfig(annual401k = 0.0, annualTba = 0.0),
        rates = RateConfig(0.03, 0.07, 0.05, 0.04, 0.04, 0.2),
        strategy = StrategyConfig(initialTdaWithdrawal = 0.0, rothConversionAmount = 0.0),
        oneTimeExpenses = expenses.toList()
    )

    private val loan = LoanExpense(
        id = "l", name = "Loan", principal = 40000.0, aprPercent = 5.0, termYears = 5,
        startYearOrAge = YearOrAge.Year(2027)
    )
    private val cash = CashExpense(id = "c", name = "Cash", amount = 1000.0, yearOrAge = YearOrAge.Year(2027))

    private fun fields(vararg e: OneTimeExpense) = ExpenseValidator.validate(config(*e)).errors.map { it.field }

    @Test
    fun `valid expenses pass`() {
        assertTrue(ExpenseValidator.validate(config(cash, loan)).isValid)
    }

    @Test
    fun `one year term is valid (start year equals end year)`() {
        assertTrue(ExpenseValidator.validate(config(loan.copy(termYears = 1))).isValid)
    }

    @Test
    fun `zero APR is valid, negative APR is rejected`() {
        assertTrue(ExpenseValidator.validate(config(loan.copy(aprPercent = 0.0))).isValid)
        assertEquals(listOf("oneTimeExpenses[0].aprPercent"), fields(loan.copy(aprPercent = -1.0)))
    }

    @Test
    fun `non positive term and amounts are rejected`() {
        assertEquals(listOf("oneTimeExpenses[0].termYears"), fields(loan.copy(termYears = 0)))
        assertEquals(listOf("oneTimeExpenses[0].principal"), fields(loan.copy(principal = 0.0, downPayment = 0.0)))
        assertEquals(listOf("oneTimeExpenses[0].amount"), fields(cash.copy(amount = -5.0)))
    }

    @Test
    fun `down payment must be between zero and principal`() {
        assertTrue(ExpenseValidator.validate(config(loan.copy(downPayment = 40000.0))).isValid)
        assertEquals(listOf("oneTimeExpenses[0].downPayment"), fields(loan.copy(downPayment = 40001.0)))
        assertEquals(listOf("oneTimeExpenses[0].downPayment"), fields(loan.copy(downPayment = -1.0)))
    }

    @Test
    fun `timing must fall within the simulation period`() {
        assertEquals(listOf("oneTimeExpenses[0].yearOrAge"), fields(cash.copy(yearOrAge = YearOrAge.Year(2024))))
        assertEquals(listOf("oneTimeExpenses[0].yearOrAge"), fields(cash.copy(yearOrAge = YearOrAge.Year(2061))))
        assertEquals(listOf("oneTimeExpenses[1].startYearOrAge"), fields(cash, loan.copy(startYearOrAge = YearOrAge.Age(59))))
    }

    @Test
    fun `simulation window is the year after the current year through age 85`() {
        // currentYear 2025 / age 60: simulated years are 2026 (age 61) to 2050 (age 85)
        assertEquals(listOf("oneTimeExpenses[0].yearOrAge"), fields(cash.copy(yearOrAge = YearOrAge.Year(2025))))
        assertEquals(emptyList(), fields(cash.copy(yearOrAge = YearOrAge.Year(2026))))
        assertEquals(emptyList(), fields(cash.copy(yearOrAge = YearOrAge.Age(85))))
        assertEquals(listOf("oneTimeExpenses[0].yearOrAge"), fields(cash.copy(yearOrAge = YearOrAge.Age(86))))
        assertEquals(listOf("oneTimeExpenses[0].startYearOrAge"), fields(loan.copy(startYearOrAge = YearOrAge.Age(86))))
        // A loan starting in the last year may run past the end of the simulation
        assertEquals(emptyList(), fields(loan.copy(startYearOrAge = YearOrAge.Age(85), termYears = 10)))
    }

    @Test
    fun `timing error names the simulation window`() {
        val error = ExpenseValidator.validate(config(cash.copy(yearOrAge = YearOrAge.Age(90)))).errors.single()
        assertEquals("Expense must fall within the simulation: 2026 (age 61) to 2050 (age 85)", error.message)
    }

    @Test
    fun `blank name is rejected`() {
        assertFalse(ExpenseValidator.validate(config(cash.copy(name = " "))).isValid)
    }
}
