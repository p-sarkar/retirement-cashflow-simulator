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
    val metrics: Metrics,
    val market: MarketRates? = null // The year's rates (FR002-009); null for the start-year snapshot
)

@Serializable
data class YearlyResult(
    val year: Int,
    val age: Int,
    val balances: Portfolio,
    val cashFlow: CashFlow,
    val metrics: Metrics,
    val oneTimeExpensesBreakdown: List<ExpenseDetail>? = null, // Breakdown when 1+ expenses in year
    // Null for the start-year snapshot, where no rates apply, and in results saved before rows had them
    val market: MarketRates? = null,
    val guardrails: YearGuardrails? = null // Risk-Based Guardrails retirement years only (spec 006, FR006-018)
)

/** The market rates applied in a simulated year (FR002-009), as decimals: configured, a preset's or a Monte Carlo run's. */
@Serializable
data class MarketRates(
    val equityReturn: Double,
    val inflation: Double,
    val bondYield: Double
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
    val tfaWithdrawal: Double = 0.0, // Tax Free withdrawal for spending, the last resort (#23); untaxed
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
    val totalInterest: Double,
    val guardrails: GuardrailsSummary? = null // Risk-Based Guardrails only (spec 006, FR006-019)
)

// --- Risk-Based Guardrails (spec 006) ---

/** The result of one guardrail review (FR006-018). */
@Serializable
enum class GuardrailOutcome { NO_CHANGE, CUT, RAISE, RAISE_CAPPED, TARGET_UNREACHABLE }

/**
 * One guardrail review, at the start of a retirement quarter. Wants are dollars of the review year;
 * probabilities are decimals. The dollar guardrails (US006-3) are null when not computed (Monte Carlo batches).
 */
@Serializable
data class GuardrailReview(
    val year: Int,
    val quarter: Int, // 1-4
    val age: Int,
    val portfolioValue: Double,
    val probabilityBefore: Double,
    val outcome: GuardrailOutcome,
    val wantsBefore: Double,
    val wantsAfter: Double,
    val spendingBefore: Double, // the year's Needs + Wants + healthcare + property tax before the review
    val probabilityAfter: Double,
    val lowerGuardrailPortfolio: Double? = null,
    val upperGuardrailPortfolio: Double? = null,
    val wantsAtLowerGuardrail: Double? = null,
    val wantsAtUpperGuardrail: Double? = null
)

/** A retirement year under Risk-Based Guardrails: its spending and reviews (FR006-018). */
@Serializable
data class YearGuardrails(
    val configuredWants: Double, // configured Wants with this year's inflation, beside cashFlow.wants (the adjusted Wants)
    val annualSpendingTodaysDollars: Double, // Needs + Wants + healthcare + property tax, divided by cumulative inflation
    val reviews: List<GuardrailReview> // 1 for yearly reviews, up to 4 for quarterly
)

/** Summary of a guardrails simulation (FR006-019). Spending is in today's dollars. */
@Serializable
data class GuardrailsSummary(
    val startingProbability: Double,
    val startingLowerGuardrailPortfolio: Double? = null,
    val startingUpperGuardrailPortfolio: Double? = null,
    val cuts: Int, // CUT and TARGET_UNREACHABLE
    val raises: Int, // RAISE and RAISE_CAPPED
    val largestCutPercent: Double?, // largest (wantsBefore - wantsAfter) / annual spending before the cut; null without cuts
    val lowestAnnualSpending: Double,
    val highestAnnualSpending: Double,
    val configuredSpendingUnreachable: Boolean // the first review cut spending (spec edge case)
)

/** Annual spending across Monte Carlo runs, today's dollars, aligned with MonteCarloResult.years (FR006-021). */
@Serializable
data class SpendingStatistics(
    val percentile10Path: List<Double?>, // null for working years
    val medianPath: List<Double?>,
    val percentile90Path: List<Double?>,
    val shareOfRunsWithCut: Double // 0.0-1.0
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

/** The computation breakdown for one year of one Monte Carlo run (spec 004 US004-6). */
@Serializable
data class MonteCarloRunBreakdownRequest(
    val config: SimulationConfig,
    val seed: Long,
    val runIndex: Int,
    val targetAge: Int
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
    val averageBondYield: Double,
    val hadSpendingCut: Boolean? = null // Risk-Based Guardrails only: any review was CUT or TARGET_UNREACHABLE
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
    val successRate: Double, // Fraction of runs that reached End Age (0.0-1.0)
    val spending: SpendingStatistics? = null // Risk-Based Guardrails only (spec 006)
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
