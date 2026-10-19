# API Contract: Saved Simulations Improvements

**Feature**: [spec.md](../spec.md) | **Models**: [data-model.md](../data-model.md)

All routes are served by the API server and proxied unchanged by the BFF (same paths, statuses and JSON bodies).
Errors are JSON: `ErrorResponse { error, statusCode }`, or `ValidationErrorResponse { error, statusCode,
validationErrors: [{ field, message }] }` for 422 (the existing shapes). Existing routes not listed here are unchanged.

## Changed

### `GET /api/simulations`

**200**: `SavedSimulationSummary[]`, ordered by last change (`updatedAt`, else `createdAt`), newest first. Each summary
now has `updatedAt`, `version` and `isCurrentVersion` (FR005-001, FR005-011).

### `GET /api/simulations/{id}`

**200**: `SavedSimulation`, whose `summary` has the new fields, plus `apiMetadata` for the running server (FR005-002).
400 malformed id, 404 missing (unchanged).

### `POST /api/simulations`

Unchanged request (`{ name, result }`) and statuses; the returned summary has the new fields (`updatedAt` null).

## New

### `PUT /api/simulations/{id}`: update (FR005-007)

**Request**: `UpdateSimulationRequest { result }`
**200**: `SavedSimulationSummary` with the new inputs' outcome, `updatedAt` set, `version` = `result.apiMetadata.version`.
**400**: malformed id. **404**: no such simulation (FR005-012).

### `PATCH /api/simulations/{id}`: rename (FR005-008)

**Request**: `RenameSimulationRequest { name }`
**200**: `SavedSimulationSummary` with the new name; `updatedAt` unchanged.
**422**: `name` blank or longer than 255 characters. **400**: malformed id. **404**: no such simulation.

### `DELETE /api/simulations/{id}`: delete (FR005-009)

**204**: deleted, no body. **400**: malformed id. **404**: no such simulation.

### `POST /api/inputs/export` (FR005-013)

**Request**: `SimulationConfig`
**200**: `InputsFile { format: "retirement-simulator-inputs", formatVersion: 1, appVersion, config }`
**422**: invalid inputs, with the same field errors as `POST /api/simulate`.

### `POST /api/inputs/import` (FR005-014 to FR005-017)

**Request**: the file's JSON content (an `InputsFile`)
**200**: `SimulationConfig`, with defaults applied to missing optional fields.
**400**: not an inputs file (wrong or missing `format`, unknown `formatVersion`, malformed JSON or config shape).
**422**: valid file, invalid inputs: every invalid field at once, same field names as a run.

## BFF

- Proxies the routes above, including PUT, PATCH and DELETE; a 204 passes through with no body.
- Allows PUT, PATCH and DELETE in its CORS preflight (`Access-Control-Allow-Methods`), since the frontend calls it from
  another origin. *(Found by the end-to-end check, T005-038: the browser blocked updates until then.)*
- Rejects an import (`POST /api/inputs/import`) body over 1 MB with **413** `{ error, statusCode }` (FR005-016). Other
  routes have no new limit (a saved result can be several hundred KB).
- 503 when the API server is unreachable (unchanged).
