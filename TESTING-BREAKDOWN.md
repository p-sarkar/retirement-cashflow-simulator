# Testing the Computation Breakdown

The 🔍 button on each results row opens the computation breakdown for that year (spec 004). The full check, with
expected results, is in [specs/004-yearly-breakdown/quickstart.md](specs/004-yearly-breakdown/quickstart.md).

## Quick check

1. Start the three services (README, "Running Services"). Restart the API server after any `api-server/` change.
2. Open http://localhost:5173, keep the default configuration and click **Run Simulation**.
3. Click 🔍 on a retirement year (age 57). The dialog lists seven sections: Starting Position, Expenses, Income,
   Income Gap, Money Movement, Taxes and Next Year.
4. Open **Money Movement**. For each account, opening + the quarter's rows = closing, and Q4's closing matches the
   results row. The flow diagram shows where money came from and went.
5. Switch the table to **Quarterly** and click 🔍 on a Q3 row: the breakdown opens on Money Movement with Q3
   highlighted.

## One-time expenses

The default configuration includes loans and a cash expense. At age 55, the Expenses section lists "Partha's car 1"
twice: its down payment (inflation-adjusted) and its loan payments (fixed for the loan term).

## Automated tests

- API server: `cd api-server && ./gradlew test` (the `Breakdown*Test` and `SimulationTraceTest` classes)
- Frontend: `cd frontend && npm test` (`BreakdownDialog.test.tsx`, `components/breakdown/*.test.tsx`)
