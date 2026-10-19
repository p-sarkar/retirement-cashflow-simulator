package com.retirement.logic

import com.retirement.data.HistoricalYear
import com.retirement.model.GuardrailReview
import com.retirement.model.Portfolio
import com.retirement.model.SimulationConfig

/**
 * How a spending strategy sets retirement Wants (spec 006, research R006-1). Which accounts pay is decided by the
 * account rules in [SpendingStrategy], shared by every strategy (FR006-015).
 */
interface SpendingPolicy {
    /** Whether the account rules cut Wants 10% in a down market with the SB at its floor (FR002-005). */
    val wantsCutInDownMarket: Boolean

    /** A review at the start of a retirement period; null when the strategy doesn't review its spending. */
    fun review(context: ReviewContext): ReviewDecision?
}

/**
 * The state at a review: the start of a retirement quarter, before that quarter's money moves (research R006-5).
 * Amounts are dollars of the review year; [wantsReal] and the configured Wants are today's dollars.
 */
data class ReviewContext(
    val config: SimulationConfig,
    val balances: Portfolio,
    val year: Int,
    val quarter: Int, // 1-4
    val age: Int,
    val yearIdx: Int, // years since the start year
    val inflationFactor: Double, // cumulative inflation since the start year
    // Cumulative inflation for years 0..yearIdx only: later years are unknown at the review (no look-ahead)
    val inflationFactorsToDate: DoubleArray,
    val priorYearTaxableIncome: Double, // taxed in the review year
    val wantsReal: Double, // the current Wants, today's dollars
    val history: List<HistoricalYear>,
    val reviewSeed: Long,
    val computeDollarGuardrails: Boolean
)

/** The Wants to use from the review on (today's dollars), and the review to report. */
data class ReviewDecision(val wantsReal: Double, val review: GuardrailReview)

/** "Partha's Spending Strategy-v0.01-20260105": the configured Wants with inflation, never reviewed. */
object FixedWantsPolicy : SpendingPolicy {
    override val wantsCutInDownMarket = true
    override fun review(context: ReviewContext): ReviewDecision? = null
}
