# US4 Integration Test Guide - Edit and Remove Expenses

**Date**: 2026-01-21  
**Feature**: One-Time Expenses - User Story 4  
**Goal**: Validate edit and remove functionality for one-time expenses

## Prerequisites

- All servers running (frontend on :5173, backend on :8000, api-server on :8090)
- Browser open to http://localhost:5173
- US1, US2, and US3 functionality verified working

## Test Setup

Start with a fresh simulation to ensure clean state.

---

## T066: Test Modifying Cash Expense Amount

**Test Case**: Modify a cash expense amount and verify results update correctly

### Steps

1. **Add initial cash expense**:
   - Type: Cash
   - Name: "New Car"
   - Amount: $50,000
   - When: Age 67

2. **Run simulation**:
   - Click "Run Simulation"
   - Note the one-time expense amount in year corresponding to age 67

3. **Edit the cash expense**:
   - Change Amount from $50,000 to $60,000
   - Do NOT click "Run Simulation" yet
   - Verify the amount field updates to $60,000

4. **Re-run simulation**:
   - Click "Run Simulation"
   - Verify the one-time expense in results table now shows $60,000
   - Verify Spend Bucket balance decreased by $60,000 (not $50,000)

### Expected Results

✅ Amount field accepts the change  
✅ New simulation reflects $60,000 expense  
✅ Previous simulation data ($50,000) is replaced  
✅ No validation errors

---

## T067: Test Modifying Loan APR with Monthly Payment Recalculation

**Test Case**: Modify loan APR and verify monthly payment recalculates automatically

### Steps

1. **Add loan expense**:
   - Type: Loan
   - Name: "Home Renovation"
   - Principal: $100,000
   - Down Payment: $0
   - APR: 6%
   - Term: 10 years
   - When: Age 65

2. **Note initial monthly payment**:
   - Monthly payment should display: ~$1,110.21

3. **Edit APR**:
   - Change APR from 6% to 5%
   - Do NOT click "Run Simulation"
   - Observe the Monthly Payment field

4. **Verify recalculation**:
   - Monthly payment should automatically update to ~$1,060.66
   - No page reload required

5. **Run simulation**:
   - Click "Run Simulation"
   - Verify one-time expenses column shows annual total of ~$12,728 (12 × $1,060.66)

### Expected Results

✅ Monthly payment recalculates immediately when APR changes  
✅ No need to click "Run Simulation" to see new monthly payment  
✅ Simulation results reflect new APR when run  
✅ Annual totals = 12 × monthly payment

**Reference Calculations**:
- 6% APR: $1,110.21/month → $13,322.52/year
- 5% APR: $1,060.66/month → $12,727.92/year

---

## T068: Test Removing One Expense from Multiple Expenses

**Test Case**: Remove one expense from a set of multiple expenses

### Steps

1. **Add multiple expenses**:
   - Expense 1: Cash, "Car", $50,000, Age 67
   - Expense 2: Cash, "Medical", $20,000, Age 70
   - Expense 3: Cash, "Travel", $15,000, Age 72

2. **Run initial simulation**:
   - Click "Run Simulation"
   - Verify all three expenses appear in respective years

3. **Remove middle expense (Medical)**:
   - Click the red trash icon on "Medical" expense
   - **Confirm deletion** in the dialog that appears
   - Verify "Medical" expense disappears from the form

4. **Run simulation again**:
   - Click "Run Simulation"
   - Verify results:
     - Age 67 year: Shows $50,000 (Car)
     - Age 70 year: Shows $0 (Medical removed)
     - Age 72 year: Shows $15,000 (Travel)

### Expected Results

✅ Confirmation dialog appears before deletion  
✅ Expense removed from form after confirmation  
✅ Remaining expenses still visible and intact  
✅ Simulation results no longer include removed expense  
✅ Other expenses unaffected

---

## T069: Test Removing All Expenses

**Test Case**: Remove all expenses and verify zero amounts in results

### Steps

1. **Start with multiple expenses**:
   - Add 3 cash expenses (any amounts, any years)
   - Run simulation to verify they all appear

2. **Remove all expenses one by one**:
   - Click delete on expense 1 → Confirm
   - Click delete on expense 2 → Confirm
   - Click delete on expense 3 → Confirm

3. **Verify form state**:
   - Form should show "No one-time expenses added" message
   - "Add Expense" button still visible

4. **Run simulation with no expenses**:
   - Click "Run Simulation"
   - Verify one-time expenses column shows $0.00 for all years
   - Verify simulation still completes successfully

### Expected Results

✅ Each deletion requires confirmation  
✅ After last deletion, empty state message appears  
✅ Can still add new expenses after removing all  
✅ Simulation runs successfully with zero expenses  
✅ All years show $0.00 in one-time expenses column

---

## Additional Verification Tests

### Test: Cancel Deletion (New functionality from T065)

1. Add one expense
2. Click delete icon
3. **Click "Cancel"** in confirmation dialog
4. Verify expense is still present in the form

**Expected**: ✅ Expense not deleted, form unchanged

### Test: Edit Expense Name

1. Add expense with name "Test"
2. Change name to "Updated Test"
3. Run simulation
4. Verify expense appears with new name in breakdown

**Expected**: ✅ Name updates successfully

### Test: Edit Loan Down Payment

1. Add loan with principal $100,000, down payment $0, 6% APR, 10 years
2. Note monthly payment (~$1,110.21)
3. Change down payment to $20,000
4. Verify monthly payment recalculates for financed amount $80,000 (~$888.17)

**Expected**: ✅ Monthly payment reflects financed amount (principal - down payment)

### Test: Edit Timing (Age to Year)

1. Add cash expense at Age 67
2. Change "When" dropdown from "Age" to "Year"
3. Enter year 2035
4. Run simulation
5. Verify expense appears in year 2035

**Expected**: ✅ Timing type change works correctly

---

## Success Criteria

For US4 to be considered complete and passing, ALL of the following must be true:

- [x] T066: Cash expense amount can be modified and results update ✅
- [x] T067: Loan APR changes trigger automatic monthly payment recalculation ✅
- [x] T068: Individual expenses can be removed from multiple expenses ✅
- [x] T069: All expenses can be removed, simulation shows $0 ✅
- [x] T065: Confirmation dialog appears before deletion (prevents accidental removal) ✅

## Notes

- **Confirmation Dialog**: This is a new safety feature added in T065. It prevents users from accidentally deleting expenses with one click.
- **Live Recalculation**: Monthly payment updates happen immediately without requiring a form submission.
- **State Management**: Form state is managed independently from simulation results until "Run Simulation" is clicked.

---

## Troubleshooting

### Issue: Monthly payment not recalculating
**Fix**: Verify `getMonthlyPaymentDisplay` function is being called on field changes

### Issue: Confirmation dialog not appearing
**Fix**: Verify Dialog component is rendered and `deleteConfirmOpen` state is working

### Issue: Removed expense still appears in results
**Fix**: Ensure simulation was re-run after deletion, old results are cached until new simulation

### Issue: All expenses disappear when editing one
**Fix**: Check that `handleFieldChange` is updating only the specific expense by ID

---

## Completion Checklist

After running all tests, verify:

- [ ] Can add multiple expenses (US1/US2)
- [ ] Can edit any expense field (US4)
- [ ] Edits reflect immediately in form (US4)
- [ ] Monthly payment recalculates on loan field changes (US4)
- [ ] Can remove individual expenses (US4)
- [ ] Confirmation dialog prevents accidental deletion (US4)
- [ ] Can remove all expenses (US4)
- [ ] Simulation runs correctly with edited expenses (US4)
- [ ] No JavaScript console errors
- [ ] No validation errors for valid inputs

**If all items checked**: ✅ **US4 COMPLETE**

