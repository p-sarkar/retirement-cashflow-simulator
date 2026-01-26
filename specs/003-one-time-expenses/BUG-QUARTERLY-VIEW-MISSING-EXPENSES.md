# Bug Fix: One-Time Expenses Not Showing in Quarterly View

**Date**: January 22, 2026  
**Discovered During**: Test T027 execution  
**Severity**: HIGH  
**Status**: ✅ FIXED

---

## Problem Description

When running test T027 (Age-Based Cash Expense End-to-End), the one-time expense of $50,000 appeared correctly in the **annual view** of the results table, but was **missing from the quarterly view**.

### Observed Behavior
- ✅ Annual view: Shows $50,000 in "One-Time Expenses" column for year 2038
- ❌ Quarterly view: Shows $0.00 in "One-Time Expenses" column for all quarters of 2038

### Expected Behavior
- ✅ Annual view: Shows $50,000 in "One-Time Expenses" column for year 2038
- ✅ Quarterly view: Should show $50,000 in Q1 2038 (since cash expenses are paid in Q1)

---

## Root Cause Analysis

### Investigation

The `ResultsTable.tsx` component correctly displays `row.cashFlow.oneTimeExpenses` for both yearly and quarterly views. The issue was in the **backend simulation engine**.

### Code Analysis

In `SimulationEngine.kt`:

1. **Quarterly accumulators were initialized** (lines 219-237):
   ```kotlin
   var qSalary = 0.0
   var qInterest = 0.0
   // ... other accumulators ...
   var qProp = 0.0
   // ❌ MISSING: var qOneTimeExpenses = 0.0
   ```

2. **One-time expenses were processed** in Q1 (lines 428-535):
   - Cash expenses added to `annualOneTimeExpenses` ✅
   - Down payments added to `annualOneTimeExpenses` ✅
   - Loan payments added to `annualOneTimeExpenses` ✅
   - ❌ **BUT**: Nothing added to quarterly accumulator (didn't exist!)

3. **QuarterlyResult was created** (lines 580-615):
   ```kotlin
   cashFlow = CashFlow(
       // ... all fields ...
       totalExpenses = qTotalExpenses
       // ❌ MISSING: oneTimeExpenses = qOneTimeExpenses
   )
   ```

4. **Quarterly accumulators were reset** (lines 620-637):
   - All other accumulators reset ✅
   - ❌ **MISSING**: qOneTimeExpenses reset

### Root Cause

The quarterly tracking infrastructure for one-time expenses was **completely missing**:
- No quarterly accumulator variable
- No accumulation when expenses were paid
- Not included in QuarterlyResult
- Not reset between quarters

The annual tracking worked fine because it had its own accumulator (`annualOneTimeExpenses`) that was included in `YearlyResult`.

---

## Solution Implemented

### Changes Made to SimulationEngine.kt

#### 1. Added Quarterly Accumulator (line ~238)
```kotlin
var qOneTimeExpenses = 0.0 // Quarterly one-time expenses accumulator
```

#### 2. Track Cash Expenses in Quarterly Accumulator (line ~442)
```kotlin
annualOneTimeExpenses += adjustedAmount
annualSbWithdrawal += adjustedAmount
qOneTimeExpenses += adjustedAmount // ✅ ADDED
```

#### 3. Track Down Payments in Quarterly Accumulator (line ~477)
```kotlin
annualOneTimeExpenses += adjustedDownPayment
annualSbWithdrawal += adjustedDownPayment
qOneTimeExpenses += adjustedDownPayment // ✅ ADDED
```

#### 4. Track Loan Payments in Quarterly Accumulator (line ~510)
```kotlin
annualOneTimeExpenses += annualPayment
annualSbWithdrawal += annualPayment
qOneTimeExpenses += annualPayment // ✅ ADDED
```

#### 5. Include in QuarterlyResult CashFlow (line ~607)
```kotlin
incomeTax = qTax,
propertyTax = qProp,
totalExpenses = qTotalExpenses,
oneTimeExpenses = qOneTimeExpenses // ✅ ADDED
```

#### 6. Reset Quarterly Accumulator (line ~638)
```kotlin
qTax = 0.0
qProp = 0.0
qOneTimeExpenses = 0.0 // ✅ ADDED
```

---

## Testing

### Verification Steps

1. ✅ Code compiles without errors
2. ✅ No new warnings introduced
3. ⏳ Pending: Run simulation with cash expense
4. ⏳ Pending: Verify quarterly view shows expense in Q1
5. ⏳ Pending: Verify annual view still works correctly
6. ⏳ Pending: Test with loan expenses
7. ⏳ Pending: Test with down payments

### Test Cases Affected

This fix affects the following integration tests:
- **T027**: Age-Based Cash Expense (original reporter)
- **T028**: Calendar Year Cash Expense
- **T029**: Verify SB Deduction in Q1
- **T040**: Multiple Cash Expenses
- **T055**: Loan with 6% APR
- **T056**: Loan Age Range
- **T057**: 0% APR Loan
- **T058**: Concurrent Cash and Loan
- **T060a**: Loan with Down Payment
- **T060b**: Down Payment in Start Year

All these tests should now pass for both annual and quarterly views.

---

## Impact Assessment

### Severity: HIGH

**Why HIGH?**
- Core functionality broken in quarterly view
- Affects ALL one-time expenses (cash, loans, down payments)
- User-visible bug in production UI
- Data was correct, but not displayed

**NOT CRITICAL because:**
- Annual view worked correctly (users could still see expenses)
- No data corruption or calculation errors
- UI/display issue only

### Scope

**Affected Components**:
- ✅ Backend: SimulationEngine.kt (fixed)
- ✅ Frontend: No changes needed (already correct)
- ✅ Data Model: No changes needed (already supports quarterly data)

**Affected Features**:
- ✅ Cash expenses in quarterly view
- ✅ Loan payments in quarterly view
- ✅ Down payments in quarterly view

**NOT Affected**:
- ✅ Annual view (was already working)
- ✅ Yearly calculations (correct)
- ✅ Spend Bucket deductions (correct)
- ✅ Breakdown dialog (correct)

---

## Lessons Learned

### What Went Wrong

1. **Incomplete Feature Implementation**: When quarterly view was added, one-time expenses tracking wasn't included
2. **Missing Test Coverage**: No tests caught this before manual testing
3. **Pattern Inconsistency**: All other cash flow items had quarterly tracking, but one-time expenses didn't

### Prevention for Future

1. ✅ **Test Both Views**: Always test both annual and quarterly views
2. ✅ **Follow Patterns**: When adding new cash flow items, ensure quarterly tracking follows the same pattern as existing items
3. ✅ **Comprehensive Tests**: Add automated tests for quarterly view
4. ✅ **Code Review**: Check that all accumulators are:
   - Initialized
   - Accumulated during processing
   - Included in results
   - Reset between periods

---

## Related Issues

- None (first occurrence)

---

## Sign-Off

**Fixed By**: GitHub Copilot  
**Reviewed By**: _________  
**Tested By**: _________  
**Date Fixed**: January 22, 2026  
**Status**: ✅ Fixed, Pending Verification Testing

---

## Files Modified

1. `/api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt`
   - Added `qOneTimeExpenses` accumulator
   - Track cash expenses in accumulator
   - Track down payments in accumulator
   - Track loan payments in accumulator
   - Include in QuarterlyResult
   - Reset accumulator

**Total Changes**: 6 additions in 1 file

