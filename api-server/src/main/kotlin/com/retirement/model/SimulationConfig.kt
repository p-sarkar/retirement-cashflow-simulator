package com.retirement.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SimulationConfig(
    val id: String? = null, // UUID string
    val name: String,
    val currentYear: Int,
    val currentAge: Int,
    val retirementAge: Int,
    val salary: Double,
    val portfolio: Portfolio,
    val spousal: SpousalDetails,
    val expenses: ExpenseConfig,
    val contributions: ContributionConfig,
    val rates: RateConfig,
    val strategy: StrategyConfig
)

@Serializable
data class ContributionConfig(
    val annual401k: Double,
    val annualTba: Double
)

@Serializable
data class Portfolio(
    val sb: Double, // Spend Bucket (HYSA)
    val cbb: Double, // Crash Buffer Bucket (Bonds)
    val tba: Double, // Taxable Brokerage
    val tda: Double, // Tax-Deferred
    val tfa: Double // Tax-Free
)

@Serializable
data class SpousalDetails(
    val spouseAge: Int,
    val lowerEarner: SocialSecurityDetails,
    val higherEarner: SocialSecurityDetails
)

@Serializable
data class SocialSecurityDetails(
    val claimAge: Int,
    val annualBenefit: Double
)

@Serializable
data class ExpenseConfig(
    val needs: Double,
    val wants: Double,
    val propertyTax: Double,
    val healthcarePreRetirement: Double,
    val healthcarePostRetirementPreMedicare: Double,
    val healthcareMedicare: Double
)

@Serializable
data class RateConfig(
    val inflation: Double,
    val preRetirementGrowth: Double,
    val postRetirementGrowth: Double,
    val bondYield: Double,
    val hysaRate: Double,
    val incomeTax: Double
)

@Serializable
data class StrategyConfig(
    val initialTdaWithdrawal: Double, // Deprecated: kept for backward compatibility
    val tdaWithdrawalPercentage: Double = 1.0, // Percentage of withdrawal needs to take from TDA (0.0-1.0, where 1.0 = 100%)
    val rothConversionAmount: Double = 0.0, // Deprecated: kept for backward compatibility
    val rothConversionPreRetirement: Double = 0.0, // Annual Roth conversion amount pre-retirement
    val rothConversionPostRetirement: Double = 0.0, // Annual Roth conversion amount post-retirement
    val type: String = "PARTHA_V0_01_20250105"
)
