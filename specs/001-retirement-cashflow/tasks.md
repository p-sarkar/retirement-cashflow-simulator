# Tasks: Retirement Cash Flow Simulator

> **Superseded by [spec 002](../002-retirement-cashflow/tasks.md) (reconciled 2026-10-14).** This was the first plan for the
> same product. The code follows spec 002, under different paths (`api-server/`, `backend/`, `com/retirement/` instead of
> `api/`, `bff/`, `com/example/`). Each task below is marked DONE (with the file that does it), SUPERSEDED (with the
> spec 002 task that tracks the remaining work) or DROPPED (with the reason). Track new work in spec 002.

## Phase 1: Setup
- [x] T001 Initialize Git repository and create project directories (api, bff, frontend, db, shared) in . — DONE: git repo with api-server/, backend/, frontend/, historical-data/ (no db/ or shared/)
- [x] T002 Initialize Kotlin API project (Gradle) in api/ — DONE: api-server/ (Gradle, Kotlin)
- [x] T003 Initialize Deno BFF project in bff/ — DONE: backend/ (Deno)
- [x] T004 Initialize React Frontend project (Vite + TS) in frontend/ — DONE: frontend/ (Vite + React + TS)
- [~] T005 Initialize SQLite database directory and migration tool in db/ — SUPERSEDED: SQLite via Exposed; DatabaseFactory creates tables at startup instead of a migration tool. Persistence is 002 T025–T031

## Phase 2: Foundational
- [x] T006 [P] Implement Deno Oak server skeleton and basic error handling in bff/src/main.ts — DONE: backend/src/main.ts, routes.ts (503/400/500 handling)
- [x] T007 [P] Implement Kotlin API server skeleton (Ktor) in api/src/main/kotlin/com/example/Application.kt — DONE: api-server/src/main/kotlin/com/retirement/Application.kt
- [~] T008 [P] Implement Frontend basic layout (MUI, Router) in frontend/src/App.tsx — SUPERSEDED: MUI theme in frontend/src/App.tsx; no router (single page). Router dropped until there is a second page
- [x] T009 Configure BFF to proxy requests to API in bff/src/routes/proxy.ts — DONE: backend/src/routes.ts proxies /api/simulate and /api/simulate/breakdown

## Phase 3: User Story 1 - Deterministic Cash-Flow Simulation
- [x] T010 [US1] Define Simulation Input/Output data models in api/src/main/kotlin/com/example/model/SimulationModels.kt — DONE: api-server/.../model/SimulationConfig.kt, SimulationResult.kt, OneTimeExpense.kt
- [~] T011 [US1] Implement Market Condition logic (presets, custom, FR-009 extension) in api/src/main/kotlin/com/example/service/MarketService.kt — SUPERSEDED: custom static rates work; presets are 002 T024.1–T024.2
- [~] T012 [US1] Implement Tax Estimator logic (Federal, State placeholder) in api/src/main/kotlin/com/example/logic/TaxEstimator.kt — SUPERSEDED: replaced by a single effective income tax rate (002 clarification 2026-01-06); federal/state estimator dropped
- [x] T013 [US1] Implement Deterministic Simulation Engine (Year-by-year, Funding Order) in api/src/main/kotlin/com/example/logic/SimulationEngine.kt — DONE: api-server/.../logic/SimulationEngine.kt (quarterly, spending strategy)
- [x] T014 [US1] Expose POST /internal/simulations/deterministic endpoint in api/src/main/kotlin/com/example/routes/SimulationRoutes.kt — DONE: as POST /api/simulate in api-server/.../api/SimulationRoutes.kt
- [x] T015 [US1] Implement POST /api/simulations/deterministic endpoint in bff/src/routes/simulation.ts — DONE: as POST /api/simulate in backend/src/routes.ts
- [x] T016 [US1] Create Frontend Input Form (Ages, Balances, Income, Expenses) in frontend/src/components/SimulationForm.tsx — DONE: frontend/src/components/SimulationForm.tsx
- [x] T017 [US1] Create Frontend Results Table (35 rows, columns per FR) in frontend/src/components/ResultsTable.tsx — DONE: frontend/src/components/ResultsTable.tsx (rows through End Age, not a fixed 35)
- [x] T018 [US1] Create Frontend Summary View (Ending balances, Success/Failure) in frontend/src/components/SummaryView.tsx — DONE: success/failure summary in ResultsTable.tsx rather than a separate SummaryView
- [x] T019 [US1] Integrate Frontend with BFF for Deterministic Run in frontend/src/api/simulationApi.ts — DONE: frontend/src/services/api.ts

## Phase 4: User Story 2 - Save and Reload Simulations
- [~] T020 [US2] Create SQL migration for simulations table in db/migrations/001_create_simulations.sql — SUPERSEDED: Simulations table defined in api-server/.../data/SimulationsTable.kt, created by DatabaseFactory; no SQL migration
- [~] T021 [US2] Implement Persistence Repository in Kotlin in api/src/main/kotlin/com/example/repository/SimulationRepository.kt — SUPERSEDED: 002 T025–T026
- [~] T022 [US2] Expose endpoints for Save/Load/List in api/src/main/kotlin/com/example/routes/PersistenceRoutes.kt — SUPERSEDED: 002 T027
- [~] T023 [US2] Implement BFF endpoints for Save/Load/List in bff/src/routes/persistence.ts — SUPERSEDED: 002 T028
- [~] T024 [US2] Create Save Simulation Dialog component in frontend/src/components/SaveDialog.tsx — SUPERSEDED: 002 T030
- [~] T025 [US2] Create Saved Simulations List page in frontend/src/pages/SavedSimulations.tsx — SUPERSEDED: 002 T029
- [~] T026 [US2] Implement Reload logic to populate form with saved data in frontend/src/pages/SimulationPage.tsx — SUPERSEDED: 002 T031

## Phase 5: User Story 3 - Monte Carlo Simulations
- [x] T027 [US3] Implement CSV Parser for historical data in api/src/main/kotlin/com/example/logic/CsvParser.kt — DONE: api-server/.../data/HistoricalDataService.kt (loaded at startup; not yet used by the engine)
- [~] T028 [US3] Implement Monte Carlo Bootstrapping and Execution logic (1000+ paths) in api/src/main/kotlin/com/example/logic/MonteCarloEngine.kt — SUPERSEDED: 002 T032
- [~] T029 [US3] Implement Percentile Calculation logic in api/src/main/kotlin/com/example/logic/Statistics.kt — SUPERSEDED: 002 T032
- [~] T030 [US3] Create SQL migration for monte_carlo_paths table in db/migrations/002_create_monte_carlo.sql — DROPPED: 002 Monte Carlo returns results in the response rather than storing paths (002 T032–T033, T039)
- [~] T031 [US3] Expose MC Run and Load Details endpoints in api/src/main/kotlin/com/example/routes/MonteCarloRoutes.kt — SUPERSEDED: 002 T033, T039
- [~] T032 [US3] Implement BFF endpoints for MC Run and Details in bff/src/routes/monteCarlo.ts — SUPERSEDED: 002 T034
- [~] T033 [US3] Create Monte Carlo Chart component (ECharts) in frontend/src/components/MonteCarloChart.tsx — SUPERSEDED: 002 T035 (Recharts instead of ECharts)
- [~] T034 [US3] Implement MC Results Page with Chart and Details View in frontend/src/pages/MonteCarloPage.tsx — SUPERSEDED: 002 T036, T038
- [~] T035 [US3] Implement Custom Dataset Upload via BFF in bff/src/routes/datasets.ts — DROPPED: custom dataset upload (001 FR-034) was not carried into spec 002

## Phase 6: Polish & Cross-Cutting
- [~] T036 Ensure all invalid inputs are gracefully handled and displayed in UI in frontend/src/components/ErrorBoundary.tsx — SUPERSEDED: inline field errors and per-field 422 errors are shown; global BFF error handling is 002 T040. React ErrorBoundary dropped
- [x] T037 Verify and Polish Styling (MUI Theme) in frontend/src/theme.ts — DONE: default MUI theme plus the 002 layout rules (002 T041–T041.5)
- [~] T038 [US1] Implement CSV export of deterministic results (FR-041) in frontend/src/utils/csvExport.ts — SUPERSEDED: 002 FR-010a, T052

## Dependencies
- US1 (Deterministic) is the core engine.
- US2 (Persistence) requires the data model from US1.
- US3 (Monte Carlo) builds on the Deterministic Engine from US1 but adds Bootstrapping.

## Parallel Execution Examples
- **US1**: Backend developers can implement `SimulationEngine.kt` (T013) while Frontend developers build `SimulationForm.tsx` (T016).
- **US2**: `PersistenceRoutes.kt` (T022) can be implemented in parallel with `SaveDialog.tsx` (T024).

## Implementation Strategy
- **MVP (US1)**: Focus on getting the deterministic calculation correct and visible. This is the P1 value.
- **Incremental (US2)**: Add persistence once the data model is stable from US1.
- **Advanced (US3)**: Add Monte Carlo as a separate mode that reuses the engine.
