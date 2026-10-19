# Contract: Spending Strategies

**Feature**: [spec.md](../spec.md) | **Models**: [data-model.md](../data-model.md)

The API server serves these routes. The BFF proxies them unchanged under the same paths. The types are
generated from the Kotlin models, and `specs/002-retirement-cashflow/contracts/api.yaml` is updated to match.

## New: `GET /api/spending-strategies`

Returns the catalog of strategies the form can offer (FR006-001, FR006-002).

**200**: `SpendingStrategyInfo[]`

```json
[
  {
    "id": "PARTHA_V0_01_20250105",
    "name": "Partha's Spending Strategy-v0.01-20260105",
    "description": "Fixed spending; a Spend Bucket and Crash Buffer decide which accounts pay, cutting Wants 10% in down markets.",
    "settings": "NONE"
  },
  {
    "id": "RISK_BASED_GUARDRAILS_V1",
    "name": "Risk-Based Guardrails",
    "description": "Adjusts Wants when the plan's probability of success crosses a guardrail, back to a target.",
    "settings": "GUARDRAILS"
  }
]
```

The first entry is the default. The descriptions here are drafts, and the catalog in `SpendingStrategyCatalog`
holds the final text.

## Changed request: `SimulationConfig.strategy`

Every endpoint that takes a `SimulationConfig` accepts the new fields:
- `POST /api/simulate` and `/api/simulate/breakdown`
- the Monte Carlo endpoints
- save and load

```json
"strategy": {
  "tdaWithdrawalPercentage": 0.5,
  "rothConversionPreRetirement": 0,
  "rothConversionPostRetirement": 0,
  "type": "RISK_BASED_GUARDRAILS_V1",
  "guardrails": {
    "targetSuccess": 0.90,
    "upperGuardrail": 0.99,
    "lowerGuardrail": 0.70,
    "reviewFrequency": "YEARLY"
  }
}
```

- When `type` is missing, the request uses `PARTHA_V0_01_20250105`.
- When `guardrails` is missing, the request uses the defaults above.
- Configs saved before this feature load unchanged (FR006-003).

**422** (existing shape, `ValidationErrorResponse`), new errors:

| field | when |
|-------|------|
| `strategy.type` | Not an id in the catalog |
| `strategy.guardrails.targetSuccess` / `upperGuardrail` / `lowerGuardrail` | Outside 0.01–0.99, or ordering broken (lower < target < upper). Checked only when guardrails is selected |
| `strategy.guardrails.reviewFrequency` | Not `YEARLY` or `QUARTERLY` |

```json
{
  "error": "Validation failed",
  "statusCode": 422,
  "validationErrors": [
    { "field": "strategy.guardrails.lowerGuardrail", "message": "Lower guardrail (90%) must be below the target (90%)" }
  ]
}
```

## Changed responses (new fields optional; absent for the existing strategy)

| Response | New field | Model |
|----------|-----------|-------|
| `SimulationResult.yearlyResults[]` | `guardrails` | `YearGuardrails` |
| `SimulationResult.summary` | `guardrails` | `GuardrailsSummary` |
| `MonteCarloResult.paths[]` | `hadSpendingCut` | Boolean |
| `MonteCarloResult.statistics` | `spending` | `SpendingStatistics` |
| `YearBreakdown` (interactive and Monte Carlo run) | `guardrails` | `GuardrailsSection` |

Example `yearlyResults[i].guardrails` for a cut:

```json
{
  "configuredWants": 41200.0,
  "annualSpendingTodaysDollars": 81300.0,
  "reviews": [{
    "year": 2032, "quarter": 1, "age": 66,
    "portfolioValue": 1480000.0,
    "probabilityBefore": 0.64, "outcome": "CUT",
    "wantsBefore": 41200.0, "wantsAfter": 27850.0, "spendingBefore": 112400.0, "probabilityAfter": 0.902,
    "lowerGuardrailPortfolio": 1610000.0, "upperGuardrailPortfolio": 2450000.0,
    "wantsAtLowerGuardrail": 31100.0, "wantsAtUpperGuardrail": 52900.0
  }]
}
```

The dollar-guardrail fields (`lowerGuardrailPortfolio`, `wantsAtLowerGuardrail`, …) are `null` in the paths
of a Monte Carlo batch (R006-4). They are filled when a single run is rebuilt with
`POST /api/simulate/monte-carlo/run` or with its breakdown.

## Compatibility

- Requests from before this feature give the same values in every existing field (FR006-007, SC006-001). The
  golden test, research R006-9, checks this.
- They are not byte-identical. `specs/002-retirement-cashflow/contracts/api.yaml` lists every new field. The server's JSON settings write out default values, so responses also contain
  the new fields: `"guardrails": null` in results and summaries, and the default `strategy.guardrails` object
  in the config echo (analysis I006-1). Saved simulations are unaffected, because `SimulationRepository` leaves
  defaults out.
- The only additions to responses are optional fields. Older clients ignore them.
- `ContractTypesTest` must pass after regenerating:
  - `frontend/src/types/contract.generated.ts`
  - `backend/src/contract.generated.ts`
- New roots in `TsContractGenerator.kt`: `SpendingStrategyInfo` (response). `GuardrailsSettings`,
  `YearGuardrails` and the rest are reached through existing roots.
