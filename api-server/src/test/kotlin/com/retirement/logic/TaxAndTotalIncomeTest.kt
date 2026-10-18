package com.retirement.logic

import com.retirement.defaultFormConfig
import com.retirement.model.*
import com.retirement.presetConfigs
import com.retirement.testConfig
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/** Issues #13, #15 and #16: what counts as taxable income and as Total Income. */
class TaxAndTotalIncomeTest {

    private fun assertMoney(expected: Double, actual: Double, message: String) =
        assertTrue(abs(expected - actual) < 0.01, "$message: expected $expected, got $actual")

    private fun run(config: SimulationConfig) =
        SimulationEngine.runSimulation(config, MarketPresets.equityReturns(config), emptyList())

    /** Taxable income a year generates: 401k is pre-tax, each Roth conversion counts once, half of TBA withdrawals */
    private fun taxable(cf: CashFlow) = (cf.salary - cf.contribution401k) + cf.interest + cf.dividends +
        cf.socialSecurity + cf.tdaWithdrawalSpend + cf.rothConversion + 0.5 * cf.tbaWithdrawal

    private fun assertTaxBills(config: SimulationConfig) {
        val rows = run(config).yearlyResults
        rows.drop(1).zipWithNext().forEach { (year, next) ->
            assertMoney(taxable(year.cashFlow) * config.rates.incomeTax, next.cashFlow.incomeTax, "${config.name} ${next.year} tax bill")
        }
    }

    @Test
    fun `401k contributions are not taxed (#13)`() {
        // $23k a year of 401k contributions while working, no Roth conversions
        val config = testConfig().copy(strategy = StrategyConfig(initialTdaWithdrawal = 0.0))
        val rows = run(config).yearlyResults
        val working = rows[1]
        assertTrue(working.cashFlow.contribution401k > 0.0)
        assertMoney(taxable(working.cashFlow) * config.rates.incomeTax, rows[2].cashFlow.incomeTax, "second year's bill")
        assertTaxBills(config)
    }

    @Test
    fun `a Roth conversion is taxed once (#15)`() {
        val config = defaultFormConfig() // $10k/$20k conversions a year, no 401k contributions
        val rows = run(config).yearlyResults
        val year = rows.first { it.cashFlow.rothConversion > 0.0 }
        val next = rows[rows.indexOf(year) + 1]
        assertMoney(taxable(year.cashFlow) * config.rates.incomeTax, next.cashFlow.incomeTax, "bill after a conversion")
        assertTaxBills(config)
    }

    @Test
    fun `tax bills follow the rule under every preset`() {
        (presetConfigs() + testConfig()).forEach(::assertTaxBills)
    }

    @Test
    fun `Total Income excludes Roth conversions, which only move money between accounts (#16)`() {
        val configs = listOf(defaultFormConfig(), testConfig()) + presetConfigs()
        assertTrue(configs.any { config -> run(config).yearlyResults.any { it.cashFlow.rothConversion > 0.0 } })
        configs.forEach { config ->
            val result = run(config)
            (result.yearlyResults.drop(1) + result.quarterlyResults.drop(1).map { it.toYearly() }).forEach { row ->
                val cf = row.cashFlow
                assertMoney(
                    cf.salary + cf.interest + cf.dividends + cf.socialSecurity + cf.tbaWithdrawal + cf.tdaWithdrawalSpend +
                        cf.tfaWithdrawal, // the TFA as a last resort (#23)
                    cf.totalIncome, "${config.name} ${row.year} total income"
                )
            }
        }
    }

    private fun QuarterlyResult.toYearly() = YearlyResult(year, age, balances, cashFlow, metrics)
}
