package com.retirement.logic

import com.retirement.model.*
import com.retirement.testConfig
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * #21: in a downturn with the SB at its floor, the strategy draws on the CBB only while the CBB has held up
 * (at least 90% of its balance 12 months earlier) or has lost less than the equities; otherwise it sells
 * equities (FR002-005).
 */
class CbbDownturnTest {

    // Retired, with a small SB and a CBB near its cap, so a downturn soon draws the CBB down
    private val config = testConfig(
        currentAge = 66, retirementAge = 65,
        portfolio = Portfolio(sb = 20000.0, cbb = 150000.0, tba = 700000.0, tda = 800000.0, tfa = 0.0)
    )

    @Test
    fun `while the market stays flat below its high, a drawn-down CBB is spared and equities refill the SB`() {
        // A 30% crash in the first year, then four flat years: down vs the all-time high, level vs 12 months earlier
        val returns = listOf(0.0, -0.30, 0.0, 0.0, 0.0, 0.0)
        val trace = SimulationTrace()
        SimulationEngine.runSimulation(config, returns, emptyList(), logExpenses = false, trace = trace)
        val quarters = trace.years.filterKeys { it <= 2030 }.values.flatMap { it.quarterStrategy }

        assertTrue(quarters.any { it.rule == StrategyRule.MARKET_DOWN_FROM_CBB }, "the CBB is drawn on first")
        val fromEquities = quarters.filter { it.rule == StrategyRule.MARKET_DOWN_FROM_EQUITIES }
        assertTrue(fromEquities.isNotEmpty(), "then equities refill the SB: ${quarters.map { it.rule }}")
        fromEquities.forEach { q ->
            assertTrue(q.cbbVs12MonthsPrior!! < 0.90, "Q${q.quarter}: CBB at ${q.cbbVs12MonthsPrior} of 12 months earlier")
            assertTrue(q.cbbLoss!! >= q.equityLoss!!, "Q${q.quarter}: CBB loss ${q.cbbLoss} vs equities ${q.equityLoss}")
            assertTrue(q.refillToSb > 0.0, "Q${q.quarter}: refilled ${q.refillToSb}")
        }
    }

    @Test
    fun `the breakdown explains why equities, not the CBB, refilled the SB`() {
        val returns = listOf(0.0, -0.30, 0.0, 0.0, 0.0, 0.0)
        val trace = SimulationTrace()
        val result = SimulationEngine.runSimulation(config, returns, emptyList(), logExpenses = false, trace = trace)
        val year = trace.years.entries.first { (_, t) -> t.quarterStrategy.any { it.rule == StrategyRule.MARKET_DOWN_FROM_EQUITIES } }.key
        val breakdown = BreakdownGenerator.generate(config, result, trace, config.currentAge + (year - config.currentYear))
        val q = breakdown.moneyMovement!!.quarterStrategy.first { it.rule == StrategyRule.MARKET_DOWN_FROM_EQUITIES }

        assertTrue("refill the SB from TDA/TBA" in q.explanation, q.explanation)
        assertTrue("below 90%" in q.explanation && "lost at least as much as the equities" in q.explanation, q.explanation)
    }
}
