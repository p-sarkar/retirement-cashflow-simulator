package com.retirement.logic

import com.retirement.data.HistoricalYear
import com.retirement.model.RateConfig

/** One simulated year's market variables, as decimals. */
data class YearDraw(val equityReturn: Double, val inflation: Double, val bondYield: Double)

/**
 * The per-year market draw shared by Monte Carlo scenarios and guardrail review paths (spec 006 research R006-3):
 * - Historical bootstrap: a random historical year's S&P 500 return and inflation together, shifted so their
 *   long-run averages equal [growth] and the configured inflation.
 * - Without historical data: normally distributed return and inflation around the configured rates.
 * Bond yields vary normally around the configured yield, never below 0.
 *
 * The random stream is consumed in a fixed order (a historical index or two gaussians, then the bond gaussian),
 * so the same [random] state always gives the same draw.
 */
object MarketDraw {
    /** Year-to-year spread of bond yields around the configured yield. */
    const val BOND_YIELD_SD = 0.01

    // Used when historical data is unavailable
    const val EQUITY_RETURN_SD = 0.18
    const val INFLATION_SD = 0.02

    const val MIN_RETURN = -0.95 // a -100% year would zero the market index

    fun drawYear(
        random: java.util.Random,
        rates: RateConfig,
        growth: Double,
        history: List<HistoricalYear>,
        historicalMeanReturn: Double,
        historicalMeanInflation: Double
    ): YearDraw {
        val equityReturn: Double
        val inflation: Double
        if (history.isNotEmpty()) {
            val year = history[random.nextInt(history.size)]
            equityReturn = growth + (year.equityReturn - historicalMeanReturn)
            inflation = rates.inflation + (year.inflation - historicalMeanInflation)
        } else {
            equityReturn = growth + random.nextGaussian() * EQUITY_RETURN_SD
            inflation = rates.inflation + random.nextGaussian() * INFLATION_SD
        }
        val bondYield = (rates.bondYield + random.nextGaussian() * BOND_YIELD_SD).coerceAtLeast(0.0)
        return YearDraw(equityReturn.coerceAtLeast(MIN_RETURN), inflation, bondYield)
    }
}
