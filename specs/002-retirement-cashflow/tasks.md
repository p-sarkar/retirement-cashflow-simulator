# Tasks: Retirement Cash Flow Simulator

**Feature**: `002-retirement-cashflow`
**Status**: Plan Approved
**Spec**: [specs/002-retirement-cashflow/spec.md](specs/002-retirement-cashflow/spec.md)

## Phase 1: Setup (Project Initialization)

**Goal**: Initialize the N-tier project structure (Frontend, Deno BFF, Kotlin API Server) and ensure all services can run and communicate.

- [x] T002-001 Initialize React frontend with Vite and TypeScript in `frontend/`
- [x] T002-002 Install frontend dependencies (MUI, Recharts, Axios) in `frontend/package.json`
- [x] T002-003 Initialize Deno backend project structure in `backend/`
- [x] T002-004 Create Deno `deno.json` configuration and `src/main.ts` entry point in `backend/`
- [x] T002-005 Initialize Kotlin API server with Gradle (Kotlin DSL) in `api-server/`
- [x] T002-006 Configure Gradle build for Ktor, Exposed, and SQLite in `api-server/build.gradle.kts`
- [x] T002-007 Create basic Ktor application entry point in `api-server/src/main/kotlin/com/retirement/Application.kt`
- [x] T002-008 [P] Configure CORS and basic routing in Deno BFF in `backend/src/main.ts`
- [x] T002-009 [P] Configure CORS and basic routing in Kotlin API Server in `api-server/src/main/kotlin/com/retirement/plugins/HTTP.kt`

## Phase 2: Foundational (Data Models & Infrastructure)

**Goal**: Establish the shared data models, database schema, and type definitions across all three tiers.

- [x] T002-010 Create `SimulationConfig` data class in `api-server/src/main/kotlin/com/retirement/model/SimulationConfig.kt`
- [x] T002-011 Create `SimulationResult` and related data classes in `api-server/src/main/kotlin/com/retirement/model/SimulationResult.kt`
- [x] T002-012 Define TypeScript interfaces for Simulation Config/Result in `backend/src/types.ts`
- [x] T002-013 Define TypeScript interfaces for Simulation Config/Result in `frontend/src/types/simulation.ts`
- [x] T002-013.1 Complete OpenAPI schema definitions in `specs/002-retirement-cashflow/contracts/api.yaml` (remove placeholder comments)
- [x] T002-014 Configure SQLite database connection using Exposed in `api-server/src/main/kotlin/com/retirement/data/DatabaseFactory.kt`
- [x] T002-015 Create `Simulations` table definition using Exposed in `api-server/src/main/kotlin/com/retirement/data/SimulationsTable.kt`
- [x] T002-015.1 Implement service to parse and ingest historical CSV data (inflation, returns) in `api-server/src/main/kotlin/com/retirement/data/HistoricalDataService.kt`

## Phase 3: User Story 1 - Interactive Simulation (P1)

**Goal**: Users can input financial details and view a 35-year cash flow simulation table.

**Note**: Per Constitution Principle I (Test-First Development), test definitions (T002-018) should be developed concurrently with logic implementation (T002-016-T002-017.x). The task ordering below reflects logical grouping, not execution order.

- [x] T002-016 [US002-1] Implement "Partha's Spending Strategy" logic in `api-server/src/main/kotlin/com/retirement/logic/SpendingStrategy.kt`
- [x] T002-017 [US002-1] Implement core `SimulationEngine` to calculate year-by-year cash flow in `api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt`
- [x] T002-017.1 [US002-1] Implement healthcare expense selection logic (Pre-Medicare vs Medicare) in `SimulationEngine.kt`
- [x] T002-017.2 [US002-1] Update `SpendingStrategy` to return detailed withdrawal breakdowns (TBA, TDA, Roth) and `SimulationEngine` to track/display all income components.
- [x] T002-017.3 [US002-1] Implement Social Security calculation logic (claim ages, spousal step-up) in `SimulationEngine.kt`
- [x] T002-017.4 [US002-1] Implement Pre-retirement logic: Adjust 'Wants' so Salary covers Expenses, saving all investment income.
- [x] T002-017.5 [US002-1] Implement Quarterly Crediting logic: Accrue Interest/Dividends monthly, credit to SB on Q1 start.
- [x] T002-017.6 [US002-1] Implement failure detection: Mark simulation failed if any account balance <= 0 before age 85.
- [x] T002-018 [US002-1] Create Unit Tests for `SimulationEngine` and `SpendingStrategy` logic in `api-server/src/test/kotlin/com/retirement/logic/`
- [x] T002-018.1 [US002-1] Verify JUnit test coverage >= 100% for SpendingStrategy and SimulationEngine classes
- [x] T002-019 [US002-1] Implement POST `/api/simulate` endpoint in `api-server/src/main/kotlin/com/retirement/api/SimulationRoutes.kt`
- [x] T002-020 [US002-1] Implement proxy route for `/api/simulate` in Deno BFF `backend/src/routes.ts`
- [x] T002-021 [US002-1] Create `SimulationForm` component for user inputs in `frontend/src/components/SimulationForm.tsx`
- [x] T002-022 [US002-1] Create `ResultsTable` component to display cash flow in `frontend/src/components/ResultsTable.tsx`
- [x] T002-022.1 [US002-1] Update `ResultsTable` to display separate columns for Needs, Wants, Healthcare, Taxes, etc. in `frontend/src/components/ResultsTable.tsx`
- [x] T002-022.2 [US002-1] Add Toggle to `ResultsTable` to switch between Yearly and Quarterly views.
- [x] T002-023 [US002-1] Integrate API client to call simulation endpoint in `frontend/src/services/api.ts`
- [x] T002-024 [US002-1] Assemble `SimulationPage` connecting Form, API, and Table in `frontend/src/pages/SimulationPage.tsx`
- [x] T002-024.1 [US002-1] Implement preset market conditions (e.g., "Historical Average", "2008 Crash", "Bull Market") in `api-server/src/main/kotlin/com/retirement/model/MarketPresets.kt`
- [x] T002-024.2 [US002-1] Add preset selector dropdown to `SimulationForm` in `frontend/src/components/SimulationForm.tsx`

## Phase 4: User Story 2 - Save & Load Simulations (P2)

**Goal**: Users can persist their simulations and reload them later.

- [x] T002-025 [US002-2] Implement Repository method to save simulation results to SQLite in `api-server/src/main/kotlin/com/retirement/data/SimulationRepository.kt`
- [x] T002-026 [US002-2] Implement Repository method to list and load simulations in `api-server/src/main/kotlin/com/retirement/data/SimulationRepository.kt`
- [x] T002-027 [US002-2] Implement POST/GET `/api/simulations` endpoints in `api-server/src/main/kotlin/com/retirement/api/SimulationRoutes.kt`
- [x] T002-028 [US002-2] Add proxy routes for persistence in Deno BFF `backend/src/routes.ts`
- [x] T002-029 [US002-2] Create `SavedSimulationsList` component in `frontend/src/components/SavedSimulationsList.tsx`
- [x] T002-030 [US002-2] Add "Save" button and logic to `SimulationPage` in `frontend/src/pages/SimulationPage.tsx`
- [x] T002-031 [US002-2] Add "Load" logic to populate form from saved data in `frontend/src/pages/SimulationPage.tsx`

## Phase 5: User Story 3 - Monte Carlo Analysis (P2)

**Goal**: Users can run 1000+ simulations with variable market conditions and view aggregate stats.

- [x] T002-032 [US002-3] Implement `MonteCarloEngine` with parallel execution in `api-server/src/main/kotlin/com/retirement/logic/MonteCarloEngine.kt`
- [x] T002-033 [US002-3] Implement POST `/api/simulate/monte-carlo` endpoint in `api-server/src/main/kotlin/com/retirement/api/MonteCarloRoutes.kt`
- [x] T002-034 [US002-3] Proxy Monte Carlo endpoint in Deno BFF `backend/src/routes.ts`
- [x] T002-035 [US002-3] Create `MonteCarloChart` component using Recharts in `frontend/src/components/MonteCarloChart.tsx`
- [x] T002-036 [US002-3] Add Monte Carlo section to `SimulationPage` in `frontend/src/pages/SimulationPage.tsx`

## Phase 6: User Story 4 - Drill-down Details (P3)

**Goal**: Users can inspect specific Monte Carlo paths.

- [x] T002-037 [US002-4] Add interactive tooltip and click handler to `MonteCarloChart` in `frontend/src/components/MonteCarloChart.tsx`
- [x] T002-038 [US002-4] Implement logic to fetch/display full details for a selected run in `frontend/src/pages/SimulationPage.tsx`
- [x] T002-039 [US002-4] (Optional) API optimization to support fetching single run details if not returned in bulk in `api-server/src/main/kotlin/com/retirement/api/MonteCarloRoutes.kt`

## Phase 7: Polish & Cross-Cutting

**Goal**: Finalize UI/UX, error handling, and documentation.

- [x] T002-040 Implement global error handling in Deno BFF (transform downstream errors) in `backend/src/middleware/errorHandler.ts`
- [x] T002-041 Improve UI styling (MUI Theme, Spacing, Responsive) in `frontend/src/App.css`
- [x] T002-041.1 Reorganize SimulationForm into compact columnar layout with sections flowing left-to-right in `frontend/src/components/SimulationForm.tsx`
- [x] T002-041.2 Remove vertical centering from page layout (body and root elements) in `frontend/src/index.css` and `frontend/src/App.css`
- [x] T002-041.3 Align form to top of page with minimal spacing in `frontend/src/pages/SimulationPage.tsx`
- [x] T002-041.4 Configure AccordionDetails to align content to flex-start in `frontend/src/pages/SimulationPage.tsx`
- [x] T002-041.5 Use small-sized text fields with fixed widths and increased vertical spacing in `frontend/src/components/SimulationForm.tsx`
- [x] T002-042 Add loading states and spinners during simulation runs in `frontend/src/pages/SimulationPage.tsx`
- [x] T002-052 [US002-1] Export the results table to CSV in the current view (yearly or quarterly), same columns as the table (FR002-010a, from T001-038) in `frontend/src/utils/csvExport.ts`, `frontend/src/components/ResultsTable.tsx`
- [x] T002-043 Verify all success criteria and edge cases:
  - [x] T002-043.1 Test edge case: Zero initial balance in all accounts
  - [x] T002-043.2 Test edge case: Market crash in Year 1 of retirement
  - [x] T002-043.3 Test edge case: Deflation (negative inflation rate)
  - [x] T002-043.4 Test edge case: Missing/invalid input field validation
- [x] T002-044 Update `README.md` with final run instructions for all 3 tiers

**Status (Phases 1-7)**: Done 2026-10-15. Tests: JUnit MarketPresetsTest (6), MarketScenarioEngineTest (5), MonteCarloEngineTest (12), SimulationRepositoryTest (4), PersistenceAndMonteCarloRoutesTest (10), EdgeCasesTest (10, T002-043.1-T002-043.4); deno routes.test.ts (11); Vitest formConfig, csvExport, monteCarloChart, ResultsTable, SavedSimulationsList, MonteCarloChart/MonteCarloResults, SimulationForm (+10), SimulationPage (4). Checked in the running app: presets, Monte Carlo (1000 runs in about 0.25 s), hover/click/Load Details, Save/Load, CSV export.
Notes:
- T002-039 was required, not optional: the analysis returns only each run's balance path and summary; `POST /api/simulate/monte-carlo/run` rebuilds one run from (config, seed, runIndex).
- T002-015.1 follow-up: HistoricalDataService read a returns file that does not exist; it now derives annual S&P 500 returns from `kaggle-SPX-since1927-monthly.csv` (1928-2019).
- SimulationEngine now computes market performance vs 12 months prior point-to-point (was a fixed 1.0 placeholder) and takes per-year inflation and bond yields.
- Market presets are served by `GET /api/market-presets`. The table and CSV share one column list (`frontend/src/utils/resultColumns.ts`). The Monte Carlo section is `MonteCarloResults.tsx`.
- T002-043.4: ConfigValidator rejects cleared (0) ages and year, claim ages outside 62-70 and negative amounts (422 per field).

## Phase 8: Configurable End Age (FR002-001a, added 2026-10-14)

**Goal**: Replace the fixed simulation end age (85) with an End Age setting on the configuration form. Replaces T003-107.

**Status**: Done 2026-10-14. Tests: EndAgeTest (9), ConfigValidatorTest, SimulationForm.test, expenseUtils.test. T002-048: the remaining 85s in BreakdownGenerator are CBB milestones and the ATH threshold, which stay fixed; the results summary now reads the age from the last row

- [x] T002-045 [P] JUnit first: End Age default (85 when omitted), validation (whole number, > Current Age, ≤ 110, 422 on `endAge`), results end at End Age, failure check uses End Age, CBB milestones unchanged, one-time expense window follows End Age in /api-server/src/test/kotlin/com/retirement/
- [x] T002-046 Add optional `endAge: Int = 85` to SimulationConfig and regenerate contract types (`./gradlew generateTsContract`) in /api-server/src/main/kotlin/com/retirement/model/SimulationConfig.kt, /frontend/src/types/contract.generated.ts, /backend/src/contract.generated.ts
- [x] T002-047 Derive the simulation period from `config.endAge` instead of the `END_AGE` constant; validate End Age in ConfigValidator (replaces the currentAge < 85 check) in /api-server/src/main/kotlin/com/retirement/model/SimulationPeriod.kt, /api-server/src/main/kotlin/com/retirement/logic/ConfigValidator.kt, SimulationEngine.kt
- [x] T002-048 Replace remaining hard-coded 85s that mean "end of simulation" (not CBB milestones) in the breakdown and results summary in /api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt, /frontend/src/components/ResultsTable.tsx
- [x] T002-049 [P] Add an End Age field to the General section with inline validation that blocks Run; pass End Age to one-time expense validation and remove the mirrored `SIMULATION_END_AGE` in /frontend/src/components/SimulationForm.tsx, /frontend/src/components/OneTimeExpenseInput.tsx, /frontend/src/utils/expenseUtils.ts
- [x] T002-050 [P] Vitest: End Age field default, validation and submit; one-time expense window follows End Age in /frontend/src/components/SimulationForm.test.tsx, /frontend/src/utils/expenseUtils.test.ts
- [x] T002-051 Update docs: FR003-041 and its "Simulation period" assumption, API contracts, data models, README in /specs/002-retirement-cashflow/, /specs/003-one-time-expenses/, /README.md

## Phase 9: Working Years and Spend Bucket Reconciliation (added 2026-10-14)

**Goal**: Working years (age <= Retirement Age) have no income gap, the Spend Bucket reconciles every year, and the SB never runs dry at quarter end.

**Status**: Done 2026-10-14. Tests: WorkingYearsTest (6), SpendingStrategyTest (4), SimulationForm.test. Checked in the running app with the default configuration: `~` for ages 51–55, AIG from 56, working-year breakdown shows "Not applicable", SB breakdown reconciles, no console errors. T002-061–T002-062 (follow-ups): SimulationEngineTest (+2), WorkingYearsTest (+1)

- [x] T002-053 [P] JUnit first: no AIG in the start year and working years, retirement-age year is a full working year, SB reconciles every year, working years save exactly interest + dividends, Roth conversions do not reduce wants, retirement years withdraw full expenses in /api-server/src/test/kotlin/com/retirement/logic/WorkingYearsTest.kt
- [x] T002-054 Use one working-year test (`age <= retirementAge`) for salary, wants, healthcare, growth rate, Roth conversion amount and the spending strategy; make `Metrics.annualIncomeGap` nullable and regenerate contract types in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt, /api-server/src/main/kotlin/com/retirement/model/SimulationResult.kt, /frontend/src/types/contract.generated.ts, /backend/src/contract.generated.ts
- [x] T002-055 Withdraw full regular expenses from the SB each quarter (withdrawing only the AIG counted passive income twice); record strategy refills and shortfall cover as the actual SB change; drop Roth conversions from the working-year wants formula in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T002-056 [P] JUnit first: SB floor and deficit repayment in both refill paths in /api-server/src/test/kotlin/com/retirement/logic/SpendingStrategyTest.kt
- [x] T002-057 SB Floor = max(0.5 × AIG, 0.25 × regular expenses); SB Cap never below it; refills add the SB Deficit; down-market "no movement" check uses the floor in /api-server/src/main/kotlin/com/retirement/logic/SpendingStrategy.kt
- [x] T002-058 Breakdown: "Not applicable" income gap in working years, full-expense SB withdrawal, net salary excludes Roth contributions, only spending TDA withdrawals reach the SB, SB Cap formula in /api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt
- [x] T002-059 [P] Show `~` in the Income Gap column when the gap is null in /frontend/src/components/ResultsTable.tsx
- [x] T002-060 Update docs: spec 002 working years, quarterly SB operations, SB floor and deficit; data model Metrics in /specs/002-retirement-cashflow/spec.md, /specs/002-retirement-cashflow/data-model.md
- [x] T002-061 Use the post-retirement interest estimate (SB holds about half a year of expenses) in the AIG passed to the spending strategy; it was computed but unused, and the actual SB balance was used instead, in /api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt
- [x] T002-062 Make `Metrics.sbCap` null in working years (not applied), show `~` in the SB Cap column and "Not applicable" in the breakdown; regenerate contract types in /api-server/src/main/kotlin/com/retirement/model/SimulationResult.kt, SimulationEngine.kt, BreakdownGenerator.kt, /frontend/src/components/ResultsTable.tsx

## Dependencies & Execution Order

1. **Phase 1 (Setup)** must be completed first. T002-001-T002-007 are foundational.
2. **Phase 2 (Models)** blocks all subsequent phases. T002-010-T002-015 ensure data contract alignment.
3. **Phase 3 (US002-1)** is the MVP. Complete T002-016-T002-024 to get a working interactive simulator.
4. **Phase 4 (US002-2)** and **Phase 5 (US002-3)** can be executed in parallel after Phase 3, though US002-2 is simpler.
5. **Phase 6 (US002-4)** depends on Phase 5.

## Parallel Execution Examples

- **Setup**: One developer sets up Kotlin (T002-005-T002-007), another sets up React (T002-001-T002-002).
- **US002-1**: Backend logic (T002-016-T002-019) can proceed while Frontend UI (T002-021-T002-022) is built using mock data.
- **US002-2**: API persistence (T002-025-T002-027) can be built parallel to Frontend Save UI (T002-029-T002-030).

## Implementation Strategy

1. **MVP (Days 1-2)**: Complete Phases 1, 2, and 3. Result: Working interactive simulator with hardcoded inputs or basic form.
2. **Persistence (Day 3)**: Complete Phase 4. Result: Ability to save/load.
3. **Advanced Features (Day 4)**: Complete Phases 5 and 6. Result: Monte Carlo analysis.
