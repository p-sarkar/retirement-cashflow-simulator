package com.retirement.model

import kotlinx.serialization.Serializable

/**
 * One entry of the spending strategy catalog (spec 006, FR006-002), served by GET /api/spending-strategies.
 * [settings] tells the form which fields the strategy uses: "NONE" or "GUARDRAILS".
 */
@Serializable
data class SpendingStrategyInfo(
    val id: String,
    val name: String,
    val description: String,
    val settings: String
)
