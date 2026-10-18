# Data Model

## Entities

### SimulationConfig
Represents the user input configuration for a simulation run.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Unique identifier (optional, for saved runs) |
| `name` | String | User-defined name for the simulation |
| `currentYear` | Integer | The starting year of the simulation |
| `currentAge` | Integer | Primary user's current age |
| `retirementAge` | Integer | Age at which retirement begins |
| `endAge` | Integer | Age the simulation runs through (FR002-001a). Optional, default 85; must be > `currentAge` and ≤ 110 |
| `salary` | Decimal | Annual salary (pre-retirement, inflation-adjusted) |
| `portfolio` | `Portfolio` | Current account balances |
| `spousal` | `SpousalDetails` | Spouse-related configuration |
| `expenses` | `ExpenseConfig` | Initial expense estimates |
| `contributions` | `ContributionConfig` | Annual contribution amounts |
| `rates` | `RateConfig` | Assumed growth and tax rates |
| `strategy` | `StrategyConfig` | Spending strategy parameters |
| `oneTimeExpenses` | List<`OneTimeExpense`> | Cash and loan expenses (spec 003). Optional, default empty |
| `marketPresetId` | String (nullable) | Id of the `MarketPreset` the market rates came from; `null` = custom market conditions (FR002-003). Optional |

### Portfolio
| Field | Type | Description |
|-------|------|-------------|
| `sb` | Decimal | Spend Bucket (HYSA) current balance |
| `cbb` | Decimal | Crash Buffer Bucket (Bonds) current balance |
| `tba` | Decimal | Taxable Brokerage Account balance |
| `tda` | Decimal | Tax-Deferred Account balance |
| `tfa` | Decimal | Tax-Free Account balance |

### SpousalDetails
| Field | Type | Description |
|-------|------|-------------|
| `spouseAge` | Integer | Spouse's current age |
| `lowerEarner` | `SocialSecurityDetails` | Details for lower earner |
| `higherEarner` | `SocialSecurityDetails` | Details for higher earner |

### SocialSecurityDetails
| Field | Type | Description |
|-------|------|-------------|
| `claimAge` | Integer | Age to start claiming SS |
| `annualBenefit` | Decimal | Annual benefit amount at claim age |

### ExpenseConfig
| Field | Type | Description |
|-------|------|-------------|
| `needs` | Decimal | Annual essential expenses |
| `wants` | Decimal | Annual discretionary expenses |
| `propertyTax` | Decimal | Annual property tax |
| `healthcarePreMedicare` | Decimal | Annual cost before 65 |
| `healthcareMedicare` | Decimal | Annual cost at 65+ |

### ContributionConfig
| Field | Type | Description |
|-------|------|-------------|
| `annual401k` | Decimal | Annual 401k contribution (pre-tax) |
| `annualRoth` | Decimal | Annual Roth contribution — Roth 401k and/or Roth IRA combined (post-tax, deposited to TFA) |
| `annualTba` | Decimal | Annual taxable brokerage contribution |

### RateConfig
| Field | Type | Description |
|-------|------|-------------|
| `inflation` | Decimal | Annual inflation rate (e.g., 0.03) |
| `preRetirementGrowth` | Decimal | Equities growth before retirement |
| `postRetirementGrowth` | Decimal | Equities growth after retirement |
| `bondYield` | Decimal | Annual bond yield |
| `hysaRate` | Decimal | HYSA interest rate |
| `incomeTax` | Decimal | Effective income tax rate |

### StrategyConfig
| Field | Type | Description |
|-------|------|-------------|
| `initialTdaWithdrawal` | Decimal | Deprecated, not used by the simulation (was the ATDAW); kept for older configs |
| `tdaWithdrawalPercentage` | Decimal | Share of each equity withdrawal taken from the TDA, the rest from the TBA (0.0–1.0, default 0.5; a whole-number percentage on the form) |
| `rothConversionAmount` | Decimal | Deprecated, not used; kept for older configs |
| `rothConversionPreRetirement` | Decimal | Annual TDA -> TFA conversion in working years (from Year 2) |
| `rothConversionPostRetirement` | Decimal | Annual TDA -> TFA conversion in retirement |
| `type` | String | "PARTHA_V0_01_20250105" |

The market thresholds (95% of 12 months earlier, 85% of the all-time high, 90% for the CBB) are fixed, not configuration (updated 2026-10-18).

### SimulationResult
The output of a simulation.

| Field | Type | Description |
|-------|------|-------------|
| `config` | `SimulationConfig` | The input configuration used |
| `yearlyResults` | List<`YearlyResult`> | Year-by-year breakdown |
| `summary` | `Summary` | Final status and aggregates |

### YearlyResult
| Field | Type | Description |
|-------|------|-------------|
| `year` | Integer | Calendar year |
| `age` | Integer | User's age |
| `balances` | `Portfolio` | Ending balances for the year |
| `cashFlow` | `CashFlow` | Income and Expenses for the year |
| `metrics` | `Metrics` | Calculated metrics (AIG, etc.) |
| `market` | `MarketRates` (nullable) | The year's applied rates (FR002-009). `null` for the Start Year snapshot and in results saved before 2026-10-17; the results table shows `~`. `QuarterlyResult` has the same field, with its year's rates |

### MarketRates
| Field | Type | Description |
|-------|------|-------------|
| `equityReturn` | Decimal | The year's equity return: the pre- or post-retirement growth rate, a market preset's return, or a Monte Carlo run's sample |
| `inflation` | Decimal | The year's inflation rate |
| `bondYield` | Decimal | The year's bond yield |

### CashFlow
| Field | Type | Description |
|-------|------|-------------|
| `salary` | Decimal | |
| `interest` | Decimal | From SB |
| `dividends` | Decimal | From CBB |
| `socialSecurity` | Decimal | Total SS benefit |
| `tbaWithdrawal` | Decimal | |
| `tdaWithdrawal` | Decimal | For spending |
| `tfaWithdrawal` | Decimal | Tax Free withdrawals for spending, the last resort when TDA and TBA can't pay (#23); not taxed. 0 in results saved before 2026-10-18 |
| `rothConversion` | Decimal | TDA -> TFA conversion (taxable) |
| `totalIncome` | Decimal | |
| `needs` | Decimal | Inflation adjusted |
| `wants` | Decimal | Inflation adjusted |
| `healthcare` | Decimal | |
| `incomeTax` | Decimal | |
| `propertyTax` | Decimal | |
| `totalExpenses` | Decimal | |

### Metrics
| Field | Type | Description |
|-------|------|-------------|
| `annualIncomeGap` | Decimal (nullable) | (Expenses - Recurring Income) in retirement years. `null` in working years (age <= Retirement Age, salary covers expenses) and for the Start Year snapshot; the results table shows `~` |
| `incomeGapExpenses` | Decimal | Total expenses used for the gap |
| `incomeGapPassiveIncome` | Decimal | Interest + Dividends + Social Security |
| `sbCap` | Decimal (nullable) | Retirement years: max(2 × Cap AIG, SB Floor), where SB Floor = max(0.5 × AIG, 0.25 × total regular expenses). `null` in working years and for the Start Year when it is a working year (the cap is not applied); the results table shows `~` |
| `cbbCap` | Decimal | 7 × Base Cap AIG, reduced at ages 65, 70, 75, 80, 85 |
| `isFailure` | Boolean | True from the first year a failure condition of FR002-008 holds (the simulation stops at the end of that year) |

### Summary
Aggregated metrics for a single simulation run.

| Field | Type | Description |
|-------|------|-------------|
| `finalTotalBalance` | Decimal | Sum of all account balances at end |
| `isSuccess` | Boolean | Whether simulation reached goal age |
| `failureYear` | Integer | Year of failure (if any) |
| `totalDividends` | Decimal | Cumulative dividends received |
| `totalInterest` | Decimal | Cumulative interest received |

### MarketPreset
A preset collection of market conditions (FR002-003), served by `GET /api/market-presets`. Selecting one in the form fills the config's market rates (inflation, growth, bond yield) and sets `marketPresetId`; editing any of those rates by hand switches back to custom.

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | e.g. `historical-average`, `crash-2008`, `bull-market`, `stagflation` |
| `name` | String | Display name |
| `description` | String | What the preset represents |
| `inflation`, `preRetirementGrowth`, `postRetirementGrowth`, `bondYield` | Decimal | The rates the preset fills in |
| `retirementReturnSequence` | List<Decimal> | Equity returns for the first retirement years, in order (the 2008 Crash replays 2008–2012: −37%, +26.5%, +15.1%, +2.1%, +16%). Interactive runs use it in place of `postRetirementGrowth` from the year after Retirement Age (or the first simulated year if already retired). Empty for static presets |

### SavedSimulation
A simulation saved with its inputs and results (FR002-011 to FR002-013). Stored in SQLite (`Simulations` table: id, name, created_at, configuration JSON, result JSON) by the API server only. Reloading returns the result exactly as saved (SC002-004).

| Field | Type | Description |
|-------|------|-------------|
| `summary` | `SavedSimulationSummary` | |
| `result` | `SimulationResult` | The full result; its `config` repopulates the form |

### SavedSimulationSummary
One row of the saved simulations list, newest first.

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | |
| `name` | String | User-chosen name (1–255 characters, trimmed) |
| `createdAt` | ISO-8601 date-time | When it was saved (the date it was run), with UTC offset |
| `isSuccess` | Boolean | |
| `failureYear` | Integer (nullable) | |
| `finalTotalBalance` | Decimal | |
| `endAge` | Integer | |

### MonteCarloRequest
| Field | Type | Description |
|-------|------|-------------|
| `config` | `SimulationConfig` | Everything except equity returns, inflation and bond yields is used as is |
| `runs` | Integer | 1–5000, default 1000 |
| `seed` | Long (nullable) | Omit to let the server pick one (within JavaScript's safe integer range). The same config, runs and seed always give the same result (Constitution IV) |

### MarketScenario
The market variables of one Monte Carlo run, one entry per year from the start year (index 0, not simulated, holds the config's rates) through the End Age year.

| Field | Type | Description |
|-------|------|-------------|
| `years` | List<Integer> | |
| `equityReturns` | List<Decimal> | Replace the pre/post-retirement growth rate each year |
| `inflationRates` | List<Decimal> | Compound year by year into the inflation factor for expenses, salary, benefits |
| `bondYields` | List<Decimal> | Drive CBB dividends each year |

Generation: each simulated year draws a random historical year (1928–2019: S&P 500 December-to-December price return and that year's inflation) and uses its return and inflation together, shifted so their long-run averages equal the configured growth and inflation rates. Bond yields are the configured yield plus normal noise (1% standard deviation), never below 0. Without historical data, returns and inflation are drawn from normal distributions around the configured rates (18% and 2% standard deviations).

### MonteCarloPath
Summary of one run: what the chart plots and what clicking a path shows.

| Field | Type | Description |
|-------|------|-------------|
| `runIndex` | Integer | Run *i* uses the scenario generated from (seed, *i*) |
| `totalBalances` | List<Decimal> | Total portfolio at the end of each year from the start year; a failed run stops at its failure year |
| `isSuccess` | Boolean | Reached End Age without failing |
| `failureYear` | Integer (nullable) | |
| `endingBalance` | Decimal | |
| `averageReturn`, `averageInflation`, `averageBondYield` | Decimal | Averages over the simulated years |

### Statistics
Confidence lines across all runs, by year (aligned with `MonteCarloResult.years`). A failed run counts as 0 after it fails.

| Field | Type | Description |
|-------|------|-------------|
| `medianPath` | List<`Decimal`> | Balance that half the runs reach or exceed |
| `percentile75Path` | List<`Decimal`> | Balance that 75% of runs reach or exceed (the 25th percentile of balances) |
| `percentile90Path` | List<`Decimal`> | Balance that 90% of runs reach or exceed (the 10th percentile): the most conservative line |
| `successRate` | Decimal | Fraction of runs that reached End Age (0–1) |

### MonteCarloResult
| Field | Type | Description |
|-------|------|-------------|
| `seed` | Long | The seed used, to reproduce the analysis or rebuild a run |
| `runCount` | Integer | |
| `method` | String | How the scenarios were generated, for display |
| `years` | List<Integer> | Start year through the End Age year |
| `paths` | List<`MonteCarloPath`> | One per run, in run order. Full tables are not included (1000 runs × 35 years would be tens of MB) |
| `statistics` | `Statistics` | |

### MonteCarloRunDetail
The full results of one run (US002-4 "Load Details"), rebuilt by `POST /api/simulate/monte-carlo/run` from the config, seed and run index.

| Field | Type | Description |
|-------|------|-------------|
| `seed` | Long | |
| `path` | `MonteCarloPath` | Same as in the analysis |
| `scenario` | `MarketScenario` | The run's variables, shown as rows below the chart |
| `result` | `SimulationResult` | The run's full cash flow table |
