# Tasks: Retirement Cash Flow Simulator

> **Superseded by [spec 002](../002-retirement-cashflow/tasks.md) (reconciled 2026-10-14).** This was the first plan for the
> same product. The code follows spec 002, under different paths (`api-server/`, `backend/`, `com/retirement/` instead of
> `api/`, `bff/`, `com/example/`). Each task below is marked DONE (with the file that does it), SUPERSEDED (with the
> spec 002 task that tracks the remaining work) or DROPPED (with the reason). Track new work in spec 002.

## Phase 1: Setup
- [x] T001-001 Initialize Git repository and create project directories (api, bff, frontend, db, shared) in . — DONE: git repo with api-server/, backend/, frontend/, historical-data/ (no db/ or shared/)
- [x] T001-002 Initialize Kotlin API project (Gradle) in api/ — DONE: api-server/ (Gradle, Kotlin)
- [x] T001-003 Initialize Deno BFF project in bff/ — DONE: backend/ (Deno)
- [x] T001-004 Initialize React Frontend project (Vite + TS) in frontend/ — DONE: frontend/ (Vite + React + TS)
- [~] T001-005 Initialize SQLite database directory and migration tool in db/ — SUPERSEDED: SQLite via Exposed; DatabaseFactory creates tables at startup instead of a migration tool. Persistence is T002-025–T002-031

## Phase 2: Foundational
- [x] T001-006 [P] Implement Deno Oak server skeleton and basic error handling in bff/src/main.ts — DONE: backend/src/main.ts, routes.ts (503/400/500 handling)
- [x] T001-007 [P] Implement Kotlin API server skeleton (Ktor) in api/src/main/kotlin/com/example/Application.kt — DONE: api-server/src/main/kotlin/com/retirement/Application.kt
- [~] T001-008 [P] Implement Frontend basic layout (MUI, Router) in frontend/src/App.tsx — SUPERSEDED: MUI theme in frontend/src/App.tsx; no router (single page). Router dropped until there is a second page
- [x] T001-009 Configure BFF to proxy requests to API in bff/src/routes/proxy.ts — DONE: backend/src/routes.ts proxies /api/simulate and /api/simulate/breakdown

## Phase 3: User Story 1 - Deterministic Cash-Flow Simulation
- [x] T001-010 [US001-1] Define Simulation Input/Output data models in api/src/main/kotlin/com/example/model/SimulationModels.kt — DONE: api-server/.../model/SimulationConfig.kt, SimulationResult.kt, OneTimeExpense.kt
- [~] T001-011 [US001-1] Implement Market Condition logic (presets, custom, FR001-009 extension) in api/src/main/kotlin/com/example/service/MarketService.kt — SUPERSEDED: custom static rates work; presets are T002-024.1–T002-024.2
- [~] T001-012 [US001-1] Implement Tax Estimator logic (Federal, State placeholder) in api/src/main/kotlin/com/example/logic/TaxEstimator.kt — SUPERSEDED: replaced by a single effective income tax rate (002 clarification 2026-01-06); federal/state estimator dropped
- [x] T001-013 [US001-1] Implement Deterministic Simulation Engine (Year-by-year, Funding Order) in api/src/main/kotlin/com/example/logic/SimulationEngine.kt — DONE: api-server/.../logic/SimulationEngine.kt (quarterly, spending strategy)
- [x] T001-014 [US001-1] Expose POST /internal/simulations/deterministic endpoint in api/src/main/kotlin/com/example/routes/SimulationRoutes.kt — DONE: as POST /api/simulate in api-server/.../api/SimulationRoutes.kt
- [x] T001-015 [US001-1] Implement POST /api/simulations/deterministic endpoint in bff/src/routes/simulation.ts — DONE: as POST /api/simulate in backend/src/routes.ts
- [x] T001-016 [US001-1] Create Frontend Input Form (Ages, Balances, Income, Expenses) in frontend/src/components/SimulationForm.tsx — DONE: frontend/src/components/SimulationForm.tsx
- [x] T001-017 [US001-1] Create Frontend Results Table (35 rows, columns per FR) in frontend/src/components/ResultsTable.tsx — DONE: frontend/src/components/ResultsTable.tsx (rows through End Age, not a fixed 35)
- [x] T001-018 [US001-1] Create Frontend Summary View (Ending balances, Success/Failure) in frontend/src/components/SummaryView.tsx — DONE: success/failure summary in ResultsTable.tsx rather than a separate SummaryView
- [x] T001-019 [US001-1] Integrate Frontend with BFF for Deterministic Run in frontend/src/api/simulationApi.ts — DONE: frontend/src/services/api.ts

## Phase 4: User Story 2 - Save and Reload Simulations
- [~] T001-020 [US001-2] Create SQL migration for simulations table in db/migrations/001_create_simulations.sql — SUPERSEDED: Simulations table defined in api-server/.../data/SimulationsTable.kt, created by DatabaseFactory; no SQL migration
- [~] T001-021 [US001-2] Implement Persistence Repository in Kotlin in api/src/main/kotlin/com/example/repository/SimulationRepository.kt — SUPERSEDED: T002-025–T002-026
- [~] T001-022 [US001-2] Expose endpoints for Save/Load/List in api/src/main/kotlin/com/example/routes/PersistenceRoutes.kt — SUPERSEDED: T002-027
- [~] T001-023 [US001-2] Implement BFF endpoints for Save/Load/List in bff/src/routes/persistence.ts — SUPERSEDED: T002-028
- [~] T001-024 [US001-2] Create Save Simulation Dialog component in frontend/src/components/SaveDialog.tsx — SUPERSEDED: T002-030
- [~] T001-025 [US001-2] Create Saved Simulations List page in frontend/src/pages/SavedSimulations.tsx — SUPERSEDED: T002-029
- [~] T001-026 [US001-2] Implement Reload logic to populate form with saved data in frontend/src/pages/SimulationPage.tsx — SUPERSEDED: T002-031

## Phase 5: User Story 3 - Monte Carlo Simulations
- [x] T001-027 [US001-3] Implement CSV Parser for historical data in api/src/main/kotlin/com/example/logic/CsvParser.kt — DONE: api-server/.../data/HistoricalDataService.kt (loaded at startup; not yet used by the engine)
- [~] T001-028 [US001-3] Implement Monte Carlo Bootstrapping and Execution logic (1000+ paths) in api/src/main/kotlin/com/example/logic/MonteCarloEngine.kt — SUPERSEDED: T002-032
- [~] T001-029 [US001-3] Implement Percentile Calculation logic in api/src/main/kotlin/com/example/logic/Statistics.kt — SUPERSEDED: T002-032
- [~] T001-030 [US001-3] Create SQL migration for monte_carlo_paths table in db/migrations/002_create_monte_carlo.sql — DROPPED: 002 Monte Carlo returns results in the response rather than storing paths (T002-032–T002-033, T002-039)
- [~] T001-031 [US001-3] Expose MC Run and Load Details endpoints in api/src/main/kotlin/com/example/routes/MonteCarloRoutes.kt — SUPERSEDED: T002-033, T002-039
- [~] T001-032 [US001-3] Implement BFF endpoints for MC Run and Details in bff/src/routes/monteCarlo.ts — SUPERSEDED: T002-034
- [~] T001-033 [US001-3] Create Monte Carlo Chart component (ECharts) in frontend/src/components/MonteCarloChart.tsx — SUPERSEDED: T002-035 (Recharts instead of ECharts)
- [~] T001-034 [US001-3] Implement MC Results Page with Chart and Details View in frontend/src/pages/MonteCarloPage.tsx — SUPERSEDED: T002-036, T002-038
- [~] T001-035 [US001-3] Implement Custom Dataset Upload via BFF in bff/src/routes/datasets.ts — DROPPED: custom dataset upload (FR001-034) was not carried into spec 002

## Phase 6: Polish & Cross-Cutting
- [~] T001-036 Ensure all invalid inputs are gracefully handled and displayed in UI in frontend/src/components/ErrorBoundary.tsx — SUPERSEDED: inline field errors and per-field 422 errors are shown; global BFF error handling is T002-040. React ErrorBoundary dropped
- [x] T001-037 Verify and Polish Styling (MUI Theme) in frontend/src/theme.ts — DONE: default MUI theme plus the 002 layout rules (T002-041–T002-041.5)
- [~] T001-038 [US001-1] Implement CSV export of deterministic results (FR001-041) in frontend/src/utils/csvExport.ts — SUPERSEDED: FR002-010a, T002-052

## Dependencies
- US001-1 (Deterministic) is the core engine.
- US001-2 (Persistence) requires the data model from US001-1.
- US001-3 (Monte Carlo) builds on the Deterministic Engine from US001-1 but adds Bootstrapping.

## Parallel Execution Examples
- **US001-1**: Backend developers can implement `SimulationEngine.kt` (T001-013) while Frontend developers build `SimulationForm.tsx` (T001-016).
- **US001-2**: `PersistenceRoutes.kt` (T001-022) can be implemented in parallel with `SaveDialog.tsx` (T001-024).

## Implementation Strategy
- **MVP (US001-1)**: Focus on getting the deterministic calculation correct and visible. This is the P1 value.
- **Incremental (US001-2)**: Add persistence once the data model is stable from US001-1.
- **Advanced (US001-3)**: Add Monte Carlo as a separate mode that reuses the engine.
