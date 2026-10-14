# C1 Resolution: Test-First Development Compliance Plan

**Date**: January 22, 2026  
**Issue**: Constitution Violation - Test-First Development Not Followed  
**Severity**: CRITICAL  
**Status**: ⚠️ In Remediation

---

## Problem Statement

The project constitution mandates **test-first development** as a NON-NEGOTIABLE principle. However, the 003-one-time-expenses feature was implemented with:

- ✅ **90%+ Implementation Complete**: All core functionality has been built
- ❌ **0% Tests Complete**: ALL integration tests are still pending (marked as [ ])

This is a **critical constitution violation** that must be addressed before the feature can be considered production-ready.

---

## Root Cause

The tasks.md file originally stated:
> "Tests: Not explicitly requested in spec.md - focusing on implementation tasks only"

This statement was incorrect and violated the project's fundamental development principles. Tests should have been written BEFORE or alongside implementation, not after.

---

## Immediate Actions Taken

### 1. Updated tasks.md
✅ Removed the statement that tests weren't requested  
✅ Added critical warning about test-first deviation  
✅ Flagged all pending integration tests

### 2. Documented the Violation
✅ Created this C1-RESOLUTION-TEST-PLAN.md document  
✅ Acknowledged the constitution violation  
✅ Created remediation plan

---

## Test Coverage Analysis

### Current State

| Category | Total Tasks | Completed | Pending | % Complete |
|----------|-------------|-----------|---------|------------|
| **Implementation** | 65 | 59 | 6 | 91% |
| **Integration Tests** | 27 | 0 | 27 | 0% |
| **TOTAL** | 92 | 59 | 33 | 64% |

### Pending Integration Tests by User Story

#### User Story 1 (Cash Expenses) - 4 Tests Pending
- [ ] T027: Test age-based cash expense end-to-end
- [ ] T028: Test calendar year cash expense end-to-end
- [ ] T029: Verify Spend Bucket deduction in Q1
- [ ] T030: Verify spending strategy triggers when SB insufficient

#### User Story 2 (Multiple Expenses) - 4 Tests Pending
- [ ] T040: Test 3 expenses in different years
- [ ] T041: Test expenses visible in main breakdown dialog
- [ ] T042: Verify one-time expenses section in breakdown
- [ ] T043: Verify breakdown displays correct details

#### User Story 3 (Loans) - 9 Tests Pending
- [ ] T055: Test loan with 6% APR - verify quarterly payment accuracy
- [ ] T056: Test loan age range (65-74)
- [ ] T057: Test 0% APR loan uses simple division
- [ ] T058: Test concurrent cash and loan expenses
- [ ] T059: Test loan extending beyond year 35
- [ ] T060: Verify annual totals = 4 quarterly payments
- [ ] T060a: Test loan with down payment on financed amount
- [ ] T060b: Test down payment in start year breakdown
- [ ] T060c: Test loan with down payment = principal

#### User Story 4 (Edit/Remove) - 4 Tests Pending
- [ ] T066: Test modifying cash expense amount
- [ ] T067: Test modifying loan APR with recalculation
- [ ] T068: Test removing one expense
- [ ] T069: Test removing all expenses

#### Phase 7 (Polish) - 6 Tests Pending
- [ ] T078: Test 10+ expenses for performance
- [ ] T079: Verify breakdown dialog loads <1 second
- [ ] T080: Test expense exceeds SB triggers strategy
- [ ] T081: Test loan extending beyond year 35

---

## Remediation Plan

### Phase 1: Create Test Framework (Priority: IMMEDIATE)

**Goal**: Establish testing infrastructure before writing tests

#### Backend Testing Setup
```kotlin
// Create test file structure
api-server/src/test/kotlin/com/retirement/logic/
├── OneTimeExpenseTest.kt
├── CashExpenseIntegrationTest.kt
├── LoanExpenseIntegrationTest.kt
├── ExpenseValidationTest.kt
└── SpendingStrategyWithExpensesTest.kt
```

**Required Actions**:
1. Set up Kotlin test dependencies (if not already present)
   - JUnit 5
   - Kotlin Test
   - MockK (for mocking)
2. Create test data builders for OneTimeExpense entities
3. Create test fixtures for SimulationConfig with expenses

#### Frontend Testing Setup
```typescript
// Create test file structure
frontend/src/__tests__/
├── components/
│   ├── OneTimeExpenseInput.test.tsx
│   └── ResultsTable.test.tsx
├── utils/
│   └── expenseUtils.test.ts
└── integration/
    ├── cash-expense.test.ts
    ├── loan-expense.test.ts
    └── expense-edit-remove.test.ts
```

**Required Actions**:
1. Verify test dependencies (Vitest, React Testing Library)
2. Create test utilities for rendering components
3. Create mock simulation data with expenses

---

### Phase 2: Unit Tests (Priority: HIGH)

**Timeline**: 2-3 days

#### Backend Unit Tests (10 tests minimum)

1. **OneTimeExpense Model Tests**
   ```kotlin
   class OneTimeExpenseTest {
       @Test
       fun `calculateQuarterlyPayment - standard loan`() {
           val payment = LoanExpense.calculateQuarterlyPayment(
               financedAmount = 100000.0,
               aprPercent = 6.0,
               termYears = 10
           )
           // Should equal $3,330.63 (quarterly payment for 6% APR over 10 years)
           assertEquals(3330.63, payment, 0.01)
       }
       
       @Test
       fun `calculateQuarterlyPayment - 0% APR uses simple division`() {
           val payment = LoanExpense.calculateQuarterlyPayment(
               financedAmount = 10000.0,
               aprPercent = 0.0,
               termYears = 5
           )
           // 10000 / (5 years * 4 quarters) = 500
           assertEquals(500.0, payment, 0.01)
       }
       
       @Test
       fun `getFinancedAmount - with down payment`() {
           val loan = LoanExpense(
               id = "test",
               name = "Car",
               principal = 50000.0,
               downPayment = 10000.0,
               aprPercent = 4.0,
               termYears = 5,
               startYearOrAge = YearOrAge.Year(2026),
               quarterlyPayment = 2213.79
           )
           assertEquals(40000.0, loan.getFinancedAmount())
       }
   }
   ```

2. **ExpenseValidator Tests**
   ```kotlin
   class ExpenseValidatorTest {
       @Test
       fun `validate cash expense - positive amount`()
       
       @Test
       fun `validate cash expense - negative amount fails`()
       
       @Test
       fun `validate loan - down payment exceeds principal fails`()
       
       @Test
       fun `validate loan - negative APR fails`()
       
       @Test
       fun `validate loan - term extends beyond simulation period allowed`()
   }
   ```

3. **YearOrAge Conversion Tests**
   ```kotlin
   class YearOrAgeTest {
       @Test
       fun `convert age to year correctly`() {
           val yearOrAge = YearOrAge.Age(67)
           val year = yearOrAge.toYear(currentAge = 55, currentYear = 2026)
           // 2026 + (67 - 55) = 2038
           assertEquals(2038, year)
       }
       
       @Test
       fun `convert year to age correctly`() {
           val yearOrAge = YearOrAge.Year(2038)
           val age = yearOrAge.toAge(currentAge = 55, currentYear = 2026)
           // 55 + (2038 - 2026) = 67
           assertEquals(67, age)
       }
   }
   ```

#### Frontend Unit Tests (8 tests minimum)

1. **expenseUtils Tests**
   ```typescript
   describe('calculateQuarterlyPayment', () => {
       it('calculates correct quarterly payment for standard loan', () => {
           const payment = calculateQuarterlyPayment(100000, 6, 10);
           expect(payment).toBeCloseTo(3330.63, 2);
       });
       
       it('handles 0% APR with simple division', () => {
           const payment = calculateQuarterlyPayment(10000, 0, 5);
           expect(payment).toBeCloseTo(500, 2);
       });
       
       it('calculates payment on financed amount (principal - down)', () => {
           const payment = calculateQuarterlyPayment(40000, 4, 5); // $50k - $10k down
           expect(payment).toBeCloseTo(2213.79, 2);
       });
   });
   ```

2. **Validation Function Tests**
   ```typescript
   describe('validateCashExpense', () => {
       it('passes for valid cash expense', () => { ... });
       it('fails for negative amount', () => { ... });
       it('fails for amount exceeding reasonable limit', () => { ... });
   });
   
   describe('validateLoanExpense', () => {
       it('passes for valid loan', () => { ... });
       it('fails when down payment > principal', () => { ... });
       it('fails for negative APR', () => { ... });
   });
   ```

---

### Phase 3: Integration Tests (Priority: CRITICAL)

**Timeline**: 3-5 days

These are the 27 pending tests from tasks.md. They must all be completed.

#### Testing Approach

**Manual Browser Testing**:
Since this is a full-stack feature with UI, most integration tests will be manual browser tests following structured test procedures.

**Test Documentation**:
Create detailed test procedures for each integration test with:
- Pre-conditions
- Step-by-step actions
- Expected results
- Actual results (to be filled in during testing)
- Pass/Fail status
- Screenshots/evidence

#### Test Procedure Template

```markdown
### Test T027: Age-Based Cash Expense End-to-End

**User Story**: US1  
**Requirement**: FR-009, FR-025  
**Priority**: P1

**Pre-conditions**:
- Application running locally
- Fresh browser session
- No existing simulations

**Test Steps**:
1. Navigate to simulation form
2. Add cash expense:
   - Type: Cash
   - Name: "New Car"
   - Amount: $50,000
   - When: Age 67
3. Set current age: 55
4. Set current year: 2026
5. Click "Run Simulation"

**Expected Results**:
- Expense appears in form with correct values
- Results table shows $50,000 in "One-Time Expenses" column for year 2038 (when age=67)
- Spend Bucket balance decreases by $50,000 in Q1 of 2038
- No errors in console

**Actual Results**:
[To be filled during test execution]

**Status**: [ ] Pass / [ ] Fail

**Evidence**:
[Screenshots to be added]

**Notes**:
[Any observations or issues]
```

#### Critical Integration Tests

**High Priority (Must Pass)**:
1. T027: Age-based cash expense
2. T028: Calendar year cash expense
3. T030: Spending strategy trigger
4. T055: Loan quarterly payment accuracy
5. T060a: Loan with down payment

**Medium Priority**:
6. T040: Multiple expenses in different years
7. T058: Concurrent cash and loan expenses
8. T066: Modify cash expense
9. T067: Modify loan APR

**Lower Priority**:
10. Edge cases (T059, T081, etc.)

---

### Phase 4: E2E Testing (Priority: MEDIUM)

**Timeline**: 2-3 days

#### E2E Test Scenarios

1. **Complete User Journey: Cash Expense**
   - User opens app
   - Creates new simulation
   - Adds cash expense
   - Runs simulation
   - Views results
   - Checks breakdown dialog
   - Modifies expense
   - Re-runs simulation
   - Saves simulation

2. **Complete User Journey: Loan Expense**
   - Similar flow for loan expense
   - Verifies quarterly payment calculation
   - Checks multi-year payments
   - Tests down payment feature

3. **Complete User Journey: Multiple Expenses**
   - Adds both cash and loan expenses
   - Verifies concurrent expense handling
   - Tests breakdown dialog with multiple entries

---

## Test Execution Plan

### Week 1: Foundation
- [ ] Day 1-2: Set up test infrastructure
- [ ] Day 3-4: Write all unit tests
- [ ] Day 5: Run unit tests and fix failures

### Week 2: Integration
- [ ] Day 1-2: Create test procedure documents for all 27 integration tests
- [ ] Day 3-4: Execute User Story 1 & 2 integration tests (8 tests)
- [ ] Day 5: Execute User Story 3 integration tests (9 tests)

### Week 3: Completion
- [ ] Day 1: Execute User Story 4 integration tests (4 tests)
- [ ] Day 2: Execute Phase 7 polish tests (6 tests)
- [ ] Day 3-4: Fix all test failures
- [ ] Day 5: Final verification and documentation

---

## Success Criteria

The C1 violation will be considered **RESOLVED** when:

1. ✅ All 27 integration tests have been executed and documented
2. ✅ At least 95% of integration tests pass
3. ✅ All critical integration tests (High Priority) pass
4. ✅ Test failure reports created for any failing tests
5. ✅ All test failures have remediation plans
6. ✅ Test results documented in TEST-RESULTS.md
7. ✅ Constitution compliance acknowledged in tasks.md

---

## Risk Assessment

### Risks of Current State

**CRITICAL RISKS**:
- ❌ Feature may have undiscovered bugs
- ❌ Regression risk when making changes
- ❌ Cannot verify business requirements are met
- ❌ Violates engineering best practices
- ❌ Technical debt accumulation

**MEDIUM RISKS**:
- ⚠️ Future maintainability concerns
- ⚠️ Difficulty onboarding new developers
- ⚠️ Manual testing burden

### Mitigation

By completing this test plan:
- ✅ Bugs will be discovered and fixed
- ✅ Regression protection established
- ✅ Business requirements verified
- ✅ Constitution compliance restored
- ✅ Technical debt addressed

---

## Accountability

### What Went Wrong

1. **Process Failure**: Test-first discipline was not enforced
2. **Documentation Failure**: tasks.md incorrectly stated tests weren't needed
3. **Review Failure**: Implementation proceeded without test verification

### What We're Doing About It

1. **Immediate**: Comprehensive test plan (this document)
2. **Short-term**: Execute all pending tests
3. **Long-term**: Prevent recurrence through stricter review process

---

## Next Steps

### Immediate (This Week)
1. ✅ Update tasks.md to acknowledge violation
2. ✅ Create this C1-RESOLUTION-TEST-PLAN.md
3. [ ] Review and approve test plan
4. [ ] Set up test infrastructure

### Short-term (Next 2-3 Weeks)
5. [ ] Write all unit tests
6. [ ] Execute all integration tests
7. [ ] Document test results
8. [ ] Fix test failures
9. [ ] Final verification

### Before Production
10. [ ] Verify 100% of critical tests pass
11. [ ] Create test summary report
12. [ ] Mark C1 as RESOLVED
13. [ ] Update feature status to "Production Ready"

---

## Conclusion

The C1 constitution violation is **ACKNOWLEDGED** and a comprehensive remediation plan is **IN PLACE**.

While the feature implementation is substantially complete, it **CANNOT BE CONSIDERED PRODUCTION-READY** until:
- All 27 integration tests are executed
- All critical tests pass
- Test results are documented
- Constitution compliance is restored

**Timeline**: 3 weeks to full remediation  
**Status**: In Progress  
**Commitment**: No shortcuts, comprehensive testing before production deployment

---

**Document Owner**: GitHub Copilot  
**Created**: January 22, 2026  
**Status**: Active Remediation Plan  
**Next Review**: Upon completion of Week 1 tasks

