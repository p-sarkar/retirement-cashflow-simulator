# Feature Specification: Retirement Cash Flow Simulator

**Feature Branch**: `001-retirement-cashflow`  
**Created**: 2025-12-16  
**Status**: Draft  
**Input**: User description: "Build an application that can help me visualize future retirement cash flow in various market conditions. The input variables are current year, current age, age at retirement, current amount in spend bucket, crash buffer and equities portfolio (taxable brokerage, tax-deferred, tax-free). Output is a 35-year table starting 2025 with four groups: account balances, income, regular yearly expenses, one-time expenses. Income includes salary, interest (spend bucket), dividends (crash buffer), short-term investment income, stock sales (taxable), distributions (tax-deferred) for spending, Roth conversion (taxable income), and Social Security. Expense group includes essentials, nice-to-have, income tax, property tax, and goals (one-time expenses). Include Total Income, Total Expenses, and Income Gap, and ensure Total Income equals Total Expenses each year." 

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate a 35-year cash-flow table (Priority: P1)
A user enters their current situation (year, age, retirement age, and starting balances across spend bucket, crash buffer, and 3 account types) and sees a year-by-year table (2025–2059 inclusive) that balances income and expenses in each year.

**Why this priority**: This is the core value of the app: a single, readable projection that shows where spending money comes from and how balances change.

**Independent Test**: Can be fully tested by entering a minimal set of inputs and verifying the table shape (rows/columns) and that each year’s Total Income equals Total Expenses.

**Acceptance Scenarios**:
1. **Given** current year = 2025 and a valid set of starting balances and expense inputs, **When** the user runs a projection, **Then** the system displays a table with exactly 35 rows labeled 2025 through 2059.
2. **Given** the projection table is displayed, **When** the user reviews any year row, **Then** Total Income equals Total Expenses for that year, and an Income Gap value is shown.

---

### User Story 2 - Compare market condition scenarios (Priority: P2)
A user defines multiple “market condition” scenarios (e.g., baseline, early crash, prolonged bear) and compares how the same plan performs under each scenario.

**Why this priority**: Scenario comparison is the main reason to simulate “various market conditions” and is how users build confidence in a plan.

**Independent Test**: Can be tested by defining two scenarios with obviously different return assumptions and verifying that at least one balance column differs between scenario outputs.

**Acceptance Scenarios**:
1. **Given** two scenarios with different return assumptions, **When** the user runs projections for both, **Then** the system produces two comparable tables for the same 35-year horizon.

---

### User Story 3 - Track one-time goals and show them in the cash-flow (Priority: P3)
A user enters one-time expenses (“goals”) for specific years (e.g., roof replacement in 2031, college gift in 2035) and sees them included in the yearly totals.

**Why this priority**: One-time expenses are often the cause of shortfalls and need explicit visibility.

**Independent Test**: Can be tested by adding a single goal expense to one year and verifying that Total Expenses increases only in that year.

**Acceptance Scenarios**:
1. **Given** a goal of $X assigned to year Y, **When** the projection is generated, **Then** the “Goals (one-time)” value is $X in year Y and $0 in all other years unless otherwise specified.

---

### Edge Cases
- Current age is greater than retirement age (invalid input).
- Starting balances include negative values (invalid input).
- User inputs lead to account depletion before year 2059 (system must show depletion and how any remaining gap is handled).
- Social Security starts in a year where the user is already past the Social Security start age (must either start immediately or clarify behavior).
- One-time goal expense is entered outside the modeled year range (must be rejected or automatically clamped) 
- Income or expenses are entered as non-numeric values (validation and clear error messages).

## Requirements *(mandatory)*

### Functional Requirements

#### Projection Setup & Horizon
- **FR-001**: System MUST produce one row per calendar year for 35 years starting at year 2025 (i.e., 2025–2059 inclusive).
- **FR-002**: System MUST accept input variables for: current year, current age, age at retirement, and starting balances for spend bucket, crash buffer, and equities split across taxable brokerage, tax-deferred, and tax-free accounts.
- **FR-003**: System MUST validate that current age <= retirement age and that all starting balances are non-negative numbers.

#### Table Structure & Grouping
- **FR-004**: System MUST present results in a tabular format with four logical column groups: (1) account balances, (2) income, (3) regular yearly expenses, and (4) one-time expenses.
- **FR-005**: Account balances group MUST include, at minimum: spend bucket balance, crash buffer balance, taxable brokerage balance, tax-deferred balance, and tax-free balance (and may include totals).
- **FR-006**: Income group MUST include, at minimum, the following columns (0 allowed): salary, interest (from spend bucket), dividends (from crash buffer), short-term investment income, sale of stocks from taxable brokerage, distributions from tax-deferred account for spending, Roth conversion amount (counted as taxable income), Social Security income, and a separate Total Income column.
- **FR-007**: Regular yearly expenses group MUST include, at minimum: essentials, nice-to-have, income tax, property tax, and a separate Total Expenses column.
- **FR-008**: One-time expenses group MUST include a Goals column representing the sum of one-time expenses allocated to that year.
- **FR-009**: System MUST include an Income Gap column that shows the difference between Total Expenses and “non-withdrawal” income (definition must be consistent and documented in the UI/help text).

#### Balancing Rule
- **FR-010**: For each year, system MUST attempt to ensure Total Income equals Total Expenses by modeling cash-flow actions that close the Income Gap (e.g., withdrawals, stock sales, distributions) unless resources are exhausted.
- **FR-011**: When closing the Income Gap, the system MUST apply a deterministic default funding order: (1) spend bucket withdrawals (principal as needed), (2) taxable brokerage stock sales, (3) tax-deferred distributions for spending, (4) tax-free withdrawals.
- **FR-012**: Roth conversions MUST be modeled as a separate, user-directed action (counted as taxable income) and MUST NOT be required for closing the Income Gap unless the user explicitly configures conversions to fund spending.
- **FR-013**: If resources are exhausted in a year (insufficient balances to close the gap), system MUST surface an explicit shortfall indicator for that year while still showing the computed values and ending balances.

#### Market Conditions / Scenario Modeling
- **FR-014**: System MUST support defining and running at least one market condition scenario that influences investment growth and/or cash-flow yields across the modeled years.
- **FR-015**: System MUST allow the user to run the same inputs under multiple scenarios and compare outputs.
- **FR-016**: Scenario definition MUST be user-understandable (described in plain language such as “annual return assumptions by bucket and optional crash year(s)”) and must apply consistently across the 35-year horizon.

#### Inputs for Income/Expense Lines
- **FR-015**: System MUST allow the user to enter (or default) salary inputs such that these can vary by year and/or change at retirement.
- **FR-016**: System MUST allow the user to enter Social Security inputs including (at minimum) claiming age and a base annual benefit amount.
- **FR-017**: System MUST allow the user to define one-time goal expenses with an amount and a target year.
- **FR-018**: Income tax handling MUST be hybrid: system calculates an estimated income tax per year from taxable income lines (including Roth conversions), and the user MUST be able to override the calculated value for any year.

#### Export / Sharing (optional but common)
- **FR-019**: Users SHOULD be able to export the resulting table to a common, portable format (e.g., CSV).

### Key Entities *(include if feature involves data)*
- **Projection Input**: User-provided parameters for year range, ages, and starting balances.
- **Account**: An account/bucket with a type (spend, crash buffer, taxable, tax-deferred, tax-free), balance, and scenario-dependent growth/yield assumptions.
- **Scenario**: A named set of market condition assumptions that affects how balances and/or yields evolve over time.
- **Year Row**: A single year’s computed values including beginning balances, income line items, expense line items, and ending balances.
- **Goal (One-time Expense)**: A dated expense item with year and amount contributing to that year’s Goals column.

## Success Criteria *(mandatory)*

### Measurable Outcomes
- **SC-001**: A user can enter inputs and generate a 35-row (2025–2059) table in under 60 seconds without referencing external documentation.
- **SC-002**: For valid inputs without depletion, 100% of projected years satisfy Total Income = Total Expenses.
- **SC-003**: Users can create at least 2 scenarios and see differences in at least one output column when scenario assumptions differ.
- **SC-004**: Users can add a one-time goal expense and observe it reflected in exactly one year’s Goals value and Total Expenses.
