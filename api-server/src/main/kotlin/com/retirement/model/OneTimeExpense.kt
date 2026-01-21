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
 * Loan expense with monthly amortized payments.
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
    val monthlyPayment: Double,
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
     * Calculate the annual payment (12 monthly payments).
     */
    fun getAnnualPayment(): Double = monthlyPayment * 12

    companion object {
        /**
         * Calculate monthly payment using standard amortization formula.
         * M = P[r(1+r)^n] / [(1+r)^n - 1]
         *
         * For 0% APR, uses simple division: P / n
         *
         * @param financedAmount The amount to be financed (principal - down payment)
         * @param aprPercent Annual percentage rate
         * @param termYears Loan term in years
         */
        fun calculateMonthlyPayment(financedAmount: Double, aprPercent: Double, termYears: Int): Double {
            val n = termYears * 12

            if (aprPercent == 0.0) {
                return financedAmount / n
            }

            val r = aprPercent / 100.0 / 12.0
            val onePlusR = 1.0 + r
            val onePlusRPowN = onePlusR.pow(n)

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
 * Used when multiple expenses occur in the same year.
 */
@Serializable
data class ExpenseDetail(
    val name: String,
    val amount: Double,
    val type: ExpenseType
)

