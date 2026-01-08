package com.retirement.logic

import com.retirement.model.*
import kotlin.math.max
import kotlin.math.min

object SpendingStrategy {
    fun executeQuarterly(
        quarter: Int,
        config: SimulationConfig,
        currentBalances: Portfolio,
        aig: Double, // Annual Income Gap (inflation adjusted)
        marketPerformance: Double, // current / 12 months prior
        athPerformance: Double, // current / all-time high
        cbbPerformance: Double // current / 12 months prior for CBB
    ): Pair<Portfolio, Double> { // Returns updated portfolio and whether simulation failed (amount short)
        
        val qAig = aig / 4.0
        val sbCap = aig * 2.0
        val cbbCap = aig * 7.0
        val qTDAw = (config.strategy.rothConversionAmount + config.strategy.initialTdaWithdrawal) / 4.0 // TODO: adjust for inflation? spec says "inflation adjusted QTDAW"
        
        var sb = currentBalances.sb
        var cbb = currentBalances.cbb
        var tba = currentBalances.tba
        var tda = currentBalances.tda
        val tfa = currentBalances.tfa
        
        var failureShortfall = 0.0

        // 1. Withdrawal decisions
        if (marketPerformance >= 0.95 && athPerformance >= 0.85) {
            val isCbbFull = cbb >= cbbCap
            val qw = if (isCbbFull) {
                min(0.25 * aig, max(0.0, sbCap - sb))
            } else {
                min(0.125 * aig, max(0.0, sbCap - sb))
            }

            // Move Money from TDA and TBA to SB
            val (newTda, newTba, withdrawnSB) = withdrawFromEquities(tda, tba, qw, qTDAw)
            tda = newTda
            tba = newTba
            sb += withdrawnSB
            if (withdrawnSB < qw) failureShortfall += (qw - withdrawnSB)

            if (!isCbbFull) {
                val qwToCbb = min(0.125 * aig, max(0.0, cbbCap - cbb))
                val (newTda2, newTba2, withdrawnCBB) = withdrawFromEquities(tda, tba, qwToCbb, qTDAw - (qw.coerceAtMost(qTDAw))) // Remaining QTDAW?
                tda = newTda2
                tba = newTba2
                cbb += withdrawnCBB
                if (withdrawnCBB < qwToCbb) failureShortfall += (qwToCbb - withdrawnCBB)
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
            } else {
                // Check relative loss: Cap Delta
                val cbbLoss = cbbCap - cbb
                // We need more info for TDA/TBA loss, but spec says "combined loss in TDA and TBA"
                // For now, let's assume if CBB loss is smaller, we use CBB
                // TODO: calculate equity loss properly
                val withdrawAmount = min(cbb, min(qw, sbDepletion))
                cbb -= withdrawAmount
                sb += withdrawAmount
            }
        }

        return Portfolio(sb, cbb, tba, tda, tfa) to failureShortfall
    }

    private fun withdrawFromEquities(tda: Double, tba: Double, target: Double, qTdaw: Double): Triple<Double, Double, Double> {
        var currentTda = tda
        var currentTba = tba
        var withdrawn = 0.0

        // Withdraw from TDA first (up to QTDAW)
        val tdaAmount = min(qTdaw, min(target, currentTda))
        currentTda -= tdaAmount
        withdrawn += tdaAmount

        // Withdraw from TBA if needed
        if (withdrawn < target) {
            val tbaAmount = min(target - withdrawn, currentTba)
            currentTba -= tbaAmount
            withdrawn += tbaAmount
        }

        return Triple(currentTda, currentTba, withdrawn)
    }
}
