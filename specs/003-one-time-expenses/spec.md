# Feature Specification: One-Time Expenses

**Feature Branch**: `003-one-time-expenses`  
**Created**: 2026-01-20  
**Status**: Draft  
**Input**: User description: "Add support for one-time expenses with the following requirements: User can enter one or more one-time expenses in the simulation form; Each expense has a user-selected name/purpose; Two types of one-time expenses: 1. Cash: One-time lump sum payment in a user-selected year (can be pre or post retirement) - Paid from the Spend Bucket (SB) in January (lump sum); 2. Loan: Fixed APR, fixed term loans with automatic amortization - Have a start year and end year - Have an APR percentage - Monthly payment automatically calculated using amortization - Paid monthly from SB from start year to end year; Years can be specified in either age terms or actual year terms; Results table includes a new column showing amounts paid towards one-time expenses; Column has an info icon that shows detailed breakdown (expense name and amounts) when there are concurrent one-time expenses; All one-time expenses are paid from the Spend Bucket (SB)"

## Clarifications

### Session 2026-01-20

- Q: When regular living expenses and one-time expenses occur in the same period, which should be paid first from the Spend Bucket? → A: One-time expenses paid AFTER regular living expenses
- Q: When one-time expenses (cash or loan payment) exceed available Spend Bucket balance, how should the spending strategy be triggered? → A: Each one-time expense independently triggers spending strategy if SB insufficient at that point
- Q: When a loan starts mid-year (e.g., June), how should the first year's payment be calculated and reflected in annual totals? → A: Monthly payment amount stays constant, first year shows pro-rated total for remaining months
- Q: What granularity should be offered for loan start timing (year only, month+year, or exact date)? → A: Year only - payments assumed to start in January
- Q: What UI pattern should be used for the expense breakdown when users click the info icon? → A: Modal dialog pattern
- Q: Should the system enforce a maximum number of one-time expenses per simulation for performance or UX simplicity? → A: Standard baseline - no explicit constraint needed; typical use <10 expenses
- Q: What behavior should occur when user clicks outside the expense breakdown modal dialog? → A: Modal auto-dismisses on clicking outside (standard Material-UI Dialog behavior)
- Q: When one-time expenses exceed the Spend Bucket balance, what should happen to prevent negative balances? → A: Automatically adjust spending strategy (draw from CBB, then equities)
- Q: When multiple one-time expenses occur in the same year, in what order should they be processed? → A: Process sequentially in entry order
- Q: Should one-time expense amounts be adjusted for inflation like other expenses (needs, wants, healthcare)? → A: Yes - expense amounts should be inflation-adjusted using the same cumulative inflation calculation as other expenses
- Q: Where should the detailed breakdown of one-time expenses be displayed in the results table? → A: In the main computation breakdown dialog (accessed via the 🔍 icon in each row) rather than inline in the one-time expenses column. This provides a unified location for all year-level computation details and avoids UI clutter from multiple info icons.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Add Single Cash Expense (Priority: P1)

A user planning retirement wants to model a major one-time purchase (e.g., new car, home renovation, or medical procedure) that will occur in a specific year. They need to see how this lump-sum expense affects their retirement cash flow and account balances.

**Why this priority**: This is the most fundamental use case and delivers immediate value. Users can model simple one-time expenses without the complexity of loans. This alone provides actionable insights for major purchase planning.

**Independent Test**: User can add a single cash expense (e.g., "$50,000 for car at age 67"), run simulation, and see the impact in the results table's one-time expenses column and account balances.

**Acceptance Scenarios**:

1. **Given** the simulation form is open, **When** the user adds a cash expense named "New Car" for $50,000 at age 67, **Then** the form accepts and displays this expense
2. **Given** a simulation with one cash expense, **When** the user runs the simulation, **Then** the results table shows $50,000 in the one-time expenses column for year when user is age 67
3. **Given** a simulation with one cash expense, **When** the user views the results, **Then** the Spend Bucket balance decreases by $50,000 in January of the specified year
4. **Given** a cash expense year specified as calendar year 2035, **When** the user runs the simulation, **Then** the expense appears in year 2035 regardless of user's current age

---

### User Story 2 - Add Multiple Cash Expenses (Priority: P2)

A user wants to model multiple one-time expenses across different years (e.g., car purchase at 67, home repair at 72, international trip at 75) to understand cumulative impact on their retirement plan.

**Why this priority**: Extends P1 to handle realistic retirement planning scenarios where multiple major expenses are anticipated. Still focuses on simple cash expenses before introducing loan complexity.

**Independent Test**: User can add 3-5 different cash expenses across different years, run simulation, and see each expense reflected correctly in the corresponding years.

**Acceptance Scenarios**:

1. **Given** the simulation form is open, **When** the user adds three separate cash expenses for different years, **Then** all three expenses are displayed in the form
2. **Given** a simulation with multiple cash expenses in different years, **When** the user runs the simulation, **Then** each expense appears in its designated year in the results table
3. **Given** a simulation with two cash expenses in the same year, **When** the user runs the simulation, **Then** both expenses are summed in that year's one-time expenses column
4. **Given** expenses exist in a year, **When** the user clicks the main computation breakdown icon (🔍) for that year, **Then** the breakdown dialog includes a "One-Time Expenses" section showing all expense names and their individual inflation-adjusted amounts
5. **Given** no expenses in a year, **When** the user views the computation breakdown, **Then** the "One-Time Expenses" section is not shown (or shows $0)

---

### User Story 3 - Add Loan with Amortization (Priority: P3)

A user wants to model taking a home equity loan or personal loan during retirement, understanding both the monthly payment burden and total interest paid over the loan term.

**Why this priority**: Adds significant value for users considering leveraging debt in retirement, but is more complex than cash expenses. Can be implemented after cash expense foundation is solid.

**Independent Test**: User can add a loan (e.g., "$100,000 at 6% APR for 10 years starting at age 65"), run simulation, and see monthly payments automatically calculated and reflected in the results.

**Acceptance Scenarios**:

1. **Given** the simulation form is open, **When** the user adds a loan expense with amount, APR, start year, and term, **Then** the monthly payment is automatically calculated and displayed
2. **Given** a loan starting at age 65 for 10 years, **When** the user runs the simulation, **Then** the one-time expenses column shows monthly payments from age 65 through 74
3. **Given** a loan with 6% APR over 10 years, **When** the monthly payment is calculated, **Then** it uses standard amortization formula: M = P[r(1+r)^n]/[(1+r)^n-1]
4. **Given** a loan expense starting in January, **When** the user views results for any year during the loan term, **Then** the annual total reflects 12 monthly payments (no pro-rating)
5. **Given** concurrent loan and cash expenses, **When** the user clicks the main computation breakdown icon (🔍), **Then** the "One-Time Expenses" section shows both loan payments and cash expenses with their names and inflation-adjusted amounts

---

### User Story 4 - Edit and Remove Expenses (Priority: P2)

A user wants to adjust their expense assumptions (e.g., reduce loan amount, change timing, remove obsolete expenses) as they refine their retirement plan.

**Why this priority**: Essential for iterative planning. Users need to experiment with different scenarios and correct mistakes without starting over.

**Independent Test**: User can modify any expense field, remove expenses, and see updated simulation results immediately.

**Acceptance Scenarios**:

1. **Given** an existing cash expense, **When** the user changes the amount from $50,000 to $60,000, **Then** the simulation reflects the updated amount
2. **Given** an existing loan expense, **When** the user changes the APR from 6% to 5%, **Then** the monthly payment is recalculated automatically
3. **Given** multiple expenses, **When** the user removes one expense, **Then** that expense no longer appears in simulation results
4. **Given** a removed expense, **When** the user runs the simulation, **Then** the results table no longer includes that expense's impact

---

### Edge Cases

- What happens when a cash expense amount exceeds the Spend Bucket balance in that year?
  - The expense automatically triggers the existing spending strategy (draw from CBB, then equities) to cover the shortfall. This prevents negative balances and ensures all one-time expenses can be paid.
- What happens when multiple one-time expenses occur in the same year?
  - Expenses are processed sequentially in entry order. Each expense independently evaluates the Spend Bucket balance at the time of processing and triggers the spending strategy if needed. This simple approach is appropriate for MVP since the spending strategy handles insufficient funds automatically.
- What happens when user enters a loan term that extends beyond the 35-year simulation period?
  - Only payments within the simulation timeframe are shown; the loan may not be fully paid off by year 35
- What happens when user specifies expense timing in both age and calendar year formats in the same simulation?
  - Both formats are supported simultaneously; age-based expenses are converted to calendar years based on current age and current year inputs
- What happens when user enters loan start year equal to end year?
  - System should validate that end year is after start year; display validation error if violated
- What happens when user enters negative expense amounts or APR?
  - System validates that amounts and APR are positive numbers; displays validation error for invalid inputs
- What happens when loan monthly payment cannot be calculated (e.g., 0% APR or invalid term)?
  - For 0% APR: monthly payment = principal / number of months (simple division)
  - For invalid term (0 or negative months): validation error displayed

## Requirements *(mandatory)*

### Functional Requirements

#### Input & Data Management

- **FR-001**: System MUST allow users to add multiple one-time expenses to a simulation
- **FR-002**: Each expense MUST have a user-provided name/description field (e.g., "New Car", "Home Renovation")
- **FR-003**: Each expense MUST specify an expense type: either "Cash" or "Loan"
- **FR-004**: Users MUST be able to edit any field of an existing expense before running simulation
- **FR-005**: Users MUST be able to remove any expense from the simulation
- **FR-006**: System MUST validate that all required fields are populated before allowing simulation execution

#### Cash Expense Requirements

- **FR-007**: Cash expenses MUST have an amount (dollar value) field
- **FR-008**: Cash expenses MUST have a timing field that specifies when the expense occurs
- **FR-009**: Timing for cash expenses MUST support both age-based specification (e.g., "at age 67") and calendar year specification (e.g., "in 2035")
- **FR-010**: Cash expenses MUST be paid as a lump sum in January of the specified year
- **FR-011**: Cash expenses MUST be deducted from the Spend Bucket balance

#### Loan Expense Requirements

- **FR-012**: Loan expenses MUST have a principal amount (dollar value) field
- **FR-013**: Loan expenses MUST have an APR (Annual Percentage Rate) field expressed as a percentage
- **FR-014**: Loan expenses MUST have a start year/age field indicating when loan payments begin
- **FR-015**: Loan expenses MUST have a term duration field (number of years) that determines when loan payments end
- **FR-016**: System MUST automatically calculate monthly payment amount using standard amortization formula: M = P[r(1+r)^n]/[(1+r)^n-1], where P=principal, r=monthly rate, n=total months
- **FR-017**: For loans with 0% APR, monthly payment MUST be calculated as principal divided by total number of months
- **FR-018**: System MUST display the calculated monthly payment to users before they run the simulation
- **FR-019**: Loan payments MUST be deducted from the Spend Bucket monthly throughout the loan term
- **FR-020**: Loan payments MUST begin in the start year and continue through the end year (start year + term - 1)
- **FR-021**: Timing for loan start MUST support both age-based and calendar year specification

#### Calculation & Simulation

- **FR-022**: All one-time expenses MUST be paid from the Spend Bucket (SB) AFTER regular living expenses are paid each period
- **FR-023**: Cash expense withdrawals MUST occur in January of the specified year
- **FR-024**: Loan payment withdrawals MUST occur monthly during the loan term starting in January of the start year; monthly payment amount remains constant throughout the entire loan term
- **FR-025**: System MUST convert age-based timing to calendar years using the user's current age and current year inputs
- **FR-026**: When Spend Bucket balance is insufficient to cover a one-time expense, the expense MUST automatically trigger the existing spending strategy (draw from CBB, then equities) to prevent negative balances. Multiple expenses in the same period MUST be processed sequentially in entry order, with each expense independently evaluating SB balance and triggering the strategy if needed.
- **FR-027**: System MUST include one-time expenses in the total annual expenses calculation
- **FR-028**: One-time expenses MUST be included in the Annual Income Gap (AIG) calculation

#### Results Display

- **FR-029**: Results table MUST include a new column labeled "One-Time Expenses" showing amounts paid toward one-time expenses for each year
- **FR-030**: For years with only one expense, the column MUST display the total amount without additional indicators
- **FR-031**: For years with concurrent (multiple) one-time expenses, the column MUST display the total amount with an info icon
- **FR-032**: When user clicks the info icon, system MUST display a detailed breakdown in a modal dialog that overlays the results table
- **FR-033**: The breakdown modal MUST list each expense name and its individual amount for that year
- **FR-034**: For loan expenses in the breakdown, system MUST show the expense name and annual payment total (12 monthly payments)
- **FR-035**: The breakdown modal MUST include a close button/mechanism to dismiss the dialog and MUST auto-dismiss when user clicks outside the modal (standard Material-UI Dialog behavior)
- **FR-036**: The one-time expenses column MUST show $0 (or be empty) for years with no one-time expenses

#### Validation Requirements

- **FR-037**: System MUST validate that expense amounts are positive numbers
- **FR-038**: System MUST validate that loan APR is a non-negative percentage
- **FR-039**: System MUST validate that loan term is a positive number of years
- **FR-040**: System MUST validate that loan end year (start year + term - 1) is after or equal to start year
- **FR-041**: System MUST validate that expense timing falls within the simulation period (current year to current year + 35)
- **FR-042**: System MUST display clear validation error messages when validation fails
- **FR-043**: System MUST prevent simulation execution when validation errors exist

### Key Entities

- **OneTimeExpense**: Represents a single one-time financial obligation in the retirement plan
  - Properties: name/description, type (Cash or Loan), timing information
  - Two subtypes: CashExpense and LoanExpense

- **CashExpense**: A one-time lump-sum payment
  - Properties: amount, payment year (can be specified as age or calendar year)
  - Behavior: Paid entirely in January of specified year from Spend Bucket

- **LoanExpense**: A fixed-term loan with amortized payments
  - Properties: principal amount, APR, start year, term (years), calculated monthly payment
  - Derived properties: end year (start year + term - 1), annual payment total (monthly payment × 12)
  - Behavior: Monthly payments deducted from Spend Bucket from start year through end year

- **ExpenseBreakdown**: Detail view showing concurrent expenses
  - Properties: year, list of expenses with names and amounts
  - Behavior: Displayed when user clicks info icon on years with multiple one-time expenses

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can add a one-time cash expense and see its impact in simulation results within 30 seconds of input
- **SC-002**: Users can add a loan expense and see the automatically calculated monthly payment displayed immediately upon entering loan parameters
- **SC-003**: Users can successfully model at least 10 different one-time expenses in a single simulation without performance degradation (no hard limit enforced; typical usage expected to be under 10 expenses)
- **SC-004**: 95% of users can understand the one-time expenses impact by viewing the results table without consulting documentation
- **SC-005**: The expense breakdown dialog loads and displays within 1 second when user clicks the info icon
- **SC-006**: Loan payment calculations are accurate to within $0.01 of standard amortization tables
- **SC-007**: Users can complete the task of "add a one-time expense and see results" in under 2 minutes
- **SC-008**: Zero calculation errors when expenses span the full 35-year simulation period
- **SC-009**: The results table correctly reflects one-time expenses for 100% of tested scenarios (cash only, loan only, mixed expenses, concurrent expenses)
- **SC-010**: Users can distinguish between years with single vs. multiple expenses without reading instructions (via presence/absence of info icon)

## Assumptions

- Users understand basic loan terminology (APR, principal, term, amortization)
- The existing simulation form already has infrastructure for adding repeatable input sections (can accommodate multiple expense entries without imposing a hard limit; typical usage expected to be under 10 expenses per simulation)
- The existing results table can accommodate additional columns without significant redesign
- Age-to-calendar-year conversion uses integer age (not fractional) and assumes expenses occur at the start of the calendar year when that age is reached
- Cash expenses and loan start dates both occur in January (year-level granularity only); "January" payment for cash expenses means the expense is reflected in the annual totals for that year, even if monthly cash flow modeling isn't visualized
- The existing Spend Bucket (SB) infrastructure can handle both one-time withdrawals and recurring monthly withdrawals
- Loan interest is compounded monthly (standard for most consumer loans)
- Loan monthly payment amount stays constant throughout the loan term (12 monthly payments per calendar year for all years of the loan term)
- Existing validation framework can be extended to validate one-time expense inputs
- The breakdown dialog uses the same UI patterns as other detail dialogs in the application (if any exist)

## Out of Scope

The following are explicitly NOT part of this feature specification:

- **Variable rate loans**: Only fixed APR loans are supported; adjustable-rate mortgages (ARMs) or variable-rate loans are not included
- **Loan refinancing**: Ability to model refinancing an existing loan at a different rate or term
- **Early loan payoff**: Modeling extra payments or early payoff of loans before term end
- **Recurring expenses**: This feature handles only one-time expenses; recurring monthly/annual expenses are part of the base retirement cashflow feature
- **Tax implications**: Tax treatment of loan interest deductions or expense deductibility is not modeled
- **Payment timing flexibility**: Cash expenses always occur in January; loan payments always monthly. No quarterly, bi-weekly, or custom payment schedules
- **Loan origination fees**: Fees, points, or closing costs associated with taking out a loan
- **Principal paydown tracking**: Tracking remaining principal balance over the loan term (only monthly payment amount is shown)
- **Interest paid reporting**: Total interest paid over the life of the loan is not separately reported
- **Expense categories/grouping**: No categorization of expenses (e.g., "home", "vehicle", "health"); each expense is independent
- **Conditional expenses**: No support for "if-then" expense logic (e.g., "buy car only if portfolio exceeds X")
- **Expense templates or presets**: No pre-defined expense scenarios or ability to save/reuse expense configurations
