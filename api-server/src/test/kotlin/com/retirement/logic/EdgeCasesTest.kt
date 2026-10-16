package com.retirement.logic

import com.retirement.api.simulationRoutes
import com.retirement.model.*
import com.retirement.testConfig
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Spec 002 edge cases (T002-043). */
class EdgeCasesTest {

    private fun run(config: SimulationConfig) =
        SimulationEngine.runSimulation(config, MarketPresets.equityReturns(config), emptyList())

    private fun SimulationResult.allNumbers(): List<Double> = yearlyResults.flatMap { y ->
        listOf(y.balances.sb, y.balances.cbb, y.balances.tba, y.balances.tda, y.balances.tfa,
            y.cashFlow.totalIncome, y.cashFlow.totalExpenses, y.metrics.cbbCap) +
            listOfNotNull(y.metrics.annualIncomeGap, y.metrics.sbCap)
    }

    // --- T002-043.1 Zero initial balance in all accounts ---

    private val empty = Portfolio(0.0, 0.0, 0.0, 0.0, 0.0)

    @Test
    fun `zero balances - salary covers the working years, the plan fails once retired`() {
        // Salary covers needs, taxes and contributions, so the SB stays at 0 while working
        val result = run(testConfig(portfolio = empty).copy(salary = 150000.0))

        assertTrue(result.allNumbers().all { it.isFinite() })
        assertTrue(result.yearlyResults.filter { it.age <= 65 }.none { it.metrics.isFailure })
        assertFalse(result.summary.isSuccess)
        // Only the contributions made while working are left to fund retirement (from 2031, age 66)
        assertTrue(result.summary.failureYear!! > 2030)
    }

    @Test
    fun `zero balances with a salary that cannot cover needs fail while working`() {
        // $120k salary < needs, healthcare, property tax, income tax and contributions: Wants drop to 0
        // and the SB drains by the rest
        val result = run(testConfig(portfolio = empty))

        assertTrue(result.yearlyResults.drop(1).all { it.cashFlow.wants == 0.0 })
        // 2028 since #13: 401k contributions are no longer taxed, so the tax bill is lower and the SB lasts a year longer
        assertEquals(2028, result.summary.failureYear)
    }

    @Test
    fun `zero balances when already retired fail in the first simulated year`() {
        val result = run(testConfig(currentAge = 70, retirementAge = 65, portfolio = empty))

        assertTrue(result.allNumbers().all { it.isFinite() })
        assertEquals(2026, result.summary.failureYear)
        assertEquals(2, result.yearlyResults.size) // start year snapshot + the failed year
    }

    // --- T002-043.2 Market crash in year 1 of retirement ---

    private fun equitySpending(result: SimulationResult, year: Int, fromQuarter: Int = 0) =
        result.quarterlyResults.filter { it.year == year && it.quarter >= fromQuarter }
            .sumOf { it.cashFlow.tbaWithdrawal + it.cashFlow.tdaWithdrawalSpend }

    @Test
    fun `a crash in the first retirement year moves spending off equities until markets recover`() {
        // 2031 (age 66) is the first retirement year; the 2008 Crash preset replays -37% then +26.5%.
        // A small SB, so the strategy needs refills (a large one would sit above its cap and need none)
        val portfolio = Portfolio(sb = 20000.0, cbb = 300000.0, tba = 800000.0, tda = 1200000.0, tfa = 100000.0)
        val crash = run(testConfig(portfolio = portfolio, marketPresetId = "crash-2008"))
        val steady = run(testConfig(portfolio = portfolio, marketPresetId = "historical-average"))

        // In steady markets the strategy refills the SB from equities
        assertTrue(equitySpending(steady, 2031, fromQuarter = 2) > 0.0)
        assertTrue(equitySpending(steady, 2032) > 0.0)
        // After the crash passes the all-time-high threshold (from Q3), equities are not sold for spending,
        // nor through the first recovery year, which is still below 85% of the high
        assertEquals(0.0, equitySpending(crash, 2031, fromQuarter = 2))
        assertEquals(0.0, equitySpending(crash, 2032))
        // The SB is refilled from the Crash Buffer instead, and the plan survives
        val cbbDrawn = crash.yearlyResults.single { it.year == 2032 }.balances.cbb <
            crash.yearlyResults.single { it.year == 2031 }.balances.cbb
        assertTrue(cbbDrawn, "CBB funds spending in the down market")
        assertTrue(crash.summary.isSuccess)
    }

    // --- T002-043.3 Deflation (negative inflation rate) ---

    @Test
    fun `deflation lowers expenses each year and the simulation completes`() {
        val config = testConfig(rates = testConfig().rates.copy(inflation = -0.02))
        assertTrue(ConfigValidator.validate(config).isValid)

        val result = run(config)
        val needs = result.yearlyResults.drop(1).map { it.cashFlow.needs }

        assertEquals(config.expenses.needs * 0.98, needs.first(), 1e-6)
        assertTrue(needs.zipWithNext().all { (a, b) -> b < a })
        assertTrue(result.allNumbers().all { it.isFinite() })
        assertEquals(85, result.yearlyResults.last().age)
    }

    // --- T002-043.4 Missing or invalid input fields ---

    private fun fields(config: SimulationConfig) = ConfigValidator.validate(config).errors.map { it.field }

    @Test
    fun `cleared ages and year (sent as 0) are rejected`() {
        val base = testConfig()
        assertEquals(listOf("currentYear"), fields(base.copy(currentYear = 0)))
        assertEquals(listOf("currentAge"), fields(base.copy(currentAge = 0)))
        assertEquals(listOf("retirementAge"), fields(base.copy(retirementAge = 0)))
        assertEquals(listOf("spousal.spouseAge"), fields(base.copy(spousal = base.spousal.copy(spouseAge = 0))))
        assertEquals(
            listOf("spousal.lowerEarner.claimAge"),
            fields(base.copy(spousal = base.spousal.copy(lowerEarner = SocialSecurityDetails(0, 20000.0))))
        )
    }

    @Test
    fun `social security claim ages must be 62 to 70`() {
        val base = testConfig()
        fun withHigherClaimAge(age: Int) = base.copy(spousal = base.spousal.copy(higherEarner = SocialSecurityDetails(age, 40000.0)))
        assertEquals(emptyList(), fields(withHigherClaimAge(62)))
        assertEquals(emptyList(), fields(withHigherClaimAge(70)))
        assertEquals(listOf("spousal.higherEarner.claimAge"), fields(withHigherClaimAge(71)))
    }

    @Test
    fun `negative amounts are rejected with a message naming the field`() {
        val base = testConfig()
        val errors = ConfigValidator.validate(
            base.copy(salary = -1.0, portfolio = base.portfolio.copy(tda = -5.0), expenses = base.expenses.copy(needs = Double.NaN))
        ).errors
        assertEquals(listOf("salary", "portfolio.tda", "expenses.needs"), errors.map { it.field })
        assertEquals("Tax Deferred (TDA) balance cannot be negative", errors[1].message)
    }

    @Test
    fun `zero amounts are valid`() {
        assertTrue(ConfigValidator.validate(testConfig(portfolio = empty)).isValid)
    }

    @Test
    fun `a request missing a required field is a 400`() = testApplication {
        install(ContentNegotiation) { json() }
        routing { simulationRoutes() }
        val response = client.post("/api/simulate") {
            contentType(ContentType.Application.Json)
            setBody("""{"name": "No ages", "currentYear": 2025}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
