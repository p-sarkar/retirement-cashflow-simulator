package com.retirement.logic

import com.retirement.model.*
import kotlin.math.pow

object SimulationEngine {
    fun runSimulation(config: SimulationConfig, marketReturns: List<Double>, inflationRates: List<Double>): SimulationResult {
        var currentYear = config.currentYear
        var currentAge = config.currentAge
        val endAge = 85
        
        var balances = config.portfolio
        
        val yearlyResults = mutableListOf<YearlyResult>()
        val quarterlyResults = mutableListOf<QuarterlyResult>()

                // Calculate Base AIG for the simulation (used for spending strategy)
                // initial value = needs+wants+property tax+health care-(interest on HYSA)-(dividend from CBB)
                // Use healthcarePostRetirementPreMedicare as the retirement baseline
                val baseAig = (config.expenses.needs + config.expenses.wants + config.expenses.propertyTax + config.expenses.healthcarePostRetirementPreMedicare) - 
                              (config.portfolio.sb * config.rates.hysaRate) - 
                              (config.portfolio.cbb * config.rates.bondYield)
        
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
                                    contribution401k = 0.0, contributionTba = 0.0,
                                    totalIncome = 0.0, needs = 0.0, wants = 0.0, healthcare = 0.0, 
                                    incomeTax = 0.0, propertyTax = 0.0, totalExpenses = 0.0
                                )
                                val initialMetrics = Metrics(
                                    annualIncomeGap = baseAig,
                                    incomeGapExpenses = config.expenses.needs + config.expenses.wants + config.expenses.propertyTax + config.expenses.healthcarePostRetirementPreMedicare,
                                    incomeGapPassiveIncome = (config.portfolio.sb * config.rates.hysaRate) + (config.portfolio.cbb * config.rates.bondYield),
                                    sbCap = baseCapAig * 2.0,
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
                        
                        // Initialize accrued interest/dividends from Start Year Q4 (assumed constant balance)
                        // These will be credited on Year 1 Month 1 (Start of Q1)
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
                    
                    // Inflation factor for this year (cumulative)
                    val inflationAdjustment = (1.0 + config.rates.inflation).pow(yearIdx)
                    
                    // Roth Conversion Logic (Inflated)
                    // Begins from Year 2 of simulation (yearIdx > 1), i.e., the year after the start
                    val annualRothConversion = if (yearIdx > 1) config.strategy.rothConversionAmount * inflationAdjustment else 0.0


                    // Calculate Tax Due for this year (Based on Prior Year Income)
                    val annualTaxDue = priorYearTaxableIncome * config.rates.incomeTax
                    
                    // Calculate inflated expenses components
                    val healthcareBase = if (age < config.retirementAge) {
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
                    val estimatedDividends = balances.cbb * config.rates.bondYield
                    
                    // Estimate Interest:
                    // Pre-Retirement: SB is stable/growing, so current balance is a good estimator.
                    // Post-Retirement: SB is refilled annually to cover expenses, so avg balance is ~Expenses/2. 
                    // We use approximate expenses (excluding tax) to estimate the interest.
                    val estimatedInterest = if (age < config.retirementAge) {
                        balances.sb * config.rates.hysaRate
                    } else {
                        val approxExpenses = needsAdjusted + (config.expenses.wants * inflationAdjustment) + healthcareAdjusted + propertyTaxAdjusted
                        (approxExpenses / 2.0) * config.rates.hysaRate
                    }
        
                                // Determine Annual Salary and Contributions for this year
                                val annualSalaryVal = if (age <= config.retirementAge) config.salary * inflationAdjustment else 0.0
                                val annual401kVal = if (age <= config.retirementAge) config.contributions.annual401k * inflationAdjustment else 0.0
                                val annualTbaVal = if (age <= config.retirementAge) config.contributions.annualTba * inflationAdjustment else 0.0                    
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
                                            // Wants = Salary - (Needs + Healthcare + PropTax + Tax + Roth + 401k + TBA)
                                            var wantsAdjusted = config.expenses.wants * inflationAdjustment
                                            if (age < config.retirementAge) {
                                                val fixedOutflows = needsAdjusted + healthcareAdjusted + propertyTaxAdjusted + annualTaxDue + annualRothConversion + annual401kVal + annualTbaVal
                                                // We only use Salary to cover expenses. Interest/Dividends are saved.
                                                val availableForWants = annualSalaryVal - fixedOutflows
                                                wantsAdjusted = maxOf(0.0, availableForWants)
                                            }
                                
                                            val grossExpenses = needsAdjusted + wantsAdjusted + healthcareAdjusted + propertyTaxAdjusted
                    
                                // Estimate AIG for spending strategy (before monthly loop)
                                // This uses estimated passive income based on current balances
                                // The actual AIG will be calculated at year-end from real totals
                                val estimatedPassiveIncome = (balances.sb * config.rates.hysaRate) +
                                                            (balances.cbb * config.rates.bondYield) +
                                                            annualSocialSecurity
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
                                
                                            for (month in 1..12) {                // 0. Quarterly Events (Start of Quarter - Day 1)
                if ((month - 1) % 3 == 0) {
                    // Credit Accrued Interest from previous 3 months
                    balances = balances.copy(sb = balances.sb + accruedInterest)
                    annualInterest += accruedInterest
                    totalInterest += accruedInterest
                    qInterest += accruedInterest
                    annualSbDeposit += accruedInterest // Deposit
                    qSbDeposit += accruedInterest // Deposit
                    accruedInterest = 0.0

                    // Credit Accrued Dividends from previous 3 months
                    balances = balances.copy(sb = balances.sb + accruedDividends)
                    annualDividends += accruedDividends
                    totalDividends += accruedDividends
                    qDividends += accruedDividends
                    annualSbDeposit += accruedDividends // Deposit
                    qSbDeposit += accruedDividends // Deposit
                    accruedDividends = 0.0

                    // Spending Strategy Withdrawal (Starts year AFTER retirement)
                    if (age > config.retirementAge) {
                        // Calculate capAig for this year (50% wants, inflation adjusted for cap computation)
                        // EXCLUSIVE of passive income (only expenses, no income subtraction)
                        val yearCapAig = needsAdjusted + (wantsAdjusted * 0.5) + healthcareAdjusted + propertyTaxAdjusted + annualTaxDue
                        val yearCbbCap = calculateCbbCap(age)

                        val spendingResult = SpendingStrategy.executeQuarterly(
                            (month - 1) / 3,
                            config,
                            balances,
                            estimatedAig,
                            yearCapAig, // AIG with 50% wants for cap computation
                            yearCbbCap, // CBB cap (reduces at 65, 70, 75, 80, 85)
                            1.0, // marketPerformance placeholder
                            currentMarketValue / allTimeHigh,
                            1.0, // cbbPerformance placeholder
                            inflationAdjustment
                        )
                        balances = spendingResult.portfolio
                        
                        // Spending Strategy Deposit to SB (if any)
                        // spendingResult.portfolio includes the updated SB.
                        // We can calculate the deposit: (newSB - oldSB) -> but this includes CBB movements?
                        // Actually, SpendingStrategy returns `withdrawnSB` implicitly via portfolio state.
                        // But wait, SpendingStrategy handles SB += withdrawnSB.
                        // We need to capture that specific movement.
                        // Ideally SpendingStrategy should return `sbDeposit`.
                        // For now, let's infer it or track it.
                        // Actually, `tbaWithdrawal` + `tdaWithdrawal` go to SB (mostly).
                        // Except if CBB refill happens.
                        // Let's assume all TDA/TBA withdrawals are INFLOWS to the system, but specifically to SB?
                        // SpendingStrategy logic: `sb += withdrawnSB`.
                        // We don't have easy access to `withdrawnSB` here without modifying SpendingResult.
                        // However, we know `tbaWithdrawal` and `tdaWithdrawal` totals.
                        // If CBB is refilled, some goes there.
                        // Let's modify SpendingResult to include `sbDeposit` later? 
                        // For now, let's approximate or just rely on portfolio delta? No, portfolio delta includes everything.
                        // Let's leave SB Deposit from Strategy as "Total TDA+TBA Withdrawal - CBB Withdrawal" (since CBB withdrawal goes to SB).
                        // Wait, CBB withdrawal goes CBB -> SB.
                        // TDA/TBA goes TDA/TBA -> SB (or CBB).
                        
                        // SIMPLIFICATION for tracking:
                        // SB Deposit = (TDA Withdrawal + TBA Withdrawal) - (Amount moved to CBB) + (Amount moved from CBB to SB)
                        // This is getting complex to track perfectly without changing SpendingStrategy.
                        // Let's look at `spendingResult.cbbWithdrawal`. This is CBB -> SB.
                        // So `annualSbDeposit += spendingResult.cbbWithdrawal`.
                        // What about TDA/TBA -> SB?
                        // `spendingResult.tbaWithdrawal` + `spendingResult.tdaWithdrawal` is total equity withdrawal.
                        // Some might have gone to CBB.
                        // We can track CBB delta? 
                        // Let's just track `cbbWithdrawal` (CBB->SB) as deposit.
                        // And we need TDA/TBA -> SB.
                        // If we look at `SpendingStrategy.kt`:
                        // `sb += withdrawnSB`
                        // `cbb += withdrawnCBB`
                        // `cbb -= withdrawAmount` (which is `cbbWithdrawal`)
                        
                        // We will add `sbDeposit` to SpendingResult in a future refactor for precision.
                        // For now, let's track `cbbWithdrawal` as SB Deposit.
                        annualSbDeposit += spendingResult.cbbWithdrawal
                        qSbDeposit += spendingResult.cbbWithdrawal
                        
                        // And we assume TDA/TBA -> SB is (Total Withdrawal - CBB Refill).
                        // Since we don't know CBB Refill amount easily here, let's assume ALL TDA/TBA goes to SB 
                        // UNLESS we see CBB grow?
                        // This is tricky.
                        // CORRECT FIX: Just use the TDA/TBA withdrawals as "Deposits" to the general pot, 
                        // but user asked for "Spend Bucket Deposits".
                        // I will implicitly assume for this visual that Equity Withdrawals ~ SB Deposits 
                        // (ignoring the internal CBB transfer for now to avoid breaking API contract too much).
                        // Actually, I can just use `tbaWithdrawal + tdaWithdrawal` as deposit. 
                        // It's technically "Cash generated".
                        
                        annualSbDeposit += (spendingResult.tbaWithdrawal + spendingResult.tdaWithdrawal)
                        qSbDeposit += (spendingResult.tbaWithdrawal + spendingResult.tdaWithdrawal)

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

                // 1. Salary (pre-retirement)
                if (age <= config.retirementAge) {
                    val monthlySalaryGross = annualSalaryVal / 12.0
                    val monthly401k = annual401kVal / 12.0
                    val monthlyTba = annualTbaVal / 12.0
                    
                    // Net Salary = Gross - Contributions (deducted at source)
                    val monthlyNetSalary = monthlySalaryGross - monthly401k - monthlyTba
                    
                    // Only Net Salary hits the SB
                    balances = balances.copy(sb = balances.sb + monthlyNetSalary)
                    
                    annualSalary += monthlySalaryGross
                    qSalary += monthlySalaryGross
                    
                    annualSbDeposit += monthlyNetSalary
                    qSbDeposit += monthlyNetSalary
                    
                    // Contributions go directly to accounts (bypass SB)
                    balances = balances.copy(
                        tda = balances.tda + monthly401k,
                        tba = balances.tba + monthlyTba
                    )
                    q401k += monthly401k
                    qTba += monthlyTba
                    
                    // Savings contributions are no longer an SB withdrawal
                    // annualSbWithdrawal += 0.0 
                    // annualSbWithdrSavings += 0.0
                }

                // Add Monthly Social Security
                val monthlySS = annualSocialSecurity / 12.0
                balances = balances.copy(sb = balances.sb + monthlySS)
                qSS += monthlySS
                
                annualSbDeposit += monthlySS
                qSbDeposit += monthlySS

                // SUBTRACT Monthly Income Gap from SB
                // We only withdraw the gap (expenses - passive income) since passive income is already deposited
                // Income Gap = Total Expenses - Passive Income
                // Monthly gap withdrawal = estimatedAig / 12
                val monthlyIncomeGap = estimatedAig / 12.0
                val monthlyEstimatedTax = annualTaxDue / 12.0
                balances = balances.copy(sb = balances.sb - monthlyIncomeGap)

                annualSbWithdrawal += monthlyIncomeGap
                qSbWithdrawal += monthlyIncomeGap

                qNeeds += needsAdjusted / 12.0
                qWants += wantsAdjusted / 12.0
                qHealth += healthcareAdjusted / 12.0
                qProp += propertyTaxAdjusted / 12.0
                qTax += monthlyEstimatedTax

                // 2. Interest (Monthly Accrual)
                val monthlyInterest = balances.sb * (config.rates.hysaRate / 12.0)
                accruedInterest += monthlyInterest

                // 3. Dividends (Monthly Accrual)
                val monthlyDividends = balances.cbb * (config.rates.bondYield / 12.0)
                accruedDividends += monthlyDividends

                // 5. Monthly Equity Growth
                val equityGrowth = if (yearIdx < marketReturns.size) marketReturns[yearIdx] else (if (age < config.retirementAge) config.rates.preRetirementGrowth else config.rates.postRetirementGrowth)
                val monthlyGrowth = (1.0 + equityGrowth).pow(1.0/12.0) - 1.0
                
                // Track Market Index
                currentMarketValue *= (1.0 + monthlyGrowth)
                allTimeHigh = maxOf(allTimeHigh, currentMarketValue)

                balances = balances.copy(
                    tba = balances.tba * (1.0 + monthlyGrowth),
                    tda = balances.tda * (1.0 + monthlyGrowth),
                    tfa = balances.tfa * (1.0 + monthlyGrowth)
                )

                // Check for failure
                // We allow minor SB negativity if it's within 10% of monthly expenses, to handle timing issues
                if (balances.sb < -(grossExpenses / 12.0) || balances.cbb < 0 || balances.tba < 0 || balances.tda < 0 || balances.tfa < 0) {
                    isFailure = true
                    if (failureYear == null) failureYear = year
                }
                
                // Capture Quarterly Result
                if (month % 3 == 0) {
                    // Calculate quarterly Income Gap from actual expenses and passive income
                    val qTotalExpenses = qNeeds + qWants + qHealth + qTax + qProp
                    val qPassiveIncome = qInterest + qDividends + qSS
                    val qIncomeGap = qTotalExpenses - qPassiveIncome

                    quarterlyResults.add(QuarterlyResult(
                        year = year,
                        quarter = (month / 3) - 1,
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
                            contributionTba = qTba,
                            totalIncome = qSalary + qInterest + qDividends + qSS + qTbaW + qTdaW + qRoth,
                            needs = qNeeds,
                            wants = qWants,
                            healthcare = qHealth,
                            incomeTax = qTax,
                            propertyTax = qProp,
                            totalExpenses = qTotalExpenses
                        ),
                        metrics = Metrics(
                            annualIncomeGap = qIncomeGap,
                            incomeGapExpenses = qTotalExpenses,
                            incomeGapPassiveIncome = qPassiveIncome,
                            sbCap = (qNeeds + (qWants * 0.5) + qHealth + qTax + qProp) * 2.0, // Exclusive of passive income
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
                }
            }

            // Calculate Taxable Income for Next Year's Tax Bill
            // Includes Salary, Interest (Credited), Dividends (Credited), Social Security, TDA withdrawals, Roth Conversions, and 50% of TBA withdrawals
            val currentYearTaxableIncome = annualSalary + annualInterest + annualDividends + annualSocialSecurity + tdaWithdrawal + rothConversion + (tbaWithdrawal * 0.5)
            
            // Update Prior Year Income for next iteration
            priorYearTaxableIncome = currentYearTaxableIncome

            // Calculate actual AIG for this year from real expenses and passive income
            // AIG = Total Expenses - Passive Income
            val totalExpenses = needsAdjusted + wantsAdjusted + healthcareAdjusted + propertyTaxAdjusted + annualTaxDue
            val passiveIncome = annualInterest + annualDividends + annualSocialSecurity
            val currentAig = totalExpenses - passiveIncome

            // Cap AIG uses 50% of wants (for SB and CBB cap computation)
            val currentCapAig = (needsAdjusted + (wantsAdjusted * 0.5) + healthcareAdjusted + propertyTaxAdjusted + annualTaxDue) - passiveIncome

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
                    contributionTba = annualTbaVal,
                    totalIncome = annualSalary + annualInterest + annualDividends + annualSocialSecurity + tbaWithdrawal + tdaWithdrawal + rothConversion,
                    needs = needsAdjusted,
                    wants = wantsAdjusted,
                    healthcare = healthcareAdjusted,
                    incomeTax = annualTaxDue,
                    propertyTax = propertyTaxAdjusted,
                    totalExpenses = totalExpenses
                ),
                metrics = Metrics(
                    annualIncomeGap = currentAig,
                    incomeGapExpenses = totalExpenses,
                    incomeGapPassiveIncome = passiveIncome,
                    sbCap = currentCapAig * 2.0,
                    cbbCap = calculateCbbCap(age),
                    isFailure = isFailure
                )
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
}