# Problem statement
Build a retirement cash-flow simulator that (1) runs a deterministic 35-year projection (2025–2059) under preset or custom market conditions (inflation + S&P returns), (2) persists simulations for later reload, and (3) runs a Monte Carlo analysis (>= 1000 paths) with an interactive plot and the ability to load a selected path’s full cash-flow table.

# Current state
The repository currently contains:
* Feature spec in `specs/001-retirement-cashflow/spec.md`.
* Historical datasets in `historical-data/*.csv`.
* No application code yet (no Vite/Deno/Gradle project structure).

# Proposed architecture (n-tier)
## Responsibilities
Frontend (React + Vite + TS + React Router + MUI)
* Collect inputs (ages, balances, income/expense assumptions, goals, tax override rules, market condition selection).
* Render deterministic results table (35 rows) and summary (ending balances, success/failure + first failure year).
* Render saved simulations list + reload flows.
* Render Monte Carlo chart: show all paths + highlighted percentile curves; hover highlights path; click selects path; “Load Details” fetches selected path’s full table.

BFF gateway (Deno + Oak)
* Single public API surface for the frontend.
* Owns request validation, CORS, and request shaping for UI needs.
* Proxies all persistence and compute operations to the Kotlin API service (no direct DB access).
* External route prefix: `/api/*` (frontend talks only to BFF).

Kotlin API service (Kotlin + Gradle)
* Implements deterministic simulation engine and Monte Carlo path generation.
* Owns persistence and all data access to SQLite (single writer/owner).
* Exposes APIs for presets, running simulations, saving/loading, and Monte Carlo “Load Details”.

Data store (SQLite)
* Owned by the Kotlin service.
* Stores presets (optional; can also be compiled in), saved deterministic simulations, and Monte Carlo run metadata.
* Stores enough Monte Carlo data to support interactive selection + “Load Details” without persisting all 1000 full tables.

## Why both Deno + Kotlin
Given the requested stack includes both, this plan uses:
* Deno/Oak as the UI-facing gateway (thin BFF) to keep frontend integration simple (single origin/port, consistent payloads).
* Kotlin as the core service that owns SQLite and the simulation/Monte Carlo logic.
This keeps SQLite ownership in one place while still allowing a dedicated frontend-friendly API layer.

# Repository layout
Create a multi-project repo:
* `frontend/` (Vite React TS)
* `bff/` (Deno/Oak)
* `api/` (Kotlin/Gradle)
* `db/` (SQLite schema + migrations)
* `shared/` (optional: JSON schema / OpenAPI spec and/or shared constants; keep minimal to avoid cross-tooling friction)

# Core domain model
## Inputs
* Projection horizon: fixed to 2025–2059 inclusive (35 rows), per FR-001.
* Ages: current age, retirement age, Social Security claiming age.
* Starting balances: spend bucket, crash buffer, taxable brokerage, tax-deferred, tax-free.
* Income lines: salary schedule (pre-retirement), Social Security base benefit.
* Expense lines: essentials, nice-to-have, property tax (starting 2025 value), one-time goals list (year + amount).
* Taxes: computed estimate from taxable income, plus optional per-year override.
* Market condition: preset id or custom (constant rates or annual series with repeat-last extension).

## Outputs
Deterministic result:
* `rows[35]`: for each year 2025..2059 includes balance columns, income columns, expense columns, goals, totals, income gap, plus key intermediate funding actions needed to satisfy FR-016/FR-017.
* `summary`: ending balances by account + total, success/failure, first failure year if any.

Monte Carlo result:
* `paths[N]` where each path includes:
  * total-balance trajectory for plotting (35 points)
  * summary metrics (success/failure, failure year if any, ending total)
  * a compact representation of the generated market sequence (see persistence section)
* `percentiles`: median/75th/90th trajectories (35 points each)

# Deterministic simulation engine (Kotlin API service)
Implement a year-by-year engine:
1. Expand market condition to arrays `inflation[35]` and `spReturn[35]` (apply FR-009 repeat-last).
2. For each year:
  * Inflate income/expense lines per FR-007 unless explicitly overridden by a per-year schedule.
  * Apply investment returns:
    * Equities accounts affected by S&P returns.
    * Spend bucket interest rate and crash buffer dividend rate are user-selected inputs (annual rates). Keep these explicit to satisfy table columns in FR-012.
  * Compute Total Income (non-withdrawal + withdrawals) and Total Expenses.
  * Compute Income Gap (FR-015; document the exact definition in the UI).
  * Close the gap using the required funding order (FR-017):
    1) spend bucket withdrawals
    2) taxable stock sales
    3) tax-deferred distributions
    4) tax-free withdrawals
  * Track balances; detect failure when any tracked balance becomes negative (FR-026) and stop after including failure year row (FR-027).
3. Produce summary at end (FR-025) or at failure.

Notes/assumptions to resolve early:
* Tax model (more realistic, MVP defaults): implement a US tax estimator with:
  * Filing status: assume Married Filing Jointly (MFJ) for MVP (configurable later)
  * Federal:
    * Standard deduction
    * Ordinary income brackets
    * Long-term capital gains (LTCG) + qualified dividends brackets
    * Social Security taxation via provisional income (taxable portion of SS)
    * Net Investment Income Tax (NIIT): include/compute (on by default)
    * Separate tracking of ordinary income vs LTCG/qualified-dividend income produced by the simulation (e.g., tax-deferred distributions + Roth conversions as ordinary; taxable stock sales as capital gains; dividends potentially qualified/non-qualified via a simplifying assumption or user setting)
    * Bracket/deduction inflation-adjustment each year using the simulation’s inflation series (reasonable approximation for multi-decade horizons)
  * State:
    * User-selectable state (for MVP: implement a small set of common states first, including PA, expand later)
    * Model state income tax based on the selected state’s brackets/deductions where feasible; if a state’s rules are not implemented yet, fall back to a transparent approximation (e.g., flat rate) and flag in UI.
  * Per-year override remains available per FR-022 and always wins.
* Roth conversions: represent as a user-configurable per-year amount; count as ordinary taxable income; do not use for funding unless user chooses (FR-018).
* 2 separate withdrawals from tax-deferred account, each year
  * Spending
  * Roth conversions

# Monte Carlo (Kotlin API service)
## Dataset handling
* Parse yearly (inflation, S&P return) observations from CSV.
* Default dataset: use repo `historical-data/*.csv` to build an observations table (align by year).
* CSV formats (MVP):
  * S&P returns: 2 columns: `year end`, `return`.
    * `year end` must be a strict year-end date for that year (12/31/YYYY).
    * `return` is a percent value (e.g., `23.31` meaning 23.31%).
  * Inflation: 2 columns: `year end`, `inflation rate`.
    * `year end` must be a strict year-end date for that year (12/31/YYYY).
    * `inflation rate` is a percent value (e.g., `2.89` meaning 2.89%).
  * In both files, the 2nd column is a percentage (not a decimal rate).
  * Implementation should be tolerant of headers like `Date,Value` (as in the checked-in datasets) by mapping them to the appropriate semantic fields.
* Support user-uploaded CSV (FR-034) via BFF; BFF forwards the dataset to the Kotlin service, which stores and uses it for bootstrapping.

## Bootstrapping approach (FR-033)
* Generate N paths (N >= 1000; enforce FR-032).
* For each path, sample a length-35 sequence of yearly (inflation, return) pairs from the observations via resampling with replacement.
* Run the deterministic engine using that sampled sequence.
* Collect:
  * total balance per year for plotting
  * summary metrics
  * a compact “sequence reference” to allow recomputation of full details for a selected path

## Percentiles
* For each year index 0..34, compute percentiles across the N total-balance values.

# Persistence model (SQLite in Kotlin service)
## Schema (initial)
SQLite is owned and migrated by the Kotlin service.
* `simulations`:
  * `id` (uuid/text primary key)
  * `name` (text nullable)
  * `created_at` (datetime)
  * `type` (text: deterministic | monte_carlo)
  * `inputs_json` (text)
  * `market_condition_json` (text)
  * `result_summary_json` (text)
  * `result_table_json` (text nullable; deterministic stores full table)
* `monte_carlo_paths` (only for MC runs):
  * `run_id` (fk to simulations.id)
  * `path_index` (int)
  * `trajectory_json` (text; 35 points)
  * `summary_json` (text)
  * `sequence_ref_json` (text; see below)
  * primary key (`run_id`, `path_index`)

## Storing MC “Load Details” without storing all full tables
To keep storage reasonable, persist per-path market sequences in a compact form:
* `sequence_ref_json` stores the list of sampled observation indices (length 35), or a seed + RNG algorithm id if deterministic reproduction is guaranteed.
When the user clicks “Load Details” for a path:
* gateway calls the Kotlin service for the selected run/path; Kotlin reconstructs the sampled sequence and runs deterministic-from-sequence to return the full table.
This meets FR-040 without storing 1000 full cash-flow tables.

# API design
## Public BFF API (frontend → BFF)
All public routes are namespaced under `/api/*` and owned by the BFF.
* `GET /api/market-conditions/presets`
* `POST /api/simulations/deterministic` (run deterministic; returns result; optional `save: true`)
* `POST /api/simulations/monte-carlo` (run MC; returns trajectories + percentiles + summaries; optional `save: true`)
* `GET /api/simulations` (list saved)
* `GET /api/simulations/:id` (load saved deterministic or MC metadata)
* `GET /api/simulations/:id/monte-carlo/path/:pathIndex/details` (recompute and return full table for that path)
* `POST /api/datasets/historical` (upload/replace dataset used for bootstrapping)

## Internal Kotlin API (BFF → Kotlin API service)
Kotlin exposes an internal-only surface under `/internal/*` (not directly reachable by the browser).
* `GET /internal/market-conditions/presets`
* `POST /internal/simulations/deterministic`
* `POST /internal/simulations/monte-carlo`
* `GET /internal/simulations`
* `GET /internal/simulations/:id`
* `GET /internal/simulations/:id/monte-carlo/path/:pathIndex/details`
* `POST /internal/datasets/historical`

# Frontend UX plan
Routes:
* `/` New simulation form (deterministic + MC tabs)
* `/results/:id?` deterministic result table (also usable for non-saved runs via state)
* `/saved` list + load actions
* `/monte-carlo/:id?` chart + selection + load-details table

Component highlights:
* Input form uses schema-driven validation matching gateway validation rules.
* Table uses a virtualized grid component (MUI DataGrid or TanStack Table + virtualization) to keep rendering snappy.
* Monte Carlo chart uses a canvas-backed library suited for ~1000 lines (e.g., ECharts). Implement hover highlight and click selection.

# Single-deployable UI (BFF serves frontend)
Production/deploy model:
* The React app is built to static assets (e.g., `frontend/dist`).
* The BFF serves those static files and also serves the `/api/*` routes.
* SPA routing uses a “fallback to index.html” so `/saved`, `/monte-carlo/...`, etc. work on refresh.
* Result: the UI + BFF are shipped as a single deployable unit (single origin). The Kotlin `api/` service remains a separate backend service because it owns SQLite.

Development model:
* Run Vite dev server for fast HMR.
* Configure Vite to proxy `/api/*` to the BFF during development.

# Dev workflow
* Provide root-level scripts (Makefile or justfile) to run all tiers locally:
  * `frontend`: `npm run dev`
  * `bff`: `deno task dev` (also capable of serving `frontend/dist` in production mode)
  * `api`: `./gradlew run`
* CORS policy:
  * Dev: BFF allows the Vite dev origin.
  * Prod: no browser CORS needed (same origin), Kotlin API is not exposed cross-origin.

# Testing strategy
* Kotlin: unit tests for deterministic engine invariants (35 rows, funding order, failure year detection, inflation extension rule, etc.) + integration tests for SQLite persistence.
* Deno: API contract tests for the BFF layer (validation, proxying, error shaping).
* Frontend: component tests for validation + basic rendering; smoke test for MC selection → load details.

# Open questions / decisions needed
2. UI library choice: MUI (selected).
3. Tax estimator details (decided for MVP): assume MFJ, include NIIT, and provide a state selector that affects tax calculation.
4. Interest/dividend rates: decided — spend-bucket interest rate and crash-buffer dividend rate are user-selected inputs.
5. CSV format for user-uploaded datasets (decided):
  * S&P returns: 2 columns `year end`, `return` (percent)
  * Inflation: 2 columns `year end`, `inflation rate` (percent)
  * `year end` is a strict year-end date (12/31/YYYY) in the first column for both files (reject otherwise)
  * tolerate `Date,Value` headers; parse year from year-end date.
