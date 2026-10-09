# Release Notes: v1.1.25

**Date**: October 9, 2026
**Build**: v1.1.24 → v1.1.25
**Branch**: 003-one-time-expenses

This release fixes how loans are simulated, enforces input validation, and adds automated tests
across all three tiers. **Simulation results change for any scenario that includes a loan.**

---

## Changes to results

### Loans (one-time expenses)

| | Before (v1.1.24) | Now (v1.1.25) |
|---|---|---|
| Payment calculated by | The frontend; the engine trusted the value it was sent | The API server |
| Payment over the term | Inflated every year | Fixed for the whole term (start-year dollars) |
| When it leaves the Spend Bucket | The whole year's payments in Q1 | One payment each quarter |

Example: a $40,000 loan at 5% for 5 years starting 2 years out (3% inflation) now costs $9,644.92 in every
year of its term. Before, it cost $9,644.92 in the first year, rising to $10,855.44 in the last, all deducted in Q1.
See [loan-payment-calculation.md](loan-payment-calculation.md).

Cash expenses and loan down payments are unchanged: inflation-adjusted lump sums in Q1.

### Validation is now enforced

Previously the server ran no validation, and the form silently dropped invalid expenses before running.

- The API returns **422** with per-field errors for invalid one-time expenses (FR-043).
- The API rejects rates outside plausible ranges, which catches whole-number percentages sent where decimals
  are expected (FR-044): inflation -10%..25%, growth -50%..50%, bond yield and HYSA rate 0%..25%,
  income tax 0%..90%, TDA withdrawal percentage 0%..100%.
- The form disables **Run Simulation** while any expense entry is incomplete or invalid, and lists server
  errors per expense ("Expense 2 (Car Loan): APR cannot be negative").

Saved or scripted configs with values outside these ranges will now be rejected instead of simulated.

### Form

- The loan payment field is now **"Est. Quarterly Payment"**, shown in today's dollars, with a note that
  the simulated payment is fixed in start-year dollars.

---

## For developers

- **Tests**: Kotlin `./gradlew test` (39), frontend `npm test` (27, Vitest added), BFF `deno task test` (3).
- **Generated API types**: `frontend/src/types/contract.generated.ts` and `backend/src/contract.generated.ts`
  are generated from the Kotlin models. After changing a model run `cd api-server && ./gradlew generateTsContract`;
  `./gradlew test` fails if they are stale.
- **BFF** passes 422 responses through as JSON, and its types now match the API.
- **Build**: the Gradle wrapper jar is committed, so `./gradlew` works on a fresh clone. Requires JDK 25.
  Local development used Node 24 LTS and Deno 2.9.
- **Spec Kit**: `.claude/skills` and `.specify` are committed; Spec Kit uses the Python scripts.

## Known issues / follow-ups

- Manual browser integration tests (tasks.md, marked MANUAL TEST REQUIRED / READY) need to be re-run.
- The 🔍 breakdown still labels loan payments "Inflation-adjusted amount"; they are now fixed.
- The BFF returns 500 rather than the contract's 503 when the API server is unreachable.
- The BFF imports Oak 14 from deno.land/x, which runs in Node-compatibility mode under Deno 2
  ("oak send() does not work under Node.js" in logs); switching to `jsr:@oak/oak` should fix it.
- No CI: the test suites and the generated-types check only run when someone runs them.
