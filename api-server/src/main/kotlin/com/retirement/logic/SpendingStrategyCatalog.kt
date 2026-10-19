package com.retirement.logic

import com.retirement.model.SimulationConfig
import com.retirement.model.SpendingStrategyInfo

/**
 * The retirement spending strategies a simulation can select (spec 006, FR006-001/FR006-002), keyed by
 * `strategy.type`. Identifiers are stable and versioned (FR006-006): adding a strategy never changes another's
 * results. The existing strategy keeps its original id, PARTHA_V0_01_20250105, though its display name carries
 * the date 20260105 (research R006-6); fixes to it (#21-#23, T002-078) keep the id, and saved simulations keep
 * the results they were saved with.
 */
object SpendingStrategyCatalog {
    const val EXISTING = "PARTHA_V0_01_20250105"
    const val GUARDRAILS = "RISK_BASED_GUARDRAILS_V1"

    val all: List<SpendingStrategyInfo> = listOf(
        SpendingStrategyInfo(
            id = EXISTING,
            name = "Partha's Spending Strategy-v0.01-20260105",
            description = "Fixed spending; a Spend Bucket and Crash Buffer decide which accounts pay, cutting Wants 10% in down markets.",
            settings = "NONE"
        ),
        SpendingStrategyInfo(
            id = GUARDRAILS,
            name = "Risk-Based Guardrails",
            description = "Adjusts Wants when the plan's probability of success crosses a guardrail, back to a target.",
            settings = "GUARDRAILS"
        )
    )

    fun find(id: String): SpendingStrategyInfo? = all.firstOrNull { it.id == id }

    /** How the strategy sets retirement Wants. */
    fun policyFor(config: SimulationConfig): SpendingPolicy = when (config.strategy.type) {
        GUARDRAILS -> RiskBasedGuardrailsPolicy
        else -> FixedWantsPolicy
    }
}
