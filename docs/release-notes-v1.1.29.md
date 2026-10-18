# Release Notes: v1.1.29

**Date**: October 18, 2026
**Build**: v1.1.28 → v1.1.29

This release adds market presets, saved simulations, Monte Carlo analysis and a step-by-step breakdown of
every year, and fixes the tax calculation and the spending strategy.
**Simulation results change**: taxes are lower, and plans with money in the Tax Free account or long flat
markets do noticeably better than in v1.1.28.

---

## Market presets, saved simulations and Monte Carlo (spec 002)

- **Market Conditions:** choose Historical Average, 2008 Crash, Bull Market or Stagflation, or set your own
  rates (Custom). The 2008 Crash replays 2008–2012 returns in the first retirement years.
- **Save and load:** save a simulation by name, list saved simulations and load one back into the form.
- **Monte Carlo:** runs the plan against many market histories (sampled from historical data, centred on
  your rates) and charts every run with median, 75% and 90% lines and the success rate. Click a run to
  see its market variables by year and its full cash flow. The same seed always gives the same runs.
- **Market performance** is compared point to point with 12 months earlier; before, it was fixed.

## Yearly computation breakdown (spec 004)

The 🔍 button on each results row explains that year in seven sections: Starting Position, Expenses,
Income, Income Gap, Money Movement (a ledger per account and a flow diagram), Taxes and Next Year. Every
amount says how it was computed and matches the results table. It also works for a Monte Carlo run's
cash flow, with that run's own market.

## Results table and CSV

- **Market rates:** each year's equity return, inflation and bond yield are the last three columns of the
  table and the CSV. The start year, and simulations saved before this release, show `~`.
- **TFA Withdrawal:** a new column for withdrawals from the Tax Free account (see below).
- **Quarterly view:** when a one-time expense is paid from Taxable or Tax Deferred, the withdrawal now
  shows in its quarter, so the quarters add up to the year (#27). The yearly view was already right.
- **Export CSV** saves the view shown (yearly or quarterly) with the same columns as the table.

## Taxes and Total Income

- **401k contributions** are no longer taxed: they were taxed in every year after the first (#13).
- **Roth conversions** are taxed once; they were taxed twice (#15).
- **Total Income** no longer counts Roth conversions, which only move money between accounts (#16).

**Expect lower tax bills** than in v1.1.28, especially with 401k contributions or Roth conversions.

## Spending strategy

- **Market down, Spend Bucket at its floor (#21):** the Crash Buffer refills the SB while it is at least
  90% of its balance 12 months earlier, or has lost less than the equities over those 12 months.
  Otherwise Taxable and Tax Deferred refill it. Before, the Crash Buffer always paid, and in long flat
  markets it was emptied.
- **Spending cut (#22):** in a downturn, Wants are cut by 10% of this year's Wants, with inflation; before,
  it was 10% of the amount entered in the form.
- **Tax Free account (#23):** it is now the last resort for spending. When Taxable and Tax Deferred can't
  pay, the rest comes from the Tax Free account, untaxed. It never refills the Crash Buffer. A plan fails
  only once it is also empty.

**Expect higher success rates.** For the default form plan, with the tax fixes already in place, Monte Carlo
success (1,000 runs, same seed) went from 23.7% to 39.5% with the Crash Buffer change, 39.6% with the
spending cut and 56.7% with the Tax Free account.

---

## For developers

- **API:**
  - New endpoints: `GET /api/market-presets`, `/api/simulations` (save, list, load),
    `POST /api/simulate/monte-carlo`, `/monte-carlo/run`, `/simulate/breakdown` (returns `YearBreakdown`)
    and `/monte-carlo/run/breakdown`.
  - Results rows have `market` (`MarketRates`, nullable) and `cashFlow.tfaWithdrawal`.
  - `QuarterStrategy` has a new rule `MARKET_DOWN_FROM_EQUITIES` and the fields `cbbVs12MonthsPrior`,
    `cbbLoss` and `equityLoss`.
  - Invalid input returns 422 with per-field errors; every error is JSON.
  - TS contracts are regenerated. **Restart the API server** after upgrading.
- **Identifiers:** every spec identifier carries its spec number (`FR004-050`, `T004-001`); see the
  constitution. The spec-kit preset `spec-numbered-ids` keeps the rule across upgrades and needs PyYAML.
- **Tests:** Kotlin `./gradlew test` (205), frontend `npm test` (127), backend `deno task test` (21).
- **Project instructions:** `CLAUDE.md`; restart only the API server with
  `lsof -ti:8090 -sTCP:LISTEN | xargs kill -9`, which leaves the BFF running.

## Known issues / follow-ups

- Retirement is assumed at the end of the year Retirement Age is reached. A retirement month is planned
  for more accurate projections.
- The Crash Buffer's value doesn't change with bond prices; its balance falls only through withdrawals.
- Builds with `--warning-mode all` still show the `Configuration.setVisible` deprecation from the Kotlin
  Gradle plugin 2.3.0.
