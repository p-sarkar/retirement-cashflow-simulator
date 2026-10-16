package com.retirement.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiMetadata(
    val version: String,
    val buildTime: String,
    val serverStartTime: String
)

@Serializable
data class SimulationResult(
    val config: SimulationConfig,
    val yearlyResults: List<YearlyResult>,
    val quarterlyResults: List<QuarterlyResult>,
    val summary: Summary,
    val apiMetadata: ApiMetadata
)

@Serializable
data class QuarterlyResult(
    val year: Int,
    val quarter: Int,
    val age: Int,
    val balances: Portfolio,
    val cashFlow: CashFlow,
    val metrics: Metrics
)

@Serializable
data class YearlyResult(
    val year: Int,
    val age: Int,
    val balances: Portfolio,
    val cashFlow: CashFlow,
    val metrics: Metrics,
    val oneTimeExpensesBreakdown: List<ExpenseDetail>? = null // Breakdown when 1+ expenses in year
)

@Serializable
data class CashFlow(
    val salary: Double,
    val interest: Double,
    val dividends: Double,
    val socialSecurity: Double,
    val tbaWithdrawal: Double,
    val tdaWithdrawal: Double, // Total TDA withdrawal (spend + roth)
    val tdaWithdrawalSpend: Double, // TDA withdrawal for spending
    val tdaWithdrawalRoth: Double, // TDA withdrawal for Roth conversion
    val sbDeposit: Double, // Inflow to SB
    val sbWithdrawal: Double, // Total Outflow from SB
    val rothConversion: Double,
    val contribution401k: Double, // Traditional 401k contribution (to TDA)
    val contributionRoth: Double, // Roth contribution: Roth 401k and/or Roth IRA (to TFA)
    val contributionTba: Double,
    val totalIncome: Double,
    val needs: Double,
    val wants: Double,
    val healthcare: Double,
    val incomeTax: Double,
    val propertyTax: Double,
    val totalExpenses: Double,
    val oneTimeExpenses: Double = 0.0 // One-time expenses total for this period
)

@Serializable
data class Metrics(
    val annualIncomeGap: Double?, // null in working years (salary covers expenses) and for the start-year snapshot
    val incomeGapExpenses: Double, // Total expenses for gap calculation
    val incomeGapPassiveIncome: Double, // Interest + Dividends + Social Security
    val sbCap: Double?, // Spend Bucket Cap; null in working years, where it is not enforced
    val cbbCap: Double,
    val isFailure: Boolean
)


@Serializable
data class Summary(
    val finalTotalBalance: Double,
    val isSuccess: Boolean,
    val failureYear: Int?,
    val totalDividends: Double,
    val totalInterest: Double
)

// --- Monte Carlo (US002-3/US002-4) ---

@Serializable
data class MonteCarloRequest(
    val config: SimulationConfig,
    val runs: Int = 1000,
    val seed: Long? = null // null = pick one; the result returns it so the analysis can be reproduced
)

/** Reruns one Monte Carlo run in full (US002-4 "Load Details"); same config and seed as the analysis. */
@Serializable
data class MonteCarloRunRequest(
    val config: SimulationConfig,
    val seed: Long,
    val runIndex: Int
)

/**
 * The variables of one Monte Carlo run, one entry per year from the start year (index 0, not simulated,
 * holds the config's rates) through the End Age year. Rates are decimals.
 */
@Serializable
data class MarketScenario(
    val years: List<Int>,
    val equityReturns: List<Double>,
    val inflationRates: List<Double>,
    val bondYields: List<Double>
)

/** Summary of one run: what the chart plots and what clicking a path shows. */
@Serializable
data class MonteCarloPath(
    val runIndex: Int,
    val totalBalances: List<Double>, // Total portfolio at the end of each year, from the start year; stops at the failure year
    val isSuccess: Boolean,
    val failureYear: Int?,
    val endingBalance: Double,
    val averageReturn: Double,
    val averageInflation: Double,
    val averageBondYield: Double
)

/**
 * Year-by-year confidence lines across all runs, aligned with MonteCarloResult.years. A failed run
 * counts as 0 after its failure year. percentileNPath is the balance that N% of runs reach or exceed
 * (so the 90th percentile line is the most conservative); medianPath is the 50% line.
 */
@Serializable
data class Statistics(
    val medianPath: List<Double>,
    val percentile75Path: List<Double>,
    val percentile90Path: List<Double>,
    val successRate: Double // Fraction of runs that reached End Age (0.0-1.0)
)

@Serializable
data class MonteCarloResult(
    val seed: Long,
    val runCount: Int,
    val method: String, // How the market scenarios were generated
    val years: List<Int>,
    val paths: List<MonteCarloPath>,
    val statistics: Statistics,
    val apiMetadata: ApiMetadata
)

@Serializable
data class MonteCarloRunDetail(
    val seed: Long,
    val path: MonteCarloPath,
    val scenario: MarketScenario,
    val result: SimulationResult
)

// --- Saved simulations (US002-2) ---

@Serializable
data class SaveSimulationRequest(
    val name: String,
    val result: SimulationResult
)

@Serializable
data class SavedSimulationSummary(
    val id: String,
    val name: String,
    val createdAt: String, // ISO-8601 with offset, e.g. 2026-10-15T10:49:03-07:00
    val isSuccess: Boolean,
    val failureYear: Int?,
    val finalTotalBalance: Double,
    val endAge: Int
)

@Serializable
data class SavedSimulation(
    val summary: SavedSimulationSummary,
    val result: SimulationResult
)
