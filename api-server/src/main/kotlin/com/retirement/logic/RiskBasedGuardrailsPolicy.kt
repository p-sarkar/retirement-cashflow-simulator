package com.retirement.logic

import com.retirement.model.GuardrailOutcome
import com.retirement.model.GuardrailReview

/** Fixed values of the Risk-Based Guardrails strategy (spec 006, data-model "GuardrailsSettings"). */
object GuardrailsDefaults {
    const val PATH_COUNT = 500 // market paths per review (FR006-011a)
    const val INTERACTIVE_SEED = 20261019L // interactive runs have no seed of their own (R006-3)
    const val RAISE_CAP = 1.5 // Wants never rise above 150% of the configured Wants (FR006-012)
    const val SUCCESS_TOLERANCE = 0.005 // an adjustment lands within 0.5 percentage point of the target
    const val WANTS_TOLERANCE = 100.0 // or within $100 a year of Wants
}

/**
 * Risk-Based Guardrails (spec 006, FR006-012): at each review, measure the probability of success at the current
 * Wants. Below the lower guardrail, cut Wants to the level that brings it back to the target; above the upper
 * guardrail, raise them to that level, at most to 150% of the configured Wants. Only Wants change (FR006-014);
 * the existing account rules still decide which accounts pay, without their 10% down-market Wants cut (FR006-015).
 *
 * Every probability in a review comes from the same market paths, so it falls steadily as Wants rise and
 * bisection converges (research R006-4).
 */
object RiskBasedGuardrailsPolicy : SpendingPolicy {
    override val wantsCutInDownMarket = false

    override fun review(context: ReviewContext): ReviewDecision {
        val settings = context.config.strategy.guardrails
        val target = settings.targetSuccess
        val projector = SuccessProjector(context, MarketPaths.generate(context))
        val current = context.wantsReal
        val cap = context.config.expenses.wants * GuardrailsDefaults.RAISE_CAP
        val before = projector.probability(current)

        val (outcome, wantsReal) = when {
            before < settings.lowerGuardrail ->
                if (projector.probability(0.0) < target) GuardrailOutcome.TARGET_UNREACHABLE to 0.0
                else GuardrailOutcome.CUT to solveWants(projector, target, 0.0, current)
            before > settings.upperGuardrail && cap > current ->
                if (projector.probability(cap) > target) GuardrailOutcome.RAISE_CAPPED to cap
                else GuardrailOutcome.RAISE to solveWants(projector, target, current, cap)
            else -> GuardrailOutcome.NO_CHANGE to current
        }
        val after = if (wantsReal == current) before else projector.probability(wantsReal)
        val inflation = context.inflationFactor
        val total = context.balances.let { it.sb + it.cbb + it.tba + it.tda + it.tfa }
        // The guardrails in dollars (US006-3): the portfolio at which each is crossed, and the Wants that would follow
        val lowerScale = if (context.computeDollarGuardrails) crossingScale(projector, current, settings.lowerGuardrail, strictlyAbove = false) else null
        val upperScale = if (context.computeDollarGuardrails) crossingScale(projector, current, settings.upperGuardrail, strictlyAbove = true) else null
        val wantsAtLower = lowerScale?.let { scale ->
            if (projector.probability(0.0, scale) < target) 0.0 else solveWants(projector, target, 0.0, current, scale)
        }
        val wantsAtUpper = upperScale?.let { scale ->
            if (cap <= current) current
            else if (projector.probability(cap, scale) > target) cap
            else solveWants(projector, target, current, cap, scale)
        }
        val review = GuardrailReview(
            year = context.year,
            quarter = context.quarter,
            age = context.age,
            portfolioValue = context.balances.let { it.sb + it.cbb + it.tba + it.tda + it.tfa },
            probabilityBefore = before,
            outcome = outcome,
            wantsBefore = current * inflation,
            wantsAfter = wantsReal * inflation,
            spendingBefore = SimulationEngine.retirementSpendingWithoutWants(context.config, context.age, inflation) + current * inflation,
            probabilityAfter = after,
            lowerGuardrailPortfolio = lowerScale?.let { it * total },
            upperGuardrailPortfolio = upperScale?.let { it * total },
            wantsAtLowerGuardrail = wantsAtLower?.let { it * inflation },
            wantsAtUpperGuardrail = wantsAtUpper?.let { it * inflation }
        )
        return ReviewDecision(wantsReal, review)
    }

    /** Dollar guardrails are searched up to this multiple of the current portfolio. */
    private const val MAX_SCALE = 10.0
    private const val SCALE_PRECISION = 0.001 // 0.1% of the portfolio

    /**
     * The smallest balance scale (all accounts together) at which the probability at [wantsReal] reaches the
     * [guardrail] (or, with [strictlyAbove], passes it), to within 0.1% of the portfolio: just below it the
     * guardrail isn't reached. Null when no scale in 0..10 crosses it.
     */
    private fun crossingScale(projector: SuccessProjector, wantsReal: Double, guardrail: Double, strictlyAbove: Boolean): Double? {
        fun crossed(scale: Double) = projector.probability(wantsReal, scale).let { if (strictlyAbove) it > guardrail else it >= guardrail }
        if (!crossed(MAX_SCALE) || crossed(0.0)) return null
        var lo = 0.0 // not crossed
        var hi = MAX_SCALE // crossed
        while (hi - lo > SCALE_PRECISION * hi) {
            val mid = (lo + hi) / 2.0
            if (crossed(mid)) hi = mid else lo = mid
        }
        return hi
    }

    /**
     * The Wants (today's dollars) in [low]..[high] at which the probability of success is the target: within
     * SUCCESS_TOLERANCE of it, or the highest Wants still at or above it once the interval is under WANTS_TOLERANCE.
     * The probability is at least the target at [low] and below it at [high].
     */
    internal fun solveWants(projector: SuccessProjector, target: Double, low: Double, high: Double, balanceScale: Double = 1.0): Double {
        var lo = low // probability >= target
        var hi = high // probability < target
        while (hi - lo >= GuardrailsDefaults.WANTS_TOLERANCE) {
            val mid = (lo + hi) / 2.0
            val p = projector.probability(mid, balanceScale)
            if (kotlin.math.abs(p - target) <= GuardrailsDefaults.SUCCESS_TOLERANCE) return mid
            if (p >= target) lo = mid else hi = mid
        }
        return lo
    }
}
