package com.retirement.logic

import com.retirement.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Spec 002 FR-001a: End Age setting. */
class EndAgeTest {

    private val currentYear = 2025
    private val currentAge = 60

    private fun config(
        endAge: Int = 85,
        currentAge: Int = this.currentAge,
        expenses: List<OneTimeExpense> = emptyList()
    ) = SimulationConfig(
        name = "End Age Test",
        currentYear = currentYear,
        currentAge = currentAge,
        retirementAge = 65,
        endAge = endAge,
        salary = 120000.0,
        portfolio = Portfolio(sb = 200000.0, cbb = 1000000.0, tba = 500000.0, tda = 1000000.0, tfa = 0.0),
        spousal = SpousalDetails(58, SocialSecurityDetails(67, 20000.0), SocialSecurityDetails(70, 40000.0)),
        expenses = ExpenseConfig(60000.0, 30000.0, 10000.0, 5000.0, 5000.0, 3000.0),
        contributions = ContributionConfig(annual401k = 23000.0, annualTba = 12000.0),
        rates = RateConfig(0.03, 0.07, 0.05, 0.04, 0.04, 0.20),
        strategy = StrategyConfig(initialTdaWithdrawal = 40000.0, rothConversionAmount = 10000.0),
        oneTimeExpenses = expenses
    )

    private fun run(c: SimulationConfig) = SimulationEngine.runSimulation(c, emptyList(), emptyList())

    private fun errorFields(c: SimulationConfig) = ConfigValidator.validate(c).errors.map { it.field }

    // --- Default ---

    @Test
    fun `end age defaults to 85 when omitted from the request`() {
        val json = Json.encodeToString(SimulationConfig.serializer(), config())
        val withoutEndAge = JsonObject(Json.parseToJsonElement(json).jsonObject - "endAge")

        val decoded = Json.decodeFromString(SimulationConfig.serializer(), withoutEndAge.toString())

        assertEquals(85, decoded.endAge)
        assertEquals(85, run(decoded).yearlyResults.last().age)
    }

    // --- Simulation period ---

    @Test
    fun `results run through the end age`() {
        listOf(75, 85, 95, 110).forEach { endAge ->
            val result = run(config(endAge = endAge))
            assertEquals(endAge, result.yearlyResults.last().age, "last yearly row for end age $endAge")
            assertEquals(currentYear + (endAge - currentAge), result.yearlyResults.last().year)
            assertEquals(endAge, result.quarterlyResults.maxOf { it.age }, "last quarterly row for end age $endAge")
            // Starting row plus one row per simulated year
            assertEquals(endAge - currentAge + 1, result.yearlyResults.size)
        }
    }

    @Test
    fun `failure check stops at the end age`() {
        val underFunded = Portfolio(sb = 50000.0, cbb = 100000.0, tba = 100000.0, tda = 200000.0, tfa = 0.0)
        fun runUnderFunded(endAge: Int) = run(config(endAge = endAge).copy(portfolio = underFunded))

        // Find where the plan fails when simulated to the limit, then end the simulation just before it
        val failureYear = assertNotNull(runUnderFunded(SimulationPeriod.MAX_END_AGE).summary.failureYear,
            "precondition: plan must fail by age ${SimulationPeriod.MAX_END_AGE}")
        val failureAge = currentAge + (failureYear - currentYear)
        assertTrue(failureAge - 1 > currentAge, "precondition: plan must survive at least one year")

        assertTrue(runUnderFunded(failureAge - 1).summary.isSuccess, "ends at ${failureAge - 1}, before the failure")
        val endsAt = runUnderFunded(failureAge)
        assertFalse(endsAt.summary.isSuccess, "ends at $failureAge, the failure age")
        assertEquals(failureYear, endsAt.summary.failureYear)
    }

    @Test
    fun `CBB cap milestones stay fixed and stop reducing after 85`() {
        val result = run(config(endAge = 95))
        fun cbbCap(age: Int) = result.yearlyResults.first { it.age == age }.metrics.cbbCap

        assertTrue(cbbCap(84) > cbbCap(85), "reduction at 85")
        (86..95).forEach { assertEquals(cbbCap(85), cbbCap(it), 0.01, "no reduction at $it") }
        assertEquals(run(config()).yearlyResults.first { it.age == 70 }.metrics.cbbCap, cbbCap(70), 0.01)
    }

    // --- Validation ---

    @Test
    fun `end age must be greater than current age and at most 110`() {
        assertEquals(listOf("endAge"), errorFields(config(endAge = currentAge)))
        assertEquals(listOf("endAge"), errorFields(config(endAge = 40)))
        assertEquals(listOf("endAge"), errorFields(config(endAge = 111)))
        assertEquals(emptyList(), errorFields(config(endAge = currentAge + 1)))
        assertEquals(emptyList(), errorFields(config(endAge = 110)))
    }

    @Test
    fun `a current age of 85 or more is valid when the end age is later`() {
        assertEquals(emptyList(), errorFields(config(currentAge = 88, endAge = 95)))
        assertEquals(95, run(config(currentAge = 88, endAge = 95)).yearlyResults.last().age)
    }

    @Test
    fun `end age error names the valid range`() {
        val error = ConfigValidator.validate(config(endAge = 60)).errors.single()
        assertEquals("End age must be greater than current age (60) and no more than 110", error.message)
    }

    // --- One-time expenses (spec 003 FR-041) ---

    @Test
    fun `one-time expense window follows the end age`() {
        val atNinety = CashExpense(id = "c", name = "Trip", amount = 10000.0, yearOrAge = YearOrAge.Age(90))

        assertEquals(listOf("oneTimeExpenses[0].yearOrAge"), errorFields(config(expenses = listOf(atNinety))))
        assertEquals(emptyList(), errorFields(config(endAge = 95, expenses = listOf(atNinety))))

        val paid = run(config(endAge = 95, expenses = listOf(atNinety)))
        assertTrue(paid.yearlyResults.first { it.age == 90 }.cashFlow.oneTimeExpenses > 10000.0)
    }

    @Test
    fun `expense timing error names the end age`() {
        val atNinetySix = CashExpense(id = "c", name = "Trip", amount = 10000.0, yearOrAge = YearOrAge.Age(96))
        val error = ConfigValidator.validate(config(endAge = 95, expenses = listOf(atNinetySix))).errors.single()
        assertEquals("Expense must fall within the simulation: 2026 (age 61) to 2060 (age 95)", error.message)
    }
}
