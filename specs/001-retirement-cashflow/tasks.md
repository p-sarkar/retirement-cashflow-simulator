# Tasks: Retirement Cash Flow Simulator

## Phase 1: Setup
- [ ] T001 Initialize Git repository and create project directories (api, bff, frontend, db, shared) in .
- [ ] T002 Initialize Kotlin API project (Gradle) in api/
- [ ] T003 Initialize Deno BFF project in bff/
- [ ] T004 Initialize React Frontend project (Vite + TS) in frontend/
- [ ] T005 Initialize SQLite database directory and migration tool in db/

## Phase 2: Foundational
- [ ] T006 [P] Implement Deno Oak server skeleton and basic error handling in bff/src/main.ts
- [ ] T007 [P] Implement Kotlin API server skeleton (Ktor) in api/src/main/kotlin/com/example/Application.kt
- [ ] T008 [P] Implement Frontend basic layout (MUI, Router) in frontend/src/App.tsx
- [ ] T009 Configure BFF to proxy requests to API in bff/src/routes/proxy.ts

## Phase 3: User Story 1 - Deterministic Cash-Flow Simulation
- [ ] T010 [US1] Define Simulation Input/Output data models in api/src/main/kotlin/com/example/model/SimulationModels.kt
- [ ] T011 [US1] Implement Market Condition logic (presets, custom, FR-009 extension) in api/src/main/kotlin/com/example/service/MarketService.kt
- [ ] T012 [US1] Implement Tax Estimator logic (Federal, State placeholder) in api/src/main/kotlin/com/example/logic/TaxEstimator.kt
- [ ] T013 [US1] Implement Deterministic Simulation Engine (Year-by-year, Funding Order) in api/src/main/kotlin/com/example/logic/SimulationEngine.kt
- [ ] T014 [US1] Expose POST /internal/simulations/deterministic endpoint in api/src/main/kotlin/com/example/routes/SimulationRoutes.kt
- [ ] T015 [US1] Implement POST /api/simulations/deterministic endpoint in bff/src/routes/simulation.ts
- [ ] T016 [US1] Create Frontend Input Form (Ages, Balances, Income, Expenses) in frontend/src/components/SimulationForm.tsx
- [ ] T017 [US1] Create Frontend Results Table (35 rows, columns per FR) in frontend/src/components/ResultsTable.tsx
- [ ] T018 [US1] Create Frontend Summary View (Ending balances, Success/Failure) in frontend/src/components/SummaryView.tsx
- [ ] T019 [US1] Integrate Frontend with BFF for Deterministic Run in frontend/src/api/simulationApi.ts

## Phase 4: User Story 2 - Save and Reload Simulations
- [ ] T020 [US2] Create SQL migration for simulations table in db/migrations/001_create_simulations.sql
- [ ] T021 [US2] Implement Persistence Repository in Kotlin in api/src/main/kotlin/com/example/repository/SimulationRepository.kt
- [ ] T022 [US2] Expose endpoints for Save/Load/List in api/src/main/kotlin/com/example/routes/PersistenceRoutes.kt
- [ ] T023 [US2] Implement BFF endpoints for Save/Load/List in bff/src/routes/persistence.ts
- [ ] T024 [US2] Create Save Simulation Dialog component in frontend/src/components/SaveDialog.tsx
- [ ] T025 [US2] Create Saved Simulations List page in frontend/src/pages/SavedSimulations.tsx
- [ ] T026 [US2] Implement Reload logic to populate form with saved data in frontend/src/pages/SimulationPage.tsx

## Phase 5: User Story 3 - Monte Carlo Simulations
- [ ] T027 [US3] Implement CSV Parser for historical data in api/src/main/kotlin/com/example/logic/CsvParser.kt
- [ ] T028 [US3] Implement Monte Carlo Bootstrapping and Execution logic (1000+ paths) in api/src/main/kotlin/com/example/logic/MonteCarloEngine.kt
- [ ] T029 [US3] Implement Percentile Calculation logic in api/src/main/kotlin/com/example/logic/Statistics.kt
- [ ] T030 [US3] Create SQL migration for monte_carlo_paths table in db/migrations/002_create_monte_carlo.sql
- [ ] T031 [US3] Expose MC Run and Load Details endpoints in api/src/main/kotlin/com/example/routes/MonteCarloRoutes.kt
- [ ] T032 [US3] Implement BFF endpoints for MC Run and Details in bff/src/routes/monteCarlo.ts
- [ ] T033 [US3] Create Monte Carlo Chart component (ECharts) in frontend/src/components/MonteCarloChart.tsx
- [ ] T034 [US3] Implement MC Results Page with Chart and Details View in frontend/src/pages/MonteCarloPage.tsx
- [ ] T035 [US3] Implement Custom Dataset Upload via BFF in bff/src/routes/datasets.ts

## Phase 6: Polish & Cross-Cutting
- [ ] T036 Ensure all invalid inputs are gracefully handled and displayed in UI in frontend/src/components/ErrorBoundary.tsx
- [ ] T037 Verify and Polish Styling (MUI Theme) in frontend/src/theme.ts
- [ ] T038 [US1] Implement CSV export of deterministic results (FR-041) in frontend/src/utils/csvExport.ts

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
