package com.retirement.logic

import com.retirement.defaultFormConfig
import com.retirement.model.*
import com.retirement.testConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** #27: each year's quarters add up to its yearly row (FR002-009), including withdrawals that cover a one-time expense. */
class QuarterlyTotalsTest {

    private fun run(config: SimulationConfig) =
        SimulationEngine.runSimulation(config, emptyList(), emptyList(), logExpenses = false)

    private fun assertQuartersAddUp(config: SimulationConfig) {
        val result = run(config)
        result.yearlyResults.drop(1).forEach { row ->
            val quarters = result.quarterlyResults.filter { it.year == row.year }.map { it.cashFlow }
            val where = "${config.name} ${row.year}"
            listOf<Pair<String, (CashFlow) -> Double>>(
                "TBA withdrawal" to { it.tbaWithdrawal },
                "TDA withdrawal" to { it.tdaWithdrawal },
                "TDA for spend" to { it.tdaWithdrawalSpend },
                "TDA for Roth" to { it.tdaWithdrawalRoth },
                "TFA withdrawal" to { it.tfaWithdrawal },
                "SB deposit" to { it.sbDeposit },
                "Total income" to { it.totalIncome }
            ).forEach { (name, value) ->
                assertEquals(value(row.cashFlow), quarters.sumOf(value), 0.01, "$where $name")
            }
        }
    }

    // The default plan with an empty Crash Buffer and a small Spend Bucket: its one-time expenses are
    // covered from equities (in 2030, age 55, a $21,487 expense)
    private val coveredFromEquities = defaultFormConfig().let {
        it.copy(name = "Empty CBB", portfolio = it.portfolio.copy(sb = 10000.0, cbb = 0.0))
    }

    @Test
    fun `a one-time expense covered from equities is in its quarter's withdrawals`() {
        val row = run(coveredFromEquities).yearlyResults.single { it.year == 2030 }
        assertTrue(row.cashFlow.tbaWithdrawal + row.cashFlow.tdaWithdrawalSpend > 0.0, "equities cover the 2030 expense")

        assertQuartersAddUp(coveredFromEquities)
    }

    @Test
    fun `the quarters add up to the year for the default and test plans`() {
        assertQuartersAddUp(defaultFormConfig())
        assertQuartersAddUp(testConfig())
    }
}
