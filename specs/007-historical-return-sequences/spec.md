# Feature Specification: Historical Return Sequences

**Feature Branch**: `feat/historical-sequences`  
**Created**: 2026-10-19  
**Status**: Draft  
**Input**: User description: "Augment the historical SPX data to the most recent month end. Then add an option to use a random historical returns sequence, or one starting with a particular month and year."

## Background

An interactive simulation uses either fixed growth and inflation rates (Custom, or a preset's), or, for the 2008 Crash preset, five historical years replayed at retirement. Monte Carlo draws random historical years but re-centres them on the configured rates, so no run replays history as it happened.

This feature lets an interactive simulation replay an actual stretch of market history: S&P 500 returns and inflation year by year, from a starting month the user picks or one chosen at random. A user can then ask, for example, "what if I had retired in January 2000?" and see the dot-com crash and 2008 hit the plan in order.

The S&P 500 month-end data was extended to September 2026 on this feature's branch. Annual inflation runs through 2025.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Replay history from a chosen month (Priority: P1)

Under Market Conditions, the user chooses **Historical sequence**, picks **Start at** and a month and year (for example, March 2000), and runs the simulation. Each simulated year, working years included, uses the S&P 500 return and inflation of the matching 12 months of history: the first year uses March 2000 to February 2001, the second March 2001 to February 2002, and so on.

**Why this priority**: This is the core of the feature: a specific, nameable piece of history the user can reason about.

**Independent Test**: Choose a start of January 2000 and run. The first simulated year's equity return equals the S&P 500's 2000 calendar-year total return, dividends included (about −9.1%), the next two are 2001's and 2002's, and the inflation shown each year is that year's.

**Acceptance Scenarios**:

1. **Given** Historical sequence with a start of January 2000, **When** the user runs the simulation, **Then** year 1 uses the S&P 500's January–December 2000 return and that year's inflation, year 2 uses 2001's, and so on.
2. **Given** a start of March 2000, **When** the simulation runs, **Then** year 1 uses the return and inflation from March 2000 to February 2001.
3. **Given** a start that runs out of historical data before the End Age, **When** the simulation runs, **Then** every year after the last complete 12-month window uses the historical average return and inflation, with no wraparound to earlier history.
4. **Given** Historical sequence is chosen, **When** the user looks at the form, **Then** inflation and pre- and post-retirement growth are greyed out, as for a preset, while bond yield, HYSA rate and income tax stay editable.

---

### User Story 2 - Replay history from a random start (Priority: P1)

The user chooses **Historical sequence** with **Random start** and runs. A starting month is drawn at random from all valid starts. The form and results show which month was drawn, so the user knows what history they're seeing. Running again keeps the same start; a **Draw another** action picks a new one.

**Why this priority**: The user asked for it explicitly. A random start shows the range of outcomes history can produce without the user choosing a period they already know.

**Independent Test**: Choose Random start and run: a starting month is shown, the results match a run with that month chosen explicitly, and running again gives the same results until the user draws another.

**Acceptance Scenarios**:

1. **Given** Random start, **When** the user runs, **Then** a starting month is drawn from the valid starts and shown in the form and the results.
2. **Given** a drawn start, **When** the user runs again without changing it, **Then** the results are identical.
3. **Given** a drawn start, **When** the user chooses Draw another and runs, **Then** a new start is drawn and shown.
4. **Given** a drawn start of, for example, June 1965, **When** the user compares it with Start at June 1965, **Then** the results are identical.

---

### User Story 3 - See which history each year replays (Priority: P2)

In the results table and the yearly breakdown, each simulated year shows the 12 months of history it replays (for example, "Mar 2000 – Feb 2001"). Years past the end of the data are marked as using the historical average.

**Why this priority**: Without it, the user can't connect a year's result to the history behind it, but the simulation is correct without it.

**Independent Test**: Run a sequence that runs out of data before the End Age, and confirm every row before the cut-over names its window and every row after it says "historical average".

**Acceptance Scenarios**:

1. **Given** a historical-sequence run, **When** the user views the results table, **Then** each year shows its history window or "historical average".
2. **Given** a historical-sequence run, **When** the user opens a year's breakdown, **Then** the Starting Position section names the window behind that year's equity return and inflation, or says the historical average was used.

---

### User Story 4 - Save and reload a historical sequence (Priority: P3)

Saving a simulation keeps the historical-sequence choice and its start month, including a randomly drawn one. Loading it restores both, so re-running gives the same results.

**Why this priority**: It matters for comparing plans later, and saved simulations already keep their results.

**Independent Test**: Save a random-start simulation, reload it, run it, and confirm the same start month and results.

**Acceptance Scenarios**:

1. **Given** a saved historical-sequence simulation, **When** the user loads it, **Then** the form shows Historical sequence with its start month, and running gives the saved results.

---

### Edge Cases

- **Start near the end of the data**: the latest valid start is the month whose 12-month window ends with the last available month of data. With S&P 500 data through September 2026, the latest start is October 2025. Later starts aren't offered, and the API rejects them.
- **Earliest start**: the earliest valid start is the first month with a complete 12-month window of both returns and inflation: January 1928, using the December 1927 close.
- **Simulation longer than the remaining data**: the historical years run out and the historical averages take over (US007-1 scenario 3). No wraparound.
- **Simulation shorter than the remaining data**: only the needed years are used.
- **Market crash in the replayed history**: the existing strategy's market-up and market-down rules (FR002-005) react to the replayed returns, the same as for any return sequence.
- **Monte Carlo with Historical sequence chosen**: Monte Carlo keeps its random-year bootstrap and uses the configured growth and inflation fields (FR007-011). The form says so.
- **Risk-Based Guardrails with Historical sequence**: the simulated years replay history, but each guardrail review still measures the probability of success over its own simulated paths at the configured rates, as in spec 006. The reviews don't see the future.
- **Invalid start**: a start before January 1928, after the latest valid start, or not a real month is rejected with a per-field message.

## Requirements *(mandatory)*

### Functional Requirements

**Choosing a sequence**

- **FR007-001**: Market Conditions MUST offer **Historical sequence** beside Custom and the presets, for interactive simulations.
- **FR007-002**: Historical sequence MUST offer two starts: **Start at** a month and year the user chooses, and **Random start**.
- **FR007-003**: Valid starts MUST run from January 1928 to the latest month whose 12-month window ends within the available data (October 2025 with data through September 2026). The form MUST offer only valid starts, and the system MUST reject others with a per-field message.
- **FR007-004**: Random start MUST draw a start from the valid starts, show it in the form and results, and keep it for later runs until the user chooses **Draw another** or changes the start mode.
- **FR007-005**: While Historical sequence is chosen, inflation and pre- and post-retirement growth MUST be greyed out (disabled, values still shown), as for a preset (FR002-003). Bond yield, HYSA rate and income tax stay editable.

**Replaying history**

- **FR007-006**: Simulated year *n* (counting the first simulated year as 1), working years included, MUST use the S&P 500 total return and the inflation of the 12 months starting *n − 1* years after the start month: the total return from the month-end closes plus the dividends paid in those 12 months, and inflation over the same 12 months.
- **FR007-007**: Values MUST be the actual historical values, not re-centred on the configured growth and inflation rates.
- **FR007-008**: Every year after the last complete 12-month window MUST use the compound (geometric) average annual total return and inflation of all complete calendar years in the data: the constant rates that turn the first year's value into the last year's. There is no wraparound. *(Q1, 2026-10-19. For scale: the compound price return for 1928–2025 is 6.27% and inflation 3.03%; the total return, with dividends, will be higher.)*
- **FR007-009**: Bond yields MUST keep the configured rate: there is no bond history.
- **FR007-010**: The same start MUST always give the same results (constitution IV), whether it was chosen or drawn.

**Other modes**

- **FR007-011**: Monte Carlo MUST keep its random-year bootstrap, re-centred on the growth and inflation fields, whatever Market Conditions is set to. Running Monte Carlo with Historical sequence chosen MUST say that the sequence applies to interactive runs only. Its historical years MUST use total returns (FR007-016).
- **FR007-012**: Risk-Based Guardrails reviews (spec 006) MUST keep measuring the probability of success over their own simulated paths. They MUST NOT use the replayed history's future years.

**Results and persistence**

- **FR007-013**: Each simulated year in the results table and the CSV export MUST show the history window it replays (for example, "Mar 2000 – Feb 2001") or "Historical average".
- **FR007-014**: The breakdown's Starting Position MUST name the window behind the year's equity return and inflation, or say the historical average was used (extends FR004-010).
- **FR007-015**: A saved simulation MUST keep the Historical sequence choice, the start mode and the start month, and restore them on load.
- **FR007-016**: The S&P 500 returns MUST be total returns, dividends included, both in historical sequences and in the Monte Carlo bootstrap, and with it Risk-Based Guardrails' review paths (spec 006), which draw from the same history. *(Q2, 2026-10-19.)* Monte Carlo re-centres each year on the configured growth rate, so this changes the spread of its draws, not their average.

### Key Entities

- **Historical sequence choice**: part of the market conditions of a simulation. It holds the start mode (chosen or random) and the start month (the chosen one, or the one drawn).
- **History window**: the 12 months of history a simulated year replays. It has a first and a last month, the S&P 500 return over it and the inflation over it.
- **Historical average**: the return and inflation used for each year after the data runs out.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC007-001**: For a start of January in any year from 1928 to 2025, every replayed year's equity return matches that calendar year's published S&P 500 total return to within 0.5 percentage point, and its inflation matches the published calendar-year inflation.
- **SC007-002**: A user can switch to Historical sequence, pick a start, and see results in under 1 minute, without leaving the form.
- **SC007-003**: 100% of repeated runs with the same start give identical results, whether the start was chosen or drawn.
- **SC007-004**: In 100% of runs that outlast the data, no year after the cut-over uses a historical value, and every such year uses the historical average.
- **SC007-005**: For every replayed year, a user can see, without calculating, which 12 months of history it replays.
- **SC007-006**: Results for Custom and the presets are unchanged by this feature. Monte Carlo results change only because its historical years include dividends (FR007-016); its average return still equals the configured growth rate.

## Assumptions

- Interactive simulations only. Monte Carlo keeps its bootstrap (decided with the user).
- Actual historical values, not re-centred (decided with the user).
- The sequence starts in the first simulated year, working years included. No wraparound: after the data, the historical averages apply (decided with the user).
- Month-aligned inflation needs monthly price-level data. The existing inflation file is calendar-year only, so a monthly consumer price index series covering December 1927 onwards will be added (for example, the U.S. CPI for all urban consumers, not seasonally adjusted). January starts must match the existing calendar-year inflation.
- The latest valid start depends on the end of whichever series ends first: S&P 500 closes, monthly dividends or monthly prices. Total returns need monthly S&P 500 dividend history from December 1927, which will be added (for example, from Robert Shiller's dataset, a copy of which is in `historical-data/reference/`, extended to the latest month from a public source).
- Total returns assume dividends are reinvested at each month-end.
- The random draw is uniform over the valid starts.
- The 2008 Crash preset keeps its behaviour: five years replayed at retirement, re-centred rates otherwise. Historical sequence is a separate choice.
- Updating the data later (a newer month-end) moves the latest valid start and the historical averages. Saved simulations keep the results they were saved with.

## Clarifications

### Session 2026-10-19

- Q: Where is the historical sequence available? → A: Interactive runs only; Monte Carlo keeps its bootstrap.
- Q: Actual or re-centred values? → A: Actual historical values.
- Q: When does the sequence start, and what happens after the data? → A: From the first simulated year; no wraparound; the historical averages after the data.
- Q: Which average after the data? → A: The compound (geometric) average (FR007-008).
- Q: Price or total returns? → A: Total returns, in historical sequences and in the Monte Carlo bootstrap (FR007-016).
