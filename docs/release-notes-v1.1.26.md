# Release Notes: v1.1.26

**Date**: October 14, 2026
**Build**: v1.1.25 → v1.1.26
**Branch**: 003-one-time-expenses

This release completes the one-time expenses feature (spec 003). It makes the simulation period
consistent, reworks the expense form, and replaces the remaining manual checks with automated tests.
**Simulation results do not change for valid inputs.** Some inputs that were accepted before are now rejected.

---

## Changes to validation

### The simulation period ends at age 85

The engine has always simulated from the year after the current year through age 85. Validation
accepted the current year through current year + 35, so an expense in the current year, or after
age 85, passed validation and was then **silently skipped**. Now:

- An expense, or a loan's start, must fall within the simulated years. For current year 2025 and
  age 60, that's 2026 (age 61) to 2050 (age 85). The error names the range:
  "Expense must fall within the simulation: 2026 (age 61) to 2050 (age 85)".
- A loan that starts in range may still run past age 85. Payments after that are not simulated.
- A current age of 85 or more is rejected (there would be no years to simulate).
- The end age is fixed at 85 for now. It is expected to become a field on the configuration form.

Saved or scripted configs with an expense in the current year, or after age 85, will now get a 422
instead of running without that expense.

---

## Form

- **Collapsible expenses**: each expense collapses to a one-line summary, e.g.
  "Loan · $50,000 ($10,000 down) · 5% · 5 yrs · from age 55 · est. $2,253.80/qtr". Saved expenses
  start collapsed; invalid ones stay open. **Expand all / Collapse all** appears with two or more.
- **Clearer validation**: blank fields say "Amount is required" rather than "must be greater than zero";
  a new expense lists what's still missing instead of showing every field in red; fractional loan
  terms are rejected. Entries re-check when **Current Age** or **Current Year** changes. Before,
  Run stayed enabled and the server rejected the request.
- **Cash vs Loan explained** in a tooltip on the section heading and in the Type menu.
- **Number fields**: dollar amounts show thousands separators (pasting "$1,250,000" works); APR and
  term show `%` and `yrs`.
- **Running state**: **Run Simulation** is disabled with a spinner while a simulation runs.
- **Errors**: failures now say what happened (service unreachable, timed out after 30 seconds, or a
  server error with its message) instead of "Network Error". The form reopens so you can fix inputs.

## Results

- A loan whose down payment covers the whole principal no longer adds $0 "Loan Payment" rows to the
  🔍 breakdown for the rest of its term.

---

## For developers

- **Tests**: Kotlin `./gradlew test` (60), frontend `npm test` (51), BFF `deno task test` (3).
  `OneTimeExpenseScenarioTest` covers the spec's acceptance scenarios and edge cases, which were
  previously manual browser checks.
- **Simulation period**: `SimulationPeriod` (`api-server/.../model/SimulationPeriod.kt`) holds
  `END_AGE` and the simulated year range for the engine and validators. The frontend mirrors it as
  `SIMULATION_END_AGE` in `frontend/src/utils/expenseUtils.ts`; change both together.
- **Logging**: `api-server/src/main/resources/logback.xml` is new and sets INFO. Without it, logback
  defaulted to DEBUG for everything. The engine logs a one-line summary of one-time expenses per run;
  set `com.retirement.logic.SimulationEngine` to DEBUG to log each expense's years and loan payment.
- **Docs**: README has a One-Time Expenses section; FR003-041, the API contract and data model are
  updated for the simulation period.

## Known issues / follow-ups

- The simulation end age (85) is not configurable yet.
- The 🔍 breakdown still labels loan payments "Inflation-adjusted amount"; they are fixed.
- The BFF returns 500 rather than the contract's 503 when the API server is unreachable.
- The BFF imports Oak 14 from deno.land/x, which runs in Node-compatibility mode under Deno 2;
  switching to `jsr:@oak/oak` should fix it.
- `npm run lint` fails: the frontend has no ESLint config.
- No CI: the test suites and the generated-types check only run when someone runs them.
