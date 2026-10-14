package com.retirement.logic

import com.retirement.model.SimulationConfig
import com.retirement.model.SimulationPeriod
import kotlin.math.roundToInt

/**
 * Validates a whole simulation config: rate/strategy ranges, End Age, plus one-time expenses.
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
        val ageErrors = if (config.endAge <= config.currentAge || config.endAge > SimulationPeriod.MAX_END_AGE) {
            listOf(ValidationError("endAge",
                "End age must be greater than current age (${config.currentAge}) and no more than ${SimulationPeriod.MAX_END_AGE}"))
        } else emptyList()

        val errors = rangeErrors + ageErrors + ExpenseValidator.validate(config).errors
        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun percent(decimal: Double) = "${(decimal * 100).roundToInt()}%"
}
