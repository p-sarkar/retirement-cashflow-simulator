# How a Simulation is Marked as Failed

**Last Updated**: January 21, 2026  
**Build**: v1.1.20+  
**Location**: `api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt` (lines 560-567)

---

## Overview

The Retirement Cash Flow Simulator continuously monitors account balances during each month of the simulation. If certain critical thresholds are breached, the simulation is marked as **failed** and terminates early, indicating that the retirement plan is not viable.

---

## Failure Detection Logic

### When Does a Simulation Fail?

A simulation is marked as **failed** if **ANY** of the following conditions are met during monthly processing:

```kotlin
val bothEquitiesNegative = balances.tba < 0 && balances.tda < 0

if (balances.sb < -(grossExpenses / 12.0) || 
    balances.cbb < 0 || 
    bothEquitiesNegative || 
    balances.tfa < 0) {
    isFailure = true
    if (failureYear == null) failureYear = year
}
```

### The Five Failure Conditions

#### 1. **Spend Bucket (SB) Severely Negative**
```kotlin
balances.sb < -(grossExpenses / 12.0)
```

**What it means**: SB has gone negative by more than one month's worth of expenses.

**Rationale**: 
- SB is the primary spending account (like a checking account)
- Minor negativity is allowed to handle timing issues (deposits vs withdrawals)
- Tolerance: Up to 10% of monthly gross expenses
- Beyond this threshold means the retiree cannot pay bills

**Example**:
- Monthly gross expenses: $10,000
- Tolerance: -$1,000
- If SB drops to -$1,001 or lower → **FAIL**

---

#### 2. **Corporate Bond Bucket (CBB) Negative**
```kotlin
balances.cbb < 0
```

**What it means**: The bond allocation has been completely depleted and gone negative.

**Rationale**:
- CBB is a conservative, income-generating account
- Should never be overdrawn
- Represents the "safety net" of bonds
- Any negative balance indicates serious cash flow problems

**Example**:
- CBB balance: -$0.01 → **FAIL**

---

#### 3. **BOTH Equity Accounts Negative** ⭐ (Updated in v1.1.20)
```kotlin
balances.tba < 0 && balances.tda < 0
```

**What it means**: Both the Tax-Free Bucket (TBA) and Tax-Deferred Account (TDA) have been depleted.

**Rationale**:
- TBA = Roth-like tax-free equities
- TDA = Traditional 401k/IRA tax-deferred equities
- The spending strategy can draw from either account
- As long as ONE equity account has funds, the plan can continue
- Failure only when BOTH are exhausted

**Why this changed** (v1.1.20):
- **Old logic**: Failed if TBA < 0 **OR** TDA < 0
- **New logic**: Fails only if TBA < 0 **AND** TDA < 0
- More realistic - allows one equity account to deplete while the other continues funding retirement

**Examples**:

| TBA Balance | TDA Balance | Result | Reason |
|-------------|-------------|--------|--------|
| $100,000 | $200,000 | ✅ Continue | Both positive |
| -$10,000 | $150,000 | ✅ Continue | TDA still available |
| $150,000 | -$10,000 | ✅ Continue | TBA still available |
| -$10,000 | -$5,000 | ❌ **FAIL** | Both depleted |
| $0 | -$100 | ❌ **FAIL** | Both at/below zero |

---

#### 4. **Tax-Free Account (TFA) Negative**
```kotlin
balances.tfa < 0
```

**What it means**: The Roth IRA / tax-free account has gone negative.

**Rationale**:
- TFA is a Roth IRA or similar tax-free account
- Designed to never be overdrawn (no loans/overdrafts allowed)
- Any negative balance is invalid

**Example**:
- TFA balance: -$0.01 → **FAIL**

---

## Failure Processing

### What Happens When Failure is Detected?

```kotlin
if (/* any failure condition */) {
    isFailure = true                    // Set failure flag
    if (failureYear == null) {          // Record first failure year
        failureYear = year
    }
}

// Later in the loop:
if (isFailure) break                    // Terminate simulation early
```

**Step-by-step**:

1. **Flag is Set**: `isFailure = true`
2. **Year Recorded**: `failureYear` captures the calendar year of failure (only first occurrence)
3. **Simulation Terminates**: Loop breaks, no further years processed
4. **Results Returned**: Partial results up to failure year are returned

---

## Account Balance Explanations

### Understanding the 6 Buckets

| Account | Full Name | Type | Purpose | Can Go Negative? |
|---------|-----------|------|---------|------------------|
| **SB** | Spend Bucket | Cash | Monthly expenses | Small tolerance only |
| **CBB** | Corporate Bond Bucket | Bonds | Income, stability | ❌ No |
| **TBA** | Tax-Free Bucket | Equities | Growth, tax-free withdrawals | Yes, if TDA positive |
| **TDA** | Tax-Deferred Account | Equities | Growth, taxed on withdrawal | Yes, if TBA positive |
| **TFA** | Tax-Free Account | Roth IRA | Tax-free growth | ❌ No |
| **401k** | Traditional 401k | Pre-tax retirement | Pre-retirement only | N/A (converts to TDA) |

---

## Spending Strategy & Failure Prevention

### How the Spending Strategy Helps

The simulation has a **spending strategy** that attempts to prevent failure by:

1. **Drawing from SB first** for all expenses
2. **If SB insufficient**, draws from:
   - CBB (up to cap)
   - TBA (equities - tax-free)
   - TDA (equities - tax-deferred, with tax implications)

3. **Replenishing SB** from these accounts as needed

**However**, if all accounts are depleted or hit limits, failure still occurs.

---

## Example Failure Scenarios

### Scenario 1: Overspending Depletes Everything

**Year 15**: 
- Excessive spending drains SB, CBB, TBA, and TDA
- SB = -$5,000 (exceeds tolerance)
- Result: **FAIL** - SB too negative

---

### Scenario 2: Market Crash + High Expenses

**Year 20**:
- Market crash reduces equity accounts
- TBA = -$10,000
- TDA = -$5,000
- Result: **FAIL** - Both equities negative

---

### Scenario 3: One Equity Account Survives (New Logic)

**Year 18**:
- Aggressive RMDs deplete TDA
- TDA = -$20,000
- TBA = $150,000 (still healthy)
- Result: ✅ **CONTINUE** - TBA available

**Year 25**:
- TBA also depleted from continued spending
- TDA = -$20,000
- TBA = -$10,000
- Result: ❌ **FAIL** - Both equities negative

---

## Simulation Result Structure

### How Failure is Communicated

The `SimulationResult` includes failure information:

```kotlin
data class SimulationResult(
    val config: SimulationConfig,
    val yearlyResults: List<YearlyResult>,  // Partial results up to failure
    val quarterlyResults: List<QuarterlyResult>,
    val summary: Summary(
        finalTotalBalance: Double,
        isSuccess: Boolean,           // false if failed
        failureYear: Int?,            // Year of failure (e.g., 2045)
        totalDividends: Double,
        totalInterest: Double
    ),
    val apiMetadata: ApiMetadata
)
```

**Each yearly result also has**:
```kotlin
data class Metrics(
    // ...other metrics...
    val isFailure: Boolean  // true for the year when failure occurred
)
```

---

## Monthly Processing Flow

### Where Failure Check Happens

```
For each year (1 to 35):
    For each month (1 to 12):
        1. Process income (salary, interest, dividends, SS)
        2. Process contributions (401k, Roth, TBA)
        3. Process expenses (needs, wants, healthcare, taxes)
        4. Process one-time expenses
        5. Apply market growth to equity accounts
        6. **CHECK FOR FAILURE** ← Happens here
        7. If failed, break out of loops
        8. Record quarterly results (every 3 months)
    Record yearly results
```

**Timing**: Failure is checked **every month** after all transactions and market growth are applied.

---

## Historical Context

### Changes in v1.1.20 (January 21, 2026)

**Before**: Simulation failed if **either** TBA or TDA went negative

**After**: Simulation fails only if **both** TBA and TDA go negative

**Impact**: 
- More realistic retirement scenarios
- Higher success rates (more accurate)
- Better reflects spending strategy flexibility

**See**: `docs/simulation-failure-logic-update.md` for full details

---

## Monte Carlo Simulations

### How Failure Affects Monte Carlo Runs

In Monte Carlo mode:
- Each run generates random market returns
- Some runs will fail, others succeed
- **Success Rate** = (Successful runs / Total runs) × 100%

**Example**:
- 1000 runs performed
- 850 runs succeed (no failure)
- 150 runs fail (hit failure conditions)
- **Success Rate**: 85%

**Failure detection is critical** for calculating accurate success rates.

---

## Common Questions

### Q: Why allow SB to go slightly negative?

**A**: Timing issues. Income deposits might happen after expense withdrawals in a given month. A small buffer (10% of monthly expenses) prevents false failures due to intra-month timing.

---

### Q: Why can equity accounts (TBA/TDA) go negative individually?

**A**: Realism and flexibility. The spending strategy can draw from either account. As long as one has funds, the retiree can continue their lifestyle. Only when BOTH are depleted is the plan truly failed.

---

### Q: What happens to the simulation after failure?

**A**: It **terminates immediately**. No further years are processed. Results show partial data up to the failure year.

---

### Q: Can a failed simulation "recover"?

**A**: No. Once `isFailure = true` and the loop breaks, the simulation is complete. There's no recovery mechanism.

---

### Q: How do I avoid failures?

**Strategies**:
1. Reduce spending (needs, wants)
2. Increase initial balances
3. Delay retirement
4. Assume higher market returns (riskier)
5. Add additional income sources
6. Reduce one-time expenses

---

### Q: Why can SB or CBB go negative even when TDA/TBA have sufficient balance?

**A**: **CRITICAL ISSUE IDENTIFIED** - This is a design flaw in the spending strategy.

**The Problem**: The spending strategy has market-conditional logic that prevents withdrawals from TDA/TBA during market downturns:

```kotlin
if (marketPerformance >= 0.95 && athPerformance >= 0.85) {
    // ✅ Withdraw from equities to refill SB/CBB
} else if (sb > (aig * 0.5)) {
    // ⚠️ Do NOTHING
} else {
    // 🚨 Withdraw from CBB ONLY, NOT from TDA/TBA
}
```

**Impact**:
- During market downturns (down >5% from 12mo prior OR >15% from all-time high)
- If CBB is depleted
- SB will drain to negative **even if TDA/TBA have millions**
- Simulation fails despite having sufficient equity assets

**Why This Happens**: The strategy assumes:
1. Don't sell equities in down markets (to avoid locking in losses)
2. Use bonds (CBB) instead during downturns
3. **FLAW**: No fallback when CBB is insufficient

**Fix Status**: 
- **One-time expenses**: Already use `coverShortfall` which DOES withdraw from equities ✅
- **Regular expenses**: Only use quarterly strategy with market conditions ❌

**See**: `docs/sb-cbb-negative-despite-equity-analysis.md` for full analysis and proposed fixes.

---

## Summary

A simulation **fails** when:

1. ✅ **SB** drops below -(monthly expenses) - **Can't pay bills**
2. ✅ **CBB** goes negative - **Bond safety net gone**
3. ✅ **BOTH TBA AND TDA** go negative - **All equities depleted**
4. ✅ **TFA** goes negative - **Roth overdrawn (invalid)**

When failure occurs:
- Simulation stops immediately
- Failure year is recorded
- `isSuccess = false` in results
- Partial yearly results returned

The failure detection ensures the retirement plan is **financially viable** throughout the entire retirement period.

---

**For Developers**: See `SimulationEngine.kt` lines 560-567 for implementation  
**For Documentation**: See `docs/simulation-failure-logic-update.md` for v1.1.20 changes

