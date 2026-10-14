# Integration Test Procedures - One-Time Expenses

**Feature**: 003-one-time-expenses  
**Purpose**: Manual browser testing procedures for integration tests  
**Status**: Template for test execution

---

## Test Execution Instructions

1. Use this document as a checklist during testing
2. Fill in "Actual Results" for each test
3. Check Pass/Fail status
4. Add screenshots in `/docs/test-evidence/` folder
5. Document any bugs discovered
6. Update test status in tasks.md when complete

---

## User Story 1: Single Cash Expense (4 Tests)

### T027: Age-Based Cash Expense End-to-End

**Priority**: CRITICAL  
**Requirements**: FR-009, FR-025, FR-010, FR-011

**Pre-conditions**:
- Application running on localhost
- Fresh browser session
- Database cleared or using test database

**Test Steps**:
1. Open application in browser
2. Navigate to simulation form
3. Scroll to "One-Time Expenses" section
4. Click "Add Expense"
5. Configure cash expense:
   - Type: Cash
   - Name: "New Car"
   - Amount: $50,000
   - Timing: Age
   - Age: 67
6. Set simulation parameters:
   - Current Age: 55
   - Current Year: 2026
   - Retirement Age: 65
7. Click "Run Simulation"
8. Review results table (Annual view)
9. Find year when age = 67 (should be 2038)
10. Check "One-Time Expenses" column (should show $50,000)
11. Switch to Quarterly view
12. Find Q1 2038 row
13. Check "One-Time Expenses" column (should show $50,000)
14. Check Q2, Q3, Q4 2038 (should show $0)
15. Open breakdown dialog for year 2038

**Expected Results**:
- ✅ Expense form accepts all inputs without errors
- ✅ Results table displays
- ✅ **Annual View**: Year 2038 row shows $50,000 in "One-Time Expenses" column
- ✅ **Quarterly View**: Q1 2038 shows $50,000 in "One-Time Expenses" column
- ✅ **Quarterly View**: Q2, Q3, Q4 2038 show $0 in "One-Time Expenses" column
- ✅ Spend Bucket balance shows decrease in Q1 2038
- ✅ Breakdown dialog shows "New Car" cash expense
- ✅ No console errors
- ✅ Age-to-year conversion: 2026 + (67-55) = 2038

**Known Issues**:
- 🐛 **FIXED 2026-01-22**: One-time expenses were not appearing in quarterly view (only annual view). This has been fixed in SimulationEngine.kt. See BUG-QUARTERLY-VIEW-MISSING-EXPENSES.md for details.

**Actual Results**:
```
[To be filled during test execution]
Date Tested: ___________
Tester: ___________
Browser: ___________
Pass: [ ]  Fail: [ ]
```

**Evidence**: `test-evidence/T027-*.png`

---

### T028: Calendar Year Cash Expense End-to-End

**Priority**: CRITICAL  
**Requirements**: FR-009, FR-004

**Pre-conditions**:
- Application running
- Fresh browser session

**Test Steps**:
1. Navigate to simulation form
2. Add cash expense:
   - Type: Cash
   - Name: "Home Renovation"
   - Amount: $75,000
   - Timing: Year
   - Year: 2035
3. Set current year: 2026
4. Click "Run Simulation"
5. Find year 2035 in results
6. Check "One-Time Expenses" column

**Expected Results**:
- ✅ Expense appears in form
- ✅ Year 2035 shows $75,000 in one-time expenses
- ✅ SB balance decreases in Q1 2035
- ✅ Breakdown shows "Home Renovation"

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

**Evidence**: `test-evidence/T028-*.png`

---

### T029: Verify Spend Bucket Deduction in Q1

**Priority**: HIGH  
**Requirements**: FR-010, FR-023

**Pre-conditions**:
- Application running
- Test simulation with cash expense

**Test Steps**:
1. Add cash expense for specific year (e.g., $50,000)
2. Run simulation
3. Switch to Quarterly view
4. Find Q1 of the expense year
5. Check "One-Time Expenses" column (should show expense amount)
6. Check "Total Expenses" column (should include expense amount)
7. Verify SB balance at start vs end of Q1
8. Open breakdown dialog
9. Verify timing shows Q1

**Expected Results**:
- ✅ Cash expense paid in Q1 (first quarter)
- ✅ **Quarterly View Q1**: "One-Time Expenses" column shows expense amount (e.g., $50,000)
- ✅ **Quarterly View Q1**: "Total Expenses" column includes one-time expense (regular expenses + $50,000)
- ✅ **Quarterly View Q2-Q4**: "One-Time Expenses" shows $0
- ✅ SB balance reflects deduction in Q1
- ✅ Breakdown indicates Q1 timing
- ✅ No partial payment across quarters

**Known Issues**:
- 🐛 **FIXED 2026-01-23**: One-time expenses were not included in quarterly "Total Expenses" calculation. This has been fixed in SimulationEngine.kt. See BUG-QUARTERLY-TOTAL-EXPENSES.md for details.

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

### T030: Verify Spending Strategy Triggers When SB Insufficient

**Priority**: CRITICAL  
**Requirements**: FR-026, FR-022

**Pre-conditions**:
- Application running
- Need to create scenario where expense > SB balance

**Test Steps**:
1. Create simulation with:
   - Low initial SB balance (e.g., $10,000)
   - High regular expenses
   - Cash expense of $100,000 at early year
2. Run simulation
3. Check year where expense occurs
4. Verify spending strategy was triggered
5. Check CBB and TBA withdrawals
6. Verify expense was fully paid

**Expected Results**:
- ✅ Expense exceeds SB balance
- ✅ Spending strategy automatically triggered
- ✅ Funds drawn from CBB/TBA to cover shortfall
- ✅ Expense fully paid (no partial payment)
- ✅ SB does not go negative
- ✅ Breakdown shows strategy execution

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

## User Story 2: Multiple Cash Expenses (4 Tests)

### T040: Test 3 Expenses in Different Years

**Priority**: HIGH  
**Requirements**: FR-001, FR-002

**Pre-conditions**:
- Application running
- Fresh simulation

**Test Steps**:
1. Add 3 cash expenses:
   - Expense 1: "Car 1" - $40,000 at age 60
   - Expense 2: "Vacation" - $15,000 in year 2035
   - Expense 3: "Medical" - $25,000 at age 70
2. Set current age: 55, current year: 2026
3. Run simulation
4. Check results for years: 2031, 2035, 2041

**Expected Results**:
- ✅ All 3 expenses appear in form
- ✅ Year 2031: $40,000
- ✅ Year 2035: $15,000
- ✅ Year 2041: $25,000
- ✅ Each expense in correct year

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

### T041: Test Expenses Visible in Main Breakdown Dialog

**Priority**: HIGH  
**Requirements**: FR-032-v2, FR-033-v2

**Pre-conditions**:
- Simulation with one-time expenses

**Test Steps**:
1. Run simulation with expenses
2. Find year with expense
3. Click 🔍 icon to open breakdown dialog
4. Look for "One-Time Expenses" section

**Expected Results**:
- ✅ Breakdown dialog opens
- ✅ "One-Time Expenses" section visible
- ✅ Expense details shown
- ✅ Name, amount, type displayed

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

### T042: Verify One-Time Expenses Section in Breakdown

**Priority**: MEDIUM  
**Requirements**: FR-033-v2

**Pre-conditions**:
- Simulation with expenses in specific year

**Test Steps**:
1. Run simulation
2. Open breakdown for year WITH expense
3. Verify section exists
4. Open breakdown for year WITHOUT expense
5. Verify section doesn't appear or shows $0

**Expected Results**:
- ✅ Section appears only when expenses exist
- ✅ Section header is clear
- ✅ Section not shown for years without expenses

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

### T043: Verify Breakdown Displays Correct Details

**Priority**: HIGH  
**Requirements**: FR-033-v2, FR-034

**Pre-conditions**:
- Simulation with expense

**Test Steps**:
1. Add cash expense: "Test" - $50,000
2. Run simulation
3. Open breakdown
4. Check displayed information

**Expected Results**:
- ✅ Expense name matches ("Test")
- ✅ Amount is inflation-adjusted
- ✅ Type shown (Cash)
- ✅ All details readable

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

## User Story 3: Loan Expenses (9 Tests)

### T055: Test Loan with 6% APR - Verify Quarterly Payment Accuracy

**Priority**: CRITICAL  
**Requirements**: FR-016, FR-018

**Pre-conditions**:
- Application running
- External loan calculator for verification (e.g., bankrate.com)

**Test Steps**:
1. Add loan expense:
   - Name: "Test Loan"
   - Principal: $100,000
   - Down Payment: $0
   - APR: 6%
   - Term: 10 years
   - Start: 2026
2. Verify displayed quarterly payment
3. Calculate expected payment externally:
   - Financed: $100,000
   - Quarterly rate: 6% / 4 = 1.5%
   - Quarters: 10 × 4 = 40
   - Formula: Q = P[r(1+r)^n]/[(1+r)^n-1]
   - Expected: $3,330.63
4. Run simulation
5. Check year 2026 annual total
6. Verify: 4 × $3,330.63 = $13,322.52

**Expected Results**:
- ✅ Displayed quarterly payment: $3,330.63
- ✅ Annual payment in results: $13,322.52
- ✅ Payment matches external calculator (±$0.01)
- ✅ All 10 years show correct amount

**Actual Results**:
```
Date Tested: ___________
External Calculator Used: ___________
Calculated Quarterly Payment: $___________
App Displayed Payment: $___________
Difference: $___________
Pass: [ ]  Fail: [ ]
```

**Evidence**: `test-evidence/T055-*.png`

---

### T056: Test Loan Age Range (65-74)

**Priority**: HIGH  
**Requirements**: FR-020, FR-021

**Test Steps**:
1. Add loan:
   - Principal: $50,000
   - APR: 5%
   - Term: 10 years
   - Start: Age 65
2. Set current age: 55, current year: 2026
3. Run simulation
4. Verify payments in years when age 65-74 (2036-2045)

**Expected Results**:
- ✅ First payment: year when age = 65 (2036)
- ✅ Last payment: year when age = 74 (2045)
- ✅ All 10 years have payments
- ✅ No payments before age 65
- ✅ No payments after age 74

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

### T057: Test 0% APR Loan Uses Simple Division

**Priority**: HIGH  
**Requirements**: FR-017

**Test Steps**:
1. Add loan:
   - Principal: $20,000
   - APR: 0%
   - Term: 5 years
2. Check displayed quarterly payment
3. Calculate: $20,000 / (5 × 4) = $1,000
4. Run simulation
5. Verify annual payment: $1,000 × 4 = $4,000

**Expected Results**:
- ✅ Quarterly payment: $1,000.00 exactly
- ✅ Annual payment: $4,000.00
- ✅ No interest component
- ✅ Simple division formula used

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

### T058: Test Concurrent Cash and Loan Expenses

**Priority**: HIGH  
**Requirements**: FR-033-v2, FR-034

**Test Steps**:
1. Add both:
   - Cash: "Car Down Payment" - $10,000 in 2030
   - Loan: "Car Loan" - $40,000, 5%, 5yrs, start 2030
2. Run simulation
3. Check year 2030 results
4. Open breakdown dialog

**Expected Results**:
- ✅ 2030 shows both expenses summed
- ✅ Breakdown shows 2 entries:
  - Cash: $10,000 (down payment)
  - Loan Payment: $XXXX (annual)
- ✅ Types correctly labeled

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

### T059: Test Loan Extending Beyond Year 35

**Priority**: MEDIUM  
**Requirements**: Edge case handling

**Test Steps**:
1. Set current year: 2026
2. Add loan:
   - Start: 2030
   - Term: 20 years (ends 2049)
3. Run simulation (goes to 2061)
4. Check years 2030-2049

**Expected Results**:
- ✅ Payments show for all 20 years (2030-2049)
- ✅ All years within simulation period
- ✅ No errors
- ✅ Loan fully paid off by year 35

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

### T060: Verify Annual Totals = 4 Quarterly Payments

**Priority**: HIGH  
**Requirements**: FR-024, FR-020

**Test Steps**:
1. Add loan with known quarterly payment
2. Run simulation
3. Check any year during loan term
4. Verify: Annual Total = Quarterly Payment × 4

**Expected Results**:
- ✅ Math is correct
- ✅ Consistent across all loan years
- ✅ No rounding errors >$0.01

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

### T060a: Test Loan with Down Payment on Financed Amount

**Priority**: CRITICAL  
**Requirements**: FR-016a, FR-012a

**Test Steps**:
1. Add loan:
   - Principal: $50,000
   - Down Payment: $10,000
   - APR: 4%
   - Term: 5 years
2. Check quarterly payment display
3. Calculate expected:
   - Financed: $50,000 - $10,000 = $40,000
   - Expected quarterly: ~$2,213.79
4. Verify calculation based on $40k, not $50k

**Expected Results**:
- ✅ Quarterly payment calculated on $40,000
- ✅ Displayed payment: ~$2,213.79
- ✅ NOT calculated on full $50,000 principal

**Actual Results**:
```
Date Tested: ___________
Financed Amount: $___________
Quarterly Payment: $___________
Pass: [ ]  Fail: [ ]
```

---

### T060b: Test Down Payment in Start Year Breakdown

**Priority**: HIGH  
**Requirements**: FR-020a, FR-053a

**Test Steps**:
1. Add loan with down payment (from T060a)
2. Run simulation
3. Open breakdown for start year
4. Look for down payment entry

**Expected Results**:
- ✅ Breakdown shows 2 entries for start year:
  1. Down payment as Cash type
  2. Loan payment as Loan Payment type
- ✅ Both in same year
- ✅ Total includes both

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

### T060c: Test Loan with Down Payment = Principal

**Priority**: MEDIUM  
**Requirements**: Edge case validation

**Test Steps**:
1. Add loan:
   - Principal: $50,000
   - Down Payment: $50,000 (100%)
   - APR: 4%
   - Term: 5 years
2. Check quarterly payment
3. Run simulation

**Expected Results**:
- ✅ Quarterly payment: $0 (no financing needed)
- ✅ Only down payment shows (as cash expense)
- ✅ No loan payments in subsequent years
- ✅ Effectively a cash purchase

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

## User Story 4: Edit and Remove (4 Tests)

### T066: Test Modifying Cash Expense Amount

**Priority**: HIGH  
**Requirements**: FR-004

**Pre-conditions**:
- Simulation with cash expense

**Test Steps**:
1. Add cash expense: $50,000
2. Run simulation - note results
3. Edit expense amount to $60,000
4. Run simulation again
5. Compare results

**Expected Results**:
- ✅ Amount changes from $50k to $60k
- ✅ Results update correctly
- ✅ New amount shown in results
- ✅ SB deduction reflects new amount

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

### T067: Test Modifying Loan APR with Recalculation

**Priority**: CRITICAL  
**Requirements**: FR-004, FR-018

**Test Steps**:
1. Add loan: $100k, 6% APR, 10 years
2. Note quarterly payment (~$3,330.63)
3. Change APR to 5%
4. Check if quarterly payment updates
5. Expected new payment: ~$3,107.35
6. Run simulation - verify new payment used

**Expected Results**:
- ✅ Quarterly payment auto-recalculates
- ✅ New payment displayed immediately
- ✅ New payment: ~$3,107.35
- ✅ Simulation uses new payment

**Actual Results**:
```
Date Tested: ___________
Original Payment (6%): $___________
New Payment (5%): $___________
Pass: [ ]  Fail: [ ]
```

---

### T068: Test Removing One Expense

**Priority**: HIGH  
**Requirements**: FR-005

**Test Steps**:
1. Add 3 expenses
2. Run simulation - note results
3. Remove middle expense
4. Run simulation again
5. Verify only 2 expenses remain

**Expected Results**:
- ✅ Removed expense disappears from form
- ✅ Removed expense doesn't appear in results
- ✅ Other 2 expenses still work
- ✅ No errors

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

### T069: Test Removing All Expenses

**Priority**: MEDIUM  
**Requirements**: FR-005, FR-036

**Test Steps**:
1. Add multiple expenses
2. Remove all expenses one by one
3. Run simulation
4. Check one-time expenses column

**Expected Results**:
- ✅ All expenses removed
- ✅ Form shows no expenses
- ✅ Results column shows $0 or empty
- ✅ Simulation runs without errors

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

## Phase 7: Polish & Edge Cases (6 Tests)

### T078: Test 10+ Expenses for Performance

**Priority**: MEDIUM  
**Success Criterion**: SC-003

**Test Steps**:
1. Add 10 expenses (mix of cash and loans)
2. Run simulation
3. Measure time to complete
4. Check UI responsiveness

**Expected Results**:
- ✅ Simulation completes
- ✅ Time <30 seconds (per SC-004)
- ✅ UI remains responsive
- ✅ All expenses processed correctly

**Actual Results**:
```
Date Tested: ___________
Number of Expenses: ___________
Simulation Time: _____ seconds
Pass: [ ]  Fail: [ ]
```

---

### T079: Verify Breakdown Dialog Loads <1 Second

**Priority**: MEDIUM  
**Success Criterion**: SC-005

**Test Steps**:
1. Run simulation with multiple expenses
2. Click breakdown icon
3. Measure time to open
4. Repeat for different years

**Expected Results**:
- ✅ Dialog opens in <1 second
- ✅ Data loads immediately
- ✅ No lag or delay

**Actual Results**:
```
Date Tested: ___________
Load Time: _____ ms
Pass: [ ]  Fail: [ ]
```

---

### T080: Test Expense Exceeds SB Triggers Strategy

**Priority**: HIGH  
**Requirements**: FR-026

**Test Steps**:
1. Create scenario: Large expense, small SB
2. Run simulation
3. Verify spending strategy triggered
4. Check breakdown for strategy execution

**Expected Results**:
- ✅ Strategy triggered automatically
- ✅ Expense fully paid
- ✅ SB never negative
- ✅ CBB/TBA withdrawals shown

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

### T081: Test Loan Extending Beyond Year 35

**Priority**: LOW  
**Requirements**: Edge case handling

**Test Steps**:
1. Add loan starting late (e.g., 2055)
2. Long term (e.g., 15 years, ends 2070)
3. Run simulation (ends 2061)
4. Verify only payments 2055-2061 shown

**Expected Results**:
- ✅ Only payments within simulation period
- ✅ No errors
- ✅ No payments after year 35

**Actual Results**:
```
Date Tested: ___________
Pass: [ ]  Fail: [ ]
```

---

## Test Summary Template

```markdown
# Test Execution Summary

**Date Range**: ___________  
**Tester**: ___________  
**Environment**: ___________

## Results

| Test ID | Description | Status | Notes |
|---------|-------------|--------|-------|
| T027 | Age-based cash expense | [ ] Pass / [ ] Fail | |
| T028 | Calendar year cash expense | [ ] Pass / [ ] Fail | |
| ... | ... | ... | |

## Overall Results

- Total Tests: 27
- Passed: ____
- Failed: ____
- Pass Rate: ____%

## Critical Failures

[List any critical test failures here]

## Bugs Discovered

[List bugs found during testing]

## Recommendations

[Next steps based on test results]
```

---

**Document Status**: Template Ready for Execution  
**Created**: January 22, 2026  
**Last Updated**: January 22, 2026

