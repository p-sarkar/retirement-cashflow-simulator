# Quickstart: Validate Saved Simulations Improvements

**Feature**: [spec.md](./spec.md) | **Contract**: [contracts/saved-simulations-api.md](./contracts/saved-simulations-api.md)

Scenarios that prove the feature end to end, in the running app and through the BFF. Unit and route tests are in
`tasks.md`.

## Prerequisites

1. **Back up the database first.** The new API server adds the `updated_at` column to `api-server/retirement.db` on
   its first start (R005-6). Before that start, with the user's confirmation:

   ```bash
   cp api-server/retirement.db api-server/retirement.db.bak-$(date +%Y%m%d)
   ```

2. Restart the API server (CLAUDE.md), then check it answers:

   ```bash
   lsof -ti:8090 -sTCP:LISTEN | xargs kill -9 2>/dev/null
   cd api-server && nohup ./gradlew run > apiserver.log 2>&1 &
   curl -s localhost:8090/
   ```

3. The BFF (`cd backend && deno task dev`) and the frontend (`cd frontend && npm run dev`) are running.

4. Check the column was added and no row was lost (read-only):

   ```bash
   sqlite3 -readonly api-server/retirement.db "select count(*), count(updated_at) from Simulations;"
   ```

   Expected: the same row count as before; `count(updated_at)` is 0.

## Scenario 1: Older results are flagged and can be re-run (US005-1, SC005-001, SC005-002)

1. Create a simulation computed by an "older version" without touching existing entries: run a simulation through
   the BFF (`POST /api/simulate`), set the response's `apiMetadata.version` to `1.1.28`, and save it with
   `POST /api/simulations` under a test name. Delete it at the end.
2. `GET /api/simulations`: that entry has `version` 1.1.28 and `isCurrentVersion: false`; entries saved by the running
   version have `isCurrentVersion: true`.
3. In the app, the Saved Simulations tab marks the older entry. Load it: the results are labelled with v1.1.28, with a
   notice naming both versions and a Re-run button. A current entry shows no notice.
4. Re-run: the results match a fresh Run Simulation of the same inputs (compare Total Portfolio in the last row). The
   outcome, failure year and ending balance of both are shown side by side; switching to Saved shows the original rows.
5. `GET /api/simulations/{id}`: the stored result is unchanged (still 1.1.28).

## Scenario 2: Update, rename, delete (US005-2, SC005-003)

1. Load a saved simulation, change an input (e.g. Wants), run, Save: the dialog offers "Update '<name>'" and "Save as
   new". Update: the list still has one entry with that name, now first, with an update date and the new outcome.
2. Rename it from the list: the new name shows; its update date doesn't change. A blank name is refused.
3. Delete it: a confirmation names it; Cancel keeps it, Confirm removes it. The loaded results stay shown, and Save now
   only offers a new simulation.
4. Through the BFF: `DELETE` the same id again returns 404 JSON; `PUT` to it returns 404, and the app offers Save as
   new.

## Scenario 3: Export and import inputs (US005-3, SC005-004, SC005-005)

1. Export a saved simulation's inputs: a file `<name>-inputs.json` downloads with `format`, `formatVersion: 1`,
   `appVersion` and `config`, and no results.
2. Reload the page, import the file: the form is filled, nothing runs. Run: the results equal the saved simulation's
   when re-run (Scenario 1, step 4).
3. Edit the file so `retirementAge` is 5 and import: the form is unchanged and the Retirement Age error is listed (with
   every other invalid field).
4. Import a file that isn't an inputs file (e.g. a CSV export), and one over 1 MB: each is refused with a message; the
   form is unchanged.
5. Remove `endAge` from a file and import: End Age is 85.

## Checks to run

- `cd api-server && ./gradlew test` (and revert `api-server/src/main/resources/version.properties`)
- `cd backend && deno task test && deno lint`
- `cd frontend && npm run lint && npm test && npm run build`
