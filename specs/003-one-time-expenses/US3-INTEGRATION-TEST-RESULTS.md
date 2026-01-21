# US3 Integration Test Results - Loan with Amortization

**Date**: 2026-01-21  
**Feature**: One-Time Expenses - US3 (Add Loan with Amortization)  
**Test Environment**: 
- API Server: http://localhost:8090
- Frontend: http://localhost:5173
- Backend: http://localhost:8000

## Test Configuration

**Base Simulation Parameters**:
- Current Age: 60
- Retirement Age: 65
- Current Year: 2026
- Starting Balances:
  - Spend Bucket (SB): $50,000
  - Tax-Deferred (TDA): $500,000
  - Tax-Advantaged (TBA): $300,000
  - Tax-Free (TFA): $200,000
  - Conservative Bond Bucket (CBB): $100,000

---

## T055: Test loan with 6% APR over 10 years - verify monthly payment accuracy

**Test Case**: Add a loan with known parameters and verify the monthly payment calculation

**Test Steps**:
1. Open http://localhost:5173 in browser
2. Click "Add Expense" button
3. Select "Loan" from Type dropdown
4. Enter the following:
   - Name: "Home Equity Loan"
   - Principal: $100,000
   - Down Payment: $0
   - APR %: 6
   - Term (yrs): 10
   - When: Age
   - Age: 65

**Expected Results**:
- Monthly Payment should display: $1,110.21
  - Formula: P=100000, r=0.06/12=0.005, n=120
  - M = 100000 * [0.005(1.005)^120] / [(1.005)^120 - 1]
  - M = 100000 * 0.00907989 = $1,110.21
- Annual payment = $1,110.21 × 12 = $13,322.52

**Actual Results**:
- [ ] Monthly payment calculation is accurate (within $0.01 of expected)
- [ ] Annual payment is correctly calculated as 12× monthly
- [ ] Financed amount correctly uses principal (no down payment)

**Status**: ⬜ NOT TESTED
**Notes**: Requires manual browser testing

---

## T056: Test loan starting at age 65, verify payments from age 65-74

**Test Case**: Verify loan payments appear in correct years

**Test Steps**:
1. Using the loan from T055 (starting at age 65, term 10 years)
2. Click "Run Simulation" button
3. Scroll through results table
4. Check years 2031-2040 (age 65-74)

**Expected Results**:
- One-Time Expenses column shows ~$13,322 (inflation-adjusted) in each year from 2031-2040
- No loan payments appear before 2031 (age 65)
- No loan payments appear after 2040 (age 74)
- Click on any year row to open computation breakdown
- Breakdown shows:
  - "One-Time Expenses" section
  - "Home Equity Loan (Loan Payment)" entry
  - Inflation-adjusted annual payment amount

**Actual Results**:
- [ ] Loan payments appear in years 2031-2040 (10 years)
- [ ] No payments before start year
- [ ] No payments after end year
- [ ] Breakdown shows loan payment details

**Status**: ⬜ NOT TESTED
**Notes**: Requires manual browser testing

---

## T057: Test 0% APR loan uses simple division (financed amount / months)

**Test Case**: Verify 0% APR loans use simple division instead of amortization formula

**Test Steps**:
1. Click "Add Expense" to add another loan
2. Select "Loan" from Type dropdown
3. Enter the following:
   - Name: "0% Car Loan"
   - Principal: $36,000
   - Down Payment: $6,000
   - APR %: 0
   - Term (yrs): 5
   - When: Age
   - Age: 67

**Expected Results**:
- Financed amount = $36,000 - $6,000 = $30,000
- Monthly Payment should display: $500.00
  - Simple division: $30,000 / (5 × 12) = $30,000 / 60 = $500.00
- Annual payment = $500 × 12 = $6,000

**Actual Results**:
- [ ] Monthly payment is exactly $500.00 (simple division)
- [ ] No amortization formula applied for 0% APR
- [ ] Financed amount correctly excludes down payment

**Status**: ⬜ NOT TESTED
**Notes**: Requires manual browser testing

---

## T058: Test concurrent cash and loan expenses with breakdown dialog

**Test Case**: Verify multiple expense types in same year display correctly in breakdown

**Test Steps**:
1. Add a cash expense:
   - Type: Cash
   - Name: "Medical Procedure"
   - Amount: $25,000
   - When: Age
   - Age: 67
2. Run simulation
3. Check year 2033 (age 67) in results table
4. Click on year 2033 row to open breakdown dialog

**Expected Results**:
- Year 2033 should show combined one-time expenses:
  - 0% Car Loan down payment: ~$6,000 (inflation-adjusted)
  - 0% Car Loan annual payment: ~$6,000 (inflation-adjusted)
  - Medical Procedure: ~$25,000 (inflation-adjusted)
  - Total: ~$37,000 (inflation-adjusted)
- Breakdown dialog "One-Time Expenses" section shows:
  - "0% Car Loan (Down Payment)" - Cash Expense
  - "Medical Procedure" - Cash Expense
  - "0% Car Loan" - Loan Payment
  - "Home Equity Loan" - Loan Payment
  - Total line with sum

**Actual Results**:
- [ ] All expenses appear in year 2033
- [ ] Breakdown shows all 4 expense entries
- [ ] Types are correctly labeled (Cash Expense vs Loan Payment)
- [ ] Total matches sum of individual entries

**Status**: ⬜ NOT TESTED
**Notes**: Requires manual browser testing

---

## T059: Test loan extending beyond simulation period (year 35)

**Test Case**: Verify loan payments stop at simulation end, not loan end

**Test Steps**:
1. Remove all existing expenses
2. Add a loan:
   - Name: "Long-term Loan"
   - Principal: $50,000
   - Down Payment: $0
   - APR %: 5
   - Term (yrs): 40
   - When: Age
   - Age: 60
3. Run simulation

**Expected Results**:
- Loan starts at age 60 (year 2026)
- Loan should run for 40 years (until age 99, year 2065)
- BUT simulation only runs until age 94 (year 2060, year 35 of simulation)
- One-Time Expenses should show loan payments from 2026 to 2060 only
- No loan payments should appear beyond year 35 of simulation

**Actual Results**:
- [ ] Loan payments appear from year 2026 (age 60)
- [ ] Loan payments continue through year 2060 (age 94, year 35)
- [ ] No payments appear beyond simulation period
- [ ] No errors or warnings

**Status**: ⬜ NOT TESTED
**Notes**: Requires manual browser testing

---

## T060: Verify annual totals show 12 monthly payments regardless of start month

**Test Case**: Confirm that loan payments are treated as annual totals (12× monthly)

**Test Steps**:
1. Using existing loans from previous tests
2. Check the one-time expenses totals in results table
3. Open breakdown for any year with loan payment

**Expected Results**:
- Each year's loan payment = monthly payment × 12
- No prorating based on start month (January assumed)
- Breakdown confirms annual total, not monthly

**Actual Results**:
- [ ] Annual totals are 12× monthly payment
- [ ] No prorating for partial years
- [ ] Breakdown shows annual amount

**Status**: ⬜ NOT TESTED
**Notes**: Requires manual browser testing

---

## T060a: Test loan with down payment - verify monthly payment calculated on financed amount

**Test Case**: Verify monthly payment uses (principal - down payment) as basis

**Test Steps**:
1. Remove all expenses
2. Add a loan:
   - Name: "Car with Down Payment"
   - Principal: $50,000
   - Down Payment: $10,000
   - APR %: 4.5
   - Term (yrs): 5
   - When: Age
   - Age: 65

**Expected Results**:
- Financed amount = $50,000 - $10,000 = $40,000
- Monthly Payment based on $40,000:
  - P = 40000, r = 0.045/12 = 0.00375, n = 60
  - M = 40000 * [0.00375(1.00375)^60] / [(1.00375)^60 - 1]
  - M ≈ $745.66
- Annual payment = $745.66 × 12 ≈ $8,947.92

**Actual Results**:
- [ ] Monthly payment is ~$745.66 (based on financed amount)
- [ ] NOT based on full principal ($50,000)
- [ ] Calculation accuracy within $0.01

**Status**: ⬜ NOT TESTED
**Notes**: Requires manual browser testing

---

## T060b: Test down payment appears as separate entry in start year breakdown

**Test Case**: Verify down payment is shown separately from loan payments in breakdown

**Test Steps**:
1. Using the "Car with Down Payment" from T060a
2. Run simulation
3. Open breakdown for year 2031 (age 65, start year)

**Expected Results**:
- Breakdown "One-Time Expenses" section shows TWO entries:
  1. "Car with Down Payment (Down Payment)" - Cash Expense - ~$10,000 (inflation-adjusted)
  2. "Car with Down Payment" - Loan Payment - ~$8,947.92 (inflation-adjusted)
- Total = ~$18,947.92 for start year
- Subsequent years (2032-2035) show only loan payment entry (~$8,947.92)

**Actual Results**:
- [ ] Two entries in start year (down payment + loan payment)
- [ ] Down payment labeled as "Cash Expense"
- [ ] Loan payment labeled as "Loan Payment"
- [ ] Only loan payment in subsequent years

**Status**: ⬜ NOT TESTED
**Notes**: Requires manual browser testing

---

## T060c: Test loan with down payment equal to principal (financed amount = 0)

**Test Case**: Edge case - verify behavior when down payment equals principal

**Test Steps**:
1. Add a loan:
   - Name: "Full Down Payment"
   - Principal: $30,000
   - Down Payment: $30,000
   - APR %: 5
   - Term (yrs): 5
   - When: Age
   - Age: 70

**Expected Results**:
- Financed amount = $30,000 - $30,000 = $0
- Monthly Payment should be: $0.00
- Run simulation should complete without errors
- Year 2036 (age 70) breakdown shows:
  - "Full Down Payment (Down Payment)" - $30,000 (inflation-adjusted)
  - No loan payment entries (or $0 loan payment)

**Actual Results**:
- [ ] Monthly payment shows $0.00
- [ ] Simulation runs without errors
- [ ] Down payment appears in breakdown
- [ ] No loan payments (or $0 payments) in any year

**Status**: ⬜ NOT TESTED
**Notes**: Edge case - requires manual browser testing

---

## Validation Summary

**Test Results**:
- Total Tests: 9
- Passed: 0
- Failed: 0
- Not Tested: 9

**Critical Issues**: None identified yet

**Non-Critical Issues**: None identified yet

**Recommendations**:
1. All tests require manual browser testing
2. Tests should be performed in order (T055-T060c)
3. Use browser developer tools to verify API requests/responses
4. Check console for any JavaScript errors
5. Verify inflation adjustments are applied consistently

---

## Amortization Calculation Reference

For verification purposes, the standard amortization formula is:

```
M = P × [r(1+r)^n] / [(1+r)^n - 1]

Where:
  M = Monthly payment
  P = Principal (or financed amount)
  r = Monthly interest rate (APR / 12 / 100)
  n = Total number of payments (term in years × 12)

Special case: If r = 0 (0% APR):
  M = P / n
```

**Example Calculations**:
1. $100,000 @ 6% for 10 years:
   - M = 100000 × [0.005(1.005)^120] / [(1.005)^120 - 1]
   - M = $1,110.21

2. $30,000 @ 0% for 5 years:
   - M = 30000 / 60
   - M = $500.00

3. $40,000 @ 4.5% for 5 years:
   - M = 40000 × [0.00375(1.00375)^60] / [(1.00375)^60 - 1]
   - M = $745.66

---

## Next Steps

1. Perform manual browser testing for all test cases (T055-T060c)
2. Document actual results in this file
3. Update task status in tasks.md based on test outcomes
4. Fix any issues discovered during testing
5. Re-test failed scenarios
6. Mark US3 integration tests as complete when all pass

