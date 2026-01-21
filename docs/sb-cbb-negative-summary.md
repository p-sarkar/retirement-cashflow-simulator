# SB/CBB Negative Issue - Executive Summary

**Date**: January 21, 2026  
**Severity**: 🚨 **CRITICAL**  
**Status**: Root cause identified, fix needed

---

## The Problem

**SB (Spend Bucket) or CBB (Corporate Bond Bucket) can go negative and cause simulation failures**, even when TDA (Tax-Deferred Account) and TBA (Tax-Free Bucket) have **millions of dollars** available.

This violates the fundamental purpose of the spending strategy: to use equity accounts to fund retirement.

---

## Root Cause

### Issue #1: Market-Conditional Gates

The spending strategy in `SpendingStrategy.kt` has **market-conditional gates** that prevent withdrawals from TDA/TBA:

```kotlin
if (marketPerformance >= 95% && athPerformance >= 85%) {
    ✅ Withdraw from TDA/TBA to refill SB/CBB
} else if (sb > 50% of AIG) {
    ⚠️ Do NOTHING
} else {
    🚨 Withdraw from CBB ONLY - NOT from TDA/TBA!
}
```

**The Gap**: When market is down AND CBB is depleted, SB drains to negative despite available equity funds.

### Issue #2: Quarterly vs Monthly Timing Mismatch ⚠️ **EVEN MORE CRITICAL**

**Expenses deducted**: EVERY MONTH (12× per year)
```kotlin
// SimulationEngine.kt line 415 - runs EVERY month
val monthlyIncomeGap = estimatedAig / 12.0
balances = balances.copy(sb = balances.sb - monthlyIncomeGap)
```

**Spending strategy runs**: QUARTERLY (4× per year) - only months 1, 4, 7, 10
```kotlin
// SimulationEngine.kt line 241 - runs only 4 times per year
if ((month - 1) % 3 == 0) {
    val spendingResult = SpendingStrategy.executeQuarterly(...)
}
```

**The Critical Gap**: SB is drained in **8 months** (2, 3, 5, 6, 8, 9, 11, 12) with **NO refill opportunity**!

Even if CBB has funds, SB can go negative in months 2 or 3 before the next quarterly refill.

---

## Example Scenario

**Accounts**:
- TDA: $500,000
- TBA: $300,000  
- CBB: $100,000 ← **PLENTY AVAILABLE!**
- SB: $30,000

**Market**: Down 20% from all-time high
**Monthly expenses**: $15,000

**What Happens**:
1. **Month 1 (January)**: SB = $30k - $15k = $15k, strategy runs, CBB refills to $30k
2. **Month 2 (February)**: SB = $30k - $15k = $15k, ❌ **NO strategy** (not quarter boundary)
3. **Month 3 (March)**: SB = $15k - $15k = $0, ❌ **NO strategy**
4. **Month 4 (April)**: SB = $0 - $15k = **-$15k** ← **FAILS BEFORE STRATEGY RUNS**

**Despite having $100k in CBB!**

---

## Why This Happens

The spending strategy has **TWO fundamental flaws**:

### Flaw #1: Market-Conditional Logic
The strategy was designed with:
- **Good markets**: Sell equities while high
- **Poor markets**: Avoid selling equities at loss, use bonds instead

**The Problem**: No fallback when bonds (CBB) are insufficient during poor markets.

### Flaw #2: Timing Mismatch (More Critical)
The strategy assumes:
- Quarterly refills are sufficient
- SB will coast between refills

**The Reality**: 
- Expenses are deducted **every month** (12× per year)
- Strategy only runs **quarterly** (4× per year)
- **8 out of 12 months** have NO refill opportunity
- SB can drain to negative in months 2, 3, 5, 6, 8, 9, 11, or 12
- **Even if CBB or equities have millions of dollars!**

---

## The Asymmetry

| Expense Type | Shortfall Handling | Uses TDA/TBA as Fallback? |
|--------------|-------------------|---------------------------|
| **One-Time Expenses** | `coverShortfall()` immediately | ✅ **YES** |
| **Regular Expenses** | `executeQuarterly()` scheduled | ❌ **NO** (only in good markets) |

One-time expenses already have proper fallback logic. Regular expenses don't.

---

## Recommended Fix

**Add monthly SB shortfall check** (same logic as one-time expenses):

```kotlin
// After deducting monthly expenses:
val monthlyIncomeGap = estimatedAig / 12.0
balances = balances.copy(sb = balances.sb - monthlyIncomeGap)

// ✅ NEW: Immediate check and fix
if (balances.sb < 0) {
    val shortfallResult = SpendingStrategy.coverShortfall(
        balances,
        -balances.sb,
        config.strategy.tdaWithdrawalPercentage,
        calculateCbbCap(age)
    )
    balances = shortfallResult.balances
}
```

**Why this works**:
- Reuses existing `coverShortfall` logic (proven)
- Withdraws from CBB first, then TDA/TBA (proper priority)
- Immediate response (no waiting for quarterly strategy)
- Consistent with one-time expense handling

---

## Impact After Fix

**Before**:
- False failures when SB negative despite equity funds
- Success rates artificially low during market downturns
- Users confused why plan "fails" with money available

**After**:
- SB replenished from equities when needed
- Failures only when truly out of money
- More accurate success rates
- Expected: Higher equity volatility in down markets

---

## Files Modified

**To implement fix**:
1. `SimulationEngine.kt` - Add monthly SB check after expense deduction

**Documentation**:
1. `docs/sb-cbb-negative-despite-equity-analysis.md` - Full analysis
2. `docs/how-simulation-failure-works.md` - Updated with this issue

---

## Next Steps

1. ✅ Root cause identified
2. ✅ Documentation created
3. ⏳ Implement fix in SimulationEngine.kt
4. ⏳ Test with down-market scenarios
5. ⏳ Verify success rates improve appropriately

---

**Priority**: **CRITICAL**  
**Effort**: Low (reuse existing logic)  
**Risk**: Low (same logic already used for one-time expenses)

**This is a high-impact bug that causes incorrect simulation failures.**

