package com.retirement.logic

import com.retirement.defaultFormConfig
import com.retirement.model.*
import com.retirement.presetConfigs
import com.retirement.runTraced
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** T004-031: the Taxes section (FR004-060–FR004-062). */
class BreakdownTaxesTest {

    private fun assertMoney(expected: Double, actual: Double, message: String) =
        assertTrue(abs(expected - actual) < 0.01, "$message: expected $expected, got $actual")

    private fun TaxComputation.assertAddsUp(where: String) {
        components.forEach { assertMoney(it.amount * it.taxedShare, it.taxable, "$where ${it.name}") }
        assertMoney(components.sumOf { it.taxable }, taxableIncome, "$where taxable income")
        assertMoney(taxableIncome * rate, tax, "$where tax")
    }

    @Test
    fun `this year's bill comes from last year's income, and this year's income sets next year's bill`() {
        (listOf(defaultFormConfig()) + presetConfigs()).forEach { config ->
            val (result, trace) = runTraced(config)
            val rows = result.yearlyResults
            rows.drop(1).forEachIndexed { i, row ->
                val where = "${config.name} ${row.year}"
                val t = assertNotNull(BreakdownGenerator.generate(config, result, trace, row.age).taxes, where)
                t.billThisYear.assertAddsUp("$where bill")
                t.generatedThisYear.assertAddsUp("$where generated")
                assertMoney(row.cashFlow.incomeTax, t.billThisYear.tax, "$where bill = results-row Income Tax")
                rows.getOrNull(i + 2)?.let { next -> assertMoney(next.cashFlow.incomeTax, t.generatedThisYear.tax, "$where next year's bill") }
                assertEquals(if (i == 0) TaxBasis.START_YEAR_ESTIMATE else TaxBasis.PRIOR_YEAR, t.billThisYear.basis, where)
                assertTrue(t.simplifications.isNotEmpty())
            }
        }
    }

    @Test
    fun `the first year's bill uses the start-year income estimate`() {
        val config = defaultFormConfig()
        val (result, trace) = runTraced(config)
        val bill = BreakdownGenerator.generate(config, result, trace, config.currentAge + 1).taxes!!.billThisYear
        assertMoney(trace.startYearTaxableIncomeEstimate, bill.taxableIncome, "estimate")
        assertEquals(listOf("Salary minus 401k contributions", "Interest", "Dividends"), bill.components.map { it.name })
    }

    @Test
    fun `half of TBA withdrawals is taxed, 401k contributions aren't, and Roth conversions count once`() {
        val config = defaultFormConfig()
        val (result, trace) = runTraced(config)
        val row = result.yearlyResults.first { it.cashFlow.tbaWithdrawal > 0.0 && it.cashFlow.rothConversion > 0.0 }
        val generated = BreakdownGenerator.generate(config, result, trace, row.age).taxes!!.generatedThisYear
        val tba = generated.components.single { it.name == "TBA withdrawals" }
        assertEquals(0.5, tba.taxedShare)
        assertMoney(row.cashFlow.tbaWithdrawal, tba.amount, "TBA")
        assertMoney(row.cashFlow.rothConversion, generated.components.single { it.name == "Roth conversions" }.taxable, "Roth")
        assertEquals(
            listOf("Salary minus 401k contributions", "Interest", "Dividends", "Social Security",
                "TDA withdrawals for spending", "Roth conversions", "TBA withdrawals", "TFA withdrawals"),
            generated.components.map { it.name }
        )
        assertMoney(row.cashFlow.tdaWithdrawalSpend, generated.components.single { it.name == "TDA withdrawals for spending" }.amount, "TDA")
    }
}
