# Feature Specification: Yearly Computation Breakdown

**Feature Branch**: `004-yearly-breakdown`
**Created**: 2026-10-16
**Status**: Draft
**Input**: User description: "the detailed computation breakdown for each simulation year needs to be improved. i want to see 
1. prior year end account balances & economic conditions 
2. expense breakdown for the current year 
3. income breakdown for the current year 
4. income gap details 
5. all money movement by quarter. tabular representations seems best, but suggest a better way. 
6. taxation details 
7. income and expense projections for the next year"

## Context

Each row of the yearly results table has a details button that opens a computation breakdown for that year.
Today the breakdown is a list of formula steps grouped into ten sections: inflation, end-of-year balances,
expenses, one-time expenses, income, income gap, taxable income, spending strategy parameters, Spend Bucket
operations, and caps and status. It shows annual totals, but not how money moved during the year. It also
can't show where the year started, how each account got from its opening balance to its closing balance,
or what the next year will look like.

This feature reorganizes the breakdown into seven sections that follow the year in order. It explains the
simulation's existing calculations and does not change them.

## Clarifications

### Session 2026-10-16

- Q: What happens to the current "Spending Strategy Parameters" and "Caps & Status" sections? → A: Fold their
  content into the Income Gap and Money Movement sections where it applies (FR-007)
- Q: How should money movement be presented? → A: A ledger per account (movement types × Q1–Q4 and year), with
  a year-level money-flow diagram above the ledgers (FR-053, FR-055)
- Q: What does the details button do on a quarterly row? → A: Opens the yearly breakdown with that quarter
  highlighted in Money Movement (FR-008)
- Out of scope: 401k contributions are taxed in years after the first (the yearly taxable income counts gross
  salary). Tracked as a separate fix; this feature reports the rule as it is.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Follow every dollar through the year (Priority: P1)

A planner opens the breakdown for a retirement year and sees, quarter by quarter, every movement of money into
and out of each account: the Spend Bucket (SB), Crash Buffer Bucket (CBB), Taxable Brokerage (TBA),
Tax-Deferred (TDA) and Tax-Free (TFA) accounts. For each account, the opening balance plus that quarter's
movements equals its closing balance, and the closing balance at the end of Q4 matches the results table.

**Why this priority**: This is the part the current breakdown can't answer at all ("why did the Spend Bucket
drop by $90k this year?"). Without it, the other sections are totals the user has to take on trust.

**Independent Test**: Open the breakdown for any year and reconcile each account from opening to closing using
only the money-movement section. Delivers value on its own because it explains every balance change.

**Acceptance Scenarios**:

1. **Given** a retirement year with spending-strategy refills, **When** the user opens its breakdown, **Then**
   each quarter shows each account's opening balance, every inflow and outflow labelled with its source and
   destination, market growth, and the closing balance, and opening + movements = closing for every account
   and quarter.
2. **Given** any year, **When** the user compares the money-movement section's Q4 closing balances with the
   results table row, **Then** every account matches.
3. **Given** a retirement year, **When** the user looks at a quarter's spending-strategy refill, **Then** they
   see which rule applied (for example "market up: refill from TDA/TBA" or "market down: refill from CBB")
   and why the amount was what it was (for example "limited by the SB cap").
4. **Given** a year in which a one-time expense left the SB short, **When** the user opens its breakdown,
   **Then** the movement that covered the shortfall appears in the quarter it happened, with its source.

---

### User Story 2 - See the year's expenses, income and income gap (Priority: P2)

A planner sees what the year cost, what came in, and the gap between them, each item with how it was
computed. Expenses list each category and one-time expense. Income separates earned and passive income
(salary, interest, dividends, Social Security) from money moved out of investment accounts (withdrawals and
Roth conversions). The income gap section shows the gap the spending strategy planned for at the start of the
year next to the actual gap at year end.

**Why this priority**: These are the numbers users ask about most after balances. They exist today but mix
income with withdrawals and don't show the estimate the strategy actually used.

**Independent Test**: Open the breakdown for a working year and a retirement year and check that each section's
totals match the results table and that every line shows its inputs.

**Acceptance Scenarios**:

1. **Given** a working year, **When** the user opens the expense section, **Then** Wants is shown as the
   amount left of salary after the other expenses and contributions, step by step, and the income gap section
   says there is no gap because salary covers expenses.
2. **Given** a retirement year, **When** the user opens the income gap section, **Then** they see the
   estimated gap used by the spending strategy and the actual gap, each as expenses minus passive income,
   with the difference between them explained.
3. **Given** a year with Social Security, **When** the user opens the income section, **Then** each earner's
   benefit is shown, including any spousal step-up.
4. **Given** a year with one-time expenses, **When** the user opens the expense section, **Then** each expense
   is listed by name with its type (cash or loan payment) and amount.

---

### User Story 3 - Know where the year started and the conditions it ran under (Priority: P3)

A planner sees the account balances at the end of the prior year and the economic conditions applied in this
year: inflation, the equity return, the bond yield, the HYSA rate, the income tax rate, and where the market
stood against its all-time high when the year began.

**Why this priority**: It sets up everything else, and lets the user tell whether a bad year came from the
market or from spending.

**Independent Test**: Open the breakdown for the first retirement year of a "2008 Crash" preset plan and
confirm the equity return shown is −37% and is attributed to the preset's sequence.

**Acceptance Scenarios**:

1. **Given** any year after the first, **When** the user opens the starting-position section, **Then** the
   opening balance of each account equals the closing balance in the prior year's results row.
2. **Given** the first simulated year, **When** the user opens the starting-position section, **Then** the
   opening balances are the starting balances the user entered.
3. **Given** a plan that uses a market preset with a return sequence, **When** the user opens a year covered by
   the sequence, **Then** the equity return shown names the preset and the year of the sequence; other years
   show the configured growth rate.

---

### User Story 4 - Understand the year's taxes (Priority: P4)

A planner sees this year's tax bill and where it came from (the prior year's taxable income by component, times
the effective tax rate), and the taxable income this year generates for next year's bill, with how each
component is treated.

**Why this priority**: Taxes are a large expense, and the one-year lag between earning income and paying tax on
it is not obvious.

**Independent Test**: Open the breakdown for a year with a Roth conversion and confirm the conversion appears as
taxable income this year and in next year's tax bill.

**Acceptance Scenarios**:

1. **Given** any year after the first, **When** the user opens the tax section, **Then** this year's tax bill
   equals the prior year's taxable income components multiplied by the effective rate.
2. **Given** the first simulated year, **When** the user opens the tax section, **Then** it says the bill is
   based on an estimate of the starting year's income and shows that estimate.
3. **Given** a year with TBA withdrawals, **When** the user opens the tax section, **Then** only the taxable
   portion is counted, and the section says what portion that is.

---

### User Story 5 - See what next year looks like (Priority: P5)

A planner sees a projection of next year's expenses, income and income gap, built from what is known at the end
of this year, with changes called out (for example "retirement starts", "Social Security starts", "Medicare
starts", "loan ends", "crash buffer cap steps down").

**Why this priority**: It makes the transition between years explicit, especially around retirement, but it
depends on the other sections being right.

**Independent Test**: For any year except the last, compare the projected expenses that don't depend on markets
with the next year's actual expenses.

**Acceptance Scenarios**:

1. **Given** the year before retirement starts, **When** the user opens the next-year section, **Then** it shows
   salary ending, healthcare moving to the post-retirement rate, and an income gap appearing.
2. **Given** any year except the last, **When** the user compares projected expenses with next year's
   breakdown, **Then** every category that doesn't depend on market returns matches.
3. **Given** the last simulated year, **When** the user opens the next-year section, **Then** it says the
   simulation ends with this year.

---

### Edge Cases

- **Start-year row** (a balance snapshot with no cash flows): the breakdown shows only the starting position,
  not the other sections.
- **Retirement-age year**: a full working year (salary all year); the next-year section shows retirement
  starting.
- **First retirement year**: the spending strategy starts; the money-movement section shows its first refills.
- **Year the plan fails**: the simulation finishes that year and stops after it. The breakdown shows all four
  quarters and marks the quarter that failed, with the account that ran out.
- **Spend Bucket briefly negative inside a quarter** (allowed up to a quarter's expenses): the ledger shows the
  negative balance as it happened, not a corrected one.
- **TDA exhausted**: a Roth conversion smaller than planned is shown at the amount actually moved, with the
  planned amount alongside.
- **Deflation** (negative inflation): inflation-adjusted amounts are lower than today's-dollar amounts; labels
  still say "inflation-adjusted".
- **Loans**: fixed payments are shown as not inflation-adjusted, unlike other expenses.
- **Last simulated year**: no next year.
- **Quarterly view of the results table**: the details button on a quarterly row opens that year's breakdown
  with that quarter highlighted in the Money Movement section (FR-008).

## Requirements *(mandatory)*

### Functional Requirements

**General**

- **FR-001**: The breakdown for a simulated year MUST present seven sections in this order: Starting Position,
  Expenses, Income, Income Gap, Money Movement, Taxes, Next Year.
- **FR-002**: Every amount in the breakdown that also appears in the results table for that year MUST equal it.
- **FR-003**: Every computed amount MUST show how it was computed: its inputs and the rule, in plain words.
- **FR-004**: The breakdown MUST state whether the year is a working year or a retirement year, and label
  amounts that apply to only one of them.
- **FR-005**: Amounts MUST be shown in the dollars of the year being viewed. Where a configured amount was
  inflation-adjusted, the configured (today's-dollar) amount MUST be shown alongside.
- **FR-006**: The breakdown MUST explain the simulation's existing calculations and MUST NOT change any
  simulation result.
- **FR-007**: The content of the current "Spending Strategy Parameters" and "Caps & Status" sections MUST be
  folded into the new sections where it applies, not kept as separate sections: the caps, the SB floor, the
  CBB cap's age-based reductions and the strategy thresholds go in Income Gap (FR-041); the rule applied in
  each quarter goes in Money Movement (FR-052); the year's success or failure goes in Money Movement (FR-054).
- **FR-008**: The details button on a quarterly row of the results table MUST open the breakdown of that
  quarter's year, with that quarter highlighted in the Money Movement section.

**Starting Position (description item 1)**

- **FR-010**: The section MUST show each account's balance at the end of the prior year, and the total. For
  the first simulated year these are the starting balances the user entered.
- **FR-011**: The section MUST show the economic conditions applied in this year: the inflation rate and the
  cumulative inflation factor since the start year; the equity return applied to TBA, TDA and TFA, and where it
  came from (the configured pre- or post-retirement growth rate, or a named market preset's return sequence and
  which year of it); the bond yield; the HYSA rate; and the effective income tax rate.
- **FR-012**: The section MUST show where the market stood against its all-time high at the start of the year,
  because that decides which spending-strategy rule applies.
- **FR-013**: The section MUST show the household's situation for the year: age, spouse's age, working or
  retirement year, each earner's Social Security status (not yet claimed or claiming), and the healthcare
  bracket (pre-retirement, post-retirement before Medicare, or Medicare).

**Expenses (item 2)**

- **FR-020**: The section MUST list Needs, Wants, Healthcare, Property Tax, Income Tax and each one-time
  expense, with the year's total, matching the results table.
- **FR-021**: In working years, the section MUST show Wants as salary minus each other expense and each
  contribution, step by step. In retirement years, it MUST show Wants as the configured amount adjusted for
  inflation.
- **FR-022**: Each one-time expense MUST be listed by name with its type (cash expense, down payment or loan
  payment). Loan payments MUST be labelled as fixed, not inflation-adjusted.

**Income (item 3)**

- **FR-030**: The section MUST separate income (salary, interest, dividends, Social Security) from money moved
  out of investment accounts (TBA withdrawals, TDA withdrawals for spending, and Roth conversions), and show
  how both make up the results table's Total Income.
- **FR-031**: Salary MUST be shown gross, with each contribution (401k, Roth, TBA) and the take-home amount
  deposited to the SB.
- **FR-032**: Social Security MUST be shown per earner, including any spousal step-up, and the year each
  benefit started.

**Income Gap (item 4)**

- **FR-040**: In retirement years, the section MUST show the income gap the spending strategy estimated at the
  start of the year and the actual income gap at year end, each as expenses minus passive income, and explain
  the difference (for example the interest estimate versus actual interest, or one-time expenses).
- **FR-041**: In retirement years, the section MUST show the amounts derived from the gap that drive the
  strategy: the gap with 50% of Wants used for caps, the SB floor, the SB cap, the CBB cap with its age-based
  reductions, and the market thresholds that decide which strategy rule applies.
- **FR-042**: In working years, the section MUST say there is no income gap because salary covers expenses,
  and show take-home salary against expenses.

**Money Movement by Quarter (item 5)**

- **FR-050**: For each quarter, the section MUST show every account's opening balance, every movement into and
  out of it, and its closing balance. Each quarter's closing balance MUST equal the next quarter's opening
  balance, and Q4's closing balances MUST equal the year-end balances in the results table.
- **FR-051**: Every movement MUST be labelled with its type, its source and its destination. Types include:
  take-home salary, contributions, interest, dividends, Social Security, regular expenses, one-time expense
  payments, spending-strategy refills (TDA/TBA → SB, TDA/TBA → CBB, CBB → SB), shortfall cover, Roth
  conversions (TDA → TFA) and market growth.
- **FR-052**: For each quarter of a retirement year, the section MUST show which spending-strategy rule applied
  and what limited the refill amount (the quarterly share of the gap, the SB cap, the SB floor, or the
  available balance).
- **FR-053**: The section MUST present movements as a ledger per account, with rows for each movement type and
  columns for Q1–Q4 and the year total.
- **FR-054**: In a year the plan fails, the section MUST mark the quarter in which the failure condition was
  first met and name the account and the condition. All four quarters are still shown, because the simulation
  completes the year and the results table shows its year-end balances.
- **FR-055**: Above the ledgers, the section MUST summarize the year in a money-flow diagram showing where money
  came from and where it went (for example salary → SB → expenses, TDA → SB, TDA → TFA), with each flow's
  amount for the year. The diagram's flows MUST add up to the ledgers' year totals.

**Taxes (item 6)**

- **FR-060**: The section MUST show this year's tax bill as the prior year's taxable income, by component,
  multiplied by the effective tax rate. For the first simulated year it MUST show the estimate of the starting
  year's income that the bill is based on.
- **FR-061**: The section MUST show the taxable income this year generates, by component (salary, interest,
  dividends, Social Security, TDA withdrawals, Roth conversions, the taxable portion of TBA withdrawals), the
  share of each that is taxed, and the resulting tax bill for next year.
- **FR-062**: The section MUST note the simplifications the simulation makes (a single effective rate,
  Social Security fully taxable, half of TBA withdrawals taxable).

**Next Year (item 7)**

- **FR-070**: For every year except the last, the section MUST project next year's expenses by category,
  income by source and income gap, from what is known at the end of this year: inflation, the healthcare
  bracket, the tax bill from this year's income, scheduled one-time expenses and loan payments, salary or its
  end, and Social Security starts.
- **FR-071**: Projected amounts that don't depend on market returns MUST equal the next year's actual amounts.
  Amounts that do (interest, dividends, and the income gap that depends on them) MUST be labelled as estimates.
- **FR-072**: The section MUST call out changes from this year, such as retirement starting, Social Security
  starting, Medicare starting, a loan starting or ending, and a CBB cap step-down.
- **FR-073**: For the last simulated year, the section MUST say the simulation ends with this year.

### Key Entities

- **Year Breakdown**: the explanation of one simulated year; belongs to one results-table row. Holds the
  seven sections.
- **Line Item**: a labelled amount with its inputs and the rule that produced it, and, where relevant, its
  today's-dollar amount.
- **Economic Conditions**: the rates applied in a year (inflation, equity return and its source, bond yield,
  HYSA rate, tax rate) and the market's position against its all-time high.
- **Account Ledger**: one account's quarter-by-quarter record: opening balance, movements, closing balance.
- **Money Movement**: one transfer in a quarter: type, source, destination, amount, and for strategy refills
  the rule that applied and what limited the amount.
- **Tax Computation**: taxable income by component, the share taxed, the effective rate and the resulting bill.
- **Next-Year Projection**: projected expenses, income and gap for the following year, each marked as exact or
  estimated, plus the list of changes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For every simulated year of the four market presets applied to the default plan, every account
  reconciles in every quarter: opening balance plus movements equals closing balance, to the dollar.
- **SC-002**: 100% of amounts that appear both in the breakdown and in the results table match.
- **SC-003**: For every year except the last, 100% of projected amounts marked exact match the following
  year's actual amounts.
- **SC-004**: Using only the breakdown, a user can answer "why did this account change by this much this year?"
  for any account and year in under 2 minutes.
- **SC-005**: The breakdown for any year opens in under 1 second.
- **SC-006**: Each of the seven kinds of information the user asked for can be found from the top of the
  breakdown with no more than one click.
- **SC-007**: From the money-flow diagram alone, a user can name the year's largest source of spending money
  and the largest expense.

## Assumptions

- The breakdown is for the yearly results of a single simulation run, not Monte Carlo results.
- The breakdown reports the tax and spending rules as the simulation applies them today. Any problems found
  with those rules are reported separately, not changed by this feature.
- Quarters are the simulation's quarters: interest and dividends are credited, and strategy refills happen,
  at the start of each quarter; salary, Social Security and expenses are spread evenly across the quarters.
- "Economic conditions" means the conditions applied in the year being viewed, plus the market's position at
  the start of that year; the prior year's conditions are visible in the prior year's breakdown.
- Some movements the simulation makes today are recorded only as yearly totals or combined with other
  movements (for example equity withdrawals that refill the CBB rather than the SB). Recording each movement
  separately is part of this feature.
- The existing ability to open a breakdown from each yearly row stays; how the sections are opened and
  collapsed follows the current dialog unless the plan finds a better layout.
