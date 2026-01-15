# Feature Specification: Retirement Cash Flow Simulator

**Feature Branch**: `002-retirement-cashflow`
**Created**: 2026-01-06
**Last Updated**: 2026-01-15
**Status**: Implementation Complete
**Input**: User description: "Build an application that can help me visualize future retirement cash flow, for 35 years, in various market conditions, which consists of inflation and s&p performance.
* Allow a preset collection of common market conditions. Also, allow user to specify market conditions.
* The input variables are current year, current age, age at retirement, current amount in spend bucket (abbreviated as SB), crash buffer bucket (abbreviated as CBB) and equities portfolio, which consisting of taxable brokerage account (abbreviated as TBA), tax-deferred account (abbreviated as TDA) and tax-free account (abbreviated as TFA), property tax.
* Following are user-configuration values to control particular cash flow simulation:
  * retirement age
  * current age of both spouses
  * lower earning spouse social security claiming age, and benefit amount
  * higher earning spouse social security claiming age, and benefit amount. both should be more than lower earning spouses's.
  * when higher earning spouse claims social security, the lower earning spouse's benefit amount adjusts upwards to 50% of the higher earning spouse's benefit
  * inflation rate - assumed to be static throughout the 35 years of simulation
  * following expenses:
    * initial annual Needs expense
    * initial annual Wants expense
    * initial annual property tax
    * annual health insurance at retirement from ACA marketplace until medicare eligible age
    * annual health insurance at medicare eligible age, 65
  * Initial Annual TDA withdrawal amount (abbreviated ATDAW). The Quarterly equivalent (QTDAW) is computed as ATDAW * 0.25
  * Pre-retirement equities growth rate
  * Post-retirement equities growth rate
  * Bonds portfolio yield
  * HYSA interest rate
* The information should be presented in a table with 4 groups, account balances, income, regular yearly expenses and one time expenses.
  * Account balances group shows balances of the SB, CBB, TBA, TDA & TFA
  * SB, consists of HYSA, generates interest income accrued monthly and credited to the SB **Quarterly** on the 1st day of the next quarter. 
  * CBB, consists of a bonds portfolio, generates dividend income accrued monthly and credited **Quarterly**, to the SB, on the 1st day of the next quarter.
  * TBA, TDA and TFA, consists of an equities portfolio grows at the configured growth rate
  * Salary is received monthly from current date till year of retirement. assume retirement at end of year in which retirement age is reached. salary is inflation adjusted
  * **Pre-Retirement Logic**: Prior to retirement, Wants expenses are dynamically adjusted so that Salary income exactly covers Total Expenses (including taxes), effectively saving all Interest and Dividend income.
  * Income group should consist of the following columns - salary, interest, dividends, TBA withdrawal, TDA withdrawal, Roth conversion, social security. Total Income should be a separate column.
  * The expense group should consist of the following columns - Needs, Wants, health insurance, income tax, property tax. Annual Income Gap (abbreviated AIG) and Total expenses should be separate columns. All expense values are inflation adjusted every year.
  * Each year, the AIG is computed as the difference between the Total projected annual expenses minus recurring income (interest, dividend and social security).
    * **Post-Retirement Interest Estimation**: For AIG purposes, interest is estimated assuming the Spend Bucket holds approximately 50% of annual expenses on average (due to annual refill).
  * **Monthly SB Operations**:
    * Passive income (interest, dividends, Social Security) is deposited into SB
    * Monthly SB Withdrawal = AIG / 12 (covers only the gap, since passive income is already deposited)
    * This ensures the SB is depleted by the Income Gap amount, not total expenses
  * SB balance should be capped at 2 years worth of Cap AIG (SB Cap = 2 × Cap AIG, where Cap AIG uses 50% of Wants)
    * the SB balance is allowed to exceed the cap, only from dividend income flowing in from the CBB
  * CBB balance should be capped at 7 years worth of initial Cap AIG (CBB Cap = 7 × Base Cap AIG initially)
    * CBB Cap is NOT inflation adjusted - it uses the base Cap AIG from initial config
    * CBB Cap reduces by 1× Base Cap AIG at each milestone age: 65, 70, 75, 80, 85
  * In years that the market is up, the AIG will be covered by sales of equities in the TBA as well as TDA in years in which the markets are up.
  * In years that the market is down, the AIG will be covered by the balance in the SB or CBB
  * How the AIG is covered each year is called the Spending Strategy
  * Total Income and Total Expense columns should match every year.
* Spending Strategy in retirement, which can be named, to draw down money from various accounts to meet annual expenses, should be user selectable. Initially only 1 spending strategy will be supported, called Partha's Spending Strategy-v0.01-20260105, described below:
  * withdrawal decisions for spending will be made and executed on the 1st business day of each quarter
  * S&P is treated as the benchmark for market performance
  * Market performance "relative to 12 months prior" is calculated as Point-to-Point: current market value vs. value exactly 12 months prior.
  * If an account balance is at 0, it can't be used for withdrawal in subsequent years
  * **If SB balance already exceeds the SB Cap, no withdrawal from TBA or TDA is needed for SB refill**
  * if market is >= 95% (default, but configurable number) of 12 months prior and market >= 85% of all time highs then
    * if CBB is full then
      * determine the quarterly withdrawal (QW) from TBA and TDA, which will be the lesser of 25% of AIG and SB depletion (SB cap minus current SB balance). If SB depletion is <= 0 (SB at or above cap), QW = 0 and no withdrawal occurs.
      * Move Money from TDA and TBA to SB as follows (only if QW > 0):
        * withdraw from TDA = MIN (inflation adjusted QTDAW, QW, TDA balance), and move to SB
        * withdraw from TBA = MIN(QW - TDA withdrawal, TBA balance, 0), move to SB
        * if TDA withdrawal + TBA withdrawal < QW, mark the simulation as failed, and stop further computations
    * else
      * determine the Quarterly Withdrawal (QW) from TBA and TDA, which will be the lesser of 12.50% of AIG and SB depletion (SB cap minus current SB balance). If SB depletion is <= 0 (SB at or above cap), QW = 0 and no withdrawal occurs.
      * Move Money from TDA and TBA to SB as follows (only if QW > 0):
        * withdraw from TDA = MIN (inflation adjusted QTDAW, QW, TDA balance), and move to SB
        * withdraw from TBA = MIN(QW - TDA withdrawal, TBA balance, 0), move to SB
        * if TDA withdrawal + TBA withdrawal < QW, mark the simulation as failed, and stop further computations
      * Move Money from TDA and TBA to CBB as follows (only if CBB needs refilling):
        * determine the quarterly withdrawal (QW to CBB) = MIN(12.5% of AIG, CBB cap - current CBB balance)
        * withdraw from TDA = MIN (inflation adjusted QTDAW, QW to CBB, TDA balance), and move to CBB
        * withdraw from TBA = MIN(QW to CBB - TDA withdrawal, TBA balance, 0), move to CBB
        * if TDA withdrawal + TBA withdrawal < QW to CBB, mark the simulation as failed, and stop further computations
  * else if SB > 6 months worth AIG then
    * No money movement needed
  * else
    * Recompute the AIG by reducing Wants component by 10%. this will be the Quarterly Withdrawal (QW)
    * if CB balance >= 90% of 12 month prior balance then
      * Move Money from CBB to SB as follows:
        * determine the quarterly withdrawal (QW) from CBB, which will be the lesser of 25% of AIG and SB depletion (SB cap minus current SB balance)
        * withdraw from CBB = MIN (CBB Balance, QW), and move to SB
    * else if CBB loss (calculated as Cap Delta: difference between current balance and 4-year AIG cap) < loss in TDA and TBA, combined then
      * Move Money from CBB to SB as follows:
        * determine the quarterly withdrawal (QW) from CBB, which will be the lesser of 25% of AIG and SB depletion (SB cap minus current SB balance)
        * withdraw from CBB = MIN (CBB Balance, QW), and move to SB
    * else
      * Move Money from TDA and TBA to SB as follows:
        * withdraw from TDA = MIN (inflation adjusted QTDAW, QW, TDA balance), and move to SB
        * withdraw from TBA = MIN(QW - TDA withdrawal, TBA balance, 0), move to SB
        * if TDA withdrawal + TBA withdrawal < QW, mark the simulation as failed, and stop further computations
* There should be a row for the **Start Year Q4 (Initial State)**, followed by rows for each quarter/year starting from **Next Year** till age 85.
* The results table should support toggling between **Yearly** and **Quarterly** views.
* Summarize the ending account balances and total.
* If any of the account balances gets 0 or less, before the age 85, mark the simulation a failure.
* Allow persisting all simulations, their input conditions, and the resulting cash flow. Should allow user to name a simulation.
* There should be a page with a listing of saved simulations, with date it was run, and a name, if the user chose to name it.
* The saved simulation should be reloadable.
* The application should have a separate section to run a monte-carlo simulation of at least 1000 combinations of inflation and sequence of market returns, using the same user-configuration values as in the interactive simulation (except, of course, the equities growth rates, which is the same thing as market return in a given year, and Bond yields, which will be the variables in each of the 1000 simulations)
  * each simulation should compute cash flow till age 85,
  * Plot each simulation on a graph, with total account balance as X-axis, and year as Y-axis.
  * Highlight the median, 75th percentile and 90th percentile.
  * The chart should be user interactive. Hovering, with a mouse, on a particular path, should visually highlight it, and clicking on it should load only summary metrics (ending balance, failure year, etc.), not the full table.
  * There should be a separate "Load Details" button, clicking which loads the cash flow details below the chart, along with the variables for that simulation (market return, bond yield, inflation, as separate rows)"

## Implementation Notes (Updated 2026-01-15)

### Cap AIG Calculation
- **Cap AIG** uses 50% of Wants (more conservative than regular AIG which uses 100% of Wants)
- **Cap AIG Formula**: `(Needs + 50%Wants + Healthcare + PropertyTax + IncomeTax) - PassiveIncome`
- Passive income IS deducted - caps represent cashflow needed to cover expenses that passive income doesn't cover
- **SB Cap** = Cap AIG × 2 (2 years of coverage)
- **CBB Cap** = 7 × Base Cap AIG initially (not inflation adjusted), reduces by 1× Base Cap AIG at ages 65, 70, 75, 80, 85

### CBB Cap Reduction Logic
- Initial CBB Cap = 7 × Base Cap AIG (calculated from initial config values, NOT inflation adjusted)
- At age 65: CBB Cap reduces by 1× Base Cap AIG (now 6× remaining)
- At age 70: CBB Cap reduces by 1× Base Cap AIG (now 5× remaining)
- At age 75: CBB Cap reduces by 1× Base Cap AIG (now 4× remaining)
- At age 80: CBB Cap reduces by 1× Base Cap AIG (now 3× remaining)
- At age 85: CBB Cap reduces by 1× Base Cap AIG (now 2× remaining)

### TDA Withdrawals - Separated by Purpose
- **TDA for Spending** (`tdaWithdrawalSpend`): Withdrawals that go to SB via spending strategy (post-retirement only)
- **TDA for Roth Conversion** (`tdaWithdrawalRoth`): Direct TDA→TFA transfers, NOT via spending strategy
- Total TDA Withdrawal = TDA for Spending + TDA for Roth

### Roth Conversion Timing
- Roth conversions begin in **Year 2 of the simulation** (not Year 1)
- Works both **pre-retirement and post-retirement**
- Pre-retirement: Direct TDA → TFA withdrawal
- Post-retirement: Direct TDA → TFA withdrawal (separate from spending strategy)
- Does NOT flow through the Spend Bucket (SB)
- Inflation-adjusted each year

### Interest and Dividend Crediting
- Interest and dividends accrue monthly but are credited **quarterly** (on the 1st day of each quarter)
- This matches the original spec for quarterly crediting

### UI Enhancements
- Age 75 rows are highlighted with yellow background (#fff9c4)
- Failure rows are highlighted with red background (#ffebee) - takes priority over age highlight
- TDA Withdrawal column shows breakdown: Total, TDA for Spend, TDA for Roth
- Detailed computation breakdown available via info icon for each row

### API Versioning
- Build number auto-increments with each successful build
- Version displayed as `{majorVersion}.{minorVersion}.{buildNumber}` (e.g., 1.1.6)
- Build time and server start time included in API response

## Clarifications

### Session 2026-01-06
- Q: How should "Income Tax" be calculated given it's a required column? → A: Flat Rate / Manual: User inputs a single effective tax rate (default 0%) applied to taxable income sources.
- Q: When Social Security adjusts for the lower earner, what is the new benefit amount? → A: 50% Spousal: Lower earner's benefit is adjusted to 50% of the higher earner's amount.
- Q: How is "market performance relative to 12 months prior" calculated? → A: Point-to-Point: Current market value vs. Value exactly 12 months prior.
- Q: How frequently should HYSA interest be credited? → A: Monthly: Interest is calculated and added to the SB balance every month.
- Q: How is "CBB loss (relative to CBB cap)" calculated? → A: Cap Delta: Difference between current CBB balance and its theoretical cap (4 years AIG).

## User Scenarios & Testing

### User Story 1 - Configure and Run Interactive Simulation (Priority: P1)

As a user planning for retirement, I want to input my financial details and run a year-by-year cash flow simulation so that I can visualize my financial health until age 85.

**Why this priority**: Core functionality of the application. Without this, no other features matter.

**Independent Test**: Can be tested by verifying that specific inputs produce the expected calculated outputs in the table.

**Acceptance Scenarios**:

1. **Given** default configuration values, **When** the user clicks "Run Simulation", **Then** a table is displayed showing year-by-year account balances, income, and expenses until age 85 (or failure).
2. **Given** a specific "Spending Strategy" (Partha's v0.01), **When** the simulation runs through a market downturn, **Then** the logic correctly withdraws from the Cash Buffer Bucket (CBB) or Spend Bucket (SB) according to the defined rules.
3. **Given** an account balance reaches zero before age 85, **When** the simulation continues, **Then** the simulation is marked as a "Failure" and the year of failure is highlighted.

---

### User Story 2 - Save and Load Simulations (Priority: P2)

As a user, I want to save my simulation inputs and results so that I can compare different scenarios and revisit them later.

**Why this priority**: Allows users to manage multiple scenarios without re-entering complex data.

**Independent Test**: Save a simulation, clear inputs, load it back, and verify inputs match.

**Acceptance Scenarios**:

1. **Given** a completed simulation, **When** the user enters a name and clicks "Save", **Then** the simulation is persisted to the list of saved simulations.
2. **Given** a list of saved simulations, **When** the user clicks "Load" on a specific entry, **Then** the configuration inputs are repopulated with the saved values and the results are displayed.

---

### User Story 3 - Monte Carlo Analysis (Priority: P2)

As a user, I want to run a Monte Carlo simulation with 1000+ iterations of market conditions so that I can understand the probability of my retirement plan's success.

**Why this priority**: Provides a robust statistical view of retirement safety beyond a single deterministic run.

**Independent Test**: Run Monte Carlo and verify that 1000 distinct paths are generated and plotted.

**Acceptance Scenarios**:

1. **Given** a configured simulation, **When** the user runs the Monte Carlo analysis, **Then** 1000 simulations are executed using variable inflation, bond yields, and market returns.
2. **Given** the Monte Carlo results, **When** displayed, **Then** a chart shows all paths with the Median, 75th percentile, and 90th percentile clearly highlighted.
3. **Given** the chart, **When** the user hovers over a path, **Then** that path is visually highlighted.

---

### User Story 4 - Detailed Drill-down of Monte Carlo Paths (Priority: P3)

As a user, I want to inspect the details of a specific Monte Carlo run so that I can understand why a particular scenario failed or succeeded.

**Why this priority**: Enhances trust in the black-box Monte Carlo results.

**Independent Test**: Select a path on the chart and verify the detail table loads below.

**Acceptance Scenarios**:

1. **Given** a Monte Carlo chart, **When** the user clicks on a specific simulation path, **Then** summary metrics for that run are displayed.
2. **Given** a selected simulation path, **When** the user clicks "Load Details", **Then** the full cash flow table and specific variable set (inflation, returns) for that run are displayed below the chart.

### Edge Cases

- **Zero Initial Balance**: What happens if the user starts with 0 in all accounts? Simulation should likely fail immediately or show 0s.
- **Market Crash Year 1**: How does the strategy handle a crash immediately upon retirement?
- **Deflation**: How does the system handle negative inflation rates?
- **Missing Data**: What if user leaves required fields (like Age) blank?

## Requirements

### Technical Constraints
- **TC-001**: System MUST utilize an N-tier architecture consisting of:
    - **Frontend**: React (Vite, TypeScript, MUI)
    - **Backend (BFF)**: Deno (TypeScript, Oak)
    - **API Server**: Kotlin (JVM, Ktor) for simulation logic and data access.

### Functional Requirements

#### Inputs & Configuration
- **FR-001**: System MUST allow users to input the following variables:
    - Current Year, Current Age, Retirement Age.
    - Annual Salary (pre-retirement, inflation-adjusted each year).
    - Current Amounts: Spend Bucket (SB), Crash Buffer Bucket (CBB), Equities Portfolio (Taxable - TBA, Tax-Deferred - TDA, Tax-Free - TFA).
    - Annual Contributions: 401k (pre-tax), Taxable Brokerage contributions.
    - Spousal Details: Current age, Social Security claiming age and benefit amount for both spouses (Lower/Higher earning logic).
    - Expenses: Initial annual Needs, Wants, property tax.
    - **Healthcare**: Annual insurance cost (ACA `healthcarePreMedicare` from retirement age until age 65, Medicare `healthcareMedicare` at 65+). Both are inflation-adjusted.
    - Rates: Inflation (static for single run), Pre/Post-retirement equity growth, Bond yield, HYSA interest, and **Effective Income Tax Rate**.
    - Strategy Params: Initial Annual TDA withdrawal (ATDAW), Roth conversion amount, market thresholds.
- **FR-002**: System MUST allow selecting a "Spending Strategy" (Initially supporting "Partha's Spending Strategy-v0.01-20260105").
- **FR-003**: System MUST allow users to specify market conditions or choose from a preset collection.

#### Simulation Logic
- **FR-004**: System MUST compute the Annual Income Gap (AIG) each year: Total projected annual expenses - Recurring Income (Interest + Dividends + Social Security).
- **FR-004a**: System MUST compute the Cap AIG (for SB/CBB caps) using 50% of Wants: `(Needs + 50%Wants + Healthcare + PropertyTax + IncomeTax) - PassiveIncome`
- **FR-005**: System MUST implement "Partha's Spending Strategy-v0.01-20260105" for covering the AIG:
    - Decisions made quarterly on 1st business day.
    - Logic checks market performance (S&P benchmark) relative to 12-month prior (Point-to-Point) and all-time highs.
    - Logic handles withdrawals from TBA/TDA based on CBB status (Full vs. Not Full) and Market status (Up vs. Down).
    - Logic manages transfers between accounts (TDA/TBA -> SB, TDA/TBA -> CBB, CBB -> SB).
    - Logic applies spending cuts (Wants reduced by 10%) if SB is low.
    - Logic respects SB Cap (2 × Cap AIG) and CBB Cap (see FR-005a).
- **FR-005a**: CBB Cap MUST be calculated as:
    - Initial CBB Cap = 7 × Base Cap AIG (from initial config, NOT inflation adjusted)
    - Reduces by 1× Base Cap AIG at each of these ages: 65, 70, 75, 80, 85
    - Minimum CBB Cap = 0
- **FR-005b**: TDA withdrawals MUST be tracked separately by purpose:
    - `tdaWithdrawalSpend`: Withdrawals for spending (go to SB via spending strategy, post-retirement only)
    - `tdaWithdrawalRoth`: Withdrawals for Roth conversion (direct TDA→TFA, NOT via SB)
    - Total TDA Withdrawal = tdaWithdrawalSpend + tdaWithdrawalRoth
- **FR-005c**: Roth conversions MUST:
    - Begin in Year 2 of the simulation (not Year 1)
    - Work both pre-retirement and post-retirement
    - Transfer directly from TDA to TFA (NOT through SB)
    - Be inflation-adjusted each year
- **FR-006**: System MUST simulate monthly Salary income until retirement.
- **FR-007**: System MUST adjust Spousal Social Security benefits (step-up logic) to 50% of the higher earner's benefit when the higher earner claims, if it is greater than the lower earner's existing benefit.
- **FR-008**: System MUST fail the simulation if any account balance drops to <= 0 before age 85.

#### Outputs & Visualization
- **FR-009**: System MUST display a results table with the following groups:
    - **Account Balances**: SB, SB Cap, CBB, CBB Cap, TBA, TDA, TFA, Total Portfolio.
    - **Income**: Salary, Interest, Dividends, Social Security, TBA Withdrawal, TDA Withdrawal (with breakdown: TDA for Spend, TDA for Roth), Roth Conversion, Total Income.
    - **Expenses**: Needs, Wants, Healthcare, Property Tax, Income Tax, Total Expenses.
    - **Metrics**: Annual Income Gap (AIG), 401k Contribution, TBA Contribution.
- **FR-009a**: Age 75 rows MUST be visually highlighted (yellow background).
- **FR-009b**: Failure rows MUST be visually highlighted (red background, takes priority over age highlighting).
- **FR-009c**: System MUST provide a detailed computation breakdown for each row (accessible via info icon).
- **FR-010**: System MUST summarize ending balances and total/failure status.

#### Persistence
- **FR-011**: System MUST allow saving the current simulation (Inputs + Result) with a user-provided name.
- **FR-012**: System MUST list saved simulations with Date and Name.
- **FR-013**: System MUST allow reloading a saved simulation.

#### Monte Carlo
- **FR-014**: System MUST run at least 1000 simulations using the current configuration but with variable Inflation and Market Returns (Equities/Bonds).
- **FR-015**: System MUST plot all 1000 paths on a line chart (X: Year, Y: Total Balance).
- **FR-016**: Chart MUST highlight Median, 75th Percentile, and 90th Percentile lines.
- **FR-017**: Chart MUST support interactive hovering to highlight individual paths.
- **FR-018**: System MUST allow loading details (Table + Specific Variables) for a selected Monte Carlo run.

### Key Entities

- **SimulationConfig**: Holds all user input parameters (ages, balances, rates, expenses).
- **MarketScenario**: A sequence of annual market returns (S&P) and inflation rates.
- **YearlyResult**: The computed state for a single year (balances, cash flows, metrics).
- **CashFlow**: Income and expense details for a period, including:
    - Income: salary, interest, dividends, socialSecurity, tbaWithdrawal, tdaWithdrawal, tdaWithdrawalSpend, tdaWithdrawalRoth, rothConversion, totalIncome
    - Expenses: needs, wants, healthcare, propertyTax, incomeTax, totalExpenses
    - Operations: sbDeposit, sbWithdrawal, contribution401k, contributionTba
- **Metrics**: Calculated values including annualIncomeGap, sbCap, cbbCap, isFailure
- **SimulationRun**: A complete execution consisting of `SimulationConfig` and a list of `YearlyResult`s.
- **MonteCarloSession**: A collection of 1000+ `SimulationRun`s and aggregate statistics.
- **ApiMetadata**: Version info (version, buildTime, serverStartTime) included in API responses.

## Success Criteria

### Measurable Outcomes

**Performance Baseline**: All performance metrics assume a standard development machine (4-core CPU, 8GB RAM).

- **SC-001**: Users can run a full 35-year interactive simulation with API response time under 500ms (P95).
- **SC-002**: Monte Carlo simulation (1000 runs):
  - API computation time < 8 seconds (P95)
  - Frontend chart render time < 2 seconds (P95)
- **SC-003**: The "Spending Strategy" logic correctly handles 100% of defined test cases (e.g., market crash scenario triggers CBB withdrawal).
- **SC-004**: Users can save and reload a simulation with 100% data integrity.
- **SC-005**: The visualization clearly distinguishes between "Safe" (Median/75th) and "Risk" (90th/Failure) outcomes in the Monte Carlo chart.

### Assumptions

- **Inflation**: For the single interactive simulation, inflation is static. For Monte Carlo, it varies.
- **Interest & Dividends**: HYSA interest and bond dividends accrue monthly but are credited **quarterly** (on the 1st day of each quarter) to the SB.
- **Roth Conversions**: Begin in Year 2 of simulation. Direct TDA→TFA transfer, does NOT flow through SB.
- **Taxation**: Income Tax is calculated as a flat percentage (Effective Income Tax Rate) applied to taxable income sources. Taxable income includes:
  - Salary (100%)
  - Interest (100%)
  - Dividends (100%)
  - Social Security (100% for model simplicity)
  - TDA Distributions (100%)
  - Roth Conversions (100%)
  - TBA Withdrawals (50% taxable, assuming 50% cost basis)
- **Market Data**: Historical data provided (CSV) will be used to seed or drive the "preset" market conditions.