# Feature Specification: Selectable Spending Strategy with Risk-Based Guardrails

**Feature Branch**: `feat/006-guardrails-spending-strategy`  
**Created**: 2026-10-19  
**Status**: Draft  
**Input**: User description: "i need to implement the modern risk based guardrails withdrawal strategy in retirement. this is a withdrawal strategy in addition to the one currently in place. i'll probably introduce additional strategies later. so, need to allow a user selectable retirement spending strategy."

## Background

Today every simulation uses one spending strategy, "Partha's Spending Strategy-v0.01-20260105" (FR002-005). That strategy keeps retirement spending fixed (apart from a 10% cut to Wants in a down market) and decides *which account* pays for it: the Spend Bucket (SB), the Crash Buffer Bucket (CBB), the equity accounts (TDA, TBA), and the Tax-Free account (TFA) as a last resort. FR002-002 already promised a selectable strategy, but only one has existed until now.

**Risk-based guardrails** (Kitces, Tharp and Fitzpatrick) decide *how much* to spend. At regular reviews the plan's **probability of success** is measured: the chance that the portfolio lasts through the end age at the current spending. Three thresholds govern spending:

- **Target**: the probability of success the plan aims for (example: 90%).
- **Upper guardrail**: if the probability rises above this level (example: 99%), the retiree is underspending, so spending goes up.
- **Lower guardrail**: if the probability falls below this level (example: 70%), the plan is at risk, so spending goes down.

When a guardrail is crossed, spending is reset to the level that brings the probability of success back to the target. Between the guardrails spending only changes with inflation. Each guardrail can also be stated in dollars: the portfolio value at which it would be crossed, and the spending change that follows. That is easier to act on than a percentage.

This feature turns the spending strategy into a user choice, adds risk-based guardrails as the second strategy, and leaves room for more strategies later.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Choose a spending strategy (Priority: P1)

When setting up or editing a simulation, the user picks the retirement spending strategy from a list. Each entry has a short description. The list holds the existing strategy and Risk-Based Guardrails. Only the settings for the selected strategy are shown. The choice is saved with the simulation and used for interactive and Monte Carlo runs.

**Why this priority**: Every other story depends on it, and it keeps existing simulations working unchanged. On its own it delivers the strategy choice that FR002-002 promised.

**Independent Test**: Open an existing saved simulation and confirm it shows the existing strategy and gives the same results as before. Switch the strategy, save, reload, and confirm the choice and its settings are kept.

**Acceptance Scenarios**:

1. **Given** a simulation saved before this feature, **When** the user opens and runs it, **Then** it shows "Partha's Spending Strategy-v0.01-20260105" as the selected strategy and its results match the results from before this feature exactly.
2. **Given** a new simulation, **When** the user opens the strategy selector, **Then** the existing strategy is selected by default and Risk-Based Guardrails is offered as an alternative, each with a one-line description.
3. **Given** the user selects Risk-Based Guardrails, **When** the form updates, **Then** the guardrail settings appear (target, upper guardrail, lower guardrail, review frequency) filled with their defaults, and the settings that apply only to the existing strategy are hidden.
4. **Given** the user saves a simulation with Risk-Based Guardrails and custom settings, **When** they reload it, **Then** the strategy and every setting come back as saved.
5. **Given** the user switches a simulation from one strategy to the other and back, **When** they return to the first strategy, **Then** that strategy's earlier settings are still there.

---

### User Story 2 - Simulate retirement with risk-based guardrails (Priority: P1)

The user runs a simulation with Risk-Based Guardrails selected. At each review during retirement the plan's probability of success is measured, and spending goes up or down when a guardrail is crossed. The results show how spending changed over retirement, each review's probability of success, and when each adjustment happened.

**Why this priority**: This is the new strategy and the reason for the feature.

**Independent Test**: Run a simulation with Risk-Based Guardrails and a fixed market sequence that includes an early crash. Confirm that spending is cut at the first review below the lower guardrail, that the probability of success returns to the target after the cut, and that spending rises again once the probability passes the upper guardrail.

**Acceptance Scenarios**:

1. **Given** a guardrails simulation whose probability of success stays between the guardrails, **When** it runs, **Then** spending changes only with inflation and no adjustment is recorded.
2. **Given** a review where the probability of success is below the lower guardrail, **When** the review runs, **Then** Wants are reduced to the level at which the probability of success equals the target, and the adjustment is recorded with its date, the probability before and after, and the old and new Wants.
3. **Given** a review where the probability of success is above the upper guardrail, **When** the review runs, **Then** Wants are increased to the level at which the probability of success equals the target, and the adjustment is recorded in the same way.
4. **Given** a guardrails simulation, **When** the user views the results, **Then** they see spending year by year next to the configured (unadjusted) spending, and each review's probability of success against the guardrails.
5. **Given** the same configuration (and seed for Monte Carlo), **When** the user runs it twice, **Then** both runs give identical results, including every review and adjustment.
6. **Given** a Monte Carlo run with Risk-Based Guardrails, **When** it finishes, **Then** the summary includes the usual success rate and portfolio percentiles, plus percentiles of annual spending over retirement and the share of runs that ever cut spending.

---

### User Story 3 - See the guardrails in dollars (Priority: P2)

For each review the user sees the guardrails as portfolio values: "if the portfolio falls below $X, spending drops to $Y a year; if it rises above $Z, spending rises to $W a year." This is how an adviser would explain the plan to a client.

**Why this priority**: It makes the strategy understandable, but the simulation is correct without it.

**Independent Test**: Run a guardrails simulation and check, for one review, that a portfolio just below the stated lower-guardrail value would produce a probability of success below the lower guardrail, and that a portfolio just above it would not.

**Acceptance Scenarios**:

1. **Given** a guardrails simulation, **When** the user views a review in the yearly breakdown, **Then** it shows the portfolio value at each guardrail and the spending that would follow if that guardrail were crossed.
2. **Given** the start of retirement, **When** the user views the results summary, **Then** it shows the starting probability of success and the starting dollar guardrails.

---

### User Story 4 - Explain each review in the yearly breakdown (Priority: P3)

The yearly computation breakdown (spec 004) explains guardrail reviews the same way it explains the existing strategy's quarterly rule: the probability of success, which guardrail was crossed (if any), the new spending, and which accounts paid for it.

**Why this priority**: It builds trust in the numbers. The strategy works without it.

**Independent Test**: Open the breakdown for a year with a spending cut and confirm it shows the review inputs, the guardrail crossed, and the old and new spending, and that the year's withdrawals add up to the adjusted spending.

**Acceptance Scenarios**:

1. **Given** a guardrails simulation, **When** the user opens the breakdown for a review year, **Then** it shows the probability of success, the target and both guardrails, the outcome (no change, cut or raise), and the spending before and after.
2. **Given** quarterly reviews and a year in which the plan fails before a later quarter's review, **When** the user opens that year's breakdown, **Then** it states that the remaining reviews didn't happen because the plan failed. *(Revised 2026-10-19, analysis U006-1: yearly and quarterly reviews leave no retirement year without a review otherwise.)*

---

### Edge Cases

- **Plan below the lower guardrail at retirement**: the first review happens at the start of retirement, so spending is cut before any money is spent. The results flag that the configured spending could not be supported at the target.
- **Plan above the upper guardrail at retirement**: the first review raises spending at the start of retirement.
- **Wants cut to zero**: guardrails adjust only Wants (FR006-014). If the probability of success stays below the target even with zero Wants, Wants are set to zero and the review records "target unreachable". Needs, healthcare and property tax are never cut.
- **No Wants configured**: guardrails have nothing to adjust. Reviews still run and report the probability of success and the dollar guardrails, but every outcome is "no change" or "target unreachable".
- **Portfolio exhausted**: the plan fails exactly as with the existing strategy (once the TFA is also exhausted, FR002-005), and reviews stop.
- **Passive income covers all expenses** (income gap of zero or less): the probability of success is 100%, so Wants rise to the raise cap (FR006-012).
- **Short horizon near the end age**: in the final years the probability of success becomes very sensitive. Reviews keep using the remaining years to the end age.
- **One-time expenses** (spec 003) are included in the probability-of-success calculation as future obligations. Guardrails never cut them.
- **Invalid settings**: a lower guardrail at or above the target, an upper guardrail at or below the target, or any value outside 1–99% is rejected with a clear, per-field message.
- **Roth conversions and the TDA withdrawal percentage** keep working under either strategy.

## Requirements *(mandatory)*

### Functional Requirements

**Strategy selection**

- **FR006-001**: The system MUST let the user choose the retirement spending strategy for each simulation from a list of available strategies. This fulfils FR002-002.
- **FR006-002**: The list MUST initially contain "Partha's Spending Strategy-v0.01-20260105" (FR002-005) and "Risk-Based Guardrails". Each entry MUST have a name and a one-line description.
- **FR006-003**: A new simulation MUST default to "Partha's Spending Strategy-v0.01-20260105". A simulation saved without a strategy choice MUST load as that strategy.
- **FR006-004**: The system MUST save the chosen strategy and its settings with the simulation and restore them on load. Settings of a strategy that is not selected MUST be kept, so switching back restores them.
- **FR006-005**: The form MUST show only the settings that apply to the selected strategy. The settings that apply to every strategy (TDA withdrawal percentage, Roth conversions) MUST stay visible for every strategy. Today every setting of the existing strategy is shared, so the existing strategy has nothing to hide; the rule matters for strategies added later.
- **FR006-006**: Adding another strategy later MUST NOT change the results of simulations saved with an existing strategy. Each strategy is identified by a stable, versioned identifier.
- **FR006-007**: Results for "Partha's Spending Strategy-v0.01-20260105" MUST NOT change because of this feature.
- **FR006-008**: The selected strategy MUST apply to interactive simulations, Monte Carlo simulations, and the yearly breakdown.

**Risk-Based Guardrails settings**

- **FR006-009**: The user MUST be able to set:
  - Target probability of success, default 90%.
  - Upper guardrail, default 99%.
  - Lower guardrail, default 70%.
  - Review frequency, yearly (default) or quarterly.
- **FR006-010**: The system MUST reject settings unless 1% ≤ lower guardrail < target < upper guardrail ≤ 99%, with a message on each invalid field.

**Risk-Based Guardrails behaviour**

- **FR006-011**: The probability of success MUST be the share of possible market outcomes in which the portfolio lasts to the end age at the current spending, taking into account the remaining years, the current balances of all accounts, future passive income (Social Security), future regular expenses with inflation, scheduled one-time expenses and loan payments, and income tax.
- **FR006-011a**: The possible market outcomes for each review MUST be a fixed set of simulated market paths (500 by default), generated from the same historical market data as the Monte Carlo simulation and from a seed derived from the simulation's seed and the review's date. An interactive simulation has no seed of its own, so it uses one fixed seed. Each path starts from the review date's balances and runs to the end age, and spending follows the strategy's current spending with inflation. The probability of success is the share of paths in which the portfolio isn't used up before the end age. Each path is a yearly projection of the same cash flows as the simulation, with the portfolio treated as one pool; it doesn't apply the quarterly bucket rules (research R006-2). *(Q1, 2026-10-19; projection and fixed seed added in planning.)*
- **FR006-012**: Spending under guardrails MUST start at the configured expenses. At the first review, at the start of retirement, and at each later review:
  - Below the lower guardrail: Wants are reduced to the level at which the probability of success equals the target, but never below zero.
  - Above the upper guardrail: Wants are increased to the level at which the probability of success equals the target. Increases are capped at 150% of the configured Wants, adjusted for inflation.
  - Otherwise: spending is unchanged.

  Each adjustment MUST be accurate to within 0.5 percentage point of the target, or $100 a year of Wants.
- **FR006-013**: Between reviews, spending MUST grow with inflation, like the configured expenses.
- **FR006-014**: Guardrails MUST adjust only Wants. Needs, healthcare, property tax, income tax, one-time expenses and loan payments MUST never be adjusted. *(Q2, 2026-10-19.)*
- **FR006-015**: Under guardrails, the existing strategy's account rules (FR002-005) MUST decide which accounts pay for the spending. These rules are the SB floor and cap, the market-up and market-down refill rules, the CBB cap and the 12-month CBB check, the TDA withdrawal percentage, and the TFA as a last resort. The rules MUST work from the income gap of the adjusted spending, not the configured spending. Two things are different under guardrails. The 10% Wants cut in a down market doesn't apply, because guardrails set Wants. The SB cap's "50% of Wants" uses the adjusted Wants. *(Q3, 2026-10-19.)*
- **FR006-016**: Plan failure MUST be detected and reported the same way as for the existing strategy (FR002-005).
- **FR006-017**: Given the same configuration (and seed for Monte Carlo), the strategy MUST produce identical results every time, including every review.

**Results and explanation**

- **FR006-018**: Results for a guardrails simulation MUST include, for each year of retirement, the adjusted spending and the configured spending. For each review, results MUST include: the probability of success, the outcome (no change, cut, raise, or target unreachable), the spending before and after, and the portfolio values at the lower and upper guardrails. The portfolio values come with US006-3. If US006-1 and US006-2 ship before US006-3, those values are empty until it lands.
- **FR006-019**: The results summary MUST show the starting probability of success, the starting dollar guardrails, the number of cuts and raises, the largest cut as a percentage of spending, and the lowest and highest annual spending.
- **FR006-020**: The yearly breakdown (spec 004) MUST explain each guardrail review: its inputs, the guardrail crossed, the spending change, and the accounts that paid.
- **FR006-021**: The Monte Carlo summary for a guardrails simulation MUST add the 10th, 50th and 90th percentiles of annual spending in retirement, and the share of runs with at least one spending cut.
- **FR006-022**: Results for the existing strategy MUST keep their current fields and meaning. Guardrail-specific results appear only for guardrails simulations.

### Key Entities

- **Spending Strategy**: a named, versioned way of covering retirement spending. It has a stable identifier, a display name, a description, and its own settings. Each simulation selects one strategy.
- **Strategy Settings**: the parameters of one strategy. For Risk-Based Guardrails: target, upper guardrail, lower guardrail, and review frequency. Settings are saved for each strategy, so switching strategies keeps them.
- **Guardrail Review**: one review during retirement. It records the date, the portfolio value, the probability of success, the outcome, the spending before and after, and the portfolio values at the lower and upper guardrails.
- **Spending Path**: the spending for each year of retirement under the strategy, next to the configured spending.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC006-001**: 100% of simulations saved before this feature give the same results after it (checked against stored reference results).
- **SC006-002**: A user can switch a simulation to Risk-Based Guardrails, keep the defaults, and see results in under 1 minute, without leaving the simulation form.
- **SC006-003**: In 100% of the test scenarios, each spending adjustment brings the probability of success to within 0.5 percentage point of the target, and no review between the guardrails changes spending.
- **SC006-004**: For a fixed market sequence with a 30% crash in the first year of retirement, the guardrails strategy cuts spending at the first review after the crash, and the plan lasts at least as long as the same plan with fixed spending.
- **SC006-005**: An interactive guardrails simulation over a 30-year retirement shows results in under 3 seconds. A 1,000-run Monte Carlo guardrails simulation finishes in under 60 seconds.
- **SC006-006**: For any review, a user can read from the breakdown, without calculating, why spending changed or didn't and by how much.
- **SC006-007**: The same configuration and seed give identical results on 100% of repeated runs.

## Assumptions

- "Modern risk-based guardrails" means the probability-of-success guardrails described by Kitces, Tharp and Fitzpatrick (Income Lab). It does not mean the classic Guyton-Klinger withdrawal-rate guardrails, which could be added later as another strategy.
- The defaults (target 90%, upper 99%, lower 70%) follow the Kitces examples. Users can change them.
- After a guardrail is crossed, spending returns to the target in one step, the published rule. Partial adjustments could become a setting later.
- The first review is at the start of retirement, so the strategy starts from the user's configured expenses and corrects them if needed. There is no separate "solve for the starting spending" mode.
- Raises are capped at 150% of the configured Wants, to stop runaway increases late in retirement, when the probability of success approaches 100%.
- Under guardrails, the bucket rules of "Partha's Spending Strategy-v0.01-20260105" still decide which accounts pay (FR006-015). Its 10% Wants cut in a down market doesn't apply, because guardrails adjust Wants themselves.
- Inside a Monte Carlo run, each review's market paths are separate from the run's own market sequence. They are drawn only from the review's seed, so results are repeatable (FR006-017). The 500-path default trades accuracy, about ±2 percentage points on the probability of success, against run time (SC006-005). Measured runs are far inside the time targets, so the path count stays fixed. Around the guardrails, the pooled projection (FR006-011a) agrees with a Monte Carlo of the full simulation within about 3 points either way (research R006-2).
- Pre-retirement behaviour (salary, contributions, Roth conversions) is the same under every strategy. Strategies differ only from retirement on.
- One strategy per simulation. Switching strategies during retirement is out of scope.
- Comparing two strategies side by side in one view is out of scope. The user can save two copies of a simulation with different strategies.

## Clarifications

### Session 2026-10-19

- Q: How is the probability of success measured at each review? → A: A fixed set of simulated market paths (500 by default), from the same historical data and seed as the Monte Carlo engine (FR006-011a).
- Q: Which spending can guardrails change? → A: Wants only. Needs, healthcare and property tax are fixed, and Wants can't go below zero (FR006-012, FR006-014).
- Q: Are the SB and CBB managed as buckets under guardrails? → A: Yes. Guardrails set how much is spent, and the existing bucket rules decide which accounts pay (FR006-015).
