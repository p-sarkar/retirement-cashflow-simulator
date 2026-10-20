# Release Notes: v2.0.1

**Date**: October 20, 2026
**Build**: v2.0.0 → v2.0.1

A maintenance release: every library and tool is upgraded to its latest stable version, and all known security
vulnerabilities in the project's dependencies are fixed. **Simulation results don't change.**

---

## Security

- `npm audit` finds **no vulnerabilities** (32 vulnerable packages before, including 2 critical and 24 high). The fixes
  cover the frontend's HTTP client (axios), the development server and build tools (Vite, Vitest, ESLint and their
  dependencies) and the end-to-end test tools (Playwright).
- The API server's and BFF's libraries had no known vulnerabilities, and are updated to their latest versions too.

## What you'll notice

- **Nothing changes in the results.** The upgrades change libraries, not the simulation.
- **Saved simulations are flagged as from an older version.** The app marks results computed by any other version, so
  simulations saved under v2.0.0 show "Computed by v2.0.0". **Re-run** gives the same results, with no difference in
  outcome or ending balance; update the simulation to clear the flag.
- The form and dialogs look the same after the move to a new major version of the UI library (MUI 9).

---

## For developers

| Area | Before | After |
|------|--------|-------|
| React | 18.3 | 19.3 |
| MUI | 7.3 | 9.5 |
| Vite / @vitejs/plugin-react | 6.4 / 4.7 | 8.3 / 6.1 |
| Vitest | 3.2 | 5.0 |
| ESLint / typescript-eslint | 8.57 / 7.18 | 10.13 / 8.72 (flat config `frontend/eslint.config.js`) |
| jsdom / jest-dom | 26 / 6.9 | 30 / 7.0 |
| Playwright | 1.49.1 | 1.64.1 (run `npm run e2e:install`) |
| axios / Recharts | 1.13 / 3.6 | 1.20 / 3.10 |
| Kotlin / Ktor | 2.3.0 / 3.3.3 | 2.4.21 / 3.6.0 |
| Exposed | 0.47 | 1.6 (`org.jetbrains.exposed.v1.*` packages) |
| logback / sqlite-jdbc | 1.5.24 / 3.51.1 | 1.6.5 / 3.53.4 |
| Gradle | 9.8.1 | 9.8.2 |
| @std/assert (BFF) | 1 | 1.0.19 |
| actions/upload-artifact (CI) | v4 | v7 |

- **After pulling:** run `npm install` in `frontend/` and `npm run e2e:install` for the new Playwright browser, then
  restart the API server and the frontend dev server (now Vite 8).
- **Held back:** TypeScript 7, since typescript-eslint 8.72 supports TypeScript below 6.1 only.
- **New skill:** `.claude/skills/upgrade-dependencies` runs this kind of upgrade: a read-only report of outdated and
  vulnerable dependencies, then safe updates first and each major upgrade on its own, with every suite run after each
  step. Its notes record what this upgrade needed (Exposed 1.x API changes, MUI 9 prop changes, the ESLint flat
  config).
- **Tests:** Kotlin `./gradlew test` (298), frontend `npm test` (178), backend `deno task test` (31), end-to-end
  `npm run e2e` (5).

## Known issues / follow-ups

- TypeScript 7 waits for typescript-eslint support.
- Retirement is assumed at the end of the year Retirement Age is reached. A retirement month is planned for more
  accurate projections.
- Spec 007 (replaying historical return sequences in interactive simulations) is specified but not implemented.
