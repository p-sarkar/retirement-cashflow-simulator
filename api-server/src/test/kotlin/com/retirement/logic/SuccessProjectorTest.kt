package com.retirement.logic

import com.retirement.data.HistoricalDataService
import com.retirement.defaultFormConfig
import com.retirement.guardrailsConfig
import com.retirement.model.*
import com.retirement.presetConfigs
import com.retirement.testConfig
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The probability-of-success projection behind guardrail reviews (spec 006, FR006-011, research R006-2). */
class SuccessProjectorTest {

    private val history = HistoricalDataService.getHistoricalYears()

    /** Records every review state the engine reaches, changing nothing. */
    private class Recorder : SpendingPolicy {
        val contexts = mutableListOf<ReviewContext>()
        override val wantsCutInDownMarket = true
        override fun review(context: ReviewContext): ReviewDecision? { contexts += context; return null }
    }

    private fun reviews(config: SimulationConfig): Pair<List<ReviewContext>, SimulationResult> {
        val recorder = Recorder()
        val result = SimulationEngine.runSimulation(config, emptyList(), emptyList(), history = history, policy = recorder)
        return recorder.contexts to result
    }

    private fun firstReview(config: SimulationConfig) = reviews(config).first.first()

    private fun scaled(config: SimulationConfig, scale: Double) = config.portfolio.let { b ->
        config.copy(portfolio = Portfolio(b.sb * scale, b.cbb * scale, b.tba * scale, b.tda * scale, b.tfa * scale))
    }

    /** Plans that retire at the start of the simulation, so a Monte Carlo of the engine starts from the review state. */
    private val retiringNow = listOf(
        testConfig(currentAge = 65, retirementAge = 64),
        defaultFormConfig().let { it.copy(currentAge = 56, retirementAge = 55, spousal = it.spousal.copy(spouseAge = 54), oneTimeExpenses = emptyList()) }
    )

    /**
     * The projection approximates the engine (research R006-2): measured against the engine's own Monte Carlo it
     * reads up to a few points high, because it doesn't model the strategy moving equities into the lower-yielding
     * Crash Buffer. Where the plan succeeds at least half the time they agree within 10 points.
     */
    @Test
    fun `the probability agrees with a Monte Carlo of the engine`() {
        for ((config, scale) in listOf(retiringNow[0] to 0.35, retiringNow[0] to 0.5, retiringNow[0] to 0.7,
                retiringNow[1] to 0.7, retiringNow[1] to 1.0)) {
            val plan = scaled(config, scale)
            val context = firstReview(plan)
            val projected = SuccessProjector(context, MarketPaths.generate(context)).probability(context.wantsReal)
            val engine = kotlinx.coroutines.runBlocking { MonteCarloEngine.run(plan, runs = 500, seed = 11, history = history) }
                .statistics.successRate
            val label = "${plan.name} at ${scale}x: projected $projected, engine Monte Carlo $engine"
            if (engine >= 0.5) assertTrue(abs(projected - engine) <= 0.10, label)
            assertTrue(projected - engine in -0.10..0.20, label)
        }
    }

    @Test
    fun `at the configured rates the projection succeeds where the engine does`() {
        val succeeding = (listOf(testConfig(), defaultFormConfig()) + presetConfigs())
            .map { reviews(it) }.filter { (_, result) -> result.summary.failureYear == null }
        assertTrue(succeeding.size >= 4, "expected most plans to succeed at their static rates")
        succeeding.forEach { (contexts, result) ->
            val context = contexts.first()
            assertEquals(null, SuccessProjector(context, MarketPaths.constant(context)).depletionStep(0, context.wantsReal), result.config.name)
        }
    }

    @Test
    fun `the probability falls as Wants rise and rises with the portfolio`() {
        val context = firstReview(guardrailsConfig())
        val projector = SuccessProjector(context, MarketPaths.generate(context))
        val byWants = listOf(0.0, 0.5, 1.0, 1.5, 3.0).map { projector.probability(context.wantsReal * it) }
        val byScale = listOf(0.25, 0.5, 1.0, 2.0, 4.0).map { projector.probability(context.wantsReal, it) }
        assertEquals(byWants.sortedDescending(), byWants)
        assertEquals(byScale.sorted(), byScale)
        assertTrue(byWants.first() > byWants.last(), "Wants must matter: $byWants")
    }

    @Test
    fun `an empty portfolio fails and income covering every expense succeeds`() {
        val empty = firstReview(testConfig()).let { it.copy(balances = Portfolio(0.0, 0.0, 0.0, 0.0, 0.0)) }
        assertEquals(0.0, SuccessProjector(empty, MarketPaths.generate(empty)).probability(empty.wantsReal))

        val rich = testConfig().let {
            it.copy(spousal = it.spousal.copy(
                lowerEarner = SocialSecurityDetails(62, 500000.0), higherEarner = SocialSecurityDetails(62, 500000.0)))
        }
        val covered = firstReview(rich)
        assertEquals(1.0, SuccessProjector(covered, MarketPaths.generate(covered)).probability(covered.wantsReal))
    }

    @Test
    fun `one-time expenses and loan payments in the remaining years lower the probability`() {
        val context = firstReview(guardrailsConfig())
        val paths = MarketPaths.generate(context)
        val base = SuccessProjector(context, paths).probability(context.wantsReal)
        val age = context.age + 10
        val cash = context.copy(config = context.config.copy(oneTimeExpenses = context.config.oneTimeExpenses +
            CashExpense("big", "Big gift", 1500000.0, YearOrAge.Age(age))))
        val loan = context.copy(config = context.config.copy(oneTimeExpenses = context.config.oneTimeExpenses +
            LoanExpense("boat", "Boat", 1500000.0, 6.0, 10, YearOrAge.Age(age))))
        assertTrue(SuccessProjector(cash, paths).probability(context.wantsReal) < base)
        assertTrue(SuccessProjector(loan, paths).probability(context.wantsReal) < base)
    }

    @Test
    fun `market paths depend only on the review seed`() {
        val context = firstReview(guardrailsConfig())
        val a = MarketPaths.generate(context)
        val b = MarketPaths.generate(context)
        val other = MarketPaths.generate(context.copy(reviewSeed = context.reviewSeed + 1))
        assertTrue(a.equity.contentEquals(b.equity) && a.inflation.contentEquals(b.inflation) && a.bond.contentEquals(b.bond))
        assertFalse(a.equity.contentEquals(other.equity))
        assertEquals(GuardrailsDefaults.PATH_COUNT, a.count)
        assertEquals(context.config.endAge - context.age + 1, a.years)
    }
}
