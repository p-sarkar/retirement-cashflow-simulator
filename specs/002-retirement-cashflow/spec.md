# Feature Specification: Retirement Cash Flow Simulator

**Feature Branch**: `002-retirement-cashflow`
**Created**: 2026-01-06
**Last Updated**: 2026-10-18
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
    * *(Superseded 2026-10-18; documents existing behaviour.)* The simulation doesn't use ATDAW/QTDAW. Each equity withdrawal instead takes the **TDA withdrawal percentage** (`strategy.tdaWithdrawalPercentage`, default 50%) of the amount from the TDA and the rest from the TBA, each limited by its balance. Read every "MIN (inflation adjusted QTDAW, …)" below as "the TDA withdrawal percentage of the amount".
  * Pre-retirement equities growth rate
  * Post-retirement equities growth rate
  * Bonds portfolio yield
  * HYSA interest rate
* The information should be presented in a table with 4 groups, account balances, income, regular yearly expenses and one time expenses.
  * *(Note 2026-10-18: this is the original request; the results table's current groups and columns are in FR002-009, including the Market group and TFA Withdrawal.)*
  * Account balances group shows balances of the SB, CBB, TBA, TDA & TFA
  * SB, consists of HYSA, generates interest income accrued monthly and credited to the SB **Quarterly** on the 1st day of the next quarter. 
  * CBB, consists of a bonds portfolio, generates dividend income accrued monthly and credited **Quarterly**, to the SB, on the 1st day of the next quarter.
  * TBA, TDA and TFA, consists of an equities portfolio grows at the configured growth rate
  * Salary is received monthly from current date till year of retirement. assume retirement at end of year in which retirement age is reached. salary is inflation adjusted
  * **Working Years**: Every year up to and including the year the retirement age is reached is a working year (salary for the full year, pre-retirement healthcare, pre-retirement growth rate). In working years, Wants expenses are dynamically adjusted so that take-home salary (salary minus 401k, Roth and TBA contributions) exactly covers Total Expenses (including taxes), effectively saving all Interest and Dividend income.
    * Roth conversions are NOT deducted when adjusting Wants: they move money directly from the TDA to the TFA without passing through the SB, and their tax is part of the following year's income tax.
  * Income group should consist of the following columns - salary, interest, dividends, TBA withdrawal, TDA withdrawal, Roth conversion, social security. Total Income should be a separate column.
  * The expense group should consist of the following columns - Needs, Wants, health insurance, income tax, property tax. Annual Income Gap (abbreviated AIG) and Total expenses should be separate columns. All expense values are inflation adjusted every year.
  * Each retirement year, the AIG is computed as the difference between the Total projected annual expenses minus recurring income (interest, dividend and social security).
    * Working years have no AIG, because salary covers expenses; the AIG column shows `~`. The Start Year row is a balance snapshot with no cash flows, so it also shows `~`.
    * **Post-Retirement Interest Estimation**: For AIG purposes, interest is estimated assuming the Spend Bucket holds approximately 50% of annual expenses (excluding income tax) on average, due to the refills, whatever its balance at the start of the year. In working years the estimate uses the current SB balance.
  * **Quarterly SB Operations**:
    * Take-home salary and passive income (interest, dividends, Social Security) are deposited into SB in full
    * Quarterly SB Withdrawal = Total regular expenses / 4 (Needs + Wants + Healthcare + Property Tax + Income Tax); one-time expenses are withdrawn as they fall due
    * Because income is deposited in full, the SB's net change from regular cash flows is income minus expenses, i.e. it is depleted by the Income Gap amount. (Withdrawing only the AIG would count passive income twice.)
    * The SB reconciles every year: start balance + deposits − withdrawals = end balance
  * SB balance should be capped at 2 years worth of Cap AIG (SB Cap = 2 × Cap AIG, where Cap AIG uses 50% of Wants)
    * The SB Cap applies in retirement years only. In working years, and for the Start Year when it is a working year, the SB Cap column shows `~`.
    * **SB Floor** = the larger of 6 months of the full AIG and 1 quarter of total regular expenses (including income tax). The SB Cap is never below the SB Floor.
      * Why: when passive income covers most expenses, Cap AIG can be zero or negative while the SB still drains by the AIG. And because interest and dividends are credited at the start of each quarter while expenses are paid through it, the SB must hold a quarter's spending to stay positive at quarter end.
    * **SB Deficit** = SB Floor minus current SB balance, if positive. Every quarterly refill into the SB (from TBA/TDA or from the CBB) adds the SB Deficit to its usual amount, still limited by SB depletion, so an SB that has fallen below its floor is restored in one quarter.
    * the SB balance is allowed to exceed the cap, only from dividend income flowing in from the CBB
  * CBB balance should be capped at 7 years worth of initial Cap AIG (CBB Cap = 7 × Base Cap AIG initially)
    * CBB Cap is NOT inflation adjusted - it uses the base Cap AIG from initial config
    * CBB Cap reduces by 1× Base Cap AIG at each milestone age: 65, 70, 75, 80, 85
  * In years that the market is up, the AIG will be covered by sales of equities in the TBA as well as TDA in years in which the markets are up.
  * In years that the market is down, the AIG will be covered by the balance in the SB or CBB
    * *(Clarified 2026-10-18, #21, #23.)* When the SB is at its floor and the CBB has fallen below 90% of its balance 12 months earlier and lost at least as much as the equities, TDA/TBA refill the SB; whatever TDA and TBA can't pay comes from the TFA.
  * How the AIG is covered each year is called the Spending Strategy
  * Total Income and Total Expense columns should match every year.
* Spending Strategy in retirement, which can be named, to draw down money from various accounts to meet annual expenses, should be user selectable. Initially only 1 spending strategy will be supported, called Partha's Spending Strategy-v0.01-20260105, described below:
  * withdrawal decisions for spending will be made and executed on the 1st business day of each quarter
  * S&P is treated as the benchmark for market performance
  * Market performance "relative to 12 months prior" is calculated as Point-to-Point: current market value vs. value exactly 12 months prior.
  * If an account balance is at 0, it can't be used for withdrawal in subsequent years
  * *(Clarified 2026-10-18, #23.)* Where the steps below say "if TDA withdrawal + TBA withdrawal < QW, mark the simulation as failed, and stop further computations": for a refill into the SB, the rest is withdrawn from the TFA (FR002-005); a refill into the CBB is simply smaller. A short withdrawal doesn't stop the simulation by itself; whether and when a plan fails is decided by FR002-008.
  * **If SB balance already exceeds the SB Cap, no withdrawal from TBA or TDA is needed for SB refill**
  * if market is >= 95% (default, but configurable number) of 12 months prior and market >= 85% of all time highs then
    * if CBB is full then
      * determine the quarterly withdrawal (QW) from TBA and TDA, which will be the lesser of (25% of AIG + SB Deficit) and SB depletion (SB cap minus current SB balance). If SB depletion is <= 0 (SB at or above cap), QW = 0 and no withdrawal occurs.
      * Move Money from TDA and TBA to SB as follows (only if QW > 0):
        * withdraw from TDA = MIN (inflation adjusted QTDAW, QW, TDA balance), and move to SB
        * withdraw from TBA = MIN(QW - TDA withdrawal, TBA balance, 0), move to SB
        * if TDA withdrawal + TBA withdrawal < QW, mark the simulation as failed, and stop further computations
    * else
      * determine the Quarterly Withdrawal (QW) from TBA and TDA, which will be the lesser of (12.50% of AIG + SB Deficit) and SB depletion (SB cap minus current SB balance). If SB depletion is <= 0 (SB at or above cap), QW = 0 and no withdrawal occurs.
      * Move Money from TDA and TBA to SB as follows (only if QW > 0):
        * withdraw from TDA = MIN (inflation adjusted QTDAW, QW, TDA balance), and move to SB
        * withdraw from TBA = MIN(QW - TDA withdrawal, TBA balance, 0), move to SB
        * if TDA withdrawal + TBA withdrawal < QW, mark the simulation as failed, and stop further computations
      * Move Money from TDA and TBA to CBB as follows (only if CBB needs refilling):
        * determine the quarterly withdrawal (QW to CBB) = MIN(12.5% of AIG, CBB cap - current CBB balance)
        * withdraw from TDA = MIN (inflation adjusted QTDAW, QW to CBB, TDA balance), and move to CBB
        * withdraw from TBA = MIN(QW to CBB - TDA withdrawal, TBA balance, 0), move to CBB
        * if TDA withdrawal + TBA withdrawal < QW to CBB, mark the simulation as failed, and stop further computations
  * else if SB > SB Floor then
    * No money movement needed
  * else
    * Recompute the AIG by reducing Wants component by 10%. this will be the Quarterly Withdrawal (QW)
      * *(Clarified 2026-10-18, #22.)* The cut is 10% of the year's inflation-adjusted Wants, not of the configured (today's dollars) amount.
    * if CB balance >= 90% of 12 month prior balance then
      * Move Money from CBB to SB as follows:
        * determine the quarterly withdrawal (QW) from CBB, which will be the lesser of (25% of the reduced AIG above + SB Deficit) and SB depletion (SB cap minus current SB balance)
        * withdraw from CBB = MIN (CBB Balance, QW), and move to SB
    * else if CBB loss (calculated as Cap Delta: difference between current balance and the CBB Cap, FR002-005a) < loss in TDA and TBA, combined then
      * *(Defined 2026-10-18, #21.)* The CBB's balance 12 months earlier is its balance at the start of the quarter 4 quarters before; before the simulation has a year of history, the CBB is taken as flat at its starting balance. The loss in TDA and TBA, combined, is what they lost to the market over the last 12 months: (TDA + TBA) × (market 12 months earlier ÷ market now − 1), or 0 if the market rose. The CBB loss is CBB Cap − CBB balance.
      * Move Money from CBB to SB as follows:
        * determine the quarterly withdrawal (QW) from CBB, which will be the lesser of (25% of the reduced AIG above + SB Deficit) and SB depletion (SB cap minus current SB balance)
        * withdraw from CBB = MIN (CBB Balance, QW), and move to SB
    * else
      * Move Money from TDA and TBA to SB as follows, the same amount as a CBB refill (25% of the reduced AIG + SB Deficit, limited by SB depletion):
        * withdraw from TDA = MIN (inflation adjusted QTDAW, QW, TDA balance), and move to SB
        * withdraw from TBA = MIN(QW - TDA withdrawal, TBA balance, 0), move to SB
        * if TDA withdrawal + TBA withdrawal < QW, mark the simulation as failed, and stop further computations
* There should be a row for the **Start Year Q4 (Initial State)**, followed by rows for each quarter/year starting from **Next Year** till age 85.
* The results table should support toggling between **Yearly** and **Quarterly** views.
* Summarize the ending account balances and total.
* If any of the account balances gets 0 or less, before the age 85, mark the simulation a failure.
  * *(Superseded 2026-10-18: see FR002-008 for the failure conditions and End Age.)*
* Allow persisting all simulations, their input conditions, and the resulting cash flow. Should allow user to name a simulation.
* There should be a page with a listing of saved simulations, with date it was run, and a name, if the user chose to name it.
* The saved simulation should be reloadable.
* The application should have a separate section to run a monte-carlo simulation of at least 1000 combinations of inflation and sequence of market returns, using the same user-configuration values as in the interactive simulation (except, of course, the equities growth rates, which is the same thing as market return in a given year, and Bond yields, which will be the variables in each of the 1000 simulations)
  * each simulation should compute cash flow till age 85,
  * Plot each simulation on a graph, with total account balance as X-axis, and year as Y-axis.
  * Highlight the median, 75th percentile and 90th percentile.
  * The chart should be user interactive. Hovering, with a mouse, on a particular path, should visually highlight it, and clicking on it should load only summary metrics (ending balance, failure year, etc.), not the full table.
  * There should be a separate "Load Details" button, clicking which loads the cash flow details below the chart, along with the variables for that simulation (market return, bond yield, inflation, as separate rows)"

## Implementation Notes (Updated 2026-10-18)

### CBB Before the TFA (2026-10-19)
- **Stranded Crash Buffer** (T002-078–T002-080): an SB refill that TDA and TBA can't pay now comes from the CBB, in any market, before the TFA (FR002-005). A plan that ran out of equities in a steadily rising market used to fail with its CBB full. For example, a low-balance test plan failed in 2039 with $414k in the CBB; it now spends the CBB and fails in 2044. Of the reference results, only the Stagflation preset changes (failure 2049 → 2051). In a seeded 50-run Monte Carlo, 20 runs change and the success rate stays 66%.

### Empty CBB in a Downturn (2026-10-19)
- **Refill from an empty CBB** (T002-081–T002-083): a down-market refill that the CBB can't cover now comes from TDA/TBA, then the TFA (FR002-005). A Monte Carlo run of the default plan under the 2008 Crash preset failed in 2050 with $7.1M in the other accounts: four quarters in a row chose the empty CBB (its "loss" of $412k was below the equities' $529k) and moved nothing. In a 300-run Monte Carlo of that plan, success rises from 69.7% to 82.7% (Risk-Based Guardrails: 79.7% to 99.7%). Of the reference results, the six fixed-market plans are unchanged; in the seeded 50-run Monte Carlo, 22 runs change and success rises from 66% to 70%.

### Taxes, Market Rates and Spending Strategy Fixes (2026-10-16 to 2026-10-18)
- **Taxes and Total Income** (T002-063–T002-067; #13, #15, #16): 401k contributions are excluded from taxable income, Roth conversions are taxed once, and Total Income excludes Roth conversions.
- **Market rates in results** (T002-068–T002-069; FR002-009): every row carries the year's equity return, inflation and bond yield, shown as the table's last three columns and in the CSV.
- **Downturn source** (T002-070–T002-071; #21): with the SB at its floor in a down market, the CBB refills it while it is at least 90% of its balance 12 months earlier or has lost less than the equities; otherwise TDA/TBA do. The breakdown explains each choice.
- **Wants cut** (T002-072; #22): 10% of the year's inflation-adjusted Wants.
- **TFA as the last resort** (T002-073–T002-075; #23): the TFA pays what TDA and TBA can't, untaxed, shown as TFA Withdrawal and in Total Income.
- **Quarterly totals** (T002-076; #27): withdrawals that cover a one-time expense are in their quarter's columns, so the quarters add up to the year.

### Market Presets, Persistence, Monte Carlo (2026-10-15)
- **Market presets** (FR002-003): Historical Average, 2008 Crash, Bull Market, Stagflation, served by the API (`GET /api/market-presets`). Choosing one fills the inflation, growth and bond yield fields and greys them out (disabled, values still shown); Custom makes them editable again, starting from the preset's values *(changed 2026-10-19; previously editing one switched to Custom)*. The 2008 Crash also replays the S&P 500 returns of 2008–2012 in the first five retirement years.
- **Market performance vs 12 months prior** is now computed point-to-point from the simulated market index (value at the start of the quarter vs 4 quarters earlier; the market is taken as flat before the start). It was previously fixed at 100%, so only the all-time-high check could detect a down market. With static growth rates the result only changes when growth is below −5%.
- **Saved simulations** (FR002-011 to FR002-013): Save on the results asks for a name (default: the config's name). The Saved Simulations tab lists name, date run, outcome, ending balance and End Age, newest first; Load repopulates the form and shows the saved results exactly as saved.
- **Monte Carlo** (FR002-014 to FR002-018): 1000 runs by default. Each simulated year draws a random historical year (1928–2025; data extended 2026-10-19) and uses its S&P 500 return and inflation together, re-centred on the configured growth and inflation rates; bond yields vary around the configured yield (±1% standard deviation). Runs are seeded and reproducible. The chart plots every run (failed runs in red) with the median, 75% and 90% lines; hovering highlights the nearest run, clicking shows its summary, and Load Details rebuilds that run in full (its variables as rows and its cash flow table).
- **Percentile lines**: "75th percentile" and "90th percentile" are confidence levels, the balance that 75% / 90% of runs reach or exceed, so the 90% line is the conservative ("risk") line of SC002-005. A failed run counts as $0 after it fails.
- **CSV export** (FR002-010a): Export CSV on the results table downloads the current view (yearly or quarterly) with the table's columns, including each year's market rates (FR002-009) as percentages; not-applicable values (`~`) are empty.
- **Input validation** (Edge Case "Missing Data"): a cleared number field is sent as 0, so the API rejects (422, per field): Current Year outside 1900–2200, Current Age outside 18–109, Retirement and Spouse Age outside 18–110, Social Security claim ages outside 62–70, negative amounts, and unknown market presets. A request missing a required field is a 400.

## Implementation Notes (2026-01-20)

### UI Layout Improvements (2026-01-20)
- **Form Layout**: Simulation form reorganized into compact, columnar layout
  - Sections displayed as vertical columns (General, Social Security, Portfolio, etc.)
  - Columns flow left-to-right and wrap to new rows as needed
  - Small text fields (`size="small"`) with appropriate fixed widths
  - Vertical spacing increased to `1.5` between fields for readability
  - Form aligned to top of page (not vertically centered)
- **Accordion Behavior**: Form collapses automatically after successful simulation run
- **Page Alignment**: Removed default vertical centering from body and root elements
  - `body`: Removed `display: flex` and `place-items: center` from index.css
  - `#root`: Removed `padding: 2rem` and `text-align: center` from App.css
  - Container top margin reduced to `mt: 1` for minimal spacing from top
- **AccordionDetails**: Configured to align content to `flex-start` (top-left)

### Cap AIG Calculation
- **Cap AIG** uses 50% of Wants (more conservative than regular AIG which uses 100% of Wants)
- **Cap AIG Formula**: `(Needs + 50%Wants + Healthcare + PropertyTax + IncomeTax) - PassiveIncome`
- Passive income IS deducted - caps represent cashflow needed to cover expenses that passive income doesn't cover
- **SB Cap** = max(Cap AIG × 2 (2 years of coverage), SB Floor)
- **SB Floor** = max(AIG × 0.5, Total regular expenses × 0.25)
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
- Q: How is "CBB loss (relative to CBB cap)" calculated? → A: Cap Delta: Difference between current CBB balance and its theoretical cap (4 years AIG). *(Superseded 2026-10-18, #21: the cap is the CBB Cap of FR002-005a, 7 × Base Cap AIG with step-downs; CBB loss = CBB Cap − CBB balance.)*

### Session 2026-10-14
- Q: Should the simulation always end at age 85? → A: No. End Age becomes a setting on the configuration form (FR002-001a), default 85. Until it is implemented, the simulation is fixed at 85.

## User Scenarios & Testing

### User Story 1 - Configure and Run Interactive Simulation (Priority: P1)

As a user planning for retirement, I want to input my financial details and run a year-by-year cash flow simulation so that I can visualize my financial health until age 85.

**Why this priority**: Core functionality of the application. Without this, no other features matter.

**Independent Test**: Can be tested by verifying that specific inputs produce the expected calculated outputs in the table.

**Acceptance Scenarios**:

1. **Given** default configuration values, **When** the user clicks "Run Simulation", **Then** a table is displayed showing year-by-year account balances, income, and expenses until age 85 (or failure).
2. **Given** a specific "Spending Strategy" (Partha's v0.01), **When** the simulation runs through a market downturn, **Then** the logic correctly covers spending from the Spend Bucket (SB), the Crash Buffer Bucket (CBB), TDA/TBA, or as a last resort the TFA, according to the defined rules (FR002-005).
3. **Given** the plan runs out of money before End Age (FR002-008), **When** the simulation continues, **Then** the simulation is marked as a "Failure" and the year of failure is highlighted.

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
- **End Age at or below Current Age**: Rejected; there would be no years to simulate (FR002-001a).
- **End Age below Retirement Age**: Allowed; the projection simply stops before retirement.

## Requirements

### UI/UX Requirements
- **UI002-001**: Form MUST use a compact, columnar layout with sections displayed as vertical columns
- **UI002-002**: Form sections MUST flow left-to-right and wrap to new rows based on available width
- **UI002-003**: Form MUST align to the top of the page (not vertically centered)
- **UI002-004**: Text fields MUST use small size (`size="small"`) with appropriate fixed widths
- **UI002-005**: Vertical spacing between fields MUST be `1.5` for readability
- **UI002-006**: Form MUST be contained within an Accordion that collapses after successful simulation
- **UI002-007**: AccordionDetails MUST align content to flex-start (top-left)
- **UI002-008**: Page layout MUST NOT apply vertical centering to body or root elements
- **UI002-009**: Form sections MUST include: General, Social Security, Portfolio Balances, Annual Expenses, Annual Contributions, Economic Assumptions (%), Withdrawal Strategy
- **UI002-010**: Each section MUST have a bold subtitle2 header with consistent spacing

### Technical Constraints
- **TC002-001**: System MUST utilize an N-tier architecture consisting of:
    - **Frontend**: React (Vite, TypeScript, MUI)
    - **Backend (BFF)**: Deno (TypeScript, Oak)
    - **API Server**: Kotlin (JVM, Ktor) for simulation logic and data access.

### Functional Requirements

#### Inputs & Configuration
- **FR002-001**: System MUST allow users to input the following variables:
    - Current Year, Current Age, Retirement Age.
    - Annual Salary (pre-retirement, inflation-adjusted each year).
    - Current Amounts: Spend Bucket (SB), Crash Buffer Bucket (CBB), Equities Portfolio (Taxable - TBA, Tax-Deferred - TDA, Tax-Free - TFA).
    - Annual Contributions: 401k (pre-tax), Taxable Brokerage contributions.
    - Spousal Details: Current age, Social Security claiming age and benefit amount for both spouses (Lower/Higher earning logic).
    - Expenses: Initial annual Needs, Wants, property tax.
    - **Healthcare**: Annual insurance cost (ACA `healthcarePreMedicare` from the year after the retirement age until age 65, Medicare `healthcareMedicare` at 65+). Both are inflation-adjusted.
    - Rates: Inflation (static for single run), Pre/Post-retirement equity growth, Bond yield, HYSA interest, and **Effective Income Tax Rate**.
    - Strategy Params: TDA withdrawal percentage (the share of each equity withdrawal taken from the TDA; the rest from the TBA; default 50%), Roth conversion amounts before and after retirement. The market thresholds (95% of 12 months earlier, 85% of the all-time high, 90% for the CBB) are fixed. *(Updated 2026-10-18 to match the form; ATDAW is no longer used.)*
- **FR002-001a**: System MUST let users set an **End Age** in the General section of the configuration form: the age of the user (not the spouse) through which the simulation runs.
    - Default 85. Configs without it (saved simulations, API requests from older clients) use 85.
    - Valid values: a whole number greater than Current Age and no more than 110. The API rejects anything else with 422 and a field error on `endAge`; the form shows the error inline and blocks Run.
    - The simulated years are the year after Current Year through the year the user reaches End Age. Everything currently fixed at age 85 uses End Age instead: the results rows, the failure check (FR002-008), the results summary ("survived to age N"), Monte Carlo paths and success rate, and the one-time expense timing window (spec 003, FR002-041).
    - Unchanged: the CBB Cap reduction milestones (FR002-005a: 65, 70, 75, 80, 85) and the Medicare age (65) stay fixed. Milestones after End Age are never reached, and there are no further reductions after 85.
    - The server is the single source of truth for the simulation period; the frontend validates against the End Age in the form rather than a hard-coded copy.
- **FR002-002**: System MUST allow selecting a "Spending Strategy" (Initially supporting "Partha's Spending Strategy-v0.01-20260105"). *(2026-10-19: Risk-Based Guardrails added, FR006-001 to FR006-022.)*
- **FR002-003**: System MUST allow users to specify market conditions or choose from a preset collection. While a preset is chosen, the market rates it sets (inflation, pre- and post-retirement growth, bond yield) MUST be disabled (greyed out, values still shown); Custom makes them editable. *(Added 2026-10-19.)*

#### Simulation Logic
- **FR002-004**: System MUST compute the Annual Income Gap (AIG) each year: Total projected annual expenses - Recurring Income (Interest + Dividends + Social Security).
- **FR002-004a**: System MUST compute the Cap AIG (for SB/CBB caps) using 50% of Wants: `(Needs + 50%Wants + Healthcare + PropertyTax + IncomeTax) - PassiveIncome`
- **FR002-005**: System MUST implement "Partha's Spending Strategy-v0.01-20260105" for covering the AIG:
    - Decisions made quarterly on 1st business day.
    - Logic checks market performance (S&P benchmark) relative to 12-month prior (Point-to-Point) and all-time highs.
    - Logic handles withdrawals from TBA/TDA based on CBB status (Full vs. Not Full) and Market status (Up vs. Down).
    - Logic manages transfers between accounts (TDA/TBA -> SB, TDA/TBA -> CBB, CBB -> SB, TFA -> SB).
    - The Tax Free account (TFA) is the last resort for spending: when TDA and TBA can't pay an SB refill (market up, or market down drawing on equities) or a shortfall (after the CBB), the rest is withdrawn from the TFA. The TFA never refills the CBB. *(Added 2026-10-18, #23.)*
    - When TDA and TBA can't pay an SB refill, in any market, the CBB pays the rest before the TFA. Before this rule, a rising market that drained the equities left the CBB full while the plan failed: in a steady market the CBB was only ever drawn in a downturn. The order for every refill is now TDA/TBA, then the CBB, then the TFA, the same as shortfall cover. *(Added 2026-10-19, T002-078.)*
    - When a down-market refill comes from the CBB and the CBB can't cover it, the rest comes from TDA/TBA, then the TFA. Before this rule, an empty CBB could be chosen (an empty CBB's "loss", its cap minus its balance, can be below the equities' loss) and the refill moved nothing, so plans failed with millions in the other accounts. *(Added 2026-10-19, T002-081.)*
    - Logic applies spending cuts (the year's inflation-adjusted Wants reduced by 10%) if SB is low.
    - In a down market with the SB at or below its floor, the SB is refilled from the CBB while the CBB is at least 90% of its balance 12 months earlier, or has lost less than TDA and TBA combined; otherwise from TDA/TBA (see the strategy above for the definitions; #21).
    - Logic respects SB Cap (2 × Cap AIG) and CBB Cap (see FR002-005a).
- **FR002-005a**: CBB Cap MUST be calculated as:
    - Initial CBB Cap = 7 × Base Cap AIG (from initial config, NOT inflation adjusted)
    - Reduces by 1× Base Cap AIG at each of these ages: 65, 70, 75, 80, 85
    - Minimum CBB Cap = 0
- **FR002-005b**: TDA withdrawals MUST be tracked separately by purpose:
    - `tdaWithdrawalSpend`: Withdrawals for spending (go to SB via spending strategy, post-retirement only)
    - `tdaWithdrawalRoth`: Withdrawals for Roth conversion (direct TDA→TFA, NOT via SB)
    - Total TDA Withdrawal = tdaWithdrawalSpend + tdaWithdrawalRoth
- **FR002-005c**: Roth conversions MUST:
    - Begin in Year 2 of the simulation (not Year 1)
    - Work both pre-retirement and post-retirement
    - Transfer directly from TDA to TFA (NOT through SB)
    - Be inflation-adjusted each year
- **FR002-006**: System MUST simulate monthly Salary income until retirement.
- **FR002-007**: System MUST adjust Spousal Social Security benefits (step-up logic) to 50% of the higher earner's benefit when the higher earner claims, if it is greater than the lower earner's existing benefit.
- **FR002-008**: System MUST mark the simulation as failed, from the first quarter before End Age (FR002-001a; default 85) in which any of these holds, checked at the end of each quarter:
    - The SB is below minus a quarter of the year's regular expenses (Needs + Wants + Healthcare + Property Tax). A smaller dip below 0 is allowed for timing within the quarter.
    - The CBB or the TFA is negative.
    - The TBA and the TDA are both negative.
    - An account at 0 alone is not a failure: the strategy draws on the next source, and the TFA is the last resort for spending (FR002-005, #23), so a plan runs out of money only once the TFA is also exhausted. The simulation finishes the failed year and stops there; that year is the failure year. *(Restated 2026-10-18 to match the engine; previously "any account balance drops to <= 0".)*

#### Outputs & Visualization
- **FR002-009**: System MUST display a results table with the following groups:
    - **Account Balances**: SB, SB Cap, CBB, CBB Cap, TBA, TDA, TFA, Total Portfolio.
    - **Income**: Salary, Interest, Dividends, Social Security, TBA Withdrawal, TDA Withdrawal (with breakdown: TDA for Spend, TDA for Roth), TFA Withdrawal (the last resort for spending, #23), Roth Conversion, Total Income (excludes Roth conversions, see Assumptions).
    - **Expenses**: Needs, Wants, Healthcare, Property Tax, Income Tax, Total Expenses.
    - **Metrics**: Annual Income Gap (AIG), 401k Contribution, TBA Contribution.
    - **Market**: Equity Return, Inflation and Bond Yield applied that year: the configured rates, a market preset's returns, or a Monte Carlo run's scenario. Quarterly rows show their year's rates; the Start Year snapshot, where no rates apply, shows `~`. *(Added 2026-10-17.)*
- **FR002-009a**: Age 75 rows MUST be visually highlighted (yellow background).
- **FR002-009b**: Failure rows MUST be visually highlighted (red background, takes priority over age highlighting).
- **FR002-009c**: System MUST provide a detailed computation breakdown for each row (accessible via info icon).
- **FR002-010**: System MUST summarize ending balances and total/failure status.
- **FR002-010a**: Users SHOULD be able to export the results table to CSV, in the view currently shown (yearly or quarterly), with the same columns as the table. *(Carried over from FR001-041, 2026-10-14.)*

#### Persistence
- **FR002-011**: System MUST allow saving the current simulation (Inputs + Result) with a user-provided name.
- **FR002-012**: System MUST list saved simulations with Date and Name.
- **FR002-013**: System MUST allow reloading a saved simulation.

#### Monte Carlo
- **FR002-014**: System MUST run at least 1000 simulations using the current configuration but with variable Inflation and Market Returns (Equities/Bonds).
- **FR002-015**: System MUST plot all 1000 paths on a line chart (X: Year, Y: Total Balance).
- **FR002-016**: Chart MUST highlight Median, 75th Percentile, and 90th Percentile lines.
- **FR002-017**: Chart MUST support interactive hovering to highlight individual paths.
- **FR002-018**: System MUST allow loading details (Table + Specific Variables) for a selected Monte Carlo run.

### Key Entities

- **SimulationConfig**: Holds all user input parameters (ages, balances, rates, expenses).
- **MarketScenario**: A sequence of annual market returns (S&P) and inflation rates.
- **YearlyResult**: The computed state for a single year (balances, cash flows, metrics, market rates); **QuarterlyResult** holds the same for a quarter.
- **MarketRates**: The equity return, inflation and bond yield applied in a year (FR002-009); none for the Start Year snapshot.
- **CashFlow**: Income and expense details for a period, including:
    - Income: salary, interest, dividends, socialSecurity, tbaWithdrawal, tdaWithdrawal, tdaWithdrawalSpend, tdaWithdrawalRoth, tfaWithdrawal, rothConversion, totalIncome
    - Expenses: needs, wants, healthcare, propertyTax, incomeTax, totalExpenses
    - Operations: sbDeposit, sbWithdrawal, contribution401k, contributionRoth (Roth 401k and/or Roth IRA, to TFA), contributionTba
- **Metrics**: Calculated values including annualIncomeGap, sbCap, cbbCap, isFailure
- **SimulationRun**: A complete execution consisting of `SimulationConfig` and a list of `YearlyResult`s.
- **MonteCarloSession**: A collection of 1000+ `SimulationRun`s and aggregate statistics.
- **ApiMetadata**: Version info (version, buildTime, serverStartTime) included in API responses.

## Success Criteria

### Measurable Outcomes

**Performance Baseline**: All performance metrics assume a standard development machine (4-core CPU, 8GB RAM).

- **SC002-001**: Users can run a full interactive simulation (through End Age, up to 110) with API response time under 500ms (P95).
- **SC002-002**: Monte Carlo simulation (1000 runs):
  - API computation time < 8 seconds (P95)
  - Frontend chart render time < 2 seconds (P95)
- **SC002-003**: The "Spending Strategy" logic correctly handles 100% of defined test cases (e.g., market crash scenario triggers CBB withdrawal).
- **SC002-004**: Users can save and reload a simulation with 100% data integrity.
- **SC002-005**: The visualization clearly distinguishes between "Safe" (Median/75th) and "Risk" (90th/Failure) outcomes in the Monte Carlo chart.

### Assumptions

- **Simulation period**: Runs from the year after Current Year through End Age (FR002-001a, default 85). The upper limit of 110 is a sanity bound, not a planning recommendation.
- **Inflation**: For the single interactive simulation, inflation is static. For Monte Carlo, it varies.
- **Interest & Dividends**: HYSA interest and bond dividends accrue monthly but are credited **quarterly** (on the 1st day of each quarter) to the SB.
- **Roth Conversions**: Begin in Year 2 of simulation. Direct TDA→TFA transfer, does NOT flow through SB.
- **Taxation**: Income Tax is calculated as a flat percentage (Effective Income Tax Rate) applied to taxable income sources. Taxable income includes:
  - Salary minus traditional 401k contributions (pre-tax; Roth contributions aren't deducted) (#13)
  - Interest (100%)
  - Dividends (100%)
  - Social Security (100% for model simplicity)
  - TDA withdrawals for spending (100%)
  - Roth Conversions (100%), counted once: a conversion is also a withdrawal from the TDA (#15)
  - TBA Withdrawals (50% taxable, assuming 50% cost basis)
  - TFA withdrawals are not taxed (qualified Roth withdrawals) (#23)
- **Total Income**: Salary + Interest + Dividends + Social Security + TBA withdrawals + TDA withdrawals for spending + TFA withdrawals. Roth conversions move money from the TDA to the TFA, so they aren't income and aren't in Total Income (#16); they show in the TDA for Roth and Roth Conversion columns.
- **Market Data**: Historical data provided (CSV) will be used to seed or drive the "preset" market conditions.