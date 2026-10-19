# Research: Selectable Spending Strategy with Risk-Based Guardrails

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-10-19

Each decision lists what was chosen, why, and what else was considered.

## R006-1: Where the strategy plugs into the engine

**Decision**: Split the strategy into two parts.

1. **Spending policy**: decides the year's (or quarter's) Wants. This is new. It is an interface,
   `SpendingPolicy`, with two implementations:
   - `FixedWantsPolicy`, today's behaviour: configured Wants with inflation.
   - `RiskBasedGuardrailsPolicy`.
2. **Account rules**: decide which accounts pay. This is the existing `SpendingStrategy.executeQuarterly` and
   `coverShortfall`, unchanged except for one flag.

`SimulationEngine` asks the policy for Wants at each review point. It then runs the same quarter loop as
today. The policy also says whether the 10% Wants cut in a down market applies: true for
"Partha's Spending Strategy-v0.01-20260105", false for guardrails (FR006-015).

`SpendingStrategyCatalog` maps `strategy.type` to the policy and holds each strategy's display name and
description (FR006-002). A future strategy adds one catalog entry and one policy. If it needs different
account rules, it adds an account-rules implementation behind the same seam.

**Rationale**:
- Q3 kept the bucket rules for guardrails, so the only behaviour that differs between strategies is how
  much is spent.
- Keeping `SpendingStrategy` untouched for the existing strategy makes FR006-007 (no change in its
  results) easy to show.
- `SimulationEngine` already computes `wantsAdjusted` in one place (line 209), so the policy replaces a
  single expression.

**Alternatives considered**:
- *A full `SpendingStrategy` interface now, with the account rules per strategy*: over-built. Both strategies
  share the account rules, so the extra layer would have one implementation.
- *`if (type == …)` branches inside `SimulationEngine`*: works for two strategies but doesn't leave room for
  the strategies the user plans to add.

## R006-2: How the probability of success is computed at a review

**Decision**: A lean yearly projector, `SuccessProjector`, separate from `SimulationEngine`.

From a review's state, it projects each of the review's market paths year by year to the end age. The state
is: balances, age, cumulative inflation, the prior year's taxable income, and the current Wants in today's
dollars. Each year the projector:

- Grows Needs, Wants, healthcare (by age bracket) and property tax with the path's inflation.
- Adds Social Security (with the spousal step-up), Roth conversions (only for their tax),
  one-time cash expenses and loan payments (loan payments are fixed in nominal dollars).
- Charges income tax as the flat rate on the previous year's taxable income, computed the same way as the
  engine. Equity withdrawals are taxed by the TDA withdrawal percentage, with 50% of TBA withdrawals taxable.
- Treats the portfolio as **one pool**. The pool keeps the review-time shares of cash (SB), bonds (CBB) and
  equities (TBA + TDA + TFA). It earns the HYSA rate, the path's bond yield and the path's equity return in
  those shares.
- Counts a path as failed if the pool falls below zero before the end age.

It uses primitive arrays and no allocation per year. The spec's FR006-011a was updated to describe this.

**Rationale**: cost. Running the real engine from each review is about 500 paths × the remaining
years × 4 quarters. Its per-quarter allocations and result lists make each path cost roughly 1 ms. The
estimates below assume about 30 reviews, roughly 12 bisection steps for each solve, and 30 years per path.

| Approach | Interactive run | 1,000-run Monte Carlo |
|----------|-----------------|-----------------------|
| Real engine, reviews and solves | ≈ 30 × 50 evaluations × 500 paths × 1 ms ≈ 12 minutes | hours |
| `SuccessProjector`, about 20 ns per path-year | ≈ 30 × 56 evaluations × 500 × 30 × 20 ns ≈ 0.5 s | ≈ 1,000 × 30 × 3 × 500 × 30 × 20 ns ≈ 27 s of CPU, about 3–4 s on 8 cores |

Only the projector meets SC006-005: under 3 seconds interactive and under 60 seconds for Monte Carlo.

What the projector leaves out:
- **Bucket timing.** The engine fails only once every account is used up, with the TFA as the last resort
  (FR002-005), so "the pool runs out" is close.
- **The CBB's down-market role.** It reduces sequence risk a little, so the projector is slightly
  conservative.

**Check** *(revised in implementation)*: the planned check compared the projector with the engine at the
configured rates, with no volatility. That check couldn't work at first. In a market that never falls, the
existing strategy kept moving equities into the Crash Buffer and never drew on it, so the engine failed with the
CBB full: a low-balance plan failed in 2039 with $414k still in the CBB, nine years before the pooled projection.
That was fixed on 2026-10-19 (FR002-005, T002-078): the CBB now pays what the equities can't, in any market, and
the same plan fails in 2044, against 2048 for the projection. The remaining gap comes from the strategy still
moving equities into lower-yielding bonds as the CBB refills, which a pool with fixed shares doesn't model.

The test instead compares the projector with a 500-run Monte Carlo of the engine, from the same bootstrap and
starting from the review state:

| Engine Monte Carlo success | Measured projector − engine (before → after the CBB fix) |
|----------------------------|-----------------------------|
| 40% and up | −0.4 to +8 points → −0.6 to +4 points |
| Below 40% | up to +15 points → up to +11 points |

So the projector reads slightly optimistic, by up to about 4 points around the guardrail band. Cuts can
therefore come a little later than a full-engine estimate would suggest. The test asserts agreement within 10 points wherever
the engine succeeds at least half the time, and never more than 20 points high. At static rates, the projection
must also succeed wherever the engine does.

**Alternatives considered**:
- *The real engine with a "start from state" entry point*: exact, but far too slow (table above). It would
  also need a large refactor of the 830-line `runSimulation`.
- *A quarterly projector with bucket rules*: about 4–10 times the cost and duplicates `SpendingStrategy`. That
  is a second copy of the rules to keep in sync.
- *A closed-form estimate*: rejected in Q1.

## R006-3: Market paths at a review and their seeds

**Decision**:
- **Paths**: 500 paths per review (`GuardrailsDefaults.PATH_COUNT`). Each path draws a historical year per
  projected year, using its S&P 500 return and inflation together. The draw is re-centred on the configured
  post-retirement growth and inflation, the same bootstrap as `MonteCarloEngine.generateScenario`. Bond
  yields vary normally around the configured yield. If historical data is missing, returns and inflation are
  normally distributed, as in the Monte Carlo fallback.
- **Shared code**: the year-draw code moves out of `MonteCarloEngine` into a shared `MarketDraw` helper.
  Monte Carlo scenarios don't change, because the same code consumes the random stream in the same order.
  A test compares scenarios before and after the move.
- **Review seed**: `reviewSeed = mix(simulationSeed, year, quarter)`, using the same spreading constant
  as `MonteCarloEngine.runSeed`.
  - In a Monte Carlo run, `simulationSeed` is the run's seed, `runSeed(seed, runIndex)`.
  - In an interactive run it is the constant `GuardrailsDefaults.INTERACTIVE_SEED = 20261019`.
- **Common random numbers**: one review uses the same 500 paths for every step of its solves. Every path
  is monotonic in Wants and in the portfolio value, so the overall probability is too, and bisection
  converges.
- **History**: loaded once per simulation (or once per Monte Carlo analysis) and passed in, not reloaded at
  every review. `getHistoricalYears()` rebuilds its list on every call.

**Rationale**:
- Constitution IV: the same config and seed always give the same reviews.
- Q1 asked for paths built from the same historical data as Monte Carlo.
- With 500 paths each path is 0.2 percentage points, fine enough for the 0.5-point accuracy in FR006-012.

**Alternatives considered**:
- *One path set for the whole simulation, shifted by year*: slightly cheaper, but it makes reviews in
  neighbouring years correlated in an unexplained way. Generation is not the cost driver: 500 × 40 draws
  is about 0.2 ms.
- *Using the outer Monte Carlo run's own future returns*: that would be look-ahead bias.

## R006-4: Solving for the target, and the dollar guardrails

**Decision**: Bisection over the shared paths of a review.

- **Spending adjustment (FR006-012)**: bisection on Wants in today's dollars, between 0 and the raise cap
  (150% of configured Wants). It stops when the probability is within 0.5 percentage point of the target, or
  the Wants interval is under $100. At most about 14 steps.
  - Probability still below the target at Wants = 0: the outcome is `TARGET_UNREACHABLE`.
  - Probability still above the target at the cap: the outcome is `RAISE_CAPPED`.
- **Dollar guardrails (US006-3)**: bisection on a factor that scales all account balances together. For each
  guardrail it finds the portfolio value at which the probability equals that guardrail. It then solves the
  Wants that would follow that crossing.
- **Monte Carlo**: the dollar guardrails are skipped in the 1,000-run batch, because no Monte Carlo output
  uses them (FR006-021). They are computed for interactive runs and when one Monte Carlo run is rebuilt
  (details and breakdown). They never feed back into the simulation, so results with and without them are
  identical. A test checks this.

**Alternatives considered**:
- *A per-path "maximum sustainable Wants" with a quantile*: the same cost (one bisection per path) and harder
  to explain in the breakdown.
- *A secant or Newton method*: the probability is a step function over 500 paths, so derivative-based methods
  are unreliable.

## R006-5: Review timing and Wants in the engine

**Decision**:
- **First review**: Q1 of the first retirement year (age `retirementAge + 1`), before that quarter's
  money moves. The retirement-age year is still a working year, as today.
- **Yearly reviews**: at each later Q1.
- **Quarterly reviews**: at the start of every retirement quarter.
- **Wants between reviews**: the engine keeps the adjusted Wants in today's dollars. Each period's Wants is
  that value × the cumulative inflation (FR006-013).
- **Mid-year changes**: today the engine fixes `wantsAdjusted`, `grossExpenses`, `estimatedAig` and
  `yearCapAig` once per year. With quarterly reviews they become per-quarter values, recomputed when a review
  changes Wants. The year's totals are the sum of its quarters, as they already are for the quarterly results.
- **Phasing**: yearly reviews come first, because they need only the year-start expression. Quarterly reviews
  follow in the same story (see plan, Phasing).
- **CBB cap**: stays as today. It is computed from the configured expenses at the start of the simulation, not
  from the adjusted Wants. The cap isn't derived from the year's income gap, so FR006-015 doesn't apply to it,
  and a stable crash buffer is its purpose.
- **SB cap**: its "50% of Wants" input uses the adjusted Wants automatically, because `yearCapAig` is built
  from `wantsAdjusted`.

## R006-6: How strategy settings travel and are validated

**Decision**:
- **Selection**: `StrategyConfig.type` stays a `String`. Its existing value `PARTHA_V0_01_20250105` is
  kept as the existing strategy's identifier, even though the display name says 20260105: identifiers must be
  stable (FR006-006). The new strategy's identifier is `RISK_BASED_GUARDRAILS_V1`.
- **Settings**: a new `StrategyConfig.guardrails: GuardrailsSettings` with defaults. It is kept in the config
  when another strategy is selected (FR006-004).
- **Review frequency**: `reviewFrequency` is a `String`, `"YEARLY"` or `"QUARTERLY"`.
- **Validation**: `ConfigValidator` rejects an unknown `type` or `reviewFrequency`, and the guardrail bounds
  (FR006-010), with a 422 and per-field errors. It checks the guardrail settings only when guardrails is
  selected, so unused settings never block a run.

**Rationale**:
- The API server has no `StatusPages` handler, so a request enum that fails to parse would return Ktor's
  default 400, not the JSON 422 that CLAUDE.md requires.
- Saved configs are JSON with `ignoreUnknownKeys` and defaults. Old saves load as the existing strategy with
  default guardrail settings, with no migration.

**Alternatives considered**:
- *Kotlin enums in the request*: better TypeScript types, but they break the JSON 422 rule for bad input.
- *A sealed class of per-strategy settings*: switching strategies would drop the other strategy's settings,
  which breaks FR006-004.

## R006-7: The strategy list for the form

**Decision**: a new `GET /api/spending-strategies` returns the catalog: `id`, `name`, `description`,
and `settings` (`"NONE"` or `"GUARDRAILS"`, which form fields to show). It is served by the API server and
proxied by the BFF, like `/api/market-presets`. If the request fails, the form falls back to the existing
strategy only.

**Rationale**: constitution II. Strategy definitions are business content and live in the API server. The
frontend already follows this pattern for market presets.

## R006-8: What results report, and in which dollars

**Decision**:
- **Annual spending**: in summaries and Monte Carlo statistics it means Needs + Wants + healthcare + property
  tax, in today's dollars (divided by cumulative inflation). Tax and one-time expenses are excluded, because
  they are never adjusted (FR006-014).
- **Monte Carlo percentiles**: Monte Carlo inflation differs by run, so only today's dollars make runs
  comparable. The percentiles are per year, aligned with `years`. A failed run doesn't count for the years
  after it failed.
- **Results tables**: per-year rows show the year's dollars, as today.
- **Fields for guardrails only**: guardrail fields are optional (`null` or absent) in every result model, and
  set only when guardrails is selected (FR006-022). Results saved before this feature deserialize unchanged.
- **Monte Carlo statistics**: the spending percentiles and the share of runs with a cut are computed on the
  server and returned in `Statistics.spending`. Each path carries only `hadSpendingCut`, so the response grows
  by one boolean per run, not a list per run.

## R006-9: Protecting existing results (SC006-001, FR006-007)

**Decision**: before any engine change, add a golden-results test.

- It runs the existing strategy for every market preset, plus the one-time-expense scenario config, and
  compares against JSON snapshots committed under `api-server/src/test/resources/golden/006/`.
- It also runs a fixed-seed Monte Carlo of 50 runs.
- Snapshots are generated once from `main` before the change. Every field in a snapshot must match, numbers
  to within 1e-9. A field the snapshot lacks is allowed only if it is `null` or its default: the server's JSON
  settings write those out, for example `guardrails: null` (analysis I006-1).
- The test stays permanently, so later strategies can't change these results either.

**Rationale**: the strongest evidence for SC006-001, and test-first (constitution I).

## R006-10: Performance check

**Decision**: quickstart §4 times an interactive guardrails run and a 1,000-run Monte Carlo on the
development machine against SC006-005. A test, `GuardrailsPerformanceTest`, asserts generous bounds: 3 s interactive,
60 s Monte Carlo. It is tagged `@Tag("perf")` (JUnit 5). `tasks.test` in `api-server/build.gradle.kts` excludes
that tag unless the `perf` Gradle property is set, so it doesn't run in a plain `./gradlew test` and CI timing
noise can't fail a build. Run it with `./gradlew test -Pperf --tests '*GuardrailsPerformanceTest*'`.

If the Monte Carlo bound is missed, the fallback is the one the spec allows: make the path count a setting,
for example 200 paths for Monte Carlo. The first fix is profiling the projector loop.

**Measured** (12 processors, 2026-10-19): a 30-year interactive run takes 78 ms with dollar guardrails and 10 ms
without, and a 1,000-run Monte Carlo takes 1.7 s. Both are far inside SC006-005, so no fallback is needed. The
tag is excluded in `tasks.test` unless `-Pperf` is set.
