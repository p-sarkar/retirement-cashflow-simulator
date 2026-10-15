# Claude Code Instructions

Retirement cash flow simulator: React frontend (port 5173) → Deno BFF (port 8000) → Kotlin API server (port 8090) → SQLite. Specs live in `specs/` (spec-kit); the governing rules are in `.specify/memory/constitution.md`.

## Restart the API server after any `api-server/` change

The Kotlin API server does not hot-reload. After changing anything in `api-server/` (Kotlin sources, `build.gradle.kts`, resources, configuration), restart it before testing in the app and before finishing the task.

- Restart once after a set of changes, not after every edit: a restart in the middle of a change fails to compile.
- The frontend (Vite) and the BFF (`deno task dev`) reload on their own; documentation changes need nothing.

Restart (from the repo root):

```bash
# 1. Stop the API server
lsof -ti:8090 | xargs kill -9 2>/dev/null

# 2. Start the API server
cd api-server && nohup ./gradlew run > apiserver.log 2>&1 &

# 3. Check it is listening (it takes about 10-20 seconds to start)
lsof -i:8090
```

Then confirm it responds (`curl -s localhost:8090/`) before relying on it. See "API Server Restart Protocol" in README.md.

## Architecture rules (constitution)

- **Strict tiers.** The frontend only presents and calls the BFF; no business logic. The BFF only proxies, validates and handles errors; no simulation. All simulation logic, validation of inputs and database access live in the Kotlin API server. Only the API server touches SQLite.
- **Contracts are generated.** Never edit `frontend/src/types/contract.generated.ts` or `backend/src/contract.generated.ts`. Change the Kotlin `@Serializable` model, then run `cd api-server && ./gradlew generateTsContract`. New request/response types must be added to the roots in `TsContractGenerator.kt`. `ContractTypesTest` fails if the files are stale.
- **Deterministic simulation.** The same config (and seed, for Monte Carlo) must always give the same result. No unseeded randomness, wall-clock time or shared mutable state in the engines.
- **Units.** Rates are decimals in the API (0.03 = 3%) and whole-number percentages in the form; loan `aprPercent` is a whole number in both. Convert only in `frontend/src/utils/formConfig.ts`.

## Development practices

- **Tests first.** Write or update the failing test before the code, especially for financial logic (`SimulationEngine`, `SpendingStrategy`, `MonteCarloEngine`). Each behaviour change needs a test that would have failed without it.
- **Test every tier you touch**, and run the full suites before calling work done:
  - API server: `cd api-server && ./gradlew test`
  - BFF: `cd backend && deno task test` (and `deno lint`)
  - Frontend: `cd frontend && npm run lint && npm test && npm run build`
  - CI (`.github/workflows/ci.yml`) runs the same on every pull request; keep it green.
- **Verify in the running app** for UI or behaviour changes, not only unit tests: restart the API server, then check the flow end to end.
- **Small, focused changes.** Match the surrounding code's style, naming and comment density. Don't refactor unrelated code or change business rules (e.g. the spending strategy) without asking; report suspected logic problems instead.
- **Validate at the API boundary.** Invalid input returns 422 with per-field errors (`ConfigValidator`); every error the frontend sees is JSON.
- **Keep specs in sync.** When implementing spec-kit tasks, mark them `[x]` in the feature's `tasks.md` and update `spec.md`, `data-model.md` and `contracts/` when behaviour or models change. Update `README.md` when run instructions or features change.
- **No secrets or local data in git.** `*.db`, logs and `.env*` stay out. Every Gradle build rewrites `api-server/src/main/resources/version.properties` (build time); revert it unless you are making a release.

## Git and releases

- Branch from `main` (`feat/…`, `fix/…`, `docs/…`) and merge through a pull request after CI passes.
- Commit messages follow Conventional Commits: `feat:`, `fix:`, `docs:`, `chore:`, `build:`, with a short body explaining what and why.
- **Releases** are their own commit and PR (`chore: Release vX.Y.Z`): run `cd api-server && ./gradlew incrementBuildNumber && ./gradlew build`, add `docs/release-notes-vX.Y.Z.md`, restart the services to verify, then commit and push.

## Never do without explicit confirmation

- Publish packages (`npm publish`) or deploy anywhere
- Run database migrations or delete data in a real database (`api-server/retirement.db` holds the user's saved simulations)
- Any destructive operation: force pushes, history rewrites, deleting branches or files you did not create

## Environment notes

- If a command fails with a segmentation fault (seen with the Gradle daemon), retry it up to 3 times; `./gradlew run --no-daemon` also helps.
- If Deno reports `invalid peer certificate: UnknownIssuer`, prefix the command with `DENO_TLS_CA_STORE=system`.
