---
description: "Task list for the Yearly Computation Breakdown feature"
---

# Tasks: Yearly Computation Breakdown

**Input**: Design documents from `specs/004-yearly-breakdown/`
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/breakdown-api.md](./contracts/breakdown-api.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The constitution (I. Test-First) and CLAUDE.md require a failing test before each behaviour
change. Write each test task first and confirm it fails before the implementation task that follows it.

**Organization**: One phase per user story, in spec priority order. Each story adds one section to the breakdown
and can be demonstrated on its own.

**Delivery**: One feature branch (`feat/004-yearly-breakdown`). Until a new section replaces it, the old
breakdown content stays visible: the response carries it as a temporary `legacySections` field, shown under
"More details" in the dialog. Polish (T004-039) removes it. So the app never loses information between phases.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: Which user story the task belongs to (US004-1–US004-5)

## Path Conventions

- API server: `api-server/src/main/kotlin/com/retirement/`, tests in `api-server/src/test/kotlin/com/retirement/`
- BFF: `backend/src/`, tests in `backend/tests/`
- Frontend: `frontend/src/`

---

## Phase 1: Setup

**Purpose**: Shared test fixtures and a green baseline

- [x] T004-001 Run the full suites on a clean `feat/004-yearly-breakdown` checkout and record the counts: `cd api-server && ./gradlew test`, `cd backend && deno task test && deno lint`, `cd frontend && npm run lint && npm test && npm run build`; revert `api-server/src/main/resources/version.properties` afterwards
- [x] T004-002 [P] Add breakdown fixtures to api-server/src/test/kotlin/com/retirement/TestConfigs.kt: `defaultFormConfig()` mirroring `defaultConfig` in frontend/src/components/SimulationForm.tsx (rates as decimals, one-time loans included), `presetConfigs()` returning `defaultFormConfig()` with each `MarketPresets.all` preset applied (rates and `marketPresetId`), and `runTraced(config)` returning the `SimulationResult` and the trace (compiles once T004-006 exists)

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: The engine trace, the new response type, and the route and dialog switched over to it, with the old
content still shown

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Tests first

- [x] T004-003 [P] Write api-server/src/test/kotlin/com/retirement/logic/SimulationTraceTest.kt: (a) for `testConfig()` and every `presetConfigs()` entry, `runSimulation` with a trace returns a `SimulationResult` equal to one without (FR004-006); (b) replaying every recorded `Movement` in order from `config.portfolio` reproduces each `QuarterlyResult.balances` to within $0.01 (SC004-001); (c) every movement has `amount > 0` and `from != to`, and `quarter` in 1–4; (d) the zero-balance config from EdgeCasesTest ("the plan fails once retired") records a `FailureMark` in its failure year with the first failing quarter
- [x] T004-004 [P] Extend api-server/src/test/kotlin/com/retirement/logic/SpendingStrategyTest.kt: `executeQuarterly` reports `rule` and `limitedBy` for each branch (market up with CBB full, market up with CBB below cap, market down holding, market down from CBB) and splits refills into `tdaToSb`, `tbaToSb`, `tdaToCbb`, `tbaToCbb`, `cbbToSb` that sum to the existing totals; `coverShortfall` reports `cbbToSb`, `tdaToSb`, `tbaToSb` — Done: `coverShortfall` needed no new fields; its existing `cbbWithdrawal`, `tdaWithdrawal` and `tbaWithdrawal` all go to the SB

### Implementation

- [x] T004-005 Create api-server/src/main/kotlin/com/retirement/model/YearBreakdown.kt with the `@Serializable` models from data-model.md "API: YearBreakdown": `YearBreakdown`, `LineItem`, `StartingPosition`, `EconomicConditions`, `HouseholdStatus`, `ExpenseSection`, `IncomeSection`, `IncomeGapSection`, `MoneyMovementSection`, `AccountLedger`, `LedgerRow`, `Flow`, `QuarterStrategy`, `FailureMark`, `TaxSection`, `TaxComputation`, `NextYearSection`, `ProjectedItem` and the enums `YearPhase` (`WORKING` | `RETIREMENT` | `START`), `MovementType`, `Endpoint`, `EquityReturnSource`, `HealthcareBracket`, `StrategyRule`, `RefillLimit`; every section field nullable except `startingPosition`; plus a temporary `legacySections: List<BreakdownSection>`
- [x] T004-006 Create api-server/src/main/kotlin/com/retirement/logic/SimulationTrace.kt (internal, not serialized): `SimulationTrace` (`startYearTaxableIncomeEstimate`, `years: Map<Int, YearTrace>`), `YearTrace` (`plan`, `conditions`, `movements`, `quarterStrategy`, `failure`), `YearPlan` (fields per data-model.md "YearPlan") and `Movement` (`quarter` 1–4; `type`; `from`/`to` "never equal"; `amount` "> 0; the direction is from → to (market losses are `MARKET` ← account)"), reusing the enums from T004-005
- [x] T004-007 Add the outputs from T004-004 to `SpendingResult` and `ShortfallResult` in api-server/src/main/kotlin/com/retirement/logic/SpendingStrategy.kt without changing any decision or amount; make T004-004 pass
- [x] T004-008 Add `trace: SimulationTrace? = null` to `SimulationEngine.runSimulation` in api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt and, only when it is non-null, record: the start-year taxable income estimate; each year's `YearPlan` (after the start-of-year estimates) and conditions (inflation factor, equity return used, market value / all-time high at the start of the year); a `Movement` at every balance change (interest, dividends, strategy refills by route, Roth conversion, take-home salary, 401k/Roth/TBA contributions, Social Security, regular expenses, each one-time expense payment, shortfall cover by route, market growth per account); each retirement quarter's `QuarterStrategy` (rule, limit, market vs 12 months prior and vs all-time high); and the first `FailureMark`. Make T004-003 pass
- [x] T004-009 Move the current section builders from api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt into a new api-server/src/main/kotlin/com/retirement/logic/LegacyBreakdownSections.kt unchanged; give `BreakdownGenerator` a new entry point `generate(config, result, trace, targetAge): YearBreakdown` that sets year, age, spouse age, `phase` (`START` for the start-year age, else working/retirement), `isLastYear`, `apiMetadata` and `legacySections`, leaving the new sections null for now — Done: `startingPosition` is a required field, so the full Starting Position is built here (US004-3 adds its tests and UI)
- [x] T004-010 Switch `/api/simulate/breakdown` in api-server/src/main/kotlin/com/retirement/api/SimulationRoutes.kt to run a traced simulation and respond with `YearBreakdown` (400 for an age outside the simulation, as today); update api-server/src/test/kotlin/com/retirement/api/SimulationRoutesTest.kt and the breakdown helper in api-server/src/test/kotlin/com/retirement/logic/OneTimeExpenseScenarioTest.kt to read `legacySections` — Done: an age outside the simulation now returns a JSON 400 (`ErrorResponse`), as CLAUDE.md requires
- [x] T004-011 Replace `ComputationBreakdown` with `YearBreakdown` in the roots of api-server/src/test/kotlin/com/retirement/contract/TsContractGenerator.kt and run `cd api-server && ./gradlew generateTsContract` (updates frontend/src/types/contract.generated.ts and backend/src/contract.generated.ts); `./gradlew test` passes
- [x] T004-012 [P] Update backend/src/routes.ts (`ComputationBreakdown` → `YearBreakdown`) and the breakdown cases in backend/tests/routes.test.ts and backend/tests/simulate.test.ts; `deno task test` and `deno lint` pass
- [x] T004-013 [P] Update `getBreakdown` in frontend/src/services/api.ts to return `YearBreakdown`; rebuild frontend/src/components/BreakdownDialog.tsx as seven accordions in spec order (Starting Position, Expenses, Income, Income Gap, Money Movement, Taxes, Next Year), each rendered only when its section is non-null, plus a "More details" accordion rendering `legacySections` as today; update frontend/src/components/BreakdownDialog.test.tsx

**Checkpoint**: The app works as before, with the dialog fed by the new response. Traced runs are proven identical
and fully reconciled.

---

## Phase 3: User Story 1 - Follow every dollar through the year (Priority: P1) 🎯 MVP

**Goal**: The Money Movement section: a ledger per account per quarter, the year's money-flow diagram, each
quarter's strategy decision, the failure mark, and quarterly rows opening the breakdown on their quarter.

**Independent Test**: Open any year's breakdown and reconcile each account from opening to closing using only
Money Movement; Q4 closing matches the results row (spec US004-1).

### Tests for User Story 1

- [x] T004-014 [P] [US004-1] Write api-server/src/test/kotlin/com/retirement/logic/BreakdownMoneyMovementTest.kt: for `defaultFormConfig()` and every `presetConfigs()` entry, every simulated year: ledgers are SB, CBB, TBA, TDA, TFA in that order; `closing[q] = opening[q] + Σ rows.amounts[q]`; `opening[q+1] = closing[q]`; `closing[3]` equals the results-row balance (FR004-050); every row has a `type` and `counterparty` (FR004-051); flows have `amount > 0` and sum, per account, to the ledger year totals (FR004-055); retirement years have 4 `quarterStrategy` entries with a rule and limit, working years none (FR004-052); the failing zero-balance config marks the failure quarter, account and condition and still shows 4 quarters (FR004-054)
- [x] T004-015 [P] [US004-1] Write frontend/src/components/breakdown/MoneyMovementSection.test.tsx: a ledger table per account with Q1–Q4 and Year columns, opening and closing rows; the `highlightQuarter` column is marked; strategy notes and a failure alert render when present; and frontend/src/utils/sankeyData.test.ts: `toSankeyData(flows)` produces one node per endpoint used and one link per flow with matching values

### Implementation for User Story 1

- [x] T004-016 [US004-1] Build `MoneyMovementSection` in api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt from the year's trace: ledgers (rows grouped by type and counterparty, signed + in / − out), flows summed by (from, to), quarter strategy with a plain-words explanation, failure mark (FR004-050–FR004-055); make T004-014 pass — Done: flows are netted per pair of endpoints (money can move both ways in a year; a Sankey can't draw cycles)
- [x] T004-017 [P] [US004-1] Create frontend/src/utils/sankeyData.ts (`toSankeyData`) and frontend/src/components/breakdown/MoneyFlowSankey.tsx using Recharts `Sankey`, with node labels and link amounts in dollars — Done: nodes are labelled with name and amount (Recharts draws bare rectangles by default)
- [x] T004-018 [P] [US004-1] Create frontend/src/components/breakdown/AccountLedgerTable.tsx (rows = movement type and counterparty, columns = Q1–Q4 and Year, opening and closing rows, optional highlighted quarter)
- [x] T004-019 [US004-1] Create frontend/src/components/breakdown/MoneyMovementSection.tsx (diagram above the five ledgers, quarter strategy notes, failure alert) and render it in frontend/src/components/BreakdownDialog.tsx; make T004-015 pass
- [x] T004-020 [US004-1] Pass the clicked quarter from quarterly rows in frontend/src/components/ResultsTable.tsx to `BreakdownDialog` as `initialQuarter`; the dialog then opens Money Movement and highlights that quarter (FR004-008); add the case to frontend/src/components/BreakdownDialog.test.tsx

**Checkpoint**: US004-1 complete. Money movement is fully explained and reconciled; the other new sections are still
empty, and "More details" still shows the old content.

---

## Phase 4: User Story 2 - See the year's expenses, income and income gap (Priority: P2)

**Goal**: The Expenses, Income and Income Gap sections, with the old strategy and caps content folded in (FR004-007).

**Independent Test**: For a working and a retirement year, each section's totals match the results row and every
line shows its inputs (spec US004-2).

### Tests for User Story 2

- [x] T004-021 [P] [US004-2] Write api-server/src/test/kotlin/com/retirement/logic/BreakdownExpenseIncomeGapTest.kt over every year of `defaultFormConfig()` and the presets: Expenses items and `total` match the results row (FR004-020, FR004-002); in working years `wantsDerivation` starts at salary and subtracts each other expense and contribution to give Wants (FR004-021); each one-time expense is listed with name, type and amount, loan payments `inflationAdjusted = false` (FR004-022); Income: `incomeTotal` + withdrawals = results-row Total Income, take-home = gross − 401k − Roth − TBA, Social Security per earner with spousal step-up (FR004-030–FR004-032); Income Gap: retirement years show estimated and actual gaps each equal to expenses − passive income, the actual gap equals the results row's, `sbCap` and `cbbCap` equal the row's metrics, the CBB-performance threshold has `evaluated = false`; working years have null estimates and show take-home salary against expenses (FR004-040–FR004-042)
- [x] T004-022 [P] [US004-2] Move the one-time expense breakdown checks (T003-040–T003-043, T003-079) in api-server/src/test/kotlin/com/retirement/logic/OneTimeExpenseScenarioTest.kt from the legacy "One-Time Expenses" section to `expenses.oneTimeExpenses` (FR003-033-v2 still holds)
- [x] T004-023 [P] [US004-2] Write frontend tests frontend/src/components/breakdown/LineItemTable.test.tsx and frontend/src/components/breakdown/ExpenseIncomeGapSections.test.tsx: line items show label, amount, rule and inputs; today's-dollar amounts show where present; the working-year Income Gap message renders

### Implementation for User Story 2

- [x] T004-024 [US004-2] Build `ExpenseSection` and `IncomeSection` in api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt from the trace's `YearPlan` and the results row — Done: Total Income in the results table counts each Roth conversion twice (as a TDA withdrawal and as a Roth conversion); the Income section lists both parts and says so, without changing the table (FR004-006); issue #16. Fixed by PR #18: Total Income now excludes Roth conversions, shown as transfers
- [x] T004-025 [US004-2] Build `IncomeGapSection` in api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt, including Cap AIG, SB floor, SB cap, CBB cap with its age reductions and the market thresholds (FR004-041, FR004-007); make T004-021 and T004-022 pass
- [x] T004-026 [P] [US004-2] Create frontend/src/components/breakdown/LineItemTable.tsx and frontend/src/components/breakdown/ExpensesSection.tsx, IncomeSection.tsx, IncomeGapSection.tsx; render them in frontend/src/components/BreakdownDialog.tsx; make T004-023 pass

**Checkpoint**: US004-1 and US004-2 complete.

---

## Phase 5: User Story 3 - Know where the year started and the conditions it ran under (Priority: P3)

**Goal**: The Starting Position section (FR004-010–FR004-013).

**Independent Test**: In a "2008 Crash" plan, the first retirement year shows an equity return of −37% attributed
to the preset's year 1 (spec US004-3).

### Tests for User Story 3

- [x] T004-027 [P] [US004-3] Write api-server/src/test/kotlin/com/retirement/logic/BreakdownStartingPositionTest.kt: opening balances equal the prior results row's balances, or `config.portfolio` in the first simulated year (FR004-010); conditions per year, with the "crash-2008" preset's first five retirement years `PRESET_SEQUENCE`, `presetName` "2008 Crash", `presetSequenceYear` 1–5 and returns −0.37, 0.265, 0.151, 0.021, 0.16, other years the configured rate (FR004-011); `marketVsAllTimeHigh` recorded (FR004-012); healthcare bracket and each earner's claiming status (FR004-013); the start-year age returns `phase = START` with only `startingPosition` — Done: passed on first run, since T004-009 built the section early (see its note)
- [x] T004-028 [P] [US004-3] Write frontend/src/components/breakdown/StartingPositionSection.test.tsx: balances, rates with the preset attribution, household status — Done: passed on first run, since T004-009 built the section early (see its note)

### Implementation for User Story 3

- [x] T004-029 [US004-3] Build `StartingPosition` in api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt (preset attribution from `MarketPresets.find(config.marketPresetId)` and the first retirement age); make T004-027 pass
- [x] T004-030 [P] [US004-3] Create frontend/src/components/breakdown/StartingPositionSection.tsx, render it in frontend/src/components/BreakdownDialog.tsx (open by default); make T004-028 pass

**Checkpoint**: US004-1–US004-3 complete.

---

## Phase 6: User Story 4 - Understand the year's taxes (Priority: P4)

**Goal**: The Taxes section (FR004-060–FR004-062).

**Independent Test**: In a year with a Roth conversion, the conversion is taxable income this year and part of next
year's bill (spec US004-4).

### Tests for User Story 4

- [x] T004-031 [P] [US004-4] Write api-server/src/test/kotlin/com/retirement/logic/BreakdownTaxesTest.kt: `billThisYear.tax` equals the results-row Income Tax and its components are the prior year's (basis `PRIOR_YEAR`); the first simulated year has basis `START_YEAR_ESTIMATE` equal to the trace's estimate; `generatedThisYear` lists salary, interest, dividends, Social Security, TDA withdrawals, Roth conversions and TBA withdrawals with `taxedShare` 0.5 for TBA, and its `tax` equals the next year's Income Tax; simplifications are listed. Salary is counted gross, as the engine did then (issue #13; fixed by PR #18: salary net of 401k contributions)
- [x] T004-032 [P] [US004-4] Write frontend/src/components/breakdown/TaxesSection.test.tsx

### Implementation for User Story 4

- [x] T004-033 [US004-4] Build `TaxSection` in api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt; make T004-031 pass — Done: the engine's taxable income counts each Roth conversion twice (within TDA withdrawals and again as a Roth conversion); the section reports it as computed and lists it among the simplifications; issue #15. Fixed by PR #18: each conversion is taxed once, and the simplification note was removed
- [x] T004-034 [P] [US004-4] Create frontend/src/components/breakdown/TaxesSection.tsx and render it in frontend/src/components/BreakdownDialog.tsx; make T004-032 pass

**Checkpoint**: US004-1–US004-4 complete.

---

## Phase 7: User Story 5 - See what next year looks like (Priority: P5)

**Goal**: The Next Year section (FR004-070–FR004-073).

**Independent Test**: For every year except the last, projected items marked exact equal the next year's actual
values (spec US004-5, SC004-003).

### Tests for User Story 5

- [x] T004-035 [P] [US004-5] Write api-server/src/test/kotlin/com/retirement/logic/BreakdownNextYearTest.kt over `defaultFormConfig()` and the presets: every `exact` expense and income item equals the next year's actual value (FR004-071, SC004-003); interest, dividends and the income gap are `exact = false`; `changes` names retirement starting (the year before the first retirement year), Social Security starting, Medicare at 65, a loan ending, and CBB cap step-downs (FR004-072); the last simulated year has `nextYear = null` and `isLastYear = true` (FR004-073)
- [x] T004-036 [P] [US004-5] Write frontend/src/components/breakdown/NextYearSection.test.tsx, including the "simulation ends with this year" message

### Implementation for User Story 5

- [x] T004-037 [US004-5] Build `NextYearSection` in api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt from the next year's `YearTrace.plan` and a comparison with this year; make T004-035 pass
- [x] T004-038 [P] [US004-5] Create frontend/src/components/breakdown/NextYearSection.tsx and render it in frontend/src/components/BreakdownDialog.tsx; make T004-036 pass

**Checkpoint**: All seven sections in place.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [x] T004-039 Remove `legacySections`, api-server/src/main/kotlin/com/retirement/logic/LegacyBreakdownSections.kt and the `ComputationBreakdown`, `BreakdownSection` and `ComputationStep` models; regenerate the contract (`./gradlew generateTsContract`); drop "More details" from frontend/src/components/BreakdownDialog.tsx; update tests that still referenced them — Done: the one setting the old sections showed that no new section had (TDA share of each equity withdrawal) was added to Income Gap first
- [x] T004-040 [P] Add a breakdown test asserting every amount shared with the results row matches, across all years and presets (SC004-002), and that `POST /api/simulate/breakdown` for a 60-year plan completes in under 1 second (SC004-005) in api-server/src/test/kotlin/com/retirement/logic/BreakdownConsistencyTest.kt
- [x] T004-041 [P] Replace the `ComputationBreakdown` schema with `YearBreakdown` in specs/002-retirement-cashflow/contracts/api.yaml, per specs/004-yearly-breakdown/contracts/breakdown-api.md
- [x] T004-042 [P] Update the breakdown description in README.md and TESTING-BREAKDOWN.md; add a note to specs/003-one-time-expenses/spec.md that FR004-033-v2 is now met by the Expenses section — Done: TESTING-BREAKDOWN.md described spec 003's superseded column icon, so it was rewritten for the new breakdown
- [x] T004-043 Run the full suites (`./gradlew test`; `deno task test` and `deno lint`; `npm run lint && npm test && npm run build`) and revert the build-time change in api-server/src/main/resources/version.properties — Done 2026-10-16: Kotlin 168, Deno 18 + lint, frontend lint, Vitest 119 and build pass
- [x] T004-044 Restart the API server (CLAUDE.md) and walk through specs/004-yearly-breakdown/quickstart.md §3 in the app, including the 2008 Crash preset, a quarterly row and the last year; check the browser console for errors; mark tasks done and set the spec status — Done 2026-10-16 in headless Chrome: all quickstart §3 steps pass, no console errors. The restart command in CLAUDE.md and README also killed the BFF (lsof without -sTCP:LISTEN matches its connection); fixed in both

---

## Dependencies & Execution Order

### Phases

- **Setup (Phase 1)**: none
- **Foundational (Phase 2)**: after Setup; blocks all stories
- **US004-1–US004-5 (Phases 3–7)**: each needs only Phase 2. They touch the same `BreakdownGenerator.kt` and
  `BreakdownDialog.tsx`, so run them one after another in priority order unless those edits are coordinated
- **Polish (Phase 8)**: after all stories (T004-039 removes the legacy content they replace)

### Within Phase 2

T004-003, T004-004 (tests) → T004-005 → T004-006 → T004-007 → T004-008 → T004-009 → T004-010 → T004-011 → T004-012, T004-013

### Within each story

Tests ([P], written first and failing) → API-server builder → frontend components → dialog wiring

## Parallel Opportunities

- **Phase 1**: T004-002 alongside T004-001
- **Phase 2**: T004-003 and T004-004; then T004-012 and T004-013 once T004-011 has regenerated the types
- **US004-1**: T004-014 and T004-015; then T004-017 and T004-018 while T004-016 is under way
- **US004-2**: T004-021, T004-022 and T004-023; then T004-026 alongside T004-024/T004-025
- **US004-3–US004-5**: each story's backend and frontend test tasks, and its frontend component task alongside the
  backend builder

### Example: User Story 1

```text
Together: T004-014 (BreakdownMoneyMovementTest.kt) and T004-015 (MoneyMovementSection.test.tsx, sankeyData.test.ts)
Then:     T004-016 (BreakdownGenerator money movement) with T004-017 (Sankey) and T004-018 (ledger table) in parallel
Then:     T004-019 (section + dialog), T004-020 (quarterly rows)
```

## Implementation Strategy

### MVP (User Story 1)

1. Phase 1 and Phase 2: the trace is proven identical and reconciled, and the dialog runs on the new response
   with the old content under "More details"
2. Phase 3 (US004-1): Money Movement
3. Stop and validate: reconcile a few years by hand in the app (quickstart §3 step 3)

### Incremental delivery

Each later story adds one section and can be checked on its own. "More details" keeps the rest visible until
Polish removes it. The feature is merged once all phases and quickstart §3 pass, in one PR or one PR per story.
