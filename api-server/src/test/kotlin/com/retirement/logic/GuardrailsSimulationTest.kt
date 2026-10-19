package com.retirement.logic

import com.retirement.data.HistoricalDataService
import com.retirement.defaultFormConfig
import com.retirement.guardrailsConfig
import com.retirement.guardrailsCrashConfig
import com.retirement.model.*
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Whole simulations under Risk-Based Guardrails (spec 006, US006-2). */
class GuardrailsSimulationTest {

    private val history = HistoricalDataService.getHistoricalYears()

    private fun run(config: SimulationConfig) =
        SimulationEngine.runSimulation(config, MarketPresets.equityReturns(config), emptyList(), history = history)

    private val config = guardrailsConfig()
    private val result = run(config)
    private val retired = result.yearlyResults.filter { it.age > config.retirementAge }

    @Test
    fun `the first review is at the start of the first retirement year, then one a year`() {
        result.yearlyResults.filter { it.age <= config.retirementAge }.forEach { assertNull(it.guardrails, "age ${it.age}") }
        assertEquals(config.retirementAge + 1, retired.first().age)
        retired.forEach { row ->
            val reviews = assertNotNull(row.guardrails).reviews
            assertEquals(1, reviews.size, "year ${row.year}")
            assertEquals(row.year, reviews.single().year)
            assertEquals(1, reviews.single().quarter)
        }
    }

    @Test
    fun `Wants follow the review and grow with inflation in between (FR006-013)`() {
        retired.forEach { row -> assertEquals(row.guardrails!!.reviews.single().wantsAfter, row.cashFlow.wants, 1e-6) }
        retired.zipWithNext().forEach { (previous, row) ->
            val review = row.guardrails!!.reviews.single()
            assertEquals(previous.cashFlow.wants * (1 + config.rates.inflation), review.wantsBefore, 1e-6)
        }
    }

    @Test
    fun `each year reports its configured Wants and annual spending`() {
        retired.forEach { row ->
            val inflation = (1 + config.rates.inflation).pow(row.year - config.currentYear)
            val g = row.guardrails!!
            assertEquals(config.expenses.wants * inflation, g.configuredWants, 1e-6)
            val c = row.cashFlow
            assertEquals((c.needs + c.wants + c.healthcare + c.propertyTax) / inflation, g.annualSpendingTodaysDollars, 1e-6)
        }
    }

    @Test
    fun `the summary describes the reviews (FR006-019)`() {
        val s = assertNotNull(result.summary.guardrails)
        val reviews = retired.flatMap { it.guardrails!!.reviews }
        assertEquals(reviews.first().probabilityBefore, s.startingProbability)
        val cuts = reviews.filter { it.outcome == GuardrailOutcome.CUT || it.outcome == GuardrailOutcome.TARGET_UNREACHABLE }
        assertEquals(cuts.size, s.cuts)
        assertEquals(reviews.count { it.outcome == GuardrailOutcome.RAISE || it.outcome == GuardrailOutcome.RAISE_CAPPED }, s.raises)
        val spending = retired.map { it.guardrails!!.annualSpendingTodaysDollars }
        assertEquals(spending.min(), s.lowestAnnualSpending, 1e-6)
        assertEquals(spending.max(), s.highestAnnualSpending, 1e-6)
        assertEquals(reviews.first().outcome in setOf(GuardrailOutcome.CUT, GuardrailOutcome.TARGET_UNREACHABLE), s.configuredSpendingUnreachable)
        if (cuts.isEmpty()) assertNull(s.largestCutPercent) else assertTrue(s.largestCutPercent!! > 0.0)
    }

    @Test
    fun `a crash in the first retirement year cuts Wants at the next review, and the plan lasts as long (SC006-004)`() {
        val crash = guardrailsCrashConfig()
        val r = run(crash)
        val secondYear = r.yearlyResults.first { it.age == crash.retirementAge + 2 }
        assertEquals(GuardrailOutcome.CUT, secondYear.guardrails!!.reviews.single().outcome)

        val fixed = run(crash.copy(strategy = crash.strategy.copy(type = SpendingStrategyCatalog.EXISTING)))
        val guardrailsFailure = r.summary.failureYear
        val fixedFailure = fixed.summary.failureYear
        assertTrue(guardrailsFailure == null || (fixedFailure != null && guardrailsFailure >= fixedFailure),
            "guardrails fail in $guardrailsFailure, fixed spending in $fixedFailure")
    }

    @Test
    fun `reviews stop at the failure year (FR006-016)`() {
        val poor = config.copy(portfolio = Portfolio(sb = 20000.0, cbb = 20000.0, tba = 50000.0, tda = 50000.0, tfa = 0.0))
        val r = run(poor)
        val failureYear = assertNotNull(r.summary.failureYear)
        assertEquals(failureYear, r.yearlyResults.last().year)
        assertNotNull(r.summary.guardrails)
    }

    @Test
    fun `guardrails turn off the downturn Wants cut (FR006-015)`() {
        assertFalse(RiskBasedGuardrailsPolicy.wantsCutInDownMarket)
        assertTrue(FixedWantsPolicy.wantsCutInDownMarket)
    }

    @Test
    fun `the existing strategy has no guardrail results (FR006-022)`() {
        val existing = run(defaultFormConfig())
        existing.yearlyResults.forEach { assertNull(it.guardrails) }
        assertNull(existing.summary.guardrails)
    }

    @Test
    fun `the same config gives the same results (FR006-017, SC006-007)`() {
        assertEquals(result.copy(apiMetadata = run(config).apiMetadata), run(config))
    }

    // --- Quarterly reviews (T006-035) ---

    private val quarterly = guardrailsCrashConfig(GuardrailsSettings(reviewFrequency = "QUARTERLY"))
    private val quarterlyResult = run(quarterly)

    @Test
    fun `quarterly reviews happen at the start of every retirement quarter`() {
        quarterlyResult.yearlyResults.filter { it.age > quarterly.retirementAge }.forEach { row ->
            assertEquals(listOf(1, 2, 3, 4), row.guardrails!!.reviews.map { it.quarter }, "year ${row.year}")
        }
    }

    @Test
    fun `a quarterly review changes Wants from its quarter on, and the quarters add up to the year`() {
        val retiredYears = quarterlyResult.yearlyResults.filter { it.age > quarterly.retirementAge }
        val midYearChange = retiredYears.flatMap { it.guardrails!!.reviews }
            .firstOrNull { it.quarter > 1 && it.outcome != GuardrailOutcome.NO_CHANGE }
        assertNotNull(midYearChange, "the crash plan should change Wants mid-year")
        retiredYears.forEach { row ->
            val quarters = quarterlyResult.quarterlyResults.filter { it.year == row.year }
            assertEquals(row.cashFlow.wants, quarters.sumOf { it.cashFlow.wants }, 1e-6)
            row.guardrails!!.reviews.forEach { review ->
                val q = quarters.single { it.quarter == review.quarter - 1 } // quarterly results count quarters from 0
                assertEquals(review.wantsAfter / 4.0, q.cashFlow.wants, 1e-6, "${row.year} Q${review.quarter}")
            }
        }
    }

    @Test
    fun `the SB cap follows the quarter's Wants`() {
        val change = quarterlyResult.yearlyResults.flatMap { it.guardrails?.reviews.orEmpty() }
            .first { it.quarter > 1 && it.outcome != GuardrailOutcome.NO_CHANGE }
        val quarters = quarterlyResult.quarterlyResults.filter { it.year == change.year }.associateBy { it.quarter + 1 }
        // The quarter's SB cap uses its own Wants, so it moves when a review changes Wants mid-year
        assertTrue(quarters[change.quarter]!!.metrics.sbCap != quarters[change.quarter - 1]!!.metrics.sbCap)
    }

    @Test
    fun `quarterly results are repeatable`() {
        assertEquals(quarterlyResult.copy(apiMetadata = run(quarterly).apiMetadata), run(quarterly))
    }

    // --- Dollar guardrails (US006-3) ---

    private fun withoutDollarGuardrails(r: SimulationResult) = r.copy(
        yearlyResults = r.yearlyResults.map { row ->
            row.copy(guardrails = row.guardrails?.let { g ->
                g.copy(reviews = g.reviews.map {
                    it.copy(lowerGuardrailPortfolio = null, upperGuardrailPortfolio = null, wantsAtLowerGuardrail = null, wantsAtUpperGuardrail = null)
                })
            })
        },
        summary = r.summary.copy(guardrails = r.summary.guardrails?.copy(startingLowerGuardrailPortfolio = null, startingUpperGuardrailPortfolio = null))
    )

    @Test
    fun `dollar guardrails never change the simulation (R006-4)`() {
        val crash = guardrailsCrashConfig()
        val on = SimulationEngine.runSimulation(crash, MarketPresets.equityReturns(crash), emptyList(), history = history, computeDollarGuardrails = true)
        val off = SimulationEngine.runSimulation(crash, MarketPresets.equityReturns(crash), emptyList(), history = history, computeDollarGuardrails = false)
        assertTrue(on.yearlyResults.flatMap { it.guardrails?.reviews.orEmpty() }.any { it.lowerGuardrailPortfolio != null })
        assertTrue(off.yearlyResults.flatMap { it.guardrails?.reviews.orEmpty() }.all { it.lowerGuardrailPortfolio == null })
        assertEquals(withoutDollarGuardrails(on), off.copy(apiMetadata = on.apiMetadata))
    }

    @Test
    fun `the summary starts from the first review's dollar guardrails`() {
        val first = retired.first().guardrails!!.reviews.first()
        val s = result.summary.guardrails!!
        assertEquals(first.lowerGuardrailPortfolio, s.startingLowerGuardrailPortfolio)
        assertEquals(first.upperGuardrailPortfolio, s.startingUpperGuardrailPortfolio)
        assertNotNull(s.startingLowerGuardrailPortfolio)
    }

    @Test
    fun `a Monte Carlo batch leaves the dollar guardrails out and a rebuilt run fills them`() = kotlinx.coroutines.runBlocking {
        val detail = MonteCarloEngine.runDetail(config, seed = 3, runIndex = 0, history = history)
        assertTrue(detail.result.yearlyResults.flatMap { it.guardrails?.reviews.orEmpty() }.any { it.lowerGuardrailPortfolio != null })
        val batch = MonteCarloEngine.run(config, runs = 2, seed = 3, history = history)
        assertEquals(batch.paths[0], detail.path)
    }
}

