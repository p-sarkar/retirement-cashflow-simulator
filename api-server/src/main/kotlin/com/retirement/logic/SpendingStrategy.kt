package com.retirement.logic

import com.retirement.model.*
import kotlin.math.max
import kotlin.math.min

data class SpendingResult(
    val portfolio: Portfolio,
    val shortfall: Double,
    val tbaWithdrawal: Double,
    val tdaWithdrawal: Double,
    val cbbWithdrawal: Double,
    val tfaWithdrawal: Double = 0.0, // last resort for spending, always to the SB (#23)
    // Reported for the computation breakdown (spec 004); they don't affect the simulation
    val rule: StrategyRule,
    val limitedBy: RefillLimit,
    val cbbRefillLimitedBy: RefillLimit? = null, // market up with the CBB below its cap; null otherwise
    val tdaToSb: Double = 0.0,
    val tbaToSb: Double = 0.0,
    val tdaToCbb: Double = 0.0,
    val tbaToCbb: Double = 0.0,
    val cbbToSb: Double = 0.0,
    // Market down with the SB at its floor: the checks that chose between the CBB and equities (#21)
    val cbbVs12MonthsPrior: Double? = null,
    val cbbLoss: Double? = null,
    val equityLoss: Double? = null
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
        cbbPerformance: Double, // CBB balance now / 12 months prior
        inflationAdjustment: Double, // cumulative inflation factor since the start year, for the year's Wants
        wantsCutInDownMarket: Boolean = true // false under Risk-Based Guardrails, which set Wants (FR006-015)
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
        var tfa = currentBalances.tfa

        var failureShortfall = 0.0
        var totalTfaWithdrawn = 0.0
        // The Tax Free account is the last resort for spending: it pays what TDA and TBA couldn't (#23)
        fun drawTfaToSb(amount: Double): Double {
            val drawn = min(amount, max(0.0, tfa))
            tfa -= drawn
            sb += drawn
            totalTfaWithdrawn += drawn
            return drawn
        }
        var totalTbaWithdrawn = 0.0
        var totalTdaWithdrawn = 0.0
        var totalCbbWithdrawn = 0.0
        // The CBB pays what TDA and TBA can't, in any market, before the TFA (FR002-005, 2026-10-19):
        // otherwise a rising market that drains the equities leaves the CBB full while the plan fails
        fun drawCbbToSb(amount: Double): Double {
            val drawn = min(amount, max(0.0, cbb))
            cbb -= drawn
            sb += drawn
            totalCbbWithdrawn += drawn
            return drawn
        }
        val rule: StrategyRule
        var limitedBy = RefillLimit.NONE
        var cbbRefillLimitedBy: RefillLimit? = null
        var tdaToSb = 0.0
        var tbaToSb = 0.0
        var tdaToCbb = 0.0
        var tbaToCbb = 0.0
        var cbbVs12MonthsPrior: Double? = null
        var cbbLoss: Double? = null
        var equityLoss: Double? = null

        // 1. Withdrawal decisions
        if (marketPerformance >= 0.95 && athPerformance >= 0.85) {
            val isCbbFull = cbb >= cbbCap
            rule = if (isCbbFull) StrategyRule.MARKET_UP_CBB_FULL else StrategyRule.MARKET_UP_CBB_REFILL

            // Calculate quarterly withdrawal - only if SB is below cap
            // If SB exceeds cap, no withdrawal needed from equities
            val sbDepletion = max(0.0, sbCap - sb)

            val requested = (if (isCbbFull) 0.25 else 0.125) * aig + sbDeficit
            val qw = if (sbDepletion <= 0.0) {
                // SB already at or above cap, no withdrawal needed
                0.0
            } else {
                min(requested, sbDepletion)
            }
            limitedBy = when {
                sbDepletion <= 0.0 || sbDepletion < requested -> RefillLimit.SB_CAP
                qw <= 0.0 -> RefillLimit.NONE
                sbDeficit > 0.0 -> RefillLimit.SB_FLOOR_DEFICIT
                else -> RefillLimit.QUARTERLY_SHARE
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
                tdaToSb += tdaW
                tbaToSb += tbaW

                val fromCbbFallback = if (withdrawnSB < qw) drawCbbToSb(qw - withdrawnSB) else 0.0
                val covered = withdrawnSB + fromCbbFallback
                val fromTfa = if (covered < qw) drawTfaToSb(qw - covered) else 0.0
                if (covered + fromTfa < qw) {
                    failureShortfall += (qw - covered - fromTfa)
                    limitedBy = RefillLimit.AVAILABLE_BALANCE
                }
            }

            if (!isCbbFull) {
                val cbbShare = 0.125 * aig
                val qwToCbb = min(cbbShare, max(0.0, cbbCap - cbb))

                // Only refill CBB if needed
                if (qwToCbb > 0.0) {
                    cbbRefillLimitedBy = if (cbbCap - cbb < cbbShare) RefillLimit.CBB_CAP else RefillLimit.QUARTERLY_SHARE
                    // No remaining QTDAW concept - use same percentage
                    val (newTda2, newTba2, withdrawnCBB, tdaW2, tbaW2) = withdrawFromEquities(tda, tba, qwToCbb, tdaPercentage)
                    tda = newTda2
                    tba = newTba2
                    cbb += withdrawnCBB

                    totalTdaWithdrawn += tdaW2
                    totalTbaWithdrawn += tbaW2
                    tdaToCbb += tdaW2
                    tbaToCbb += tbaW2

                    if (withdrawnCBB < qwToCbb) {
                        failureShortfall += (qwToCbb - withdrawnCBB)
                        cbbRefillLimitedBy = RefillLimit.AVAILABLE_BALANCE
                    }
                }
            }
        } else if (sb > sbFloor) {
            // No money movement needed
            rule = StrategyRule.MARKET_DOWN_HOLD
        } else {
            // Low SB and Market Down: refill from the CBB while it has held up (>= 90% of its balance 12 months
            // earlier) or has lost less than the equities; otherwise sell equities (FR002-005, #21)
            // Reduce Wants by 10%: the year's Wants, i.e. the configured Wants with inflation (#22).
            // Not under Risk-Based Guardrails, which set Wants themselves (FR006-015)
            val reducedAig = if (wantsCutInDownMarket) aig - config.expenses.wants * inflationAdjustment * 0.1 else aig
            val qw = reducedAig / 4.0 + sbDeficit
            val sbDepletion = max(0.0, sbCap - sb)
            val target = min(qw, sbDepletion)
            cbbVs12MonthsPrior = cbbPerformance
            if (cbbPerformance < 0.90) {
                // CBB loss: how far below its cap; equities' loss: what TDA + TBA lost to the market over 12 months
                cbbLoss = cbbCap - cbb
                equityLoss = (tda + tba) * max(0.0, 1.0 / marketPerformance - 1.0)
            }
            val fromCbb = cbbLoss == null || cbbLoss < equityLoss!!
            // Either way every account can pay: the chosen source first, then the others (2026-10-19)
            val available = max(0.0, cbb) + max(0.0, tda) + max(0.0, tba) + max(0.0, tfa)
            limitedBy = when {
                available < target -> RefillLimit.AVAILABLE_BALANCE
                sbDepletion < qw -> RefillLimit.SB_CAP
                sbDeficit > 0.0 -> RefillLimit.SB_FLOOR_DEFICIT
                else -> RefillLimit.QUARTERLY_SHARE
            }

            if (fromCbb) {
                rule = StrategyRule.MARKET_DOWN_FROM_CBB
                // Move Money from CBB to SB; what the CBB can't cover comes from TDA/TBA, then the TFA (FR002-005,
                // 2026-10-19): otherwise an empty CBB chosen by the loss comparison moves nothing and the plan fails
                // with the equities untouched
                val fromCbbAmount = drawCbbToSb(max(0.0, target))
                if (fromCbbAmount < target) {
                    val (newTda, newTba, withdrawnSB, tdaW, tbaW) = withdrawFromEquities(tda, tba, target - fromCbbAmount, tdaPercentage)
                    tda = newTda
                    tba = newTba
                    sb += withdrawnSB
                    totalTdaWithdrawn += tdaW
                    totalTbaWithdrawn += tbaW
                    tdaToSb += tdaW
                    tbaToSb += tbaW
                    val covered = fromCbbAmount + withdrawnSB
                    val fromTfa = if (covered < target) drawTfaToSb(target - covered) else 0.0
                    failureShortfall += target - covered - fromTfa
                }
            } else {
                rule = StrategyRule.MARKET_DOWN_FROM_EQUITIES
                // Move Money from TDA and TBA to SB
                if (target > 0.0) {
                    val (newTda, newTba, withdrawnSB, tdaW, tbaW) = withdrawFromEquities(tda, tba, target, tdaPercentage)
                    tda = newTda
                    tba = newTba
                    sb += withdrawnSB
                    totalTdaWithdrawn += tdaW
                    totalTbaWithdrawn += tbaW
                    tdaToSb += tdaW
                    tbaToSb += tbaW
                    val fromCbbFallback = if (withdrawnSB < target) drawCbbToSb(target - withdrawnSB) else 0.0
                    val covered = withdrawnSB + fromCbbFallback
                    val fromTfa = if (covered < target) drawTfaToSb(target - covered) else 0.0
                    failureShortfall += target - covered - fromTfa
                }
            }
        }

        return SpendingResult(
            portfolio = Portfolio(sb, cbb, tba, tda, tfa),
            shortfall = failureShortfall,
            tbaWithdrawal = totalTbaWithdrawn,
            tdaWithdrawal = totalTdaWithdrawn,
            cbbWithdrawal = totalCbbWithdrawn,
            tfaWithdrawal = totalTfaWithdrawn,
            rule = rule,
            limitedBy = limitedBy,
            cbbRefillLimitedBy = cbbRefillLimitedBy,
            tdaToSb = tdaToSb,
            tbaToSb = tbaToSb,
            tdaToCbb = tdaToCbb,
            tbaToCbb = tbaToCbb,
            cbbToSb = totalCbbWithdrawn,
            cbbVs12MonthsPrior = cbbVs12MonthsPrior,
            cbbLoss = cbbLoss,
            equityLoss = equityLoss
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
        val cbbWithdrawal: Double,
        val tfaWithdrawal: Double = 0.0
    )

    /**
     * Cover an immediate shortfall (e.g., from one-time expenses).
     * Withdraws from CBB first, then from equities (TDA/TBA), then from the TFA as a last resort (#23).
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
        var tfa = currentBalances.tfa

        var remaining = shortfallAmount
        var totalCbbWithdrawn = 0.0
        var totalTdaWithdrawn = 0.0
        var totalTbaWithdrawn = 0.0
        var totalTfaWithdrawn = 0.0

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
            remaining -= withdrawn
        }

        // Then the Tax Free account, as a last resort (#23)
        if (remaining > 0.0 && tfa > 0.0) {
            val tfaAmount = min(tfa, remaining)
            tfa -= tfaAmount
            sb += tfaAmount
            totalTfaWithdrawn += tfaAmount
            remaining -= tfaAmount
        }

        return ShortfallResult(
            balances = Portfolio(sb, cbb, tba, tda, tfa),
            tbaWithdrawal = totalTbaWithdrawn,
            tdaWithdrawal = totalTdaWithdrawn,
            cbbWithdrawal = totalCbbWithdrawn,
            tfaWithdrawal = totalTfaWithdrawn
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
