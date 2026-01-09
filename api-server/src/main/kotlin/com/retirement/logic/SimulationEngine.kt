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
        
        var totalDividends = 0.0
        var totalInterest = 0.0
        
        var isFailure = false
        var failureYear: Int? = null

        // Tracking ATH for S&P (normalized to 1.0 at start)
        var currentMarketValue = 1.0
        var allTimeHigh = 1.0
        
        for (yearIdx in 0 until (endAge - config.currentAge + 1)) {
            val year = currentYear + yearIdx
            val age = currentAge + yearIdx
            
            // Inflation factor for this year (cumulative)
            val inflationAdjustment = (1.0 + config.rates.inflation).pow(yearIdx)
            
            // Calculate inflated expenses
            val healthcareBase = if (age < 65) config.expenses.healthcarePreMedicare else config.expenses.healthcareMedicare
            val healthcareAdjusted = healthcareBase * inflationAdjustment
            val needsAdjusted = config.expenses.needs * inflationAdjustment
            val wantsAdjusted = config.expenses.wants * inflationAdjustment
            val propertyTaxAdjusted = config.expenses.propertyTax * inflationAdjustment
            
            val grossExpenses = needsAdjusted + wantsAdjusted + healthcareAdjusted + propertyTaxAdjusted
            
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
            
            // Estimate Interest and Dividends for AIG calculation (recurring income)
            val estimatedInterest = balances.sb * config.rates.hysaRate
            val estimatedDividends = balances.cbb * config.rates.bondYield
            
            // Determine Annual Salary for this year
            val annualSalaryVal = if (age < config.retirementAge) config.salary * inflationAdjustment else 0.0
            
            // Estimate Income Tax on Salary (simplification: Flat Rate on Salary)
            val estimatedTaxOnSalary = annualSalaryVal * config.rates.incomeTax
            
            // AIG = (Gross Expenses + Tax on Salary) - (Recurring Income + Salary)
            val currentAig = maxOf(0.0, (grossExpenses + estimatedTaxOnSalary) - (annualSocialSecurity + estimatedInterest + estimatedDividends + annualSalaryVal))
            
            // Track annual flows
            var annualSalary = 0.0
            var annualInterest = 0.0
            var annualDividends = 0.0
            var tbaWithdrawal = 0.0
            var tdaWithdrawal = 0.0
            var rothConversion = 0.0

            for (month in 1..12) {
                // 1. Salary (pre-retirement)
                if (age < config.retirementAge) {
                    val monthlySalary = annualSalaryVal / 12.0
                    balances = balances.copy(sb = balances.sb + monthlySalary)
                    annualSalary += monthlySalary
                }

                // 2. Interest (Monthly)
                val monthlyInterest = balances.sb * (config.rates.hysaRate / 12.0)
                balances = balances.copy(sb = balances.sb + monthlyInterest)
                annualInterest += monthlyInterest
                totalInterest += monthlyInterest

                // 3. Quarterly Events
                if (month % 3 == 0) {
                    // Dividends from CBB
                    val quarterlyDividend = balances.cbb * (config.rates.bondYield / 4.0)
                    balances = balances.copy(sb = balances.sb + quarterlyDividend)
                    annualDividends += quarterlyDividend
                    totalDividends += quarterlyDividend

                    // Spending Strategy Withdrawal
                    if (age >= config.retirementAge) {
                        // Market data
                        val marketRet = if (yearIdx < marketReturns.size) marketReturns[yearIdx] else config.rates.postRetirementGrowth
                        val qMarketRet = (1.0 + marketRet).pow(0.25) - 1.0
                        currentMarketValue *= (1.0 + qMarketRet)
                        allTimeHigh = maxOf(allTimeHigh, currentMarketValue)
                        
                        val spendingResult = SpendingStrategy.executeQuarterly(
                            month / 3,
                            config,
                            balances,
                            currentAig, 
                            1.0, // marketPerformance placeholder
                            currentMarketValue / allTimeHigh,
                            1.0, // cbbPerformance placeholder
                            inflationAdjustment
                        )
                        balances = spendingResult.portfolio
                        
                        tbaWithdrawal += spendingResult.tbaWithdrawal
                        tdaWithdrawal += spendingResult.tdaWithdrawal
                    }
                }

                // 4. Monthly Equity Growth
                val equityGrowth = if (age < config.retirementAge) config.rates.preRetirementGrowth else config.rates.postRetirementGrowth
                val monthlyGrowth = (1.0 + equityGrowth).pow(1.0/12.0) - 1.0
                balances = balances.copy(
                    tba = balances.tba * (1.0 + monthlyGrowth),
                    tda = balances.tda * (1.0 + monthlyGrowth),
                    tfa = balances.tfa * (1.0 + monthlyGrowth)
                )

                // Check for failure
                if (balances.sb < 0 || balances.cbb < 0 || balances.tba < 0 || balances.tda < 0 || balances.tfa < 0) {
                    isFailure = true
                    if (failureYear == null) failureYear = year
                }
            }

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
                    incomeTax = estimatedTaxOnSalary,
                    propertyTax = propertyTaxAdjusted,
                    totalExpenses = needsAdjusted + wantsAdjusted + healthcareAdjusted + propertyTaxAdjusted + estimatedTaxOnSalary
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