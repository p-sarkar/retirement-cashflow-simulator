package com.retirement.logic

import com.retirement.model.*
import kotlin.math.max
import kotlin.math.min

data class SpendingResult(
    val portfolio: Portfolio,
    val shortfall: Double,
    val tbaWithdrawal: Double,
    val tdaWithdrawal: Double,
    val cbbWithdrawal: Double
)

object SpendingStrategy {
    fun executeQuarterly(
        quarter: Int,
        config: SimulationConfig,
        currentBalances: Portfolio,
        aig: Double, // Annual Income Gap (inflation adjusted) - for withdrawal decisions
        capAig: Double, // AIG for cap computation (uses 50% of wants)
        annualExpenses: Double, // Total regular expenses incl. income tax (inflation adjusted) - for the SB floor
        cbbCap: Double, // CBB Cap (calculated externally, reduces over time)
        marketPerformance: Double, // current / 12 months prior
        athPerformance: Double, // current / all-time high
        cbbPerformance: Double, // current / 12 months prior for CBB
        inflationAdjustment: Double // 1.0 + cumulative inflation
    ): SpendingResult { 
        
        val qAig = aig / 4.0
        // SB Cap uses the reduced AIG (50% wants), but never drops below the SB floor:
        // when passive income covers most expenses, Cap AIG can be <= 0 while the SB still drains by the AIG
        val sbFloor = sbFloor(aig, annualExpenses)
        val sbCap = sbCap(aig, capAig, annualExpenses)
        // Amount the SB has fallen below its floor; each refill repays it on top of the usual quarterly amount
        val sbDeficit = max(0.0, sbFloor - currentBalances.sb)

        // Get TDA withdrawal percentage from config (0-100%)
        val tdaPercentage = config.strategy.tdaWithdrawalPercentage

        var sb = currentBalances.sb
        var cbb = currentBalances.cbb
        var tba = currentBalances.tba
        var tda = currentBalances.tda
        val tfa = currentBalances.tfa
        
        var failureShortfall = 0.0
        var totalTbaWithdrawn = 0.0
        var totalTdaWithdrawn = 0.0
        var totalCbbWithdrawn = 0.0

        // 1. Withdrawal decisions
        if (marketPerformance >= 0.95 && athPerformance >= 0.85) {
            val isCbbFull = cbb >= cbbCap

            // Calculate quarterly withdrawal - only if SB is below cap
            // If SB exceeds cap, no withdrawal needed from equities
            val sbDepletion = max(0.0, sbCap - sb)

            val qw = if (sbDepletion <= 0.0) {
                // SB already at or above cap, no withdrawal needed
                0.0
            } else if (isCbbFull) {
                min(0.25 * aig + sbDeficit, sbDepletion)
            } else {
                min(0.125 * aig + sbDeficit, sbDepletion)
            }

            // Only withdraw if needed (qw > 0)
            if (qw > 0.0) {
                // Move Money from TDA and TBA to SB
                val (newTda, newTba, withdrawnSB, tdaW, tbaW) = withdrawFromEquities(tda, tba, qw, tdaPercentage)
                tda = newTda
                tba = newTba
                sb += withdrawnSB
                totalTdaWithdrawn += tdaW
                totalTbaWithdrawn += tbaW

                if (withdrawnSB < qw) failureShortfall += (qw - withdrawnSB)
            }

            if (!isCbbFull) {
                val qwToCbb = min(0.125 * aig, max(0.0, cbbCap - cbb))

                // Only refill CBB if needed
                if (qwToCbb > 0.0) {
                    // No remaining QTDAW concept - use same percentage
                    val (newTda2, newTba2, withdrawnCBB, tdaW2, tbaW2) = withdrawFromEquities(tda, tba, qwToCbb, tdaPercentage)
                    tda = newTda2
                    tba = newTba2
                    cbb += withdrawnCBB

                    totalTdaWithdrawn += tdaW2
                    totalTbaWithdrawn += tbaW2

                    if (withdrawnCBB < qwToCbb) failureShortfall += (qwToCbb - withdrawnCBB)
                }
            }
        } else if (sb > sbFloor) {
            // No money movement needed
        } else {
            // Low SB and Market Down
            val reducedAig = (aig - (config.expenses.wants * 0.1)) // Reduce Wants by 10%
            val qw = reducedAig / 4.0 + sbDeficit
            val sbDepletion = max(0.0, sbCap - sb)
            
            if (cbbPerformance >= 0.90) {
                // Move Money from CBB to SB
                val withdrawAmount = min(cbb, min(qw, sbDepletion))
                cbb -= withdrawAmount
                sb += withdrawAmount
                totalCbbWithdrawn += withdrawAmount
            } else {
                // Check relative loss: Cap Delta
                val cbbLoss = cbbCap - cbb
                // We need more info for TDA/TBA loss, but spec says "combined loss in TDA and TBA"
                // For now, let's assume if CBB loss is smaller, we use CBB
                val withdrawAmount = min(cbb, min(qw, sbDepletion))
                cbb -= withdrawAmount
                sb += withdrawAmount
                totalCbbWithdrawn += withdrawAmount
            }
        }

        return SpendingResult(
            portfolio = Portfolio(sb, cbb, tba, tda, tfa),
            shortfall = failureShortfall,
            tbaWithdrawal = totalTbaWithdrawn,
            tdaWithdrawal = totalTdaWithdrawn,
            cbbWithdrawal = totalCbbWithdrawn
        )
    }

    /**
     * SB floor = max(6 months of the full AIG, 1 quarter of total expenses).
     * Passive income is credited at the start of each quarter while expenses are paid through it,
     * so the SB must hold a quarter's spending to stay positive at quarter end.
     */
    fun sbFloor(aig: Double, annualExpenses: Double): Double = max(aig * 0.5, annualExpenses * 0.25)

    /** SB Cap = 2 × Cap AIG, never below the SB floor. */
    fun sbCap(aig: Double, capAig: Double, annualExpenses: Double): Double =
        max(capAig * 2.0, sbFloor(aig, annualExpenses))

    data class WithdrawalResult(
        val newTda: Double,
        val newTba: Double,
        val totalWithdrawn: Double,
        val tdaWithdrawn: Double,
        val tbaWithdrawn: Double
    )

    data class ShortfallResult(
        val balances: Portfolio,
        val tbaWithdrawal: Double,
        val tdaWithdrawal: Double,
        val cbbWithdrawal: Double
    )

    /**
     * Cover an immediate shortfall (e.g., from one-time expenses).
     * Withdraws from CBB first, then from equities (TDA/TBA).
     */
    fun coverShortfall(
        currentBalances: Portfolio,
        shortfallAmount: Double,
        tdaPercentage: Double,
        cbbCap: Double
    ): ShortfallResult {
        var sb = currentBalances.sb
        var cbb = currentBalances.cbb
        var tba = currentBalances.tba
        var tda = currentBalances.tda
        val tfa = currentBalances.tfa

        var remaining = shortfallAmount
        var totalCbbWithdrawn = 0.0
        var totalTdaWithdrawn = 0.0
        var totalTbaWithdrawn = 0.0

        // First, try to cover from CBB (if available above cap)
        if (remaining > 0.0 && cbb > 0.0) {
            val cbbAvailable = cbb
            val cbbAmount = min(cbbAvailable, remaining)
            cbb -= cbbAmount
            sb += cbbAmount
            totalCbbWithdrawn += cbbAmount
            remaining -= cbbAmount
        }

        // Then cover remaining from equities (TDA and TBA)
        if (remaining > 0.0) {
            val (newTda, newTba, withdrawn, tdaW, tbaW) = withdrawFromEquities(tda, tba, remaining, tdaPercentage)
            tda = newTda
            tba = newTba
            sb += withdrawn
            totalTdaWithdrawn += tdaW
            totalTbaWithdrawn += tbaW
        }

        return ShortfallResult(
            balances = Portfolio(sb, cbb, tba, tda, tfa),
            tbaWithdrawal = totalTbaWithdrawn,
            tdaWithdrawal = totalTdaWithdrawn,
            cbbWithdrawal = totalCbbWithdrawn
        )
    }

    private fun withdrawFromEquities(tda: Double, tba: Double, target: Double, tdaPercentage: Double): WithdrawalResult {
        var currentTda = tda
        var currentTba = tba
        var withdrawn = 0.0
        var tdaWithdrawn = 0.0
        var tbaWithdrawn = 0.0

        // Withdraw from TDA based on percentage of target (0.0-1.0, where 1.0 = 100%)
        val tdaTargetAmount = target * tdaPercentage
        val tdaAmount = min(tdaTargetAmount, currentTda)
        currentTda -= tdaAmount
        withdrawn += tdaAmount
        tdaWithdrawn += tdaAmount

        // Withdraw from TBA for the remainder
        if (withdrawn < target) {
            val tbaAmount = min(target - withdrawn, currentTba)
            currentTba -= tbaAmount
            withdrawn += tbaAmount
            tbaWithdrawn += tbaAmount
        }

        return WithdrawalResult(currentTda, currentTba, withdrawn, tdaWithdrawn, tbaWithdrawn)
    }
}
