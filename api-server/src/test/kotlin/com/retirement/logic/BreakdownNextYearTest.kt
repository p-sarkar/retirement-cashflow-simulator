package com.retirement.logic

import com.retirement.defaultFormConfig
import com.retirement.model.*
import com.retirement.presetConfigs
import com.retirement.runTraced
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** T004-035: the Next Year section (FR004-070–FR004-073). */
class BreakdownNextYearTest {

    private fun assertMoney(expected: Double, actual: Double, message: String) =
        assertTrue(abs(expected - actual) < 0.01, "$message: expected $expected, got $actual")

    @Test
    fun `exact projections equal next year's actual values, estimates are marked`() {
        (listOf(defaultFormConfig()) + presetConfigs()).forEach { config ->
            val (result, trace) = runTraced(config)
            val rows = result.yearlyResults
            rows.drop(1).dropLast(1).forEachIndexed { i, row ->
                val next = rows[i + 2]
                val where = "${config.name} ${row.year}"
                val n = assertNotNull(BreakdownGenerator.generate(config, result, trace, row.age).nextYear, where)
                assertEquals(next.year, n.year)
                val actual = mapOf(
                    "Needs" to next.cashFlow.needs, "Wants" to next.cashFlow.wants, "Healthcare" to next.cashFlow.healthcare,
                    "Property Tax" to next.cashFlow.propertyTax, "Income Tax" to next.cashFlow.incomeTax,
                    "One-time expenses" to next.cashFlow.oneTimeExpenses, "Salary" to next.cashFlow.salary,
                    "Social Security" to next.cashFlow.socialSecurity
                )
                (n.expenses + n.income).filter { it.exact }.forEach { item ->
                    assertMoney(actual.getValue(item.label), item.amount, "$where ${item.label}")
                }
                assertEquals(actual.keys, (n.expenses + n.income).filter { it.exact }.map { it.label }.toSet(), where)
                assertTrue((n.income).filter { !it.exact }.map { it.label }.containsAll(listOf("Interest (estimate)", "Dividends (estimate)")))
                assertTrue(n.incomeGap == null || !n.incomeGap!!.exact)
                // T004-052: every projection says how it was computed (FR004-003)
                (n.expenses + n.income + listOfNotNull(n.incomeGap)).forEach { assertTrue(it.rule.isNotBlank(), "$where ${it.label}") }
            }
        }
    }

    @Test
    fun `changes from this year are called out`() {
        val config = defaultFormConfig()
        val (result, trace) = runTraced(config)
        fun changes(age: Int) = BreakdownGenerator.generate(config, result, trace, age).nextYear!!.changes.joinToString(" | ")

        assertTrue("Retirement starts" in changes(55), changes(55))
        assertNull(BreakdownGenerator.generate(config, result, trace, 54).nextYear!!.incomeGap)
        assertNotNull(BreakdownGenerator.generate(config, result, trace, 55).nextYear!!.incomeGap)
        assertTrue("Lower earner starts Social Security" in changes(63), changes(63))
        assertTrue("Higher earner starts Social Security" in changes(69), changes(69))
        assertTrue("Medicare" in changes(64), changes(64))
        assertTrue("CBB cap steps down" in changes(64), changes(64))
        assertTrue("Loan payments for Partha's car 1 end" in changes(59), changes(59))
        assertTrue("Loan payments for New HVAC start" in changes(56), changes(56))
        assertTrue("Down payment for Mou's car 1" in changes(55), changes(55))
    }

    @Test
    fun `the last simulated year has no next year`() {
        val config = defaultFormConfig()
        val (result, trace) = runTraced(config)
        val last = BreakdownGenerator.generate(config, result, trace, config.endAge)
        assertTrue(last.isLastYear)
        assertNull(last.nextYear)
    }
}
