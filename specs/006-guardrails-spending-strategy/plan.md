# Implementation Plan: Selectable Spending Strategy with Risk-Based Guardrails

**Branch**: `feat/006-guardrails-spending-strategy` | **Date**: 2026-10-19 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/006-guardrails-spending-strategy/spec.md`

## Summary

The user picks the retirement spending strategy for each simulation. The list starts with the existing
"Partha's Spending Strategy-v0.01-20260105" and adds **Risk-Based Guardrails**:

- At each review (yearly or quarterly), the plan's probability of success is measured over 500 seeded market
  paths.
- When it crosses the lower (70%) or upper (99%) guardrail, Wants are reset to the level that brings it back
  to the target (90%).
- The existing bucket rules still decide which accounts pay.

Approach (research R006-1 to R006-10):

- **Engine seam**: a `SpendingPolicy` decides each period's Wants, and the existing `SpendingStrategy`
  account rules stay as they are. A `SpendingStrategyCatalog` maps `strategy.type` to a policy, a name and a
  description.
- **Probability of success**: a lean yearly `SuccessProjector` treats the portfolio as one pool, with the
  same cash flows as the engine. Using the real engine for every path would be too slow (R006-2). The
  projector is checked against the engine.
- **Solves**: bisection on Wants for adjustments, and on a portfolio scale factor for the dollar guardrails.
  Dollar guardrails are skipped in Monte Carlo batches.
- **Seeds**: review paths come from the shared `MarketDraw` bootstrap. Their seeds derive from the run's seed,
  or a fixed interactive seed (R006-3).
- **Safety net**: a golden-results test, written first, guarantees existing-strategy results don't change
  (R006-9).

## Technical Context

**Language/Version**: Kotlin 2.3 on JDK 25 (API server); TypeScript 5 strict (BFF on Deno 2, frontend on React 18)
**Primary Dependencies**: Ktor, kotlinx.serialization, kotlinx.coroutines (API server); Oak 17 (BFF); MUI 7,
Recharts 3 (frontend). No new dependencies.
**Storage**: SQLite through the existing `SimulationRepository`. Configs and results are JSON with
`ignoreUnknownKeys` and defaults, so the new fields need no migration (R006-6).
**Testing**: JUnit 5 through `kotlin-test-junit5` (`./gradlew test`); `deno task test`; Vitest + Testing Library
**Target Platform**: Local web app (browser + three local services)
**Project Type**: Web application, three tiers: `frontend/` → `backend/` (BFF) → `api-server/`
**Performance Goals**: An interactive 30-year guardrails run in under 3 s. A 1,000-run Monte Carlo in under
60 s (SC006-005). Estimates are about 0.5 s and about 3–4 s on 8 cores (R006-2). The existing strategy's speed
is unchanged.
**Constraints**:
- Existing-strategy results identical (FR006-007).
- Deterministic (constitution IV).
- Generated contracts only (constitution III).
- Invalid input returns a JSON 422 with per-field errors, so request fields use `String`, not enums (R006-6).
- Rates and probabilities are decimals in the API and whole-number percentages in the form.
**Scale/Scope**:
- 2 strategies, 4 settings, 1 new endpoint, and optional fields added to 6 response models.
- About 30 reviews × 500 paths × up to about 56 projector evaluations per interactive run.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Status |
|-----------|-------|--------|
| I. Test-first | Tests are written first: the golden-results test (before any engine change), projector-versus-engine consistency, policy outcomes, and the simulation scenarios in quickstart §2. Each task writes its failing test before the code, and stubs any required field until its story (lesson from spec 004's Complexity Tracking). `SpendingPolicy`, `SuccessProjector` and `RiskBasedGuardrailsPolicy` are domain logic. Every branch and outcome has a JUnit 5 test that would fail without it (constitution 1.2, Testing Strategy; analysis C006-1) | Pass |
| II. Strict tiers | Probability, solves, reviews, validation and the strategy catalog are all in the API server. The BFF only proxies the new `GET /api/spending-strategies`. The frontend renders the catalog and results and converts units in `formConfig.ts` only | Pass |
| III. Explicit contracts | New `@Serializable` models. `SpendingStrategyInfo` is added to the `TsContractGenerator` roots, and the TypeScript is regenerated for the BFF and frontend. `api.yaml` (spec 002) is updated | Pass |
| IV. Deterministic | Review paths are seeded from the run seed (or the fixed `INTERACTIVE_SEED`), the year and the quarter. Paths are shared within a review. The history list is read-only and passed in. No wall-clock time is used. Monte Carlo runs stay independent across coroutines. Tested: repeated runs are identical, and results with and without dollar guardrails are identical | Pass |

**Post-design re-check (after Phase 1)**: Pass.
- The design adds no tier crossing, no new storage, no dependency, and no shared mutable state.
- The one approximation, the yearly pooled projector in place of the full engine for the probability of
  success, is a modelling decision. It is recorded in the spec (FR006-011a) and research R006-2 and bounded by
  a consistency test. It is not a constitution violation.

## Project Structure

### Documentation (this feature)

```text
specs/006-guardrails-spending-strategy/
├── spec.md
├── plan.md                 # This file
├── research.md             # Phase 0
├── data-model.md           # Phase 1
├── quickstart.md           # Phase 1
├── contracts/
│   └── strategy-api.md     # Phase 1
├── checklists/
│   └── requirements.md
└── tasks.md                # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
api-server/src/main/kotlin/com/retirement/
├── logic/
│   ├── SimulationEngine.kt          # asks the SpendingPolicy for Wants at review points; per-quarter Wants for
│   │                                #   quarterly reviews; records reviews; takes a guardrail seed and the history
│   ├── SpendingStrategy.kt          # executeQuarterly gains wantsCutInDownMarket (default true)
│   ├── SpendingPolicy.kt            # NEW: SpendingPolicy, FixedWantsPolicy, ReviewContext, ReviewDecision
│   ├── SpendingStrategyCatalog.kt   # NEW: id → policy, name, description, settings kind
│   ├── RiskBasedGuardrailsPolicy.kt # NEW: reviews, bisection solves, dollar guardrails
│   ├── SuccessProjector.kt          # NEW: lean yearly pooled projection over MarketPaths
│   ├── MarketDraw.kt                # NEW: the per-year bootstrap draw, shared with MonteCarloEngine
│   ├── MonteCarloEngine.kt          # uses MarketDraw; passes the run seed; spending statistics; hadSpendingCut
│   ├── BreakdownGenerator.kt        # GuardrailsSection
│   └── ConfigValidator.kt           # strategy.type, guardrail bounds and ordering, reviewFrequency
├── model/
│   ├── SimulationConfig.kt          # StrategyConfig.guardrails, GuardrailsSettings
│   ├── SimulationResult.kt          # YearGuardrails, GuardrailReview, GuardrailOutcome, GuardrailsSummary,
│   │                                #   SpendingStatistics, MonteCarloPath.hadSpendingCut
│   ├── SpendingStrategyInfo.kt      # NEW
│   └── YearBreakdown.kt             # GuardrailsSection
└── api/SimulationRoutes.kt          # GET /api/spending-strategies

api-server/src/test/
├── kotlin/com/retirement/logic/
│   ├── ExistingStrategyGoldenTest.kt      # NEW, first
│   ├── MarketDrawTest.kt                  # NEW
│   ├── SuccessProjectorTest.kt            # NEW
│   ├── RiskBasedGuardrailsPolicyTest.kt   # NEW
│   ├── GuardrailsSimulationTest.kt        # NEW
│   ├── GuardrailsPerformanceTest.kt       # NEW, @Tag("perf"), runs only with -Pperf
│   ├── MonteCarloEngineTest.kt, BreakdownGuardrailsTest.kt (NEW), ConfigValidatorTest.kt, SpendingStrategyTest.kt
├── kotlin/com/retirement/api/       # SimulationRoutesTest, PersistenceAndMonteCarloRoutesTest
├── kotlin/com/retirement/contract/TsContractGenerator.kt   # root SpendingStrategyInfo
└── resources/golden/006/            # NEW: snapshots of existing-strategy results

backend/src/
├── routes.ts                        # proxy GET /api/spending-strategies
├── contract.generated.ts            # regenerated
└── (tests)                          # proxy test

frontend/src/
├── components/SimulationForm.tsx    # strategy select + description; guardrail fields
├── components/GuardrailsSummary.tsx # NEW: tiles, spending vs configured chart, probability by review chart
├── components/ResultsTable.tsx      # adjusted vs configured Wants, review outcome (guardrails only)
├── components/MonteCarloResults.tsx # spending percentiles, share of runs with a cut
├── components/breakdown/GuardrailsSection.tsx  # NEW
├── components/BreakdownDialog.tsx   # renders GuardrailsSection
├── pages/SimulationPage.tsx         # loads the strategy catalog; shows GuardrailsSummary
├── services/                        # fetch the strategy catalog
├── utils/formConfig.ts              # percent ⇄ decimal for guardrails; defaults for old configs
├── types/contract.generated.ts      # regenerated
└── **/*.test.tsx                    # form, summary, table, Monte Carlo, breakdown tests

specs/002-retirement-cashflow/contracts/api.yaml   # new endpoint and fields
specs/002-retirement-cashflow/spec.md              # FR002-002 cross-reference to spec 006
README.md                                          # strategies and how to choose one
```

**Structure Decision**: The existing three-tier web layout. New Kotlin files sit next to `SpendingStrategy.kt`
in `logic/`. Guardrails UI gets its own components (`GuardrailsSummary.tsx`, `breakdown/GuardrailsSection.tsx`),
so the existing components only gain a conditional render.

## Phasing (for /speckit-tasks)

Each user story can be released on its own after the foundation:

1. **Setup and safety net**
   - Branch `feat/006-guardrails-spending-strategy`.
   - `ExistingStrategyGoldenTest` and snapshots, generated on unchanged code.
   - `MarketDraw` extraction with its scenario-equality test.
2. **Foundation**
   - `SpendingPolicy`, with `FixedWantsPolicy` wired into the engine (goldens still pass).
   - The `SpendingStrategyCatalog`.
   - The `wantsCutInDownMarket` flag.
   - `StrategyConfig.guardrails` model, validation and contract regeneration.
   - `GET /api/spending-strategies` with its BFF proxy.
3. **US006-1 (P1) Choose a strategy**
   - Form select, guardrail fields and unit conversion.
   - Settings kept across switches; save and reload.
   - Shippable alone: guardrails can be selected but runs as fixed Wants until US006-2. Otherwise
     US006-1 and US006-2 ship together in one PR.
4. **US006-2 (P1) Simulate with guardrails**
   - `SuccessProjector` and its consistency test.
   - `RiskBasedGuardrailsPolicy` with yearly reviews, solves and outcomes.
   - Engine review recording and `GuardrailsSummary`.
   - Monte Carlo seeds, `hadSpendingCut` and `SpendingStatistics`.
   - Quarterly reviews (per-quarter Wants).
   - Results UI: summary panel, charts, table columns, Monte Carlo tiles.
   - Performance check (quickstart §4).
5. **US006-3 (P2) Dollar guardrails**
   - Portfolio and Wants solves at each guardrail; skipped in Monte Carlo batches, and a test that results
     are identical either way.
   - Dollar guardrails shown in the summary and in each review.
6. **US006-4 (P3) Breakdown**
   - `GuardrailsSection` in `YearBreakdown`, interactive and Monte Carlo run.
   - `breakdown/GuardrailsSection.tsx`.
7. **Polish**
   - `api.yaml`, README, and the spec 002 cross-reference.
   - Full suites in all tiers, and the end-to-end check (quickstart §5).
   - Revert `version.properties`.

**Recommendation**: ship US006-1, US006-2 and US006-3 together. Without US006-2, a user could select a strategy
that doesn't yet behave as described. Without US006-3, the reviews would leave out the dollar guardrails that
FR006-018 lists (analysis A006-1). US006-4 can follow.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| Constitution I (Test-First): T006-046's test and T006-047's dollar-guardrails sentence were written in the same step. | Not needed: a slip while finishing US006-3. | Not justified. The tests cover the behaviour (both guardrails, one guardrail, none), so no code change is needed. Every other task wrote its test first and saw it fail. |

The `SuccessProjector` duplicates the cash-flow formulas of `SimulationEngine` (expenses, Social Security,
tax), in a lean form. That is a maintenance cost, not a violation.

- **Mitigation**: the projector calls the same helpers wherever they exist, such as the Social Security
  step-up and the loan payment (`quarterlyPaymentFor`). Where it can't, a consistency test against the engine
  fails if the two drift (R006-2).
- **Rejected alternative**: running the real engine per path costs over 1,000 times more (R006-2 table).
