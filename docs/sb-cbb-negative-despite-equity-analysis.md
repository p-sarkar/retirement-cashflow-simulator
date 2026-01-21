# Why SB or CBB Can Go Negative Despite Sufficient TDA/TBA Balance

**Issue Identified**: January 21, 2026  
**Severity**: Critical - Design Flaw in Spending Strategy  
**Status**: Root Cause Identified, Fix Needed

---

## Problem Statement

Users have observed that **SB (Spend Bucket) or CBB (Corporate Bond Bucket)** can go negative and cause simulation failures, even when **TDA (Tax-Deferred Account) and TBA (Tax-Free Bucket)** have sufficient balances to cover the Annual Income Gap (AIG).

This appears to be a violation of the spending strategy's purpose: to use equity accounts to fund retirement when other sources are insufficient.

---

## Root Cause Analysis

### The Issue: Market-Conditional Withdrawals

The spending strategy in `SpendingStrategy.kt` has **market performance conditions** that prevent withdrawals from TDA/TBA under certain circumstances.

#### Code Location
`/api-server/src/main/kotlin/com/retirement/logic/SpendingStrategy.kt` (lines 46-116)

### Current Logic Flow

```kotlin
if (marketPerformance >= 0.95 && athPerformance >= 0.85) {
    // ✅ GOOD: Withdraw from TDA/TBA to refill SB and CBB
    withdrawFromEquities(tda, tba, qw, tdaPercentage)
} else if (sb > (aig * 0.5)) {
    // ⚠️ WARNING: Do NOTHING - no withdrawals at all
} else {
    // 🚨 PROBLEM: Only withdraw from CBB, NOT from TDA/TBA
    withdrawAmount = min(cbb, min(qw, sbDepletion))
    cbb -= withdrawAmount
    sb += withdrawAmount
}
```

### The Three Scenarios

#### Scenario 1: Good Market ✅ Works Correctly
**Conditions**: 
- marketPerformance >= 95%
- athPerformance >= 85%

**Action**: 
- Withdraws from TDA/TBA to refill SB
- Refills CBB if needed
- **Result**: SB and CBB stay healthy

---

#### Scenario 2: Poor Market + High SB ⚠️ Risky
**Conditions**:
- Market is down (performance < 95% or < 85% from ATH)
- SB > (AIG * 0.5)

**Action**: 
- **NOTHING** - No withdrawals from any account
- Assumes SB is "high enough" to coast

**Problem**: 
- SB continues to drain monthly due to expenses
- If this condition persists for multiple quarters, SB will eventually drop below AIG * 0.5
- Then it falls into Scenario 3...

---

#### Scenario 3: Poor Market + Low SB 🚨 **CRITICAL BUG**
**Conditions**:
- Market is down
- SB <= (AIG * 0.5)

**Action**:
- Withdraws from **CBB ONLY** to refill SB
- **Does NOT withdraw from TDA/TBA at all!**

**Problem**: 
- If CBB is depleted or near zero, SB cannot be refilled
- SB continues to drain until negative
- **Even if TDA/TBA have millions of dollars**, they are not touched
- **Simulation fails** due to SB going negative, despite having sufficient equity assets

---

## Example Failure Scenario

### Setup
- TDA: $500,000
- TBA: $300,000
- CBB: $5,000 (low)
- SB: $15,000
- AIG: $60,000 annually ($15,000 quarterly)
- Market: Down 20% from ATH

### Month-by-Month Progression

**Quarter 1 (Market Down)**:
- SB starts at $15,000
- Monthly expenses drain: $15,000 / month
- After 3 months: SB = $0
- Condition check: `sb <= (aig * 0.5)` → true (0 <= 30,000)
- **Action**: Withdraw from CBB only
- CBB has only $5,000 → Withdraws $5,000
- **SB after refill**: $5,000

**Quarter 2**:
- SB starts at $5,000
- Monthly expenses: $15,000/month
- After 1 month: SB = **-$10,000** ← **NEGATIVE!**
- **Simulation FAILS** despite $800k in TDA/TBA

---

## Why This Happens: Spending Strategy Design

The spending strategy was designed with these principles:

1. **In good markets**: Sell equities while they're high
2. **In poor markets**: Avoid selling equities at a loss
3. **Use bonds first** when markets are down (more stable)

### The Flaw

The strategy assumes:
- CBB will always have enough to cover expenses during market downturns
- Market downturns are temporary and SB can coast

**Reality**:
- CBB has a cap and can be depleted
- Market downturns can last multiple quarters/years
- **The strategy has NO FALLBACK** when CBB is insufficient

---

## Where the Gap Exists

### Regular Expenses (Monthly Income Gap)
**Location**: `SimulationEngine.kt` line 415

```kotlin
val monthlyIncomeGap = estimatedAig / 12.0
balances = balances.copy(sb = balances.sb - monthlyIncomeGap)
annualSbWithdrawal += monthlyIncomeGap
```

**Issue**: 
- Expenses are deducted from SB **every month**
- Spending strategy only runs **quarterly**
- If spending strategy doesn't refill SB, it drains for 3 months before next check

### One-Time Expenses
**Location**: `SimulationEngine.kt` lines 438-449

```kotlin
// Pay cash expense from SB
balances = balances.copy(sb = balances.sb - adjustedAmount)

// Trigger spending strategy if SB insufficient
if (balances.sb < 0) {
    val shortfallResult = SpendingStrategy.coverShortfall(...)
    balances = shortfallResult.balances
}
```

**Good**: One-time expenses immediately trigger `coverShortfall` if SB goes negative

**coverShortfall Logic** (lines 148-192):
```kotlin
// First, try to cover from CBB
if (remaining > 0.0 && cbb > 0.0) {
    val cbbAmount = min(cbbAvailable, remaining)
    cbb -= cbbAmount
    sb += cbbAmount
}

// Then cover remaining from equities (TDA and TBA)
if (remaining > 0.0) {
    val (newTda, newTba, withdrawn, tdaW, tbaW) = withdrawFromEquities(...)
    sb += withdrawn
}
```

**Good**: `coverShortfall` DOES use TDA/TBA as fallback after CBB!

---

## The Asymmetry

| Expense Type | Shortfall Handling | Uses TDA/TBA? |
|--------------|-------------------|---------------|
| **One-Time Expenses** | `coverShortfall()` immediately | ✅ Yes |
| **Regular Expenses (AIG)** | `executeQuarterly()` on schedule | ❌ Only if market is good |

**One-time expenses** have proper fallback logic that uses equities.

**Regular expenses** rely on quarterly strategy that may NOT use equities if market is down.

---

## Solutions

### Option 1: Always Allow Equity Withdrawals (Recommended)

Remove or modify the market condition gates to ensure SB never goes dangerously low.

**Change in `SpendingStrategy.kt`**:

```kotlin
if (marketPerformance >= 0.95 && athPerformance >= 0.85) {
    // Normal withdrawals from equities
    withdrawFromEquities(...)
} else if (sb > (aig * 0.5)) {
    // No withdrawals needed - SB is healthy
} else {
    // Low SB - must refill
    // Try CBB first
    if (cbb > 0 && cbbPerformance >= 0.90) {
        withdrawFromCBB(...)
    }
    
    // ✅ NEW: If SB still low, use equities as emergency fallback
    if (sb < (aig * 0.25)) {  // Emergency threshold
        withdrawFromEquities(tda, tba, sbDepletion, tdaPercentage)
    }
}
```

**Pros**: 
- Prevents SB from going negative
- Still prioritizes CBB in down markets
- Emergency fallback to equities when needed

**Cons**: 
- Sells equities at potentially low prices
- Violates original "don't sell low" principle

---

### Option 2: More Frequent Shortfall Checks

Check SB balance **monthly** and trigger `coverShortfall` if needed.

**Change in `SimulationEngine.kt`** (after line 415):

```kotlin
val monthlyIncomeGap = estimatedAig / 12.0
balances = balances.copy(sb = balances.sb - monthlyIncomeGap)
annualSbWithdrawal += monthlyIncomeGap

// ✅ NEW: Check for SB shortfall after regular expenses
if (balances.sb < 0) {
    val shortfallResult = SpendingStrategy.coverShortfall(
        balances,
        -balances.sb,
        config.strategy.tdaWithdrawalPercentage,
        calculateCbbCap(age)
    )
    balances = shortfallResult.balances
    // Track the emergency withdrawals...
}
```

**Pros**:
- Uses existing `coverShortfall` logic
- Immediate response to SB going negative
- Minimal code changes

**Cons**:
- More frequent equity withdrawals
- Could result in selling equities every month in sustained down markets

---

### Option 3: Hybrid Approach (Best of Both)

1. Keep quarterly strategy for **normal** refills
2. Add **emergency** monthly check for critical SB levels
3. Use different thresholds for "planned" vs "emergency" withdrawals

**Implementation**:

```kotlin
// In executeQuarterly:
if (marketPerformance >= 0.95 && athPerformance >= 0.85) {
    // Normal refills to full cap
    targetSB = sbCap
} else if (sb > (aig * 0.5)) {
    // No action
} else {
    // Emergency mode: keep SB above critical level
    val criticalLevel = aig * 0.25  // 3 months of expenses
    if (sb < criticalLevel) {
        // Use equities to reach critical level (not full cap)
        targetSB = criticalLevel
        withdrawFromEquities(...)
    } else {
        // Use CBB if available
        withdrawFromCBB(...)
    }
}
```

**Pros**:
- Balances original strategy goals with safety
- Only uses equities when truly necessary
- Different targets for good vs poor markets

**Cons**:
- More complex logic
- Requires careful threshold tuning

---

## Recommended Fix

**Implement Option 2** (Monthly Shortfall Check) as immediate fix:

1. **Simplest to implement** - reuse existing `coverShortfall` logic
2. **Most reliable** - guarantees SB never stays negative
3. **Consistent** - same logic for all expense types
4. **Least risky** - proven logic already used for one-time expenses

Then **evaluate Option 3** (Hybrid) if users complain about excessive equity sales in down markets.

---

## Impact Assessment

### Current Behavior (Broken)
- Simulations can fail with `SB < 0` despite massive TDA/TBA balances
- Failure rate is **artificially high** during market downturns
- Success rates are **misleadingly low**

### After Fix
- SB will be replenished from equities when needed
- Failures only occur when **truly out of money** (all accounts depleted)
- Success rates will **more accurately reflect** plan viability
- May see more equity volatility in down markets (expected)

---

## Code Files to Modify

1. **SpendingStrategy.kt** (Option 1 or 3)
   - Modify `executeQuarterly` logic
   - Add emergency equity withdrawal path

2. **SimulationEngine.kt** (Option 2 or 3)
   - Add monthly SB check after expense deduction
   - Call `coverShortfall` if SB negative

---

## Testing Requirements

After implementing fix:

1. **Test Scenario**: Down market (20% below ATH) with low CBB
2. **Expected**: SB refilled from TDA/TBA, simulation continues
3. **Verify**: No false failures due to SB negative
4. **Check**: Success rates increase appropriately

---

## Summary

**Root Cause**: Spending strategy only withdraws from TDA/TBA in good markets. In poor markets with depleted CBB, SB can drain to negative despite available equity funds.

**Immediate Fix**: Add monthly SB shortfall check using existing `coverShortfall` logic.

**Long-term**: Redesign spending strategy to have emergency equity withdrawal path.

**Priority**: **HIGH** - This is causing incorrect simulation failures.

---

**Status**: Analysis complete, fix needed  
**Recommended Action**: Implement Option 2 immediately, evaluate Option 3 for next release

