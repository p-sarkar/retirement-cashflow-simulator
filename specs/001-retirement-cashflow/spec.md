# Feature Specification: Retirement Cash Flow Simulator

**Feature Branch**: `001-retirement-cashflow`  
**Created**: 2025-12-16  
**Updated**: 2025-12-18  
**Status**: Draft  
**Input**: User description: "Build an application to visualize future retirement cash flow under market conditions defined by inflation and S&P 500 performance. Provide preset market conditions and allow user-defined conditions. Inputs include ages, retirement age, starting balances across spend bucket, crash buffer, and equities split across taxable brokerage, tax-deferred, and tax-free, plus property tax inputs (starting value/rate). Output is a 35-year (2025–2059) table with account balances, income, regular yearly expenses, and one-time expenses. Property tax must show a year-by-year increase with inflation. Include a summary of ending balances and declare failure if any account balance goes negative before 35 years. Persist simulations with inputs and results; allow naming; list and reload saved simulations. Also provide Monte Carlo simulation (>= 1000 paths) of inflation + return sequences with an interactive plot showing each run and highlighting median / 75th / 90th percentile. Hovering highlights a path; clicking selects a path and shows its details and cash-flow below the chart." 

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Run a deterministic cash-flow simulation (Priority: P1)
A user selects a market condition (preset or custom), enters their starting balances and retirement inputs, and receives a 35-year (2025–2059) cash-flow table that shows account balances, income, expenses, and one-time goals; includes ending balance summaries; and clearly indicates whether the simulation succeeded or failed.

**Why this priority**: This is the core product value: a single scenario projection the user can read, sanity-check, and iterate on.

**Independent Test**: Can be tested by running a simulation with known inputs and verifying (a) 35 rows exist for 2025–2059, (b) required columns exist, (c) Total Income equals Total Expenses for each year until failure (if any), and (d) a success/failure outcome is shown.

**Acceptance Scenarios**:
1. **Given** current year = 2025 and valid inputs and a selected market condition, **When** the user runs a simulation, **Then** the system displays a table with exactly 35 rows labeled 2025 through 2059.
2. **Given** the projection table is displayed, **When** the user reviews any year row prior to failure, **Then** Total Income equals Total Expenses for that year.
3. **Given** the simulation finishes the 35-year horizon without any account balance becoming negative, **When** results are shown, **Then** the system marks the simulation as successful and summarizes ending balances by account and in total.
4. **Given** any account balance becomes negative at or before year 2059, **When** results are shown, **Then** the system marks the simulation as failed and identifies the first year of failure.

---

### User Story 2 - Save and reload simulations (Priority: P2)
A user saves a simulation run (including all inputs, selected market condition definition, and resulting cash-flow output), optionally names it, and later views a list of saved simulations and reloads any saved simulation.

**Why this priority**: Users will iterate on assumptions. Saving and reloading prevents losing work and enables comparison over time.

**Independent Test**: Can be tested by running a simulation, saving it with a name, verifying it appears in the saved list with run date, and reloading it to reproduce the same output.

**Acceptance Scenarios**:
1. **Given** a completed simulation run, **When** the user saves it with a name, **Then** it appears in the saved simulations list with the chosen name and the date it was run.
2. **Given** a saved simulation in the list, **When** the user reloads it, **Then** the system restores inputs and renders the same table output that was saved.

---

### User Story 3 - Run Monte Carlo simulations and view percentile outcomes (Priority: P3)
A user runs a Monte Carlo analysis that generates at least 1000 combinations (paths) of inflation and sequences of market returns and sees a plot of the resulting total account balance trajectories over time, with the median, 75th percentile, and 90th percentile highlighted. The chart supports basic interaction so users can inspect and drill into a single simulated path.

**Why this priority**: Monte Carlo reveals sequence-of-returns risk and provides a distributional view instead of a single-path forecast.

**Independent Test**: Can be tested by running a Monte Carlo analysis with N=1000 and verifying that (a) at least 1000 paths are generated, (b) the plot contains multiple trajectories plus highlighted percentile curves, (c) the axis encodes total balance vs year as specified, and (d) hover/click interaction works on individual paths.

**Acceptance Scenarios**:
1. **Given** valid baseline inputs, **When** the user runs Monte Carlo with at least 1000 simulations, **Then** the system produces at least 1000 simulated trajectories.
2. **Given** Monte Carlo results are displayed, **When** the user views the graph, **Then** each simulation is plotted with total account balance on the X-axis and year on the Y-axis and percentile curves (median, 75th, 90th) are visually distinct.
3. **Given** Monte Carlo results are displayed, **When** the user hovers over a specific path, **Then** that path is visually highlighted.
4. **Given** Monte Carlo results are displayed, **When** the user clicks a specific path, **Then** the application selects that simulation and displays that simulation’s input parameters and cash-flow details below the chart.

---

### Edge Cases
- Current age is greater than retirement age (invalid input).
- Starting balances include negative values (invalid input).
- A custom market condition definition is missing inflation values or S&P return values (invalid input).
- A custom market condition includes fewer than 35 annual values (system must apply a clear rule: reject, or allow constant extension; the behavior must be consistent).
- One-time goal expense is entered outside the modeled year range (must be rejected).
- A simulation fails mid-horizon due to a negative balance (system must clearly show the failure year and the balances leading into it).
- Monte Carlo requested with fewer than 1000 simulations (must be rejected or auto-rounded up to 1000).

## Requirements *(mandatory)*

### Functional Requirements

#### Projection Setup & Horizon
- **FR-001**: System MUST produce one row per calendar year for 35 years starting at year 2025 (i.e., 2025–2059 inclusive).
- **FR-002**: System MUST accept input variables for: current year, current age, age at retirement, and starting balances for spend bucket, crash buffer, and equities split across taxable brokerage, tax-deferred, and tax-free accounts.
- **FR-003**: System MUST validate that current age <= retirement age and that all starting balances are non-negative numbers.

#### Market Conditions (Inflation + S&P Performance)
- **FR-004**: System MUST model market conditions as a combination of (a) inflation and (b) S&P 500 returns that influence, at minimum, (1) income and expense lines via inflation adjustments and (2) the equities portfolio value via returns.
- **FR-005**: System MUST include a preset collection of common market conditions (at least 5 presets) that can be selected by the user.
- **FR-006**: System MUST allow the user to define a custom market condition.
- **FR-007**: For market conditions, system MUST treat inflation as applying automatically to both (a) expenses and (b) income streams where inflation adjustment is expected, specifically: essentials, nice-to-have, property tax, one-time goals, salary (until retirement), and Social Security (after claiming), unless a user explicitly overrides a specific line with an explicit per-year schedule.
- **FR-008**: For a custom market condition, user MUST be able to provide either (A) a full annual series for 2025–2059 for inflation and S&P returns, or (B) constant annual inflation and S&P return rates.
- **FR-009**: If a user provides a custom annual series shorter than 35 years, the system MUST extend it to 35 years by repeating the last provided value for remaining years.

#### Table Structure & Grouping
- **FR-010**: System MUST present results in a tabular format with four logical column groups: (1) account balances, (2) income, (3) regular yearly expenses, and (4) one-time expenses.
- **FR-011**: Account balances group MUST include, at minimum: spend bucket balance, crash buffer balance, taxable brokerage balance, tax-deferred balance, and tax-free balance (and MAY include totals).
- **FR-012**: Income group MUST include, at minimum, the following columns (0 allowed): salary, interest (from spend bucket), dividends (from crash buffer), short-term investment income, sale of stocks from taxable brokerage, distributions from tax-deferred account for spending, Roth conversion amount (counted as taxable income), Social Security income, and a separate Total Income column.
- **FR-013**: Regular yearly expenses group MUST include, at minimum: essentials, nice-to-have, income tax, property tax, and a separate Total Expenses column.
- **FR-014**: One-time expenses group MUST include a Goals column representing the sum of one-time expenses allocated to that year.
- **FR-015**: System MUST include an Income Gap column that shows the difference between Total Expenses and “non-withdrawal” income (definition must be consistent and documented in the UI/help text).

#### Balancing Rule (Cash-Flow Funding Order)
- **FR-016**: For each year, system MUST attempt to ensure Total Income equals Total Expenses by modeling cash-flow actions that close the Income Gap unless resources are exhausted.
- **FR-017**: When closing the Income Gap, the system MUST apply a deterministic default funding order: (1) spend bucket withdrawals (principal as needed), (2) taxable brokerage stock sales, (3) tax-deferred distributions for spending, (4) tax-free withdrawals.
- **FR-018**: Roth conversions MUST be modeled as a separate, user-directed action (counted as taxable income) and MUST NOT be required for closing the Income Gap unless the user explicitly configures conversions to fund spending.

#### Inputs for Income/Expense Lines
- **FR-019**: System MUST allow the user to enter (or default) salary inputs such that these can vary by year and/or change at retirement.
- **FR-020**: System MUST allow the user to enter Social Security inputs including (at minimum) claiming age and a base annual benefit amount.
- **FR-021**: System MUST allow the user to define one-time goal expenses with an amount and a target year.
- **FR-022**: Income tax handling MUST be hybrid: system calculates an estimated income tax per year from taxable income lines (including Roth conversions), and the user MUST be able to override the calculated value for any year.
- **FR-023**: System MUST allow the user to provide a starting annual property tax amount (or an equivalent starting tax rate that the system can convert into an annual amount) for year 2025.
- **FR-024**: System MUST increase the property tax line item year-by-year using the selected market condition’s inflation assumptions, unless the user overrides property tax with an explicit per-year schedule.

#### Success / Failure and End-of-Horizon Summary
- **FR-025**: System MUST summarize ending account balances (end of 2059) for each tracked account and a total ending balance.
- **FR-026**: If any tracked account balance becomes negative before or at year 2059, the system MUST mark the simulation as failed and identify the first year the balance went negative.
- **FR-027**: If a simulation fails, the system MUST still present the computed rows up to and including the first failure year.

#### Persistence of Simulations
- **FR-028**: System MUST allow persisting simulation runs including: all input variables, the selected market condition definition, the produced cash-flow table, the ending balance summary, and the success/failure outcome.
- **FR-029**: System MUST allow the user to optionally provide a name for a saved simulation.
- **FR-030**: System MUST provide a saved simulations listing view showing (at minimum) the run date and the user-provided name (if any).
- **FR-031**: System MUST allow reloading any saved simulation such that it reproduces the saved results.

#### Monte Carlo Simulation
- **FR-032**: System MUST provide a Monte Carlo simulation mode that runs at least 1000 combinations of inflation and sequences of market returns.
- **FR-033**: Monte Carlo generation MUST bootstrap annual (inflation, S&P return) pairs from a built-in historical dataset of yearly observations, resampling sequences to form 35-year paths.
- **FR-034**: Monte Carlo MUST produce a plot where each simulation’s total account balance trajectory is shown with total account balance on the X-axis and year on the Y-axis.
- **FR-035**: Monte Carlo results MUST highlight the median, 75th percentile, and 90th percentile trajectories.
- **FR-036**: The Monte Carlo plot MUST be user-interactive such that hovering over a path visually highlights it.
- **FR-037**: Clicking a path MUST select that simulation run and display the selected run’s parameters and cash-flow details below the chart.

#### Export / Sharing (optional but common)
- **FR-038**: Users SHOULD be able to export deterministic simulation tables to a common, portable format (e.g., CSV).

### Key Entities *(include if feature involves data)*
- **Simulation Input**: User-provided parameters for year range, ages, starting balances, income/expense inputs, and selected market condition.
- **Market Condition**: A definition of annual inflation rates and annual S&P return rates for the modeled horizon, including presets and user-defined.
- **Simulation Result**: A stored record containing the year-by-year table, ending balances summary, and success/failure outcome.
- **Year Row**: A single year’s computed values including beginning balances, income line items, expense line items (including one-time goals), and ending balances.
- **Goal (One-time Expense)**: A dated expense item with year and amount contributing to that year’s Goals column.
- **Monte Carlo Run**: A batch of simulated paths, each path producing a year-by-year total balance trajectory and percentiles across paths.

## Success Criteria *(mandatory)*

### Measurable Outcomes
- **SC-001**: A user can select a market condition, enter inputs, and generate a deterministic 35-row (2025–2059) table in under 60 seconds without external documentation.
- **SC-002**: For a successful deterministic run, 100% of projected years satisfy Total Income = Total Expenses.
- **SC-003**: Users can save a simulation with a name and later reload it to reproduce the same outputs.
- **SC-004**: Users can run a Monte Carlo analysis of at least 1000 paths and see percentile curves (median, 75th, 90th) highlighted on the plot.
