# Loan Payment Calculation for One-Time Expenses

**Applies to**: v1.1.25 and later (feature `003-one-time-expenses`)
**Spec**: FR003-016, FR003-016b, FR003-016c, FR003-017, FR003-019, FR003-020a, FR003-024

Loan-type one-time expenses are modeled as fixed-rate loans with **quarterly** payments. The API server
calculates the payment; it stays the same in every quarter of the loan term.

---

## Summary

| Rule | Behavior |
|------|----------|
| Who calculates | The Kotlin API server. Any `quarterlyPayment` sent by a client is ignored |
| Payment frequency | Quarterly (Q1–Q4) for every year of the term, from Q1 of the start year |
| Payment amount | Fixed (nominal) for the whole term; **not** inflated year to year |
| Inflation | The financed amount is entered in today's dollars and inflated to the loan's start year, once |
| Down payment | Inflated to the start year and paid as a lump sum in Q1 of the start year |
| End year | `startYear + termYears - 1` (a 1-year loan starts and ends in the same year) |
| Source of funds | Spend Bucket, after regular expenses; spending strategy refills it if it goes negative |

---

## The formula

Standard amortization, compounded quarterly:

```
Q = P × r(1+r)^n / ((1+r)^n − 1)

P = financed amount in start-year dollars = (principal − downPayment) × (1 + inflation)^(startYear − currentYear)
r = quarterly rate = APR / 100 / 4
n = number of quarterly payments = termYears × 4
```

For 0% APR: `Q = P / n`.

---

## Worked example

Car loan: principal $40,000, no down payment, 5% APR, 5 years, starting in 2027.
Simulation starts in 2025 with 3% inflation.

1. Financed amount in 2027 dollars: $40,000 × 1.03² = **$42,436.00**
2. Quarterly rate r = 0.0125, n = 20
3. Quarterly payment Q = **$2,411.23**, paid in every quarter from 2027 Q1 to 2031 Q4
4. Annual total shown in results and the breakdown: 4 × Q = **$9,644.92** in each year 2027–2031, then $0

With a $10,000 down payment on a $50,000 principal, the financed amount is still $40,000, so Q is the same.
In addition, 2027 Q1 includes the down payment of $10,000 × 1.03² = **$10,609.00**.

Other reference values (SC003-006, accurate to $0.01):

| Loan | Quarterly payment |
|------|-------------------|
| $100,000, 6% APR, 10 years | $3,342.71 |
| $60,000, 0% APR, 5 years | $3,000.00 |

---

## The estimate shown in the form

The expense form shows **"Est. Quarterly Payment"** using the same formula on today's dollars (no
inflation). For the example above it shows $2,272.82, while the simulation charges $2,411.23 because the
loan starts two years out. The note under the field says so. The actual amounts are in the results table
and the 🔍 computation breakdown.

---

## Where it lives

| Concern | File |
|---------|------|
| Formula and fixed payment | `api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt` (`calculateQuarterlyPayment`, `quarterlyPaymentFor`) |
| Quarterly deduction from the Spend Bucket | `api-server/src/main/kotlin/com/retirement/logic/SimulationEngine.kt` |
| Validation (APR 0–99.99, term 1–50 years, 0 ≤ down payment ≤ principal) | `api-server/src/main/kotlin/com/retirement/logic/ExpenseValidator.kt` |
| Form estimate (display only) | `frontend/src/utils/expenseUtils.ts` |
| Tests | `OneTimeExpenseTest.kt`, `OneTimeExpenseSimulationTest.kt`, `frontend/src/utils/expenseUtils.test.ts` |

---

## Changes from earlier versions

Before v1.1.25:

- The engine used the payment calculated by the frontend instead of calculating it itself.
- The annual payment was inflated every year, so a fixed-rate loan's cost grew over its term
  (the example loan cost $9,644.92 in its first year and $10,855.44 in its last).
- The whole year's payments were deducted from the Spend Bucket in Q1 instead of quarterly.

An older version of this document described monthly payments; the simulation has never charged monthly.
