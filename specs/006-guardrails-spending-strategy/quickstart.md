# Quickstart: Validating Risk-Based Guardrails

**Feature**: [spec.md](./spec.md) | **Models**: [data-model.md](./data-model.md) | **Contract**: [contracts/strategy-api.md](./contracts/strategy-api.md)

How to check that the feature works, from unit tests to the running app. All commands run from the repo root.

## 1. Before changing the engine: golden results (SC006-001)

On a clean `main`, generate the golden snapshots once (R006-9), commit them with the test, and confirm the
test passes against unchanged code:

```bash
cd api-server && ./gradlew test --tests '*ExistingStrategyGoldenTest*'
```

The test must keep passing through every later step. If it fails, an existing-strategy result changed, which
breaks FR006-007.

## 2. Unit and integration tests (API server)

```bash
cd api-server && ./gradlew test
```

| Area | Expected |
|------|----------|
| `ExistingStrategyGoldenTest` | Every preset, plus the one-time-expense config and a 50-run seeded Monte Carlo, match the snapshots |
| `MarketDrawTest` | Monte Carlo scenarios are identical before and after the year draw moves into `MarketDraw` (R006-3) |
| `SuccessProjectorTest` | Paths at the configured rates with no volatility deplete within one year of the engine's failure year. Success and failure agree for every preset. The probability falls as Wants rise and rises with the portfolio |
| `RiskBasedGuardrailsPolicyTest` | Between the guardrails: no change (SC006-003). A cut or raise ends within 0.5 percentage point of the target. The raise stops at 150% (`RAISE_CAPPED`). Zero Wants still misses the target (`TARGET_UNREACHABLE`). No Wants configured |
| `GuardrailsSimulationTest` | First review at Q1 of the first retirement year. Wants grow with inflation between reviews. A 30% crash in the first retirement year cuts Wants at the next review, and the plan lasts at least as long as with fixed spending (SC006-004). The 10% down-market Wants cut is off. Quarterly reviews work |
| Determinism | The same config, or config and seed, run twice gives identical results. Results with and without dollar guardrails are identical (R006-4) |
| `ConfigValidatorTest` | Unknown `strategy.type` or `reviewFrequency`, values outside the bounds, and broken ordering each return a 422 with the right field. Guardrail settings are ignored when the existing strategy is selected |
| Routes | `GET /api/spending-strategies` returns both entries. Simulate, breakdown and Monte Carlo accept guardrails configs. A saved guardrails simulation reloads with its settings (FR006-004) |
| `ContractTypesTest` | Generated TypeScript is current |

Contracts after any model change:

```bash
cd api-server && ./gradlew generateTsContract
```

## 3. BFF and frontend

```bash
cd backend && deno task test && deno lint
cd frontend && npm run lint && npm test && npm run build
```

| Area | Expected |
|------|----------|
| BFF | `GET /api/spending-strategies` is proxied. A 422 from the API server passes through as JSON |
| `SimulationForm` | Strategy select with descriptions, existing strategy by default. Guardrail fields appear only for guardrails, in whole-number percentages. Switching strategies and back keeps the settings. 422 messages show on the right fields |
| `formConfig` | 90 ⇄ 0.90 for the three probabilities. A missing `type` or `guardrails` loads as the defaults |
| Results | For a guardrails result: a summary panel with starting probability, dollar guardrails, cuts and raises, largest cut, and lowest and highest spending. A spending-versus-configured chart, and probability by review against the guardrail band. Table rows show adjusted and configured Wants. Nothing extra for the existing strategy |
| Monte Carlo | Spending percentile tiles and lines, and the share of runs with a cut, for guardrails only |
| Breakdown | The guardrails section shows the review, outcome and Wants before and after, plus the "no review" note (US006-4) |

## 4. Performance (SC006-005)

```bash
cd api-server && ./gradlew test -Pperf --tests '*GuardrailsPerformanceTest*'
```

Expected:
- An interactive guardrails run, from age 65 to 95 with yearly reviews, takes under 3 s.
- A 1,000-run Monte Carlo takes under 60 s.

Record the measured times in `tasks.md`. If Monte Carlo misses the bound, follow the fallback in R006-10.

## 5. End to end in the running app

Restart the API server (CLAUDE.md, "Restart the API server"). The frontend and BFF reload on their own.

1. Open the app (http://localhost:5173) and load a simulation saved before this feature. Run it: the
   strategy shows "Partha's Spending Strategy-v0.01-20260105" and the results match its saved results
   (US006-1 scenario 1).
2. Select **Risk-Based Guardrails** and keep the defaults (90 / 99 / 70, yearly). Run it in under a minute
   from opening the form (SC006-002). The results show:
   - the starting probability of success
   - the dollar guardrails ("below $X, Wants fall to $Y")
   - the spending path against configured spending
3. Select the **2008 Crash** market preset and run it. A `CUT` appears at the first review after the crash,
   and its breakdown explains it (US006-2, US006-4).
4. Set the lower guardrail to 95% with the target at 90%. Running shows the field error, and nothing runs
   (FR006-010).
5. Run Monte Carlo with 1,000 runs. The success rate, spending percentiles and share of runs with a cut appear.
   Open one run's details and breakdown: its reviews include the dollar guardrails.
6. Save as "Guardrails test", reload it, and check that the strategy and settings are restored. Switch to the
   existing strategy and back: the guardrail settings are still there (FR006-004).
7. Revert `api-server/src/main/resources/version.properties` (CLAUDE.md).
