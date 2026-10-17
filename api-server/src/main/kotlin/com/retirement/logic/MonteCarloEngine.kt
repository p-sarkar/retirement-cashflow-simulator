package com.retirement.logic

import com.retirement.data.HistoricalDataService
import com.retirement.data.HistoricalYear
import com.retirement.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.ceil

/**
 * Monte Carlo analysis (FR002-014 to FR002-018): the interactive simulation run many times, each with
 * its own sequence of equity returns, inflation and bond yields; everything else comes from the config.
 *
 * Deterministic (Constitution IV): run i always uses the scenario generated from (seed, i), so a single
 * run can be rebuilt in full later (US002-4 "Load Details") without returning every table up front.
 */
object MonteCarloEngine {
    const val DEFAULT_RUNS = 1000
    const val MAX_RUNS = 5000

    /** Year-to-year spread of bond yields around the configured yield. */
    private const val BOND_YIELD_SD = 0.01

    // Used when historical data is unavailable
    private const val EQUITY_RETURN_SD = 0.18
    private const val INFLATION_SD = 0.02

    private val MIN_RETURN = -0.95 // a -100% year would zero the market index

    /**
     * Scenario generation, given the historical years available:
     * - Historical bootstrap: each simulated year draws a random historical year and uses its S&P 500 return and
     *   inflation together (keeping their correlation and the real spread of outcomes), shifted so that their
     *   long-run averages equal the configured growth and inflation rates.
     * - Without historical data: normally distributed returns and inflation around the configured rates.
     * Bond yields vary normally around the configured yield (no bond history is available), never below 0.
     */
    fun describeMethod(history: List<HistoricalYear> = HistoricalDataService.getHistoricalYears()): String =
        if (history.isNotEmpty()) {
            "Historical bootstrap: each year draws a random year from ${history.first().year}–${history.last().year} " +
                "and uses its S&P 500 return and inflation, re-centred on your growth and inflation rates. " +
                "Bond yields vary around your bond yield (±${pct(BOND_YIELD_SD)} standard deviation)."
        } else {
            "Random returns around your growth rates (±${pct(EQUITY_RETURN_SD)} standard deviation) and inflation " +
                "around your inflation rate (±${pct(INFLATION_SD)}); historical data was not available. " +
                "Bond yields vary around your bond yield (±${pct(BOND_YIELD_SD)})."
        }

    suspend fun run(
        config: SimulationConfig,
        runs: Int = DEFAULT_RUNS,
        seed: Long,
        history: List<HistoricalYear> = HistoricalDataService.getHistoricalYears()
    ): MonteCarloResult = coroutineScope {
        // Runs are independent, so spread them across cores in batches
        val batchSize = ceil(runs / (Runtime.getRuntime().availableProcessors() * 4.0)).toInt().coerceAtLeast(1)
        val paths = (0 until runs).chunked(batchSize).map { batch ->
            async(Dispatchers.Default) { batch.map { runIndex -> simulateRun(config, seed, runIndex, history).path } }
        }.awaitAll().flatten()

        val years = (config.currentYear..SimulationPeriod.lastYear(config)).toList()
        MonteCarloResult(
            seed = seed,
            runCount = runs,
            method = describeMethod(history),
            years = years,
            paths = paths,
            statistics = statistics(paths, years.size),
            apiMetadata = com.retirement.util.ServerInfo.getMetadata()
        )
    }

    /** Rebuild one run in full: its scenario, summary and complete simulation result. */
    fun runDetail(
        config: SimulationConfig,
        seed: Long,
        runIndex: Int,
        history: List<HistoricalYear> = HistoricalDataService.getHistoricalYears()
    ): MonteCarloRunDetail {
        val run = simulateRun(config, seed, runIndex, history)
        return MonteCarloRunDetail(seed = seed, path = run.path, scenario = run.scenario, result = run.result)
    }

    /**
     * The computation breakdown for one year of run [runIndex] (spec 004 US004-6): the run rebuilt as in
     * [runDetail], with a trace. Null when [targetAge] isn't in the run.
     */
    fun runBreakdown(
        config: SimulationConfig,
        seed: Long,
        runIndex: Int,
        targetAge: Int,
        history: List<HistoricalYear> = HistoricalDataService.getHistoricalYears()
    ): YearBreakdown? {
        val trace = SimulationTrace()
        val run = simulateRun(config, seed, runIndex, history, trace)
        if (run.result.yearlyResults.none { it.age == targetAge }) return null
        return BreakdownGenerator.generate(config, run.result, trace, targetAge, monteCarloRun = runIndex + 1)
    }

    private class Run(val scenario: MarketScenario, val result: SimulationResult, val path: MonteCarloPath)

    private fun simulateRun(
        config: SimulationConfig,
        seed: Long,
        runIndex: Int,
        history: List<HistoricalYear>,
        trace: SimulationTrace? = null
    ): Run {
        val scenario = generateScenario(config, seed, runIndex, history)
        val result = SimulationEngine.runSimulation(
            config,
            marketReturns = scenario.equityReturns,
            inflationRates = scenario.inflationRates,
            bondYields = scenario.bondYields,
            logExpenses = false,
            trace = trace
        )
        val simulated = 1 until scenario.years.size // index 0 is the start year, not simulated
        val path = MonteCarloPath(
            runIndex = runIndex,
            totalBalances = result.yearlyResults.map { it.balances.total() },
            isSuccess = result.summary.isSuccess,
            failureYear = result.summary.failureYear,
            endingBalance = result.summary.finalTotalBalance,
            averageReturn = simulated.map { scenario.equityReturns[it] }.average(),
            averageInflation = simulated.map { scenario.inflationRates[it] }.average(),
            averageBondYield = simulated.map { scenario.bondYields[it] }.average()
        )
        return Run(scenario, result, path)
    }

    /** The market variables for run [runIndex], always the same for the same config, seed and history. */
    fun generateScenario(config: SimulationConfig, seed: Long, runIndex: Int, history: List<HistoricalYear>): MarketScenario {
        val random = java.util.Random(runSeed(seed, runIndex))
        val rates = config.rates
        val historicalMeanReturn = history.map { it.equityReturn }.average()
        val historicalMeanInflation = history.map { it.inflation }.average()

        val duration = config.endAge - config.currentAge
        val equityReturns = DoubleArray(duration + 1)
        val inflationRates = DoubleArray(duration + 1)
        val bondYields = DoubleArray(duration + 1)
        // Start year: not simulated, holds the configured rates
        equityReturns[0] = rates.preRetirementGrowth
        inflationRates[0] = rates.inflation
        bondYields[0] = rates.bondYield

        for (yearIdx in 1..duration) {
            val isWorkingYear = config.currentAge + yearIdx <= config.retirementAge
            val growth = if (isWorkingYear) rates.preRetirementGrowth else rates.postRetirementGrowth
            if (history.isNotEmpty()) {
                val year = history[random.nextInt(history.size)]
                equityReturns[yearIdx] = growth + (year.equityReturn - historicalMeanReturn)
                inflationRates[yearIdx] = rates.inflation + (year.inflation - historicalMeanInflation)
            } else {
                equityReturns[yearIdx] = growth + random.nextGaussian() * EQUITY_RETURN_SD
                inflationRates[yearIdx] = rates.inflation + random.nextGaussian() * INFLATION_SD
            }
            equityReturns[yearIdx] = equityReturns[yearIdx].coerceAtLeast(MIN_RETURN)
            bondYields[yearIdx] = (rates.bondYield + random.nextGaussian() * BOND_YIELD_SD).coerceAtLeast(0.0)
        }

        return MarketScenario(
            years = (config.currentYear..config.currentYear + duration).toList(),
            equityReturns = equityReturns.toList(),
            inflationRates = inflationRates.toList(),
            bondYields = bondYields.toList()
        )
    }

    /** Spread run seeds apart so neighbouring runs don't get correlated random streams. */
    private fun runSeed(seed: Long, runIndex: Int): Long = seed + runIndex * -0x61c8864680b583ebL

    /**
     * Confidence lines per year. A run that failed (or stopped early) counts as 0 for its missing years.
     * The N% line is the balance that N% of runs reach or exceed, i.e. the (100 − N)th percentile of balances.
     */
    internal fun statistics(paths: List<MonteCarloPath>, yearCount: Int): Statistics {
        fun line(confidence: Double) = (0 until yearCount).map { i ->
            percentile(paths.map { it.totalBalances.getOrElse(i) { 0.0 } }, 1.0 - confidence)
        }
        return Statistics(
            medianPath = line(0.50),
            percentile75Path = line(0.75),
            percentile90Path = line(0.90),
            successRate = if (paths.isEmpty()) 0.0 else paths.count { it.isSuccess }.toDouble() / paths.size
        )
    }

    /** Linear-interpolated percentile, [fraction] from 0.0 (lowest) to 1.0 (highest). */
    internal fun percentile(values: List<Double>, fraction: Double): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val position = fraction * (sorted.size - 1)
        val lower = position.toInt()
        val upper = minOf(lower + 1, sorted.size - 1)
        return sorted[lower] + (sorted[upper] - sorted[lower]) * (position - lower)
    }

    private fun pct(decimal: Double) = "${(decimal * 100).toInt()}%"
}

fun Portfolio.total() = sb + cbb + tba + tda + tfa
