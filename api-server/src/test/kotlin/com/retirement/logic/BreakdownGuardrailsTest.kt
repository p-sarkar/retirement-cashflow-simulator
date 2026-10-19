package com.retirement.logic

import com.retirement.data.HistoricalDataService
import com.retirement.defaultFormConfig
import com.retirement.guardrailsCrashConfig
import com.retirement.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The guardrails section of the yearly breakdown (spec 006, US006-4, FR006-020). */
class BreakdownGuardrailsTest {

    private val history = HistoricalDataService.getHistoricalYears()

    private fun traced(config: SimulationConfig): Pair<SimulationResult, SimulationTrace> {
        val trace = SimulationTrace()
        val result = SimulationEngine.runSimulation(config, MarketPresets.equityReturns(config), emptyList(), trace = trace, history = history)
        return result to trace
    }

    private val config = guardrailsCrashConfig()
    private val traced = traced(config)
    private fun breakdown(age: Int, c: SimulationConfig = config, t: Pair<SimulationResult, SimulationTrace> = traced) =
        BreakdownGenerator.generate(c, t.first, t.second, age)

    @Test
    fun `a retirement year explains its review`() {
        val cutYear = traced.first.yearlyResults.first { row -> row.guardrails?.reviews?.any { it.outcome == GuardrailOutcome.CUT } == true }
        val g = assertNotNull(breakdown(cutYear.age).guardrails)
        assertEquals(config.strategy.guardrails.targetSuccess, g.targetSuccess)
        assertEquals(config.strategy.guardrails.lowerGuardrail, g.lowerGuardrail)
        assertEquals(config.strategy.guardrails.upperGuardrail, g.upperGuardrail)
        assertEquals("YEARLY", g.reviewFrequency)
        assertEquals(cutYear.guardrails!!.reviews, g.reviews)
        assertNull(g.noReviewNote)
    }

    @Test
    fun `the adjusted Wants match the Expenses section and say how they were computed`() {
        traced.first.yearlyResults.filter { it.guardrails != null }.forEach { row ->
            val b = breakdown(row.age)
            val g = assertNotNull(b.guardrails)
            val expensesWants = b.expenses!!.items.single { it.label == "Wants" }
            assertEquals(row.cashFlow.wants, g.adjustedWants.amount, 1e-6)
            assertEquals(expensesWants.amount, g.adjustedWants.amount, 1e-6)
            assertEquals(row.guardrails!!.configuredWants, g.configuredWants.amount, 1e-6)
            assertTrue(g.adjustedWants.rule.startsWith("Today's-dollar Wants × cumulative inflation"), g.adjustedWants.rule)
            // The Expenses section no longer claims the configured amount under guardrails
            assertTrue(expensesWants.rule.contains("guardrail", ignoreCase = true), expensesWants.rule)
        }
    }

    @Test
    fun `a quarterly year cut short by failure notes the reviews that didn't happen`() {
        val poor = guardrailsCrashConfig(GuardrailsSettings(reviewFrequency = "QUARTERLY"))
            .copy(portfolio = Portfolio(sb = 20000.0, cbb = 20000.0, tba = 60000.0, tda = 60000.0, tfa = 0.0))
        val t = traced(poor)
        val failed = t.first.yearlyResults.last()
        assertTrue(failed.metrics.isFailure)
        val g = assertNotNull(breakdown(failed.age, poor, t).guardrails)
        if (g.reviews.size < 4) assertNotNull(g.noReviewNote) else assertNull(g.noReviewNote)
        assertTrue(g.reviews.size in 1..4)
    }

    @Test
    fun `working years and the existing strategy have no guardrails section`() {
        val working = traced.first.yearlyResults.first { it.age <= config.retirementAge && it.age > config.currentAge }
        assertNull(breakdown(working.age).guardrails)
        val existing = defaultFormConfig()
        val t = traced(existing)
        t.first.yearlyResults.drop(1).forEach { assertNull(breakdown(it.age, existing, t).guardrails) }
    }

    @Test
    fun `a Monte Carlo run's breakdown has the section with dollar guardrails`() {
        val retiredAge = config.retirementAge + 1
        val b = assertNotNull(MonteCarloEngine.runBreakdown(config, seed = 5, runIndex = 0, targetAge = retiredAge, history = history))
        val g = assertNotNull(b.guardrails)
        assertTrue(g.reviews.single().lowerGuardrailPortfolio != null || g.reviews.single().upperGuardrailPortfolio != null)
    }
}
