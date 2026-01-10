package com.retirement.logic

import com.retirement.model.*
import kotlin.math.pow

object SimulationEngine {
    fun runSimulation(config: SimulationConfig, marketReturns: List<Double>, inflationRates: List<Double>): SimulationResult {
        var currentYear = config.currentYear
        var currentAge = config.currentAge
        val endAge = 85
        
        var balances = config.portfolio
        
        // ADDED: Interest and Dividend from the starting year are added to the next year balances (start of simulation)
        val startYearInterest = balances.sb * config.rates.hysaRate
        val startYearDividends = balances.cbb * config.rates.bondYield
        balances = balances.copy(sb = balances.sb + startYearInterest + startYearDividends)

        val yearlyResults = mutableListOf<YearlyResult>()
        val quarterlyResults = mutableListOf<QuarterlyResult>()
        
        var totalDividends = 0.0
        var totalInterest = 0.0
        
        var isFailure = false
        var failureYear: Int? = null

                // Tracking ATH for S&P (normalized to 1.0 at start)
                var currentMarketValue = 1.0
                var allTimeHigh = 1.0
                
                var accruedInterest = 0.0
        var accruedDividends = 0.0
                
                // CHANGED: Start simulation from Year 1 (Next Year)
                val duration = endAge - config.currentAge
                for (yearIdx in 1..duration) {
                    val year = currentYear + yearIdx
                    val age = currentAge + yearIdx
                    
                    // Inflation factor for this year (cumulative)
                    val inflationAdjustment = (1.0 + config.rates.inflation).pow(yearIdx)
                    
                    // Calculate inflated expenses components
                    val healthcareBase = if (age < 65) config.expenses.healthcarePreMedicare else config.expenses.healthcareMedicare
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
        
                    // Determine Annual Salary for this year
                    val annualSalaryVal = if (age < config.retirementAge) config.salary * inflationAdjustment else 0.0
                    
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
                    
                    // Roth Conversion Logic (Inflated)
                    val annualRothConversion = if (age >= config.retirementAge) config.strategy.rothConversionAmount * inflationAdjustment else 0.0
        
                    // Estimate Tax for Wants Calculation / AIG
                    val estimatedTaxableIncomePre = annualSalaryVal + annualSocialSecurity + estimatedInterest + estimatedDividends + annualRothConversion
                    val estimatedTaxOnKnownIncome = estimatedTaxableIncomePre * config.rates.incomeTax
        
                    // ADDED: Pre-Retirement Wants Adjustment
                    // Before retirement, wants expenses is adjusted so that total expenses are covered entirely by salary.
                    // This means Interest and Dividends are effectively saved.
                    // Wants = Salary - (Needs + Healthcare + PropTax + Tax + Roth)
                    var wantsAdjusted = config.expenses.wants * inflationAdjustment
                    if (age < config.retirementAge) {
                        val fixedOutflows = needsAdjusted + healthcareAdjusted + propertyTaxAdjusted + estimatedTaxOnKnownIncome + annualRothConversion
                        // We only use Salary to cover expenses. Interest/Dividends are saved.
                        val availableForWants = annualSalaryVal - fixedOutflows
                        wantsAdjusted = maxOf(0.0, availableForWants)
                    }
        
                    val grossExpenses = needsAdjusted + wantsAdjusted + healthcareAdjusted + propertyTaxAdjusted
        
                    // AIG Calculation
                    // AIG = (Gross Expenses + Tax on Known Income + Roth Conversion) - (Recurring Income + Salary)
                    val rawGap = maxOf(0.0, (grossExpenses + estimatedTaxOnKnownIncome + annualRothConversion) - (annualSocialSecurity + estimatedInterest + estimatedDividends + annualSalaryVal))
                    val taxDivisor = maxOf(0.01, 1.0 - config.rates.incomeTax)
                    val currentAig = rawGap / taxDivisor
                    
                    // Track annual flows
                    var annualSalary = 0.0
                    var annualInterest = 0.0
                    var annualDividends = 0.0
                    var tbaWithdrawal = 0.0
                    var tdaWithdrawal = 0.0
                                var rothConversion = 0.0
                                
                                // Quarterly accumulators
                                var qSalary = 0.0
                                var qInterest = 0.0
                                var qDividends = 0.0
                                var qSS = 0.0
                                var qTbaW = 0.0
                                var qTdaW = 0.0
                                var qRoth = 0.0
                                var qNeeds = 0.0
                                var qWants = 0.0
                                var qHealth = 0.0
                                var qTax = 0.0
                                var qProp = 0.0
                    
                                for (month in 1..12) {                // 0. Quarterly Events (Start of Quarter - Day 1)
                if ((month - 1) % 3 == 0) {
                    // Credit Accrued Interest from previous 3 months
                    balances = balances.copy(sb = balances.sb + accruedInterest)
                    annualInterest += accruedInterest
                    totalInterest += accruedInterest
                    qInterest += accruedInterest
                    accruedInterest = 0.0

                    // Credit Accrued Dividends from previous 3 months
                    balances = balances.copy(sb = balances.sb + accruedDividends)
                    annualDividends += accruedDividends
                    totalDividends += accruedDividends
                    qDividends += accruedDividends
                    accruedDividends = 0.0

                    // Spending Strategy Withdrawal
                    if (age >= config.retirementAge) {
                        val spendingResult = SpendingStrategy.executeQuarterly(
                            (month - 1) / 3,
                            config,
                            balances,
                            currentAig, 
                            1.0, // marketPerformance placeholder
                            currentMarketValue / allTimeHigh,
                            1.0, // cbbPerformance placeholder
                            inflationAdjustment
                        )
                        balances = spendingResult.portfolio
                        
                        // Roth Conversion logic: Move money from SB to TFA (Quarterly)
                        // The SpendingStrategy already pulled enough for AIG which includes Roth conversion
                        val quarterlyRoth = annualRothConversion / 4.0
                        if (balances.sb >= quarterlyRoth) {
                            balances = balances.copy(
                                sb = balances.sb - quarterlyRoth,
                                tfa = balances.tfa + quarterlyRoth
                            )
                            rothConversion += quarterlyRoth
                            qRoth += quarterlyRoth
                        }

                        tbaWithdrawal += spendingResult.tbaWithdrawal
                        tdaWithdrawal += spendingResult.tdaWithdrawal
                        qTbaW += spendingResult.tbaWithdrawal
                        qTdaW += spendingResult.tdaWithdrawal
                    }
                }

                // 1. Salary (pre-retirement)
                if (age < config.retirementAge) {
                    val monthlySalary = annualSalaryVal / 12.0
                    balances = balances.copy(sb = balances.sb + monthlySalary)
                    annualSalary += monthlySalary
                    qSalary += monthlySalary
                }

                // Add Monthly Social Security
                val monthlySS = annualSocialSecurity / 12.0
                balances = balances.copy(sb = balances.sb + monthlySS)
                qSS += monthlySS

                // SUBTRACT Monthly Expenses and Estimated Tax
                // This prevents the SB from growing indefinitely
                val monthlyExpenses = grossExpenses / 12.0
                val monthlyEstimatedTax = estimatedTaxOnKnownIncome / 12.0
                balances = balances.copy(sb = balances.sb - monthlyExpenses - monthlyEstimatedTax)
                
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
                    quarterlyResults.add(QuarterlyResult(
                        year = year,
                        quarter = month / 3,
                        age = age,
                        balances = balances,
                        cashFlow = CashFlow(
                            salary = qSalary,
                            interest = qInterest,
                            dividends = qDividends,
                            socialSecurity = qSS,
                            tbaWithdrawal = qTbaW,
                            tdaWithdrawal = qTdaW,
                            rothConversion = qRoth,
                            totalIncome = qSalary + qInterest + qDividends + qSS + qTbaW + qTdaW + qRoth,
                            needs = qNeeds,
                            wants = qWants,
                            healthcare = qHealth,
                            incomeTax = qTax,
                            propertyTax = qProp,
                            totalExpenses = qNeeds + qWants + qHealth + qTax + qProp
                        ),
                        metrics = Metrics(currentAig, isFailure)
                    ))
                    
                    // Reset Accumulators
                    qSalary = 0.0
                    qInterest = 0.0
                    qDividends = 0.0
                    qSS = 0.0
                    qTbaW = 0.0
                    qTdaW = 0.0
                    qRoth = 0.0
                    qNeeds = 0.0
                    qWants = 0.0
                    qHealth = 0.0
                    qTax = 0.0
                    qProp = 0.0
                }
            }

            // Calculate Final Income Tax
            // Includes Salary, Interest, Dividends, Social Security, TDA withdrawals, Roth Conversions, and 50% of TBA withdrawals
            val totalTaxableIncome = annualSalary + annualInterest + annualDividends + annualSocialSecurity + tdaWithdrawal + rothConversion + (tbaWithdrawal * 0.5)
            val finalIncomeTax = totalTaxableIncome * config.rates.incomeTax

            // Adjust SB for tax difference (True-up)
            val taxDifference = finalIncomeTax - estimatedTaxOnKnownIncome
            balances = balances.copy(sb = balances.sb - taxDifference)

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
                    rothConversion = rothConversion,
                    totalIncome = annualSalary + annualInterest + annualDividends + annualSocialSecurity + tbaWithdrawal + tdaWithdrawal + rothConversion,
                    needs = needsAdjusted,
                    wants = wantsAdjusted,
                    healthcare = healthcareAdjusted,
                    incomeTax = finalIncomeTax,
                    propertyTax = propertyTaxAdjusted,
                    totalExpenses = needsAdjusted + wantsAdjusted + healthcareAdjusted + propertyTaxAdjusted + finalIncomeTax
                ),
                metrics = Metrics(
                    annualIncomeGap = currentAig,
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
            )
        )
    }
}