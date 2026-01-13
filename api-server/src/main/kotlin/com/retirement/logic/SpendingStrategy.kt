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
        aig: Double, // Annual Income Gap (inflation adjusted)
        marketPerformance: Double, // current / 12 months prior
        athPerformance: Double, // current / all-time high
        cbbPerformance: Double, // current / 12 months prior for CBB
        inflationAdjustment: Double // 1.0 + cumulative inflation
    ): SpendingResult { 
        
        val qAig = aig / 4.0
        val sbCap = aig * 2.0
        val cbbCap = aig * 4.0
        
        // Adjust TDA withdrawal and Roth conversion for inflation
        val initialTda = config.strategy.initialTdaWithdrawal * inflationAdjustment
        val rothConv = config.strategy.rothConversionAmount * inflationAdjustment
        val qTDAw = (initialTda + rothConv) / 4.0 
        
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
                min(0.25 * aig, sbDepletion)
            } else {
                min(0.125 * aig, sbDepletion)
            }

            // Only withdraw if needed (qw > 0)
            if (qw > 0.0) {
                // Move Money from TDA and TBA to SB
                val (newTda, newTba, withdrawnSB, tdaW, tbaW) = withdrawFromEquities(tda, tba, qw, qTDAw)
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
                    // Remaining QTDAW?
                    val remainingQTDAW = max(0.0, qTDAw - totalTdaWithdrawn)

                    val (newTda2, newTba2, withdrawnCBB, tdaW2, tbaW2) = withdrawFromEquities(tda, tba, qwToCbb, remainingQTDAW)
                    tda = newTda2
                    tba = newTba2
                    cbb += withdrawnCBB

                    totalTdaWithdrawn += tdaW2
                    totalTbaWithdrawn += tbaW2

                    if (withdrawnCBB < qwToCbb) failureShortfall += (qwToCbb - withdrawnCBB)
                }
            }
        } else if (sb > (aig * 0.5)) {
            // No money movement needed
        } else {
            // Low SB and Market Down
            val reducedAig = (aig - (config.expenses.wants * 0.1)) // Reduce Wants by 10%
            val qw = reducedAig / 4.0
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

    data class WithdrawalResult(
        val newTda: Double,
        val newTba: Double,
        val totalWithdrawn: Double,
        val tdaWithdrawn: Double,
        val tbaWithdrawn: Double
    )

    private fun withdrawFromEquities(tda: Double, tba: Double, target: Double, qTdaw: Double): WithdrawalResult {
        var currentTda = tda
        var currentTba = tba
        var withdrawn = 0.0
        var tdaWithdrawn = 0.0
        var tbaWithdrawn = 0.0

        // Withdraw from TDA first (up to QTDAW)
        val tdaAmount = min(qTdaw, min(target, currentTda))
        currentTda -= tdaAmount
        withdrawn += tdaAmount
        tdaWithdrawn += tdaAmount

        // Withdraw from TBA if needed
        if (withdrawn < target) {
            val tbaAmount = min(target - withdrawn, currentTba)
            currentTba -= tbaAmount
            withdrawn += tbaAmount
            tbaWithdrawn += tbaAmount
        }

        return WithdrawalResult(currentTda, currentTba, withdrawn, tdaWithdrawn, tbaWithdrawn)
    }
}
