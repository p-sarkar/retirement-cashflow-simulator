# Research: Yearly Computation Breakdown

**Feature**: [spec.md](./spec.md) | **Date**: 2026-10-16

The Technical Context had no open unknowns: the stack, tiers and test tools are fixed by the existing code and the
constitution. The decisions below settle how to get the data the spec asks for out of the engine without
changing any result.

## R004-1. Where the per-quarter movements come from

**Decision**: The engine records every balance change as a **movement** (quarter, type, from, to, amount) through an
optional `SimulationTrace` passed to `SimulationEngine.runSimulation`. With no trace (the default), the engine
does nothing extra. `/api/simulate` and Monte Carlo don't pass one. Only `/api/simulate/breakdown` does.

**Rationale**:
- The engine changes balances in about 15 places (interest and dividend credits, strategy refills, Roth
  conversions, take-home salary, contributions, Social Security, regular expenses, one-time expenses, shortfall
  cover, market growth). Only the engine knows each one; reconstructing them afterwards from yearly totals is what
  the current breakdown does, and why it can't reconcile.
- Ledgers (FR004-050) and the flow diagram (FR004-055) are both built from the same movement list, so they always add
  up to each other.
- Monte Carlo runs the engine thousands of times. Recording nothing when there is no trace keeps its speed and
  memory unchanged.
- Determinism (constitution IV) holds: the trace only receives values and never feeds back into the engine.

**Alternatives considered**:
- *Add quarterly movement fields to `QuarterlyResult`*: would bloat every `/api/simulate` and Monte Carlo
  response with data only the breakdown uses.
- *Rebuild movements in `BreakdownGenerator` from yearly and quarterly totals*: can't separate combined flows
  (for example equity withdrawals that went to the CBB rather than the SB) and would duplicate engine rules.

## R004-2. Proving the breakdown doesn't change results (FR004-006)

**Decision**: Two engine-level tests, written first:
1. For each market preset applied to the default plan, a run with a trace returns a `SimulationResult` equal to a
   run without one.
2. Replaying the recorded movements from the starting balances reproduces every `QuarterlyResult` balance, to the
   cent. This is SC004-001 and also proves no balance change was missed.

**Rationale**: Test 2 fails as soon as a new balance change is added to the engine without being recorded, so the
breakdown can't silently drift from the engine later.

## R004-3. Spending-strategy detail per quarter (FR004-052)

**Decision**: `SpendingStrategy.executeQuarterly` returns what it moved and why, as new fields on
`SpendingResult`: the rule that applied (market up with CBB full, market up with CBB below its cap, market down
with the SB above its floor (no movement), market down drawing from the CBB) and what limited the refill (the
quarterly share of the gap, the SB cap, the SB floor deficit, the CBB cap, or the available balance), and the
amounts for TDA→SB, TBA→SB, TDA→CBB, TBA→CBB and CBB→SB separately. `coverShortfall` returns the same split.

**Rationale**: Today `SpendingResult` only has totals per source, so the engine can't tell a TDA→SB refill from a
TDA→CBB refill. Adding output fields changes no decision.

**Note for the breakdown**: CBB performance is still a fixed placeholder (1.0) in the engine, so the down-market
"CBB has fallen" branch never runs. The thresholds shown in Income Gap (FR004-041) must say so rather than imply it
is evaluated. Market performance against 12 months earlier and against the all-time high *are* evaluated each
quarter (since #10), and are recorded per quarter.

## R004-4. Next-year projection (FR004-070–FR004-073)

**Decision**: Build the projection from the **next year's own start-of-year plan** recorded by the trace: its
inflation-adjusted expenses, tax bill, salary, Social Security, scheduled one-time expenses and loan payments, the
estimated interest and dividends, and the estimated income gap. These are all fixed before any market return of
that year is applied, so they are exactly what is knowable at the end of the year being viewed. Mark interest,
dividends and the gap as estimates. The changes list (FR004-072) comes from comparing this year's and next year's
situation (phase, healthcare bracket, Social Security status, loans active, CBB cap).

**Rationale**: Reusing the engine's own next-year values guarantees SC004-003 (exact items match next year's actuals)
without a second copy of the expense rules. The engine already computes these values at the start of each year;
the trace records them.

**Alternatives considered**: *A separate projection function*: duplicates the expense, healthcare, tax and Social
Security rules and can drift from the engine.

## R004-5. The first simulated year

**Decision**: The trace also records the engine's estimate of the start year's taxable income (salary minus 401k,
plus interest and dividends on the starting balances), which sets the first year's tax bill (FR004-060). Opening
balances are the configured starting balances (FR004-010).

## R004-6. The year the plan fails (FR004-054)

**Finding**: When a failure condition is met, the engine still finishes all four quarters of that year and stops
after it. The results table shows that year's year-end balances.

**Decision**: The ledger shows all four quarters, so it still reconciles with the results table (FR004-050), and marks
the quarter where the failure condition was first met, with the account and the condition (for example "TBA and
TDA both below zero"). FR004-054 is amended from "end at the quarter that failed" to "mark the quarter that failed".

## R004-7. Response shape

**Decision**: Replace `ComputationBreakdown` (a generic list of `label / formula / values / result / explanation`
steps) with a structured `YearBreakdown`: one typed object per section (see [data-model.md](./data-model.md)).
Generic `LineItem`s stay for the explanatory lines. The request (`BreakdownRequest { config, targetAge }`) is
unchanged.

**Rationale**: The ledgers and the flow diagram need structured data (accounts × quarters, from/to/amount); a
flat list of steps can't carry them. Types come from the Kotlin models through `generateTsContract`
(constitution III).

**Breaking change**: The BFF only proxies the endpoint and needs no logic change; its generated types update.
Callers of `/api/simulate/breakdown` outside this repo get the new shape.

## R004-8. Flow diagram (FR004-055)

**Decision**: A Sankey diagram with Recharts' `Sankey`. Recharts is already a dependency (the Monte Carlo chart
uses it), so nothing new is added. Nodes are the sources and destinations (Salary, Market, each account,
Expenses, Taxes); links are the year's movements summed by (from, to). The API sends the summed flows; the
frontend only lays them out.

**Alternatives considered**:
- *Hand-drawn SVG*: more code for the layout Recharts already does.
- *A new chart library*: avoidable.

## R004-9. Quarterly rows (FR004-008)

**Decision**: The frontend passes the clicked quarter to the dialog, which opens Money Movement and highlights that
quarter's column. No API change: the breakdown is always for the whole year.

## R004-10. Layout and SC004-006

**Decision**: Keep the current dialog. The seven sections are accordions in spec order, all headers visible, so
any section is one click away (SC004-006). Starting Position opens by default; Money Movement opens by default when the
dialog is opened from a quarterly row. In Money Movement, the flow diagram sits above one ledger table per account
(rows = movement types, columns = Q1–Q4 + Year), with opening and closing rows.

## R004-11. Existing tests and docs that change

- `OneTimeExpenseScenarioTest` (T003-040–T003-043, T003-079) reads the "One-Time Expenses" breakdown section. Those checks
  move to the Expenses section (FR003-033-v2 still holds: each expense by name, type and amount).
- `BreakdownDialog.test.tsx`, `SimulationRoutesTest` (breakdown route) and the BFF breakdown tests update to the
  new shape.
- `specs/002-retirement-cashflow/contracts/api.yaml` (`ComputationBreakdown` → `YearBreakdown`), README's
  breakdown description, and `TESTING-BREAKDOWN.md` if it describes the old sections.
