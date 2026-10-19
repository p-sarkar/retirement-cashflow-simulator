package com.retirement.logic

import com.retirement.data.HistoricalDataService
import com.retirement.guardrailsConfig
import com.retirement.model.*
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A guardrail review's decision (spec 006, FR006-012, SC006-003, research R006-4). */
class RiskBasedGuardrailsPolicyTest {

    private val history = HistoricalDataService.getHistoricalYears()
    private val settings = GuardrailsSettings()

    private class Recorder : SpendingPolicy {
        val contexts = mutableListOf<ReviewContext>()
        override val wantsCutInDownMarket = false
        override fun review(context: ReviewContext): ReviewDecision? { contexts += context; return null }
    }

    private fun contexts(config: SimulationConfig = guardrailsConfig()): List<ReviewContext> {
        val recorder = Recorder()
        SimulationEngine.runSimulation(config, emptyList(), emptyList(), history = history, policy = recorder)
        return recorder.contexts
    }

    private val first = contexts().first()
    private val configuredReal = first.config.expenses.wants

    private fun probability(context: ReviewContext, wantsReal: Double) =
        SuccessProjector(context, MarketPaths.generate(context)).probability(wantsReal)

    /** The first balance scale on a geometric grid whose context satisfies [accept]. */
    private fun scaled(base: ReviewContext = first, accept: (ReviewContext) -> Boolean): ReviewContext {
        val scales = generateSequence(0.02) { it * 1.08 }.takeWhile { it < 30.0 }
        return scales.map { s ->
            val b = base.balances
            base.copy(balances = Portfolio(b.sb * s, b.cbb * s, b.tba * s, b.tda * s, b.tfa * s))
        }.firstOrNull(accept) ?: error("no balance scale satisfies the scenario")
    }

    /** probabilityAfter within 0.5 point of the target, or the bisection stopped on a Wants interval under $100. */
    private fun assertOnTarget(context: ReviewContext, review: GuardrailReview) {
        val wantsAfterReal = review.wantsAfter / context.inflationFactor
        val onTarget = abs(review.probabilityAfter - settings.targetSuccess) <= GuardrailsDefaults.SUCCESS_TOLERANCE
        val intervalStop = review.probabilityAfter >= settings.targetSuccess &&
            probability(context, wantsAfterReal + GuardrailsDefaults.WANTS_TOLERANCE) < settings.targetSuccess
        assertTrue(onTarget || intervalStop, "probability after ${review.probabilityAfter}, target ${settings.targetSuccess}")
    }

    private fun decide(context: ReviewContext) = assertNotNull(RiskBasedGuardrailsPolicy.review(context))

    @Test
    fun `between the guardrails Wants are unchanged`() {
        val context = scaled { probability(it, it.wantsReal) in 0.75..0.95 }
        val d = decide(context)
        assertEquals(GuardrailOutcome.NO_CHANGE, d.review.outcome)
        assertEquals(d.review.wantsBefore, d.review.wantsAfter)
        assertEquals(context.wantsReal, d.wantsReal)
        assertEquals(d.review.probabilityBefore, d.review.probabilityAfter)
    }

    @Test
    fun `below the lower guardrail Wants are cut to the target`() {
        val context = scaled { probability(it, it.wantsReal) < 0.6 && probability(it, 0.0) > 0.95 }
        val d = decide(context)
        assertEquals(GuardrailOutcome.CUT, d.review.outcome)
        assertTrue(d.wantsReal < context.wantsReal)
        assertOnTarget(context, d.review)
    }

    @Test
    fun `above the upper guardrail Wants are raised to the target`() {
        val low = first.copy(wantsReal = configuredReal * 0.3)
        val context = scaled(low) {
            probability(it, it.wantsReal) > 0.995 && probability(it, configuredReal * GuardrailsDefaults.RAISE_CAP) < 0.85
        }
        val d = decide(context)
        assertEquals(GuardrailOutcome.RAISE, d.review.outcome)
        assertTrue(d.wantsReal > context.wantsReal)
        assertOnTarget(context, d.review)
    }

    @Test
    fun `a raise stops at 150 percent of the configured Wants`() {
        val context = scaled { probability(it, configuredReal * GuardrailsDefaults.RAISE_CAP) > 0.995 }
        val d = decide(context)
        assertEquals(GuardrailOutcome.RAISE_CAPPED, d.review.outcome)
        assertEquals(configuredReal * GuardrailsDefaults.RAISE_CAP, d.wantsReal, 1e-6)
    }

    @Test
    fun `when zero Wants still miss the target Wants become zero`() {
        val context = scaled { probability(it, 0.0) < 0.5 }
        val d = decide(context)
        assertEquals(GuardrailOutcome.TARGET_UNREACHABLE, d.review.outcome)
        assertEquals(0.0, d.wantsReal)
        assertEquals(0.0, d.review.wantsAfter)
    }

    @Test
    fun `with no configured Wants the only outcomes are no change and target unreachable (U006-2)`() {
        val noWants = contexts(guardrailsConfig().let { it.copy(expenses = it.expenses.copy(wants = 0.0)) }).first()
        assertEquals(0.0, noWants.wantsReal)
        val rich = scaled(noWants) { probability(it, 0.0) > 0.995 }
        assertEquals(GuardrailOutcome.NO_CHANGE, decide(rich).review.outcome)
        val poor = scaled(noWants) { probability(it, 0.0) < 0.5 }
        assertEquals(GuardrailOutcome.TARGET_UNREACHABLE, decide(poor).review.outcome)
    }

    @Test
    fun `reviews one and two years before the end age still decide`() {
        contexts().takeLast(2).forEach { context ->
            val d = decide(context)
            assertTrue(d.review.probabilityBefore in 0.0..1.0 && d.review.probabilityAfter in 0.0..1.0)
            if (d.review.outcome == GuardrailOutcome.CUT || d.review.outcome == GuardrailOutcome.RAISE) assertOnTarget(context, d.review)
        }
    }

    @Test
    fun `without dollar guardrails those fields are null, and the decision is repeatable`() {
        val d = decide(first.copy(computeDollarGuardrails = false))
        assertNull(d.review.lowerGuardrailPortfolio)
        assertNull(d.review.upperGuardrailPortfolio)
        assertNull(d.review.wantsAtLowerGuardrail)
        assertNull(d.review.wantsAtUpperGuardrail)
        assertEquals(d, decide(first.copy(computeDollarGuardrails = false)))
        assertEquals(first.year, d.review.year)
        assertEquals(first.balances.sb + first.balances.cbb + first.balances.tba + first.balances.tda + first.balances.tfa,
            d.review.portfolioValue, 1e-6)
    }

    // --- Dollar guardrails (US006-3) ---

    private fun withScale(context: ReviewContext, scale: Double) = context.balances.let { b ->
        context.copy(balances = Portfolio(b.sb * scale, b.cbb * scale, b.tba * scale, b.tda * scale, b.tfa * scale))
    }

    @Test
    fun `the lower guardrail portfolio is where the probability crosses the lower guardrail`() {
        val context = first.copy(computeDollarGuardrails = true)
        val review = decide(context).review
        val total = review.portfolioValue
        val lower = assertNotNull(review.lowerGuardrailPortfolio)
        assertTrue(probability(withScale(context, lower * 0.99 / total), context.wantsReal) < settings.lowerGuardrail)
        assertTrue(probability(withScale(context, lower * 1.01 / total), context.wantsReal) >= settings.lowerGuardrail)

        // The Wants that would follow that crossing bring the probability back to the target
        val atLower = withScale(context, lower / total)
        val wantsReal = assertNotNull(review.wantsAtLowerGuardrail) / context.inflationFactor
        val p = probability(atLower, wantsReal)
        assertTrue(abs(p - settings.targetSuccess) <= GuardrailsDefaults.SUCCESS_TOLERANCE ||
            (p >= settings.targetSuccess && probability(atLower, wantsReal + GuardrailsDefaults.WANTS_TOLERANCE) < settings.targetSuccess),
            "probability at the lower guardrail after the cut: $p")
        assertTrue(wantsReal < context.wantsReal)
    }

    @Test
    fun `the upper guardrail portfolio is where the probability passes the upper guardrail`() {
        val context = first.copy(computeDollarGuardrails = true)
        val review = decide(context).review
        val total = review.portfolioValue
        val upper = assertNotNull(review.upperGuardrailPortfolio)
        assertTrue(probability(withScale(context, upper * 1.01 / total), context.wantsReal) > settings.upperGuardrail)
        assertTrue(probability(withScale(context, upper * 0.99 / total), context.wantsReal) <= settings.upperGuardrail)
        assertTrue(assertNotNull(review.wantsAtUpperGuardrail) > review.wantsBefore)
    }

    @Test
    fun `the upper guardrail is null when no portfolio up to ten times reaches it`() {
        // Expenses no portfolio can carry to 99%: Needs alone far beyond ten times the balances' reach
        val heavy = first.copy(computeDollarGuardrails = true,
            config = first.config.copy(expenses = first.config.expenses.copy(needs = 5_000_000.0)))
        val review = decide(heavy).review
        assertNull(review.upperGuardrailPortfolio)
        assertNull(review.wantsAtUpperGuardrail)
    }
}

