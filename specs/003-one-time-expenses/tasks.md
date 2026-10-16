---
description: "Task list for One-Time Expenses feature implementation"
---

# Tasks: One-Time Expenses

**Feature Branch**: `003-one-time-expenses`  
**Input**: Design documents from `/specs/003-one-time-expenses/`  
**Prerequisites**: spec.md  

**Testing Status** (updated 2026-10-09): Automated tests now cover all three tiers
- History: implementation originally preceded tests, violating Constitution I (Test-First); see C003-1-RESOLUTION-TEST-PLAN.md
- Remediated in Phase 8: Kotlin JUnit (`./gradlew test`), frontend Vitest (`npm test`), BFF `deno task test`
- Manual browser integration tests were replaced 2026-10-09 by automated checks (OneTimeExpenseScenarioTest, OneTimeExpenseInput.test); final browser walkthrough (T003-085) done 2026-10-14
- **Update 2026-10-09**: Kotlin JUnit coverage added for the model, validator and engine (Phase 8, T003-086–T003-088). Frontend (Vitest: `npm test`) and BFF (`deno task test`) tests added 2026-10-09 (T003-093–T003-096)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US003-1, US003-2, US003-3, US003-4)
- Include exact file paths in descriptions

## Path Conventions

This project uses:
- **Frontend**: `/frontend/src/` (React + TypeScript + Vite + Material-UI)
- **API Server**: `/api-server/src/main/kotlin/com/retirement/` (Kotlin + Ktor)
- **Backend**: `/backend/src/` (Deno + Oak - minimal involvement for this feature)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure - No new dependencies needed

- [x] T003-001 Verify project builds successfully and all existing tests pass
- [x] T003-002 Create feature branch `003-one-time-expenses` from main

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data models and type definitions that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### API Server - Core Models

- [x] T003-003 [P] Create OneTimeExpense sealed interface in /api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt
- [x] T003-004 [P] Create CashExpense data class in /api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt
- [x] T003-005 [P] Create LoanExpense data class with amortization calculation in /api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt
- [x] T003-006 Add oneTimeExpenses list to SimulationConfig in /api-server/src/main/kotlin/com/retirement/model/SimulationConfig.kt
- [x] T003-007 Add oneTimeExpenses field to YearlyResult CashFlow in /api-server/src/main/kotlin/com/retirement/model/SimulationResult.kt
- [x] T003-008 Add oneTimeExpensesBreakdown map to YearlyResult in /api-server/src/main/kotlin/com/retirement/model/SimulationResult.kt

### Frontend - Core Types

- [x] T003-009 [P] Create OneTimeExpense type interfaces in /frontend/src/types/simulation.ts
- [x] T003-010 [P] Create CashExpense interface in /frontend/src/types/simulation.ts
- [x] T003-011 [P] Create LoanExpense interface in /frontend/src/types/simulation.ts
- [x] T003-012 Add oneTimeExpenses array to SimulationConfig interface in /frontend/src/types/simulation.ts
- [x] T003-013 Add oneTimeExpenses field to CashFlow interface in /frontend/src/types/simulation.ts
- [x] T003-014 Add oneTimeExpensesBreakdown to YearlyResult interface in /frontend/src/types/simulation.ts

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

- [x] T003-015 [US003-1] Add expense state management to SimulationForm in /frontend/src/components/SimulationForm.tsx
- [x] T003-016 [US003-1] Create OneTimeExpenseInput component for single cash expense in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T003-017 [US003-1] Add expense input section to SimulationForm layout in /frontend/src/components/SimulationForm.tsx
- [x] T003-018 [US003-1] Implement age-to-year and year-to-age conversion utilities in /frontend/src/utils/expenseUtils.ts
- [x] T003-019 [US003-1] Add client-side validation for cash expense fields in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T003-020 [US003-1] Add "One-Time Expenses" column to ResultsTable in /frontend/src/components/ResultsTable.tsx

### API Server Implementation - User Story 1

- [x] T003-021 [US003-1] Implement expense validation logic in /api-server/src/main/kotlin/com/retirement/logic/ExpenseValidator.kt
- [x] T003-022 [US003-1] Add timing conversion (age/year) logic to SimulationEngine in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T003-023 [US003-1] Integrate cash expense withdrawal into SB processing in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T003-024 [US003-1] Update spending strategy trigger for insufficient SB balance in /api-server/src/main/kotlin/com/retirement/logic/SpendingStrategy.kt
- [x] T003-025 [US003-1] Add oneTimeExpenses to yearly cash flow totals in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T003-026 [US003-1] Include oneTimeExpenses in Annual Income Gap (AIG) calculation in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T003-026a [US003-1] Apply inflation adjustment to one-time expense amounts in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt

### Integration & Validation - User Story 1

- [x] T003-027 [US003-1] Test age-based cash expense (e.g., $50k at age 67) end-to-end in browser
- [x] T003-028 [US003-1] Test calendar year cash expense (e.g., $50k in 2035) end-to-end in browser
- [x] T003-029 [US003-1] Verify Spend Bucket deduction occurs in Q1 of specified year
- [x] T003-030 [US003-1] Verify spending strategy triggers when SB insufficient for expense (automated: OneTimeExpenseSimulationTest "spend bucket never goes negative")

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

**Dependencies**: Builds on US003-1 foundation

### Frontend Implementation - User Story 2

- [x] T003-031 [US003-2] Update OneTimeExpenseInput to support multiple expense entries in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T003-032 [US003-2] Add "Add Another Expense" button with dynamic form fields in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T003-033 [US003-2] Implement expense list rendering with individual controls in /frontend/src/components/OneTimeExpenseInput.tsx
- [~] T003-034 [US003-2] ~~Create ExpenseBreakdownDialog component~~ OBSOLETE: Using main breakdown instead
- [~] T003-035 [US003-2] ~~Add info icon rendering logic to ResultsTable for concurrent expenses~~ OBSOLETE: Using main breakdown instead
- [~] T003-036 [US003-2] ~~Implement breakdown dialog open/close handlers~~ OBSOLETE: Using main breakdown instead
- [x] T003-034a [US003-2] Remove inline expense breakdown dialog from ResultsTable in /frontend/src/components/ResultsTable.tsx
- [x] T003-035a [US003-2] Remove inline info icon logic from one-time expenses column in /frontend/src/components/ResultsTable.tsx
- [x] T003-036a [US003-2] Update BreakdownDialog to display one-time expenses section in /frontend/src/components/BreakdownDialog.tsx

### API Server Implementation - User Story 2

- [~] T003-037 [US003-2] ~~Add expense breakdown generation logic in BreakdownGenerator.kt~~ OBSOLETE: Using existing breakdown
- [x] T003-038 [US003-2] Update yearly results to include breakdown map in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T003-039 [US003-2] Handle multiple expenses in same year processing in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T003-037a [US003-2] Add one-time expenses section to computation breakdown in /api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt

### Integration & Validation - User Story 2

- [x] T003-040 [US003-2] Test adding 3 expenses in different years end-to-end (automated: OneTimeExpenseScenarioTest "T003-040 …")
- [x] T003-041 [US003-2] Test expenses visible in main computation breakdown dialog (automated: OneTimeExpenseScenarioTest breakdown section labels/amounts; BreakdownDialog.test renders the section)
- [x] T003-042 [US003-2] Verify one-time expenses section shows in breakdown when expenses exist (automated: OneTimeExpenseScenarioTest "T003-042 …", BreakdownDialog.test)
- [x] T003-043 [US003-2] Verify breakdown displays correct expense names, types, and inflation-adjusted amounts (automated: OneTimeExpenseScenarioTest "T003-041 T003-042 T003-043 …")

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

**Dependencies**: Builds on US003-1 and US003-2 foundations

### Frontend Implementation - User Story 3

- [x] T003-044 [US003-3] Add loan expense type selector to OneTimeExpenseInput in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T003-045 [US003-3] Create loan-specific input fields (APR, term, start year) in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T003-045a [US003-3] Add down payment input field to loan expense form in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T003-046 [US003-3] Implement amortization calculator utility in /frontend/src/utils/expenseUtils.ts
- [x] T003-047 [US003-3] Add auto-calculated quarterly payment display in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T003-047a [US003-3] Update quarterly payment calculation to use financed amount (principal - down payment) in /frontend/src/components/OneTimeExpenseInput.tsx
- [x] T003-048 [US003-3] Add loan validation (positive term, valid APR, end >= start) in /frontend/src/components/OneTimeExpenseInput.tsx

### API Server Implementation - User Story 3

- [x] T003-049 [US003-3] Implement loan amortization calculation in LoanExpense model in /api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt
- [x] T003-050 [US003-3] Add 0% APR special case handling (simple division) in /api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt
- [x] T003-051 [US003-3] Integrate quarterly loan payments into SimulationEngine in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T003-051a [US003-3] Process down payment as lump sum in start year in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T003-052 [US003-3] Process loan payments across multi-year terms in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T003-053 [US003-3] Update breakdown generator to show loan annual totals in /api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt
- [x] T003-053a [US003-3] Add down payment to breakdown as separate Cash entry in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T003-054 [US003-3] Add loan payment validation in ExpenseValidator in /api-server/src/main/kotlin/com/retirement/logic/ExpenseValidator.kt
- [x] T003-054a [US003-3] Add down payment validation (must be >= 0 and <= principal) in /api-server/src/main/kotlin/com/retirement/logic/ExpenseValidator.kt

### Integration & Validation - User Story 3

**NOTE**: All integration tests require manual browser testing. See US003-3-INTEGRATION-TEST-RESULTS.md for detailed test procedures.

- [x] T003-055 [US003-3] Test loan with 6% APR over 10 years - verify quarterly payment accuracy (automated: OneTimeExpenseTest "quarterly payment matches standard amortization table")
- [x] T003-056 [US003-3] Test loan starting at age 65, verify payments from age 65-74 (automated: OneTimeExpenseScenarioTest "T003-056 …")
- [x] T003-057 [US003-3] Test 0% APR loan uses simple division (financed amount / quarters) (automated: OneTimeExpenseTest "zero APR uses simple division")
- [x] T003-058 [US003-3] Test concurrent cash and loan expenses with breakdown dialog (automated: OneTimeExpenseScenarioTest "T003-058 …")
- [x] T003-059 [US003-3] Test loan extending beyond simulation period (year 35) (automated: OneTimeExpenseScenarioTest "T003-059 T003-081 …")
- [x] T003-060 [US003-3] Verify every year of the loan term shows 4 equal quarterly payments (loans always start in Q1) (automated: OneTimeExpenseScenarioTest "T003-060 …")
- [x] T003-060a [US003-3] Test loan with down payment - verify quarterly payment calculated on financed amount (automated: OneTimeExpenseScenarioTest "T003-060a …")
- [x] T003-060b [US003-3] Test down payment appears as separate entry in start year breakdown (automated: OneTimeExpenseScenarioTest "T003-060b …")
- [x] T003-060c [US003-3] Test loan with down payment equal to principal (financed amount = 0) (automated: OneTimeExpenseScenarioTest "T003-060c …"; engine no longer emits $0 loan payment rows when nothing is financed)

**Checkpoint**: All primary user stories (US003-1, US003-2, US003-3) should now be independently functional

---

## Phase 6: User Story 4 - Edit and Remove Expenses (Priority: P2)

**Goal**: Allow users to adjust expense assumptions (modify fields, remove expenses) as they refine their retirement plan, essential for iterative planning.

**Independent Test**: User can modify any expense field, remove expenses, and see updated simulation results immediately:
1. Modify cash expense amount and see updated results
2. Modify loan APR and see recalculated quarterly payment
3. Remove expense and verify it no longer appears in results
4. Changes reflect immediately in simulation without errors

**Dependencies**: Requires US003-1-US003-3 to be complete for full testing

### Frontend Implementation - User Story 4

- [X] T003-061 [US003-4] Add edit handlers for each expense field in /frontend/src/components/OneTimeExpenseInput.tsx
- [X] T003-062 [US003-4] Add remove expense button and handler in /frontend/src/components/OneTimeExpenseInput.tsx
- [X] T003-063 [US003-4] Implement live recalculation of quarterly payment on loan field changes in /frontend/src/components/OneTimeExpenseInput.tsx
- [X] T003-064 [US003-4] Update form state management to handle expense updates in /frontend/src/components/SimulationForm.tsx
- [~] T003-065 [US003-4] ~~Add confirmation dialog for expense deletion~~ OBSOLETE: removed 2026-01-22, see CHANGE-REMOVE-DELETE-CONFIRMATION.md; in /frontend/src/components/OneTimeExpenseInput.tsx

### Integration & Validation - User Story 4

- [x] T003-066 [US003-4] Test modifying cash expense amount from $50k to $60k and verify results (automated: OneTimeExpenseScenarioTest + OneTimeExpenseInput.test "T003-066 …")
- [x] T003-067 [US003-4] Test modifying loan APR from 6% to 5% and verify quarterly payment recalculates (automated: OneTimeExpenseScenarioTest + OneTimeExpenseInput.test "T003-067 …")
- [x] T003-068 [US003-4] Test removing one expense from multiple expenses and verify results (automated: OneTimeExpenseScenarioTest + OneTimeExpenseInput.test "T003-068 …")
- [x] T003-069 [US003-4] Test removing all expenses and verify results show $0 in one-time expenses column (automated: OneTimeExpenseScenarioTest + OneTimeExpenseInput.test "T003-069 …")

**Checkpoint**: All user stories (US003-1-US003-4) should now be fully functional with complete CRUD operations

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories and final quality checks

- [x] T003-070 [P] Add comprehensive form validation error messages in /frontend/src/components/OneTimeExpenseInput.tsx (required/whole-number messages, missing-field summary, re-validation when current age/year change; OneTimeExpenseInput.test)
- [x] T003-071 [P] Add loading states and error handling for simulation API calls in /frontend/src/pages/SimulationPage.tsx (Run button disabled with spinner while running, actionable network/timeout/5xx messages via describeRequestError, dismissible alert; api.test.ts, SimulationForm.test)
- [x] T003-072 [P] Improve expense input UX with collapsible sections or accordion in /frontend/src/components/OneTimeExpenseInput.tsx (each expense collapses to a one-line summary; saved entries start collapsed, invalid ones stay open; Expand/Collapse all; OneTimeExpenseInput.test)
- [x] T003-073 [P] Add tooltips explaining cash vs loan expense types in /frontend/src/components/OneTimeExpenseInput.tsx (info tooltip on the section heading, per-type tooltip and descriptive Type menu; OneTimeExpenseInput.test)
- [x] T003-074 [P] Format currency and percentage fields with proper UI controls in /frontend/src/components/OneTimeExpenseInput.tsx (dollar fields grouped with thousands separators, % and yrs adornments, min/step limits; OneTimeExpenseInput.test)
- [x] T003-075 Update default config with sample one-time expense in /frontend/src/components/SimulationForm.tsx (already done: default config ships sample cash and loan expenses)
- [x] T003-076 [P] Add edge case validation (negative amounts, invalid APR) with clear error messages in /api-server/src/main/kotlin/com/retirement/logic/ExpenseValidator.kt (automated: ExpenseValidatorTest, ConfigValidatorTest, SimulationRoutesTest)
- [x] T003-076a [P] Add validation for extreme inflation rates (done via T003-102: ConfigValidator, inflation -10%..25%) in /api-server/src/main/kotlin/com/retirement/logic/ConfigValidator.kt
- [x] T003-076b [P] Add validation for historical expense years (before currentYear) in /api-server/src/main/kotlin/com/retirement/logic/ExpenseValidator.kt (automated: ExpenseValidatorTest "timing must fall within the simulation period")
- [x] T003-077 [P] Add logging for one-time expense processing in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt (INFO summary per run, DEBUG per expense with resolved years and fixed quarterly payment; logback.xml added at INFO)
- [x] T003-078 Test full simulation with 10+ expenses to verify no performance degradation (automated: OneTimeExpenseScenarioTest "T003-078 …", 12 expenses)
- [x] T003-079 Verify expense breakdown dialog displays within 1 second (automated server side: OneTimeExpenseScenarioTest "T003-079 …" simulate + breakdown < 1s; browser render not timed)
- [x] T003-080 Test edge case: expense exceeds SB balance triggers spending strategy correctly (automated: OneTimeExpenseSimulationTest "spend bucket never goes negative")
- [x] T003-081 Test edge case: loan term extends beyond year 35 (only shows payments in range) (automated: OneTimeExpenseScenarioTest "T003-059 T003-081 …")
- [x] T003-082 Test edge case: 1-year loan (start year equals end year) is accepted and pays 4 quarters in the start year (covered by ExpenseValidatorTest, OneTimeExpenseSimulationTest)
- [x] T003-082a Test edge case: multiple expenses in same year processed in entry order (verify deterministic) (automated: OneTimeExpenseSimulationTest "simulation with expenses is deterministic")
- [x] T003-082b Test edge case: negative inflation adjustment produces correct results (automated: OneTimeExpenseScenarioTest "T003-082b …")
- [x] T003-082c Test edge case: expense year in the past is rejected with clear validation error (automated: ExpenseValidatorTest "timing must fall within the simulation period")
- [x] T003-083 [P] Update README.md with one-time expenses feature documentation in /README.md (README "One-Time Expenses" section)
- [x] T003-084 [P] Add inline comments explaining amortization formula in code (OneTimeExpense.kt calculateQuarterlyPayment, expenseUtils.ts)
- [x] T003-085 Final verification: All acceptance scenarios from spec.md pass (computed results automated across all tiers; browser walkthrough of the form, results table and 🔍 dialog done 2026-10-14)

---

## Phase 8: Analysis Remediation (2026-10-09)

**Purpose**: Resolve constitution violations C003-1–C003-3 and spec conflicts I003-1–I003-2 from `/speckit-analyze`

### Tests (Constitution I)

- [x] T003-086 [P] JUnit: amortization ($100k/6%/10y = $3,342.71), 0% APR, down payment, inflated financed amount in /api-server/src/test/kotlin/com/retirement/model/OneTimeExpenseTest.kt
- [x] T003-087 [P] JUnit: validator rules (1-year term valid, APR ≥ 0, term > 0, 0 ≤ down payment ≤ principal, timing window) in /api-server/src/test/kotlin/com/retirement/logic/ExpenseValidatorTest.kt
- [x] T003-088 [P] JUnit: engine processing (server-computed payment, fixed nominal across term, quarterly deduction, down payment Q1 lump, cash Q1 lump, quarterly sums = yearly, SB never negative, determinism) in /api-server/src/test/kotlin/com/retirement/logic/OneTimeExpenseSimulationTest.kt
- [x] T003-093 [P] `deno test`: /api/simulate proxies `oneTimeExpenses` and `oneTimeExpensesBreakdown` unchanged in /backend/tests/simulate.test.ts (requires Deno)
- [x] T003-094 [P] Vitest: `calculateQuarterlyPayment`, `toYear`, `toAge` in /frontend/src/utils/expenseUtils.test.ts (requires adding vitest to frontend devDependencies)
- [x] T003-095 [P] Vitest: OneTimeExpenseInput add/edit/remove, quarterly estimate display, validation errors in /frontend/src/components/OneTimeExpenseInput.test.tsx
- [x] T003-096 [P] Vitest: BreakdownDialog renders One-Time Expenses section only when breakdown present in /frontend/src/components/BreakdownDialog.test.tsx

### Implementation

- [x] T003-089 [US003-3] Compute quarterly payment server-side; make `quarterlyPayment` optional/display-only (FR003-016b, Constitution II) in /api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt
- [x] T003-090 [US003-3] Fixed nominal loan payments: amortize financed amount inflated to start year, no yearly inflation of payments (FR003-016c, FR003-024) in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T003-091 [US003-3] Deduct loan payments every quarter instead of an annual Q1 lump (FR003-019) in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T003-092 [P] Add OneTimeExpense contract types to BFF (Constitution III) in /backend/src/types.ts
- [x] T003-097 Invoke `ExpenseValidator.validate` in /api/simulate and /api/simulate/breakdown, returning 422 with field errors per contracts/api-contract.md (FR003-043) in /api-server/src/main/kotlin/com/retirement/api/SimulationRoutes.kt; tests in /api-server/src/test/kotlin/com/retirement/api/SimulationRoutesTest.kt
- [x] T003-098 Frontend: block Run while any expense entry is invalid (previously dropped silently) and show 422 errors per expense (FR003-006, FR003-043) in /frontend/src/components/OneTimeExpenseInput.tsx, SimulationForm.tsx, /frontend/src/pages/SimulationPage.tsx; BFF passes 422 JSON through with its content type in /backend/src/routes.ts
- [x] T003-099 [US003-3] Label form payment as "Est. Quarterly Payment" in today's dollars with note that the simulated payment is fixed in start-year dollars; stop sending `quarterlyPayment`; remove stale hard-coded sample payments (FR003-018, FR003-016b) in /frontend/src/components/OneTimeExpenseInput.tsx, SimulationForm.tsx
- [x] T003-100 [P] Sync BFF contract types with Kotlin/frontend (BFF was missing salary, annualRoth401k, strategy, quarterly results, metrics, breakdown types); type BFF route bodies; add drift test (Constitution III) in /backend/src/types.ts, /backend/src/routes.ts, /backend/tests/types_sync.test.ts
- [x] T003-101 Generate TypeScript contract types from Kotlin models (`./gradlew generateTsContract`); ContractTypesTest fails if generated files are stale; frontend/BFF re-export generated types; replaces types_sync.test.ts (Constitution III) in /api-server/src/test/kotlin/com/retirement/contract/, /frontend/src/types/contract.generated.ts, /backend/src/contract.generated.ts
- [x] T003-102 Guard against unit mismatches: server range checks for rates/tdaWithdrawalPercentage returning 422 (FR003-044) in /api-server/src/main/kotlin/com/retirement/logic/ConfigValidator.kt; frontend test that submit converts whole-number percentages to decimals (and keeps aprPercent whole) in /frontend/src/components/SimulationForm.test.tsx
- [x] T003-103 Align expense timing validation with the simulated years (decision 2026-10-14: the simulation ends at age 85, fixed until it becomes a form field). FR003-041 revised to currentYear+1 through age 85; shared `SimulationPeriod` (END_AGE = 85) used by the engine and ExpenseValidator; ConfigValidator rejects currentAge >= 85; frontend mirrors it as SIMULATION_END_AGE; errors name the valid range (ExpenseValidatorTest, ConfigValidatorTest, expenseUtils.test) in /api-server/src/main/kotlin/com/retirement/model/SimulationPeriod.kt, ExpenseValidator.kt, ConfigValidator.kt, /frontend/src/utils/expenseUtils.ts


---

## Phase 9: Follow-ups (known issues from v1.1.26)

**Purpose**: Known issues listed in docs/release-notes-v1.1.26.md. None block the feature; only T003-104 is specific to one-time expenses, the rest are project-wide

- [x] T003-104 [P] [US003-3] Label loan payments in the 🔍 breakdown as fixed, not "Inflation-adjusted amount" (e.g. formula "Fixed quarterly payment × 4", explanation in start-year dollars; down payment and cash rows keep the inflation-adjusted label) (FR003-016c, FR003-034) in /api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt; test in /api-server/src/test/kotlin/com/retirement/logic/OneTimeExpenseScenarioTest.kt (done 2026-10-14: loan rows show "Fixed quarterly payment × 4" with the quarterly payment; cash rows keep "Inflation-adjusted amount"; OneTimeExpenseScenarioTest "T003-104 …")
- [x] T003-105 [P] BFF returns 503 (not 500) when the Kotlin API server is unreachable, per contracts/api-contract.md, for /api/simulate and /api/simulate/breakdown in /backend/src/routes.ts; test in /backend/tests/simulate.test.ts; frontend `describeRequestError` keeps a clear message for 503 in /frontend/src/services/api.ts (done 2026-10-14: 503 when the API server is unreachable, 400 for malformed JSON, API errors passed through; shared proxyPost helper; frontend names the API server on 503; simulate.test.ts, api.test.ts)
- [x] T003-106 [P] Import Oak and CORS through deno.json (`jsr:@oak/oak`, already mapped) instead of deno.land/x URLs, so the BFF stops running in Node-compatibility mode under Deno 2 ("oak send() does not work under Node.js") in /backend/src/main.ts, /backend/src/routes.ts, /backend/deno.json (done 2026-10-14: Oak 17 from JSR via deno.json; the deno.land/x CORS module replaced by a small inline middleware; no Node-compatibility warning at startup; PORT env var, default 8000)
- [~] T003-107 ~~Make the simulation end age a configuration form field~~ MOVED 2026-10-14: requirement added as FR002-001a; implementation tracked in specs/002-retirement-cashflow/tasks.md Phase 8 (T003-045–T003-051)
- [x] T003-108 [P] Add a frontend ESLint config (ESLint 8, @typescript-eslint, react-hooks, react-refresh — already in devDependencies) so `npm run lint` runs, and fix or explicitly suppress its findings in /frontend/.eslintrc.cjs (done 2026-10-14: Vite React-TS ESLint config; fixed 5 `any` handler parameters in SimulationForm.tsx; `npm run lint` passes)
- [x] T003-109 Add CI (GitHub Actions) on push and pull request: `./gradlew test` (includes the generated-types check), `npm ci && npm test && npm run build`, `deno task test`, and `npm run lint` once T003-108 is done, in /.github/workflows/ci.yml (done 2026-10-14: api-server, frontend and backend jobs on push to main and on pull requests)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phases 3-6)**: All depend on Foundational phase completion
  - US003-1 (Phase 3): MVP - Should be completed first
  - US003-2 (Phase 4): Can start after Foundational, builds on US003-1
  - US003-3 (Phase 5): Can start after Foundational, builds on US003-1 and US003-2
  - US003-4 (Phase 6): Requires US003-1-US003-3 for comprehensive testing
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

```
Foundation (Phase 2)
    ↓
    ├─→ US003-1 (Phase 3) - MVP [Independent after foundation]
    │       ↓
    │   US003-2 (Phase 4) - Extends US003-1 [Can start after foundation, integrates with US003-1]
    │       ↓
    │   US003-3 (Phase 5) - Extends US003-1+US003-2 [Can start after foundation, integrates with US003-1+US003-2]
    │       ↓
    └─→ US003-4 (Phase 6) - Requires US003-1-US003-3 for full testing
            ↓
    Polish (Phase 7)
```

### Recommended Execution Order

1. **Sequential (Single Developer)**:
   - Phase 1 (Setup) → Phase 2 (Foundation) → Phase 3 (US003-1/MVP) → VALIDATE
   - Phase 4 (US003-2) → VALIDATE
   - Phase 5 (US003-3) → VALIDATE
   - Phase 6 (US003-4) → VALIDATE
   - Phase 7 (Polish) → FINAL VALIDATION

2. **Parallel (Multiple Developers)**:
   - After Phase 2 completes:
     - Dev A: Phase 3 (US003-1) → Phase 6 (US003-4)
     - Dev B: Phase 4 (US003-2) → Phase 7 (Polish tasks)
     - Dev C: Phase 5 (US003-3) → Phase 7 (Polish tasks)

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
T003-003: OneTimeExpense interface
T003-004: CashExpense class
T003-005: LoanExpense class

# All frontend type tasks can run in parallel:
T003-009: OneTimeExpense types
T003-010: CashExpense interface
T003-011: LoanExpense interface
```

**Within US003-1 (Phase 3)**:
```bash
# Frontend and API work can proceed in parallel:
Frontend Track: T003-015-T003-020
API Track: T003-021-T003-026
```

**Within Polish (Phase 7)**:
```bash
# Documentation and validation tasks:
T003-070, T003-071, T003-072, T003-073, T003-074: UI improvements (parallel)
T003-076, T003-077: API improvements (parallel)
T003-083, T003-084: Documentation (parallel)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T003-001-T003-002)
2. Complete Phase 2: Foundational (T003-003-T003-014) - CRITICAL
3. Complete Phase 3: User Story 1 (T003-015-T003-030)
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
     - Phase 3 (US003-1 - MVP) → Must complete first
     - Phase 6 (US003-4 - Edit/Remove)
   - **Developer B**:
     - Phase 4 (US003-2 - Multiple expenses)
     - Phase 7 (Polish - UI improvements)
   - **Developer C**:
     - Phase 5 (US003-3 - Loans)
     - Phase 7 (Polish - API improvements, docs)
3. **Integration Point**: All developers sync after their stories complete
4. **Final Sprint**: Team validates all stories work together (Phase 7 validation tasks)

---

## Validation Checkpoints

Signed off 2026-10-14: automated suites (`./gradlew test`, `npm test`, `deno task test`) plus the T003-085 browser walkthrough.

### After Foundation (Phase 2)
- [x] TypeScript types compile without errors
- [x] Kotlin models compile without errors
- [x] SimulationConfig accepts oneTimeExpenses array
- [x] No breaking changes to existing simulation functionality

### After US003-1 (Phase 3) - MVP
- [x] User can add single cash expense in form
- [x] Simulation runs without errors with one expense
- [x] Results table shows expense in correct year
- [x] Spend Bucket balance decreases correctly
- [x] Age-based and year-based timing both work
- [x] Spending strategy triggers when SB insufficient

### After US003-2 (Phase 4)
- [x] User can add 5+ expenses
- [x] Multiple expenses in same year sum correctly
- [x] 🔍 breakdown shows a "One-Time Expenses" section for years with expenses
- [x] Breakdown dialog opens and displays correctly
- [x] All US003-1 scenarios still work

### After US003-3 (Phase 5)
- [x] User can add loan expense
- [x] Quarterly payment calculates correctly (validate against external amortization table for quarterly compounding)
- [x] Loan payments span correct year range
- [x] 0% APR uses simple division
- [x] Breakdown shows loan annual totals
- [x] All US003-1 and US003-2 scenarios still work

### After US003-4 (Phase 6)
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

