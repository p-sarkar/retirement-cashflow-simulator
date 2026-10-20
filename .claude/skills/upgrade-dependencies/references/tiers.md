# Per-tier upgrade notes

Commands and pitfalls for each tier of this repository. Read the section for the tier you're changing.

## Contents

- [API server (Gradle, Kotlin)](#api-server-gradle-kotlin)
- [BFF (Deno)](#bff-deno)
- [Frontend (npm)](#frontend-npm)
- [GitHub Actions and CI toolchains](#github-actions-and-ci-toolchains)

---

## API server (Gradle, Kotlin)

**Where versions live**

| What | File | Note |
|------|------|------|
| Ktor, logback, Exposed, sqlite-jdbc | `api-server/gradle.properties` (`ktorVersion`, …) | One property per library family: every Ktor module uses `ktorVersion`, every Exposed module `exposedVersion` |
| Kotlin | `api-server/build.gradle.kts` `plugins { kotlin("jvm") version "…"; kotlin("plugin.serialization") version "…" }` **and** `gradle.properties` `kotlinVersion` | Three places; keep them equal. The plugins block is what actually takes effect |
| Gradle | `api-server/gradle/wrapper/gradle-wrapper.properties` | Upgrade with the wrapper task, below |
| Java | `kotlin { jvmToolchain(25) }` in `build.gradle.kts`, and `java-version` in `.github/workflows/ci.yml` | Change together |

**Commands** (from `api-server/`)

- Gradle wrapper: the project pins the distribution's SHA-256 (`distributionSha256Sum`), so pass the new release's
  published checksum:
  `SUM=$(curl -sL https://services.gradle.org/distributions/gradle-<X>-bin.zip.sha256)`, then
  `./gradlew wrapper --gradle-version <X> --distribution-type bin --gradle-distribution-sha256-sum "$SUM"`, then run
  `./gradlew wrapper --gradle-distribution-sha256-sum "$SUM"` once more so the wrapper jar and scripts come from the new
  version. Without the checksum the task fails ("contains distributionSha256Sum property, but the wrapper configuration
  does not have one"). Commit `gradle/wrapper/*`, `gradlew`, `gradlew.bat` together.
- Build and test: `./gradlew test`. A segmentation fault from the Gradle daemon is a known local issue: retry up to 3
  times, or use `--no-daemon`.
- After a Kotlin, kotlinx-serialization or model change: `./gradlew generateTsContract`. `ContractTypesTest` fails if
  the generated TypeScript is stale, so a green suite proves the contracts still match.
- Restart the API server before checking the app (CLAUDE.md, "Restart the API server").

**Pitfalls**

- **Exposed 0.x → 1.x** (done 2026-10-20, 0.47 → 1.6): go to the latest 0.x (0.61) first, check it compiles without
  deprecation warnings and the tests pass, then to 1.x. The 1.x changes this code needed:
  - Imports: JDBC operations (`Database`, `SchemaUtils`, `transaction`, `insert`, `insertAndGetId`, `selectAll`,
    `update`, `deleteWhere`) → `org.jetbrains.exposed.v1.jdbc.*`; core types (`SortOrder`, `ResultRow`, `Coalesce`,
    `eq`) → `org.jetbrains.exposed.v1.core.*`; `UUIDTable` with `java.util.UUID` →
    `org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable`; `datetime` → `org.jetbrains.exposed.v1.javatime.datetime`.
  - `Transaction` → `JdbcTransaction` (its `exec(sql) { rs -> }` still passes a `ResultSet`).
  - `newSuspendedTransaction(Dispatchers.IO) { }` → `withContext(Dispatchers.IO) { suspendTransaction { } }`.
  The repository tests (temporary SQLite databases) cover the persistence code, including the `updated_at` column
  migration in `DatabaseFactory`.
- **The user's database** (`api-server/retirement.db`) holds their saved simulations. A database library upgrade must
  not touch it until the tests pass; back it up (`cp retirement.db retirement.db.bak-<date>`, git-ignored) before the
  first server start on a new Exposed or sqlite-jdbc version, and check the row count afterwards.
- **Ktor 3.x minors** occasionally deprecate plugin APIs; deprecation warnings in the build output are worth reading.
- **Kotlin minors** can tighten compiler checks; fix the code rather than suppressing warnings.

## BFF (Deno)

**Where versions live**: `backend/deno.json` `imports` (`jsr:@oak/oak@^17.2.0`, `jsr:@std/assert@1`), resolved in
`backend/deno.lock`.

**Commands** (from `backend/`)

- See what's outdated: `deno outdated` (within ranges) and `deno outdated --latest`.
- Update within ranges: `deno outdated --update`; to the latest including majors: `deno outdated --update --latest`
  (or edit `deno.json` and run `deno install`). Both update `deno.lock`.
- Check: `deno task test`, `deno lint`, `deno check src/main.ts`.
- If Deno reports `invalid peer certificate: UnknownIssuer`, prefix the command with `DENO_TLS_CA_STORE=system`.
- The BFF reloads on its own (`deno task dev` watches), so no restart is needed.

**Pitfalls**: an Oak major can change the middleware and router APIs used in `src/app.ts` (CORS, error handling) and
`src/routes.ts`. The route tests in `backend/tests/` drive the app through `app.handle()` and cover CORS preflights,
so they catch most breakage.

## Frontend (npm)

**Where versions live**: `frontend/package.json` (ranges) and `frontend/package-lock.json` (exact). Keep both in step:
raise the range in `package.json` when you move to a new minor, so the manifest says what you tested.

**Commands** (from `frontend/`)

- Report: `npm outdated`, `npm audit` (the bundled `dependency_report.py` summarizes both).
- Patch/minor for one package: `npm install <pkg>@^<latest-in-same-major>` (updates the range and the lockfile).
- Patch/minor within existing ranges for everything: `npm update`, then raise the ranges in `package.json` to match.
- Security fixes that don't need a major: `npm audit fix`. **Never `npm audit fix --force`**: it installs majors
  without checking anything, which defeats the one-major-at-a-time approach.
- Transitive vulnerability whose parent hasn't released a fix: an `overrides` entry in `package.json` is acceptable
  if the tests pass; note it in the report so it can be removed when the parent catches up.
- Check: `npm run lint` (ESLint, plus `tsc -p e2e/tsconfig.json`), `npm test` (Vitest), `npm run build` (`tsc` + Vite).
- End-to-end: `npm run e2e` against the running app (start the API server, BFF and frontend first). After changing
  `@playwright/test`, run `npm run e2e:install` to get the matching Chromium.

**Pitfalls**

- **Peer dependencies decide groupings.** Check `npm info <pkg>@<version> peerDependencies` before a major. Don't use
  `--legacy-peer-deps` or `--force` to get past a peer conflict: the conflict is real information about what works
  together.
- **`@playwright/test` is pinned exactly** (no caret) so local and CI use the same browser build. Bump it to an exact
  version, and keep it pinned.
- **ESLint 9+** drops `.eslintrc.*` for flat config (`eslint.config.js`) and needs typescript-eslint 8 and newer React
  plugins. Upgrade ESLint, `@typescript-eslint/*` (or the `typescript-eslint` meta package), `eslint-plugin-react-hooks`
  and `eslint-plugin-react-refresh` as one group, port the rules from `.eslintrc.cjs` to `eslint.config.js`, update the
  `lint` script if its flags changed (`--ext` is gone in flat config), and keep `--max-warnings 0`.
- **Vite, `@vitejs/plugin-react` and Vitest** move together (peer ranges). `vite.config.ts` holds the Vitest config.
- **React 19** moves with `react-dom`, `@types/react`, `@types/react-dom`, and must be inside the peer ranges of MUI,
  Recharts and Testing Library. React 19 removes some deprecated APIs; run the official codemods if the release notes
  recommend them, then fix what remains.
- **MUI majors** move `@mui/material` and `@mui/icons-material` together (and `@emotion/*` if required); read the
  migration guide, since prop and import changes show up as type errors in `npm run build`.
- **TypeScript majors** must be inside typescript-eslint's supported range, or lint fails. (2026-10-20: typescript-eslint
  8.72 supports TypeScript < 6.1, so TypeScript 7 was held back.)
- **Recharts minors** can tighten prop types (3.10: `Tooltip` `formatter` receives Recharts' `ValueType`, not
  `number`); fix the callback's types rather than casting.

**Lessons from the 2026-10-20 upgrade** (React 19, MUI 9, ESLint 10, Vite 8, Vitest 5, jsdom 30)

- **ESLint 10 flat config** is in `frontend/eslint.config.js` (`@eslint/js`, `typescript-eslint`, `globals`). Version 7
  of `eslint-plugin-react-hooks` widened its `recommended` preset with React Compiler rules; the config lists only
  `rules-of-hooks` and `exhaustive-deps`, as before. Adopting the new rules is a separate decision.
  `eslint-plugin-react-refresh` 0.5 rejects non-primitive constants exported from component files: move them to a
  module in `src/utils/`.
- **Vitest 5**: `ReturnType<typeof vi.fn>` is too broad to pass as a typed callback; type mocks as
  `Mock<(arg: T) => void>` with `vi.fn<(arg: T) => void>()`.
- **Test-environment upgrades (jsdom, Vitest) can expose timing races**: a test that checks results right after
  `userEvent.upload()` passed on jsdom 26 and failed intermittently on jsdom 30, because `FileReader` completes later.
  Run the full suite several times after such an upgrade; fix a flaky test by awaiting the outcome (`vi.waitFor`,
  `findBy…`), never by retrying it.
- **Vite majors**: restart the dev server (`npm run dev`) before the end-to-end tests, or they run against the old Vite.
- **MUI majors**: the official codemods (`npx @mui/codemod …`) crashed on Node 24 (`yargs.command is not a function`).
  The v7 → v9 changes, done by hand: `fontWeight`/`alignItems` props → `sx`; `inputProps` → `slotProps.htmlInput`;
  `InputProps` → `slotProps.input`; `FormHelperTextProps` → `slotProps.formHelperText` (one `slotProps` per element);
  Dialog `PaperProps` → `slotProps.paper`; `Grid container direction="column"` → `Stack` with `Box` rows. Take a
  screenshot of the form and a dialog afterwards: layout regressions don't fail tests.
- **npm 11** prints `install-scripts` warnings for optional native packages (fsevents); they're harmless.
- On macOS, `sed` has no `\|` alternation; use Python for multi-pattern substitutions.

## GitHub Actions and CI toolchains

**Where versions live**: `.github/workflows/ci.yml`: actions pinned to major tags (`actions/checkout@v7`), and the
toolchain versions `java-version`, `node-version`, `deno-version`.

- An action major can change its inputs or the Node runtime it runs on; read its release notes, then update the
  `@vN` tag and any changed inputs.
- Toolchains: keep Java in step with `jvmToolchain`; Node on an active LTS line; Deno on `v2.x` unless a Deno major is
  part of the upgrade.
- CI changes can only be proven by CI itself. Say in the report that the workflow changes are verified when the branch
  is pushed and the PR's checks run.
