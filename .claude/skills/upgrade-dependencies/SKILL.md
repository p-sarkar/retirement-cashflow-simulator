---
name: upgrade-dependencies
description: Upgrade this project's libraries and tools to their latest stable versions and fix known security
  vulnerabilities, across all tiers (Kotlin/Gradle API server, Deno BFF, React/npm frontend, GitHub Actions). Applies
  security fixes and patch/minor updates first, then each major upgrade on its own, with every test suite run after
  each step. Use this whenever the user asks to upgrade, update or bump dependencies, libraries, packages, Gradle,
  Kotlin, Ktor, Exposed, React, MUI, Vite, ESLint, Deno or Oak; to fix npm audit, Dependabot, CVE or security
  vulnerability findings; or to check what's outdated, even if they only mention one tier or one package.
argument-hint: "Optional scope, e.g. 'frontend only', 'security fixes only', 'just Ktor'"
---

# Upgrade dependencies

Bring the project's dependencies to their latest stable versions and close known vulnerabilities, without leaving
the app broken. The approach is **safe changes first, then majors one at a time**: patch and minor releases and
non-breaking security fixes rarely need code changes, so they go in together; a major release can break things, so
each one gets its own step, its own full test run and its own way back.

The project's rules in `CLAUDE.md` apply throughout. The ones that matter most here:

- **Never commit or push unless the user explicitly asks.** Step 1 asks once whether to commit each step as it passes.
- **Run every suite of every tier you touch** (API server, BFF, frontend), and restart the API server after changing
  anything in `api-server/` before checking the app.
- **The user's database** (`api-server/retirement.db`) holds their saved simulations: back it up before a new database
  library first runs against it.
- **Don't change business rules or refactor unrelated code.** An upgrade may require code changes; keep them to what
  the new version needs.

Per-tier commands and pitfalls are in [references/tiers.md](references/tiers.md). Read the section for each tier as
you start changing it.

## 1. Start from a known-good state

1. **Check the working tree is clean** and `main` is up to date (`git fetch`, `git status`). If there are uncommitted
   changes, stop and ask: mixing them with an upgrade makes both hard to review.
2. **Create a branch** from `origin/main`: `chore/upgrade-deps-<YYYY-MM-DD>`.
3. **Ask once how to checkpoint.** Committing each step locally (no push) after its tests pass gives clean, reviewable
   history and an easy way to undo a failed major. Ask: "Commit each upgrade step on the branch as it passes (no push),
   or keep everything uncommitted for you to review?" If they decline, use the snapshot method in step 5.
4. **Run the baseline** so later failures can be attributed to the upgrade, not to something already broken:
   - `cd api-server && ./gradlew test`
   - `cd backend && deno task test && deno lint && deno check src/main.ts`
   - `cd frontend && npm run lint && npm test && npm run build`
   - End-to-end, if the services are running: `cd frontend && npm run e2e`

   Note the test counts. If the baseline fails, stop and report: an upgrade can't be verified on a red baseline.

## 2. Report what's outdated and vulnerable

Run the bundled report (read-only, about 10 seconds):

```bash
python3 .claude/skills/upgrade-dependencies/scripts/dependency_report.py
```

It lists, per tier, each dependency's current and latest stable version, whether the update is patch, minor or
MAJOR, known vulnerabilities (OSV for Maven and npm packages, `npm audit` for the frontend), and the GitHub Actions
versions. Save the output to the scratchpad: the final report compares against it.

Show the user a short summary before changing anything: how many patch/minor updates, which majors (grouped as in
step 4), and the vulnerabilities by severity, noting which fixes need a major. If the user gave a scope (one tier,
security only, one library), plan only that.

## 3. Safe batch: security fixes and patch/minor updates

Apply, per tier (see references/tiers.md for the exact commands):

- **Frontend:** `npm audit fix` (never `--force`), then each patch/minor update to the latest version in its current
  major, raising the range in `package.json`. `@playwright/test` stays pinned exactly; run `npm run e2e:install` if it
  changes.
- **API server:** each `gradle.properties` version to the latest in its current major; Kotlin in all three places;
  the Gradle wrapper to the latest patch/minor.
- **BFF:** `deno outdated --update`.
- **Actions:** nothing; they're pinned to major tags.

Then run every suite (as in the baseline), regenerate the contracts if Kotlin or serialization changed
(`./gradlew generateTsContract`), restart the API server and check the app responds (`curl -s localhost:8090/`, then a
simulation through the BFF). If something fails, find the package responsible (bisect the batch) and hold that one
back to the major phase or the report, rather than weakening a test.

Checkpoint: commit as `build(deps): Update patch and minor versions; fix <n> vulnerabilities` if the user agreed, with
the before/after versions in the body.

## 4. Majors, one group at a time

Some packages must move together because of peer dependencies or shared versions. Group them, then order the groups:

1. **Majors that fix a vulnerability** (the report shows these as "fix via …(MAJOR)"), most severe first.
2. **Build and test tooling** (Gradle, ESLint and its plugins, TypeScript, Vite and Vitest, jsdom, Testing Library,
   Playwright, Actions): failures show up in the build or tests, not in the user's results.
3. **Runtime libraries** (React, MUI, Recharts, axios, Ktor, Exposed, sqlite-jdbc, logback, Oak): these affect the
   running app, so check it in the browser after each.

Typical groups in this repo: React + react-dom + @types/react + @types/react-dom; ESLint + typescript-eslint +
eslint-plugin-react-hooks + eslint-plugin-react-refresh (+ flat config); Vite + @vitejs/plugin-react + Vitest;
@mui/material + @mui/icons-material; all Exposed modules (one property); all Ktor modules (one property).

For each group:

1. **Read the migration guide or release notes** for the versions you're crossing (WebFetch the official changelog).
   Knowing the breaking changes up front is faster than reverse-engineering compiler errors.
2. **Checkpoint the current state** (step 5), so this group can be undone without losing the others.
3. **Upgrade the group** and fix what the new versions require: code, config, scripts. Keep each fix minimal and in
   the style of the surrounding code; don't take the opportunity to refactor.
4. **Run every suite**, regenerate the contracts if models or serialization changed, and for runtime libraries
   restart the API server (and the Vite dev server for frontend tooling) and check the app end to end (`npm run e2e`
   with the services running, or the flows the library touches). After a test-environment upgrade (Vitest, jsdom), run
   the frontend suite several times: new timing can expose flaky tests. After a UI library upgrade, look at screenshots
   of the main screens, since layout changes don't fail tests.
5. **If it's green**, checkpoint it (commit `build(deps): Upgrade <group> to <version>` if agreed). **If it can't be
   made green** with reasonable, targeted changes (for example, a peer it needs isn't released yet, or the migration
   needs design decisions), restore the checkpoint and record why. One stuck major shouldn't block the others.

Don't get past a failure by loosening the project's checks: no `--legacy-peer-deps` or `--force`, no disabled lint
rules or `@ts-ignore`, no skipped or weakened tests. Each of those hides exactly the breakage the upgrade introduced.
If a test's expectation genuinely changes because a library's behaviour changed, explain it in the report.

## 5. Checkpoints without commits

If the user declined per-step commits, snapshot before each major group:

```bash
snap=$(git stash create)        # a commit object of the working tree; changes nothing, adds nothing to any branch
git status --porcelain          # note untracked files now, to know which ones the attempt creates
```

To undo a failed group: `git checkout $snap -- .` restores the tracked files (when `snap` is empty, nothing had changed
yet: use `git checkout HEAD -- .`). Then delete only the untracked files that this attempt created, and reinstall
(`npm ci` in `frontend/`, `deno install` in `backend/`) so `node_modules` and caches match the restored lockfiles.

## 6. Final verification

1. Run every suite one last time, and `npm run e2e` with the services running.
2. Restart the API server; check the app in the browser (a simulation, a saved simulation, Monte Carlo).
3. Re-run `dependency_report.py` and compare with step 2.
4. Update docs that state versions (README's tech stack, CLAUDE.md commands) if they changed.

## 7. Report

Write the report for the user in this shape:

```markdown
## Dependency upgrade: <date>

**Result**: <n> packages updated, <n> majors done, <n> majors held back; vulnerabilities <before> → <after>.

| Tier | Package | Before | After | Notes |
|------|---------|--------|-------|-------|

**Vulnerabilities fixed**: <by severity, with advisory names for high/critical>
**Still open**: <each remaining vulnerability, why (no fix released, needs a held-back major), and the plan>
**Majors held back**: <each, the blocker, what it would take>
**Code changes the upgrades needed**: <file, what changed, why>
**Checks**: <suites and counts before/after; end-to-end; app checks>
**CI**: workflow changes are verified when the branch is pushed and the PR's checks run.
```

Then stop. Push, open a PR or merge only when the user asks. If the user asks to commit everything at once, use
`build(deps): Upgrade dependencies (<date>)` with the table in the body.
