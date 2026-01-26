# Bug Fix: One-Time Expenses Not Included in Quarterly Total Expenses

**Date**: January 23, 2026  
**Discovered During**: Test T029 execution  
**Severity**: HIGH  
**Status**: ✅ FIXED

---

## Problem Description

When running test T029 (Verify Spend Bucket Deduction in Q1), one-time expenses were correctly appearing in the quarterly "One-Time Expenses" column, but they were **not being included in the "Total Expenses" column** for the quarter.

### Observed Behavior
- ✅ Quarterly view Q1: Shows $50,000 in "One-Time Expenses" column
- ❌ Quarterly view Q1: "Total Expenses" does NOT include the $50,000
- ✅ Annual view: "Total Expenses" correctly includes one-time expenses

### Expected Behavior
- ✅ Quarterly view Q1: Shows $50,000 in "One-Time Expenses" column
- ✅ Quarterly view Q1: "Total Expenses" includes the $50,000 (needs + wants + healthcare + tax + property tax + **one-time expenses**)
- ✅ Annual view: "Total Expenses" correctly includes one-time expenses

---

## Root Cause Analysis

### Investigation

In `SimulationEngine.kt`, when calculating quarterly total expenses (line 576):

```kotlin
val qTotalExpenses = qNeeds + qWants + qHealth + qTax + qProp
```

The calculation was **missing** `qOneTimeExpenses`! 

### Code Flow

1. ✅ One-time expenses are tracked in `qOneTimeExpenses` accumulator
2. ✅ `qOneTimeExpenses` is included in `QuarterlyResult.cashFlow.oneTimeExpenses`
3. ❌ **BUT**: `qOneTimeExpenses` was NOT added to `qTotalExpenses`
4. ❌ **RESULT**: Total expenses column doesn't reflect one-time expenses

### Comparison with Annual View

The annual view calculation (line 646) correctly includes one-time expenses:

```kotlin
val totalExpenses = needsAdjusted + wantsAdjusted + healthcareAdjusted + 
                    propertyTaxAdjusted + annualTaxDue + annualOneTimeExpenses
```

But the quarterly view was inconsistent!

---

## Solution Implemented

### Change Made to SimulationEngine.kt

**Line 576**: Added `qOneTimeExpenses` to the total expenses calculation:

```kotlin
// BEFORE:
val qTotalExpenses = qNeeds + qWants + qHealth + qTax + qProp

// AFTER:
val qTotalExpenses = qNeeds + qWants + qHealth + qTax + qProp + qOneTimeExpenses
```

### Impact

This single-line change ensures:
- ✅ Quarterly total expenses include one-time expenses
- ✅ Quarterly income gap calculation is correct (uses total expenses)
- ✅ Consistency between annual and quarterly views
- ✅ Accurate representation of spending in each quarter

---

## Related Impact

### Income Gap Calculation

Since `qIncomeGap` is calculated as:
```kotlin
val qIncomeGap = qTotalExpenses - qPassiveIncome
```

The income gap was **incorrectly calculated** before this fix. Large one-time expenses would not affect the quarterly income gap metric, which could lead to:
- ❌ Misleading quarterly income gap values
- ❌ Incorrect quarterly SB cap calculations (based on income gap)
- ❌ Potential issues with spending strategy triggers

### SB Cap Calculation

The quarterly SB cap is calculated using expenses:
```kotlin
sbCap = ((qNeeds + (qWants * 0.5) + qHealth + qTax + qProp) - qPassiveIncome) * 2.0
```

**Note**: This SB cap calculation also doesn't include one-time expenses! This might be intentional (one-time expenses shouldn't affect the ongoing spend bucket cap), but it should be verified.

---

## Testing

### Verification Steps

1. ✅ Code compiles without errors
2. ✅ Build successful
3. ⏳ Pending: Run simulation with cash expense
4. ⏳ Pending: Verify quarterly total expenses = regular expenses + one-time expenses
5. ⏳ Pending: Verify quarterly income gap reflects one-time expenses
6. ⏳ Pending: Test with loan expenses
7. ⏳ Pending: Test with down payments

### Test Cases Affected

This fix affects:
- **T029**: Verify Spend Bucket Deduction in Q1 (original reporter) ⭐
- **T027**: Age-Based Cash Expense
- **T028**: Calendar Year Cash Expense
- **T030**: Verify Spending Strategy Triggers
- **T040-T043**: Multiple expenses tests
- **T055-T060c**: All loan tests

All these tests should now show correct total expenses in quarterly view.

---

## Impact Assessment

### Severity: HIGH

**Why HIGH?**
- Affects calculation accuracy (total expenses, income gap)
- User-visible data inconsistency
- Could lead to incorrect financial planning decisions
- Affects quarterly metrics used for simulation logic

**NOT CRITICAL because:**
- Annual view was correct (users could see accurate annual totals)
- Balances were still calculated correctly
- Simulation failure logic was not affected

### Scope

**Affected Components**:
- ✅ Backend: SimulationEngine.kt (fixed)
- ✅ Quarterly total expenses calculation
- ✅ Quarterly income gap calculation
- ✅ Frontend display (will automatically show correct values)

**NOT Affected**:
- ✅ Annual view calculations (already correct)
- ✅ Account balances (calculated correctly)
- ✅ One-time expense display (already correct)
- ✅ Spend bucket deductions (already correct)

---

## Lessons Learned

### What Went Wrong

1. **Inconsistency Between Views**: Annual and quarterly calculations used different formulas
2. **Missing Test Coverage**: No tests verified quarterly total expenses
3. **Copy-Paste Error**: When quarterly view was added, the total expenses formula wasn't updated to match annual

### Prevention for Future

1. ✅ **Consistency Checks**: When adding new expense types, verify ALL calculations include them
2. ✅ **Test Both Views**: Always test both annual and quarterly views for consistency
3. ✅ **Formula Documentation**: Document the expected formula for total expenses
4. ✅ **Code Review**: Check that quarterly and annual calculations match

---

## Additional Findings

### Question: Should SB Cap Include One-Time Expenses?

Currently, the quarterly SB cap calculation does NOT include one-time expenses:
```kotlin
sbCap = ((qNeeds + (qWants * 0.5) + qHealth + qTax + qProp) - qPassiveIncome) * 2.0
```

This might be intentional because:
- ✅ SB cap represents ongoing spending needs
- ✅ One-time expenses are irregular, not recurring
- ✅ SB cap is meant to buffer regular expenses

**Recommendation**: Document this design decision in code comments.

---

## Related Issues

- **First Bug**: One-time expenses missing from quarterly view (BUG-QUARTERLY-VIEW-MISSING-EXPENSES.md)
- **Second Bug**: This issue (total expenses calculation)

Both bugs show the quarterly view needs more thorough testing!

---

## Sign-Off

**Fixed By**: GitHub Copilot  
**Reviewed By**: _________  
**Tested By**: _________  
**Date Fixed**: January 23, 2026  
**Build**: 1.1.21 at 2026-01-23 09:38:42  
**Status**: ✅ Fixed, Pending Verification Testing

---

## Files Modified

1. `/api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt`
   - Line 576: Added `qOneTimeExpenses` to `qTotalExpenses` calculation

**Total Changes**: 1 line modified

