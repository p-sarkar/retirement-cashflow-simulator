# Implementation Plan: Saved Simulations Improvements

**Branch**: `feat/005-saved-simulations` | **Date**: 2026-10-18 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-saved-simulations/spec.md`

## Summary

Make saved simulations trustworthy and manageable. The API server reports, for every saved simulation, the version
that computed its results and whether that is the running version; the frontend flags older results, re-runs them
with the existing simulate endpoint and shows the saved and re-run results side by side until the user saves
(US005-1). New endpoints update, rename and delete a saved simulation, with a nullable `updated_at` column added to
the `Simulations` table (US005-2). An inputs file format owned by the API server is produced by an export endpoint and
checked by an import endpoint with the same validation as a run (US005-3). Decisions are in [research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin (JVM toolchain 25) for the API server; TypeScript 5 (Deno for the BFF, React 18 for the frontend)
**Primary Dependencies**: Ktor, Exposed 0.47 with SQLite JDBC, kotlinx.serialization; Oak (BFF); React, MUI, Vite
**Storage**: SQLite `api-server/retirement.db`, `Simulations` table (id, name, created_at, configuration, result);
gains a nullable `updated_at` column (R005-6)
**Testing**: JUnit 5 (`./gradlew test`), `deno test` + `deno lint`, Vitest + ESLint + `npm run build`
**Target Platform**: Local web application (frontend 5173 → BFF 8000 → API server 8090)
**Project Type**: N-tier web application (existing)
**Performance Goals**: List, load, update, rename, delete, export and import each answer in under 500 ms for up to
200 saved simulations; a re-run is one interactive simulation (SC002-001)
**Constraints**: strict tiers (constitution II); contracts generated from Kotlin (III); the user's database holds
their saved simulations, so the schema change needs a backup and their confirmation before it touches it (CLAUDE.md)
**Scale/Scope**: One user, tens to a few hundred saved simulations; 3 user stories, 18 functional requirements

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How |
|-----------|--------|-----|
| I. Test-first | PASS | Every story starts with failing JUnit route and repository tests (temporary database), BFF route tests and Vitest component tests (tasks phase) |
| II. Strict tiers | PASS | The API server decides whether a version is current (R005-1), owns the inputs file format and validates imports (R005-9), and is the only tier touching SQLite. The BFF only proxies (new methods, R005-8) and limits the import's size. The frontend presents: it holds the saved and re-run results it already received and shows their summary values side by side (R005-2) |
| III. Contracts | PASS | New and changed models are Kotlin `@Serializable` classes added to `TsContractGenerator` roots; TypeScript is regenerated (R005-10) |
| IV. Deterministic simulation | PASS | No engine change. Results depend only on inputs and version, which is what makes "computed by the running version" safe to trust without a re-run |
| Identifiers | PASS | FR005-, SC005-, US005-, R005- identifiers throughout |

**Post-design re-check (after Phase 1)**: PASS. The design adds no business logic outside the API server; the one
risk is the schema change, handled as an additive, idempotent column with a confirmation gate (R005-6).

## Project Structure

### Documentation (this feature)

```text
specs/005-saved-simulations/
├── plan.md              # This file
├── research.md          # Phase 0: decisions R005-1 to R005-11
├── data-model.md        # Phase 1: changed and new models, table change
├── quickstart.md        # Phase 1: end-to-end validation scenarios
├── contracts/
│   └── saved-simulations-api.md   # Phase 1: new and changed endpoints
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
api-server/src/main/kotlin/com/retirement/
├── api/SimulationRoutes.kt          # savedSimulationRoutes: PUT, PATCH, DELETE; inputs export/import routes
├── data/SimulationsTable.kt         # + updatedAt (nullable)
├── data/DatabaseFactory.kt          # add missing columns at startup (R005-6)
├── data/SimulationRepository.kt     # update, rename, delete; version and order by last change
├── logic/InputsFile.kt              # new: build and check the inputs file (R005-9)
└── model/SimulationResult.kt        # SavedSimulationSummary fields; request models; InputsFile
api-server/src/test/kotlin/com/retirement/
├── data/SimulationRepositoryTest.kt # + update, rename, delete, ordering, column added to an old database
├── api/PersistenceAndMonteCarloRoutesTest.kt # + new routes, 404s, 422s, version flag
├── logic/InputsFileTest.kt          # new: round trip, older files, invalid files
└── contract/TsContractGenerator.kt  # + new roots

backend/
├── src/routes.ts                    # proxy PUT/PATCH/DELETE (204), inputs export/import, 1 MB import limit
└── tests/routes.test.ts             # + the new routes

frontend/src/
├── pages/SimulationPage.tsx         # loaded simulation id, saved vs re-run results, update/save-as-new, import
├── components/SavedSimulationsList.tsx  # version flag, last update, rename, delete (confirm), export
├── components/SaveSimulationDialog.tsx  # update or save as new
├── components/RerunNotice.tsx       # new: older-version notice, re-run, saved/re-run switch and differences
├── components/ImportInputsButton.tsx    # new: file picker, size check, field errors
└── services/api.ts                  # new calls
```

**Structure Decision**: The existing three tiers. No new project or dependency; one new Kotlin file for the inputs
file and two new React components.

## Complexity Tracking

No constitution violations. One operational risk is tracked instead: the `updated_at` column changes the schema of
the user's database (R005-6). The change is additive and nullable, and the first start against
`api-server/retirement.db` waits for a backup and the user's confirmation.
