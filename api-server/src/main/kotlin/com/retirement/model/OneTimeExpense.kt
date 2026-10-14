package com.retirement.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.pow

/**
 * Timing specification for one-time expenses.
 * Can be specified as either a calendar year or an age.
 */
@Serializable
sealed interface YearOrAge {
    @Serializable
    @SerialName("YEAR")
    data class Year(val year: Int) : YearOrAge

    @Serializable
    @SerialName("AGE")
    data class Age(val age: Int) : YearOrAge

    /**
     * Convert to calendar year based on current age and year.
     */
    fun toYear(currentAge: Int, currentYear: Int): Int {
        return when (this) {
            is Year -> year
            is Age -> currentYear + (age - currentAge)
        }
    }

    /**
     * Convert to age based on current age and year.
     */
    fun toAge(currentAge: Int, currentYear: Int): Int {
        return when (this) {
            is Year -> currentAge + (year - currentYear)
            is Age -> age
        }
    }
}

/**
 * Base interface for all one-time expenses.
 * Uses sealed interface for type-safe polymorphism.
 */
@Serializable
sealed interface OneTimeExpense {
    val id: String
    val name: String
}

/**
 * One-time lump sum cash expense.
 * Examples: car purchase, home renovation, medical procedure.
 */
@Serializable
@SerialName("CASH")
data class CashExpense(
    override val id: String,
    override val name: String,
    val amount: Double,
    val yearOrAge: YearOrAge
) : OneTimeExpense

/**
 * Loan expense with quarterly amortized payments.
 * Examples: home equity loan, car loan, personal loan.
 */
@Serializable
@SerialName("LOAN")
data class LoanExpense(
    override val id: String,
    override val name: String,
    val principal: Double,
    val aprPercent: Double,
    val termYears: Int,
    val startYearOrAge: YearOrAge,
    val quarterlyPayment: Double? = null,  // Display-only estimate from the client; ignored by the engine
    val downPayment: Double = 0.0  // Down payment amount (optional, defaults to 0)
) : OneTimeExpense {

    /**
     * Calculate the financed amount (principal - down payment).
     */
    fun getFinancedAmount(): Double = principal - downPayment

    /**
     * Calculate the end year of the loan.
     */
    fun getEndYear(currentAge: Int, currentYear: Int): Int {
        val startYear = startYearOrAge.toYear(currentAge, currentYear)
        return startYear + termYears - 1
    }

    /**
     * Fixed nominal quarterly payment. The financed amount is entered in today's dollars,
     * inflated to the start year, then amortized; the payment stays constant for the term.
     *
     * @param startYearInflationFactor Cumulative inflation from the simulation start to the loan start year
     */
    fun quarterlyPaymentFor(startYearInflationFactor: Double): Double =
        calculateQuarterlyPayment(getFinancedAmount() * startYearInflationFactor, aprPercent, termYears)

    companion object {
        /**
         * Calculate quarterly payment using standard amortization formula.
         * Q = P[r(1+r)^n] / [(1+r)^n - 1]
         *
         * This is the level payment whose present value at rate r over n periods equals P:
         * P = Q[1 - (1+r)^-n] / r, solved for Q. Interest compounds quarterly, so r is the
         * quarterly rate (APR / 4) and n the number of quarters. E.g. $100,000 at 6% for 10 years:
         * r = 0.015, n = 40, Q = $3,342.71 (SC-006: within $0.01 of a standard amortization table).
         *
         * For 0% APR the formula divides by zero, so it uses simple division: P / n
         *
         * @param financedAmount The amount to be financed (principal - down payment)
         * @param aprPercent Annual percentage rate as a whole number (5 = 5%)
         * @param termYears Loan term in years
         */
        fun calculateQuarterlyPayment(financedAmount: Double, aprPercent: Double, termYears: Int): Double {
            val n = termYears * 4 // number of quarterly payments

            if (aprPercent == 0.0) {
                return financedAmount / n
            }

            val r = aprPercent / 100.0 / 4.0 // quarterly interest rate
            val onePlusR = 1.0 + r
            val onePlusRPowN = onePlusR.pow(n) // growth of $1 over the term

            // P·r is the first quarter's interest; scaling by (1+r)^n / ((1+r)^n - 1) adds enough principal
            // each quarter that the balance reaches exactly zero after n payments
            return (financedAmount * r * onePlusRPowN) / (onePlusRPowN - 1.0)
        }
    }
}

/**
 * Type of expense for breakdown display.
 */
@Serializable
enum class ExpenseType {
    CASH,
    LOAN_PAYMENT
}

/**
 * Detail record for expense breakdown display.
 * Populated whenever one or more expenses occur in the year.
 */
@Serializable
data class ExpenseDetail(
    val name: String,
    val amount: Double,
    val type: ExpenseType
)

