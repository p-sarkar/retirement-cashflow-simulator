# Contract: `POST /api/simulate/breakdown`

Served by the API server and proxied unchanged by the BFF. The OpenAPI definition in
`specs/002-retirement-cashflow/contracts/api.yaml` is updated to match: its `ComputationBreakdown` schema is
replaced by `YearBreakdown`. Types are generated from the Kotlin models (`./gradlew generateTsContract`); field
definitions are in [data-model.md](../data-model.md).

## Request (unchanged)

```json
{ "config": { "...": "SimulationConfig" }, "targetAge": 57 }
```

`targetAge` may be the start year's age (returns `phase: "START"`) or any simulated age.

## Responses

| Status | When | Body |
|--------|------|------|
| 200 | Breakdown built | `YearBreakdown` |
| 400 | Malformed JSON, or `targetAge` outside the simulation | `ErrorResponse` |
| 422 | Config fails validation | `ValidationErrorResponse` (per-field errors) |
| 503 | API server unreachable (from the BFF) | `ErrorResponse` |

Unchanged from today except the 200 body.

## 200 body: example (retirement year, abbreviated)

```json
{
  "year": 2032, "age": 57, "spouseAge": 55, "phase": "RETIREMENT", "isLastYear": false,
  "startingPosition": {
    "openingBalances": { "sb": 160768, "cbb": 513444, "tba": 1718163, "tda": 2342188, "tfa": 721490 },
    "openingTotal": 5456054,
    "conditions": {
      "inflationRate": 0.03, "cumulativeInflation": 1.194, "equityReturn": 0.08,
      "equityReturnSource": "POST_RETIREMENT_RATE", "presetName": null, "presetSequenceYear": null,
      "bondYield": 0.05, "hysaRate": 0.04, "incomeTaxRate": 0.12, "marketVsAllTimeHigh": 1.0
    },
    "household": { "healthcareBracket": "POST_RETIREMENT_PRE_MEDICARE", "earners": [] }
  },
  "moneyMovement": {
    "ledgers": [
      {
        "account": "SB",
        "opening": [160768, 0, 0, 0],
        "rows": [
          { "type": "INTEREST", "counterparty": "BANK", "amounts": [1600, 0, 0, 0], "yearTotal": 4503 },
          { "type": "REGULAR_EXPENSES", "counterparty": "EXPENSES", "amounts": [-68000, 0, 0, 0], "yearTotal": -272000 }
        ],
        "closing": [0, 0, 0, 68195]
      }
    ],
    "flows": [ { "from": "TDA", "to": "SB", "amount": 156074 } ],
    "quarterStrategy": [
      {
        "quarter": 1, "rule": "MARKET_UP_CBB_REFILL", "marketVs12MonthsPrior": 1.08, "marketVsAllTimeHigh": 1.0,
        "refillToSb": 30000, "refillToCbb": 30000, "limitedBy": "QUARTERLY_SHARE",
        "explanation": "Market up and CBB below its cap: refill the SB and the CBB from TDA/TBA"
      }
    ],
    "failure": null
  },
  "expenses": { "...": "ExpenseSection" },
  "income": { "...": "IncomeSection" },
  "incomeGap": { "...": "IncomeGapSection" },
  "taxes": { "...": "TaxSection" },
  "nextYear": { "...": "NextYearSection" },
  "apiMetadata": { "version": "1.1.28", "buildTime": "…", "serverStartTime": "…" }
}
```

Numbers are illustrative, and zeros stand in for the quarters left out.

## Guarantees (tested)

- Ledgers reconcile: `closing[q] = opening[q] + Σ rows[*].amounts[q]`, `opening[q+1] = closing[q]`, and
  `closing[3]` equals the results-table year-end balance (FR-050, SC-001).
- `flows` sum to the ledgers' year totals (FR-055).
- Every amount that also appears in the results row equals it (FR-002, SC-002).
- `nextYear` items with `exact = true` equal the next year's actual values (FR-071, SC-003).
- Building a breakdown doesn't change the simulation's results (FR-006).
