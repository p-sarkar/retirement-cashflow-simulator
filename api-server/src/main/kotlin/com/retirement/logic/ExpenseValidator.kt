package com.retirement.logic

import com.retirement.model.*

/**
 * Validation errors for one-time expenses.
 */
data class ValidationError(
    val field: String,
    val message: String
)

/**
 * Result of validation.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError>
)

/**
 * Validates one-time expense data.
 */
object ExpenseValidator {

    /**
     * Validate all one-time expenses in a simulation config.
     */
    fun validate(config: SimulationConfig): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        config.oneTimeExpenses.forEachIndexed { index, expense ->
            when (expense) {
                is CashExpense -> validateCashExpense(expense, index, config, errors)
                is LoanExpense -> validateLoanExpense(expense, index, config, errors)
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun validateCashExpense(
        expense: CashExpense,
        index: Int,
        config: SimulationConfig,
        errors: MutableList<ValidationError>
    ) {
        val prefix = "oneTimeExpenses[$index]"

        // Name validation
        if (expense.name.isBlank()) {
            errors.add(ValidationError("$prefix.name", "Expense name is required"))
        } else if (expense.name.length > 100) {
            errors.add(ValidationError("$prefix.name", "Expense name must be 100 characters or less"))
        }

        // Amount validation
        if (expense.amount <= 0) {
            errors.add(ValidationError("$prefix.amount", "Amount must be greater than zero"))
        } else if (expense.amount >= 1e12) {
            errors.add(ValidationError("$prefix.amount", "Amount must be less than \$1 trillion"))
        }

        // Timing validation
        val year = expense.yearOrAge.toYear(config.currentAge, config.currentYear)
        if (year < config.currentYear) {
            errors.add(ValidationError("$prefix.yearOrAge", "Expense cannot occur in the past"))
        } else if (year > config.currentYear + 35) {
            errors.add(ValidationError("$prefix.yearOrAge", "Expense must occur within simulation period"))
        }
    }

    private fun validateLoanExpense(
        expense: LoanExpense,
        index: Int,
        config: SimulationConfig,
        errors: MutableList<ValidationError>
    ) {
        val prefix = "oneTimeExpenses[$index]"

        // Name validation
        if (expense.name.isBlank()) {
            errors.add(ValidationError("$prefix.name", "Loan name is required"))
        } else if (expense.name.length > 100) {
            errors.add(ValidationError("$prefix.name", "Loan name must be 100 characters or less"))
        }

        // Principal validation
        if (expense.principal <= 0) {
            errors.add(ValidationError("$prefix.principal", "Principal must be greater than zero"))
        } else if (expense.principal >= 1e12) {
            errors.add(ValidationError("$prefix.principal", "Principal must be less than \$1 trillion"))
        }

        // APR validation
        if (expense.aprPercent < 0) {
            errors.add(ValidationError("$prefix.aprPercent", "APR cannot be negative"))
        } else if (expense.aprPercent > 99.99) {
            errors.add(ValidationError("$prefix.aprPercent", "APR must be 99.99% or less"))
        }

        // Term validation
        if (expense.termYears <= 0) {
            errors.add(ValidationError("$prefix.termYears", "Term must be greater than zero"))
        } else if (expense.termYears > 50) {
            errors.add(ValidationError("$prefix.termYears", "Term must be 50 years or less"))
        }

        // Timing validation
        val startYear = expense.startYearOrAge.toYear(config.currentAge, config.currentYear)
        if (startYear < config.currentYear) {
            errors.add(ValidationError("$prefix.startYearOrAge", "Loan cannot start in the past"))
        } else if (startYear > config.currentYear + 35) {
            errors.add(ValidationError("$prefix.startYearOrAge", "Loan must start within simulation period"))
        }
    }
}

