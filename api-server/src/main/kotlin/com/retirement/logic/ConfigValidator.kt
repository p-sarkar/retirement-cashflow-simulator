package com.retirement.logic

import com.retirement.model.MarketPresets
import com.retirement.model.SimulationConfig
import com.retirement.model.SimulationPeriod
import kotlin.math.roundToInt

/**
 * Validates a whole simulation config: rate/strategy ranges, End Age, ages, non-negative amounts,
 * the market preset, plus one-time expenses.
 *
 * Rates and tdaWithdrawalPercentage are decimals (0.03 = 3%). The bounds are generous for real inputs
 * but reject whole-number percentages sent by mistake (e.g. inflation = 3.0, i.e. 300%).
 */
object ConfigValidator {

    private class Bound(
        val field: String,
        val label: String,
        val min: Double,
        val max: Double,
        val value: (SimulationConfig) -> Double
    )

    private val bounds = listOf(
        Bound("rates.inflation", "Inflation", -0.10, 0.25) { it.rates.inflation },
        Bound("rates.preRetirementGrowth", "Pre-retirement growth", -0.50, 0.50) { it.rates.preRetirementGrowth },
        Bound("rates.postRetirementGrowth", "Post-retirement growth", -0.50, 0.50) { it.rates.postRetirementGrowth },
        Bound("rates.bondYield", "Bond yield", 0.0, 0.25) { it.rates.bondYield },
        Bound("rates.hysaRate", "HYSA rate", 0.0, 0.25) { it.rates.hysaRate },
        Bound("rates.incomeTax", "Income tax", 0.0, 0.90) { it.rates.incomeTax },
        Bound("strategy.tdaWithdrawalPercentage", "TDA withdrawal percentage", 0.0, 1.0) { it.strategy.tdaWithdrawalPercentage }
    )

    fun validate(config: SimulationConfig): ValidationResult {
        val rangeErrors = bounds
            .filter { bound -> bound.value(config) !in bound.min..bound.max } // NaN also fails
            .map { bound ->
                ValidationError(
                    bound.field,
                    "${bound.label} must be between ${percent(bound.min)} and ${percent(bound.max)} " +
                        "(as a decimal, e.g. 0.03 for 3%)"
                )
            }

        // End Age must leave at least one simulated year (FR-001a)
        val endAgeErrors = if (config.endAge <= config.currentAge || config.endAge > SimulationPeriod.MAX_END_AGE) {
            listOf(ValidationError("endAge",
                "End age must be greater than current age (${config.currentAge}) and no more than ${SimulationPeriod.MAX_END_AGE}"))
        } else emptyList()

        // A field the user clears in the form arrives as 0, so out-of-range ages and years also catch missing input
        val wholeNumberErrors = wholeNumberBounds
            .filter { bound -> bound.value(config) !in bound.min..bound.max }
            .map { bound -> ValidationError(bound.field, "${bound.label} must be a whole number from ${bound.min} to ${bound.max}") }

        val negativeAmountErrors = amounts
            .filter { (_, _, value) -> !(value(config) >= 0.0) } // NaN also fails
            .map { (field, label, _) -> ValidationError(field, "$label cannot be negative") }

        val presetErrors = config.marketPresetId
            ?.takeIf { MarketPresets.find(it) == null }
            ?.let { listOf(ValidationError("marketPresetId", "Unknown market preset '$it'")) }
            .orEmpty()

        val errors = rangeErrors + endAgeErrors + wholeNumberErrors + negativeAmountErrors + presetErrors +
            ExpenseValidator.validate(config).errors
        return ValidationResult(errors.isEmpty(), errors)
    }

    private class IntBound(val field: String, val label: String, val min: Int, val max: Int, val value: (SimulationConfig) -> Int)

    private val wholeNumberBounds = listOf(
        IntBound("currentYear", "Current year", 1900, 2200) { it.currentYear },
        IntBound("currentAge", "Current age", 18, SimulationPeriod.MAX_END_AGE - 1) { it.currentAge },
        IntBound("retirementAge", "Retirement age", 18, SimulationPeriod.MAX_END_AGE) { it.retirementAge },
        IntBound("spousal.spouseAge", "Spouse age", 18, SimulationPeriod.MAX_END_AGE) { it.spousal.spouseAge },
        // Social Security can be claimed from 62 to 70
        IntBound("spousal.lowerEarner.claimAge", "Lower earner claim age", 62, 70) { it.spousal.lowerEarner.claimAge },
        IntBound("spousal.higherEarner.claimAge", "Higher earner claim age", 62, 70) { it.spousal.higherEarner.claimAge }
    )

    private val amounts: List<Triple<String, String, (SimulationConfig) -> Double>> = listOf(
        Triple("salary", "Salary") { it.salary },
        Triple("portfolio.sb", "Spend Bucket balance") { it.portfolio.sb },
        Triple("portfolio.cbb", "Crash Buffer balance") { it.portfolio.cbb },
        Triple("portfolio.tba", "Taxable (TBA) balance") { it.portfolio.tba },
        Triple("portfolio.tda", "Tax Deferred (TDA) balance") { it.portfolio.tda },
        Triple("portfolio.tfa", "Tax Free (TFA) balance") { it.portfolio.tfa },
        Triple("expenses.needs", "Needs") { it.expenses.needs },
        Triple("expenses.wants", "Wants") { it.expenses.wants },
        Triple("expenses.propertyTax", "Property tax") { it.expenses.propertyTax },
        Triple("expenses.healthcarePreRetirement", "Healthcare (pre-retirement)") { it.expenses.healthcarePreRetirement },
        Triple("expenses.healthcarePostRetirementPreMedicare", "Healthcare (retirement to 65)") { it.expenses.healthcarePostRetirementPreMedicare },
        Triple("expenses.healthcareMedicare", "Healthcare (Medicare)") { it.expenses.healthcareMedicare },
        Triple("contributions.annual401k", "401k contribution") { it.contributions.annual401k },
        Triple("contributions.annualRoth", "Roth contribution") { it.contributions.annualRoth },
        Triple("contributions.annualTba", "TBA contribution") { it.contributions.annualTba },
        Triple("spousal.lowerEarner.annualBenefit", "Lower earner benefit") { it.spousal.lowerEarner.annualBenefit },
        Triple("spousal.higherEarner.annualBenefit", "Higher earner benefit") { it.spousal.higherEarner.annualBenefit },
        Triple("strategy.rothConversionPreRetirement", "Roth conversion (pre-retirement)") { it.strategy.rothConversionPreRetirement },
        Triple("strategy.rothConversionPostRetirement", "Roth conversion (post-retirement)") { it.strategy.rothConversionPostRetirement }
    )

    private fun percent(decimal: Double) = "${(decimal * 100).roundToInt()}%"
}
