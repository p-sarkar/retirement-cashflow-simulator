# Why SB Goes Negative Even When CBB Has Sufficient Funds

**Date**: January 21, 2026  
**Severity**: 🚨 **CRITICAL**  
**Status**: Additional root cause identified

---

## The New Problem

Even when **CBB has sufficient funds**, SB can still go negative and cause simulation failures. This is a **timing issue** between expense deductions and spending strategy execution.

---

## Root Cause: Quarterly vs Monthly Mismatch

### The Timing Problem

**Expenses are deducted MONTHLY** (`SimulationEngine.kt` line 415):
```kotlin
// EVERY MONTH (1-12)
val monthlyIncomeGap = estimatedAig / 12.0
balances = balances.copy(sb = balances.sb - monthlyIncomeGap)
```

**Spending strategy runs QUARTERLY** (`SimulationEngine.kt` line 241):
```kotlin
// ONLY in months 1, 4, 7, 10
if ((month - 1) % 3 == 0) {
    val spendingResult = SpendingStrategy.executeQuarterly(...)
    balances = spendingResult.portfolio
}
```

### What This Means

**Months 1, 4, 7, 10**:
- ✅ Expenses deducted
- ✅ Spending strategy runs
- ✅ SB may be refilled from CBB or equities

**Months 2, 3, 5, 6, 8, 9, 11, 12**:
- ❌ Expenses deducted
- ❌ **NO spending strategy**
- ❌ SB continues draining with no refill

---

## Example Scenario: CBB Available But SB Still Goes Negative

### Setup
- SB: $30,000 (start of quarter)
- CBB: $100,000 (plenty available!)
- Monthly AIG: $15,000
- Market: Down (but CBB performance good)

### Month-by-Month Timeline

**Month 1 (January)** - Quarter Start:
- SB starts: $30,000
- Expense deduction: -$15,000
- **SB after expenses**: $15,000
- ✅ **Spending strategy runs**
- CBB → SB withdrawal: +$15,000
- **SB after refill**: $30,000

**Month 2 (February)**:
- SB starts: $30,000
- Expense deduction: -$15,000
- **SB after expenses**: $15,000
- ❌ **No spending strategy** (not a quarter boundary)
- **SB stays at**: $15,000

**Month 3 (March)**:
- SB starts: $15,000
- Expense deduction: -$15,000
- **SB after expenses**: $0
- ❌ **No spending strategy**
- **SB stays at**: $0

**Month 4 (April)** - Quarter Start:
- SB starts: $0
- Expense deduction: -$15,000
- **SB after expenses**: **-$15,000** ← **NEGATIVE!**
- 🚨 **SIMULATION FAILS BEFORE STRATEGY RUNS**

**Despite CBB having $100k available!**

---

## Why This Happens

### Design Assumption (Flawed)

The quarterly strategy assumes:
1. Refills happen every 3 months
2. SB is refilled to enough to last 3 months
3. Market conditions/caps may limit refill amount

**Reality**:
- If refill is limited (due to caps, market conditions, or withdrawal limits)
- SB may not get enough to last 3 months
- SB can go negative BEFORE next quarterly refill

### The Quarterly Withdrawal Calculation

```kotlin
// In good markets:
val qw = if (sbDepletion <= 0.0) {
    0.0  // No withdrawal if SB above cap
} else if (isCbbFull) {
    min(0.25 * aig, sbDepletion)  // Limited to 25% of annual AIG
} else {
    min(0.125 * aig, sbDepletion)  // Limited to 12.5% of annual AIG
}
```

**The Problem**: Withdrawal is capped at **25% of AIG** (3 months) or **12.5% of AIG** (1.5 months).

If SB is below cap and needs more than the cap allows, it won't get enough to last 3 months.

---

## Specific Scenarios Where SB Goes Negative

### Scenario 1: SB Below Cap Mid-Quarter

**Example**:
- Quarter starts with SB = $10,000
- SB Cap = $120,000 (2× annual AIG of $60k)
- SB depletion = $110,000
- Quarterly withdrawal (isCbbFull = false): `min(0.125 * 60000, 110000) = $7,500`
- **SB after refill**: $10,000 + $7,500 = $17,500
- Monthly expenses: $5,000
- **After 3 months**: $17,500 - $15,000 = $2,500 (barely survives)
- **Next month (before strategy)**: $2,500 - $5,000 = **-$2,500** ← **FAILS**

---

### Scenario 2: Market Down, CBB Not Full

When market is down and CBB is not at cap:
```kotlin
val qw = min(0.125 * aig, sbDepletion)  // Only 12.5% of annual AIG
```

This is only **1.5 months** of expenses, but the strategy won't run again for **3 months**.

**Math**:
- Annual AIG: $60,000
- Quarterly withdrawal: 0.125 × $60,000 = $7,500
- Monthly expenses: $60,000 / 12 = $5,000
- **Coverage**: $7,500 / $5,000 = **1.5 months**
- **Gap**: 3.0 - 1.5 = **1.5 months uncovered**

Result: SB goes negative in month 3.

---

### Scenario 3: SB Already at Cap

```kotlin
val sbDepletion = max(0.0, sbCap - sb)
```

If SB is already at cap, `sbDepletion = 0`, so **no withdrawal happens at all**.

But SB still has expenses deducted monthly, so it will drain below cap and stay there until next quarter.

---

## The CBB Withdrawal Logic (Also Problematic)

When market is down, strategy withdraws from CBB:

```kotlin
// Market down, low SB
val reducedAig = (aig - (config.expenses.wants * 0.1))
val qw = reducedAig / 4.0
val sbDepletion = max(0.0, sbCap - sb)

val withdrawAmount = min(cbb, min(qw, sbDepletion))
cbb -= withdrawAmount
sb += withdrawAmount
```

**Issues**:
1. Uses `sbDepletion` which is based on cap, not actual need
2. If SB above cap, `sbDepletion = 0` → **no withdrawal**
3. Withdraws `qw` (quarterly amount), not enough for 3 months

---

## Why One-Time Expenses Work Better

One-time expenses trigger **immediate** shortfall handling:

```kotlin
// One-time expense
balances = balances.copy(sb = balances.sb - adjustedAmount)

// Immediate check
if (balances.sb < 0) {
    val shortfallResult = SpendingStrategy.coverShortfall(
        balances,
        -balances.sb,  // Exact shortfall amount
        tdaPercentage,
        cbbCap
    )
    balances = shortfallResult.balances
}
```

**Key differences**:
- ✅ Immediate response (not quarterly)
- ✅ Exact shortfall amount (not capped)
- ✅ Fallback to equities if CBB insufficient

---

## Root Causes Summary

### Primary Issue: Timing Mismatch
- Expenses: **Monthly** (12× per year)
- Strategy: **Quarterly** (4× per year)
- **Gap**: 8 months with no refill opportunity

### Secondary Issues:
1. **Capped withdrawals**: Strategy may not withdraw enough to last 3 months
2. **SB cap logic**: If SB above cap, no withdrawal even if will go negative soon
3. **No mid-quarter checks**: SB can drain to negative between strategy runs

---

## The Complete Fix (Enhanced)

### Fix 1: Monthly Shortfall Check (Immediate)

Add check **every month** after expense deduction:

```kotlin
// After monthly expense deduction:
val monthlyIncomeGap = estimatedAig / 12.0
balances = balances.copy(sb = balances.sb - monthlyIncomeGap)

// ✅ CRITICAL FIX: Check and fix SB shortfall immediately
if (balances.sb < 0) {
    val shortfallResult = SpendingStrategy.coverShortfall(
        balances,
        -balances.sb,
        config.strategy.tdaWithdrawalPercentage,
        calculateCbbCap(age)
    )
    balances = shortfallResult.balances
    
    // Track emergency withdrawals for reporting
    tbaWithdrawal += shortfallResult.tbaWithdrawal
    tdaWithdrawal += shortfallResult.tdaWithdrawal
    // (add to monthly/quarterly/annual totals)
}
```

**Why this works**:
- ✅ Immediate response every month
- ✅ Uses exact shortfall amount
- ✅ CBB first, then equities (proper priority)
- ✅ Reuses proven `coverShortfall` logic

---

### Fix 2: Improve Quarterly Withdrawal Logic (Long-term)

Update the quarterly strategy to ensure it withdraws **enough to last 3 months**:

```kotlin
// Calculate how much SB needs to last until next quarterly refill
val monthsUntilNextRefill = 3
val estimatedMonthlyDrain = aig / 12.0
val targetSB = estimatedMonthlyDrain * monthsUntilNextRefill

val sbNeeded = max(0.0, targetSB - sb)

// Withdraw enough to reach target (not limited by caps)
val qw = sbNeeded
```

**Note**: This may conflict with original strategy design, so **Fix 1 is safer** as immediate solution.

---

## Impact Analysis

### Current Behavior (Broken)
- SB can go negative between quarterly refills
- Even with CBB available
- False failures in months 2, 3, 5, 6, 8, 9, 11, 12
- Success rates artificially low

### After Fix 1 (Monthly Check)
- SB immediately refilled from CBB/equities when negative
- No false failures due to timing
- Success rates improve
- More frequent (but smaller) withdrawals

### After Fix 2 (Better Quarterly Logic)
- Fewer emergency withdrawals needed
- More predictable withdrawal patterns
- Still compatible with Fix 1 as safety net

---

## Testing Scenarios

### Test 1: SB Goes Negative in Month 2
**Setup**:
- SB: $10,000 (quarter start)
- Monthly expenses: $6,000
- CBB: $50,000

**Expected**:
- Month 1: SB = $10,000 - $6,000 = $4,000, strategy refills to some amount
- Month 2: SB drains, if negative → immediate refill from CBB
- **Result**: No failure

### Test 2: CBB Depleted, Equities Available
**Setup**:
- SB: $5,000
- CBB: $2,000
- TDA: $500,000
- Monthly expenses: $10,000

**Expected**:
- Month 1: SB negative after expenses
- CBB withdraws $2,000 (not enough)
- Equities withdraw remaining shortfall
- **Result**: No failure

### Test 3: Long Market Downturn
**Setup**:
- Market down 30% for 12 months
- SB draining monthly
- CBB available

**Expected**:
- Monthly refills from CBB
- SB never goes negative
- **Result**: Simulation continues

---

## Files to Modify

### Immediate Fix (Priority 1)
**File**: `SimulationEngine.kt`
**Location**: After line 415 (monthly expense deduction)
**Change**: Add monthly SB shortfall check using `coverShortfall`

### Optional Enhancement (Priority 2)
**File**: `SpendingStrategy.kt`
**Location**: `executeQuarterly` function
**Change**: Improve withdrawal calculation to ensure 3-month coverage

---

## Recommendation

**Implement Fix 1 immediately**:
- Low risk (reuses existing logic)
- High impact (prevents all false failures)
- Consistent with one-time expense handling
- Simple to test

**Evaluate Fix 2 later**:
- Requires more design discussion
- May conflict with strategy philosophy
- Fix 1 acts as safety net anyway

---

## Summary

**Root Cause #1**: Quarterly refills vs monthly expenses create 2-month gaps with no refill

**Root Cause #2**: Withdrawal amounts may be insufficient to last 3 months (caps, SB cap logic)

**Result**: SB goes negative in months 2, 3, 5, 6, 8, 9, 11, 12 despite CBB having funds

**Fix**: Add monthly shortfall check using existing `coverShortfall` logic

**Impact**: Eliminates false failures due to timing, improves success rate accuracy

---

**Priority**: 🚨 **CRITICAL**  
**Effort**: Low (10-20 lines of code)  
**Risk**: Low (reuses proven logic)  
**Testing**: Medium (verify monthly refills work correctly)

This is the **most critical bug** - it causes failures even when all accounts have sufficient funds!

