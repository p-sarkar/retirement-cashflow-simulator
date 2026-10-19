---
description: "Task list for the Selectable Spending Strategy with Risk-Based Guardrails feature"
---

# Tasks: Selectable Spending Strategy with Risk-Based Guardrails

**Input**: Design documents from `specs/006-guardrails-spending-strategy/`
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/strategy-api.md](./contracts/strategy-api.md), [quickstart.md](./quickstart.md)

**Tests**: Required. The constitution (I. Test-First) and CLAUDE.md require a failing test before each behaviour
change. Write each test task first, and confirm it fails before the implementation task that follows it.
A required field that a later story fills is stubbed (`null`), not built early. This is the lesson from spec
004's Complexity Tracking.

**Compiling tests first** (analysis I006-2): Kotlin compiles all tests together, so one test that uses a missing
type stops every test from running, including the golden test (T006-003). A tests-first task therefore also adds
empty stubs of the main types it uses. Examples: a class with its fields, or a function that returns a default or
calls `TODO()`. The test then compiles and fails on its checks. The implementation task that follows replaces the
stubs.

**Organization**: One phase per user story, in spec priority order. US006-1, US006-2 and US006-3 ship together in one PR
(plan, Phasing; analysis A006-1). US006-4 can follow in the same PR or a later one.

**Delivery**: One feature branch, `feat/006-guardrails-spending-strategy`. Mark each task `[x]` when done, and
note any deviation after "Done:".

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: The user story the task belongs to (US006-1 to US006-4)

## Path Conventions

- API server: `api-server/src/main/kotlin/com/retirement/`, tests in `api-server/src/test/kotlin/com/retirement/`
- BFF: `backend/src/`, tests in `backend/tests/`
- Frontend: `frontend/src/`
- After any `api-server/` change, restart the API server before testing in the app (CLAUDE.md).

---

## Phase 1: Setup

**Purpose**: A green baseline, fixtures, and the safety net for existing results, all before any engine change

- [ ] T006-001 Create branch `feat/006-guardrails-spending-strategy` from `main`, then run the full suites and record the test counts here. Afterwards, revert api-server/src/main/resources/version.properties. The suites:
  - `cd api-server && ./gradlew test`
  - `cd backend && deno task test && deno lint`
  - `cd frontend && npm run lint && npm test && npm run build`
- [ ] T006-002 [P] Add fixtures to api-server/src/test/kotlin/com/retirement/TestConfigs.kt. They use only existing fields (`strategy.type` is already a `String`), so they compile now. T006-009 adds an optional `GuardrailsSettings` parameter to both:
  - `guardrailsConfig()`: `defaultFormConfig()` with `strategy.type = "RISK_BASED_GUARDRAILS_V1"`
  - `guardrailsCrashConfig()`: `guardrailsConfig()` with `marketPresetId = "crash-2008"`
- [ ] T006-003 Write api-server/src/test/kotlin/com/retirement/logic/ExistingStrategyGoldenTest.kt (R006-9, SC006-001, FR006-007), and generate its snapshots on unchanged code:
  - **Inputs**: `testConfig()`, every `presetConfigs()` entry, the one-time-expense config from OneTimeExpenseScenarioTest, and a 50-run `MonteCarloEngine.run` with seed 42.
  - **Check**: each result is serialized with the server's response JSON settings, Ktor's `json()` (which writes out default values), with `apiMetadata` removed. It is compared with JSON in api-server/src/test/resources/golden/006/:
    - Every field in the snapshot must be present and equal, numbers to within 1e-9.
    - A field the snapshot lacks is allowed only if it is `null` or equals its declared default. This covers `guardrails: null` and the default `strategy.guardrails` in the config echo (analysis I006-1).
  - **Snapshots**: written when `GOLDEN_UPDATE=1` is set. Without it, a missing snapshot fails the test.
  - Generate the snapshots and confirm the test passes. This test must stay green for every later task.
- [ ] T006-004 [P] Write api-server/src/test/kotlin/com/retirement/logic/MarketDrawTest.kt:
  - `MonteCarloEngine.generateScenario` for seeds 1, 42 and 20261019 and run indexes 0–4 equals a snapshot taken now, both with the real history and with an empty history (the normal fallback). The snapshot goes in api-server/src/test/resources/golden/006/scenarios.json.
  - It uses only existing code, so it compiles before T006-005. T006-005 adds a `MarketDraw.drawYear` case to this file.
- [ ] T006-005 Extract the per-year draw from `MonteCarloEngine.generateScenario` into api-server/src/main/kotlin/com/retirement/logic/MarketDraw.kt (R006-3).
  - The function is `drawYear(random, rates, growth, history, historicalMeanReturn, historicalMeanInflation): YearDraw(equityReturn, inflation, bondYield)`. It consumes the random stream in exactly the current order: a historical index or two gaussians, then the bond gaussian. It applies `MIN_RETURN` and the bond floor of 0.
  - Move the constants (`BOND_YIELD_SD`, `EQUITY_RETURN_SD`, `INFLATION_SD`, `MIN_RETURN`) with it.
  - Make `generateScenario` call it. Make `runSeed` `internal`, so review seeds can reuse its spreading constant.
  - Done when T006-003 and T006-004 pass.

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: The settings model, the strategy catalog and endpoint, the `SpendingPolicy` seam in the engine, and the
down-market Wants flag. Existing results stay identical throughout (T006-003).

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Tests first

- [ ] T006-006 [P] Extend api-server/src/test/kotlin/com/retirement/logic/SpendingStrategyTest.kt. In the market-down, SB-at-floor branch, `executeQuarterly(..., wantsCutInDownMarket = false)` refills `aig / 4 + sbDeficit` (no 10% Wants reduction). With the default (`true`), the amount is unchanged (FR006-015).
- [ ] T006-007 [P] Extend api-server/src/test/kotlin/com/retirement/logic/ConfigValidatorTest.kt (FR006-010, data-model "GuardrailsSettings"):
  - These return the field `strategy.type`:
    - an unknown `strategy.type`
  - These apply when guardrails is selected:
    - `targetSuccess`, `upperGuardrail` or `lowerGuardrail` outside "Between 0.01 and 0.99" returns that field
    - ordering broken ("`lowerGuardrail < targetSuccess < upperGuardrail`"): the error goes on `strategy.guardrails.lowerGuardrail` when it is not below the target, and on `strategy.guardrails.upperGuardrail` when that is not above it
    - `reviewFrequency` other than "`"YEARLY"` or `"QUARTERLY"`" returns `strategy.guardrails.reviewFrequency`
  - Invalid guardrail settings with the existing strategy selected give no error.
  - A config without `type` or `guardrails`, deserialized from JSON, is valid and gets the defaults (FR006-003).
- [ ] T006-008 [P] Write api-server/src/test/kotlin/com/retirement/logic/SpendingStrategyCatalogTest.kt, and extend api-server/src/test/kotlin/com/retirement/api/SimulationRoutesTest.kt:
  - The catalog has `PARTHA_V0_01_20250105` first, with settings "NONE", and `RISK_BASED_GUARDRAILS_V1`, with settings "GUARDRAILS". Each has a non-blank name and a one-line description (FR006-002).
  - `GET /api/spending-strategies` returns 200 with both entries, as in contracts/strategy-api.md.
  - A guardrails config POSTed to `/api/simulate` with `lowerGuardrail = 0.95` returns a 422 JSON `ValidationErrorResponse`.

### Implementation

- [ ] T006-009 In api-server/src/main/kotlin/com/retirement/model/SimulationConfig.kt:
  - Add `@Serializable data class GuardrailsSettings(val targetSuccess: Double = 0.90, val upperGuardrail: Double = 0.99, val lowerGuardrail: Double = 0.70, val reviewFrequency: String = "YEARLY")`.
  - Add `val guardrails: GuardrailsSettings = GuardrailsSettings()` to `StrategyConfig`. Keep `type: String = "PARTHA_V0_01_20250105"` (R006-6).
  - Add an optional `guardrails: GuardrailsSettings = GuardrailsSettings()` parameter to the T006-002 fixtures in api-server/src/test/kotlin/com/retirement/TestConfigs.kt.
  - Create api-server/src/main/kotlin/com/retirement/model/SpendingStrategyInfo.kt: `@Serializable data class SpendingStrategyInfo(val id: String, val name: String, val description: String, val settings: String)`.
- [ ] T006-010 [P] Add `wantsCutInDownMarket: Boolean = true` to `SpendingStrategy.executeQuarterly` in api-server/src/main/kotlin/com/retirement/logic/SpendingStrategy.kt. When it is false, `reducedAig = aig`. Make T006-006 pass.
- [ ] T006-011 Create api-server/src/main/kotlin/com/retirement/logic/SpendingPolicy.kt (R006-1, data-model "Internal"):
  - `interface SpendingPolicy { val wantsCutInDownMarket: Boolean; fun review(context: ReviewContext): ReviewDecision? }`
  - `data class ReviewContext(balances, year, quarter, age, inflationFactor, priorYearTaxableIncome, wantsReal, config, history, reviewSeed, computeDollarGuardrails)`
  - `data class ReviewDecision(val wantsReal: Double, val review: GuardrailReview?)`. Declare `GuardrailReview` as a placeholder in this file until T006-028 moves it to the model.
  - `object FixedWantsPolicy`: cut flag true; `review` returns null.
- [ ] T006-012 Create api-server/src/main/kotlin/com/retirement/logic/SpendingStrategyCatalog.kt:
  - Constants `EXISTING = "PARTHA_V0_01_20250105"` and `GUARDRAILS = "RISK_BASED_GUARDRAILS_V1"`.
  - `val all: List<SpendingStrategyInfo>` with the names and descriptions in contracts/strategy-api.md. The display name is "Partha's Spending Strategy-v0.01-20260105".
  - `fun find(id): SpendingStrategyInfo?`
  - `fun policyFor(config): SpendingPolicy`. It returns `FixedWantsPolicy` for both ids until T006-033.
  - Make the catalog part of T006-008 pass.
- [ ] T006-013 Wire the policy into api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt without changing any result:
  - Retirement years take Wants from a tracked `wantsReal` (initially `config.expenses.wants`) × `inflationAdjustment`. This replaces `config.expenses.wants * inflationAdjustment` at the `wantsAdjusted` initialisation; working years are unchanged.
  - Pass `policy.wantsCutInDownMarket` to `executeQuarterly`.
  - Add the parameters `guardrailSeed: Long = GuardrailsDefaults.INTERACTIVE_SEED` and `history: List<HistoricalYear> = emptyList()`. Create `GuardrailsDefaults` in api-server/src/main/kotlin/com/retirement/logic/RiskBasedGuardrailsPolicy.kt, holding only the data-model constants (`PATH_COUNT = 500`, `INTERACTIVE_SEED = 20261019`, `RAISE_CAP = 1.5`, `SUCCESS_TOLERANCE = 0.005`, `WANTS_TOLERANCE = 100.0`).
  - T006-003 must still pass.
- [ ] T006-014 Add `strategy.type` and guardrail validation to api-server/src/main/kotlin/com/retirement/logic/ConfigValidator.kt, with messages in the style of contracts/strategy-api.md (for example "Lower guardrail (90%) must be below the target (90%)"). Make T006-007 pass.
- [ ] T006-015 Add `get("/spending-strategies") { call.respond(SpendingStrategyCatalog.all) }` to api-server/src/main/kotlin/com/retirement/api/SimulationRoutes.kt.
  - Add `SpendingStrategyInfo.serializer().descriptor` to the response roots in api-server/src/test/kotlin/com/retirement/contract/TsContractGenerator.kt.
  - Run `cd api-server && ./gradlew generateTsContract`.
  - Make T006-008 and `ContractTypesTest` pass.
- [ ] T006-016 [P] In backend/src/routes.ts, proxy `GET /api/spending-strategies`: `proxy<never, SpendingStrategyInfo[]>(ctx, "GET", "/api/spending-strategies")`, next to `/api/market-presets`. Add a proxy test in backend/tests/routes.test.ts, then run `deno task test && deno lint`.
- [ ] T006-017 [P] In frontend/src/services/api.ts, add `getSpendingStrategies(): Promise<SpendingStrategyInfo[]>`, mirroring `getMarketPresets`. Re-export the new generated types from frontend/src/types/simulation.ts, and add a test in frontend/src/services/api.test.ts.

**Checkpoint**: the catalog is served, the settings validate, the engine runs through the policy seam, and all golden results are unchanged

---

## Phase 3: User Story 1 - Choose a spending strategy (Priority: P1) 🎯 MVP (with US006-2)

**Goal**: The user picks the strategy from a described list. Guardrail settings appear only for guardrails, and the
choice and every setting persist (FR006-001 to FR006-005).

**Independent Test**: Load a simulation saved before this feature: it shows the existing strategy and its results
are unchanged. Switch to guardrails, change the settings, save, reload: everything comes back. Switch away and back:
the settings are kept.

### Tests first

- [ ] T006-018 [P] [US006-1] Extend frontend/src/utils/formConfig.test.ts:
  - Guardrail probabilities convert 90 ⇄ 0.90, 99 ⇄ 0.99 and 70 ⇄ 0.70.
  - `reviewFrequency` passes through.
  - A config without `strategy.type` loads as `PARTHA_V0_01_20250105`, and one without `strategy.guardrails` loads with defaults 90 / 99 / 70 / YEARLY (FR006-003).
- [ ] T006-019 [P] [US006-1] Extend frontend/src/components/SimulationForm.test.tsx:
  - The strategy select lists the catalog names with their descriptions, and the existing strategy is selected by default (US006-1 scenario 2).
  - Selecting Risk-Based Guardrails shows Target %, Upper guardrail %, Lower guardrail % and Review frequency with their defaults. The TDA withdrawal % and Roth fields stay visible (scenario 3, FR006-005).
  - Switching to the existing strategy hides the guardrail fields. Switching back restores edited values (scenario 5).
  - A 422 with `strategy.guardrails.lowerGuardrail` shows on that field.
  - When the catalog fails to load, only the existing strategy is offered (R006-7).
- [ ] T006-020 [P] [US006-1] Extend api-server/src/test/kotlin/com/retirement/api/PersistenceAndMonteCarloRoutesTest.kt:
  - A result saved with a guardrails config and custom settings (0.85 / 0.97 / 0.60 / QUARTERLY) reloads with `strategy.type` and every setting intact (US006-1 scenario 4).
  - A stored result JSON from before this feature, with no `guardrails` key, loads with defaults (FR006-003).

### Implementation

- [ ] T006-021 [US006-1] In frontend/src/utils/formConfig.ts, add the guardrail fields to the form state as whole-number percentages, converted to decimals in the API config (constitution, "Units"), with defaults for configs that lack them. Make T006-018 pass.
- [ ] T006-022 [US006-1] In frontend/src/components/SimulationForm.tsx "Withdrawal Strategy":
  - Add a strategy `Select` fed by a `strategies: SpendingStrategyInfo[]` prop. Show the selected strategy's description under it.
  - When the selected entry's `settings === "GUARDRAILS"`, render Target %, Upper guardrail % and Lower guardrail % as number inputs (step 1, 1–99), and Review frequency (Yearly / Quarterly).
  - Keep the guardrail values in form state while another strategy is selected.
  - Map the 422 field errors.
  - Make T006-019 pass.
- [ ] T006-023 [US006-1] In frontend/src/pages/SimulationPage.tsx, load `getSpendingStrategies()` next to `getMarketPresets()` (line 86). Fall back to the existing strategy alone on error, and pass the list to `SimulationForm`.

**Checkpoint**: the strategy can be selected and is saved. Until US006-2, guardrails runs with fixed Wants, so ship it together with US006-2.

---

## Phase 4: User Story 2 - Simulate retirement with risk-based guardrails (Priority: P1) 🎯 MVP

**Goal**: Reviews measure the probability of success over 500 seeded paths, and adjust Wants back to the target when
a guardrail is crossed. The existing bucket rules decide which accounts pay. Results show the spending path,
the reviews and the summary, and Monte Carlo adds spending statistics (FR006-011 to FR006-019, FR006-021).

**Independent Test**: Run `guardrailsCrashConfig()`:
- Wants are cut at the first review after the crash, and the probability after the cut is within 0.5 percentage point of 90%.
- Wants rise again once the probability passes 99%.
- A repeated run is identical.

### Tests first

- [ ] T006-024 [P] [US006-2] Write api-server/src/test/kotlin/com/retirement/logic/SuccessProjectorTest.kt (R006-2):
  - **Consistency with the engine**: on paths fixed at the configured rates (no volatility, configured inflation and bond yield), the projector's depletion year is within 1 year of `SimulationEngine`'s `failureYear`, and success or failure agrees, for `testConfig()`, every `presetConfigs()` entry run at its static rates, and a low-balance config that fails.
  - **Monotonic**: the probability is non-increasing in `wantsReal` and non-decreasing in `balanceScale`.
  - **Bounds**: a portfolio of 0 with expenses above income gives probability 0. Income covering all expenses gives 1.0.
  - **Cash flows**: one-time cash expenses and loan payments in the remaining years lower the probability.
  - **Paths**: `MarketPaths` for the same seed are identical, and differ for another seed.
- [ ] T006-025 [P] [US006-2] Write api-server/src/test/kotlin/com/retirement/logic/RiskBasedGuardrailsPolicyTest.kt, with `ReviewContext` fixtures (FR006-012, SC006-003):
  - **NO_CHANGE**: a probability between the guardrails returns `wantsAfter == wantsBefore`.
  - **CUT**: below the lower guardrail, `probabilityAfter` is within 0.005 of the target, or the Wants interval is under $100.
  - **RAISE**: above the upper guardrail, the same accuracy.
  - **RAISE_CAPPED**: Wants stop at 1.5 × configured Wants with inflation.
  - **TARGET_UNREACHABLE**: Wants = 0 still misses the target, so Wants become 0.
  - **Zero configured Wants**: only NO_CHANGE or TARGET_UNREACHABLE. A probability above the upper guardrail gives NO_CHANGE, not RAISE_CAPPED, because the cap is 0 (analysis U006-2).
  - **Short horizon**: a review one year and two years before `endAge` returns a valid decision, with a probability between 0 and 1, and its solve ends within the tolerance (spec edge case "Short horizon near the end age").
  - **Dollar fields**: the dollar-guardrail fields are null (until US006-3).
  - **Determinism**: the same context gives the same decision.
- [ ] T006-026 [P] [US006-2] Write api-server/src/test/kotlin/com/retirement/logic/GuardrailsSimulationTest.kt (yearly reviews):
  - **Review timing**: the first review is year `retirementAge + 1`, quarter 1. There is one review per later retirement year, and none in working years (R006-5).
  - **Wants**: between reviews, `cashFlow.wants` = the adjusted Wants in today's dollars × the cumulative inflation (FR006-013).
  - **Results fields**: `yearlyResults[].guardrails.configuredWants` = configured Wants × inflation. `summary.guardrails` has `startingProbability`, cuts and raises counts, `largestCutPercent`, the lowest and highest annual spending in today's dollars, and `configuredSpendingUnreachable` (data-model "GuardrailsSummary").
  - **Crash**: on `guardrailsCrashConfig()`, the first review after the crash is `CUT`, and the plan lasts at least as long as the same config with the existing strategy (SC006-004).
  - **Failure**: reviews stop at the failure year (FR006-016).
  - **Down-market Wants cut**: it is not applied (FR006-015).
  - **Fields on the existing strategy**: `guardrails` fields stay null there (FR006-022).
  - **Determinism**: two runs give equal results (FR006-017, SC006-007).
- [ ] T006-027 [P] [US006-2] Extend api-server/src/test/kotlin/com/retirement/logic/MonteCarloEngineTest.kt, with a 50-run guardrails Monte Carlo:
  - `paths[].hadSpendingCut` is set.
  - `statistics.spending` has `percentile10Path ≤ medianPath ≤ percentile90Path` for each retirement year, null for working years, and `shareOfRunsWithCut` in 0–1 equal to the share of paths with `hadSpendingCut` (FR006-021).
  - The same seed twice gives identical results.
  - `runDetail` for run i equals the batch's path i.
  - The existing strategy leaves `statistics.spending` and `hadSpendingCut` null.

### Implementation

- [ ] T006-028 [US006-2] Add the result models from data-model.md "Result models" to api-server/src/main/kotlin/com/retirement/model/SimulationResult.kt, all optional with a `null` default:
  - `GuardrailReview` (dollar-guardrail fields `Double?`), `GuardrailOutcome` (`NO_CHANGE`, `CUT`, `RAISE`, `RAISE_CAPPED`, `TARGET_UNREACHABLE`), `YearGuardrails`, `GuardrailsSummary`, `SpendingStatistics`
  - `YearlyResult.guardrails`, `Summary.guardrails`, `MonteCarloPath.hadSpendingCut`, `Statistics.spending`
  - Remove the placeholder from T006-011, then run `./gradlew generateTsContract`. T006-003 must still pass. The new fields appear in responses as `null`, which T006-003's comparison allows (analysis I006-1).
- [ ] T006-029 [US006-2] Add `MarketPaths` to api-server/src/main/kotlin/com/retirement/logic/SuccessProjector.kt:
  - `generate(config, fromAge, seed, history, count = PATH_COUNT)` fills `DoubleArray`s of equity return, inflation and bond yield for each path and year, through `MarketDraw.drawYear` at the post-retirement growth (R006-3).
  - `reviewSeed(simulationSeed, year, quarter)` mixes with `runSeed`'s constant.
- [ ] T006-030 [US006-2] Implement `SuccessProjector.probability(context, wantsReal, balanceScale = 1.0): Double` in api-server/src/main/kotlin/com/retirement/logic/SuccessProjector.kt, following research R006-2:
  - **Pool**: one pool with the review's cash, bond and equity shares, scaled by `balanceScale`.
  - **Expenses**: per year, Needs, Wants, healthcare (bracket by age, as the engine) and property tax, grown with the path's inflation.
  - **Social Security**: with the spousal step-up, shared with the engine through an extracted `socialSecurity(config, yearIdx, inflation)` helper in api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt.
  - **One-time expenses**: cash × inflation in their year; loan payments through `LoanExpense.quarterlyPaymentFor`.
  - **Income tax**: the flat rate on the previous year's taxable income (Social Security, interest, dividends, Roth conversions, withdrawals × (tdaPct + (1 − tdaPct) × 0.5)).
  - **Failure**: a path fails when the pool goes below 0 before `endAge`.
  - No allocation inside the year loop. Make T006-024 pass.
- [ ] T006-031 [US006-2] Implement `RiskBasedGuardrailsPolicy` in api-server/src/main/kotlin/com/retirement/logic/RiskBasedGuardrailsPolicy.kt (R006-4):
  - Set `wantsCutInDownMarket = false`.
  - `review`: build the review's `MarketPaths` once and compute `probabilityBefore`.
  - Below `lowerGuardrail`: bisect Wants in [0, current] to the target, giving `CUT`, or `TARGET_UNREACHABLE` at 0.
  - Above `upperGuardrail`: bisect in [current, RAISE_CAP × configured Wants in today's dollars], giving `RAISE`, or `RAISE_CAPPED`. When the cap is 0 (no configured Wants), the outcome is `NO_CHANGE`.
  - Stop when within `SUCCESS_TOLERANCE`, or when the interval is under `WANTS_TOLERANCE`.
  - Return the `GuardrailReview` with the dollar fields null.
  - Make T006-025 pass.
- [ ] T006-032 [US006-2] In api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt (yearly reviews):
  - At Q1 of each retirement year, before `wantsAdjusted` is computed, build the `ReviewContext` and call `policy.review`. Use the year-start balances, `inflationAdjustment`, `priorYearTaxableIncome`, `reviewSeed(guardrailSeed, year, 1)` and the passed `history`.
  - Update `wantsReal`, and record `YearlyResult.guardrails` (`configuredWants`, `reviews`).
  - Stop reviewing after a failure.
  - Build `Summary.guardrails` from the reviews and the yearly cash flows. Annual spending is Needs + Wants + healthcare + property tax, divided by the cumulative inflation.
- [ ] T006-033 [US006-2] Return `RiskBasedGuardrailsPolicy` for `RISK_BASED_GUARDRAILS_V1` in `SpendingStrategyCatalog.policyFor` in api-server/src/main/kotlin/com/retirement/logic/SpendingStrategyCatalog.kt.
  - In api-server/src/main/kotlin/com/retirement/api/SimulationRoutes.kt, pass `HistoricalDataService.getHistoricalYears()` (loaded once per request) to `runSimulation` from `runInteractive` and the breakdown route.
  - Make T006-026 pass, with T006-003 still green.
- [ ] T006-034 [US006-2] In api-server/src/main/kotlin/com/retirement/logic/MonteCarloEngine.kt:
  - Pass `guardrailSeed = runSeed(seed, runIndex)` and the analysis's `history` to `runSimulation` in `simulateRun`.
  - Set `MonteCarloPath.hadSpendingCut` when any review is `CUT` or `TARGET_UNREACHABLE`.
  - Compute `Statistics.spending`: per-year 10th, 50th and 90th percentiles of annual spending in today's dollars over the runs still going that year, null for working years, plus `shareOfRunsWithCut`. Use the existing `percentile`.
  - Make T006-027 pass.
- [ ] T006-035 [US006-2] Extend api-server/src/test/kotlin/com/retirement/logic/GuardrailsSimulationTest.kt with quarterly reviews (`reviewFrequency = "QUARTERLY"`):
  - Four reviews in each full retirement year, with quarters 1–4.
  - A review that changes Wants changes `cashFlow.wants` for that quarter onward. The quarterly results' Wants add up to the year's `cashFlow.wants`.
  - The SB floor and cap and the AIG estimate use the quarter's Wants.
  - Results are deterministic.
- [ ] T006-036 [US006-2] Support quarterly reviews in api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt (R006-5):
  - Make `wantsAdjusted`, `grossExpenses`, `estimatedAig` and `yearCapAig` per-quarter values, recomputed after a review changes Wants at the start of Q2–Q4. Keep the year-start computation for Q1.
  - The yearly cash flow sums the quarters' Wants.
  - Make T006-035 pass, with T006-003 and T006-026 still green.
- [ ] T006-037 [P] [US006-2] Write frontend/src/components/GuardrailsSummary.test.tsx, extend frontend/src/components/ResultsTable.test.tsx, and add a frontend/src/components/MonteCarloResults.test.tsx case, all with a guardrails `SimulationResult` and `MonteCarloResult` fixture:
  - **GuardrailsSummary**: tiles for the starting probability, cuts and raises, the largest cut, and the lowest and highest annual spending.
  - **Charts**: a spending-versus-configured chart, and probability by review against the lower and upper guardrail band.
  - **Warning**: shown when `configuredSpendingUnreachable` is set.
  - **ResultsTable**: shows "Wants (configured)" and the review outcome only for guardrails results.
  - **MonteCarloResults**: shows the spending percentile tiles and the share of runs with a cut only for guardrails.
  - Nothing new renders for an existing-strategy result.
- [ ] T006-038 [US006-2] Create frontend/src/components/GuardrailsSummary.tsx: stat tiles plus two Recharts charts (Wants per year versus configured Wants, and probability per review with the guardrail band), as in T006-037. Render it in frontend/src/pages/SimulationPage.tsx above `ResultsTable` when `summary.guardrails` is set.
- [ ] T006-039 [P] [US006-2] Add the guardrails columns to frontend/src/components/ResultsTable.tsx and frontend/src/utils/resultColumns.ts: configured Wants next to Wants, the review probability and the outcome chip. Show them only when `guardrails` is set. Include them in the CSV export in frontend/src/utils/csvExport.ts.
- [ ] T006-040 [P] [US006-2] In frontend/src/components/MonteCarloResults.tsx, add the spending percentile tiles and the share of runs with a cut when `statistics.spending` is set. Make T006-037 pass.
- [ ] T006-041 [US006-2] Write api-server/src/test/kotlin/com/retirement/logic/GuardrailsPerformanceTest.kt (R006-10, SC006-005), tagged `@Tag("perf")`. In api-server/build.gradle.kts, change `useJUnitPlatform()` to `useJUnitPlatform { if (!project.hasProperty("perf")) excludeTags("perf") }`.
  - An interactive `guardrailsConfig()` run from age 65 to 95 with yearly reviews finishes in under 3 s.
  - A 1,000-run Monte Carlo finishes in under 60 s.
  - Run `./gradlew test -Pperf --tests '*GuardrailsPerformanceTest*'` and record the times here. If the bound is missed, profile `SuccessProjector` first, then apply R006-10's fallback, and update spec.md.
- [ ] T006-042 [US006-2] Restart the API server (CLAUDE.md), then run specs/006-guardrails-spending-strategy/quickstart.md §5 steps 1–3 and 5 against http://localhost:5173. Fix any gap, with a test first.

**Checkpoint**: guardrails simulate end to end, interactive and Monte Carlo. US006-1 and US006-2 work; ship them with US006-3 (FR006-018's dollar guardrails).

---

## Phase 5: User Story 3 - See the guardrails in dollars (Priority: P2)

**Goal**: Each review states the portfolio values at the lower and upper guardrails and the Wants that would follow
(US006-3). The summary shows the starting dollar guardrails.

**Independent Test**: For one review, a portfolio scaled to just below `lowerGuardrailPortfolio` gives a probability
below the lower guardrail, and one just above doesn't. Results with and without dollar guardrails are identical.

### Tests first

- [ ] T006-043 [P] [US006-3] Extend api-server/src/test/kotlin/com/retirement/logic/RiskBasedGuardrailsPolicyTest.kt and GuardrailsSimulationTest.kt:
  - With `computeDollarGuardrails = true`, scaling the balances to `lowerGuardrailPortfolio × 0.99` gives a probability below `lowerGuardrail`, and × 1.01 doesn't. The same holds for the upper guardrail, in the mirrored direction.
  - `wantsAtLowerGuardrail` brings the probability at `lowerGuardrailPortfolio` back to the target, within 0.005 or $100.
  - `upperGuardrailPortfolio` is null when no scale up to 10× reaches the upper guardrail.
  - A full simulation's `cashFlow`, `balances` and outcomes are identical with the flag on and off (R006-4).
  - Monte Carlo batch paths leave the dollar fields null, and `runDetail` fills them.
  - `summary.guardrails.startingLowerGuardrailPortfolio` and `startingUpperGuardrailPortfolio` come from the first review.

### Implementation

- [ ] T006-044 [US006-3] In api-server/src/main/kotlin/com/retirement/logic/RiskBasedGuardrailsPolicy.kt, when `computeDollarGuardrails` is set:
  - Bisect `balanceScale` in [0, 10] for each guardrail on the review's shared paths.
  - At each found portfolio value, solve the Wants that return the probability to the target.
  - Fill the four dollar fields.
- [ ] T006-045 [US006-3] Pass `computeDollarGuardrails` through the engine. It is true for interactive runs, the interactive breakdown, `MonteCarloEngine.runDetail` and `runBreakdown`, and false in the `MonteCarloEngine.run` batch. Change these files:
  - api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt: a parameter, default `true`.
  - api-server/src/main/kotlin/com/retirement/logic/MonteCarloEngine.kt.

  Then fill the summary's starting dollar fields, and make T006-043 pass.
- [ ] T006-046 [P] [US006-3] Extend frontend/src/components/GuardrailsSummary.test.tsx: the summary states "If your portfolio falls below $X, Wants drop to $Y a year; above $Z, they rise to $W", and each review row has the same figures in a tooltip. Values that are null are left out of the sentence.
- [ ] T006-047 [US006-3] Show the starting dollar guardrails in frontend/src/components/GuardrailsSummary.tsx, and each review's dollar guardrails in the probability chart tooltip. Make T006-046 pass.

**Checkpoint**: the dollar guardrails show for interactive runs and single Monte Carlo runs

---

## Phase 6: User Story 4 - Explain each review in the yearly breakdown (Priority: P3)

**Goal**: The yearly breakdown explains each guardrail review: settings, probability, outcome, and Wants before and after.
It also says when a year had no review (FR006-020).

**Independent Test**: Open the breakdown for the cut year of `guardrailsCrashConfig()`. It shows the review, `CUT`,
and the old and new Wants, and the year's Money Movement adds up to the adjusted spending.

### Tests first

- [ ] T006-048 [P] [US006-4] Write api-server/src/test/kotlin/com/retirement/logic/BreakdownGuardrailsTest.kt:
  - For a guardrails retirement year, `YearBreakdown.guardrails` has the settings and the year's `reviews` (equal to `yearlyResults[].guardrails.reviews`).
  - It also has `configuredWants` and `adjustedWants` as `LineItem`s whose amounts match the Expenses section's Wants. The adjusted rule reads "today's-dollar Wants × cumulative inflation".
  - With quarterly reviews, a year in which the plan fails before a later quarter's review sets `noReviewNote` (analysis U006-1).
  - Working years and the existing strategy leave `guardrails` null.
  - A Monte Carlo run breakdown (`MonteCarloEngine.runBreakdown`) also has the section, with dollar fields.

### Implementation

- [ ] T006-049 [US006-4] Add `GuardrailsSection` (data-model "GuardrailsSection") and `YearBreakdown.guardrails: GuardrailsSection? = null` to api-server/src/main/kotlin/com/retirement/model/YearBreakdown.kt.
  - Build it in api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt from `config.strategy.guardrails` and the year's `YearlyResult.guardrails`.
  - Run `./gradlew generateTsContract`, and make T006-048 pass.
- [ ] T006-050 [P] [US006-4] Write frontend/src/components/breakdown/GuardrailsSection.test.tsx:
  - Renders the settings, each review (probability before and after, outcome, Wants before and after, and the dollar guardrails when present), and the `noReviewNote`.
  - Nothing renders when the section is null.
- [ ] T006-051 [US006-4] Create frontend/src/components/breakdown/GuardrailsSection.tsx, using `KeyValueTable` and `LineItemTable` from the same folder, and render it in frontend/src/components/BreakdownDialog.tsx after the Expenses section. Make T006-050 pass.

**Checkpoint**: every story works on its own

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T006-052 [P] Update specs/002-retirement-cashflow/contracts/api.yaml with `GET /api/spending-strategies`, `StrategyConfig.guardrails`, the new result fields and the 422 fields (contracts/strategy-api.md).
- [ ] T006-053 [P] Add a cross-reference to specs/002-retirement-cashflow/spec.md. FR002-002 ("Initially supporting …") gets the note "*(2026-10-19: Risk-Based Guardrails added, FR006-001 to FR006-022.)*".
- [ ] T006-054 [P] Update README.md: the spending strategies, how to choose one, the guardrail settings and what the results show, and the `-Pperf` performance test command.
- [ ] T006-055 Bring specs/006-guardrails-spending-strategy/spec.md, data-model.md and contracts/strategy-api.md in line with anything that changed during implementation, for example the final catalog descriptions or the performance fallback.
- [ ] T006-056 Run the full suites. All must pass, with api-server/src/test/kotlin/com/retirement/logic/ExistingStrategyGoldenTest.kt and its snapshots unchanged:
  - `cd api-server && ./gradlew test`
  - `cd backend && deno task test && deno lint`
  - `cd frontend && npm run lint && npm test && npm run build`
- [ ] T006-057 Restart the API server, then run all of quickstart §5 in the app. Revert api-server/src/main/resources/version.properties. Mark every task here `[x]`.

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: T006-003 (golden) must exist and pass **before** any engine change (T006-005 onward).
- **Foundational (Phase 2)**: depends on Phase 1. Blocks all stories.
- **US006-1 (Phase 3)**: depends on Phase 2 only (frontend and persistence).
- **US006-2 (Phase 4)**: depends on Phase 2. It is independent of US006-1 in code, but ship them together (plan, Phasing).
- **US006-3 (Phase 5)**: depends on US006-2 (T006-031, T006-032).
- **US006-4 (Phase 6)**: depends on US006-2 (the reviews in `YearlyResult`). Its dollar fields show once US006-3 is done, and are null before.
- **Polish (Phase 7)**: after the stories that ship.

### Within US006-2

- T006-028 (models) comes before T006-029 to T006-034.
- T006-029 (paths) comes before T006-030 (projector), which comes before T006-031 (policy), then T006-032 and T006-033 (engine wiring), then T006-034 (Monte Carlo).
- T006-035 and T006-036 (quarterly) come after T006-032.
- The frontend tasks T006-037 to T006-040 need only T006-028 and its regenerated contract, so they can run alongside T006-029 to T006-036.

### Parallel opportunities

- Phase 1: T006-002 and T006-004, alongside T006-003.
- Phase 2: the tests T006-006, T006-007 and T006-008 together. T006-010 alongside T006-011 and T006-012. T006-016 (BFF) and T006-017 (frontend) together once T006-015 has regenerated the contract.
- US006-1: T006-018, T006-019 and T006-020 together. US006-1 as a whole alongside US006-2's API-server work.
- US006-2: T006-024, T006-025, T006-026 and T006-027 together. T006-039 and T006-040 together.
- US006-3 and US006-4: T006-043 and T006-048 together. T006-046 and T006-050 together.
- Polish: T006-052, T006-053 and T006-054 together.

### Parallel example: User Story 2

```text
# Tests first, together:
T006-024 SuccessProjectorTest.kt
T006-025 RiskBasedGuardrailsPolicyTest.kt
T006-026 GuardrailsSimulationTest.kt
T006-027 MonteCarloEngineTest.kt (guardrails cases)

# After T006-028 regenerates the contract, the frontend alongside the engine:
T006-037 → T006-038, T006-039, T006-040 (frontend)
T006-029 → T006-030 → T006-031 → T006-032/033 → T006-034 (API server)
```

## Implementation Strategy

### MVP (US006-1 + US006-2 + US006-3)

1. Phase 1: confirm the golden test is green on unchanged code. Nothing else starts before this.
2. Phase 2: catalog, settings, validation and the policy seam. The goldens stay green.
3. US006-1 and US006-2 (yearly reviews first, then quarterly). Check performance (T006-041) and the app (T006-042).
4. US006-3 (dollar guardrails, T006-043 to T006-047).
5. **Stop and validate**, then open the PR for the MVP if shipping in stages.

### Incremental delivery

- **Next**: US006-4 (breakdown), a small follow-up with its own tests.
- **Polish**: last. Docs and specs stay in sync (CLAUDE.md, "Keep specs in sync").
- **Commits**: none without the user's explicit request (CLAUDE.md).
