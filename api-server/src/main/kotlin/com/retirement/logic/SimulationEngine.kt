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
        val marketHistory = mutableListOf(1.0) // Monthly or yearly? Spec says quarterly decisions.
        
        for (yearIdx in 0 until (endAge - config.currentAge + 1)) {
            val year = currentYear + yearIdx
            val age = currentAge + yearIdx
            
            // Inflation factor for this year (cumulative)
            // Spec says "inflation rate assumed to be static throughout 35 years" for single run.
            // But Monte Carlo uses variable. We'll use the list provided.
            val inflationFactor = if (yearIdx < inflationRates.size) (1.0 + inflationRates[yearIdx]) else (1.0 + config.rates.inflation)
            
            // Monthly processing
            var annualSalary = 0.0
            var annualInterest = 0.0
            var annualDividends = 0.0
            
            // Quarterly withdrawal trackers
            var tbaWithdrawal = 0.0
            var tdaWithdrawal = 0.0
            var rothConversion = 0.0
            
            // AIG calculation (Needs + Wants inflation adjusted)
            val baseExpenses = (config.expenses.needs + config.expenses.wants)
            val inflationAdjustment = (1.0 + config.rates.inflation).pow(yearIdx)
            val currentAig = baseExpenses * inflationAdjustment // Simplified: recurring income not yet subtracted
            
            for (month in 1..12) {
                // 1. Salary (pre-retirement)
                if (age < config.retirementAge) {
                    val monthlySalary = (120000.0 / 12.0) * inflationAdjustment // Placeholder salary logic
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
                        // Market data (Placeholder for now)
                        val marketRet = if (yearIdx < marketReturns.size) marketReturns[yearIdx] else config.rates.postRetirementGrowth
                        val qMarketRet = (1.0 + marketRet).pow(0.25) - 1.0
                        currentMarketValue *= (1.0 + qMarketRet)
                        allTimeHigh = maxOf(allTimeHigh, currentMarketValue)
                        
                        val spendingResult = SpendingStrategy.executeQuarterly(
                            month / 3,
                            config,
                            balances,
                            currentAig, // Simplified AIG
                            1.0, // marketPerformance placeholder
                            currentMarketValue / allTimeHigh,
                            1.0 // cbbPerformance placeholder
                        )
                        balances = spendingResult.portfolio
                        
                        tbaWithdrawal += spendingResult.tbaWithdrawal
                        tdaWithdrawal += spendingResult.tdaWithdrawal
                        // TODO: Implement Roth Conversion logic in SpendingStrategy if needed, or track separate TDA distribution types
                        
                        if (spendingResult.shortfall > 0) {
                            // mark failure if shortfall exists?
                        }
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
            val healthcareBase = if (age < 65) config.expenses.healthcarePreMedicare else config.expenses.healthcareMedicare
            val healthcareAdjusted = healthcareBase * inflationAdjustment
            val needsAdjusted = config.expenses.needs * inflationAdjustment
            val wantsAdjusted = config.expenses.wants * inflationAdjustment
            val propertyTaxAdjusted = config.expenses.propertyTax * inflationAdjustment
            
            yearlyResults.add(YearlyResult(
                year = year,
                age = age,
                balances = balances,
                cashFlow = CashFlow(
                    salary = annualSalary,
                    interest = annualInterest,
                    dividends = annualDividends,
                    socialSecurity = 0.0, // TODO
                    tbaWithdrawal = tbaWithdrawal,
                    tdaWithdrawal = tdaWithdrawal,
                    rothConversion = rothConversion,
                    totalIncome = annualSalary + annualInterest + annualDividends + tbaWithdrawal + tdaWithdrawal + rothConversion,
                    needs = needsAdjusted,
                    wants = wantsAdjusted,
                    healthcare = healthcareAdjusted,
                    incomeTax = 0.0,
                    propertyTax = propertyTaxAdjusted,
                    totalExpenses = needsAdjusted + wantsAdjusted + healthcareAdjusted + propertyTaxAdjusted
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
