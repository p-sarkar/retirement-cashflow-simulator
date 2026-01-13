# Tasks: Retirement Cash Flow Simulator

**Feature**: `002-retirement-cashflow`
**Status**: Plan Approved
**Spec**: [specs/002-retirement-cashflow/spec.md](specs/002-retirement-cashflow/spec.md)

## Phase 1: Setup (Project Initialization)

**Goal**: Initialize the N-tier project structure (Frontend, Deno BFF, Kotlin API Server) and ensure all services can run and communicate.

- [x] T001 Initialize React frontend with Vite and TypeScript in `frontend/`
- [x] T002 Install frontend dependencies (MUI, Recharts, Axios) in `frontend/package.json`
- [x] T003 Initialize Deno backend project structure in `backend/`
- [x] T004 Create Deno `deno.json` configuration and `src/main.ts` entry point in `backend/`
- [x] T005 Initialize Kotlin API server with Gradle (Kotlin DSL) in `api-server/`
- [x] T006 Configure Gradle build for Ktor, Exposed, and SQLite in `api-server/build.gradle.kts`
- [x] T007 Create basic Ktor application entry point in `api-server/src/main/kotlin/com/retirement/Application.kt`
- [x] T008 [P] Configure CORS and basic routing in Deno BFF in `backend/src/main.ts`
- [x] T009 [P] Configure CORS and basic routing in Kotlin API Server in `api-server/src/main/kotlin/com/retirement/plugins/HTTP.kt`

## Phase 2: Foundational (Data Models & Infrastructure)

**Goal**: Establish the shared data models, database schema, and type definitions across all three tiers.

- [x] T010 Create `SimulationConfig` data class in `api-server/src/main/kotlin/com/retirement/model/SimulationConfig.kt`
- [x] T011 Create `SimulationResult` and related data classes in `api-server/src/main/kotlin/com/retirement/model/SimulationResult.kt`
- [x] T012 Define TypeScript interfaces for Simulation Config/Result in `backend/src/types.ts`
- [x] T013 Define TypeScript interfaces for Simulation Config/Result in `frontend/src/types/simulation.ts`
- [ ] T013.1 Complete OpenAPI schema definitions in `specs/002-retirement-cashflow/contracts/api.yaml` (remove placeholder comments)
- [x] T014 Configure SQLite database connection using Exposed in `api-server/src/main/kotlin/com/retirement/data/DatabaseFactory.kt`
- [x] T015 Create `Simulations` table definition using Exposed in `api-server/src/main/kotlin/com/retirement/data/SimulationsTable.kt`
- [x] T015.1 Implement service to parse and ingest historical CSV data (inflation, returns) in `api-server/src/main/kotlin/com/retirement/data/HistoricalDataService.kt`

## Phase 3: User Story 1 - Interactive Simulation (P1)

**Goal**: Users can input financial details and view a 35-year cash flow simulation table.

**Note**: Per Constitution Principle I (Test-First Development), test definitions (T018) should be developed concurrently with logic implementation (T016-T017.x). The task ordering below reflects logical grouping, not execution order.

- [x] T016 [US1] Implement "Partha's Spending Strategy" logic in `api-server/src/main/kotlin/com/retirement/logic/SpendingStrategy.kt`
- [x] T017 [US1] Implement core `SimulationEngine` to calculate year-by-year cash flow in `api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt`
- [x] T017.1 [US1] Implement healthcare expense selection logic (Pre-Medicare vs Medicare) in `SimulationEngine.kt`
- [x] T017.2 [US1] Update `SpendingStrategy` to return detailed withdrawal breakdowns (TBA, TDA, Roth) and `SimulationEngine` to track/display all income components.
- [x] T017.3 [US1] Implement Social Security calculation logic (claim ages, spousal step-up) in `SimulationEngine.kt`
- [x] T017.4 [US1] Implement Pre-retirement logic: Adjust 'Wants' so Salary covers Expenses, saving all investment income.
- [x] T017.5 [US1] Implement Quarterly Crediting logic: Accrue Interest/Dividends monthly, credit to SB on Q1 start.
- [x] T017.6 [US1] Implement failure detection: Mark simulation failed if any account balance <= 0 before age 85.
- [x] T018 [US1] Create Unit Tests for `SimulationEngine` and `SpendingStrategy` logic in `api-server/src/test/kotlin/com/retirement/logic/`
- [x] T018.1 [US1] Verify JUnit test coverage >= 100% for SpendingStrategy and SimulationEngine classes
- [x] T019 [US1] Implement POST `/api/simulate` endpoint in `api-server/src/main/kotlin/com/retirement/api/SimulationRoutes.kt`
- [x] T020 [US1] Implement proxy route for `/api/simulate` in Deno BFF `backend/src/routes.ts`
- [x] T021 [US1] Create `SimulationForm` component for user inputs in `frontend/src/components/SimulationForm.tsx`
- [x] T022 [US1] Create `ResultsTable` component to display cash flow in `frontend/src/components/ResultsTable.tsx`
- [x] T022.1 [US1] Update `ResultsTable` to display separate columns for Needs, Wants, Healthcare, Taxes, etc. in `frontend/src/components/ResultsTable.tsx`
- [x] T022.2 [US1] Add Toggle to `ResultsTable` to switch between Yearly and Quarterly views.
- [x] T023 [US1] Integrate API client to call simulation endpoint in `frontend/src/services/api.ts`
- [x] T024 [US1] Assemble `SimulationPage` connecting Form, API, and Table in `frontend/src/pages/SimulationPage.tsx`
- [ ] T024.1 [US1] Implement preset market conditions (e.g., "Historical Average", "2008 Crash", "Bull Market") in `api-server/src/main/kotlin/com/retirement/model/MarketPresets.kt`
- [ ] T024.2 [US1] Add preset selector dropdown to `SimulationForm` in `frontend/src/components/SimulationForm.tsx`

## Phase 4: User Story 2 - Save & Load Simulations (P2)

**Goal**: Users can persist their simulations and reload them later.

- [ ] T025 [US2] Implement Repository method to save simulation results to SQLite in `api-server/src/main/kotlin/com/retirement/data/SimulationRepository.kt`
- [ ] T026 [US2] Implement Repository method to list and load simulations in `api-server/src/main/kotlin/com/retirement/data/SimulationRepository.kt`
- [ ] T027 [US2] Implement POST/GET `/api/simulations` endpoints in `api-server/src/main/kotlin/com/retirement/api/SimulationRoutes.kt`
- [ ] T028 [US2] Add proxy routes for persistence in Deno BFF `backend/src/routes.ts`
- [ ] T029 [US2] Create `SavedSimulationsList` component in `frontend/src/components/SavedSimulationsList.tsx`
- [ ] T030 [US2] Add "Save" button and logic to `SimulationPage` in `frontend/src/pages/SimulationPage.tsx`
- [ ] T031 [US2] Add "Load" logic to populate form from saved data in `frontend/src/pages/SimulationPage.tsx`

## Phase 5: User Story 3 - Monte Carlo Analysis (P2)

**Goal**: Users can run 1000+ simulations with variable market conditions and view aggregate stats.

- [ ] T032 [US3] Implement `MonteCarloEngine` with parallel execution in `api-server/src/main/kotlin/com/retirement/logic/MonteCarloEngine.kt`
- [ ] T033 [US3] Implement POST `/api/simulate/monte-carlo` endpoint in `api-server/src/main/kotlin/com/retirement/api/MonteCarloRoutes.kt`
- [ ] T034 [US3] Proxy Monte Carlo endpoint in Deno BFF `backend/src/routes.ts`
- [ ] T035 [US3] Create `MonteCarloChart` component using Recharts in `frontend/src/components/MonteCarloChart.tsx`
- [ ] T036 [US3] Add Monte Carlo section to `SimulationPage` in `frontend/src/pages/SimulationPage.tsx`

## Phase 6: User Story 4 - Drill-down Details (P3)

**Goal**: Users can inspect specific Monte Carlo paths.

- [ ] T037 [US4] Add interactive tooltip and click handler to `MonteCarloChart` in `frontend/src/components/MonteCarloChart.tsx`
- [ ] T038 [US4] Implement logic to fetch/display full details for a selected run in `frontend/src/pages/SimulationPage.tsx`
- [ ] T039 [US4] (Optional) API optimization to support fetching single run details if not returned in bulk in `api-server/src/main/kotlin/com/retirement/api/MonteCarloRoutes.kt`

## Phase 7: Polish & Cross-Cutting

**Goal**: Finalize UI/UX, error handling, and documentation.

- [ ] T040 Implement global error handling in Deno BFF (transform downstream errors) in `backend/src/middleware/errorHandler.ts`
- [ ] T041 Improve UI styling (MUI Theme, Spacing, Responsive) in `frontend/src/App.css`
- [ ] T042 Add loading states and spinners during simulation runs in `frontend/src/pages/SimulationPage.tsx`
- [ ] T043 Verify all success criteria and edge cases:
  - [ ] T043.1 Test edge case: Zero initial balance in all accounts
  - [ ] T043.2 Test edge case: Market crash in Year 1 of retirement
  - [ ] T043.3 Test edge case: Deflation (negative inflation rate)
  - [ ] T043.4 Test edge case: Missing/invalid input field validation
- [ ] T044 Update `README.md` with final run instructions for all 3 tiers

## Dependencies & Execution Order

1. **Phase 1 (Setup)** must be completed first. T001-T007 are foundational.
2. **Phase 2 (Models)** blocks all subsequent phases. T010-T015 ensure data contract alignment.
3. **Phase 3 (US1)** is the MVP. Complete T016-T024 to get a working interactive simulator.
4. **Phase 4 (US2)** and **Phase 5 (US3)** can be executed in parallel after Phase 3, though US2 is simpler.
5. **Phase 6 (US4)** depends on Phase 5.

## Parallel Execution Examples

- **Setup**: One developer sets up Kotlin (T005-T007), another sets up React (T001-T002).
- **US1**: Backend logic (T016-T019) can proceed while Frontend UI (T021-T022) is built using mock data.
- **US2**: API persistence (T025-T027) can be built parallel to Frontend Save UI (T029-T030).

## Implementation Strategy

1. **MVP (Days 1-2)**: Complete Phases 1, 2, and 3. Result: Working interactive simulator with hardcoded inputs or basic form.
2. **Persistence (Day 3)**: Complete Phase 4. Result: Ability to save/load.
3. **Advanced Features (Day 4)**: Complete Phases 5 and 6. Result: Monte Carlo analysis.
