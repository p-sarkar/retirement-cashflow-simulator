package com.retirement.model

import kotlinx.serialization.Serializable

@Serializable
data class SimulationResult(
    val config: SimulationConfig,
    val yearlyResults: List<YearlyResult>,
    val quarterlyResults: List<QuarterlyResult>,
    val summary: Summary
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
    val metrics: Metrics
)

@Serializable
data class CashFlow(
    val salary: Double,
    val interest: Double,
    val dividends: Double,
    val socialSecurity: Double,
    val tbaWithdrawal: Double,
    val tdaWithdrawal: Double,
    val sbDeposit: Double, // Inflow to SB
    val sbWithdrawal: Double, // Total Outflow from SB
    val rothConversion: Double,
    val contribution401k: Double,
    val contributionTba: Double,
    val totalIncome: Double,
    val needs: Double,
    val wants: Double,
    val healthcare: Double,
    val incomeTax: Double,
    val propertyTax: Double,
    val totalExpenses: Double
)

@Serializable
data class Metrics(
    val annualIncomeGap: Double,
    val incomeGapExpenses: Double, // Total expenses for gap calculation
    val incomeGapPassiveIncome: Double, // Interest + Dividends + Social Security
    val sbCap: Double, // Spend Bucket Cap (2× AIG)
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

@Serializable
data class SimulationRun(
    val config: SimulationConfig,
    val yearlyResults: List<YearlyResult>,
    val isSuccess: Boolean,
    val failureYear: Int?,
    val endingBalance: Double
)

@Serializable
data class MonteCarloResult(
    val runs: List<SimulationRun>,
    val statistics: Statistics
)

@Serializable
data class Statistics(
    val medianPath: List<Double>,
    val percentile75Path: List<Double>,
    val percentile90Path: List<Double>,
    val successRate: Double
)
