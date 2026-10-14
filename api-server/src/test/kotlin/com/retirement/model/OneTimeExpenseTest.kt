package com.retirement.model

import kotlin.test.Test
import kotlin.test.assertEquals

class OneTimeExpenseTest {

    private fun loan(
        principal: Double = 40000.0,
        downPayment: Double = 0.0,
        aprPercent: Double = 5.0,
        termYears: Int = 5,
        quarterlyPayment: Double? = null
    ) = LoanExpense(
        id = "loan-1",
        name = "Car",
        principal = principal,
        aprPercent = aprPercent,
        termYears = termYears,
        startYearOrAge = YearOrAge.Year(2027),
        quarterlyPayment = quarterlyPayment,
        downPayment = downPayment
    )

    @Test
    fun `quarterly payment matches standard amortization table`() {
        // $100k at 6% APR over 10 years, quarterly compounding (SC-006: within $0.01)
        assertEquals(3342.71, LoanExpense.calculateQuarterlyPayment(100000.0, 6.0, 10), 0.01)
    }

    @Test
    fun `zero APR uses simple division`() {
        assertEquals(3000.0, LoanExpense.calculateQuarterlyPayment(60000.0, 0.0, 5), 1e-9)
    }

    @Test
    fun `payment is calculated on financed amount when down payment present`() {
        val expected = LoanExpense.calculateQuarterlyPayment(40000.0, 4.0, 5)
        assertEquals(expected, loan(principal = 50000.0, downPayment = 10000.0, aprPercent = 4.0).quarterlyPaymentFor(1.0), 1e-9)
    }

    @Test
    fun `down payment equal to principal yields zero payment`() {
        assertEquals(0.0, loan(principal = 50000.0, downPayment = 50000.0).quarterlyPaymentFor(1.0), 1e-9)
        assertEquals(0.0, loan(principal = 50000.0, downPayment = 50000.0, aprPercent = 0.0).quarterlyPaymentFor(1.0), 1e-9)
    }

    @Test
    fun `financed amount is inflated to start year before amortizing`() {
        val factor = 1.03 * 1.03
        val expected = LoanExpense.calculateQuarterlyPayment(40000.0 * factor, 5.0, 5)
        assertEquals(expected, loan().quarterlyPaymentFor(factor), 1e-9)
    }

    @Test
    fun `client supplied quarterly payment is ignored`() {
        assertEquals(loan().quarterlyPaymentFor(1.0), loan(quarterlyPayment = 999999.0).quarterlyPaymentFor(1.0), 1e-9)
    }

    @Test
    fun `end year is start year plus term minus one`() {
        assertEquals(2031, loan().getEndYear(currentAge = 60, currentYear = 2025))
        assertEquals(2027, loan(termYears = 1).getEndYear(currentAge = 60, currentYear = 2025))
    }
}
