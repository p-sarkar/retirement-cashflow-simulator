# Fix: Include One-Time Expenses in Breakdown Total Expenses

**Date**: January 23, 2026  
**Component**: BreakdownGenerator.kt  
**Type**: Bug Fix  
**Status**: ✅ FIXED

---

## Problem Description

In the Computation Breakdown dialog (accessed via the 🔍 icon), the **Expenses** section showed individual expense categories but the **Total Expenses** calculation did not include one-time expenses. This caused the displayed expenses to not add up to the total.

### Observed Behavior

In the Expenses section breakdown:
```
Needs:              $45,000
Wants:              $22,000
Healthcare:         $6,000
Property Tax:       $11,000
Income Tax:         $16,000
Total Expenses:     $150,000  ← Doesn't match sum above ($100,000)
```

The displayed total was correct ($150,000), but the individual items shown only added up to $100,000 because the $50,000 one-time expense was missing from the breakdown display.

### Expected Behavior

In the Expenses section breakdown:
```
Needs:              $45,000
Wants:              $22,000
Healthcare:         $6,000
Property Tax:       $11,000
Income Tax:         $16,000
One-Time Expenses:  $50,000  ← Now shown as its own line!
Total Expenses:     $150,000  ← Matches sum above
```

---

## Root Cause

In `BreakdownGenerator.kt`, the `createExpensesSection()` function's "Total Expenses" step (line 245) included only:
- needs
- wants
- healthcare
- propertyTax
- incomeTax

But was **missing** `oneTimeExpenses` from the values map!

The actual `result.cashFlow.totalExpenses` value was correct (it included one-time expenses), but the breakdown display didn't show one-time expenses as one of the components, making it impossible to verify the total.

---

## Solution Implemented

### Change Made to BreakdownGenerator.kt

**Lines 245-263**: Added a separate "One-Time Expenses" step before the Total Expenses step:

```kotlin
// BEFORE:
steps.add(ComputationStep(
    label = "Income Tax",
    formula = "priorYearTaxableIncome × effectiveTaxRate",
    values = mapOf("taxRate" to config.rates.incomeTax),
    result = result.cashFlow.incomeTax,
    explanation = "Tax on prior year's taxable income at effective rate"
))
steps.add(ComputationStep(
    label = "Total Expenses",
    formula = "Needs + Wants + Healthcare + PropertyTax + IncomeTax",
    values = mapOf(
        "needs" to result.cashFlow.needs,
        "wants" to result.cashFlow.wants,
        "healthcare" to result.cashFlow.healthcare,
        "propertyTax" to result.cashFlow.propertyTax,
        "incomeTax" to result.cashFlow.incomeTax
    ),
    result = result.cashFlow.totalExpenses,
    explanation = "Sum of all expense categories"
))

// AFTER:
steps.add(ComputationStep(
    label = "Income Tax",
    formula = "priorYearTaxableIncome × effectiveTaxRate",
    values = mapOf("taxRate" to config.rates.incomeTax),
    result = result.cashFlow.incomeTax,
    explanation = "Tax on prior year's taxable income at effective rate"
))
steps.add(ComputationStep(
    label = "One-Time Expenses",  // ✅ NEW STEP ADDED
    formula = "sum of all one-time expenses",
    values = mapOf(),
    result = result.cashFlow.oneTimeExpenses,
    explanation = "Total of all one-time expenses (cash expenses and loan payments) for this year"
))
steps.add(ComputationStep(
    label = "Total Expenses",
    formula = "Needs + Wants + Healthcare + PropertyTax + IncomeTax + OneTimeExpenses",
    values = mapOf(
        "needs" to result.cashFlow.needs,
        "wants" to result.cashFlow.wants,
        "healthcare" to result.cashFlow.healthcare,
        "propertyTax" to result.cashFlow.propertyTax,
        "incomeTax" to result.cashFlow.incomeTax,
        "oneTimeExpenses" to result.cashFlow.oneTimeExpenses  // ✅ ALSO ADDED HERE
    ),
    result = result.cashFlow.totalExpenses,
    explanation = "Sum of all expense categories including one-time expenses"  // ✅ UPDATED
))
```

### What Changed

1. ✅ Added a NEW separate step for "One-Time Expenses" that displays as its own row
2. ✅ Added `"oneTimeExpenses" to result.cashFlow.oneTimeExpenses` to the Total Expenses values map
3. ✅ Updated Total Expenses formula to include `+ OneTimeExpenses`
4. ✅ Updated Total Expenses explanation to mention "including one-time expenses"

---

## Impact

### Before Fix

**Expenses Breakdown Section**:
- Shows: Needs, Wants, Healthcare, Property Tax, Income Tax
- Total shown: Correct value including one-time expenses
- **Problem**: Individual items don't add up to total (confusing!)

**One-Time Expenses Breakdown Section** (separate):
- Shows: Detailed list of individual one-time expenses
- **Problem**: User had to mentally connect this to the Expenses section

### After Fix

**Expenses Breakdown Section**:
- Shows: Needs, Wants, Healthcare, Property Tax, Income Tax, **One-Time Expenses** (as separate line)
- Total shown: Correct value
- ✅ **Fixed**: Individual items now add up to total!
- ✅ One-time expenses appear as their own row in the breakdown table

**One-Time Expenses Breakdown Section** (separate):
- Shows: Detailed list of individual one-time expenses (unchanged)
- ✅ Still provides detail view of what makes up the one-time expenses total

### User Experience Improvement

**Before**:
1. User sees Total Expenses = $150,000
2. User adds up shown items: $45k + $22k + $6k + $11k + $16k = $100k
3. User is confused: "Where's the other $50k??"
4. User has to remember there's a separate One-Time Expenses section
5. User mentally adds the two together

**After**:
1. User sees Total Expenses = $150,000
2. User adds up shown items: $45k + $22k + $6k + $11k + $16k + **$50k** = $150k
3. ✅ **Everything adds up!**
4. User can still click to see One-Time Expenses detail if needed

---

## Design Decision: Keep Separate One-Time Expenses Section

The separate "One-Time Expenses" breakdown section (which shows the detailed list of individual one-time expenses with names and types) is **intentionally kept as-is** per user request:

> "the One-time Expenses breakdown should stay as-is"

This provides:
- ✅ Summary view in Expenses section (total one-time expenses)
- ✅ Detail view in separate section (individual expenses with names)

**Example**:

**Expenses Section**:
```
One-Time Expenses: $63,323  ← Total shown here
```

**One-Time Expenses Section** (separate):
```
New Car (Cash):          $50,000
Car Loan (Loan Payment): $13,323
```

This two-level approach gives users both a quick summary and detailed breakdown when needed.

---

## Testing

### Verification Steps

1. ✅ Code compiles without errors
2. ✅ Build successful (2026-01-23 12:08:43)
3. ⏳ Pending: Run simulation with one-time expense
4. ⏳ Pending: Open breakdown dialog for year with expense
5. ⏳ Pending: Expand "Expenses" section
6. ⏳ Pending: Verify "One-Time Expenses" appears in values
7. ⏳ Pending: Verify all shown values add up to Total Expenses
8. ⏳ Pending: Verify separate "One-Time Expenses" section still exists

### Test Scenarios

**Scenario 1: Year with Cash Expense**
- Add $50,000 cash expense
- Open breakdown
- Expected: Expenses section shows oneTimeExpenses: $50,000 in values

**Scenario 2: Year with Loan Payment**
- Add loan with annual payment of $13,323
- Open breakdown
- Expected: Expenses section shows oneTimeExpenses: $13,323 in values

**Scenario 3: Year with Multiple Expenses**
- Add cash expense $50,000 + loan payment $13,323
- Open breakdown
- Expected: Expenses section shows oneTimeExpenses: $63,323 in values
- Expected: Separate section shows both expenses individually

**Scenario 4: Year without One-Time Expenses**
- No one-time expenses
- Open breakdown
- Expected: Expenses section shows oneTimeExpenses: $0 in values
- Expected: No separate One-Time Expenses section

---

## Related Fixes

This is the **third fix** for one-time expenses in breakdown/results display:

1. **Fix #1** (2026-01-22): One-time expenses missing from quarterly view
   - Added `qOneTimeExpenses` tracking

2. **Fix #2** (2026-01-23): One-time expenses not in quarterly total expenses
   - Added `qOneTimeExpenses` to total calculation

3. **Fix #3** (2026-01-23): One-time expenses not shown in breakdown total ⭐ (this fix)
   - Added `oneTimeExpenses` to breakdown values display

**Pattern**: All three fixes involved ensuring one-time expenses are properly included in totals and displays!

---

## Verification

✅ **Build Status**: SUCCESSFUL  
✅ **Compilation**: No errors  
✅ **Warnings**: No new warnings  
✅ **Build Time**: 2026-01-23 12:14:14

---

**Fixed By**: GitHub Copilot  
**Date**: January 23, 2026  
**Build**: 1.1.21 at 2026-01-23 12:14:14  
**Status**: ✅ Fixed, Ready for Testing

---

## Files Modified

1. `/api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt`
   - Lines 245-250: Added new "One-Time Expenses" step as separate line item
   - Line 252: Updated Total Expenses formula to include OneTimeExpenses
   - Line 259: Added oneTimeExpenses to Total Expenses values map
   - Line 262: Updated Total Expenses explanation text

**Total Changes**: 1 new step added, 3 lines modified in 1 file

