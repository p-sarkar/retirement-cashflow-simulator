package com.retirement.model

/**
 * The years a simulation covers. The current year holds the starting balances and is not simulated;
 * simulation runs from the following year through the year the user reaches the config's End Age
 * (FR002-001a).
 */
object SimulationPeriod {
    /** End Age for configs that don't set one, including saved simulations from before FR002-001a. */
    const val DEFAULT_END_AGE = 85

    /** Sanity bound, not a planning recommendation. */
    const val MAX_END_AGE = 110

    fun firstYear(config: SimulationConfig): Int = config.currentYear + 1

    fun lastYear(config: SimulationConfig): Int = config.currentYear + (config.endAge - config.currentAge)

    fun contains(config: SimulationConfig, year: Int): Boolean = year in firstYear(config)..lastYear(config)

    /** e.g. "2026 (age 61) to 2050 (age 85)" */
    fun describe(config: SimulationConfig): String =
        "${firstYear(config)} (age ${config.currentAge + 1}) to ${lastYear(config)} (age ${config.endAge})"
}
