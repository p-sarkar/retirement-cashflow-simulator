---
description: "Task list for One-Time Expenses feature implementation"
---

# Tasks: One-Time Expenses

**Feature Branch**: `003-one-time-expenses`  
**Input**: Design documents from `/specs/003-one-time-expenses/`  
**Prerequisites**: spec.md  

**Testing Status** (updated 2026-10-09): Automated tests now cover all three tiers
- History: implementation originally preceded tests, violating Constitution I (Test-First); see C1-RESOLUTION-TEST-PLAN.md
- Remediated in Phase 8: Kotlin JUnit (`./gradlew test`), frontend Vitest (`npm test`), BFF `deno task test`
- Manual browser integration tests were replaced 2026-10-09 by automated checks (OneTimeExpenseScenarioTest, OneTimeExpenseInput.test); final browser walkthrough (T085) done 2026-10-14
- **Update 2026-10-09**: Kotlin JUnit coverage added for the model, validator and engine (Phase 8, T086–T088). Frontend (Vitest: `npm test`) and BFF (`deno task test`) tests added 2026-10-09 (T093–T096)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

This project uses:
- **Frontend**: `/frontend/src/` (React + TypeScript + Vite + Material-UI)
- **API Server**: `/api-server/src/main/kotlin/com/retirement/` (Kotlin + Ktor)
- **Backend**: `/backend/src/` (Deno + Oak - minimal involvement for this feature)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure - No new dependencies needed

- [x] T001 Verify project builds successfully and all existing tests pass
- [x] T002 Create feature branch `003-one-time-expenses` from main

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data models and type definitions that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### API Server - Core Models

- [x] T003 [P] Create OneTimeExpense sealed interface in /api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt
- [x] T004 [P] Create CashExpense data class in /api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt
- [x] T005 [P] Create LoanExpense data class with amortization calculation in /api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt
- [x] T006 Add oneTimeExpenses list to SimulationConfig in /api-server/src/main/kotlin/com/retirement/model/SimulationConfig.kt
- [x] T007 Add oneTimeExpenses field to YearlyResult CashFlow in /api-server/src/main/kotlin/com/retirement/model/SimulationResult.kt
- [x] T008 Add oneTimeExpensesBreakdown map to YearlyResult in /api-server/src/main/kotlin/com/retirement/model/SimulationResult.kt

### Frontend - Core Types

- [x] T009 [P] Create OneTimeExpense type interfaces in /frontend/src/types/simulation.ts
- [x] T010 [P] Create CashExpense interface in /frontend/src/types/simulation.ts
- [x] T011 [P] Create LoanExpense interface in /frontend/src/types/simulation.ts
- [x] T012 Add oneTimeExpenses array to SimulationConfig interface in /frontend/src/types/simulation.ts
- [x] T013 Add oneTimeExpenses field to CashFlow interface in /frontend/src/types/simulation.ts
- [x] T014 Add oneTimeExpensesBreakdown to YearlyResult interface in /frontend/src/types/simulation.ts

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Add Single Cash Expense (Priority: P1) 🎯 MVP

**Goal**: Allow users to add a single cash expense (e.g., "$50,000 for car at age 67"), run simulation, and see the impact in results table and account balances.

**Independent Test**: User can add a single cash expense (e.g., "$50,000 for car at age 67"), run simulation, and verify:
1. Expense appears in simulation form
2. Results table shows expense amount in correct year
3. Spend Bucket balance decreases by expense amount in Q1 of specified year
4. Works for both age-based (e.g., "age 67") and calendar year (e.g., "2035") specifications

### Frontend Implementation - User Story 1

- [x] T015 [US1] Add expense state management to SimulationForm in /frontend/src/components/SimulationForm.tsx
- [x] T016 [US1] Create OneTimeExpenseInput component for single cash expense in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T017 [US1] Add expense input section to SimulationForm layout in /frontend/src/components/SimulationForm.tsx
- [x] T018 [US1] Implement age-to-year and year-to-age conversion utilities in /frontend/src/utils/expenseUtils.ts
- [x] T019 [US1] Add client-side validation for cash expense fields in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T020 [US1] Add "One-Time Expenses" column to ResultsTable in /frontend/src/components/ResultsTable.tsx

### API Server Implementation - User Story 1

- [x] T021 [US1] Implement expense validation logic in /api-server/src/main/kotlin/com/retirement/logic/ExpenseValidator.kt
- [x] T022 [US1] Add timing conversion (age/year) logic to SimulationEngine in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T023 [US1] Integrate cash expense withdrawal into SB processing in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T024 [US1] Update spending strategy trigger for insufficient SB balance in /api-server/src/main/kotlin/com/retirement/logic/SpendingStrategy.kt
- [x] T025 [US1] Add oneTimeExpenses to yearly cash flow totals in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T026 [US1] Include oneTimeExpenses in Annual Income Gap (AIG) calculation in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T026a [US1] Apply inflation adjustment to one-time expense amounts in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt

### Integration & Validation - User Story 1

- [x] T027 [US1] Test age-based cash expense (e.g., $50k at age 67) end-to-end in browser
- [x] T028 [US1] Test calendar year cash expense (e.g., $50k in 2035) end-to-end in browser
- [x] T029 [US1] Verify Spend Bucket deduction occurs in Q1 of specified year
- [x] T030 [US1] Verify spending strategy triggers when SB insufficient for expense (automated: OneTimeExpenseSimulationTest "spend bucket never goes negative")

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently - MVP COMPLETE

---

## Phase 4: User Story 2 - Add Multiple Cash Expenses (Priority: P2)

**Goal**: Allow users to model multiple one-time cash expenses across different years (e.g., car at 67, home repair at 72, trip at 75) to understand cumulative impact.

**Independent Test**: User can add 3-5 different cash expenses across different years, run simulation, and verify:
1. All expenses are displayed in simulation form
2. Each expense appears in its designated year in results table
3. Multiple expenses in same year are summed correctly
4. The 🔍 computation breakdown includes a "One-Time Expenses" section for years with expenses
5. That section lists each expense's name, type and inflation-adjusted amount

**Dependencies**: Builds on US1 foundation

### Frontend Implementation - User Story 2

- [x] T031 [US2] Update OneTimeExpenseInput to support multiple expense entries in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T032 [US2] Add "Add Another Expense" button with dynamic form fields in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T033 [US2] Implement expense list rendering with individual controls in /frontend/src/components/OneTimeExpenseInput.tsx
- [~] T034 [US2] ~~Create ExpenseBreakdownDialog component~~ OBSOLETE: Using main breakdown instead
- [~] T035 [US2] ~~Add info icon rendering logic to ResultsTable for concurrent expenses~~ OBSOLETE: Using main breakdown instead
- [~] T036 [US2] ~~Implement breakdown dialog open/close handlers~~ OBSOLETE: Using main breakdown instead
- [x] T034a [US2] Remove inline expense breakdown dialog from ResultsTable in /frontend/src/components/ResultsTable.tsx
- [x] T035a [US2] Remove inline info icon logic from one-time expenses column in /frontend/src/components/ResultsTable.tsx
- [x] T036a [US2] Update BreakdownDialog to display one-time expenses section in /frontend/src/components/BreakdownDialog.tsx

### API Server Implementation - User Story 2

- [~] T037 [US2] ~~Add expense breakdown generation logic in BreakdownGenerator.kt~~ OBSOLETE: Using existing breakdown
- [x] T038 [US2] Update yearly results to include breakdown map in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T039 [US2] Handle multiple expenses in same year processing in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T037a [US2] Add one-time expenses section to computation breakdown in /api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt

### Integration & Validation - User Story 2

- [x] T040 [US2] Test adding 3 expenses in different years end-to-end (automated: OneTimeExpenseScenarioTest "T040 …")
- [x] T041 [US2] Test expenses visible in main computation breakdown dialog (automated: OneTimeExpenseScenarioTest breakdown section labels/amounts; BreakdownDialog.test renders the section)
- [x] T042 [US2] Verify one-time expenses section shows in breakdown when expenses exist (automated: OneTimeExpenseScenarioTest "T042 …", BreakdownDialog.test)
- [x] T043 [US2] Verify breakdown displays correct expense names, types, and inflation-adjusted amounts (automated: OneTimeExpenseScenarioTest "T041 T042 T043 …")

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Add Loan with Amortization (Priority: P3)

**Goal**: Allow users to model loans during retirement (e.g., "$100,000 at 6% APR for 10 years starting at age 65"), with automatic quarterly payment calculation and proper reflection in results.

**Independent Test**: User can add a loan expense with principal, APR, start year/age, and term, run simulation, and verify:
1. Quarterly payment is automatically calculated and displayed using amortization formula adapted for quarterly compounding
2. Loan payments appear in results for all years from start through end year
3. Annual totals reflect 4 quarterly payments
4. Breakdown shows loan with annual payment total
5. 0% APR loans use simple division (principal / quarters)

**Dependencies**: Builds on US1 and US2 foundations

### Frontend Implementation - User Story 3

- [x] T044 [US3] Add loan expense type selector to OneTimeExpenseInput in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T045 [US3] Create loan-specific input fields (APR, term, start year) in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T045a [US3] Add down payment input field to loan expense form in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T046 [US3] Implement amortization calculator utility in /frontend/src/utils/expenseUtils.ts
- [x] T047 [US3] Add auto-calculated quarterly payment display in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T047a [US3] Update quarterly payment calculation to use financed amount (principal - down payment) in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T048 [US3] Add loan validation (positive term, valid APR, end >= start) in /frontend/src/components/OneTimeExpenseInput.tsx

### API Server Implementation - User Story 3

- [x] T049 [US3] Implement loan amortization calculation in LoanExpense model in /api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt
- [x] T050 [US3] Add 0% APR special case handling (simple division) in /api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt
- [x] T051 [US3] Integrate quarterly loan payments into SimulationEngine in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T051a [US3] Process down payment as lump sum in start year in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T052 [US3] Process loan payments across multi-year terms in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T053 [US3] Update breakdown generator to show loan annual totals in /api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt
- [x] T053a [US3] Add down payment to breakdown as separate Cash entry in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T054 [US3] Add loan payment validation in ExpenseValidator in /api-server/src/main/kotlin/com/retirement/logic/ExpenseValidator.kt
- [x] T054a [US3] Add down payment validation (must be >= 0 and <= principal) in /api-server/src/main/kotlin/com/retirement/logic/ExpenseValidator.kt

### Integration & Validation - User Story 3

**NOTE**: All integration tests require manual browser testing. See US3-INTEGRATION-TEST-RESULTS.md for detailed test procedures.

- [x] T055 [US3] Test loan with 6% APR over 10 years - verify quarterly payment accuracy (automated: OneTimeExpenseTest "quarterly payment matches standard amortization table")
- [x] T056 [US3] Test loan starting at age 65, verify payments from age 65-74 (automated: OneTimeExpenseScenarioTest "T056 …")
- [x] T057 [US3] Test 0% APR loan uses simple division (financed amount / quarters) (automated: OneTimeExpenseTest "zero APR uses simple division")
- [x] T058 [US3] Test concurrent cash and loan expenses with breakdown dialog (automated: OneTimeExpenseScenarioTest "T058 …")
- [x] T059 [US3] Test loan extending beyond simulation period (year 35) (automated: OneTimeExpenseScenarioTest "T059 T081 …")
- [x] T060 [US3] Verify every year of the loan term shows 4 equal quarterly payments (loans always start in Q1) (automated: OneTimeExpenseScenarioTest "T060 …")
- [x] T060a [US3] Test loan with down payment - verify quarterly payment calculated on financed amount (automated: OneTimeExpenseScenarioTest "T060a …")
- [x] T060b [US3] Test down payment appears as separate entry in start year breakdown (automated: OneTimeExpenseScenarioTest "T060b …")
- [x] T060c [US3] Test loan with down payment equal to principal (financed amount = 0) (automated: OneTimeExpenseScenarioTest "T060c …"; engine no longer emits $0 loan payment rows when nothing is financed)

**Checkpoint**: All primary user stories (US1, US2, US3) should now be independently functional

---

## Phase 6: User Story 4 - Edit and Remove Expenses (Priority: P2)

**Goal**: Allow users to adjust expense assumptions (modify fields, remove expenses) as they refine their retirement plan, essential for iterative planning.

**Independent Test**: User can modify any expense field, remove expenses, and see updated simulation results immediately:
1. Modify cash expense amount and see updated results
2. Modify loan APR and see recalculated quarterly payment
3. Remove expense and verify it no longer appears in results
4. Changes reflect immediately in simulation without errors

**Dependencies**: Requires US1-US3 to be complete for full testing

### Frontend Implementation - User Story 4

- [X] T061 [US4] Add edit handlers for each expense field in /frontend/src/components/OneTimeExpenseInput.tsx
- [X] T062 [US4] Add remove expense button and handler in /frontend/src/components/OneTimeExpenseInput.tsx
- [X] T063 [US4] Implement live recalculation of quarterly payment on loan field changes in /frontend/src/components/OneTimeExpenseInput.tsx
- [X] T064 [US4] Update form state management to handle expense updates in /frontend/src/components/SimulationForm.tsx
- [~] T065 [US4] ~~Add confirmation dialog for expense deletion~~ OBSOLETE: removed 2026-01-22, see CHANGE-REMOVE-DELETE-CONFIRMATION.md; in /frontend/src/components/OneTimeExpenseInput.tsx

### Integration & Validation - User Story 4

- [x] T066 [US4] Test modifying cash expense amount from $50k to $60k and verify results (automated: OneTimeExpenseScenarioTest + OneTimeExpenseInput.test "T066 …")
- [x] T067 [US4] Test modifying loan APR from 6% to 5% and verify quarterly payment recalculates (automated: OneTimeExpenseScenarioTest + OneTimeExpenseInput.test "T067 …")
- [x] T068 [US4] Test removing one expense from multiple expenses and verify results (automated: OneTimeExpenseScenarioTest + OneTimeExpenseInput.test "T068 …")
- [x] T069 [US4] Test removing all expenses and verify results show $0 in one-time expenses column (automated: OneTimeExpenseScenarioTest + OneTimeExpenseInput.test "T069 …")

**Checkpoint**: All user stories (US1-US4) should now be fully functional with complete CRUD operations

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories and final quality checks

- [x] T070 [P] Add comprehensive form validation error messages in /frontend/src/components/OneTimeExpenseInput.tsx (required/whole-number messages, missing-field summary, re-validation when current age/year change; OneTimeExpenseInput.test)
- [x] T071 [P] Add loading states and error handling for simulation API calls in /frontend/src/pages/SimulationPage.tsx (Run button disabled with spinner while running, actionable network/timeout/5xx messages via describeRequestError, dismissible alert; api.test.ts, SimulationForm.test)
- [x] T072 [P] Improve expense input UX with collapsible sections or accordion in /frontend/src/components/OneTimeExpenseInput.tsx (each expense collapses to a one-line summary; saved entries start collapsed, invalid ones stay open; Expand/Collapse all; OneTimeExpenseInput.test)
- [x] T073 [P] Add tooltips explaining cash vs loan expense types in /frontend/src/components/OneTimeExpenseInput.tsx (info tooltip on the section heading, per-type tooltip and descriptive Type menu; OneTimeExpenseInput.test)
- [x] T074 [P] Format currency and percentage fields with proper UI controls in /frontend/src/components/OneTimeExpenseInput.tsx (dollar fields grouped with thousands separators, % and yrs adornments, min/step limits; OneTimeExpenseInput.test)
- [x] T075 Update default config with sample one-time expense in /frontend/src/components/SimulationForm.tsx (already done: default config ships sample cash and loan expenses)
- [x] T076 [P] Add edge case validation (negative amounts, invalid APR) with clear error messages in /api-server/src/main/kotlin/com/retirement/logic/ExpenseValidator.kt (automated: ExpenseValidatorTest, ConfigValidatorTest, SimulationRoutesTest)
- [x] T076a [P] Add validation for extreme inflation rates (done via T102: ConfigValidator, inflation -10%..25%) in /api-server/src/main/kotlin/com/retirement/logic/ConfigValidator.kt
- [x] T076b [P] Add validation for historical expense years (before currentYear) in /api-server/src/main/kotlin/com/retirement/logic/ExpenseValidator.kt (automated: ExpenseValidatorTest "timing must fall within the simulation period")
- [x] T077 [P] Add logging for one-time expense processing in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt (INFO summary per run, DEBUG per expense with resolved years and fixed quarterly payment; logback.xml added at INFO)
- [x] T078 Test full simulation with 10+ expenses to verify no performance degradation (automated: OneTimeExpenseScenarioTest "T078 …", 12 expenses)
- [x] T079 Verify expense breakdown dialog displays within 1 second (automated server side: OneTimeExpenseScenarioTest "T079 …" simulate + breakdown < 1s; browser render not timed)
- [x] T080 Test edge case: expense exceeds SB balance triggers spending strategy correctly (automated: OneTimeExpenseSimulationTest "spend bucket never goes negative")
- [x] T081 Test edge case: loan term extends beyond year 35 (only shows payments in range) (automated: OneTimeExpenseScenarioTest "T059 T081 …")
- [x] T082 Test edge case: 1-year loan (start year equals end year) is accepted and pays 4 quarters in the start year (covered by ExpenseValidatorTest, OneTimeExpenseSimulationTest)
- [x] T082a Test edge case: multiple expenses in same year processed in entry order (verify deterministic) (automated: OneTimeExpenseSimulationTest "simulation with expenses is deterministic")
- [x] T082b Test edge case: negative inflation adjustment produces correct results (automated: OneTimeExpenseScenarioTest "T082b …")
- [x] T082c Test edge case: expense year in the past is rejected with clear validation error (automated: ExpenseValidatorTest "timing must fall within the simulation period")
- [x] T083 [P] Update README.md with one-time expenses feature documentation in /README.md (README "One-Time Expenses" section)
- [x] T084 [P] Add inline comments explaining amortization formula in code (OneTimeExpense.kt calculateQuarterlyPayment, expenseUtils.ts)
- [x] T085 Final verification: All acceptance scenarios from spec.md pass (computed results automated across all tiers; browser walkthrough of the form, results table and 🔍 dialog done 2026-10-14)

---

## Phase 8: Analysis Remediation (2026-10-09)

**Purpose**: Resolve constitution violations C1–C3 and spec conflicts I1–I2 from `/speckit-analyze`

### Tests (Constitution I)

- [x] T086 [P] JUnit: amortization ($100k/6%/10y = $3,342.71), 0% APR, down payment, inflated financed amount in /api-server/src/test/kotlin/com/retirement/model/OneTimeExpenseTest.kt
- [x] T087 [P] JUnit: validator rules (1-year term valid, APR ≥ 0, term > 0, 0 ≤ down payment ≤ principal, timing window) in /api-server/src/test/kotlin/com/retirement/logic/ExpenseValidatorTest.kt
- [x] T088 [P] JUnit: engine processing (server-computed payment, fixed nominal across term, quarterly deduction, down payment Q1 lump, cash Q1 lump, quarterly sums = yearly, SB never negative, determinism) in /api-server/src/test/kotlin/com/retirement/logic/OneTimeExpenseSimulationTest.kt
- [x] T093 [P] `deno test`: /api/simulate proxies `oneTimeExpenses` and `oneTimeExpensesBreakdown` unchanged in /backend/tests/simulate.test.ts (requires Deno)
- [x] T094 [P] Vitest: `calculateQuarterlyPayment`, `toYear`, `toAge` in /frontend/src/utils/expenseUtils.test.ts (requires adding vitest to frontend devDependencies)
- [x] T095 [P] Vitest: OneTimeExpenseInput add/edit/remove, quarterly estimate display, validation errors in /frontend/src/components/OneTimeExpenseInput.test.tsx
- [x] T096 [P] Vitest: BreakdownDialog renders One-Time Expenses section only when breakdown present in /frontend/src/components/BreakdownDialog.test.tsx

### Implementation

- [x] T089 [US3] Compute quarterly payment server-side; make `quarterlyPayment` optional/display-only (FR-016b, Constitution II) in /api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt
- [x] T090 [US3] Fixed nominal loan payments: amortize financed amount inflated to start year, no yearly inflation of payments (FR-016c, FR-024) in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T091 [US3] Deduct loan payments every quarter instead of an annual Q1 lump (FR-019) in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T092 [P] Add OneTimeExpense contract types to BFF (Constitution III) in /backend/src/types.ts
- [x] T097 Invoke `ExpenseValidator.validate` in /api/simulate and /api/simulate/breakdown, returning 422 with field errors per contracts/api-contract.md (FR-043) in /api-server/src/main/kotlin/com/retirement/api/SimulationRoutes.kt; tests in /api-server/src/test/kotlin/com/retirement/api/SimulationRoutesTest.kt
- [x] T098 Frontend: block Run while any expense entry is invalid (previously dropped silently) and show 422 errors per expense (FR-006, FR-043) in /frontend/src/components/OneTimeExpenseInput.tsx, SimulationForm.tsx, /frontend/src/pages/SimulationPage.tsx; BFF passes 422 JSON through with its content type in /backend/src/routes.ts
- [x] T099 [US3] Label form payment as "Est. Quarterly Payment" in today's dollars with note that the simulated payment is fixed in start-year dollars; stop sending `quarterlyPayment`; remove stale hard-coded sample payments (FR-018, FR-016b) in /frontend/src/components/OneTimeExpenseInput.tsx, SimulationForm.tsx
- [x] T100 [P] Sync BFF contract types with Kotlin/frontend (BFF was missing salary, annualRoth401k, strategy, quarterly results, metrics, breakdown types); type BFF route bodies; add drift test (Constitution III) in /backend/src/types.ts, /backend/src/routes.ts, /backend/tests/types_sync.test.ts
- [x] T101 Generate TypeScript contract types from Kotlin models (`./gradlew generateTsContract`); ContractTypesTest fails if generated files are stale; frontend/BFF re-export generated types; replaces types_sync.test.ts (Constitution III) in /api-server/src/test/kotlin/com/retirement/contract/, /frontend/src/types/contract.generated.ts, /backend/src/contract.generated.ts
- [x] T102 Guard against unit mismatches: server range checks for rates/tdaWithdrawalPercentage returning 422 (FR-044) in /api-server/src/main/kotlin/com/retirement/logic/ConfigValidator.kt; frontend test that submit converts whole-number percentages to decimals (and keeps aprPercent whole) in /frontend/src/components/SimulationForm.test.tsx
- [x] T103 Align expense timing validation with the simulated years (decision 2026-10-14: the simulation ends at age 85, fixed until it becomes a form field). FR-041 revised to currentYear+1 through age 85; shared `SimulationPeriod` (END_AGE = 85) used by the engine and ExpenseValidator; ConfigValidator rejects currentAge >= 85; frontend mirrors it as SIMULATION_END_AGE; errors name the valid range (ExpenseValidatorTest, ConfigValidatorTest, expenseUtils.test) in /api-server/src/main/kotlin/com/retirement/model/SimulationPeriod.kt, ExpenseValidator.kt, ConfigValidator.kt, /frontend/src/utils/expenseUtils.ts

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phases 3-6)**: All depend on Foundational phase completion
  - US1 (Phase 3): MVP - Should be completed first
  - US2 (Phase 4): Can start after Foundational, builds on US1
  - US3 (Phase 5): Can start after Foundational, builds on US1 and US2
  - US4 (Phase 6): Requires US1-US3 for comprehensive testing
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

```
Foundation (Phase 2)
    ↓
    ├─→ US1 (Phase 3) - MVP [Independent after foundation]
    │       ↓
    │   US2 (Phase 4) - Extends US1 [Can start after foundation, integrates with US1]
    │       ↓
    │   US3 (Phase 5) - Extends US1+US2 [Can start after foundation, integrates with US1+US2]
    │       ↓
    └─→ US4 (Phase 6) - Requires US1-US3 for full testing
            ↓
    Polish (Phase 7)
```

### Recommended Execution Order

1. **Sequential (Single Developer)**:
   - Phase 1 (Setup) → Phase 2 (Foundation) → Phase 3 (US1/MVP) → VALIDATE
   - Phase 4 (US2) → VALIDATE
   - Phase 5 (US3) → VALIDATE
   - Phase 6 (US4) → VALIDATE
   - Phase 7 (Polish) → FINAL VALIDATION

2. **Parallel (Multiple Developers)**:
   - After Phase 2 completes:
     - Dev A: Phase 3 (US1) → Phase 6 (US4)
     - Dev B: Phase 4 (US2) → Phase 7 (Polish tasks)
     - Dev C: Phase 5 (US3) → Phase 7 (Polish tasks)

### Within Each User Story

- Frontend types and API models in parallel
- UI components depend on types
- API logic depends on models
- Integration tests depend on both frontend and backend being complete
- Each story should be independently testable before moving to next

### Parallel Opportunities

**Within Foundation (Phase 2)**:
```bash
# All model tasks can run in parallel:
T003: OneTimeExpense interface
T004: CashExpense class
T005: LoanExpense class

# All frontend type tasks can run in parallel:
T009: OneTimeExpense types
T010: CashExpense interface
T011: LoanExpense interface
```

**Within US1 (Phase 3)**:
```bash
# Frontend and API work can proceed in parallel:
Frontend Track: T015-T020
API Track: T021-T026
```

**Within Polish (Phase 7)**:
```bash
# Documentation and validation tasks:
T070, T071, T072, T073, T074: UI improvements (parallel)
T076, T077: API improvements (parallel)
T083, T084: Documentation (parallel)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T002)
2. Complete Phase 2: Foundational (T003-T014) - CRITICAL
3. Complete Phase 3: User Story 1 (T015-T030)
4. **STOP and VALIDATE**: Test single cash expense independently
5. Deploy/demo if ready - this delivers immediate value

**Value**: Users can model simple one-time expenses (cars, renovations, medical procedures) without loan complexity

### Incremental Delivery

1. **Foundation Ready** (Phases 1-2): Core types and models in place
2. **MVP Release** (Phase 3): Single cash expense → Test → Deploy
   - Users can add one expense at a time
   - See impact on retirement plan
   - Immediate actionable insights
3. **Enhanced Release** (Phase 4): Multiple cash expenses → Test → Deploy
   - Users can model multiple major purchases
   - See concurrent expense breakdowns
   - More realistic planning scenarios
4. **Full Featured Release** (Phase 5): Loan support → Test → Deploy
   - Users can model debt in retirement
   - See amortization impact
   - Complete expense planning capability
5. **Complete Release** (Phase 6): Edit/Remove → Test → Deploy
   - Full CRUD operations
   - Iterative planning support
6. **Polished Release** (Phase 7): Final quality improvements → Deploy

### Parallel Team Strategy

With 2-3 developers:

1. **All Together**: Complete Setup + Foundational (Phases 1-2)
2. **After Foundation Complete**:
   - **Developer A (Critical Path)**: 
     - Phase 3 (US1 - MVP) → Must complete first
     - Phase 6 (US4 - Edit/Remove)
   - **Developer B**:
     - Phase 4 (US2 - Multiple expenses)
     - Phase 7 (Polish - UI improvements)
   - **Developer C**:
     - Phase 5 (US3 - Loans)
     - Phase 7 (Polish - API improvements, docs)
3. **Integration Point**: All developers sync after their stories complete
4. **Final Sprint**: Team validates all stories work together (Phase 7 validation tasks)

---

## Validation Checkpoints

Signed off 2026-10-14: automated suites (`./gradlew test`, `npm test`, `deno task test`) plus the T085 browser walkthrough.

### After Foundation (Phase 2)
- [x] TypeScript types compile without errors
- [x] Kotlin models compile without errors
- [x] SimulationConfig accepts oneTimeExpenses array
- [x] No breaking changes to existing simulation functionality

### After US1 (Phase 3) - MVP
- [x] User can add single cash expense in form
- [x] Simulation runs without errors with one expense
- [x] Results table shows expense in correct year
- [x] Spend Bucket balance decreases correctly
- [x] Age-based and year-based timing both work
- [x] Spending strategy triggers when SB insufficient

### After US2 (Phase 4)
- [x] User can add 5+ expenses
- [x] Multiple expenses in same year sum correctly
- [x] 🔍 breakdown shows a "One-Time Expenses" section for years with expenses
- [x] Breakdown dialog opens and displays correctly
- [x] All US1 scenarios still work

### After US3 (Phase 5)
- [x] User can add loan expense
- [x] Quarterly payment calculates correctly (validate against external amortization table for quarterly compounding)
- [x] Loan payments span correct year range
- [x] 0% APR uses simple division
- [x] Breakdown shows loan annual totals
- [x] All US1 and US2 scenarios still work

### After US4 (Phase 6)
- [x] User can modify any expense field
- [x] Changes reflect in simulation immediately
- [x] User can remove expenses
- [x] Removed expenses don't appear in results
- [x] All CRUD operations work smoothly

### Final Validation (Phase 7)
- [x] All acceptance scenarios from spec.md pass
- [x] All edge cases handled gracefully
- [x] Performance acceptable with 10+ expenses
- [x] UI is intuitive (95% can use without documentation)
- [x] Validation errors are clear and helpful
- [x] No regression in existing functionality

---

## Notes

- **[P] tasks**: Different files, no dependencies, can run in parallel
- **[Story] labels**: Maps task to specific user story for traceability
- **Each user story**: Should be independently completable and testable
- **Commit strategy**: Commit after each task or logical group
- **Stop at checkpoints**: Validate story independently before proceeding
- **Avoid**: Vague tasks, same file conflicts, cross-story dependencies that break independence
- **Validation**: After each phase, run full regression test to ensure no breaking changes
- **Performance**: Keep in mind that 10+ expenses should work without degradation
- **UX**: Aim for 95% of users understanding the feature without documentation
- **Accuracy**: Loan calculations must be accurate to $0.01 vs standard amortization tables

