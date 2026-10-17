package com.retirement.logic

import com.retirement.model.*

/**
 * What the engine did, recorded for the computation breakdown (spec 004, research R004-1). Filled only when a
 * trace is passed to [SimulationEngine.runSimulation]; it never feeds back into the simulation (FR004-006).
 */
class SimulationTrace {
    /** Estimate of the start year's taxable income, which sets the first simulated year's tax bill. */
    var startYearTaxableIncomeEstimate: Double = 0.0
        internal set

    private val _years = sortedMapOf<Int, YearTrace>()
    val years: Map<Int, YearTrace> get() = _years

    internal fun startYear(year: Int, plan: YearPlan, conditions: TraceConditions): YearTrace =
        YearTrace(plan, conditions).also { _years[year] = it }
}

class YearTrace(val plan: YearPlan, val conditions: TraceConditions) {
    private val _movements = mutableListOf<Movement>()
    private val _quarterStrategy = mutableListOf<QuarterStrategy>()

    /** Every balance change, in the order the engine made it. */
    val movements: List<Movement> get() = _movements
    val quarterStrategy: List<QuarterStrategy> get() = _quarterStrategy

    var failure: FailureMark? = null
        internal set

    /** Records a balance change; a negative amount is recorded as the reverse movement, zero is skipped. */
    internal fun move(quarter: Int, type: MovementType, from: Endpoint, to: Endpoint, amount: Double) {
        when {
            amount > 0.0 -> _movements += Movement(quarter, type, from, to, amount)
            amount < 0.0 -> _movements += Movement(quarter, type, to, from, -amount)
        }
    }

    internal fun strategy(entry: QuarterStrategy) {
        _quarterStrategy += entry
    }
}

/** One balance change: `amount` (> 0) moved from → to in a quarter (1–4). */
data class Movement(
    val quarter: Int,
    val type: MovementType,
    val from: Endpoint,
    val to: Endpoint,
    val amount: Double
)

/** Conditions applied in a year, before any of its market returns. */
data class TraceConditions(
    val inflationRate: Double, // the rate applied this year (configured, or a Monte Carlo run's sample)
    val cumulativeInflation: Double,
    val equityReturn: Double,
    val bondYield: Double,
    val marketVsAllTimeHigh: Double
)

/** Values fixed at the start of a year, before any market return of that year (research R004-4). */
data class YearPlan(
    val isWorkingYear: Boolean,
    val healthcareBracket: HealthcareBracket,
    val needs: Double,
    val wants: Double,
    val healthcare: Double,
    val propertyTax: Double,
    val incomeTax: Double,
    val salary: Double,
    val contribution401k: Double,
    val contributionRoth: Double,
    val contributionTba: Double,
    val lowerEarnerBenefit: Double,
    val higherEarnerBenefit: Double,
    val spousalStepUp: Boolean,
    val rothConversionPlanned: Double,
    val estimatedInterest: Double,
    val estimatedDividends: Double,
    val estimatedAig: Double,
    val capAig: Double,
    val sbFloor: Double,
    val sbCap: Double,
    val cbbCap: Double
)
