# Tasks: Saved Simulations Improvements

**Input**: Design documents from `/specs/005-saved-simulations/`
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/saved-simulations-api.md](./contracts/saved-simulations-api.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The constitution makes test-first non-negotiable (principle I): each story's tests are written
first and must fail before its implementation.

**Organization**: Tasks are grouped by user story so each story can be implemented and tested on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: The user story the task belongs to (`US005-1`, `US005-2`, `US005-3`)

## Path Conventions

API server `api-server/src/main/kotlin/com/retirement/` (tests under `api-server/src/test/kotlin/com/retirement/`),
BFF `backend/src/` (tests `backend/tests/`), frontend `frontend/src/`.

---

## Phase 1: Setup

**Purpose**: A known-good starting point.

- [x] T005-001 Run the full suites on `feat/005-saved-simulations` and note the counts (`cd api-server && ./gradlew test`, `cd backend && deno task test && deno lint`, `cd frontend && npm run lint && npm test && npm run build`); revert `api-server/src/main/resources/version.properties`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The `updated_at` column (R005-6), needed by US005-2 and present in every summary.

**⚠️ CRITICAL**: The schema change must never run against `api-server/retirement.db` before a backup and the user's confirmation (T005-037).

- [x] T005-002 JUnit first: `DatabaseFactory.init` on a database created with the old `Simulations` schema (no `updated_at`) and one saved row adds a nullable `updated_at` column, keeps the row (name, created_at, configuration, result unchanged, `updated_at` null), and a second `init` changes nothing, in api-server/src/test/kotlin/com/retirement/data/SimulationRepositoryTest.kt
- [x] T005-003 Add `val updatedAt = datetime("updated_at").nullable()` to `Simulations`; in `DatabaseFactory.init`, after `SchemaUtils.create(Simulations)`, read SQLite's table info and run `ALTER TABLE Simulations ADD COLUMN updated_at` only if the column is missing (no other schema statements, R005-6), in api-server/src/main/kotlin/com/retirement/data/SimulationsTable.kt, api-server/src/main/kotlin/com/retirement/data/DatabaseFactory.kt

**Checkpoint**: Old databases open with the new column and keep their rows.

---

## Phase 3: User Story 1 - Know when saved results are out of date, and refresh them (Priority: P1) 🎯 MVP

**Goal**: The list and the load flag results computed by another or unknown version; one action re-runs them, and the saved and re-run results can be compared until the user saves (FR005-001 to FR005-006).

**Independent Test**: Save a result stamped 1.1.28 through the BFF; the list marks it, loading shows the notice with both versions, Re-run gives the same results as a fresh run, and the stored simulation is unchanged (quickstart Scenario 1).

### Tests for User Story 1 ⚠️ (write first, must fail)

- [x] T005-004 [P] [US005-1] JUnit: a simulation saved with the running version has `version` = running version and `isCurrentVersion` true; one whose result has `apiMetadata.version` "1.1.28" has `version` "1.1.28" and `isCurrentVersion` false; a row whose result JSON has no readable `apiMetadata.version` (inserted directly) lists with `version` null and `isCurrentVersion` false instead of failing the list (FR005-001, R005-1), in api-server/src/test/kotlin/com/retirement/data/SimulationRepositoryTest.kt
- [x] T005-005 [P] [US005-1] JUnit: `GET /api/simulations` and `GET /api/simulations/{id}` return `version` and `isCurrentVersion` in each summary, in api-server/src/test/kotlin/com/retirement/api/PersistenceAndMonteCarloRoutesTest.kt
- [x] T005-006 [P] [US005-1] Vitest: the list marks entries with `isCurrentVersion` false ("Computed by v1.1.28", or "Computed by an unknown version" when `version` is null) and shows no mark otherwise (FR005-001, FR005-006), in frontend/src/components/SavedSimulationsList.test.tsx
- [x] T005-007 [P] [US005-1] Vitest: the re-run notice names the saved and running versions and offers Re-run; after a re-run it switches between Re-run and Saved and shows each one's outcome, failure year and final total balance and the balance difference (FR005-002 to FR005-004), in frontend/src/components/RerunNotice.test.tsx
- [x] T005-008 [US005-1] Vitest: loading a flagged simulation shows its saved results and the notice; Re-run calls `runSimulation` with the saved config and shows its results; switching shows the saved rows again; loading another simulation, running other inputs or importing drops the comparison; a current simulation shows no notice (FR005-002 to FR005-006, R005-2), in frontend/src/pages/SimulationPage.test.tsx

### Implementation for User Story 1

- [x] T005-009 [US005-1] Add `version: String?` and `isCurrentVersion: Boolean` to `SavedSimulationSummary`; in the repository read `result.apiMetadata.version` from the stored JSON as a `JsonObject` (null if missing or unreadable) and compare it with `ServerInfo.version` (R005-1); regenerate contract types (`cd api-server && ./gradlew generateTsContract`), in api-server/src/main/kotlin/com/retirement/model/SimulationResult.kt, api-server/src/main/kotlin/com/retirement/data/SimulationRepository.kt, frontend/src/types/contract.generated.ts, backend/src/contract.generated.ts
- [x] T005-010 [P] [US005-1] Show the older-version mark on list entries, in frontend/src/components/SavedSimulationsList.tsx
- [x] T005-011 [P] [US005-1] Create the re-run notice: both versions, Re-run button (busy while running), Saved / Re-run switch, side-by-side outcome, failure year and final total balance with the difference, in frontend/src/components/RerunNotice.tsx
- [x] T005-012 [US005-1] Wire the page: keep the saved summary and result on load; show `RerunNotice` above the results when `isCurrentVersion` is false; Re-run runs the saved config and stores `{ saved, rerun, showing }`; the results table, chart and breakdown use the result being shown; drop the comparison on another load, another run or an import (R005-2), in frontend/src/pages/SimulationPage.tsx

**Checkpoint**: US005-1 works on its own: older results are flagged and refreshable; nothing is stored until the user saves.

---

## Phase 4: User Story 2 - Keep the saved list current: update, rename, delete (Priority: P2)

**Goal**: Update a loaded simulation or save as new, rename and delete with confirmation; the list shows and orders by the last change (FR005-007 to FR005-012).

**Independent Test**: Load, edit, run and update a simulation (one entry, update date set); rename it; delete it with confirmation; a second delete or an update of the deleted id gets a 404 and the app offers Save as new (quickstart Scenario 2).

### Tests for User Story 2 ⚠️ (write first, must fail)

- [x] T005-013 [P] [US005-2] JUnit: `update(id, result)` replaces configuration and result, keeps name and created_at, sets `updatedAt` and the new `version`; `rename(id, name)` changes only the name (`updatedAt` unchanged); `delete(id)` removes the row; each returns null/false for an unknown id; `list()` orders by `COALESCE(updated_at, created_at)` descending (an older simulation updated now comes first) (FR005-007, FR005-008, FR005-009, FR005-011), in api-server/src/test/kotlin/com/retirement/data/SimulationRepositoryTest.kt
- [x] T005-014 [P] [US005-2] JUnit: `PUT /api/simulations/{id}` 200 with the updated summary, 400 malformed id, 404 unknown; `PATCH` 200, 422 `name` for blank or over 255 characters ("Name: trimmed; 1–255 characters, else 422 with a `name` field error"), 404 unknown; `DELETE` 204 with no body, 404 on a second delete; all errors are JSON (FR005-012), in api-server/src/test/kotlin/com/retirement/api/PersistenceAndMonteCarloRoutesTest.kt
- [x] T005-015 [P] [US005-2] Deno test: the BFF proxies PUT, PATCH and DELETE to the API server with the same path and body, passes a 204 through with an empty body and a 404/422 JSON error through unchanged (R005-8), in backend/tests/routes.test.ts
- [x] T005-016 [P] [US005-2] Vitest: with a loaded simulation, the save dialog offers "Update '<name>'" and "Save as new" (name field, same 255-character rule); without one, it saves as today (FR005-007), in frontend/src/components/SaveSimulationDialog.test.tsx
- [x] T005-017 [P] [US005-2] Vitest: list entries show "Updated <date>" when `updatedAt` is set; rename refuses a blank name and calls rename with the trimmed name; delete opens a confirmation naming the simulation, Cancel keeps it, Confirm calls delete (FR005-008, FR005-009, FR005-011), in frontend/src/components/SavedSimulationsList.test.tsx
- [x] T005-018 [US005-2] Vitest: Update calls `updateSimulation` with the shown result and refreshes the list; deleting the loaded simulation keeps its results shown and makes Save offer only a new simulation; a 404 on update or rename tells the user the simulation no longer exists, refreshes the list and offers Save as new (FR005-010, FR005-012, R005-11), in frontend/src/pages/SimulationPage.test.tsx

### Implementation for User Story 2

- [x] T005-019 [US005-2] Add `updatedAt: String?` to `SavedSimulationSummary` (ISO-8601 with offset, null if never updated); add `UpdateSimulationRequest(result)` and `RenameSimulationRequest(name)`; add both to the `TsContractGenerator` roots and regenerate contract types, in api-server/src/main/kotlin/com/retirement/model/SimulationResult.kt, api-server/src/test/kotlin/com/retirement/contract/TsContractGenerator.kt, frontend/src/types/contract.generated.ts, backend/src/contract.generated.ts
- [x] T005-020 [US005-2] Implement `update`, `rename` and `delete`, and order `list()` by `COALESCE(updated_at, created_at)` descending (R005-3, R005-4, R005-5, R005-7), in api-server/src/main/kotlin/com/retirement/data/SimulationRepository.kt
- [x] T005-021 [US005-2] Add `put("/{id}")`, `patch("/{id}")` and `delete("/{id}")` to `savedSimulationRoutes`, sharing one id parser and one name validation with `post` (MAX_NAME_LENGTH 255), in api-server/src/main/kotlin/com/retirement/api/SimulationRoutes.kt
- [x] T005-022 [US005-2] Extend `proxy` to PUT, PATCH and DELETE (a 204 is returned with no body) and add the three routes, in backend/src/routes.ts
- [x] T005-023 [P] [US005-2] Add `updateSimulation(id, result)`, `renameSimulation(id, name)` and `deleteSimulation(id)`, in frontend/src/services/api.ts
- [x] T005-024 [P] [US005-2] Update or save as new in the save dialog, in frontend/src/components/SaveSimulationDialog.tsx
- [x] T005-025 [P] [US005-2] Last update date, rename (inline or dialog) and delete with a confirmation dialog naming the simulation, in frontend/src/components/SavedSimulationsList.tsx
- [x] T005-026 [US005-2] Track the loaded simulation `{ id, name }` (set by Load and Save as new, kept across runs, cleared by import, by deleting it or by a 404); handle update, rename and delete with list refresh and the 404 path; an update also drops the US005-1 comparison (FR005-005), in frontend/src/pages/SimulationPage.tsx

**Checkpoint**: US005-1 and US005-2 both work; the saved list can be kept current.

---

## Phase 5: User Story 3 - Export and import inputs as a file (Priority: P3)

**Goal**: Export inputs to a versioned file and import it, validated and defaulted by the API server like a run (FR005-013 to FR005-018).

**Independent Test**: Export a saved simulation's inputs, import into a reloaded page, run: same results; an invalid file leaves the form unchanged and lists every field error (quickstart Scenario 3).

### Tests for User Story 3 ⚠️ (write first, must fail)

- [x] T005-027 [P] [US005-3] JUnit: export returns `InputsFile` with `format` "retirement-simulator-inputs", `formatVersion` 1, `appVersion` = running version and the config; import of that file returns the same config; for `defaultFormConfig()`, `testConfig()` and every market preset, export then import then run gives a result equal to running the original (SC005-004, FR005-018); a file without `endAge` imports with 85 (FR005-017); a wrong or missing `format` or `formatVersion` 2 is "not an inputs file"; an invalid config returns all field errors at once (FR005-015), in api-server/src/test/kotlin/com/retirement/logic/InputsFileTest.kt
- [x] T005-028 [P] [US005-3] JUnit: `POST /api/inputs/export` 200 / 422 with field errors; `POST /api/inputs/import` 200 with the config, 400 JSON for not an inputs file or malformed JSON, 422 with the same field names as `POST /api/simulate`, in api-server/src/test/kotlin/com/retirement/api/PersistenceAndMonteCarloRoutesTest.kt
- [x] T005-029 [P] [US005-3] Deno test: export and import are proxied; an import body over 1 MB returns 413 JSON without calling the API server; other routes have no new limit (R005-8), in backend/tests/routes.test.ts
- [x] T005-030 [P] [US005-3] Vitest: Export downloads `<slug>-inputs.json` (same slug rule as the CSV export) with the server's file content; Import refuses a file over 1 MB or not JSON with a message before any request; a 400 shows "This file isn't a simulation inputs file"; a 422 lists every field error and leaves the form unchanged; success fills the form without running (FR005-013 to FR005-016), in frontend/src/components/InputsFileButtons.test.tsx

### Implementation for User Story 3

- [x] T005-031 [US005-3] Add the `InputsFile` model ("`format`: always `retirement-simulator-inputs`", "`formatVersion`: 1. Higher versions are rejected", `appVersion`, `config`) to the `TsContractGenerator` roots and regenerate; implement building and checking the file (lenient decoding with defaults, `ConfigValidator`), in api-server/src/main/kotlin/com/retirement/model/SimulationResult.kt, api-server/src/main/kotlin/com/retirement/logic/InputsFile.kt, api-server/src/test/kotlin/com/retirement/contract/TsContractGenerator.kt
- [x] T005-032 [US005-3] Add `POST /api/inputs/export` and `POST /api/inputs/import` routes, in api-server/src/main/kotlin/com/retirement/api/SimulationRoutes.kt
- [x] T005-033 [US005-3] Add the two BFF routes and the 1 MB limit on the import route (413 `{ error, statusCode }`), in backend/src/routes.ts
- [x] T005-034 [P] [US005-3] Add `exportInputs(config)` and `importInputs(file content)`, and the Export / Import buttons (form header and each saved simulation's Export), in frontend/src/services/api.ts, frontend/src/components/InputsFileButtons.tsx, frontend/src/components/SavedSimulationsList.tsx
- [x] T005-035 [US005-3] Wire import into the page: fill the form via `toFormConfig`, clear the loaded simulation and the comparison, show the field errors; export from the form uses the form's current config, in frontend/src/pages/SimulationPage.tsx

**Checkpoint**: All three stories work independently.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [x] T005-036 Update docs: README features table (saved simulations: version flag and re-run, update, rename, delete, export and import), spec 002 data model (SavedSimulationSummary new fields, pointing to spec 005), spec 002 `contracts/api.yaml` (new routes and fields), in README.md, specs/002-retirement-cashflow/data-model.md, specs/002-retirement-cashflow/contracts/api.yaml
- [x] T005-037 **Ask the user to confirm**, then back up the database (`cp api-server/retirement.db api-server/retirement.db.bak-<date>`), restart the API server, check the row count is unchanged and `updated_at` exists (quickstart Prerequisites)
- [x] T005-038 Run quickstart Scenarios 1–3 in the running app and through the BFF; delete the test simulations created for them. *(Found two gaps, fixed test-first: the BFF's CORS preflight didn't allow PUT, PATCH or DELETE, in backend/src/app.ts; a saved simulation's export was named after its inputs' name, not its current name, in frontend/src/pages/SimulationPage.tsx.)*
- [x] T005-039 Run all suites (API server, BFF with lint, frontend lint, tests and build); revert `api-server/src/main/resources/version.properties`; mark tasks `[x]` and set the spec's status

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: after Setup; blocks US005-2 (it needs `updated_at`). US005-1 and US005-3 don't use the column but share the repository tests, so do Phase 2 first.
- **US005-1 (Phase 3)**, **US005-2 (Phase 4)**, **US005-3 (Phase 5)**: after Phase 2, in priority order, or in parallel by different people (they touch the same `SimulationPage.tsx`, `SimulationRoutes.kt` and `routes.ts`, so merge carefully).
- **Polish (Phase 6)**: after the stories wanted. T005-037 needs the user's confirmation.

### User Story Dependencies

- **US005-1**: independent; the MVP.
- **US005-2**: independent of US005-1, except that an update also drops US005-1's comparison (T005-026) when both exist.
- **US005-3**: independent; an import also drops the comparison and the loaded simulation when those exist.

### Within Each User Story

- Tests first, failing (constitution I).
- Kotlin models and contract regeneration before frontend code that uses the generated types.
- Repository before routes; routes before the BFF route tests pass; API client before page wiring.

### Parallel Opportunities

- US005-1: T005-004, T005-005, T005-006, T005-007 in parallel; then T005-010 and T005-011 in parallel.
- US005-2: T005-013 to T005-017 in parallel; then T005-023, T005-024, T005-025 in parallel.
- US005-3: T005-027 to T005-030 in parallel; T005-034 alongside the Kotlin work once T005-031 has regenerated the types.

---

## Parallel Example: User Story 1

```text
# Tests together (different files):
T005-004 SimulationRepositoryTest: version and isCurrentVersion
T005-005 PersistenceAndMonteCarloRoutesTest: fields in list and load
T005-006 SavedSimulationsList.test: older-version mark
T005-007 RerunNotice.test: versions, re-run, switch, differences

# Then, after T005-009 regenerates the types:
T005-010 SavedSimulationsList.tsx
T005-011 RerunNotice.tsx
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Phase 1 and Phase 2.
2. Phase 3 (US005-1).
3. Validate with quickstart Scenario 1; this alone stops decisions on outdated results.

### Incremental Delivery

1. US005-1 → validate → commit when the user asks.
2. US005-2 → validate (quickstart Scenario 2).
3. US005-3 → validate (quickstart Scenario 3).
4. Polish: docs, the database backup with confirmation, full suites.
