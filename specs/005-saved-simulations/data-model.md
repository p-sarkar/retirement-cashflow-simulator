# Data Model: Saved Simulations Improvements

**Feature**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

Changes to spec 002's saved simulation models ([data-model](../002-retirement-cashflow/data-model.md),
SavedSimulation and SavedSimulationSummary) and two new models. All are Kotlin `@Serializable` classes generated into
TypeScript (R005-10). Dates are ISO-8601 with offset (e.g. `2026-10-18T09:44:07-04:00`).

## Storage: `Simulations` table (API server only)

| Column | Type | Change |
|--------|------|--------|
| `id` | UUID | — |
| `name` | varchar(255) | — (rename changes it) |
| `created_at` | datetime | — |
| `updated_at` | datetime, nullable | **New**. Null until the first update (R005-6) |
| `configuration` | text (JSON) | Replaced by an update |
| `result` | text (JSON) | Replaced by an update |

Added at startup with one `ALTER TABLE … ADD COLUMN` when missing; existing rows keep null (R005-6).

## SavedSimulationSummary (changed)

One row of the list, ordered by `COALESCE(updatedAt, createdAt)` descending (FR005-011).

| Field | Type | Description |
|-------|------|-------------|
| `id`, `name`, `createdAt`, `isSuccess`, `failureYear`, `finalTotalBalance`, `endAge` | | As in spec 002 |
| `updatedAt` | String (nullable) | **New**. When inputs and results were last replaced; null if never (FR005-007) |
| `version` | String (nullable) | **New**. The version that computed the stored results (`result.apiMetadata.version`); null if it can't be read (R005-1) |
| `isCurrentVersion` | Boolean | **New**. True only when `version` equals the running version; drives the flag and the notice (FR005-001, FR005-006) |

## SavedSimulation (changed)

`{ summary: SavedSimulationSummary, result: SimulationResult, apiMetadata: ApiMetadata }`. The summary carries the new
fields above. **`apiMetadata` is new** and describes the running server, so the notice can name both versions
(FR005-002); `result.apiMetadata` remains the version that computed the results. *(Added during implementation: the
page doesn't know the running version until something has run.)*

## UpdateSimulationRequest (new)

Body of `PUT /api/simulations/{id}` (FR005-007).

| Field | Type | Rules |
|-------|------|-------|
| `result` | SimulationResult | The results shown; its `config` and `apiMetadata.version` become the stored inputs and version |

## RenameSimulationRequest (new)

Body of `PATCH /api/simulations/{id}` (FR005-008).

| Field | Type | Rules |
|-------|------|-------|
| `name` | String | Trimmed; 1–255 characters, else 422 with a `name` field error (same rule as saving) |

## InputsFile (new)

The exported inputs file and the body of an import (FR005-013 to FR005-018).

| Field | Type | Rules |
|-------|------|-------|
| `format` | String | Always `retirement-simulator-inputs`; anything else is not an inputs file (400) |
| `formatVersion` | Int | 1. Higher versions are rejected as written by a newer app (400) |
| `appVersion` | String | The version that wrote the file; informational |
| `config` | SimulationConfig | The inputs. Missing optional fields take their defaults (e.g. `endAge` 85); validated like a run (422 with field errors) |

File name on export: `<simulation name as a slug>-inputs.json` (e.g. `my-retirement-plan-inputs.json`), the same slug
rule as the CSV export. Size limit 1 MB (FR005-016).

## Frontend state (not stored)

| State | Description |
|-------|-------------|
| Loaded simulation | `{ id, name }` of the saved simulation the shown results came from; decides Update vs Save as new (R005-11) |
| Comparison | `{ saved: SimulationResult, rerun: SimulationResult, showing: 'saved' \| 'rerun' }` after a re-run of a flagged simulation (R005-2) |

## Validation summary

| Rule | Where | Result |
|------|-------|--------|
| Name trimmed, 1–255 characters | API server (save, rename) | 422 `name` |
| Simulation exists | API server (load, update, rename, delete) | 404 |
| Inputs valid | API server (`ConfigValidator`: export, import, run) | 422 per field |
| Inputs file format and version | API server (import) | 400 |
| Import body ≤ 1 MB | BFF (import route); frontend pre-check | 413 / message |
