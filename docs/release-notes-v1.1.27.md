# Release Notes: v1.1.27

**Date**: October 14, 2026
**Build**: v1.1.26 → v1.1.27

This release adds an **End Age** setting, resolves the known issues listed in v1.1.26, and adds CI.
**Simulation results do not change** unless you set an End Age other than 85.

---

## End Age setting (spec 002 FR-001a)

The simulation used to always run to age 85. The **General** section of the form now has an **End Age** field.

- Defaults to 85. Saved simulations and API requests without it still run to 85.
- Must be a whole number greater than Current Age and no more than 110. Otherwise the field shows an
  error and **Run Simulation** is disabled; the API returns 422 with a field error on `endAge`.
- The results table, the failure check, the "survived to age N" summary and the dates allowed for
  one-time expenses all follow End Age.
- Unchanged: the crash buffer cap still steps down at 65, 70, 75, 80 and 85, and Medicare still starts
  at 65. With an End Age above 85 the cap stops shrinking after 85.
- Current Age no longer has to be under 85, as long as End Age is later.

**Restart the API server** after upgrading. An API server from before this release rejects requests
from the new form with HTTP 400, because it doesn't know the `endAge` field.

## Fixes

- **🔍 breakdown:** loan payments are labelled "Fixed quarterly payment × 4" and show the quarterly
  amount, instead of "Inflation-adjusted amount". Cash expenses keep the inflation-adjusted label.
- **API server unreachable:** the backend now returns 503 (was 500), and the app says to check that the
  API server is running. Malformed JSON gets a 400.

---

## For developers

- **CI** (`.github/workflows/ci.yml`) runs on pull requests and pushes to `main`: `./gradlew test`
  (including the generated-types check), frontend lint, tests and build, and `deno task test`. Actions
  are on their Node 24 versions.
- **Lint:** the frontend has an ESLint config, so `npm run lint` works.
- **Backend:** Oak 17 from JSR, so it no longer runs in Node-compatibility mode under Deno 2. CORS is a
  few lines of middleware instead of the deno.land/x module. The port can be set with `PORT`
  (default 8000).
- **Gradle 9.8.1** (was 9.2.1). The wrapper checks the distribution's SHA-256. `build.gradle.kts` no
  longer uses the `by project` syntax that Gradle 10 removes.
- **Tests:** Kotlin `./gradlew test` (70), frontend `npm test` (59), backend `deno task test` (7).
- **Simulation period:** `SimulationPeriod` derives the simulated years from `config.endAge`. The
  frontend no longer keeps its own copy of 85; it only mirrors the 110 limit (`MAX_END_AGE`).

## Known issues / follow-ups

- Builds with `--warning-mode all` show a `Configuration.setVisible` deprecation from the Kotlin Gradle
  plugin 2.3.0. It breaks nothing until Gradle 11; upgrading Kotlin fixes it.
- GitHub moves `ubuntu-latest` to Ubuntu 26 from October 19, 2026. CI may need attention if something
  breaks on the new image.
