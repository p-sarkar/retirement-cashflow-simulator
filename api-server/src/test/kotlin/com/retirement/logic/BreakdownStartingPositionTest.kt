package com.retirement.logic

import com.retirement.defaultFormConfig
import com.retirement.model.*
import com.retirement.presetConfigs
import com.retirement.runTraced
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** T004-027: the Starting Position section (FR004-010–FR004-013). */
class BreakdownStartingPositionTest {

    private fun assertMoney(expected: Double, actual: Double, message: String) =
        assertTrue(abs(expected - actual) < 0.01, "$message: expected $expected, got $actual")

    @Test
    fun `opening balances are the prior year-end balances, or the configured ones in the first year`() {
        val config = defaultFormConfig()
        val (result, trace) = runTraced(config)
        result.yearlyResults.drop(1).forEachIndexed { i, row ->
            val opening = BreakdownGenerator.generate(config, result, trace, row.age).startingPosition.openingBalances
            val expected = if (i == 0) config.portfolio else result.yearlyResults[i].balances
            assertEquals(expected, opening, "age ${row.age}")
        }
    }

    @Test
    fun `the 2008 Crash preset's sequence is attributed year by year`() {
        val config = presetConfigs().single { it.marketPresetId == "crash-2008" }
        val (result, trace) = runTraced(config)
        val expected = listOf(-0.37, 0.265, 0.151, 0.021, 0.16)
        // Retirement at 55: the sequence covers ages 56–60
        (56..60).forEachIndexed { i, age ->
            val c = BreakdownGenerator.generate(config, result, trace, age).startingPosition.conditions
            assertEquals(EquityReturnSource.PRESET_SEQUENCE, c.equityReturnSource, "age $age")
            assertEquals("2008 Crash", c.presetName)
            assertEquals(i + 1, c.presetSequenceYear)
            assertMoney(expected[i], c.equityReturn, "age $age return")
        }
        listOf(55 to EquityReturnSource.PRE_RETIREMENT_RATE, 61 to EquityReturnSource.POST_RETIREMENT_RATE).forEach { (age, source) ->
            val c = BreakdownGenerator.generate(config, result, trace, age).startingPosition.conditions
            assertEquals(source, c.equityReturnSource, "age $age")
            assertNull(c.presetSequenceYear)
        }
        // After the −37% year the market starts the next year below its all-time high
        val after = BreakdownGenerator.generate(config, result, trace, 57).startingPosition.conditions
        assertTrue(after.marketVsAllTimeHigh < 0.7, "${after.marketVsAllTimeHigh}")
    }

    @Test
    fun `conditions and household follow the year`() {
        val config = defaultFormConfig()
        val (result, trace) = runTraced(config)
        fun at(age: Int) = BreakdownGenerator.generate(config, result, trace, age).startingPosition
        assertMoney(Math.pow(1.03, 5.0), at(56).conditions.cumulativeInflation, "inflation factor")
        assertEquals(HealthcareBracket.PRE_RETIREMENT, at(55).household.healthcareBracket)
        assertEquals(HealthcareBracket.POST_RETIREMENT_PRE_MEDICARE, at(56).household.healthcareBracket)
        assertEquals(HealthcareBracket.MEDICARE, at(65).household.healthcareBracket)
        // Lower earner (spouse 2 years younger) claims at 62; higher earner at 70
        fun claiming(age: Int, role: EarnerRole) = at(age).household.earners.single { it.role == role }.claiming
        assertTrue(!claiming(63, EarnerRole.LOWER) && claiming(64, EarnerRole.LOWER))
        assertTrue(!claiming(69, EarnerRole.HIGHER) && claiming(70, EarnerRole.HIGHER))
    }

    @Test
    fun `the start-year snapshot has only a starting position`() {
        val config = defaultFormConfig()
        val (result, trace) = runTraced(config)
        val b = BreakdownGenerator.generate(config, result, trace, config.currentAge)
        assertEquals(YearPhase.START, b.phase)
        assertEquals(config.portfolio, b.startingPosition.openingBalances)
        listOf(b.expenses, b.income, b.incomeGap, b.moneyMovement, b.taxes, b.nextYear).forEach { assertNull(it) }
    }
}
