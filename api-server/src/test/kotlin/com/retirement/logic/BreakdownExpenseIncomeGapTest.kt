package com.retirement.logic

import com.retirement.defaultFormConfig
import com.retirement.model.*
import com.retirement.presetConfigs
import com.retirement.runTraced
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** T004-021: the Expenses, Income and Income Gap sections (FR004-020–FR004-042). */
class BreakdownExpenseIncomeGapTest {

    private fun assertMoney(expected: Double, actual: Double, message: String) =
        assertTrue(abs(expected - actual) < 0.01, "$message: expected $expected, got $actual")

    private fun breakdowns(): List<Triple<SimulationConfig, YearlyResult, YearBreakdown>> =
        (listOf(defaultFormConfig()) + presetConfigs()).flatMap { config ->
            val (result, trace) = runTraced(config)
            result.yearlyResults.drop(1).map { row -> Triple(config, row, BreakdownGenerator.generate(config, result, trace, row.age)) }
        }

    @Test
    fun `expenses match the results row, item by item`() {
        breakdowns().forEach { (config, row, b) ->
            val e = assertNotNull(b.expenses, "${config.name} ${row.year}")
            val byLabel = e.items.associate { it.label to it.amount }
            assertMoney(row.cashFlow.needs, byLabel.getValue("Needs"), "needs")
            assertMoney(row.cashFlow.wants, byLabel.getValue("Wants"), "wants")
            assertMoney(row.cashFlow.healthcare, byLabel.getValue("Healthcare"), "healthcare")
            assertMoney(row.cashFlow.propertyTax, byLabel.getValue("Property Tax"), "property tax")
            assertMoney(row.cashFlow.incomeTax, byLabel.getValue("Income Tax"), "income tax")
            assertMoney(row.cashFlow.oneTimeExpenses, e.oneTimeExpenses.sumOf { it.amount }, "one-time")
            assertMoney(row.cashFlow.totalExpenses, e.total, "${config.name} ${row.year} total")
        }
    }

    @Test
    fun `working-year Wants is salary minus each other expense and contribution`() {
        breakdowns().filter { it.third.phase == YearPhase.WORKING }.forEach { (config, row, b) ->
            val steps = b.expenses!!.wantsDerivation
            assertEquals("Salary", steps.first().label)
            assertEquals("Wants", steps.last().label)
            assertMoney(max(0.0, steps.dropLast(1).sumOf { it.amount }), steps.last().amount, "${config.name} ${row.year}")
            assertMoney(row.cashFlow.wants, steps.last().amount, "${config.name} ${row.year} wants")
        }
        breakdowns().filter { it.third.phase == YearPhase.RETIREMENT }.forEach { (_, _, b) ->
            assertTrue(b.expenses!!.wantsDerivation.isEmpty())
            assertNotNull(b.expenses!!.items.first { it.label == "Wants" }.todaysDollars)
        }
    }

    @Test
    fun `one-time expenses are listed by name and type, loan payments not inflation-adjusted`() {
        val (_, row, b) = breakdowns().first { (config, row, _) -> config.name == "Default form plan" && row.age == 55 }
        val items = b.expenses!!.oneTimeExpenses
        assertEquals(setOf("Partha's car 1"), items.map { it.name }.toSet())
        assertEquals(setOf(OneTimeExpenseKind.DOWN_PAYMENT, OneTimeExpenseKind.LOAN_PAYMENT), items.map { it.type }.toSet())
        assertTrue(items.single { it.type == OneTimeExpenseKind.LOAN_PAYMENT }.inflationAdjusted.not())
        assertTrue(items.single { it.type == OneTimeExpenseKind.DOWN_PAYMENT }.inflationAdjusted)
        assertMoney(row.cashFlow.oneTimeExpenses, items.sumOf { it.amount }, "total")
    }

    @Test
    fun `income separates earnings from withdrawals and adds up to Total Income`() {
        breakdowns().forEach { (config, row, b) ->
            val i = assertNotNull(b.income, "${config.name} ${row.year}")
            val where = "${config.name} ${row.year}"
            assertMoney(row.cashFlow.totalIncome, i.totalIncome, "$where total")
            assertMoney(i.totalIncome, i.incomeTotal + i.withdrawals.sumOf { it.amount }, "$where income + withdrawals")
            // Roth conversions move money between accounts: shown as a transfer, not income (#16)
            assertMoney(row.cashFlow.rothConversion, i.transfers.single { it.label == "Roth conversions" }.amount, "$where Roth")
            assertMoney(row.cashFlow.socialSecurity, i.socialSecurity.sumOf { it.amount }, "$where SS")
            if (b.phase == YearPhase.WORKING) {
                val s = assertNotNull(i.salary)
                assertMoney(row.cashFlow.salary, s.gross, "$where gross")
                assertMoney(s.gross - s.contribution401k - s.contributionRoth - s.contributionTba, s.takeHome, "$where take-home")
            } else {
                assertNull(i.salary)
            }
        }
    }

    @Test
    fun `the lower earner's spousal step-up is shown once the higher earner claims`() {
        // Own benefit $10k < half the higher earner's $40k, so the step-up applies from age 70
        val config = defaultFormConfig().let { it.copy(spousal = it.spousal.copy(lowerEarner = SocialSecurityDetails(62, 10000.0))) }
        val (result, trace) = runTraced(config)
        val at70 = BreakdownGenerator.generate(config, result, trace, 70).income!!.socialSecurity
        val lower = at70.single { it.role == EarnerRole.LOWER }
        assertTrue(lower.spousalStepUp)
        assertMoney(at70.single { it.role == EarnerRole.HIGHER }.amount / 2, lower.amount, "step-up")
        assertTrue(BreakdownGenerator.generate(config, result, trace, 69).income!!.socialSecurity.none { it.spousalStepUp })
    }

    @Test
    fun `retirement years show the estimated and actual income gap`() {
        breakdowns().filter { it.third.phase == YearPhase.RETIREMENT }.forEach { (config, row, b) ->
            val g = assertNotNull(b.incomeGap, "${config.name} ${row.year}")
            val where = "${config.name} ${row.year}"
            listOf(g.estimated!!, g.actual!!).forEach { assertMoney(it.expenses - it.passiveIncome, it.gap, where) }
            assertMoney(row.metrics.annualIncomeGap!!, g.actual!!.gap, "$where actual gap")
            assertMoney(g.actual!!.gap - g.estimated!!.gap, g.differences.sumOf { it.amount }, "$where differences")
            assertMoney(row.metrics.sbCap!!, g.sbCap!!.amount, "$where SB cap")
            assertMoney(row.metrics.cbbCap, g.cbbCap!!.amount, "$where CBB cap")
            assertTrue(g.thresholds.any { !it.evaluated }, "$where: the CBB-performance threshold isn't evaluated")
            assertEquals(config.strategy.tdaWithdrawalPercentage, g.thresholds.single { "Tax Deferred" in it.name }.value)
        }
    }

    @Test
    fun `working years explain that salary covers expenses`() {
        breakdowns().filter { it.third.phase == YearPhase.WORKING }.forEach { (config, row, b) ->
            val g = assertNotNull(b.incomeGap, "${config.name} ${row.year}")
            assertNull(g.estimated)
            assertNull(g.actual)
            assertMoney(b.income!!.salary!!.takeHome, g.takeHomeSalary!!.amount, "take-home")
            assertMoney(row.cashFlow.totalExpenses, g.workingExpenses!!.amount, "expenses")
            assertTrue(g.explanation.isNotBlank())
        }
    }
}
