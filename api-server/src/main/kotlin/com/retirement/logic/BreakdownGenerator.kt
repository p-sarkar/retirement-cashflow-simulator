package com.retirement.logic

import com.retirement.model.*
import kotlin.math.pow

object BreakdownGenerator {

    fun generateBreakdown(
        config: SimulationConfig,
        targetAge: Int,
        yearlyResult: YearlyResult,
        priorYearResult: YearlyResult?
    ): ComputationBreakdown {
        val yearIdx = targetAge - config.currentAge
        val inflationAdjustment = (1.0 + config.rates.inflation).pow(yearIdx)

        val sections = mutableListOf<BreakdownSection>()

        // Section 1: Inflation Adjustment
        sections.add(createInflationSection(config, yearIdx, inflationAdjustment))

        // Section 2: Account Balances
        sections.add(createBalancesSection(yearlyResult, priorYearResult))

        // Section 3: Expenses
        sections.add(createExpensesSection(config, yearlyResult, inflationAdjustment, targetAge))

        // Section 4: Income Sources
        sections.add(createIncomeSection(config, yearlyResult, inflationAdjustment, targetAge))

        // Section 5: Income Gap Calculation
        sections.add(createIncomeGapSection(yearlyResult))

        // Section 6: Taxable Income Calculation
        sections.add(createTaxableIncomeSection(config, yearlyResult, priorYearResult))

        // Section 7: Spending Strategy (if post-retirement)
        if (targetAge > config.retirementAge) {
            sections.add(createSpendingStrategySection(config, yearlyResult, inflationAdjustment))
        }

        // Section 8: SB Operations
        sections.add(createSBOperationsSection(config, yearlyResult, targetAge, priorYearResult))

        // Section 9: Caps & Thresholds
        sections.add(createCapsSection(yearlyResult, targetAge))

        return ComputationBreakdown(
            year = yearlyResult.year,
            age = targetAge,
            sections = sections,
            apiMetadata = com.retirement.util.ServerInfo.getMetadata()
        )
    }

    private fun createInflationSection(
        config: SimulationConfig,
        yearIdx: Int,
        inflationAdjustment: Double
    ): BreakdownSection {
        val steps = listOf(
            ComputationStep(
                label = "Inflation Rate",
                formula = "config.rates.inflation",
                values = mapOf("inflation" to config.rates.inflation),
                result = config.rates.inflation,
                explanation = "Annual inflation rate used for all adjustments"
            ),
            ComputationStep(
                label = "Years from Start",
                formula = "targetAge - currentAge",
                values = mapOf("yearIdx" to yearIdx.toDouble()),
                result = yearIdx.toDouble(),
                explanation = "Number of years since simulation start"
            ),
            ComputationStep(
                label = "Cumulative Inflation Factor",
                formula = "(1 + inflation)^years",
                values = mapOf("inflation" to config.rates.inflation, "years" to yearIdx.toDouble()),
                result = inflationAdjustment,
                explanation = "Multiplier applied to all dollar amounts to adjust for inflation"
            )
        )
        return BreakdownSection("Inflation Adjustment", steps)
    }

    private fun createBalancesSection(
        result: YearlyResult,
        priorResult: YearlyResult?
    ): BreakdownSection {
        val steps = mutableListOf<ComputationStep>()

        if (priorResult != null) {
            steps.add(ComputationStep(
                label = "Prior Year Spend Bucket",
                formula = "priorYear.balances.sb",
                values = mapOf("priorSB" to priorResult.balances.sb),
                result = priorResult.balances.sb,
                explanation = "SB balance at end of prior year"
            ))
        }

        steps.addAll(listOf(
            ComputationStep(
                label = "Spend Bucket (SB)",
                formula = "endOfYear.balances.sb",
                values = mapOf("sb" to result.balances.sb),
                result = result.balances.sb,
                explanation = "HYSA account for covering expenses. Earns interest monthly."
            ),
            ComputationStep(
                label = "Crash Buffer Bucket (CBB)",
                formula = "endOfYear.balances.cbb",
                values = mapOf("cbb" to result.balances.cbb),
                result = result.balances.cbb,
                explanation = "Bond portfolio buffer. Used when markets are down."
            ),
            ComputationStep(
                label = "Taxable Brokerage (TBA)",
                formula = "endOfYear.balances.tba",
                values = mapOf("tba" to result.balances.tba),
                result = result.balances.tba,
                explanation = "Taxable investment account with equities."
            ),
            ComputationStep(
                label = "Tax-Deferred Account (TDA)",
                formula = "endOfYear.balances.tda",
                values = mapOf("tda" to result.balances.tda),
                result = result.balances.tda,
                explanation = "Traditional 401k/IRA. Withdrawals are taxable income."
            ),
            ComputationStep(
                label = "Tax-Free Account (TFA)",
                formula = "endOfYear.balances.tfa",
                values = mapOf("tfa" to result.balances.tfa),
                result = result.balances.tfa,
                explanation = "Roth IRA/401k. Qualified withdrawals are tax-free."
            ),
            ComputationStep(
                label = "Total Portfolio",
                formula = "SB + CBB + TBA + TDA + TFA",
                values = mapOf(
                    "sb" to result.balances.sb,
                    "cbb" to result.balances.cbb,
                    "tba" to result.balances.tba,
                    "tda" to result.balances.tda,
                    "tfa" to result.balances.tfa
                ),
                result = result.balances.sb + result.balances.cbb + result.balances.tba + result.balances.tda + result.balances.tfa,
                explanation = "Sum of all account balances"
            )
        ))

        return BreakdownSection("Account Balances (End of Year)", steps)
    }

    private fun createExpensesSection(
        config: SimulationConfig,
        result: YearlyResult,
        inflationAdjustment: Double,
        age: Int
    ): BreakdownSection {
        val healthcareType = when {
            age < config.retirementAge -> "Pre-Retirement"
            age < 65 -> "Post-Retirement Pre-Medicare"
            else -> "Medicare (65+)"
        }

        val steps = mutableListOf(
            ComputationStep(
                label = "Needs (Essential Expenses)",
                formula = "baseNeeds × inflationFactor",
                values = mapOf("baseNeeds" to config.expenses.needs, "inflationFactor" to inflationAdjustment),
                result = result.cashFlow.needs,
                explanation = "Essential living expenses adjusted for inflation"
            )
        )

        // Detailed Wants breakdown
        if (age < config.retirementAge) {
            // Pre-retirement: Wants = Salary - Fixed Outflows
            val salaryAdjusted = config.salary * inflationAdjustment
            val needsAdjusted = config.expenses.needs * inflationAdjustment
            val healthcareAdjusted = result.cashFlow.healthcare
            val propertyTaxAdjusted = config.expenses.propertyTax * inflationAdjustment
            val annual401k = config.contributions.annual401k * inflationAdjustment
            val annualTba = config.contributions.annualTba * inflationAdjustment
            val rothConv = config.strategy.rothConversionAmount * inflationAdjustment
            val fixedOutflows = needsAdjusted + healthcareAdjusted + propertyTaxAdjusted + result.cashFlow.incomeTax + rothConv + annual401k + annualTba

            steps.add(ComputationStep(
                label = "Wants Calculation (Pre-Retirement)",
                formula = "Salary - FixedOutflows",
                values = mapOf(
                    "salaryAdjusted" to salaryAdjusted,
                    "needsAdjusted" to needsAdjusted,
                    "healthcareAdjusted" to healthcareAdjusted,
                    "propertyTaxAdjusted" to propertyTaxAdjusted,
                    "incomeTax" to result.cashFlow.incomeTax,
                    "rothConversion" to rothConv,
                    "annual401k" to annual401k,
                    "annualTba" to annualTba,
                    "fixedOutflows" to fixedOutflows
                ),
                result = result.cashFlow.wants,
                explanation = "Pre-retirement: Wants = Salary - (Needs + Healthcare + PropertyTax + Tax + RothConv + 401k + TBA). This ensures salary exactly covers expenses, saving all investment income."
            ))
        } else {
            // Post-retirement: Wants = baseWants × inflationFactor
            steps.add(ComputationStep(
                label = "Wants (Discretionary)",
                formula = "baseWants × inflationFactor",
                values = mapOf("baseWants" to config.expenses.wants, "inflationFactor" to inflationAdjustment),
                result = result.cashFlow.wants,
                explanation = "Post-retirement: Discretionary spending adjusted for inflation"
            ))
        }

        steps.add(ComputationStep(
            label = "Healthcare ($healthcareType)",
            formula = "baseHealthcare × inflationFactor",
            values = mapOf("inflationFactor" to inflationAdjustment),
            result = result.cashFlow.healthcare,
            explanation = "Healthcare costs based on age bracket: $healthcareType"
        ))
        steps.add(ComputationStep(
            label = "Property Tax",
            formula = "basePropertyTax × inflationFactor",
            values = mapOf("basePropertyTax" to config.expenses.propertyTax, "inflationFactor" to inflationAdjustment),
            result = result.cashFlow.propertyTax,
            explanation = "Annual property tax adjusted for inflation"
        ))
        steps.add(ComputationStep(
            label = "Income Tax",
            formula = "priorYearTaxableIncome × effectiveTaxRate",
            values = mapOf("taxRate" to config.rates.incomeTax),
            result = result.cashFlow.incomeTax,
            explanation = "Tax on prior year's taxable income at effective rate"
        ))
        steps.add(ComputationStep(
            label = "Total Expenses",
            formula = "Needs + Wants + Healthcare + PropertyTax + IncomeTax",
            values = mapOf(
                "needs" to result.cashFlow.needs,
                "wants" to result.cashFlow.wants,
                "healthcare" to result.cashFlow.healthcare,
                "propertyTax" to result.cashFlow.propertyTax,
                "incomeTax" to result.cashFlow.incomeTax
            ),
            result = result.cashFlow.totalExpenses,
            explanation = "Sum of all expense categories"
        ))

        return BreakdownSection("Expenses", steps)
    }

    private fun createIncomeSection(
        config: SimulationConfig,
        result: YearlyResult,
        inflationAdjustment: Double,
        age: Int
    ): BreakdownSection {
        val steps = mutableListOf<ComputationStep>()

        if (age <= config.retirementAge) {
            steps.add(ComputationStep(
                label = "Salary",
                formula = "baseSalary × inflationFactor",
                values = mapOf("baseSalary" to config.salary, "inflationFactor" to inflationAdjustment),
                result = result.cashFlow.salary,
                explanation = "Annual salary adjusted for inflation (until retirement)"
            ))
        }

        steps.addAll(listOf(
            ComputationStep(
                label = "Interest (from SB)",
                formula = "SB_balance × HYSA_rate (accrued monthly, credited quarterly)",
                values = mapOf("hysaRate" to config.rates.hysaRate),
                result = result.cashFlow.interest,
                explanation = "Interest earned on Spend Bucket HYSA balance"
            ),
            ComputationStep(
                label = "Dividends (from CBB)",
                formula = "CBB_balance × bondYield (accrued monthly, credited quarterly)",
                values = mapOf("bondYield" to config.rates.bondYield),
                result = result.cashFlow.dividends,
                explanation = "Dividends from Crash Buffer bond portfolio"
            ),
            ComputationStep(
                label = "Social Security",
                formula = "lowerEarnerBenefit + higherEarnerBenefit (if claiming age reached)",
                values = mapOf(),
                result = result.cashFlow.socialSecurity,
                explanation = "Combined SS benefits based on claim ages and spousal step-up rules"
            ),
            ComputationStep(
                label = "TBA Withdrawal",
                formula = "Quarterly withdrawals from Taxable Brokerage",
                values = mapOf(),
                result = result.cashFlow.tbaWithdrawal,
                explanation = "Stock sales from taxable account to refill SB/CBB"
            ),
            ComputationStep(
                label = "TDA Withdrawal (Total)",
                formula = "TDA for Spending + TDA for Roth",
                values = mapOf(
                    "tdaTotal" to result.cashFlow.tdaWithdrawal,
                    "tdaSpend" to result.cashFlow.tdaWithdrawalSpend,
                    "tdaRoth" to result.cashFlow.tdaWithdrawalRoth
                ),
                result = result.cashFlow.tdaWithdrawal,
                explanation = "Total TDA withdrawals: ${String.format("%.2f", result.cashFlow.tdaWithdrawalSpend)} for spending (→SB via spending strategy) + ${String.format("%.2f", result.cashFlow.tdaWithdrawalRoth)} for Roth conversion (→TFA direct)"
            ),
            ComputationStep(
                label = "Roth Conversion",
                formula = "rothConversionAmount × inflationFactor (starts Year 2)",
                values = mapOf("baseRothConv" to config.strategy.rothConversionAmount),
                result = result.cashFlow.rothConversion,
                explanation = "Annual TDA to TFA conversion. Begins from Year 2 of simulation. Direct TDA→TFA withdrawal, separate from spending strategy, does not flow through SB."
            ),
            ComputationStep(
                label = "Total Income",
                formula = "Salary + Interest + Dividends + SS + TBA_W + TDA_W + RothConv",
                values = mapOf(),
                result = result.cashFlow.totalIncome,
                explanation = "Sum of all income sources"
            )
        ))

        return BreakdownSection("Income Sources", steps)
    }

    private fun createIncomeGapSection(result: YearlyResult): BreakdownSection {
        val steps = listOf(
            ComputationStep(
                label = "Gap Expenses (Total for Gap)",
                formula = "Needs + Wants + Healthcare + PropertyTax + IncomeTax",
                values = mapOf(),
                result = result.metrics.incomeGapExpenses,
                explanation = "Total expenses that need to be covered"
            ),
            ComputationStep(
                label = "Passive Income",
                formula = "Interest + Dividends + SocialSecurity",
                values = mapOf(
                    "interest" to result.cashFlow.interest,
                    "dividends" to result.cashFlow.dividends,
                    "socialSecurity" to result.cashFlow.socialSecurity
                ),
                result = result.metrics.incomeGapPassiveIncome,
                explanation = "Income that doesn't require selling assets"
            ),
            ComputationStep(
                label = "Annual Income Gap (AIG)",
                formula = "GapExpenses - PassiveIncome",
                values = mapOf(
                    "gapExpenses" to result.metrics.incomeGapExpenses,
                    "passiveIncome" to result.metrics.incomeGapPassiveIncome
                ),
                result = result.metrics.annualIncomeGap,
                explanation = "Amount that must be covered by withdrawals from investment accounts"
            )
        )

        return BreakdownSection("Income Gap Calculation", steps)
    }

    private fun createTaxableIncomeSection(
        config: SimulationConfig,
        result: YearlyResult,
        priorYearResult: YearlyResult?
    ): BreakdownSection {
        val steps = mutableListOf<ComputationStep>()

        // Calculate taxable income components for THIS year (will affect NEXT year's tax)
        val salaryTaxable = result.cashFlow.salary
        val interestTaxable = result.cashFlow.interest
        val dividendsTaxable = result.cashFlow.dividends
        val ssTaxable = result.cashFlow.socialSecurity // Simplified: 100% taxable
        val tdaWithdrawalTaxable = result.cashFlow.tdaWithdrawal
        val rothConversionTaxable = result.cashFlow.rothConversion
        val tbaWithdrawalTaxable = result.cashFlow.tbaWithdrawal * 0.5 // 50% taxable (cost basis)

        val totalTaxableIncome = salaryTaxable + interestTaxable + dividendsTaxable +
                                 ssTaxable + tdaWithdrawalTaxable + rothConversionTaxable + tbaWithdrawalTaxable

        val estimatedTax = totalTaxableIncome * config.rates.incomeTax

        steps.add(ComputationStep(
            label = "Salary (100% taxable)",
            formula = "salary × 1.0",
            values = mapOf("salary" to result.cashFlow.salary),
            result = salaryTaxable,
            explanation = "Full salary is taxable income"
        ))

        steps.add(ComputationStep(
            label = "Interest (100% taxable)",
            formula = "interest × 1.0",
            values = mapOf("interest" to result.cashFlow.interest),
            result = interestTaxable,
            explanation = "HYSA interest is ordinary income"
        ))

        steps.add(ComputationStep(
            label = "Dividends (100% taxable)",
            formula = "dividends × 1.0",
            values = mapOf("dividends" to result.cashFlow.dividends),
            result = dividendsTaxable,
            explanation = "Bond dividends are ordinary income"
        ))

        steps.add(ComputationStep(
            label = "Social Security (100% taxable)",
            formula = "socialSecurity × 1.0 (simplified)",
            values = mapOf("socialSecurity" to result.cashFlow.socialSecurity),
            result = ssTaxable,
            explanation = "Simplified: assuming 100% of SS is taxable (actual varies 0-85%)"
        ))

        steps.add(ComputationStep(
            label = "TDA Withdrawal (100% taxable)",
            formula = "tdaWithdrawal × 1.0",
            values = mapOf("tdaWithdrawal" to result.cashFlow.tdaWithdrawal),
            result = tdaWithdrawalTaxable,
            explanation = "Traditional IRA/401k distributions are fully taxable"
        ))

        steps.add(ComputationStep(
            label = "Roth Conversion (100% taxable)",
            formula = "rothConversion × 1.0",
            values = mapOf("rothConversion" to result.cashFlow.rothConversion),
            result = rothConversionTaxable,
            explanation = "Roth conversions from TDA are taxable events"
        ))

        steps.add(ComputationStep(
            label = "TBA Withdrawal (50% taxable)",
            formula = "tbaWithdrawal × 0.5 (assuming 50% cost basis)",
            values = mapOf("tbaWithdrawal" to result.cashFlow.tbaWithdrawal, "taxableRate" to 0.5),
            result = tbaWithdrawalTaxable,
            explanation = "Only gains are taxable; assuming 50% cost basis"
        ))

        steps.add(ComputationStep(
            label = "Total Taxable Income (This Year)",
            formula = "Salary + Interest + Dividends + SS + TDA_W + RothConv + (TBA_W × 0.5)",
            values = mapOf(
                "salary" to salaryTaxable,
                "interest" to interestTaxable,
                "dividends" to dividendsTaxable,
                "socialSecurity" to ssTaxable,
                "tdaWithdrawal" to tdaWithdrawalTaxable,
                "rothConversion" to rothConversionTaxable,
                "tbaWithdrawal50pct" to tbaWithdrawalTaxable
            ),
            result = totalTaxableIncome,
            explanation = "Sum of all taxable income components"
        ))

        steps.add(ComputationStep(
            label = "Estimated Tax (Next Year)",
            formula = "totalTaxableIncome × effectiveTaxRate",
            values = mapOf("taxableIncome" to totalTaxableIncome, "taxRate" to config.rates.incomeTax),
            result = estimatedTax,
            explanation = "This year's income determines next year's tax bill"
        ))

        // Show prior year's taxable income that caused THIS year's tax
        if (priorYearResult != null) {
            val priorSalary = priorYearResult.cashFlow.salary
            val priorInterest = priorYearResult.cashFlow.interest
            val priorDividends = priorYearResult.cashFlow.dividends
            val priorSS = priorYearResult.cashFlow.socialSecurity
            val priorTda = priorYearResult.cashFlow.tdaWithdrawal
            val priorRoth = priorYearResult.cashFlow.rothConversion
            val priorTba = priorYearResult.cashFlow.tbaWithdrawal * 0.5
            val priorTotalTaxable = priorSalary + priorInterest + priorDividends + priorSS + priorTda + priorRoth + priorTba

            steps.add(ComputationStep(
                label = "Prior Year Taxable Income",
                formula = "Sum of prior year's taxable components",
                values = mapOf(
                    "priorSalary" to priorSalary,
                    "priorInterest" to priorInterest,
                    "priorDividends" to priorDividends,
                    "priorSS" to priorSS,
                    "priorTdaW" to priorTda,
                    "priorRothConv" to priorRoth,
                    "priorTbaW50pct" to priorTba
                ),
                result = priorTotalTaxable,
                explanation = "Prior year's taxable income that determines THIS year's tax"
            ))

            steps.add(ComputationStep(
                label = "This Year's Tax Bill (from Prior Year)",
                formula = "priorYearTaxableIncome × effectiveTaxRate",
                values = mapOf("priorTaxable" to priorTotalTaxable, "taxRate" to config.rates.incomeTax),
                result = result.cashFlow.incomeTax,
                explanation = "Tax paid this year based on prior year's income"
            ))
        } else {
            steps.add(ComputationStep(
                label = "This Year's Tax Bill",
                formula = "Based on estimated prior year income",
                values = mapOf(),
                result = result.cashFlow.incomeTax,
                explanation = "Tax for Year 1 is based on estimated Year 0 income"
            ))
        }

        return BreakdownSection("Taxable Income Calculation", steps)
    }

    private fun createSpendingStrategySection(
        config: SimulationConfig,
        result: YearlyResult,
        inflationAdjustment: Double
    ): BreakdownSection {
        val sbCap = result.metrics.sbCap
        val cbbCap = result.metrics.cbbCap
        val qAig = result.metrics.annualIncomeGap / 4.0

        // Calculate Cap AIG (uses 50% of wants, EXCLUSIVE of passive income)
        val capAig = result.cashFlow.needs + (result.cashFlow.wants * 0.5) + result.cashFlow.healthcare + result.cashFlow.propertyTax + result.cashFlow.incomeTax

        val steps = mutableListOf(
            ComputationStep(
                label = "Quarterly AIG (QAIG)",
                formula = "AIG / 4",
                values = mapOf("aig" to result.metrics.annualIncomeGap),
                result = qAig,
                explanation = "Quarterly withdrawal target for spending"
            ),
            ComputationStep(
                label = "Cap AIG (for SB/CBB caps)",
                formula = "Needs + 50% Wants + Healthcare + PropTax + Tax (EXCLUSIVE of passive income)",
                values = mapOf(
                    "needs" to result.cashFlow.needs,
                    "wants50pct" to (result.cashFlow.wants * 0.5),
                    "healthcare" to result.cashFlow.healthcare,
                    "propertyTax" to result.cashFlow.propertyTax,
                    "incomeTax" to result.cashFlow.incomeTax
                ),
                result = capAig,
                explanation = "Cap AIG uses 50% of Wants, EXCLUSIVE of passive income. Only expenses, no income subtraction."
            ),
            ComputationStep(
                label = "SB Cap",
                formula = "Cap AIG × 2",
                values = mapOf("capAig" to capAig),
                result = sbCap,
                explanation = "Maximum target balance for Spend Bucket (2 years of Cap AIG)"
            ),
            ComputationStep(
                label = "CBB Cap",
                formula = "Initial 7× Base Cap AIG, reduces by 1× at 65, 70, 75, 80, 85",
                values = mapOf("currentCbbCap" to cbbCap),
                result = cbbCap,
                explanation = "CBB Cap starts at 7× base Cap AIG (not inflation adjusted), reduces by 1× base Cap AIG at age 65 and every 5 years thereafter"
            ),
            ComputationStep(
                label = "Market Threshold (vs 12mo prior)",
                formula = "configurable, default 95%",
                values = mapOf("threshold" to 0.95),
                result = 0.95,
                explanation = "If market >= 95% of 12 months ago, withdrawals from equities are allowed"
            ),
            ComputationStep(
                label = "ATH Threshold",
                formula = "configurable, default 85%",
                values = mapOf("threshold" to 0.85),
                result = 0.85,
                explanation = "If market >= 85% of all-time high, withdrawals from equities are allowed"
            ),
            ComputationStep(
                label = "Quarterly TDA Withdrawal Target (QTDAW)",
                formula = "(initialTdaWithdrawal + rothConversionAmount) × inflationFactor / 4",
                values = mapOf(
                    "initialTdaW" to config.strategy.initialTdaWithdrawal,
                    "rothConv" to config.strategy.rothConversionAmount,
                    "inflationFactor" to inflationAdjustment
                ),
                result = (config.strategy.initialTdaWithdrawal + config.strategy.rothConversionAmount) * inflationAdjustment / 4.0,
                explanation = "Preferred quarterly withdrawal from TDA before using TBA"
            )
        )

        return BreakdownSection("Spending Strategy Parameters", steps)
    }

    private fun createSBOperationsSection(
        config: SimulationConfig,
        result: YearlyResult,
        targetAge: Int,
        priorYearResult: YearlyResult?
    ): BreakdownSection {
        val steps = mutableListOf<ComputationStep>()

        // Starting balance
        if (priorYearResult != null) {
            steps.add(ComputationStep(
                label = "SB Starting Balance (Beginning of Year)",
                formula = "Prior year ending SB balance",
                values = mapOf("priorYearSB" to priorYearResult.balances.sb),
                result = priorYearResult.balances.sb,
                explanation = "Spend Bucket balance at the beginning of this year (end of prior year)"
            ))
        } else {
            steps.add(ComputationStep(
                label = "SB Starting Balance (Beginning of Year)",
                formula = "Initial portfolio SB balance",
                values = mapOf("initialSB" to config.portfolio.sb),
                result = config.portfolio.sb,
                explanation = "Initial Spend Bucket balance from configuration"
            ))
        }

        steps.add(ComputationStep(
            label = "════════════════════════════════",
            formula = "DEPOSITS TO SB (Inflows)",
            values = mapOf(),
            result = 0.0,
            explanation = "The following items are deposited into the SB throughout the year"
        ))

        // SB Deposit breakdown - Salary with more detail
        val monthlySalaryGross = if (targetAge <= config.retirementAge) result.cashFlow.salary / 12.0 else 0.0
        val monthly401k = if (targetAge <= config.retirementAge) result.cashFlow.contribution401k / 12.0 else 0.0
        val monthlyTbaContrib = if (targetAge <= config.retirementAge) result.cashFlow.contributionTba / 12.0 else 0.0
        val monthlyNetSalary = monthlySalaryGross - monthly401k - monthlyTbaContrib
        val annualNetSalary = result.cashFlow.salary - result.cashFlow.contribution401k - result.cashFlow.contributionTba

        val numSalaryMonths = if (targetAge < config.retirementAge) 12.0 else if (targetAge == config.retirementAge) 12.0 else 0.0
        steps.add(ComputationStep(
            label = "SB Deposit: Salary (NET, after contributions)",
            formula = if (targetAge <= config.retirementAge) "(Gross Salary - 401k - TBA) / 12 × 12 months" else "No salary post-retirement",
            values = mapOf(
                "grossSalary" to result.cashFlow.salary,
                "contribution401k" to result.cashFlow.contribution401k,
                "contributionTba" to result.cashFlow.contributionTba,
                "netSalary" to annualNetSalary,
                "monthlyNet" to monthlyNetSalary
            ),
            result = annualNetSalary,
            explanation = if (targetAge <= config.retirementAge)
                "NET Salary to SB: Gross $${String.format("%,.2f", result.cashFlow.salary)} - 401k $${String.format("%,.2f", result.cashFlow.contribution401k)} - TBA $${String.format("%,.2f", result.cashFlow.contributionTba)} = $${String.format("%,.2f", annualNetSalary)}/year. Deposited monthly ($${String.format("%,.2f", monthlyNetSalary)}/month)."
                else "No salary income post-retirement"
        ))

        steps.add(ComputationStep(
            label = "SB Deposit: Interest (HYSA)",
            formula = "SB_balance × HYSA_rate, accrued monthly, credited quarterly",
            values = mapOf(
                "hysaRate" to config.rates.hysaRate,
                "annualInterest" to result.cashFlow.interest,
                "quarterlyCredit" to result.cashFlow.interest / 4.0,
                "monthlyAccrual" to result.cashFlow.interest / 12.0
            ),
            result = result.cashFlow.interest,
            explanation = "Interest: $${String.format("%,.2f", result.cashFlow.interest)}/year. Accrues at $${String.format("%,.2f", result.cashFlow.interest / 12.0)}/month, credited $${String.format("%,.2f", result.cashFlow.interest / 4.0)} per quarter on day 1 of Q1, Q2, Q3, Q4."
        ))

        steps.add(ComputationStep(
            label = "SB Deposit: Dividends (from CBB)",
            formula = "CBB_balance × bondYield, accrued monthly, credited quarterly",
            values = mapOf(
                "bondYield" to config.rates.bondYield,
                "annualDividends" to result.cashFlow.dividends,
                "quarterlyCredit" to result.cashFlow.dividends / 4.0,
                "monthlyAccrual" to result.cashFlow.dividends / 12.0
            ),
            result = result.cashFlow.dividends,
            explanation = "Dividends: $${String.format("%,.2f", result.cashFlow.dividends)}/year. Accrues at $${String.format("%,.2f", result.cashFlow.dividends / 12.0)}/month from CBB bonds, credited $${String.format("%,.2f", result.cashFlow.dividends / 4.0)} per quarter to SB on day 1 of Q1, Q2, Q3, Q4."
        ))

        val monthlySS = if (result.cashFlow.socialSecurity > 0) result.cashFlow.socialSecurity / 12.0 else 0.0
        steps.add(ComputationStep(
            label = "SB Deposit: Social Security",
            formula = "lowerEarnerBenefit + higherEarnerBenefit, deposited monthly",
            values = mapOf(
                "annualSS" to result.cashFlow.socialSecurity,
                "monthlySS" to monthlySS
            ),
            result = result.cashFlow.socialSecurity,
            explanation = if (result.cashFlow.socialSecurity > 0)
                "Social Security: $${String.format("%,.2f", monthlySS)}/month × 12 = $${String.format("%,.2f", result.cashFlow.socialSecurity)}/year. Deposited monthly if claiming age reached."
                else "No Social Security benefits yet (claiming age not reached)"
        ))

        if (targetAge > config.retirementAge) {
            val quarterlyTda = result.cashFlow.tdaWithdrawal / 4.0
            steps.add(ComputationStep(
                label = "SB Deposit: TDA Withdrawal (Quarterly)",
                formula = "Per spending strategy, withdrawn quarterly from TDA",
                values = mapOf(
                    "annualTdaWithdrawal" to result.cashFlow.tdaWithdrawal,
                    "quarterlyTda" to quarterlyTda
                ),
                result = result.cashFlow.tdaWithdrawal,
                explanation = "TDA withdrawals: $${String.format("%,.2f", quarterlyTda)}/quarter × 4 = $${String.format("%,.2f", result.cashFlow.tdaWithdrawal)}/year. Executed on 1st business day of each quarter per spending strategy."
            ))

            val quarterlyTba = result.cashFlow.tbaWithdrawal / 4.0
            steps.add(ComputationStep(
                label = "SB Deposit: TBA Withdrawal (Quarterly)",
                formula = "Per spending strategy, withdrawn quarterly from TBA after TDA limit",
                values = mapOf(
                    "annualTbaWithdrawal" to result.cashFlow.tbaWithdrawal,
                    "quarterlyTba" to quarterlyTba
                ),
                result = result.cashFlow.tbaWithdrawal,
                explanation = "TBA withdrawals: $${String.format("%,.2f", quarterlyTba)}/quarter × 4 = $${String.format("%,.2f", result.cashFlow.tbaWithdrawal)}/year. Executed after TDA limit reached, on 1st business day of each quarter."
            ))
        }

        // Calculate total deposits
        // For pre-retirement: Salary that goes to SB is NET (Gross - 401k - TBA contributions)
        val netSalaryToSB = result.cashFlow.salary - result.cashFlow.contribution401k - result.cashFlow.contributionTba
        val totalDepositsCalculated = netSalaryToSB + result.cashFlow.interest +
                           result.cashFlow.dividends + result.cashFlow.socialSecurity +
                           result.cashFlow.tdaWithdrawal + result.cashFlow.tbaWithdrawal

        // Show the net salary calculation for pre-retirement
        if (result.cashFlow.salary > 0 && (result.cashFlow.contribution401k > 0 || result.cashFlow.contributionTba > 0)) {
            steps.add(ComputationStep(
                label = "Net Salary to SB (Pre-Retirement)",
                formula = "Gross Salary - 401k Contribution - TBA Contribution",
                values = mapOf(
                    "grossSalary" to result.cashFlow.salary,
                    "contribution401k" to result.cashFlow.contribution401k,
                    "contributionTba" to result.cashFlow.contributionTba,
                    "netSalaryToSB" to netSalaryToSB
                ),
                result = netSalaryToSB,
                explanation = "Only NET salary goes to SB. 401k goes directly to TDA, TBA contribution goes directly to TBA (bypasses SB)."
            ))
        }

        steps.add(ComputationStep(
            label = "Total SB Deposits (Calculated)",
            formula = "NetSalary + Interest + Dividends + SS + TDA_W + TBA_W",
            values = mapOf(
                "netSalary" to netSalaryToSB,
                "interest" to result.cashFlow.interest,
                "dividends" to result.cashFlow.dividends,
                "socialSecurity" to result.cashFlow.socialSecurity,
                "tdaWithdrawal" to result.cashFlow.tdaWithdrawal,
                "tbaWithdrawal" to result.cashFlow.tbaWithdrawal,
                "calculatedTotal" to totalDepositsCalculated
            ),
            result = totalDepositsCalculated,
            explanation = "Sum of all deposit sources = $${String.format("%,.2f", totalDepositsCalculated)}"
        ))

        // Show actual sbDeposit and reconcile if different
        if (kotlin.math.abs(totalDepositsCalculated - result.cashFlow.sbDeposit) > 0.01) {
            steps.add(ComputationStep(
                label = "Actual SB Deposits (from simulation)",
                formula = "Actual deposits recorded in simulation",
                values = mapOf(
                    "actualDeposit" to result.cashFlow.sbDeposit,
                    "calculatedDeposit" to totalDepositsCalculated,
                    "difference" to (result.cashFlow.sbDeposit - totalDepositsCalculated)
                ),
                result = result.cashFlow.sbDeposit,
                explanation = "Note: Actual deposits ($${String.format("%,.2f", result.cashFlow.sbDeposit)}) differ from calculated ($${String.format("%,.2f", totalDepositsCalculated)}) by $${String.format("%,.2f", result.cashFlow.sbDeposit - totalDepositsCalculated)}. This may include other deposits not shown above."
            ))
        }

        // Withdrawals separator
        steps.add(ComputationStep(
            label = "════════════════════════════════",
            formula = "WITHDRAWALS FROM SB (Outflows)",
            values = mapOf(),
            result = 0.0,
            explanation = "The following items are withdrawn from the SB to cover expenses"
        ))

        // SB Withdrawal breakdown
        val monthlyGap = result.metrics.annualIncomeGap / 12.0
        steps.add(ComputationStep(
            label = "SB Withdrawal: Monthly Income Gap",
            formula = "AIG / 12 (per month)",
            values = mapOf(
                "annualIncomeGap" to result.metrics.annualIncomeGap,
                "monthlyGap" to monthlyGap
            ),
            result = monthlyGap,
            explanation = "Monthly withdrawal: $${String.format("%,.2f", monthlyGap)}/month to cover the income gap (expenses minus passive income). Withdrawn on 1st of each month."
        ))

        steps.add(ComputationStep(
            label = "Total SB Withdrawals (Annual)",
            formula = "monthlyIncomeGap × 12",
            values = mapOf(
                "monthlyGap" to result.metrics.annualIncomeGap / 12.0,
                "months" to 12.0
            ),
            result = result.cashFlow.sbWithdrawal,
            explanation = "Total annual withdrawal = Income Gap (since we withdraw only what passive income doesn't cover)"
        ))

        steps.add(ComputationStep(
            label = "SB Withdrawal Logic",
            formula = "Withdraw AIG, NOT total expenses",
            values = mapOf(
                "totalExpenses" to result.cashFlow.totalExpenses,
                "passiveIncome" to result.metrics.incomeGapPassiveIncome,
                "incomeGap" to result.metrics.annualIncomeGap
            ),
            result = result.cashFlow.sbWithdrawal,
            explanation = "We withdraw only the Income Gap because passive income (interest, dividends, SS) is already deposited into SB"
        ))

        steps.add(ComputationStep(
            label = "Net SB Change",
            formula = "SB_Deposits - SB_Withdrawals",
            values = mapOf(
                "deposits" to result.cashFlow.sbDeposit,
                "withdrawals" to result.cashFlow.sbWithdrawal
            ),
            result = result.cashFlow.sbDeposit - result.cashFlow.sbWithdrawal,
            explanation = "Net change in Spend Bucket balance for the year"
        ))

        // Ending balance and status
        if (priorYearResult != null) {
            val beginningBalance = priorYearResult.balances.sb
            val endingBalance = result.balances.sb
            steps.add(ComputationStep(
                label = "SB Ending Balance (End of Year)",
                formula = "BeginningBalance + Deposits - Withdrawals",
                values = mapOf(
                    "beginningBalance" to beginningBalance,
                    "deposits" to result.cashFlow.sbDeposit,
                    "withdrawals" to result.cashFlow.sbWithdrawal,
                    "netChange" to (result.cashFlow.sbDeposit - result.cashFlow.sbWithdrawal)
                ),
                result = endingBalance,
                explanation = "Spend Bucket balance at end of year = Start + Deposits - Withdrawals"
            ))

            val sbCapStatus = if (endingBalance >= result.metrics.sbCap) "AT OR ABOVE CAP" else "BELOW CAP"
            val sbCapDiff = endingBalance - result.metrics.sbCap
            steps.add(ComputationStep(
                label = "SB vs Cap Status",
                formula = "EndingBalance compared to SB Cap",
                values = mapOf(
                    "endingBalance" to endingBalance,
                    "sbCap" to result.metrics.sbCap,
                    "difference" to sbCapDiff
                ),
                result = sbCapDiff,
                explanation = "Status: $sbCapStatus. ${if (sbCapDiff >= 0) "No TBA/TDA withdrawal needed for SB refill" else "SB may need refill from TBA/TDA in future quarters"}"
            ))
        }

        // Monthly flow summary
        val monthlyInflow = result.cashFlow.sbDeposit / 12.0
        val monthlyOutflow = result.cashFlow.sbWithdrawal / 12.0
        steps.add(ComputationStep(
            label = "═══ MONTHLY FLOW SUMMARY ═══",
            formula = "Average monthly inflows and outflows",
            values = mapOf(
                "avgMonthlyInflow" to monthlyInflow,
                "avgMonthlyOutflow" to monthlyOutflow,
                "avgNetMonthly" to (monthlyInflow - monthlyOutflow)
            ),
            result = monthlyInflow - monthlyOutflow,
            explanation = "On average, SB receives ${String.format("%.2f", monthlyInflow)} per month and withdraws ${String.format("%.2f", monthlyOutflow)} per month"
        ))

        // Interest and dividends timing
        steps.add(ComputationStep(
            label = "Interest Crediting Timing",
            formula = "Accrued monthly, credited quarterly (Q1 start)",
            values = mapOf(
                "annualInterest" to result.cashFlow.interest,
                "quarterlyCredit" to result.cashFlow.interest / 4.0
            ),
            result = result.cashFlow.interest / 4.0,
            explanation = "Interest accrues monthly but is credited to SB on the 1st day of each new quarter (4 credits per year)"
        ))

        steps.add(ComputationStep(
            label = "Dividend Crediting Timing",
            formula = "Accrued monthly, credited quarterly (Q1 start)",
            values = mapOf(
                "annualDividends" to result.cashFlow.dividends,
                "quarterlyCredit" to result.cashFlow.dividends / 4.0
            ),
            result = result.cashFlow.dividends / 4.0,
            explanation = "Bond dividends accrue monthly but are credited to SB on the 1st day of each new quarter (4 credits per year)"
        ))

        // TDA/TBA withdrawal timing (post-retirement only)
        if (targetAge > config.retirementAge && (result.cashFlow.tdaWithdrawal > 0 || result.cashFlow.tbaWithdrawal > 0)) {
            steps.add(ComputationStep(
                label = "TDA/TBA Withdrawal Timing",
                formula = "Quarterly withdrawals (1st business day of quarter)",
                values = mapOf(
                    "annualTdaWithdrawal" to result.cashFlow.tdaWithdrawal,
                    "annualTbaWithdrawal" to result.cashFlow.tbaWithdrawal,
                    "avgQuarterlyTda" to result.cashFlow.tdaWithdrawal / 4.0,
                    "avgQuarterlyTba" to result.cashFlow.tbaWithdrawal / 4.0
                ),
                result = (result.cashFlow.tdaWithdrawal + result.cashFlow.tbaWithdrawal) / 4.0,
                explanation = "Spending strategy executes quarterly: avg TDA withdrawal = ${String.format("%.2f", result.cashFlow.tdaWithdrawal / 4.0)}/qtr, avg TBA = ${String.format("%.2f", result.cashFlow.tbaWithdrawal / 4.0)}/qtr"
            ))
        }

        return BreakdownSection("Spend Bucket Operations", steps)
    }

    private fun createCapsSection(result: YearlyResult, targetAge: Int): BreakdownSection {
        // Calculate Cap AIG (uses 50% of wants, EXCLUSIVE of passive income)
        val capAig = result.cashFlow.needs + (result.cashFlow.wants * 0.5) + result.cashFlow.healthcare + result.cashFlow.propertyTax + result.cashFlow.incomeTax

        val steps = mutableListOf(
            ComputationStep(
                label = "Cap AIG (50% Wants)",
                formula = "Needs + 50%Wants + Healthcare + PropTax + Tax (EXCLUSIVE of passive income)",
                values = mapOf(
                    "needs" to result.cashFlow.needs,
                    "wants50pct" to (result.cashFlow.wants * 0.5),
                    "healthcare" to result.cashFlow.healthcare,
                    "propertyTax" to result.cashFlow.propertyTax,
                    "incomeTax" to result.cashFlow.incomeTax
                ),
                result = capAig,
                explanation = "Cap AIG uses 50% of Wants, EXCLUSIVE of passive income. Only expenses, no income subtraction."
            ),
            ComputationStep(
                label = "SB Cap",
                formula = "Cap AIG × 2.0",
                values = mapOf("capAig" to capAig),
                result = result.metrics.sbCap,
                explanation = "If SB >= SB Cap, no withdrawal from TBA/TDA needed for SB refill"
            ),
            ComputationStep(
                label = "CBB Cap",
                formula = "Initial 7× Base Cap AIG - (reductions × Base Cap AIG)",
                values = mapOf("currentCbbCap" to result.metrics.cbbCap),
                result = result.metrics.cbbCap,
                explanation = "CBB Cap starts at 7× base Cap AIG. Reduces by 1× base Cap AIG at age 65, 70, 75, 80, 85. Current age: $targetAge"
            ),
            ComputationStep(
                label = "SB vs Cap Status",
                formula = "SB_balance >= SB_Cap ? 'At/Above Cap' : 'Below Cap'",
                values = mapOf("sb" to result.balances.sb, "sbCap" to result.metrics.sbCap),
                result = if (result.balances.sb >= result.metrics.sbCap) 1.0 else 0.0,
                explanation = if (result.balances.sb >= result.metrics.sbCap) "SB is at or above cap - no TBA/TDA withdrawal needed" else "SB is below cap - may need TBA/TDA withdrawal"
            ),
            ComputationStep(
                label = "Simulation Status",
                formula = "all balances > 0 ? 'Success' : 'Failure'",
                values = mapOf(),
                result = if (result.metrics.isFailure) 0.0 else 1.0,
                explanation = if (result.metrics.isFailure) "FAILURE: One or more accounts depleted" else "SUCCESS: All accounts positive"
            )
        )

        return BreakdownSection("Caps & Status", steps)
    }
}

