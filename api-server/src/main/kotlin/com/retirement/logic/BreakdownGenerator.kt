package com.retirement.logic

import com.retirement.model.*

/**
 * Builds the computation breakdown for one year (spec 004) from a traced simulation run. It explains what the
 * engine did and never changes a result (FR004-006).
 */
object BreakdownGenerator {

    fun generate(config: SimulationConfig, result: SimulationResult, trace: SimulationTrace, targetAge: Int): YearBreakdown {
        val rows = result.yearlyResults
        val index = rows.indexOfFirst { it.age == targetAge }
        require(index >= 0) { "Age $targetAge is not in the simulation" }
        val row = rows[index]
        val prior = rows.getOrNull(index - 1)
        val yearTrace = trace.years[row.year]
        val phase = when {
            yearTrace == null -> YearPhase.START
            yearTrace.plan.isWorkingYear -> YearPhase.WORKING
            else -> YearPhase.RETIREMENT
        }

        return YearBreakdown(
            year = row.year,
            age = row.age,
            spouseAge = spouseAge(config, row.age),
            phase = phase,
            isLastYear = index == rows.lastIndex,
            startingPosition = startingPosition(config, row, prior, yearTrace),
            expenses = yearTrace?.let { expenses(config, row, it.plan, it.conditions) },
            income = yearTrace?.let { income(config, row, it.plan) },
            incomeGap = yearTrace?.let { incomeGap(config, row, it.plan) },
            moneyMovement = yearTrace?.let { moneyMovement(prior?.balances ?: config.portfolio, it) },
            taxes = yearTrace?.let { taxes(config, row, prior.takeIf { index > 1 }) },
            nextYear = yearTrace?.let { thisYear ->
                val next = rows.getOrNull(index + 1) ?: return@let null
                nextYear(row, thisYear.plan, next, trace.years.getValue(next.year).plan)
            },
            apiMetadata = com.retirement.util.ServerInfo.getMetadata()
        )
    }

    private fun spouseAge(config: SimulationConfig, age: Int) = config.spousal.spouseAge + (age - config.currentAge)

    // --- Section 1: Starting Position (FR004-010–FR004-013) ---

    private fun startingPosition(
        config: SimulationConfig,
        row: YearlyResult,
        prior: YearlyResult?,
        yearTrace: YearTrace?
    ): StartingPosition {
        // The start-year snapshot and the first simulated year both open with the configured balances
        val opening = prior?.balances ?: config.portfolio
        val isWorkingYear = yearTrace?.plan?.isWorkingYear ?: (row.age <= config.retirementAge)
        val conditions = yearTrace?.conditions
        val equityReturn = conditions?.equityReturn
            ?: if (isWorkingYear) config.rates.preRetirementGrowth else config.rates.postRetirementGrowth

        // A preset's return sequence starts in the first retirement year (MarketPresets.equityReturns)
        val preset = config.marketPresetId?.let(MarketPresets::find)
        val sequenceYear = preset?.retirementReturnSequence?.let { sequence ->
            val firstRetirementAge = maxOf(config.retirementAge, config.currentAge) + 1
            (row.age - firstRetirementAge).takeIf { yearTrace != null && !isWorkingYear && it in sequence.indices }?.plus(1)
        }

        return StartingPosition(
            openingBalances = opening,
            openingTotal = opening.sb + opening.cbb + opening.tba + opening.tda + opening.tfa,
            conditions = EconomicConditions(
                inflationRate = config.rates.inflation,
                cumulativeInflation = conditions?.cumulativeInflation ?: 1.0,
                equityReturn = equityReturn,
                equityReturnSource = when {
                    sequenceYear != null -> EquityReturnSource.PRESET_SEQUENCE
                    isWorkingYear -> EquityReturnSource.PRE_RETIREMENT_RATE
                    else -> EquityReturnSource.POST_RETIREMENT_RATE
                },
                presetName = preset?.name,
                presetSequenceYear = sequenceYear,
                bondYield = conditions?.bondYield ?: config.rates.bondYield,
                hysaRate = config.rates.hysaRate,
                incomeTaxRate = config.rates.incomeTax,
                marketVsAllTimeHigh = conditions?.marketVsAllTimeHigh ?: 1.0
            ),
            household = HouseholdStatus(
                healthcareBracket = yearTrace?.plan?.healthcareBracket ?: when {
                    isWorkingYear -> HealthcareBracket.PRE_RETIREMENT
                    row.age < 65 -> HealthcareBracket.POST_RETIREMENT_PRE_MEDICARE
                    else -> HealthcareBracket.MEDICARE
                },
                earners = listOf(
                    EarnerStatus(EarnerRole.HIGHER, row.age, config.spousal.higherEarner.claimAge,
                        claiming = row.age >= config.spousal.higherEarner.claimAge),
                    spouseAge(config, row.age).let { spouseAge ->
                        EarnerStatus(EarnerRole.LOWER, spouseAge, config.spousal.lowerEarner.claimAge,
                            claiming = spouseAge >= config.spousal.lowerEarner.claimAge)
                    }
                )
            )
        )
    }

    // --- Section 2: Expenses (FR004-020–FR004-022) ---

    private fun expenses(config: SimulationConfig, row: YearlyResult, plan: YearPlan, conditions: TraceConditions): ExpenseSection {
        val inflation = conditions.cumulativeInflation
        fun inflated(label: String, amount: Double, configured: Double) = LineItem(
            label, amount, "Configured amount × cumulative inflation",
            mapOf("configured" to configured, "inflationFactor" to inflation), todaysDollars = configured
        )
        val healthcareConfigured = when (plan.healthcareBracket) {
            HealthcareBracket.PRE_RETIREMENT -> config.expenses.healthcarePreRetirement
            HealthcareBracket.POST_RETIREMENT_PRE_MEDICARE -> config.expenses.healthcarePostRetirementPreMedicare
            HealthcareBracket.MEDICARE -> config.expenses.healthcareMedicare
        }
        val wants = if (plan.isWorkingYear) {
            LineItem("Wants", row.cashFlow.wants, "Working year: what take-home salary leaves after the other expenses (see below)")
        } else {
            inflated("Wants", row.cashFlow.wants, config.expenses.wants)
        }
        val wantsDerivation = if (!plan.isWorkingYear) emptyList() else listOf(
            LineItem("Salary", plan.salary, "Configured salary × cumulative inflation"),
            LineItem("Needs", -plan.needs, "Subtracted"),
            LineItem("Healthcare", -plan.healthcare, "Subtracted"),
            LineItem("Property Tax", -plan.propertyTax, "Subtracted"),
            LineItem("Income Tax", -plan.incomeTax, "Subtracted"),
            LineItem("401k contribution", -plan.contribution401k, "Subtracted"),
            LineItem("Roth contribution", -plan.contributionRoth, "Subtracted"),
            LineItem("Taxable contribution", -plan.contributionTba, "Subtracted"),
            LineItem("Wants", row.cashFlow.wants, "What's left, never below zero; Roth conversions aren't deducted (they move TDA → TFA)")
        )
        val oneTime = row.oneTimeExpensesBreakdown.orEmpty().map { detail ->
            when {
                detail.type == ExpenseType.LOAN_PAYMENT -> OneTimeExpenseItem(detail.name, OneTimeExpenseKind.LOAN_PAYMENT, detail.amount, false)
                detail.name.endsWith(DOWN_PAYMENT_SUFFIX) ->
                    OneTimeExpenseItem(detail.name.removeSuffix(DOWN_PAYMENT_SUFFIX), OneTimeExpenseKind.DOWN_PAYMENT, detail.amount, true)
                else -> OneTimeExpenseItem(detail.name, OneTimeExpenseKind.CASH, detail.amount, true)
            }
        }
        return ExpenseSection(
            items = listOf(
                inflated("Needs", row.cashFlow.needs, config.expenses.needs),
                wants,
                inflated("Healthcare", row.cashFlow.healthcare, healthcareConfigured),
                inflated("Property Tax", row.cashFlow.propertyTax, config.expenses.propertyTax),
                LineItem("Income Tax", row.cashFlow.incomeTax, "Last year's taxable income × the effective tax rate (see Taxes)")
            ),
            wantsDerivation = wantsDerivation,
            oneTimeExpenses = oneTime,
            total = row.cashFlow.totalExpenses
        )
    }

    private const val DOWN_PAYMENT_SUFFIX = " (Down Payment)"

    // --- Section 3: Income (FR004-030–FR004-032) ---

    private fun income(config: SimulationConfig, row: YearlyResult, plan: YearPlan): IncomeSection {
        val cf = row.cashFlow
        val lastYear = SimulationPeriod.lastYear(config)
        fun startYear(age: Int, claimAge: Int) = (config.currentYear + (claimAge - age)).takeIf { it <= lastYear }
        val salary = if (!plan.isWorkingYear) null else SalaryBreakdown(
            gross = cf.salary,
            contribution401k = cf.contribution401k,
            contributionRoth = cf.contributionRoth,
            contributionTba = cf.contributionTba,
            takeHome = cf.salary - cf.contribution401k - cf.contributionRoth - cf.contributionTba
        )
        return IncomeSection(
            salary = salary,
            interest = LineItem("Interest", cf.interest, "Spend Bucket balance × HYSA rate, credited at the start of each quarter"),
            dividends = LineItem("Dividends", cf.dividends, "Crash Buffer balance × bond yield, credited at the start of each quarter"),
            socialSecurity = listOf(
                EarnerBenefit(EarnerRole.HIGHER, plan.higherEarnerBenefit, false,
                    startYear(config.currentAge, config.spousal.higherEarner.claimAge)),
                EarnerBenefit(EarnerRole.LOWER, plan.lowerEarnerBenefit, plan.spousalStepUp,
                    startYear(config.spousal.spouseAge, config.spousal.lowerEarner.claimAge))
            ),
            incomeTotal = cf.salary + cf.interest + cf.dividends + cf.socialSecurity,
            withdrawals = listOf(
                LineItem("TBA withdrawals", cf.tbaWithdrawal, "Stock sales from the Taxable account: spending-strategy refills and shortfall cover"),
                LineItem("TDA withdrawals for spending", cf.tdaWithdrawalSpend, "Tax Deferred sales: spending-strategy refills and shortfall cover"),
                LineItem("TDA withdrawals for Roth conversion", cf.tdaWithdrawalRoth, "Taken from Tax Deferred and moved to Tax Free"),
                LineItem("Roth conversions", cf.rothConversion,
                    "The same money as the TDA withdrawal for Roth conversion, arriving in Tax Free; the results table's Total Income counts it under both (issue #16)")
            ),
            totalIncome = cf.totalIncome
        )
    }

    // --- Section 4: Income Gap (FR004-040–FR004-042) ---

    private fun incomeGap(config: SimulationConfig, row: YearlyResult, plan: YearPlan): IncomeGapSection {
        val cf = row.cashFlow
        if (plan.isWorkingYear) {
            val takeHome = cf.salary - cf.contribution401k - cf.contributionRoth - cf.contributionTba
            return IncomeGapSection(
                takeHomeSalary = LineItem("Take-home salary", takeHome, "Gross salary minus 401k, Roth and taxable contributions"),
                workingExpenses = LineItem("Total expenses", cf.totalExpenses, "Regular expenses (Wants absorb what's left of take-home salary) plus one-time expenses"),
                explanation = "Working year: salary covers expenses, so there is no income gap. Wants absorb what's left of " +
                    "take-home salary, so interest and dividends are saved; one-time expenses come out of the Spend Bucket."
            )
        }
        val ss = plan.lowerEarnerBenefit + plan.higherEarnerBenefit
        val estimatedExpenses = plan.needs + plan.wants + plan.healthcare + plan.propertyTax + plan.incomeTax
        val estimatedPassive = plan.estimatedInterest + plan.estimatedDividends + ss
        val ages = listOf(65, 70, 75, 80, 85)
        return IncomeGapSection(
            estimated = GapComputation(estimatedExpenses, estimatedPassive, plan.estimatedAig),
            actual = GapComputation(row.metrics.incomeGapExpenses, row.metrics.incomeGapPassiveIncome, row.metrics.annualIncomeGap ?: 0.0),
            differences = listOf(
                LineItem("One-time expenses", cf.oneTimeExpenses, "Not in the estimate; paid as they fall due"),
                LineItem("Interest above (−) or below (+) the estimate", -(cf.interest - plan.estimatedInterest),
                    "The estimate assumes the Spend Bucket holds half a year of expenses",
                    mapOf("estimated" to plan.estimatedInterest, "actual" to cf.interest)),
                LineItem("Dividends above (−) or below (+) the estimate", -(cf.dividends - plan.estimatedDividends),
                    "The estimate uses the Crash Buffer balance at the start of the year",
                    mapOf("estimated" to plan.estimatedDividends, "actual" to cf.dividends))
            ),
            capAig = LineItem("Cap AIG", plan.capAig, "The estimated gap with half of Wants; used for the caps"),
            sbFloor = LineItem("SB floor", plan.sbFloor, "The larger of 6 months of the estimated gap and a quarter of regular expenses"),
            sbCap = LineItem("SB cap", row.metrics.sbCap ?: plan.sbCap,
                "max(2 × Cap AIG, SB floor), from the year's actual figures as in the results table",
                mapOf("capUsedByStrategy" to plan.sbCap)),
            cbbCap = LineItem("CBB cap", row.metrics.cbbCap, "7 × the base Cap AIG, less one base Cap AIG at each of ages 65, 70, 75, 80, 85"),
            cbbCapReductions = ages.filter { row.age >= it },
            thresholds = listOf(
                StrategyThreshold("Market vs 12 months earlier, at least", 0.95, evaluated = true),
                StrategyThreshold("Market vs its all-time high, at least", 0.85, evaluated = true),
                StrategyThreshold("Crash Buffer vs 12 months earlier, at least (not modelled yet: always passes)", 0.90, evaluated = false),
                StrategyThreshold("Share of each equity withdrawal taken from Tax Deferred (the rest from Taxable)",
                    config.strategy.tdaWithdrawalPercentage, evaluated = true)
            ),
            explanation = "Expenses minus passive income (interest, dividends, Social Security). The spending strategy " +
                "plans each quarter's refill with the estimate made at the start of the year; the actual gap is known at year end."
        )
    }

    // --- Section 6: Taxes (FR004-060–FR004-062) ---

    private fun taxes(config: SimulationConfig, row: YearlyResult, priorSimulated: YearlyResult?): TaxSection {
        val rate = config.rates.incomeTax
        fun computation(basis: TaxBasis, components: List<TaxComponent>): TaxComputation {
            val taxable = components.sumOf { it.taxable }
            return TaxComputation(basis, components, taxable, rate, taxable * rate)
        }
        fun component(name: String, amount: Double, share: Double = 1.0) = TaxComponent(name, amount, share, amount * share)
        // The engine's taxable income for a simulated year (SimulationEngine, "Taxable Income for Next Year's Tax Bill")
        fun yearComponents(cf: CashFlow) = listOf(
            component("Salary", cf.salary),
            component("Interest", cf.interest),
            component("Dividends", cf.dividends),
            component("Social Security", cf.socialSecurity),
            component("TDA withdrawals (including for Roth conversion)", cf.tdaWithdrawal),
            component("Roth conversions", cf.rothConversion),
            component("TBA withdrawals", cf.tbaWithdrawal, 0.5)
        )
        val bill = if (priorSimulated != null) {
            computation(TaxBasis.PRIOR_YEAR, yearComponents(priorSimulated.cashFlow))
        } else {
            // First simulated year: the engine estimates the start year's income from the configuration
            computation(TaxBasis.START_YEAR_ESTIMATE, listOf(
                component("Salary minus 401k contributions", config.salary - config.contributions.annual401k),
                component("Interest", config.portfolio.sb * config.rates.hysaRate),
                component("Dividends", config.portfolio.cbb * config.rates.bondYield)
            )) // = trace.startYearTaxableIncomeEstimate (BreakdownTaxesTest)
        }
        return TaxSection(
            billThisYear = bill,
            generatedThisYear = computation(TaxBasis.THIS_YEAR, yearComponents(row.cashFlow)),
            simplifications = listOf(
                "A single effective tax rate applies to all taxable income.",
                "Social Security is fully taxable.",
                "Half of each TBA withdrawal is taxable (a 50% cost basis is assumed).",
                "Tax is paid the year after the income is earned.",
                "Salary counts in full: 401k contributions are deducted only in the first year's estimate (issue #13).",
                "Each Roth conversion counts twice: within TDA withdrawals and again as a Roth conversion (issue #15)."
            )
        )
    }

    // --- Section 7: Next Year (FR004-070–FR004-073) ---

    /**
     * Next year's plan is fixed before any of its market returns, so it is what is known at the end of this year
     * (research R004-4). Interest, dividends and the gap depend on balances and markets: marked as estimates.
     */
    private fun nextYear(row: YearlyResult, plan: YearPlan, next: YearlyResult, nextPlan: YearPlan): NextYearSection {
        val cf = row.cashFlow
        fun item(label: String, amount: Double, thisYear: Double, exact: Boolean = true) =
            ProjectedItem(label, amount, exact, amount - thisYear)
        val nextSs = nextPlan.lowerEarnerBenefit + nextPlan.higherEarnerBenefit

        val changes = mutableListOf<String>()
        if (plan.isWorkingYear && !nextPlan.isWorkingYear) {
            changes += "Retirement starts: salary and contributions end, and the spending strategy begins"
        }
        if (plan.healthcareBracket != nextPlan.healthcareBracket) {
            changes += when (nextPlan.healthcareBracket) {
                HealthcareBracket.MEDICARE -> "Medicare starts: healthcare moves to the Medicare rate"
                HealthcareBracket.POST_RETIREMENT_PRE_MEDICARE -> "Healthcare moves to the post-retirement, pre-Medicare rate"
                HealthcareBracket.PRE_RETIREMENT -> "Healthcare moves to the pre-retirement rate"
            }
        }
        if (plan.higherEarnerBenefit == 0.0 && nextPlan.higherEarnerBenefit > 0.0) changes += "Higher earner starts Social Security"
        if (plan.lowerEarnerBenefit == 0.0 && nextPlan.lowerEarnerBenefit > 0.0) changes += "Lower earner starts Social Security"
        if (!plan.spousalStepUp && nextPlan.spousalStepUp) changes += "The lower earner's spousal step-up starts"
        if (nextPlan.cbbCap < plan.cbbCap) changes += "CBB cap steps down at age ${next.age}"
        val loansNow = row.oneTimeExpensesBreakdown.orEmpty().filter { it.type == ExpenseType.LOAN_PAYMENT }.map { it.name }.toSet()
        val loansNext = next.oneTimeExpensesBreakdown.orEmpty().filter { it.type == ExpenseType.LOAN_PAYMENT }.map { it.name }.toSet()
        (loansNext - loansNow).forEach { changes += "Loan payments for $it start" }
        (loansNow - loansNext).forEach { changes += "Loan payments for $it end" }
        next.oneTimeExpensesBreakdown.orEmpty().filter { it.type == ExpenseType.CASH }.forEach {
            changes += if (it.name.endsWith(DOWN_PAYMENT_SUFFIX)) "Down payment for ${it.name.removeSuffix(DOWN_PAYMENT_SUFFIX)}"
                else "One-time expense: ${it.name}"
        }

        return NextYearSection(
            year = next.year,
            age = next.age,
            expenses = listOf(
                item("Needs", nextPlan.needs, cf.needs),
                item("Wants", nextPlan.wants, cf.wants),
                item("Healthcare", nextPlan.healthcare, cf.healthcare),
                item("Property Tax", nextPlan.propertyTax, cf.propertyTax),
                item("Income Tax", nextPlan.incomeTax, cf.incomeTax),
                item("One-time expenses", next.cashFlow.oneTimeExpenses, cf.oneTimeExpenses)
            ),
            income = listOf(
                item("Salary", nextPlan.salary, cf.salary),
                item("Social Security", nextSs, cf.socialSecurity),
                item("Interest (estimate)", nextPlan.estimatedInterest, cf.interest, exact = false),
                item("Dividends (estimate)", nextPlan.estimatedDividends, cf.dividends, exact = false)
            ),
            incomeGap = if (nextPlan.isWorkingYear) null else
                item("Income gap (estimate)", nextPlan.estimatedAig, row.metrics.annualIncomeGap ?: 0.0, exact = false),
            changes = changes
        )
    }

    // --- Section 5: Money Movement (FR004-050–FR004-055) ---

    private val accounts = listOf(Endpoint.SB, Endpoint.CBB, Endpoint.TBA, Endpoint.TDA, Endpoint.TFA)

    private fun Portfolio.of(account: Endpoint) = when (account) {
        Endpoint.SB -> sb
        Endpoint.CBB -> cbb
        Endpoint.TBA -> tba
        Endpoint.TDA -> tda
        Endpoint.TFA -> tfa
        else -> error("$account is not an account")
    }

    /** Ledgers and flows both come from the recorded movements, so they always add up to each other. */
    private fun moneyMovement(opening: Portfolio, yearTrace: YearTrace): MoneyMovementSection {
        val movements = yearTrace.movements
        val ledgers = accounts.map { account ->
            val rows = movements
                .filter { it.from == account || it.to == account }
                .groupBy { it.type to (if (it.to == account) it.from else it.to) }
                .map { (key, moves) ->
                    val amounts = (1..4).map { q ->
                        moves.filter { it.quarter == q }.sumOf { if (it.to == account) it.amount else -it.amount }
                    }
                    LedgerRow(key.first, key.second, amounts, amounts.sum())
                }
                .sortedWith(compareBy({ it.type.ordinal }, { it.counterparty.ordinal }))
            val openings = mutableListOf<Double>()
            val closings = mutableListOf<Double>()
            var balance = opening.of(account)
            for (q in 0..3) {
                openings += balance
                balance += rows.sumOf { it.amounts[q] }
                closings += balance
            }
            AccountLedger(account, openings, rows, closings)
        }
        // One net flow per pair of endpoints: money can move both ways within a year (market gains and losses,
        // interest on a negative SB), and the flow diagram can't draw cycles
        val flows = movements
            .groupBy { setOf(it.from, it.to) }
            .mapNotNull { (_, moves) ->
                val (a, b) = moves.first().let { it.from to it.to }
                val net = moves.sumOf { if (it.from == a) it.amount else -it.amount }
                when {
                    net > 0.005 -> Flow(a, b, net)
                    net < -0.005 -> Flow(b, a, -net)
                    else -> null
                }
            }
        return MoneyMovementSection(
            ledgers = ledgers,
            flows = flows,
            quarterStrategy = yearTrace.quarterStrategy.map { it.copy(explanation = strategyExplanation(it)) },
            failure = yearTrace.failure
        )
    }

    private fun strategyExplanation(q: QuarterStrategy): String {
        val market = "market at ${percent(q.marketVs12MonthsPrior)} of 12 months earlier and " +
            "${percent(q.marketVsAllTimeHigh)} of its all-time high"
        val rule = when (q.rule) {
            StrategyRule.MARKET_UP_CBB_FULL -> "Market up ($market) and the CBB full: refill the SB from TDA/TBA"
            StrategyRule.MARKET_UP_CBB_REFILL ->
                "Market up ($market) and the CBB below its cap: refill the SB and the CBB from TDA/TBA"
            StrategyRule.MARKET_DOWN_HOLD -> "Market down ($market) and the SB above its floor: nothing moved"
            StrategyRule.MARKET_DOWN_FROM_CBB -> "Market down ($market) and the SB at or below its floor: refill the SB from the CBB"
        }
        val limit = when (q.limitedBy) {
            RefillLimit.QUARTERLY_SHARE -> "the refill was the quarter's share of the income gap"
            RefillLimit.SB_CAP -> "the refill was limited by the SB cap"
            RefillLimit.SB_FLOOR_DEFICIT -> "the refill also repaid the SB's shortfall below its floor"
            RefillLimit.CBB_CAP -> "the refill was limited by the CBB cap"
            RefillLimit.AVAILABLE_BALANCE -> "the refill was limited by the balance available"
            RefillLimit.NONE -> null
        }
        return if (limit == null) "$rule." else "$rule; $limit."
    }

    private fun percent(ratio: Double) = "%.0f%%".format(ratio * 100)
}
