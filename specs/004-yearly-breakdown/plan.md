# Implementation Plan: Yearly Computation Breakdown

**Branch**: `feat/004-yearly-breakdown` | **Date**: 2026-10-16 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/004-yearly-breakdown/spec.md`

## Summary

Rebuild the per-year computation breakdown as seven sections: Starting Position, Expenses, Income, Income Gap,
Money Movement, Taxes, Next Year. Money Movement has a ledger per account per quarter and a Sankey flow diagram
for the year. Every figure must match the results table, and every account must reconcile each quarter.

Approach (research R1–R4): the simulation engine records each balance change, the start-of-year plan and each
quarter's strategy decision into an optional trace. Only the breakdown route passes a trace, so `/api/simulate`
and Monte Carlo are unchanged. `BreakdownGenerator` builds a typed `YearBreakdown` from the trace and the yearly
results. The next-year projection reuses the next year's own start-of-year plan, so it can't drift from the
engine. The frontend dialog renders the sections, including the diagram with Recharts' Sankey, which is already a
dependency.

## Technical Context

**Language/Version**: Kotlin 2.3 on JDK 25 (API server); TypeScript 5 strict (BFF on Deno 2, frontend on React 18)
**Primary Dependencies**: Ktor, kotlinx.serialization (API server); Oak 17 (BFF); MUI 7, Recharts 3 (frontend)
**Storage**: N/A. The breakdown is computed per request, and nothing is persisted.
**Testing**: JUnit 5 via `./gradlew test`; `deno task test`; Vitest + Testing Library
**Target Platform**: Local web app (browser + three local services)
**Project Type**: Web application, three tiers: `frontend/` → `backend/` (BFF) → `api-server/`
**Performance Goals**: The breakdown opens in under 1 second (SC-005). It runs one traced simulation of at most
about 60 years × 4 quarters; `/api/simulate` and Monte Carlo speed unchanged (no trace)
**Constraints**: No change to any simulation result (FR-006); deterministic (constitution IV); generated contracts
only (constitution III)
**Scale/Scope**: About 15 movement types × 5 accounts × 4 quarters per year; 1 endpoint reshaped; 1 dialog rebuilt

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Status |
|-----------|-------|--------|
| I. Test-first | Reconciliation, identical-results and per-section tests are written before the engine trace and the generator (quickstart §1). Domain logic stays fully covered: `SpendingStrategy` gains output fields with tests. | Pass |
| II. Strict tiers | All computation (trace, ledgers, flows, projections) is in the API server. The BFF only proxies. The frontend only lays out the data it gets, including the Sankey. | Pass |
| III. Explicit contracts | `YearBreakdown` is a Kotlin `@Serializable` model added to `TsContractGenerator` roots; TS types regenerated for BFF and frontend; `api.yaml` updated. | Pass |
| IV. Deterministic | The trace only receives values; it never feeds back. Tested: results with and without a trace are identical for every preset. | Pass |

**Post-design re-check (after Phase 1)**: Pass. The design adds no tier crossing and no new storage. The only new
dependency is a Recharts component that's already installed. The breaking change to the breakdown response is
contained in this repo (frontend and BFF regenerate) and noted in research R7.

## Project Structure

### Documentation (this feature)

```text
specs/004-yearly-breakdown/
├── spec.md
├── plan.md              # This file
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   └── breakdown-api.md # Phase 1
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
api-server/src/main/kotlin/com/retirement/
├── logic/
│   ├── SimulationEngine.kt        # optional trace parameter; record movements, plan, conditions, failure
│   ├── SimulationTrace.kt         # NEW: trace, YearTrace, Movement, YearPlan (internal)
│   ├── SpendingStrategy.kt        # SpendingResult/ShortfallResult gain rule, limit and per-route amounts
│   └── BreakdownGenerator.kt      # rewritten: builds YearBreakdown from the trace and yearly results
├── model/
│   └── YearBreakdown.kt           # NEW: @Serializable response models (replaces ComputationBreakdown)
└── api/
    └── SimulationRoutes.kt        # breakdown route runs a traced simulation

api-server/src/test/kotlin/com/retirement/
├── logic/SimulationTraceTest.kt   # NEW: identical results; movements reproduce quarterly balances
├── logic/BreakdownGeneratorTest.kt# NEW: per-section requirements, all presets × all years
├── logic/SpendingStrategyTest.kt  # rule and limit outputs
├── logic/OneTimeExpenseScenarioTest.kt  # breakdown checks move to the Expenses section
├── api/SimulationRoutesTest.kt    # breakdown route returns YearBreakdown
└── contract/TsContractGenerator.kt      # roots: YearBreakdown

backend/
├── src/contract.generated.ts      # regenerated
├── src/routes.ts                  # type name only
└── tests/                         # breakdown proxy tests use the new type

frontend/src/
├── components/BreakdownDialog.tsx # rebuilt: seven sections, initial quarter
├── components/breakdown/          # NEW: one component per section; MoneyFlowSankey; AccountLedgerTable
├── components/ResultsTable.tsx    # quarterly rows pass their quarter
├── types/contract.generated.ts    # regenerated
└── components/*.test.tsx          # dialog, ledger and Sankey rendering tests

specs/002-retirement-cashflow/contracts/api.yaml   # ComputationBreakdown → YearBreakdown
README.md, TESTING-BREAKDOWN.md                    # breakdown description
```

**Structure Decision**: The existing three-tier web layout. New Kotlin files sit beside the code they serve
(`logic/`, `model/`). On the frontend, section components go in a new `components/breakdown/` folder to keep
`BreakdownDialog.tsx` small.

## Phasing (for /speckit-tasks)

Maps to the spec's user stories, each releasable on its own:

1. **Foundation**: trace and `SpendingStrategy` outputs, with the identical-results and reconciliation tests;
   `YearBreakdown` skeleton, contract regeneration, route switch.
2. **US1 (P1) Money Movement**: ledgers, flows, quarter strategy, failure mark; Sankey and ledger tables; quarterly
   row → highlighted quarter.
3. **US2 (P2) Expenses, Income, Income Gap**, folding in the old strategy and caps content (FR-007).
4. **US3 (P3) Starting Position**: conditions with preset attribution, household.
5. **US4 (P4) Taxes**.
6. **US5 (P5) Next Year**.
7. **Polish**: `api.yaml`, README, `TESTING-BREAKDOWN.md`, spec 002/003 cross-references, end-to-end check in the
   app (quickstart §3).

Until US2–US5 land, the old `ComputationBreakdown` sections aren't available. To avoid a gap, the foundation phase
keeps the old section content reachable inside the new response until each new section replaces it, or the
phases ship together in one PR. `/speckit-tasks` should pick one.

## Complexity Tracking

No constitution violations to justify.
