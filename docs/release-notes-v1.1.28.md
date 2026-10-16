# Release Notes: v1.1.28

**Date**: October 15, 2026
**Build**: v1.1.27 → v1.1.28

This release fixes how the Spend Bucket (SB) is funded and reports working years without an income gap.
**Simulation results change**: projections are more conservative than in v1.1.27.

---

## Spend Bucket double count fixed

Interest, dividends and Social Security were deposited into the SB, but the SB then paid out only the
income gap (expenses minus that same income). That counted the income twice, so the SB gained roughly a
year's passive income every year.

- The SB now pays out the full regular expenses each quarter.
- The SB balances out every year: start + deposits − withdrawals = end. The 🔍 breakdown shows this.
- In working years, the SB grows by exactly that year's interest and dividends.

**Expect lower SB balances and, for some plans, lower success** than in v1.1.27. The earlier numbers
were too optimistic.

## Working years (spec 002)

Every year up to and including the year you reach Retirement Age is a working year: salary for the
whole year, pre-retirement healthcare and the pre-retirement growth rate.

- **Income Gap** shows `~` in working years, because salary covers expenses. The Start Year row is a
  balance snapshot, so it shows `~` too. Before, it showed a retirement-baseline gap of all-zero cash flows.
- **SB Cap** shows `~` in working years, where it isn't applied.
- Roth conversions no longer reduce working-year Wants. They move money from Tax Deferred to Tax Free
  without touching the SB, and their tax is in the next year's tax bill.

## Spending strategy

- **SB floor:** the SB Cap is never below the larger of 6 months of the income gap and 1 quarter of
  expenses, and each refill also repays any shortfall below that floor. Without this, the SB could run
  dry in plans where dividends and Social Security cover most expenses.
- **Interest estimate:** in retirement the strategy estimates interest as if the SB holds about half a
  year of expenses, as the spec describes, instead of using the start-of-year SB balance.

## Form defaults

The form's default Current Year is now this year (was last year), with updated default balances, Wants,
HYSA rate, income tax rate and post-retirement Roth conversion.

---

## For developers

- **API:** `metrics.annualIncomeGap` and `metrics.sbCap` are now nullable (`null` in working years and
  for the Start Year snapshot). The TS contracts are regenerated. **Restart the API server** after
  upgrading; the frontend expects the new values.
- **Tests:** Kotlin `./gradlew test` (83, new `WorkingYearsTest` and `SpendingStrategyTest`), frontend
  `npm test` (59), backend `deno task test` (7).
- **Spec 002:** spec, data model and Phase 9 tasks (T002-053–T002-062).

## Known issues / follow-ups

- Retirement is assumed at the end of the year Retirement Age is reached. A retirement month is planned
  for more accurate projections.
- Builds with `--warning-mode all` still show the `Configuration.setVisible` deprecation from the Kotlin
  Gradle plugin 2.3.0.
