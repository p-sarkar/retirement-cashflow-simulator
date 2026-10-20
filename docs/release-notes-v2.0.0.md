# Release Notes: v2.0.0

**Date**: October 20, 2026
**Build**: v1.1.29 → v2.0.0

This release adds a second spending strategy, Risk-Based Guardrails, makes saved simulations manageable and aware of
the version that computed them, extends the market history to 2025, and changes how versions are numbered.
**Simulation results change**: fixes to the existing spending strategy and the longer market history change some
results, mostly for the better in down markets.

---

## Risk-Based Guardrails spending strategy (spec 006)

The spending strategy is now chosen per simulation, in **Withdrawal Strategy** on the form.

- **Partha's Spending Strategy** (the existing one) stays the default.
- **Risk-Based Guardrails** reviews the plan yearly or quarterly. Each review estimates the probability of success over
  500 market paths and, when it crosses the lower or upper guardrail, resets Wants to bring it back to the target.
  Only Wants change, and raises stop at 150% of the configured Wants. The bucket rules still decide which accounts pay.
- Results show a guardrails summary, the dollar guardrails, Wants and probability charts, extra table and CSV columns,
  a section in the 🔍 breakdown, and, for Monte Carlo, spending percentiles and the share of runs that cut spending.
- Settings are validated field by field, kept when you switch strategies, and saved with the simulation.

## Saved simulations (spec 005)

- **Results from an older version are flagged.** A saved simulation computed by another version is marked in the list.
  Loading it shows a notice with both versions; **Re-run** computes it with this version, and you can compare the
  outcome, failure year and ending balance with the saved results before saving. *Every simulation saved under v1.1.29
  will be flagged after this upgrade, since 2.0.0 changes some results; re-run it to see the current ones.*
- **Update, rename, delete.** Saving a loaded simulation offers **Update** or **Save as new**. Rename and delete are in
  the list; delete asks for confirmation. The list shows when each was last updated, most recent first.
- **Export and import inputs.** **Export inputs** / **Import inputs** on the form, and **Export** on each saved
  simulation, save and load a simulation's inputs as a file, for backup or another machine. An import fills the form
  without running anything; an invalid file changes nothing and lists every problem.

## Spending strategy fixes (Partha's Spending Strategy)

- When TDA and TBA can't pay a Spend Bucket refill, the Crash Buffer pays it, in any market, before the Tax Free
  account. A plan that ran out of equities in a rising market used to fail with its Crash Buffer full.
- In a down market, a refill the Crash Buffer can't cover (for example, when it's empty) now comes from TDA/TBA, then
  the Tax Free account. Plans used to fail with money left in those accounts. For the default plan under the 2008 Crash
  at 10% growth (300 Monte Carlo runs), success rises from 69.7% to 82.7%.

## Market data and presets

- **S&P 500 data to September 2026.** Monte Carlo now samples years 1928–2025 instead of 1928–2019. Fixed-market
  results are unchanged; Monte Carlo results shift slightly.
- **Preset rates are read-only.** Choosing a market preset fills inflation, growth and bond yield and greys those fields
  out; choose **Custom** to edit them. Before, editing one silently switched the form to Custom.

## Versioning

- **The version changes only in a release.** This release is the first under the new rule: v2.0.0 follows v1.1.29.
  Commits and builds no longer change the version.
- **Each build is identified** by SemVer build metadata from git, for example `2.0.0+3.gabc1234` (3 commits after the
  v2.0.0 tag), shown in the header next to the version.

---

## For developers

- **API:**
  - New: `GET /api/spending-strategies`; `PUT`, `PATCH`, `DELETE /api/simulations/{id}`; `POST /api/inputs/export` and
    `/api/inputs/import`.
  - `SavedSimulationSummary` gains `updatedAt`, `version`, `isCurrentVersion`; `SavedSimulation` gains the running
    server's `apiMetadata`; `ApiMetadata` gains `build`. Guardrails settings and results are added to the config,
    results and breakdown.
  - TypeScript contracts are regenerated. **Restart the API server** after upgrading.
- **Database:** on first start the API server adds a nullable `updated_at` column to `Simulations` in
  `retirement.db`; existing rows are unchanged. Back the database up first (`*.db.bak*` is git-ignored).
- **Releases:** `cd api-server && ./gradlew release` (patch; `-Pbump=minor` or `-Pbump=major`) replaces
  `incrementBuildNumber`. The runtime version file is generated under `api-server/build/`, so builds leave no changes.
- **Tests:** Kotlin `./gradlew test` (298), frontend `npm test` (178), backend `deno task test` (31), and Playwright
  end-to-end tests (`npm run e2e`), which CI now runs.
- **Specs:** 005 (saved simulations) and 006 (guardrails) implemented; 007 (historical return sequences) specified,
  not yet implemented.

## Known issues / follow-ups

- Retirement is assumed at the end of the year Retirement Age is reached. A retirement month is planned for more
  accurate projections.
- The Crash Buffer's value doesn't change with bond prices; its balance falls only through withdrawals.
- Spec 007 (replaying historical return sequences in interactive simulations) is specified but not implemented.
