package com.retirement.logic

import com.retirement.model.CashExpense
import com.retirement.model.LoanExpense
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The market paths a guardrail review projects over (spec 006, FR006-011a, research R006-3): [count] paths of
 * [years] years each, from the review year (step 0) through the End Age year. Values are decimals, stored
 * path-major: index = path × years + step.
 */
class MarketPaths(val count: Int, val years: Int, val equity: DoubleArray, val inflation: DoubleArray, val bond: DoubleArray) {
    companion object {
        /** Paths drawn with the Monte Carlo bootstrap at the post-retirement growth rate, from the review's seed. */
        fun generate(context: ReviewContext, count: Int = GuardrailsDefaults.PATH_COUNT): MarketPaths {
            val config = context.config
            val years = config.endAge - context.age + 1
            val random = java.util.Random(context.reviewSeed)
            val history = context.history
            val meanReturn = if (history.isEmpty()) 0.0 else history.map { it.equityReturn }.average()
            val meanInflation = if (history.isEmpty()) 0.0 else history.map { it.inflation }.average()
            val equity = DoubleArray(count * years)
            val inflation = DoubleArray(count * years)
            val bond = DoubleArray(count * years)
            for (i in 0 until count * years) {
                val draw = MarketDraw.drawYear(random, config.rates, config.rates.postRetirementGrowth, history, meanReturn, meanInflation)
                equity[i] = draw.equityReturn
                inflation[i] = draw.inflation
                bond[i] = draw.bondYield
            }
            return MarketPaths(count, years, equity, inflation, bond)
        }

        /** One path at the configured rates, with no volatility (for checking the projection against the engine). */
        fun constant(context: ReviewContext): MarketPaths {
            val rates = context.config.rates
            val years = context.config.endAge - context.age + 1
            return MarketPaths(1, years, DoubleArray(years) { rates.postRetirementGrowth },
                DoubleArray(years) { rates.inflation }, DoubleArray(years) { rates.bondYield })
        }

        /** A review's seed: the simulation's seed (a Monte Carlo run's, or a fixed one) spread by year and quarter. */
        fun reviewSeed(simulationSeed: Long, year: Int, quarter: Int): Long =
            simulationSeed + (year * 4L + quarter) * MonteCarloEngine.SEED_SPREAD
    }
}

/**
 * Projects a review's state over its [paths] to the End Age (research R006-2): a yearly projection of the same
 * cash flows as the simulation, with the portfolio as one pool that keeps the review's shares of cash (SB),
 * bonds (CBB) and equities (TBA + TDA + TFA). The bucket rules are left out on purpose; a test checks that at the
 * configured rates the projection runs out within a year of the engine.
 *
 * Each year: Needs, Wants, healthcare and property tax grow with the path's inflation; Social Security comes in;
 * one-time cash expenses and loan payments go out; income tax is the flat rate on the previous year's taxable
 * income. A review in quarter q projects only the year's remaining quarters, and treats the past quarters'
 * taxable income as a share of last year's.
 */
class SuccessProjector(private val context: ReviewContext, private val paths: MarketPaths) {
    private val config = context.config
    private val years = paths.years
    private val firstFraction = (5 - context.quarter) / 4.0 // share of the review year still to come

    // Per step, in today's dollars unless noted
    private val spendingWithoutWants = DoubleArray(years)
    private val socialSecurity = DoubleArray(years)
    private val rothConversion = DoubleArray(years)
    private val lumpSums = DoubleArray(years) // cash expenses and down payments (Q1 of their year)
    private val knownLoanPayments = DoubleArray(years) // nominal: loans that started by the review year
    private val futureLoans: List<FutureLoan>

    private class FutureLoan(val startStep: Int, val quarterlyPaymentToday: Double, val quarters: IntArray)

    private val cashShare: Double
    private val bondShare: Double
    private val equityShare: Double
    private val startingTotal: Double
    private val withdrawalTaxableShare = config.strategy.tdaWithdrawalPercentage +
        (1.0 - config.strategy.tdaWithdrawalPercentage) * 0.5 // TDA fully taxed, half of TBA withdrawals

    init {
        for (k in 0 until years) {
            val yearIdx = context.yearIdx + k
            val age = context.age + k
            spendingWithoutWants[k] = SimulationEngine.retirementSpendingWithoutWants(config, age, 1.0)
            socialSecurity[k] = SimulationEngine.socialSecurity(config, yearIdx, 1.0).total
            rothConversion[k] = if (yearIdx > 1) config.strategy.rothConversionPostRetirement else 0.0
        }
        val loans = mutableListOf<FutureLoan>()
        fun stepOf(year: Int) = year - context.year
        val lumpSumsDue = { k: Int -> k > 0 || context.quarter == 1 } // paid in Q1
        config.oneTimeExpenses.forEach { expense ->
            when (expense) {
                is CashExpense -> {
                    val k = stepOf(expense.yearOrAge.toYear(config.currentAge, config.currentYear))
                    if (k in 0 until years && lumpSumsDue(k)) lumpSums[k] += expense.amount
                }
                is LoanExpense -> {
                    val startYear = expense.startYearOrAge.toYear(config.currentAge, config.currentYear)
                    val endYear = expense.getEndYear(config.currentAge, config.currentYear)
                    val startStep = stepOf(startYear)
                    if (startStep in 0 until years && expense.downPayment > 0.0 && lumpSumsDue(startStep)) {
                        lumpSums[startStep] += expense.downPayment
                    }
                    if (expense.getFinancedAmount() > 0.0) {
                        val quarters = IntArray(years) { k ->
                            val year = context.year + k
                            if (year in startYear..endYear) (if (k == 0) 5 - context.quarter else 4) else 0
                        }
                        val payment = expense.quarterlyPaymentFor(1.0) // payments scale with the start year's inflation
                        if (startStep <= 0) {
                            val startIdx = startYear - config.currentYear
                            val factor = context.inflationFactorsToDate.getOrElse(startIdx) {
                                (1.0 + config.rates.inflation).pow(startIdx)
                            }
                            for (k in 0 until years) knownLoanPayments[k] += payment * factor * quarters[k]
                        } else if (startStep < years) {
                            loans += FutureLoan(startStep, payment, quarters)
                        }
                    }
                }
            }
        }
        futureLoans = loans

        val b = context.balances
        val total = b.sb + b.cbb + b.tba + b.tda + b.tfa
        startingTotal = total
        if (total > 0.0) {
            cashShare = max(0.0, b.sb) / total
            bondShare = max(0.0, b.cbb) / total
            equityShare = 1.0 - cashShare - bondShare
        } else {
            cashShare = 0.0; bondShare = 0.0; equityShare = 1.0
        }
    }

    /** The share of paths in which the portfolio lasts through the End Age year, at [wantsReal] (today's dollars). */
    fun probability(wantsReal: Double, balanceScale: Double = 1.0): Double {
        val inflationAtStep = DoubleArray(years)
        var successes = 0
        for (p in 0 until paths.count) {
            if (project(p, wantsReal, balanceScale, inflationAtStep) < 0) successes++
        }
        return successes.toDouble() / paths.count
    }

    /** The step at which [path] runs out of money, or null if it lasts. */
    fun depletionStep(path: Int, wantsReal: Double, balanceScale: Double = 1.0): Int? =
        project(path, wantsReal, balanceScale, DoubleArray(years)).takeIf { it >= 0 }

    /** Projects one path; returns the step it runs out in, or -1 if it lasts. */
    private fun project(path: Int, wantsReal: Double, balanceScale: Double, inflationAtStep: DoubleArray): Int {
        val rates = config.rates
        val base = path * years
        var pool = startingTotal * balanceScale
        var inflation = context.inflationFactor
        var taxablePrevious = context.priorYearTaxableIncome
        for (k in 0 until years) {
            if (k > 0) inflation *= 1.0 + paths.inflation[base + k]
            inflationAtStep[k] = inflation
            val fraction = if (k == 0) firstFraction else 1.0
            val income = cashShare * rates.hysaRate + bondShare * paths.bond[base + k]
            val growth = if (fraction == 1.0) 1.0 + income + equityShare * paths.equity[base + k]
                else (1.0 + income + equityShare * paths.equity[base + k]).pow(fraction)

            var loanPayments = knownLoanPayments[k]
            for (loan in futureLoans) {
                if (loan.quarters[k] > 0) loanPayments += loan.quarterlyPaymentToday * inflationAtStep[loan.startStep] * loan.quarters[k]
            }
            val spending = (spendingWithoutWants[k] + wantsReal) * inflation * fraction
            val tax = taxablePrevious * rates.incomeTax * fraction
            val socialSecurityIn = socialSecurity[k] * inflation * fraction
            val withdrawal = spending + tax + lumpSums[k] * inflation + loanPayments - socialSecurityIn

            val passiveIncome = pool * income * fraction
            // Withdrawals spread through the year: about half a year's growth on them
            pool = pool * growth - withdrawal * sqrt(growth)
            if (pool < 0.0) return k

            var taxable = socialSecurityIn + passiveIncome + max(0.0, withdrawal) * equityShare * withdrawalTaxableShare +
                rothConversion[k] * inflation * fraction
            if (k == 0) taxable += (1.0 - fraction) * context.priorYearTaxableIncome // the quarters already past
            taxablePrevious = taxable
        }
        return -1
    }
}
