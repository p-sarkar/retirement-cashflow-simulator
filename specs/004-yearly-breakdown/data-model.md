# Data Model: Yearly Computation Breakdown

**Feature**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

Two groups of models: the **trace** the engine fills during a breakdown run (internal to the API server), and the
**`YearBreakdown`** response built from it (part of the API contract, generated into TypeScript). All amounts are
nominal dollars of the year shown; rates are decimals (0.03 = 3%).

## Internal: simulation trace (API server only)

Not serialized. Filled only when a trace is passed to `SimulationEngine.runSimulation` (research R1).

### SimulationTrace

| Field | Type | Description |
|-------|------|-------------|
| `startYearTaxableIncomeEstimate` | Decimal | Estimate of the start year's taxable income that sets the first year's tax bill (R5) |
| `years` | Map<Int, YearTrace> | Keyed by simulated year |

### YearTrace

| Field | Type | Description |
|-------|------|-------------|
| `plan` | YearPlan | Values fixed at the start of the year, before any market return (R4) |
| `conditions` | EconomicConditions | Rates applied this year and the market position at the start of the year |
| `movements` | List<Movement> | Every balance change, in the order the engine made it |
| `quarterStrategy` | List<QuarterStrategy> | One per quarter in retirement years (R3) |
| `failure` | FailureMark? | Set if a failure condition was met this year (R6) |

### YearPlan

Inflation-adjusted Needs, Wants (and, in working years, each input to the Wants formula), Healthcare and its
bracket, Property Tax, the tax bill, salary, contributions, each earner's Social Security, scheduled one-time
expenses and loan payments, estimated interest, estimated dividends, estimated income gap, Cap AIG, SB floor, SB cap
and CBB cap.

### Movement

| Field | Type | Rules |
|-------|------|-------|
| `quarter` | Int | 1–4 |
| `type` | MovementType | See below |
| `from` | Endpoint | Account or outside source |
| `to` | Endpoint | Account or outside destination; never equal to `from` |
| `amount` | Decimal | > 0; the direction is from → to (market losses are `MARKET` ← account) |

**MovementType**: `INTEREST`, `DIVIDENDS`, `TAKE_HOME_SALARY`, `CONTRIBUTION_401K`, `CONTRIBUTION_ROTH`,
`CONTRIBUTION_TBA`, `SOCIAL_SECURITY`, `REGULAR_EXPENSES`, `ONE_TIME_EXPENSE`, `STRATEGY_REFILL`,
`SHORTFALL_COVER`, `ROTH_CONVERSION`, `MARKET_GROWTH`.

**Endpoint**: the accounts `SB`, `CBB`, `TBA`, `TDA`, `TFA`, and the outside parties `SALARY`, `SOCIAL_SECURITY`,
`BANK` (interest), `BONDS` (dividends), `EXPENSES`, `MARKET`.

**Invariant (R2, SC-001)**: for each account, starting balance + Σ movements in − Σ movements out, applied in
order, equals the `QuarterlyResult` balance at the end of each quarter.

## API: `YearBreakdown` (response of `POST /api/simulate/breakdown`)

Replaces `ComputationBreakdown`. The request is unchanged: `BreakdownRequest { config, targetAge }`.

| Field | Type | Description |
|-------|------|-------------|
| `year`, `age`, `spouseAge` | Int | |
| `phase` | `WORKING` \| `RETIREMENT` \| `START` | `START` for the start-year snapshot: only `startingPosition` is set (spec edge case) |
| `startingPosition` | StartingPosition | Section 1 |
| `expenses` | ExpenseSection? | Section 2; null for `START` |
| `income` | IncomeSection? | Section 3 |
| `incomeGap` | IncomeGapSection? | Section 4 |
| `moneyMovement` | MoneyMovementSection? | Section 5 |
| `taxes` | TaxSection? | Section 6 |
| `nextYear` | NextYearSection? | Section 7; null for `START` and for the last simulated year (FR-073 is shown from `isLastYear`) |
| `isLastYear` | Boolean | |
| `apiMetadata` | ApiMetadata | As today |

### LineItem (shared)

| Field | Type | Description |
|-------|------|-------------|
| `label` | String | |
| `amount` | Decimal | |
| `rule` | String | How it was computed, in plain words (FR-003) |
| `inputs` | Map<String, Decimal> | Named inputs to the rule; may be empty |
| `todaysDollars` | Decimal? | The configured amount before inflation adjustment (FR-005) |

### StartingPosition (FR-010–FR-013)

| Field | Type | Description |
|-------|------|-------------|
| `openingBalances` | Portfolio | Prior year-end balances; the configured balances for the first simulated year |
| `openingTotal` | Decimal | |
| `conditions` | EconomicConditions | |
| `household` | HouseholdStatus | |

**EconomicConditions**: `inflationRate`, `cumulativeInflation` (factor since the start year), `equityReturn`,
`equityReturnSource` (`PRE_RETIREMENT_RATE` \| `POST_RETIREMENT_RATE` \| `PRESET_SEQUENCE`),
`presetName` (String?), `presetSequenceYear` (Int?, 1-based), `bondYield`, `hysaRate`, `incomeTaxRate`,
`marketVsAllTimeHigh` (ratio at the start of the year).

**HouseholdStatus**: `healthcareBracket` (`PRE_RETIREMENT` \| `POST_RETIREMENT_PRE_MEDICARE` \| `MEDICARE`),
`earners`: list of `{ role: HIGHER | LOWER, age, claimAge, claiming: Boolean }`.

### ExpenseSection (FR-020–FR-022)

`items`: LineItem per category (Needs, Wants, Healthcare, Property Tax, Income Tax); `wantsDerivation`:
List<LineItem> (working years: salary, then each deduction, then the result); `oneTimeExpenses`: list of
`{ name, type: CASH | DOWN_PAYMENT | LOAN_PAYMENT, amount, inflationAdjusted: Boolean }`; `total` (= results-table
Total Expenses).

### IncomeSection (FR-030–FR-032)

`salary`: `{ gross, contribution401k, contributionRoth, contributionTba, takeHome }` or null in retirement years;
`interest`, `dividends`: LineItem; `socialSecurity`: list of `{ role, amount, spousalStepUp: Boolean, startYear }`;
`incomeTotal`; `withdrawals`: LineItem list (TBA withdrawals, TDA withdrawals for spending, Roth conversions);
`totalIncome` (= results-table Total Income, = `incomeTotal` + withdrawals).

### IncomeGapSection (FR-040–FR-042)

Retirement years: `estimated` and `actual`, each `{ expenses, passiveIncome, gap }`; `differences`: LineItem list
explaining estimated vs actual; `capAig`, `sbFloor`, `sbCap`, `cbbCap`: LineItem; `cbbCapReductions`: list of ages
reached; `thresholds`: list of `{ name, value, evaluated: Boolean }` (the CBB-performance threshold has
`evaluated = false`, research R3).
Working years: `takeHomeSalary` and `expenses` LineItems and an explanation; the other fields are null.

### MoneyMovementSection (FR-050–FR-055)

| Field | Type | Description |
|-------|------|-------------|
| `ledgers` | List<AccountLedger> | One per account, in order SB, CBB, TBA, TDA, TFA |
| `flows` | List<Flow> | The year's movements summed by (from, to); for the diagram |
| `quarterStrategy` | List<QuarterStrategy> | Retirement years: one per quarter |
| `failure` | FailureMark? | |

**AccountLedger**: `account`; `opening`: Decimal[4]; `rows`: list of `{ type: MovementType, counterparty: Endpoint,
amounts: Decimal[4] (signed: + in, − out), yearTotal }`; `closing`: Decimal[4].
Rules: `closing[q] = opening[q] + Σ rows.amounts[q]`; `opening[q+1] = closing[q]`; `closing[3]` = the results-table
year-end balance.

**Flow**: `{ from: Endpoint, to: Endpoint, amount }` with `amount > 0`. Σ flows into or out of an account equals
that account's ledger year totals.

**QuarterStrategy**: `quarter`; `rule` (`MARKET_UP_CBB_FULL` \| `MARKET_UP_CBB_REFILL` \| `MARKET_DOWN_HOLD` \|
`MARKET_DOWN_FROM_CBB`); `marketVs12MonthsPrior`, `marketVsAllTimeHigh`; `refillToSb`, `refillToCbb`;
`limitedBy` (`QUARTERLY_SHARE` \| `SB_CAP` \| `SB_FLOOR_DEFICIT` \| `CBB_CAP` \| `AVAILABLE_BALANCE` \| `NONE`);
`explanation`.

**FailureMark**: `quarter`, `account`, `condition` (plain words).

### TaxSection (FR-060–FR-062)

`billThisYear`: TaxComputation with `basis` (`PRIOR_YEAR` \| `START_YEAR_ESTIMATE`); `generatedThisYear`:
TaxComputation (sets next year's bill); `simplifications`: list of strings.

**TaxComputation**: `components`: list of `{ name, amount, taxedShare, taxable }`; `taxableIncome`; `rate`; `tax`.

### NextYearSection (FR-070–FR-072)

`year`, `age`; `expenses`, `income`: list of `{ label, amount, exact: Boolean, change: Decimal (vs this year) }`;
`incomeGap`: `{ amount, exact = false }` or null in working years; `changes`: list of strings (retirement starts,
Social Security starts, Medicare starts, loan starts/ends, CBB cap steps down).

## Unchanged

`SimulationResult`, `YearlyResult`, `QuarterlyResult`, `BreakdownRequest` and Monte Carlo models don't change.
