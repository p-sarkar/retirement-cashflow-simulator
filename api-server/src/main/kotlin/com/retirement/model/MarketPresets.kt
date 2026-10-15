package com.retirement.model

import kotlinx.serialization.Serializable

/**
 * A preset collection of market conditions (spec 002 FR-003). Selecting a preset fills the config's
 * market rates; a preset can also replace the equity return of the first retirement years with a
 * sequence, to replay a crash or boom right at retirement. All rates are decimals (0.03 = 3%).
 */
@Serializable
data class MarketPreset(
    val id: String,
    val name: String,
    val description: String,
    val inflation: Double,
    val preRetirementGrowth: Double,
    val postRetirementGrowth: Double,
    val bondYield: Double,
    /** Equity returns for the first retirement years, in order; later years use postRetirementGrowth. */
    val retirementReturnSequence: List<Double> = emptyList()
)

object MarketPresets {
    val all = listOf(
        MarketPreset(
            id = "historical-average",
            name = "Historical Average",
            description = "Long-run US averages: S&P 500 total return about 10%, inflation about 3%, bonds about 5%",
            inflation = 0.03, preRetirementGrowth = 0.10, postRetirementGrowth = 0.10, bondYield = 0.05
        ),
        MarketPreset(
            id = "crash-2008",
            name = "2008 Crash",
            description = "Historical averages, but the first five retirement years replay the S&P 500 total returns " +
                "of 2008–2012 (−37%, +26.5%, +15.1%, +2.1%, +16%)",
            inflation = 0.03, preRetirementGrowth = 0.10, postRetirementGrowth = 0.10, bondYield = 0.05,
            retirementReturnSequence = listOf(-0.37, 0.265, 0.151, 0.021, 0.16)
        ),
        MarketPreset(
            id = "bull-market",
            name = "Bull Market",
            description = "Sustained strong markets like the late 1990s: equities 12%, inflation 2.5%, bonds 4%",
            inflation = 0.025, preRetirementGrowth = 0.12, postRetirementGrowth = 0.12, bondYield = 0.04
        ),
        MarketPreset(
            id = "stagflation",
            name = "Stagflation",
            description = "High inflation and weak real returns like the 1970s: equities 6%, inflation 7%, bonds 7%",
            inflation = 0.07, preRetirementGrowth = 0.06, postRetirementGrowth = 0.06, bondYield = 0.07
        )
    )

    fun find(id: String): MarketPreset? = all.find { it.id == id }

    /**
     * Equity return for each simulated year, indexed like the engine's marketReturns (0 = start year,
     * not simulated). Empty when the config has no preset or its preset has no return sequence, so the
     * engine uses the config's static growth rates.
     *
     * The sequence starts in the first retirement year (the year after Retirement Age, or the first
     * simulated year if already retired). Other years use the config's growth rates, which the form
     * fills from the preset but the user may have edited.
     */
    fun equityReturns(config: SimulationConfig): List<Double> {
        val sequence = config.marketPresetId?.let(::find)?.retirementReturnSequence.orEmpty()
        if (sequence.isEmpty()) return emptyList()

        val firstRetirementAge = maxOf(config.retirementAge, config.currentAge) + 1
        return (0..config.endAge - config.currentAge).map { yearIdx ->
            val age = config.currentAge + yearIdx
            val retirementYear = age - firstRetirementAge
            when {
                age <= config.retirementAge -> config.rates.preRetirementGrowth
                retirementYear in sequence.indices -> sequence[retirementYear]
                else -> config.rates.postRetirementGrowth
            }
        }
    }
}
