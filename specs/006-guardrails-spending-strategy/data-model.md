# Data Model: Selectable Spending Strategy with Risk-Based Guardrails

**Feature**: [spec.md](./spec.md) | **Research**: [research.md](./research.md) | **Date**: 2026-10-19

All models are Kotlin `@Serializable` data classes in `api-server/src/main/kotlin/com/retirement/model/`. The
TypeScript types are generated from them (`./gradlew generateTsContract`); they are never edited by hand.

Units:
- Probabilities and rates are decimals in the API (0.90 = 90%) and whole-number percentages in the form. The
  conversion happens in `frontend/src/utils/formConfig.ts`.
- Amounts are dollars of the year shown, unless marked *today's dollars* (divided by cumulative inflation).

## Input models

### StrategyConfig (changed)

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `type` | String | `"PARTHA_V0_01_20250105"` | Existing field. Must be an id in the catalog (FR006-003, R006-6) |
| `guardrails` | GuardrailsSettings | `GuardrailsSettings()` | **New.** Kept when another strategy is selected (FR006-004) |
| *(existing fields)* | | | `tdaWithdrawalPercentage` and the Roth conversion amounts apply to every strategy (FR006-005) |

### GuardrailsSettings (new)

| Field | Type | Default | Validation (only when `type` is `RISK_BASED_GUARDRAILS_V1`) |
|-------|------|---------|------|
| `targetSuccess` | Double | 0.90 | Between 0.01 and 0.99, and `lowerGuardrail < targetSuccess < upperGuardrail` (FR006-010) |
| `upperGuardrail` | Double | 0.99 | Between 0.01 and 0.99 |
| `lowerGuardrail` | Double | 0.70 | Between 0.01 and 0.99 |
| `reviewFrequency` | String | `"YEARLY"` | `"YEARLY"` or `"QUARTERLY"` |

Validation error fields:
- `strategy.type`
- `strategy.guardrails.targetSuccess`
- `strategy.guardrails.upperGuardrail`
- `strategy.guardrails.lowerGuardrail`
- `strategy.guardrails.reviewFrequency`

When the ordering fails, the error goes on the field that breaks it: the lower guardrail if it is not below
the target, and the upper guardrail if it is not above it.

Fixed constants, not settings (`GuardrailsDefaults`):

| Constant | Value | Source |
|----------|-------|--------|
| `PATH_COUNT` | 500 | R006-3 |
| `INTERACTIVE_SEED` | 20261019 | R006-3 |
| `RAISE_CAP` | 1.5 × configured Wants | FR006-012 |
| `SUCCESS_TOLERANCE` | 0.005 | FR006-012 |
| `WANTS_TOLERANCE` | $100 | FR006-012 |

## Catalog

### SpendingStrategyInfo (new response model, `GET /api/spending-strategies`)

| Field | Type | Notes |
|-------|------|-------|
| `id` | String | Stable, versioned identifier (FR006-006) |
| `name` | String | Display name |
| `description` | String | One line (FR006-002) |
| `settings` | String | `"NONE"` or `"GUARDRAILS"`: which form fields the strategy uses |

Initial entries:

| id | name | settings |
|----|------|----------|
| `PARTHA_V0_01_20250105` | Partha's Spending Strategy-v0.01-20260105 | NONE |
| `RISK_BASED_GUARDRAILS_V1` | Risk-Based Guardrails | GUARDRAILS |

## Result models

### YearlyResult (changed)

| Field | Type | Notes |
|-------|------|-------|
| `guardrails` | YearGuardrails? = null | **New.** Set only for guardrails simulations in retirement years (FR006-018, FR006-022) |

### YearGuardrails (new)

| Field | Type | Notes |
|-------|------|-------|
| `configuredWants` | Double | The configured Wants with this year's inflation, for comparison with `cashFlow.wants` (the adjusted Wants) |
| `reviews` | List&lt;GuardrailReview&gt; | One per review in the year: 1 for yearly reviews, up to 4 for quarterly. Empty when the plan failed before the year's first review |

### GuardrailReview (new)

| Field | Type | Notes |
|-------|------|-------|
| `year`, `quarter`, `age` | Int | `quarter` is 1–4 |
| `portfolioValue` | Double | Total of all accounts at the review |
| `probabilityBefore` | Double | At the current Wants |
| `outcome` | GuardrailOutcome | See below |
| `wantsBefore`, `wantsAfter` | Double | Dollars of the year |
| `probabilityAfter` | Double | At `wantsAfter`. Equals `probabilityBefore` when the outcome is `NO_CHANGE` |
| `lowerGuardrailPortfolio` | Double? | Portfolio value at which the probability equals the lower guardrail. Null in Monte Carlo batch runs (R006-4) |
| `upperGuardrailPortfolio` | Double? | The same for the upper guardrail. Null when no portfolio value reaches it, or in Monte Carlo batch runs |
| `wantsAtLowerGuardrail`, `wantsAtUpperGuardrail` | Double? | The Wants that would follow crossing that guardrail (US006-3). Null as above |

### GuardrailOutcome (new enum; response only)

| Value | Meaning |
|-------|---------|
| `NO_CHANGE` | The probability was between the guardrails |
| `CUT` | Below the lower guardrail. Wants were lowered to the target |
| `RAISE` | Above the upper guardrail. Wants were raised to the target |
| `RAISE_CAPPED` | Above the upper guardrail, but the raise stopped at 150% of configured Wants before reaching the target. With no configured Wants the cap is 0, so the outcome is `NO_CHANGE` instead (analysis U006-2) |
| `TARGET_UNREACHABLE` | Below the lower guardrail, and even Wants of zero don't reach the target. Wants were set to 0 |

### Summary (changed)

| Field | Type | Notes |
|-------|------|-------|
| `guardrails` | GuardrailsSummary? = null | **New.** Set for guardrails simulations only (FR006-019) |

### GuardrailsSummary (new)

| Field | Type | Notes |
|-------|------|-------|
| `startingProbability` | Double | `probabilityBefore` of the first review |
| `startingLowerGuardrailPortfolio`, `startingUpperGuardrailPortfolio` | Double? | From the first review |
| `cuts`, `raises` | Int | `CUT` and `TARGET_UNREACHABLE` count as cuts; `RAISE` and `RAISE_CAPPED` count as raises |
| `largestCutPercent` | Double? | The largest (wantsBefore − wantsAfter) / annual spending before the cut, as a decimal. Null when there were no cuts |
| `lowestAnnualSpending`, `highestAnnualSpending` | Double | Annual spending (Needs + Wants + healthcare + property tax) in *today's dollars*, over retirement years (R006-8) |
| `configuredSpendingUnreachable` | Boolean | True when the first review's outcome was `CUT` or `TARGET_UNREACHABLE` (spec edge case: below the lower guardrail at retirement) |

### MonteCarloPath (changed)

| Field | Type | Notes |
|-------|------|-------|
| `hadSpendingCut` | Boolean? = null | **New.** Guardrails only. True when any review in the run was `CUT` or `TARGET_UNREACHABLE` |

### Statistics (changed)

| Field | Type | Notes |
|-------|------|-------|
| `spending` | SpendingStatistics? = null | **New.** Guardrails only (FR006-021) |

### SpendingStatistics (new)

| Field | Type | Notes |
|-------|------|-------|
| `percentile10Path`, `medianPath`, `percentile90Path` | List&lt;Double?&gt; | Annual spending in *today's dollars* per year, aligned with `MonteCarloResult.years`. Null for working years. Each year counts only the runs still going that year |
| `shareOfRunsWithCut` | Double | 0.0–1.0 |

### YearBreakdown (changed)

| Field | Type | Notes |
|-------|------|-------|
| `guardrails` | GuardrailsSection? = null | **New.** Set for guardrails simulations in retirement years (FR006-020) |

### GuardrailsSection (new)

| Field | Type | Notes |
|-------|------|-------|
| `targetSuccess`, `upperGuardrail`, `lowerGuardrail` | Double | The settings |
| `reviewFrequency` | String | |
| `reviews` | List&lt;GuardrailReview&gt; | The same objects as in `YearGuardrails` |
| `configuredWants`, `adjustedWants` | LineItem | The adjusted Wants is explained as a rule: "today's-dollar Wants × cumulative inflation" |
| `noReviewNote` | String? | Set when a quarterly-review year stops reviewing because the plan failed before a later quarter's review (US006-4 scenario 2). Null otherwise |

The accounts that paid are already in the Money Movement section, so this section doesn't repeat them.

## Internal (not serialized)

- **SpendingPolicy** (interface, `logic/`):
  - `wantsCutInDownMarket: Boolean`
  - `review(context): ReviewDecision?`, called at the review points of R006-5. Returns null when the
    strategy has no reviews.
- **ReviewContext**: balances, age, year, quarter, cumulative inflation, prior-year taxable income, current
  Wants in today's dollars, the config, the history, the review seed, and whether to compute dollar guardrails.
- **SuccessProjector** (`logic/`): `probability(context, wantsReal, balanceScale = 1.0): Double` over the
  review's `MarketPaths`, held in primitive `DoubleArray`s.
- **MarketDraw** (`logic/`): the per-year draw shared by Monte Carlo scenarios and review paths (R006-3).

## State over a retirement (guardrails)

```text
configured Wants ──first review (Q1 of the first retirement year)──► adjusted Wants (today's $)
      each later review:  P < lower → CUT / TARGET_UNREACHABLE
                          P > upper → RAISE / RAISE_CAPPED
                          otherwise → NO_CHANGE
      between reviews: the year's Wants = adjusted Wants (today's $) × cumulative inflation
      plan failure → reviews stop
```
