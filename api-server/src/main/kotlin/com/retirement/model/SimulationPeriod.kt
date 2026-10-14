package com.retirement.model

/**
 * The years a simulation covers. The current year holds the starting balances and is not simulated;
 * simulation runs from the following year through the year the user reaches [END_AGE].
 *
 * The end age is fixed for now; it is expected to become a field on the configuration form.
 * The frontend mirrors it as SIMULATION_END_AGE in frontend/src/utils/expenseUtils.ts.
 */
object SimulationPeriod {
    const val END_AGE = 85

    fun firstYear(config: SimulationConfig): Int = config.currentYear + 1

    fun lastYear(config: SimulationConfig): Int = config.currentYear + (END_AGE - config.currentAge)

    fun contains(config: SimulationConfig, year: Int): Boolean = year in firstYear(config)..lastYear(config)

    /** e.g. "2026 (age 61) to 2050 (age 85)" */
    fun describe(config: SimulationConfig): String {
        val first = firstYear(config)
        return "$first (age ${config.currentAge + 1}) to ${lastYear(config)} (age $END_AGE)"
    }
}
