# Research: Saved Simulations Improvements

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

What exists today (spec 002): `POST /api/simulations` saves `{ name, result }` (name trimmed, 1–255 characters, 422
otherwise), `GET /api/simulations` lists summaries newest first, `GET /api/simulations/{id}` returns the saved
simulation (400 for a malformed id, 404 if missing). The `Simulations` table stores id, name, created_at, the config
JSON and the result JSON. Every result carries `apiMetadata.version`. The BFF proxies GET and POST only.

## R005-1. Who decides whether saved results are out of date (FR005-001, FR005-002, FR005-006)

**Decision**: The API server. `SavedSimulationSummary` gains `version` (the `apiMetadata.version` of the stored result,
or null when it can't be read) and `isCurrentVersion` (true only when `version` equals the running
`ServerInfo.version`). The repository reads the version from the stored JSON directly, so a result too old to decode
still lists as "unknown version" instead of failing the whole list.

**Rationale**: Whether results are current is a business rule (results depend only on inputs and version, constitution
IV); the frontend only shows the flag (constitution II). Equality, not "older than", covers a downgraded app too (spec
edge case) and needs no version parsing.

The load response (`SavedSimulation`) also carries the running server's `apiMetadata`, as other responses do, so the
notice can name the running version before anything has run (FR005-002).

**Alternatives considered**: Frontend compares the saved version with the version of its last response: it has no
version before the first run, and puts the rule in the presentation tier. A `GET /api/version` endpoint: the load
response carrying `apiMetadata` gives the same with no new endpoint.

## R005-2. Re-run and comparison (FR005-003 to FR005-005)

**Decision**: The re-run calls the existing `POST /api/simulate` with the saved config. The page keeps both results it
received (saved and re-run) and a switch between them; the comparison shows each result's own summary values
(outcome, failure year, final total balance) next to each other, with the balance difference. Nothing is sent to the
server until the user saves. The comparison is dropped when another simulation is loaded, inputs are imported, other
inputs are run, or the simulation is updated (FR005-005).

**Rationale**: A re-run is exactly a fresh run of the same inputs (SC005-002), so no new endpoint is needed. Showing two
results the server computed, and one subtraction for display, is presentation, not business logic.

**Alternatives considered**: A server-side "re-run saved simulation" endpoint that returns both results: duplicates
`/api/simulate` and the load call. Automatic re-run on load: rejected in the spec (no automatic migration).

## R005-3. Update (FR005-007)

**Decision**: `PUT /api/simulations/{id}` with `{ result }`. It replaces the configuration and result, keeps the name
and created_at, sets updated_at to now, and returns the new summary. 404 if the simulation no longer exists (FR005-012:
the frontend then offers Save as new with the same result).

**Rationale**: Mirrors the save body; the name is changed only by rename (R005-4), so an update can't rename by
accident.

**Alternatives considered**: Delete then save: loses the creation date and id. A single PATCH for both name and result:
mixes two user actions with different validation.

## R005-4. Rename (FR005-008)

**Decision**: `PATCH /api/simulations/{id}` with `{ name }`, validated by the same function as save (trimmed, 1–255
characters, 422 with a `name` field error). It changes only the name; updated_at is not touched (renaming doesn't change
inputs or results).

**Rationale**: One name rule for save and rename. The spec draft said 100 characters; aligned to the existing 255
(column size and save dialog) so saved names can always be renamed to themselves.

## R005-5. Delete (FR005-009, FR005-010, FR005-012)

**Decision**: `DELETE /api/simulations/{id}` returns 204, or 404 if missing. The confirmation (naming the simulation)
is in the frontend. If the deleted simulation is the loaded one, the page clears its loaded id, so its results stay
shown and saving creates a new simulation.

**Rationale**: Standard REST semantics; the confirmation is a user-interaction concern.

## R005-6. Schema change: `updated_at` (FR005-007, FR005-011)

**Decision**: Add a nullable `updated_at` datetime column. At startup, `DatabaseFactory.init` creates the table if
missing (as today) and, if the existing table lacks `updated_at`, runs one `ALTER TABLE Simulations ADD COLUMN
updated_at` (checked with SQLite's table info). Existing rows keep null, read as "never updated". The list orders by
`COALESCE(updated_at, created_at)` descending.

**Safety**: The change is additive and idempotent: no existing column or row is modified, and a second start does
nothing. Tests use temporary databases, including one created with the old schema and a saved row, to prove the row
survives and gains a null `updated_at`. **Before the first start of the new API server against the user's
`api-server/retirement.db`, back it up (`cp retirement.db retirement.db.bak-<date>`) and get the user's explicit
confirmation** (CLAUDE.md: "Run database migrations … need explicit confirmation").

**Alternatives considered**: `SchemaUtils.createMissingTablesAndColumns`: available in Exposed 0.47, but it also
issues statements to alter existing columns, which SQLite largely doesn't support; adding one known column is
narrower. Storing the update date inside the result JSON: hides a list field inside a blob and complicates ordering.

## R005-7. List order and dates (FR005-011)

**Decision**: `SavedSimulationSummary` gains `updatedAt` (ISO-8601 with offset, like `createdAt`; null if never
updated). The list shows "Updated <date>" when set, otherwise the creation date, and is ordered by the latest of the two.

## R005-8. BFF proxy for the new methods

**Decision**: Extend the BFF's `proxy` to PUT, PATCH and DELETE, and pass a 204 through with an empty body, and allow
those methods in the BFF's CORS preflight (the frontend runs on another origin). Add the
inputs routes (R005-9). The BFF rejects an import body over 1 MB with 413 and a JSON error (FR005-016), as request
validation, its role in constitution II. The limit applies to the import route only: a saved result is about 190 KB
for the default plan (age 51 to 85) and could approach 1 MB for the longest plans (18 to 110), so a global limit could
block saving a valid simulation.

## R005-9. Inputs file format, export and import (FR005-013 to FR005-018)

**Decision**: A Kotlin `InputsFile` model: `format` (always `retirement-simulator-inputs`), `formatVersion` (1),
`appVersion` (the version that wrote it) and `config` (a `SimulationConfig`).

- **Export**: `POST /api/inputs/export` with a `SimulationConfig` validates it like a run (422 with field errors) and
  returns the `InputsFile`; the frontend downloads it as `<slug of name>-inputs.json`. For a saved simulation, the
  frontend sends the saved config.
- **Import**: the frontend reads the chosen file (rejecting > 1 MB or unparsable JSON with a message, before any
  request) and posts its content to `POST /api/inputs/import`. The server checks `format` and `formatVersion` (≤ 1;
  400 "not an inputs file" otherwise), decodes the config with defaults for missing fields (FR005-017, the same
  lenient decoding as saved simulations), validates it with `ConfigValidator` (422 with the same field errors as a
  run, FR005-015) and returns the config. The frontend fills the form with it (FR005-014).

**Rationale**: The file format is a contract owned by the API server, generated into TypeScript like every other
model; the server stamps the version and applies the same validation and defaults as everywhere else. Only the
download/upload of bytes happens in the browser.

**Alternatives considered**: Build and parse the file in the frontend only: puts the format and defaults in the
presentation tier, and the API would see the config only at run time. Validate imports by calling `/api/simulate`:
runs a simulation the user didn't ask for (FR005-014).

## R005-10. Contracts

**Decision**: New or changed `@Serializable` models: `SavedSimulationSummary` (+ `version`, `isCurrentVersion`,
`updatedAt`), `UpdateSimulationRequest`, `RenameSimulationRequest`, `InputsFile`. Add the new ones to
`TsContractGenerator` roots and run `./gradlew generateTsContract`; `ContractTypesTest` guards staleness.

## R005-11. Which save action the dialog offers (FR005-007, FR005-010)

**Decision**: The page tracks the id and name of the loaded saved simulation. While set, Save opens a dialog with
"Update '<name>'" and "Save as new" (with a name field); otherwise it works as today. The id is set by Load and by Save
as new, kept across re-runs and runs of edited inputs, and cleared by an import, a delete of that simulation, or a 404
on update.
