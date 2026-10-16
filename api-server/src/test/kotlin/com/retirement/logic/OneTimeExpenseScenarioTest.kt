package com.retirement.logic

import com.retirement.model.*
import kotlin.math.pow
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Acceptance scenarios from spec.md (US003-2–US003-4) and the Phase 7 edge cases, run against the engine
 * and the breakdown generator. These replace the manual browser checks for the computed numbers;
 * the browser checks remain for rendering only.
 */
class OneTimeExpenseScenarioTest {

    private val currentYear = 2025
    private val currentAge = 60 // simulation covers 2026 (age 61) through 2050 (age 85)

    private fun config(expenses: List<OneTimeExpense>, inflation: Double = 0.03) = SimulationConfig(
        id = "scenario-test",
        name = "One-Time Expense Scenarios",
        currentYear = currentYear,
        currentAge = currentAge,
        retirementAge = 65,
        salary = 120000.0,
        portfolio = Portfolio(sb = 200000.0, cbb = 1000000.0, tba = 500000.0, tda = 1000000.0, tfa = 0.0),
        spousal = SpousalDetails(58, SocialSecurityDetails(67, 20000.0), SocialSecurityDetails(70, 40000.0)),
        expenses = ExpenseConfig(60000.0, 30000.0, 10000.0, 5000.0, 5000.0, 3000.0),
        contributions = ContributionConfig(annual401k = 23000.0, annualTba = 12000.0),
        rates = RateConfig(inflation, 0.07, 0.05, 0.04, 0.04, 0.20),
        strategy = StrategyConfig(initialTdaWithdrawal = 40000.0, rothConversionAmount = 10000.0),
        oneTimeExpenses = expenses
    )

    private fun run(expenses: List<OneTimeExpense>, inflation: Double = 0.03): Pair<SimulationConfig, SimulationResult> {
        val c = config(expenses, inflation)
        return c to SimulationEngine.runSimulation(c, List(40) { 0.05 }, List(40) { inflation })
    }

    private fun factor(year: Int, inflation: Double = 0.03) = (1.0 + inflation).pow(year - currentYear)

    private fun SimulationResult.year(y: Int) = yearlyResults.first { it.year == y }
    private fun SimulationResult.oneTime(y: Int) = year(y).cashFlow.oneTimeExpenses
    private fun SimulationResult.quarters(y: Int) =
        quarterlyResults.filter { it.year == y }.sortedBy { it.quarter }.map { it.cashFlow.oneTimeExpenses }

    private fun breakdownSection(c: SimulationConfig, r: SimulationResult, year: Int): BreakdownSection? {
        val yr = r.year(year)
        val prior = r.yearlyResults.firstOrNull { it.year == year - 1 }
        return BreakdownGenerator.generateBreakdown(c, yr.age, yr, prior)
            .sections.firstOrNull { it.title == "One-Time Expenses" }
    }

    private fun cash(id: String, amount: Double, year: Int, name: String = id) =
        CashExpense(id = id, name = name, amount = amount, yearOrAge = YearOrAge.Year(year))

    private fun loan(
        principal: Double = 100000.0,
        apr: Double = 6.0,
        term: Int = 10,
        start: YearOrAge = YearOrAge.Age(65),
        down: Double = 0.0,
        name: String = "Home Equity Loan"
    ) = LoanExpense(
        id = "loan-$name", name = name, principal = principal, aprPercent = apr, termYears = term,
        startYearOrAge = start, downPayment = down
    )

    private fun loanQ(l: LoanExpense, inflation: Double = 0.03): Double {
        val start = l.startYearOrAge.toYear(currentAge, currentYear)
        return LoanExpense.calculateQuarterlyPayment(l.getFinancedAmount() * factor(start, inflation), l.aprPercent, l.termYears)
    }

    // --- US003-2: multiple expenses and breakdown (T003-040–T003-043) ---

    @Test
    fun `T003-040 three expenses in different years each land only in their own year`() {
        val expenses = listOf(cash("Car", 50000.0, 2027), cash("Roof", 30000.0, 2030), cash("Trip", 15000.0, 2035))
        val (_, r) = run(expenses)

        val expected = mapOf(2027 to 50000.0, 2030 to 30000.0, 2035 to 15000.0)
        r.yearlyResults.filter { it.year > currentYear }.forEach { yr ->
            val want = expected[yr.year]?.let { it * factor(yr.year) } ?: 0.0
            assertEquals(want, yr.cashFlow.oneTimeExpenses, 0.01, "one-time expenses in ${yr.year}")
        }
    }

    @Test
    fun `two expenses in the same year are summed`() {
        val (_, r) = run(listOf(cash("Car", 50000.0, 2028), cash("Roof", 30000.0, 2028)))
        assertEquals(80000.0 * factor(2028), r.oneTime(2028), 0.01)
        assertEquals(listOf("Car", "Roof"), r.year(2028).oneTimeExpensesBreakdown!!.map { it.name })
    }

    @Test
    fun `T003-041 T003-042 T003-043 breakdown lists each expense name, type and inflation adjusted amount`() {
        val (c, r) = run(listOf(cash("c1", 50000.0, 2028, "New Car"), cash("c2", 30000.0, 2028, "Roof")))

        val section = breakdownSection(c, r, 2028)!!
        assertEquals(
            listOf("New Car (Cash Expense)", "Roof (Cash Expense)", "Total One-Time Expenses"),
            section.steps.map { it.label }
        )
        assertEquals(50000.0 * factor(2028), section.steps[0].result, 0.01)
        assertEquals(30000.0 * factor(2028), section.steps[1].result, 0.01)
        assertEquals(80000.0 * factor(2028), section.steps[2].result, 0.01)
    }

    @Test
    fun `T003-104 breakdown labels loan payments as fixed and cash as inflation adjusted`() {
        val l = loan(principal = 40000.0, apr = 5.0, term = 5, start = YearOrAge.Year(2028), name = "Car Loan")
        val (c, r) = run(listOf(l, cash("c1", 20000.0, 2029, "Roof")))

        val loanStep = breakdownSection(c, r, 2029)!!.steps.first { it.label == "Car Loan (Loan Payment)" }
        assertEquals("Fixed quarterly payment × 4", loanStep.formula)
        assertEquals(loanQ(l), loanStep.values["quarterlyPayment"]!!, 0.01)
        assertTrue("not adjusted for inflation" in loanStep.explanation)

        val cashStep = breakdownSection(c, r, 2029)!!.steps.first { it.label == "Roof (Cash Expense)" }
        assertEquals("Inflation-adjusted amount", cashStep.formula)
    }

    @Test
    fun `T003-042 breakdown has no One-Time Expenses section in a year without expenses`() {
        val (c, r) = run(listOf(cash("c1", 50000.0, 2028, "New Car")))
        assertNull(breakdownSection(c, r, 2029))
        assertEquals(0.0, r.oneTime(2029), 1e-9)
    }

    // --- US003-3: loans (T003-056, T003-058–T003-060c) ---

    @Test
    fun `T003-056 loan starting at age 65 for 10 years pays from age 65 through 74`() {
        val l = loan()
        val (_, r) = run(listOf(l))

        r.yearlyResults.filter { it.year > currentYear }.forEach { yr ->
            val inTerm = yr.age in 65..74
            assertEquals(if (inTerm) loanQ(l) * 4 else 0.0, yr.cashFlow.oneTimeExpenses, 0.01, "age ${yr.age}")
        }
    }

    @Test
    fun `T003-060 every year of the term shows four equal quarterly payments`() {
        val l = loan()
        val (_, r) = run(listOf(l))
        (65..74).forEach { age ->
            val qs = r.quarters(currentYear + age - currentAge)
            assertEquals(4, qs.size)
            qs.forEach { assertEquals(loanQ(l), it, 0.01, "age $age") }
        }
    }

    @Test
    fun `T003-058 concurrent cash and loan expenses both appear in the breakdown`() {
        val l = loan(principal = 40000.0, apr = 5.0, term = 5, start = YearOrAge.Year(2028), name = "Car Loan")
        val (c, r) = run(listOf(l, cash("c1", 20000.0, 2028, "Roof")))

        val section = breakdownSection(c, r, 2028)!!
        assertEquals(
            listOf("Car Loan (Loan Payment)", "Roof (Cash Expense)", "Total One-Time Expenses"),
            section.steps.map { it.label }
        )
        assertEquals(loanQ(l) * 4, section.steps[0].result, 0.01)
        assertEquals(20000.0 * factor(2028), section.steps[1].result, 0.01)
        assertEquals(loanQ(l) * 4 + 20000.0 * factor(2028), r.oneTime(2028), 0.01)
    }

    @Test
    fun `T003-059 T003-081 loan extending past the last simulated year only pays within the simulation`() {
        // Starts at age 80 for 10 years; the simulation ends at age 85
        val l = loan(start = YearOrAge.Age(80))
        val (_, r) = run(listOf(l))

        val last = r.yearlyResults.last()
        assertEquals(85, last.age)
        (80..85).forEach { age ->
            assertEquals(loanQ(l) * 4, r.year(currentYear + age - currentAge).cashFlow.oneTimeExpenses, 0.01, "age $age")
        }
        assertTrue(r.yearlyResults.none { it.age > 85 })
        assertTrue(r.quarterlyResults.none { it.age > 85 })
    }

    @Test
    fun `T003-060a payment is amortized on the financed amount, not the principal`() {
        val l = loan(principal = 50000.0, down = 10000.0, apr = 5.0, term = 5, start = YearOrAge.Year(2028))
        val (_, r) = run(listOf(l))
        val expected = LoanExpense.calculateQuarterlyPayment(40000.0 * factor(2028), 5.0, 5)
        r.quarters(2029).forEach { assertEquals(expected, it, 0.01) }
    }

    @Test
    fun `T003-060b down payment is a separate entry in the start year breakdown only`() {
        val l = loan(principal = 50000.0, down = 10000.0, apr = 5.0, term = 5, start = YearOrAge.Year(2028), name = "Car")
        val (c, r) = run(listOf(l))

        val start = breakdownSection(c, r, 2028)!!.steps.map { it.label }
        assertEquals(listOf("Car (Down Payment) (Cash Expense)", "Car (Loan Payment)", "Total One-Time Expenses"), start)

        val next = breakdownSection(c, r, 2029)!!.steps.map { it.label }
        assertEquals(listOf("Car (Loan Payment)", "Total One-Time Expenses"), next)
    }

    @Test
    fun `T003-060c down payment equal to principal pays only the down payment`() {
        val l = loan(principal = 30000.0, down = 30000.0, apr = 5.0, term = 5, start = YearOrAge.Year(2028), name = "Car")
        val (_, r) = run(listOf(l))

        assertEquals(30000.0 * factor(2028), r.oneTime(2028), 0.01)
        assertEquals(listOf("Car (Down Payment)"), r.year(2028).oneTimeExpensesBreakdown!!.map { it.name })
        // No $0 "Loan Payment" rows for the rest of the term
        (2029..2032).forEach {
            assertEquals(0.0, r.oneTime(it), 1e-9, "year $it")
            assertNull(r.year(it).oneTimeExpensesBreakdown, "breakdown in $it")
        }
    }

    // --- US003-4: edit and remove (T003-066–T003-069) ---

    @Test
    fun `T003-066 changing a cash expense from 50k to 60k changes that year by the inflated difference`() {
        val (_, before) = run(listOf(cash("c1", 50000.0, 2028)))
        val (_, after) = run(listOf(cash("c1", 60000.0, 2028)))
        assertEquals(10000.0 * factor(2028), after.oneTime(2028) - before.oneTime(2028), 0.01)
    }

    @Test
    fun `T003-067 lowering the APR from 6 to 5 percent lowers the quarterly payment`() {
        val six = loan(apr = 6.0)
        val five = loan(apr = 5.0)
        val (_, r6) = run(listOf(six))
        val (_, r5) = run(listOf(five))

        val year = currentYear + 65 - currentAge
        assertEquals(loanQ(six), r6.quarters(year)[0], 0.01)
        assertEquals(loanQ(five), r5.quarters(year)[0], 0.01)
        assertTrue(r5.quarters(year)[0] < r6.quarters(year)[0])
    }

    @Test
    fun `T003-068 removing one of several expenses removes only its impact`() {
        val car = cash("Car", 50000.0, 2027)
        val roof = cash("Roof", 30000.0, 2030)
        val (_, r) = run(listOf(roof))

        assertEquals(0.0, r.oneTime(2027), 1e-9)
        assertEquals(30000.0 * factor(2030), r.oneTime(2030), 0.01)
        assertTrue(r.yearlyResults.none { yr -> yr.oneTimeExpensesBreakdown?.any { it.name == car.name } == true })
    }

    @Test
    fun `T003-069 removing all expenses leaves zero one-time expenses and matches a run without them`() {
        val (_, none) = run(emptyList())
        assertTrue(none.yearlyResults.all { it.cashFlow.oneTimeExpenses == 0.0 && it.oneTimeExpensesBreakdown == null })
        assertTrue(none.quarterlyResults.all { it.cashFlow.oneTimeExpenses == 0.0 })
    }

    // --- Phase 7 edge cases (T003-078, T003-082b) ---

    @Test
    fun `T003-078 a simulation with 12 expenses runs without performance degradation`() {
        val many = (0 until 12).map { i ->
            if (i % 2 == 0) cash("cash-$i", 10000.0 + i * 1000, 2027 + i)
            else loan(principal = 30000.0, apr = 5.0, term = 5, start = YearOrAge.Year(2027 + i), name = "Loan $i")
        }
        run(many); run(emptyList()) // warm up

        val baseline = measureTimeMillis { repeat(20) { run(emptyList()) } }
        val withExpenses = measureTimeMillis { repeat(20) { run(many) } }
        // SC003-003: generous bound so the check is stable on slow CI machines
        assertTrue(withExpenses < baseline * 5 + 500, "12 expenses took ${withExpenses}ms vs ${baseline}ms baseline (20 runs)")

        val (_, r) = run(many)
        assertEquals(12, r.yearlyResults.flatMap { it.oneTimeExpensesBreakdown.orEmpty() }.map { it.name }.toSet().size)
    }

    @Test
    fun `T003-079 breakdown for a year with many expenses is computed well within 1 second`() {
        val many = (0 until 12).map { i -> cash("cash-$i", 10000.0, 2030, "Expense $i") }
        run(many) // warm up

        // Same work as POST /api/simulate/breakdown: full simulation, then the breakdown for one year
        val elapsed = measureTimeMillis {
            val (c, r) = run(many)
            assertEquals(13, breakdownSection(c, r, 2030)!!.steps.size)
        }
        assertTrue(elapsed < 1000, "breakdown took ${elapsed}ms (SC003-005 budget is 1s including the browser)")
    }

    @Test
    fun `T003-082b negative inflation deflates cash amounts and the loan financed amount`() {
        val inflation = -0.02
        val c = cash("Roof", 50000.0, 2030)
        val l = loan(principal = 40000.0, apr = 5.0, term = 5, start = YearOrAge.Year(2028), name = "Car")
        val (_, r) = run(listOf(c, l), inflation)

        assertEquals(50000.0 * factor(2030, inflation), r.year(2030).oneTimeExpensesBreakdown!!.first { it.name == "Roof" }.amount, 0.01)
        assertTrue(r.year(2030).oneTimeExpensesBreakdown!!.first { it.name == "Roof" }.amount < 50000.0)
        r.quarters(2029).forEach { assertEquals(loanQ(l, inflation), it, 0.01) }
        // Fixed nominal payments: the same amount every year of the term, even with deflation
        (2028..2032).forEach { assertEquals(loanQ(l, inflation) * 4, r.year(it).oneTimeExpensesBreakdown!!.first { e -> e.name == "Car" }.amount, 0.01) }
        assertTrue(r.quarterlyResults.all { it.balances.sb >= -0.01 })
    }
}
