package com.retirement.logic

import com.retirement.model.*
import org.slf4j.LoggerFactory
import kotlin.math.pow

object SimulationEngine {
    private val log = LoggerFactory.getLogger(SimulationEngine::class.java)

    /**
     * Runs one simulation. The per-year lists are indexed by year of the simulation (0 = start year, not
     * simulated); a year missing from a list uses the config's static rate. Interactive runs pass empty
     * lists (or a preset's equity returns); Monte Carlo passes a full scenario.
     */
    fun runSimulation(
        config: SimulationConfig,
        marketReturns: List<Double>,
        inflationRates: List<Double>,
        bondYields: List<Double> = emptyList(),
        logExpenses: Boolean = true
    ): SimulationResult {
        if (logExpenses) logOneTimeExpenses(config)
        var currentYear = config.currentYear
        var currentAge = config.currentAge
        val endAge = config.endAge
        
        var balances = config.portfolio
        
        val yearlyResults = mutableListOf<YearlyResult>()
        val quarterlyResults = mutableListOf<QuarterlyResult>()

        // Cumulative inflation factor for each year of the simulation (index 0 = start year = 1.0)
        val inflationFactors = cumulativeInflation(config.rates.inflation, inflationRates, endAge - config.currentAge)
        fun bondYieldFor(yearIdx: Int) = bondYields.getOrNull(yearIdx) ?: config.rates.bondYield
        
                // Calculate Base Cap AIG (uses 50% of wants) for SB and CBB cap computation
                // This is NOT inflation adjusted - it's the fixed base for cap calculations
                val baseCapAig = (config.expenses.needs + (config.expenses.wants * 0.5) + config.expenses.propertyTax + config.expenses.healthcarePostRetirementPreMedicare) -
                                 (config.portfolio.sb * config.rates.hysaRate) -
                                 (config.portfolio.cbb * config.rates.bondYield)

                // Initial CBB Cap = 7 × baseCapAig (does NOT adjust with inflation)
                // Reduces by 1 × baseCapAig at age 65, and every 5 years thereafter (70, 75, 80, 85)
                val initialCbbCap = baseCapAig * 7.0

                // Helper function to calculate CBB cap reduction based on age
                fun calculateCbbCap(age: Int): Double {
                    var reductions = 0
                    if (age >= 65) reductions++
                    if (age >= 70) reductions++
                    if (age >= 75) reductions++
                    if (age >= 80) reductions++
                    if (age >= 85) reductions++
                    return maxOf(0.0, initialCbbCap - (reductions * baseCapAig))
                }

                                // Capture Initial State (Starting Line)
                                val initialCashFlow = CashFlow(
                                    salary = 0.0, interest = 0.0, dividends = 0.0, socialSecurity = 0.0, 
                                    tbaWithdrawal = 0.0, tdaWithdrawal = 0.0, 
                                    tdaWithdrawalSpend = 0.0, tdaWithdrawalRoth = 0.0,
                                    sbDeposit = 0.0, sbWithdrawal = 0.0,
                                    rothConversion = 0.0,
                                    contribution401k = 0.0, contributionRoth = 0.0, contributionTba = 0.0,
                                    totalIncome = 0.0, needs = 0.0, wants = 0.0, healthcare = 0.0,
                                    incomeTax = 0.0, propertyTax = 0.0, totalExpenses = 0.0
                                )
                                // The start year is a balance snapshot with no cash flows, so it has no income gap
                                val initialMetrics = Metrics(
                                    annualIncomeGap = null,
                                    incomeGapExpenses = config.expenses.needs + config.expenses.wants + config.expenses.propertyTax + config.expenses.healthcarePostRetirementPreMedicare,
                                    incomeGapPassiveIncome = (config.portfolio.sb * config.rates.hysaRate) + (config.portfolio.cbb * config.rates.bondYield),
                                    sbCap = if (currentAge <= config.retirementAge) null else baseCapAig * 2.0,
                                    cbbCap = calculateCbbCap(currentAge),
                                    isFailure = false
                                )

                yearlyResults.add(YearlyResult(
                    year = currentYear,
                    age = currentAge,
                    balances = balances,
                    cashFlow = initialCashFlow,
                    metrics = initialMetrics
                ))
                
                quarterlyResults.add(QuarterlyResult(
                    year = currentYear,
                    quarter = 3,
                    age = currentAge,
                    balances = balances,
                    cashFlow = initialCashFlow,
                    metrics = initialMetrics
                ))
                
                var totalDividends = 0.0
                var totalInterest = 0.0
                
                var isFailure = false
                var failureYear: Int? = null
        
                        // Tracking ATH for S&P (normalized to 1.0 at start)
                        var currentMarketValue = 1.0
                        var allTimeHigh = 1.0
                        // Market value at the start of each quarter, for the point-to-point 12-month comparison
                        val quarterStartMarketValues = mutableListOf<Double>()
                        
                        // Initialize accrued interest/dividends from Start Year Q4 (assumed constant balance)
                        // These will be credited on Year 1 Q1 (Start of first quarter)
                        var accruedInterest = balances.sb * (config.rates.hysaRate / 4.0)
                        var accruedDividends = balances.cbb * (config.rates.bondYield / 4.0)
                        
                        // Estimate Prior Year (Year 0) Taxable Income for Year 1 Tax Bill
                        // Assume full year of salary and investment income
                        // Subtract 401k (Pre-Tax)
                        var priorYearTaxableIncome = (config.salary - config.contributions.annual401k) + 
                                                     (balances.sb * config.rates.hysaRate) + 
                                                     (balances.cbb * config.rates.bondYield)
                        // Add SS if applicable in Year 0? Simplified: Ignore SS for Year 0 estimate unless obviously claiming.
                        
                        // CHANGED: Start simulation from Year 1 (Next Year)
                        val duration = endAge - config.currentAge
                        for (yearIdx in 1..duration) {                    val year = currentYear + yearIdx
                    val age = currentAge + yearIdx

                    // Working year: salary all year, salary covers expenses, no income gap.
                    // The retirement-age year counts as a full working year.
                    val isWorkingYear = age <= config.retirementAge

                    // Inflation factor for this year (cumulative)
                    val inflationAdjustment = inflationFactors[yearIdx]
                    val bondYield = bondYieldFor(yearIdx)
                    
                    // Roth Conversion Logic (Inflated)
                    // Begins from Year 2 of simulation (yearIdx > 1), i.e., the year after the start
                    // Uses different amounts for pre-retirement vs post-retirement
                    val baseRothAmount = if (isWorkingYear) {
                        config.strategy.rothConversionPreRetirement
                    } else {
                        config.strategy.rothConversionPostRetirement
                    }
                    val annualRothConversion = if (yearIdx > 1) baseRothAmount * inflationAdjustment else 0.0


                    // Calculate Tax Due for this year (Based on Prior Year Income)
                    val annualTaxDue = priorYearTaxableIncome * config.rates.incomeTax
                    
                    // Calculate inflated expenses components
                    val healthcareBase = if (isWorkingYear) {
                        config.expenses.healthcarePreRetirement
                    } else if (age < 65) {
                        config.expenses.healthcarePostRetirementPreMedicare
                    } else {
                        config.expenses.healthcareMedicare
                    }
                    val healthcareAdjusted = healthcareBase * inflationAdjustment
                    val needsAdjusted = config.expenses.needs * inflationAdjustment
                    val propertyTaxAdjusted = config.expenses.propertyTax * inflationAdjustment
                    
                    // Estimate Recurring Income for logic
                    val estimatedDividends = balances.cbb * bondYield
                    
                    // Estimate Interest (approximate expenses exclude tax)
                    val approxExpenses = needsAdjusted + (config.expenses.wants * inflationAdjustment) + healthcareAdjusted + propertyTaxAdjusted
                    val estimatedInterest = estimatedAnnualInterest(isWorkingYear, balances.sb, config.rates.hysaRate, approxExpenses)
        
                                // Determine Annual Salary and Contributions for this year
                                val annualSalaryVal = if (isWorkingYear) config.salary * inflationAdjustment else 0.0
                                val annual401kVal = if (isWorkingYear) config.contributions.annual401k * inflationAdjustment else 0.0
                                val annualRothVal = if (isWorkingYear) config.contributions.annualRoth * inflationAdjustment else 0.0
                                val annualTbaVal = if (isWorkingYear) config.contributions.annualTba * inflationAdjustment else 0.0
                    // Social Security Logic
                    val lowerEarnerAge = config.spousal.spouseAge + yearIdx
                    val higherEarnerAge = age 
                    
                    var annualSocialSecurity = 0.0
                    
                    // Determine Lower Earner Benefit
                    var lowerEarnerBenefit = 0.0
                    if (lowerEarnerAge >= config.spousal.lowerEarner.claimAge) {
                        lowerEarnerBenefit = config.spousal.lowerEarner.annualBenefit * inflationAdjustment
                    }
                    
                    // Determine Higher Earner Benefit
                    var higherEarnerBenefit = 0.0
                    val higherEarnerClaimed = higherEarnerAge >= config.spousal.higherEarner.claimAge
                    if (higherEarnerClaimed) {
                        higherEarnerBenefit = config.spousal.higherEarner.annualBenefit * inflationAdjustment
                    }
                    
                    // Spousal Step-Up Logic
                    if (higherEarnerClaimed && lowerEarnerAge >= config.spousal.lowerEarner.claimAge) {
                        lowerEarnerBenefit = maxOf(lowerEarnerBenefit, higherEarnerBenefit * 0.5)
                    }
                    
                    annualSocialSecurity = lowerEarnerBenefit + higherEarnerBenefit
                    
                                            // ADDED: Pre-Retirement Wants Adjustment
                                            // Before retirement, wants expenses is adjusted so that total expenses are covered entirely by salary.
                                            // This means Interest and Dividends are effectively saved.
                                            // Wants = Salary - (Needs + Healthcare + PropTax + Tax + 401k + Roth + TBA)
                                            // Roth conversions are not deducted: they move TDA→TFA without touching the SB,
                                            // and their tax is in next year's tax bill.
                                            var wantsAdjusted = config.expenses.wants * inflationAdjustment
                                            if (isWorkingYear) {
                                                val fixedOutflows = needsAdjusted + healthcareAdjusted + propertyTaxAdjusted + annualTaxDue + annual401kVal + annualRothVal + annualTbaVal
                                                // We only use Salary to cover expenses. Interest/Dividends are saved.
                                                val availableForWants = annualSalaryVal - fixedOutflows
                                                wantsAdjusted = maxOf(0.0, availableForWants)
                                            }
                                
                                            val grossExpenses = needsAdjusted + wantsAdjusted + healthcareAdjusted + propertyTaxAdjusted
                    
                                // Estimate AIG for spending strategy (before monthly loop)
                                // This uses estimated passive income (see estimatedAnnualInterest)
                                // The actual AIG will be calculated at year-end from real totals
                                val estimatedPassiveIncome = estimatedInterest + estimatedDividends + annualSocialSecurity
                                val estimatedAig = (needsAdjusted + wantsAdjusted + healthcareAdjusted + propertyTaxAdjusted + annualTaxDue) - estimatedPassiveIncome

                                // Track annual flows
                                var annualSalary = 0.0
                                var annualInterest = 0.0
                                var annualDividends = 0.0
                                var tbaWithdrawal = 0.0
                                var tdaWithdrawal = 0.0
                                var tdaWithdrawalSpend = 0.0 // TDA withdrawal for spending
                                var tdaWithdrawalRoth = 0.0 // TDA withdrawal for Roth conversion
                                var annualSbDeposit = 0.0 // Track deposits to SB
                                var annualSbWithdrawal = 0.0 // Track withdrawals from SB
                                var rothConversion = 0.0
                                var annualOneTimeExpenses = 0.0 // Track one-time expenses for this year
                                val yearOneTimeExpenseBreakdown = mutableListOf<ExpenseDetail>() // Breakdown of expenses

                                // Quarterly accumulators
                                var qSalary = 0.0
                                var qInterest = 0.0
                                var qDividends = 0.0
                                var qSS = 0.0
                                var qTbaW = 0.0
                                var qTdaW = 0.0
                                var qTdaWSpend = 0.0 // Quarterly TDA withdrawal for spending
                                var qTdaWRoth = 0.0 // Quarterly TDA withdrawal for Roth
                                var qSbDeposit = 0.0 // Quarterly SB Deposit
                                var qSbWithdrawal = 0.0 // Quarterly SB Withdrawal
                                var qRoth = 0.0
                                var qNeeds = 0.0
                                var qWants = 0.0
                                var qHealth = 0.0
                                            var qTax = 0.0
                                            var qProp = 0.0
                                            var q401k = 0.0
                                            var qTba = 0.0
                                var qOneTimeExpenses = 0.0 // Quarterly one-time expenses accumulator

                                            for (quarter in 1..4) {                // 0. Quarterly Events (Start of Quarter - Day 1)
                // Point-to-point market performance: value now vs. exactly 12 months (4 quarters) earlier.
                // Before the simulation has a year of history, the market is taken as flat before the start.
                val twelveMonthsPrior = quarterStartMarketValues.getOrNull(quarterStartMarketValues.size - 4) ?: 1.0
                val marketPerformance = currentMarketValue / twelveMonthsPrior
                quarterStartMarketValues.add(currentMarketValue)

                if (quarter >= 1) {
                    // Credit Accrued Interest from previous quarter
                    balances = balances.copy(sb = balances.sb + accruedInterest)
                    annualInterest += accruedInterest
                    totalInterest += accruedInterest
                    qInterest += accruedInterest
                    annualSbDeposit += accruedInterest // Deposit
                    qSbDeposit += accruedInterest // Deposit
                    accruedInterest = 0.0

                    // Credit Accrued Dividends from previous quarter
                    balances = balances.copy(sb = balances.sb + accruedDividends)
                    annualDividends += accruedDividends
                    totalDividends += accruedDividends
                    qDividends += accruedDividends
                    annualSbDeposit += accruedDividends // Deposit
                    qSbDeposit += accruedDividends // Deposit
                    accruedDividends = 0.0

                    // Spending Strategy Withdrawal (Starts year AFTER retirement)
                    if (!isWorkingYear) {
                        // Calculate capAig for this year (50% wants, inflation adjusted for cap computation)
                        // Passive income IS deducted - caps represent cashflow needed to cover expenses that passive income doesn't cover
                        val yearCapAig = (needsAdjusted + (wantsAdjusted * 0.5) + healthcareAdjusted + propertyTaxAdjusted + annualTaxDue) - estimatedPassiveIncome
                        val yearCbbCap = calculateCbbCap(age)

                        val spendingResult = SpendingStrategy.executeQuarterly(
                            quarter - 1,
                            config,
                            balances,
                            estimatedAig,
                            yearCapAig, // AIG with 50% wants for cap computation
                            grossExpenses + annualTaxDue, // Regular expenses for the SB floor
                            yearCbbCap, // CBB cap (reduces at 65, 70, 75, 80, 85)
                            marketPerformance,
                            currentMarketValue / allTimeHigh,
                            1.0, // cbbPerformance placeholder
                            inflationAdjustment
                        )
                        // The strategy only ever adds to the SB (equity refills to the CBB bypass it),
                        // so the SB delta is exactly what was deposited
                        val strategySbDeposit = spendingResult.portfolio.sb - balances.sb
                        balances = spendingResult.portfolio
                        annualSbDeposit += strategySbDeposit
                        qSbDeposit += strategySbDeposit

                        tbaWithdrawal += spendingResult.tbaWithdrawal
                        tdaWithdrawal += spendingResult.tdaWithdrawal
                        tdaWithdrawalSpend += spendingResult.tdaWithdrawal // Track as spending purpose
                        qTbaW += spendingResult.tbaWithdrawal
                        qTdaW += spendingResult.tdaWithdrawal
                        qTdaWSpend += spendingResult.tdaWithdrawal // Track as spending purpose
                    }

                    // Roth Conversion logic: Direct TDA to TFA withdrawal (Quarterly)
                    // Starts in Year 2 of simulation, works both pre-retirement and post-retirement
                    // This is SEPARATE from spending strategy - goes directly TDA→TFA, NOT through SB
                    if (yearIdx > 1 && annualRothConversion > 0) {
                        val quarterlyRoth = annualRothConversion / 4.0

                        // Direct withdrawal from TDA to TFA (both pre and post retirement)
                        val actualRoth = minOf(quarterlyRoth, balances.tda)
                        if (actualRoth > 0) {
                            balances = balances.copy(
                                tda = balances.tda - actualRoth,
                                tfa = balances.tfa + actualRoth
                            )
                            rothConversion += actualRoth
                            qRoth += actualRoth

                            // Track as TDA withdrawal for Roth conversion (separate from spending)
                            tdaWithdrawal += actualRoth
                            tdaWithdrawalRoth += actualRoth
                            qTdaW += actualRoth
                            qTdaWRoth += actualRoth
                        }
                    }
                }

                // 1. Salary (working years) - Quarterly
                if (isWorkingYear) {
                    val quarterlySalaryGross = annualSalaryVal / 4.0
                    val quarterly401k = annual401kVal / 4.0
                    val quarterlyRothContribution = annualRothVal / 4.0
                    val quarterlyTba = annualTbaVal / 4.0

                    // Net Salary = Gross - Contributions (deducted at source)
                    val quarterlyNetSalary = quarterlySalaryGross - quarterly401k - quarterlyRothContribution - quarterlyTba

                    // Only Net Salary hits the SB
                    balances = balances.copy(sb = balances.sb + quarterlyNetSalary)

                    annualSalary += quarterlySalaryGross
                    qSalary += quarterlySalaryGross

                    annualSbDeposit += quarterlyNetSalary
                    qSbDeposit += quarterlyNetSalary

                    // Contributions go directly to accounts (bypass SB)
                    balances = balances.copy(
                        tda = balances.tda + quarterly401k,
                        tfa = balances.tfa + quarterlyRothContribution, // Roth contributions go directly to TFA
                        tba = balances.tba + quarterlyTba
                    )
                    q401k += quarterly401k
                    qTba += quarterlyTba

                    // Savings contributions are no longer an SB withdrawal
                    // annualSbWithdrawal += 0.0 
                    // annualSbWithdrSavings += 0.0
                }

                // Add Quarterly Social Security
                val quarterlySS = annualSocialSecurity / 4.0
                balances = balances.copy(sb = balances.sb + quarterlySS)
                qSS += quarterlySS

                annualSbDeposit += quarterlySS
                qSbDeposit += quarterlySS

                // SUBTRACT Quarterly Expenses from SB
                // Salary, interest, dividends and Social Security were deposited in full above,
                // so the full expenses come out here (withdrawing only the income gap would count that income twice)
                val quarterlyEstimatedTax = annualTaxDue / 4.0
                val quarterlyExpenses = (grossExpenses + annualTaxDue) / 4.0
                balances = balances.copy(sb = balances.sb - quarterlyExpenses)

                annualSbWithdrawal += quarterlyExpenses
                qSbWithdrawal += quarterlyExpenses

                qNeeds += needsAdjusted / 4.0
                qWants += wantsAdjusted / 4.0
                qHealth += healthcareAdjusted / 4.0
                qProp += propertyTaxAdjusted / 4.0
                qTax += quarterlyEstimatedTax

                // Process one-time expenses (after regular expenses), sequentially in entry order.
                // Each payment independently triggers the spending strategy if SB is insufficient.
                fun payOneTimeExpense(amount: Double) {
                    balances = balances.copy(sb = balances.sb - amount)
                    annualOneTimeExpenses += amount
                    annualSbWithdrawal += amount
                    qSbWithdrawal += amount
                    qOneTimeExpenses += amount // Add to quarterly accumulator

                    if (balances.sb < 0) {
                        val shortfallResult = SpendingStrategy.coverShortfall(
                            balances,
                            -balances.sb,
                            config.strategy.tdaWithdrawalPercentage,
                            calculateCbbCap(age)
                        )
                        val shortfallSbDeposit = shortfallResult.balances.sb - balances.sb
                        balances = shortfallResult.balances
                        annualSbDeposit += shortfallSbDeposit
                        qSbDeposit += shortfallSbDeposit
                        tbaWithdrawal += shortfallResult.tbaWithdrawal
                        tdaWithdrawal += shortfallResult.tdaWithdrawal
                        tdaWithdrawalSpend += shortfallResult.tdaWithdrawal
                    }
                }

                config.oneTimeExpenses.forEach { expense ->
                    when (expense) {
                        is CashExpense -> {
                            // Cash expenses: inflation-adjusted lump sum in Q1 of the expense year
                            val expenseYear = expense.yearOrAge.toYear(config.currentAge, config.currentYear)
                            if (quarter == 1 && expenseYear == year) {
                                val adjustedAmount = expense.amount * inflationAdjustment
                                payOneTimeExpense(adjustedAmount)
                                yearOneTimeExpenseBreakdown.add(ExpenseDetail(
                                    name = expense.name,
                                    amount = adjustedAmount,
                                    type = ExpenseType.CASH
                                ))
                            }
                        }
                        is LoanExpense -> {
                            val startYear = expense.startYearOrAge.toYear(config.currentAge, config.currentYear)
                            val endYear = expense.getEndYear(config.currentAge, config.currentYear)

                            // Down payment: inflation-adjusted lump sum in Q1 of the start year
                            if (quarter == 1 && year == startYear && expense.downPayment > 0.0) {
                                val adjustedDownPayment = expense.downPayment * inflationAdjustment
                                payOneTimeExpense(adjustedDownPayment)
                                yearOneTimeExpenseBreakdown.add(ExpenseDetail(
                                    name = "${expense.name} (Down Payment)",
                                    amount = adjustedDownPayment,
                                    type = ExpenseType.CASH
                                ))
                            }

                            // Loan payments: fixed nominal amount every quarter of the term,
                            // computed server-side from the financed amount in start-year dollars.
                            // A down payment covering the whole principal leaves nothing to amortize.
                            if (year in startYear..endYear && expense.getFinancedAmount() > 0.0) {
                                val startYearInflation = inflationFactors.getOrElse(startYear - config.currentYear) {
                                    (1.0 + config.rates.inflation).pow(startYear - config.currentYear)
                                }
                                val quarterlyPayment = expense.quarterlyPaymentFor(startYearInflation)
                                payOneTimeExpense(quarterlyPayment)

                                // Breakdown shows the annual total (4 quarterly payments)
                                if (quarter == 1) {
                                    yearOneTimeExpenseBreakdown.add(ExpenseDetail(
                                        name = expense.name,
                                        amount = quarterlyPayment * 4,
                                        type = ExpenseType.LOAN_PAYMENT
                                    ))
                                }
                            }
                        }
                    }
                }

                // 2. Interest (Quarterly Accrual)
                val quarterlyInterest = balances.sb * (config.rates.hysaRate / 4.0)
                accruedInterest += quarterlyInterest

                // 3. Dividends (Quarterly Accrual)
                val quarterlyDividends = balances.cbb * (bondYield / 4.0)
                accruedDividends += quarterlyDividends

                // 5. Quarterly Equity Growth
                val equityGrowth = if (yearIdx < marketReturns.size) marketReturns[yearIdx] else (if (isWorkingYear) config.rates.preRetirementGrowth else config.rates.postRetirementGrowth)
                val quarterlyGrowth = (1.0 + equityGrowth).pow(1.0/4.0) - 1.0

                // Track Market Index
                currentMarketValue *= (1.0 + quarterlyGrowth)
                allTimeHigh = maxOf(allTimeHigh, currentMarketValue)

                balances = balances.copy(
                    tba = balances.tba * (1.0 + quarterlyGrowth),
                    tda = balances.tda * (1.0 + quarterlyGrowth),
                    tfa = balances.tfa * (1.0 + quarterlyGrowth)
                )

                // Check for failure
                // We allow minor SB negativity if it's within 10% of quarterly expenses, to handle timing issues
                // Simulation fails when BOTH TBA and TDA go below 0 (allows one equity account to go negative)
                // Or when SB, CBB, or TFA go negative beyond thresholds
                val bothEquitiesNegative = balances.tba < 0 && balances.tda < 0
                if (balances.sb < -(grossExpenses / 4.0) || balances.cbb < 0 || bothEquitiesNegative || balances.tfa < 0) {
                    isFailure = true
                    if (failureYear == null) failureYear = year
                }
                
                // Capture Quarterly Result (at end of each quarter)
                if (quarter >= 1) {
                    // Calculate quarterly Income Gap from actual expenses and passive income
                    val qTotalExpenses = qNeeds + qWants + qHealth + qTax + qProp + qOneTimeExpenses
                    val qPassiveIncome = qInterest + qDividends + qSS
                    val qIncomeGap = if (isWorkingYear) null else qTotalExpenses - qPassiveIncome

                    quarterlyResults.add(QuarterlyResult(
                        year = year,
                        quarter = quarter - 1,
                        age = age,
                        balances = balances,
                        cashFlow = CashFlow(
                            salary = qSalary,
                            interest = qInterest,
                            dividends = qDividends,
                            socialSecurity = qSS,
                            tbaWithdrawal = qTbaW,
                            tdaWithdrawal = qTdaW,
                            tdaWithdrawalSpend = qTdaWSpend,
                            tdaWithdrawalRoth = qTdaWRoth,
                            sbDeposit = qSbDeposit,
                            sbWithdrawal = qSbWithdrawal,
                            rothConversion = qRoth,
                            contribution401k = q401k,
                            contributionRoth = 0.0, // Quarterly contribution not tracked separately
                            contributionTba = qTba,
                            totalIncome = qSalary + qInterest + qDividends + qSS + qTbaW + qTdaW + qRoth,
                            needs = qNeeds,
                            wants = qWants,
                            healthcare = qHealth,
                            incomeTax = qTax,
                            propertyTax = qProp,
                            totalExpenses = qTotalExpenses,
                            oneTimeExpenses = qOneTimeExpenses // Include quarterly one-time expenses
                        ),
                        metrics = Metrics(
                            annualIncomeGap = qIncomeGap,
                            incomeGapExpenses = qTotalExpenses,
                            incomeGapPassiveIncome = qPassiveIncome,
                            sbCap = qIncomeGap?.let { gap ->
                                val qCapAig = (qNeeds + (qWants * 0.5) + qHealth + qTax + qProp) - qPassiveIncome
                                SpendingStrategy.sbCap(gap, qCapAig, qTotalExpenses - qOneTimeExpenses)
                            },
                            cbbCap = calculateCbbCap(age),
                            isFailure = isFailure
                        )
                    ))
                    
                    // Reset Accumulators
                    qSalary = 0.0
                    qInterest = 0.0
                    qDividends = 0.0
                    qSS = 0.0
                    qTbaW = 0.0
                    qTdaW = 0.0
                    qTdaWSpend = 0.0
                    qTdaWRoth = 0.0
                    qSbDeposit = 0.0
                    qSbWithdrawal = 0.0
                    qRoth = 0.0
                    q401k = 0.0
                    qTba = 0.0
                    qNeeds = 0.0
                    qWants = 0.0
                    qHealth = 0.0
                    qTax = 0.0
                    qProp = 0.0
                    qOneTimeExpenses = 0.0 // Reset quarterly one-time expenses
                }
            }

            // Calculate Taxable Income for Next Year's Tax Bill
            // Includes Salary, Interest (Credited), Dividends (Credited), Social Security, TDA withdrawals, Roth Conversions, and 50% of TBA withdrawals
            val currentYearTaxableIncome = annualSalary + annualInterest + annualDividends + annualSocialSecurity + tdaWithdrawal + rothConversion + (tbaWithdrawal * 0.5)
            
            // Update Prior Year Income for next iteration
            priorYearTaxableIncome = currentYearTaxableIncome

            // Calculate actual AIG for this year from real expenses and passive income
            // AIG = Total Expenses - Passive Income (including one-time expenses); none in working years
            val totalExpenses = needsAdjusted + wantsAdjusted + healthcareAdjusted + propertyTaxAdjusted + annualTaxDue + annualOneTimeExpenses
            val passiveIncome = annualInterest + annualDividends + annualSocialSecurity
            val currentAig = if (isWorkingYear) null else totalExpenses - passiveIncome

            // Cap AIG uses 50% of wants (for SB and CBB cap computation)
            val currentCapAig = (needsAdjusted + (wantsAdjusted * 0.5) + healthcareAdjusted + propertyTaxAdjusted + annualTaxDue + annualOneTimeExpenses) - passiveIncome

            // Record yearly result
            yearlyResults.add(YearlyResult(
                year = year,
                age = age,
                balances = balances,
                cashFlow = CashFlow(
                    salary = annualSalary,
                    interest = annualInterest,
                    dividends = annualDividends,
                    socialSecurity = annualSocialSecurity, 
                    tbaWithdrawal = tbaWithdrawal,
                    tdaWithdrawal = tdaWithdrawal,
                    tdaWithdrawalSpend = tdaWithdrawalSpend,
                    tdaWithdrawalRoth = tdaWithdrawalRoth,
                    sbDeposit = annualSbDeposit,
                    sbWithdrawal = annualSbWithdrawal,
                    rothConversion = rothConversion,
                    contribution401k = annual401kVal,
                    contributionRoth = annualRothVal,
                    contributionTba = annualTbaVal,
                    totalIncome = annualSalary + annualInterest + annualDividends + annualSocialSecurity + tbaWithdrawal + tdaWithdrawal + rothConversion,
                    needs = needsAdjusted,
                    wants = wantsAdjusted,
                    healthcare = healthcareAdjusted,
                    incomeTax = annualTaxDue,
                    propertyTax = propertyTaxAdjusted,
                    totalExpenses = totalExpenses,
                    oneTimeExpenses = annualOneTimeExpenses
                ),
                metrics = Metrics(
                    annualIncomeGap = currentAig,
                    incomeGapExpenses = totalExpenses,
                    incomeGapPassiveIncome = passiveIncome,
                    sbCap = currentAig?.let { SpendingStrategy.sbCap(it, currentCapAig, grossExpenses + annualTaxDue) },
                    cbbCap = calculateCbbCap(age),
                    isFailure = isFailure
                ),
                oneTimeExpensesBreakdown = if (yearOneTimeExpenseBreakdown.isNotEmpty()) yearOneTimeExpenseBreakdown.toList() else null
            ))

            if (isFailure) break
        }

        return SimulationResult(
            config = config,
            yearlyResults = yearlyResults,
            quarterlyResults = quarterlyResults,
            summary = Summary(
                finalTotalBalance = balances.sb + balances.cbb + balances.tba + balances.tda + balances.tfa,
                isSuccess = !isFailure,
                failureYear = failureYear,
                totalDividends = totalDividends,
                totalInterest = totalInterest
            ),
            apiMetadata = com.retirement.util.ServerInfo.getMetadata()
        )
    }

    /**
     * Cumulative inflation factor for years 0..years. With no per-year rates it is (1 + inflation)^year,
     * as for an interactive run; otherwise the product of each year's (1 + rate).
     */
    internal fun cumulativeInflation(inflation: Double, inflationRates: List<Double>, years: Int): DoubleArray {
        if (inflationRates.isEmpty()) return DoubleArray(years + 1) { (1.0 + inflation).pow(it) }
        val factors = DoubleArray(years + 1)
        factors[0] = 1.0
        for (i in 1..years) factors[i] = factors[i - 1] * (1.0 + (inflationRates.getOrNull(i) ?: inflation))
        return factors
    }

    /**
     * Interest estimate for the spending strategy's AIG.
     * Working years: the SB is stable or growing, so its current balance is a good estimator.
     * Retirement years: the SB is refilled through the year to cover expenses, so its average balance is about
     * half the annual expenses, whatever it holds at the start of the year.
     */
    internal fun estimatedAnnualInterest(isWorkingYear: Boolean, sb: Double, hysaRate: Double, annualExpenses: Double): Double =
        if (isWorkingYear) sb * hysaRate else (annualExpenses / 2.0) * hysaRate

    /**
     * Log each one-time expense with its resolved timing and, for loans, the simulated quarterly payment,
     * so a report of "wrong expense amount" can be traced from the server log.
     */
    private fun logOneTimeExpenses(config: SimulationConfig) {
        if (config.oneTimeExpenses.isEmpty()) return
        val lastYear = SimulationPeriod.lastYear(config)
        log.info("Simulation '{}': {} one-time expense(s), simulated years {}..{}",
            config.name, config.oneTimeExpenses.size, SimulationPeriod.firstYear(config), lastYear)
        if (!log.isDebugEnabled) return
        config.oneTimeExpenses.forEachIndexed { i, expense ->
            when (expense) {
                is CashExpense -> {
                    val year = expense.yearOrAge.toYear(config.currentAge, config.currentYear)
                    log.debug("  [{}] CASH '{}': {} in {} (today's dollars)", i, expense.name, "%.2f".format(expense.amount), year)
                }
                is LoanExpense -> {
                    val startYear = expense.startYearOrAge.toYear(config.currentAge, config.currentYear)
                    val endYear = expense.getEndYear(config.currentAge, config.currentYear)
                    val payment = expense.quarterlyPaymentFor((1.0 + config.rates.inflation).pow(startYear - config.currentYear))
                    log.debug("  [{}] LOAN '{}': financed {} at {}% for {}y, {}..{}, fixed quarterly payment {}{}",
                        i, expense.name, "%.2f".format(expense.getFinancedAmount()), expense.aprPercent, expense.termYears,
                        startYear, endYear, "%.2f".format(payment),
                        if (endYear > lastYear) " (payments after $lastYear fall outside the simulation)" else "")
                }
            }
        }
    }
}
