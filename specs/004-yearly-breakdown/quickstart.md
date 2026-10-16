# Quickstart: Validate the Yearly Computation Breakdown

How to check the feature end to end. Field definitions: [data-model.md](./data-model.md). Endpoint:
[contracts/breakdown-api.md](./contracts/breakdown-api.md).

## Prerequisites

The three services running (see README): frontend on 5173, BFF on 8000, API server on 8090. Restart the API server
after building the feature (CLAUDE.md, "Restart the API server").

## 1. Automated checks

```bash
cd api-server && ./gradlew test              # engine trace, reconciliation, breakdown sections
cd backend && deno task test && deno lint    # BFF proxies the new shape
cd frontend && npm run lint && npm test && npm run build
```

Expected: all pass. The API-server suite includes:
- With and without a trace, every market preset gives identical results (FR004-006).
- Replaying the recorded movements reproduces every quarterly balance (SC004-001).
- For every year of every preset: breakdown amounts match the results row (SC004-002), flows match ledgers (FR004-055),
  exact next-year items match the next year (SC004-003).

## 2. The endpoint

```bash
curl -s localhost:8000/api/simulate/breakdown -H 'Content-Type: application/json' \
  -d "{\"config\": $(cat config.json), \"targetAge\": 57}" | jq '.phase, .moneyMovement.ledgers[0].closing'
```

`config.json` is any valid `SimulationConfig` (for example the form's default, as sent by the app). Expected: `"RETIREMENT"`,
and four closing balances, the last equal to the SB balance in that year's results row.

## 3. In the app (default configuration)

1. Open http://localhost:5173 and run the simulation.
2. **Working year (age 53):** open 🔍. Seven sections are listed. Starting Position is open and shows the prior
   year-end balances and the year's rates. Income Gap says salary covers expenses. Expenses shows Wants derived
   from salary step by step.
3. **First retirement year (age 56):** open Money Movement. The flow diagram shows salary absent and TDA/TBA → SB.
   For each account ledger, check opening + rows = closing per quarter, and that Q4 closing matches the results
   row. Each quarter shows the strategy rule and what limited the refill.
4. **Taxes (age 57):** this year's bill = last year's taxable income × 12%; a Roth conversion appears in the
   taxable income generated this year.
5. **Next Year (age 55):** shows retirement starting, salary ending, healthcare moving to the post-retirement rate,
   and an estimated income gap appearing.
6. **Quarterly view:** switch the table to Quarterly and open 🔍 on a Q3 row. The breakdown opens on Money
   Movement with Q3 highlighted.
7. **Market preset:** choose "2008 Crash", run, and open the first retirement year: equity return −37%, attributed
   to the preset's year 1.
8. **Last year (age 85):** Next Year says the simulation ends this year.
9. The browser console shows no errors.
