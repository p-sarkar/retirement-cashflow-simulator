# Speckit: Retirement Cash Flow Simulator

## Overview
**Purpose:** Web application for retirement planning that simulates 35-year cash flow projections (2025-2059) using market conditions, with deterministic and Monte Carlo analysis capabilities.

**Status:** 📋 Planning Complete - Ready for Development  
**Branch:** `001-retirement-cashflow`  
**Last Updated:** 2025-12-18

---

## Quick Start

### Prerequisites
- Node.js (for Vite + React frontend)
- Deno (for BFF gateway)
- Kotlin + Gradle (for API service)
- SQLite

### Project Structure
```
retirement-cash-flow-simulator/
├── frontend/          # React + Vite + TypeScript + MUI (to be created)
├── bff/              # Deno + Oak gateway (to be created)
├── api/              # Kotlin + Gradle simulation engine (to be created)
├── db/               # SQLite schema + migrations (to be created)
├── historical-data/  # Historical S&P 500 & inflation datasets
│   ├── s-p-500-annual-returns-since-1928.csv (98 years)
│   └── annual-inflation-since-1914.csv (112 years)
└── specs/            # Feature specifications
    └── 001-retirement-cashflow/
        ├── spec.md
        ├── plan.md
        ├── tasks.md
        └── checklists/
```

### Development (Planned)
```bash
# Frontend (when created)
cd frontend && npm run dev

# BFF Gateway (when created)
cd bff && deno task dev

# API Service (when created)
cd api && ./gradlew run
```

---

## Architecture

### System Design
**N-tier architecture** with clear separation of concerns:

```
Browser → BFF Gateway → Kotlin API → SQLite
          (Deno/Oak)    (Ktor)
```

**Responsibilities:**
- **Frontend:** Input collection, table rendering, Monte Carlo visualization
- **BFF:** Request validation, CORS, frontend-friendly API (`/api/*`)
- **Kotlin API:** Simulation engine, persistence, SQLite owner (`/internal/*`)
- **SQLite:** Single writer (owned by Kotlin only)

### Technology Stack
| Layer | Technology |
|-------|-----------|
| Frontend | React, Vite, TypeScript, MUI, React Router |
| BFF | Deno, Oak |
| API | Kotlin, Gradle, Ktor |
| Database | SQLite |
| Charts | ECharts (for Monte Carlo visualization) |

---

## Core Features

### 1. Deterministic Simulation (Priority: P1)
**User Story:** Select market condition, enter parameters, get 35-year projection

**Inputs:**
- Ages (current, retirement, Social Security claiming)
- Starting balances (spend bucket, crash buffer, taxable, tax-deferred, tax-free)
- Income (salary schedule, Social Security benefit)
- Expenses (essentials, discretionary, property tax, one-time goals)
- Market condition (preset or custom inflation + S&P returns)

**Output Table (35 rows, 2025-2059):**
| Group | Columns |
|-------|---------|
| **Balances** | Spend Bucket, Crash Buffer, Taxable, Tax-Deferred, Tax-Free |
| **Income** | Salary, Interest, Dividends, Stock Sales, Distributions, Conversions, Social Security, Total |
| **Expenses** | Essentials, Nice-to-Have, Income Tax, Property Tax, Total |
| **One-Time** | Goals |
| **Derived** | Income Gap |

**Features:**
- ✅ Inflation-adjusted expenses & income
- ✅ Tax estimation (Federal MFJ + NIIT + State)
- ✅ Funding hierarchy: Spend Bucket → Taxable → Tax-Deferred → Tax-Free
- ✅ Success/failure detection (negative balance = failure)
- ✅ Ending balance summary

### 2. Save & Reload (Priority: P2)
**User Story:** Persist simulations with optional names, list saved runs, reload any

**Persistence:**
- Inputs (all parameters + market condition)
- Full 35-year table
- Summary (ending balances, success/failure)
- Metadata (run date, user name)

### 3. Monte Carlo Analysis (Priority: P3)
**User Story:** Run 1000+ simulations, view percentile outcomes, explore individual paths

**Process:**
1. Bootstrap historical (inflation, S&P return) pairs
2. Generate ≥1000 paths (35 years each)
3. Calculate percentiles (median, 75th, 90th)
4. Plot trajectories with total balance vs. year

**Interactions:**
- Hover → highlight path
- Click → select path, show summary (success/failure, ending balance)
- "Load Details" → fetch full 35-year table for selected path

**Efficiency:**
- Store compact sequence references (not full tables)
- Recompute full details on-demand

---

## Data Model

### Market Condition
```typescript
{
  type: "preset" | "custom",
  id?: string,  // for presets
  inflation: number[] | number,  // 35 values or constant
  spReturn: number[] | number    // 35 values or constant
}
```

**Extension Rule:** If custom series < 35 years, repeat last value

### Simulation Input
```typescript
{
  currentYear: 2025,
  currentAge: number,
  retirementAge: number,
  ssClaimingAge: number,
  
  startingBalances: {
    spendBucket: number,
    crashBuffer: number,
    taxableBrokerage: number,
    taxDeferred: number,
    taxFree: number
  },
  
  income: {
    salarySchedule: YearlyValue[],
    ssBenefit: number
  },
  
  expenses: {
    essentials: number,  // base year 2025
    niceToHave: number,
    propertyTax2025: number,
    oneTimeGoals: { year: number, amount: number }[]
  },
  
  marketCondition: MarketCondition,
  
  taxOverrides?: { year: number, amount: number }[]
}
```

### Simulation Result
```typescript
{
  rows: YearRow[35],  // 2025-2059
  summary: {
    success: boolean,
    failureYear?: number,
    endingBalances: {
      spendBucket: number,
      crashBuffer: number,
      taxable: number,
      taxDeferred: number,
      taxFree: number,
      total: number
    }
  }
}
```

### Monte Carlo Result
```typescript
{
  paths: {
    index: number,
    trajectory: number[35],  // total balance per year
    summary: { success: boolean, failureYear?: number, endingTotal: number },
    sequenceRef: number[]  // sampled observation indices
  }[],
  percentiles: {
    median: number[35],
    p75: number[35],
    p90: number[35]
  }
}
```

---

## API Endpoints

### Public BFF API (Frontend → BFF)
All routes prefixed with `/api/*`:

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/market-conditions/presets` | List preset conditions |
| `POST` | `/api/simulations/deterministic` | Run deterministic (optional save) |
| `POST` | `/api/simulations/monte-carlo` | Run Monte Carlo (optional save) |
| `GET` | `/api/simulations` | List saved simulations |
| `GET` | `/api/simulations/:id` | Load saved simulation |
| `GET` | `/api/simulations/:id/monte-carlo/path/:pathIndex/details` | Load path details |
| `POST` | `/api/datasets/historical` | Upload custom historical data |

### Internal Kotlin API (BFF → Kotlin)
Routes prefixed with `/internal/*` (not browser-accessible):
- Mirror structure of BFF endpoints
- BFF validates & proxies all requests

---

## Business Rules

### Funding Hierarchy (FR-017)
When income < expenses (Income Gap), close gap in order:
1. **Spend Bucket** withdrawals (principal)
2. **Taxable Brokerage** stock sales
3. **Tax-Deferred** distributions
4. **Tax-Free** withdrawals

Roth conversions are separate (user-configured, counted as taxable income).

### Tax Estimation
**Federal (Married Filing Jointly):**
- Standard deduction (inflation-adjusted)
- Ordinary income brackets
- LTCG + qualified dividends brackets
- Social Security taxation (provisional income)
- NIIT (Net Investment Income Tax)

**State:**
- User-selectable (PA for MVP, expand later)
- State-specific brackets where implemented
- Fallback to transparent approximation

**Per-year overrides** always take precedence.

### Inflation Application (FR-007)
Auto-applies to:
- Salary (until retirement)
- Social Security (after claiming)
- Essentials
- Nice-to-Have
- Property Tax
- One-time goals

Unless user provides explicit per-year schedule.

### Failure Detection (FR-026)
- Simulation **fails** if any account balance < 0
- Stop after including failure year in results
- Report first failure year

---

## Historical Data

### S&P 500 Annual Returns
- **File:** `historical-data/s-p-500-annual-returns-since-1928.csv`
- **Period:** 1928-2026 (98 years)
- **Format:** `"Year End Date","Annual Return %"`
- **Example:** `"12/31/1928",37.88`

### Inflation Rates
- **File:** `historical-data/annual-inflation-since-1914.csv`
- **Period:** 1914-2026 (112 years)
- **Format:** `"Year End Date","Inflation Rate"`
- **Example:** `"12/31/1914",1`

**Note:** Both files use percentage values (e.g., `23.31` = 23.31%), not decimal rates.

---

## Development Roadmap

### Phase 1: Setup (T001-T005)
- [ ] Initialize directory structure (`api/`, `bff/`, `frontend/`, `db/`)
- [ ] Initialize Kotlin project (Gradle + Ktor)
- [ ] Initialize Deno project (Oak)
- [ ] Initialize React project (Vite + TypeScript + MUI)
- [ ] Setup SQLite + migration tool

### Phase 2: Foundational (T006-T009)
- [ ] Deno server skeleton + error handling
- [ ] Kotlin API server skeleton (Ktor)
- [ ] Frontend layout (MUI + Router)
- [ ] BFF proxy configuration

### Phase 3: User Story 1 - Deterministic (T010-T019)
**Goal:** Run single scenario projection

- [ ] Data models (Kotlin)
- [ ] Market condition logic (presets, custom, extension)
- [ ] Tax estimator (Federal + State)
- [ ] **Simulation engine** (year-by-year, funding order)
- [ ] API endpoints (Kotlin + BFF)
- [ ] Input form (React)
- [ ] Results table (35 rows, all columns)
- [ ] Summary view (ending balances, success/failure)
- [ ] Integration

### Phase 4: User Story 2 - Persistence (T020-T026)
**Goal:** Save/reload simulations

- [ ] SQLite schema (`simulations` table)
- [ ] Persistence repository (Kotlin)
- [ ] Save/Load/List endpoints (Kotlin + BFF)
- [ ] Save dialog (React)
- [ ] Saved simulations list page
- [ ] Reload logic

### Phase 5: User Story 3 - Monte Carlo (T027-T035)
**Goal:** Run 1000+ paths with interactive visualization

- [ ] CSV parser for historical data
- [ ] Monte Carlo engine (bootstrapping, 1000+ paths)
- [ ] Percentile calculation
- [ ] SQLite schema (`monte_carlo_paths` table)
- [ ] MC endpoints (run, load details)
- [ ] MC chart component (ECharts, hover/click)
- [ ] MC results page
- [ ] Custom dataset upload

### Phase 6: Polish (T036-T038)
- [ ] Error handling + validation
- [ ] MUI theming
- [ ] CSV export (deterministic results)

---

## Testing Strategy

### Kotlin (Simulation Engine)
**Unit Tests:**
- 35 rows generated for 2025-2059 ✓
- Funding order applied correctly ✓
- Failure year detection ✓
- Inflation extension rule (FR-009) ✓
- Total Income = Total Expenses (pre-failure) ✓

**Integration Tests:**
- SQLite persistence (save/load round-trip) ✓

### Deno (BFF)
**API Contract Tests:**
- Request validation ✓
- Proxy behavior ✓
- Error response shaping ✓

### React (Frontend)
**Component Tests:**
- Form validation ✓
- Table rendering ✓

**Smoke Tests:**
- MC path selection → load details ✓

---

## Edge Cases

| Scenario | Handling |
|----------|----------|
| Current age > retirement age | ❌ Reject (invalid input) |
| Negative starting balances | ❌ Reject (invalid input) |
| Custom market condition missing values | ❌ Reject (invalid input) |
| Custom series < 35 years | ✅ Extend by repeating last value (FR-009) |
| One-time goal outside 2025-2059 | ❌ Reject (invalid input) |
| Simulation fails mid-horizon | ✅ Show failure year + rows up to failure |
| Monte Carlo N < 1000 | ❌ Reject or auto-round up to 1000 |

---

## Success Criteria

- **SC-001:** Generate 35-row table in <60s without external docs
- **SC-002:** 100% of pre-failure years satisfy Total Income = Total Expenses
- **SC-003:** Save/reload reproduces identical outputs
- **SC-004:** Monte Carlo plots ≥1000 paths with visible percentiles

---

## References

### Specification Documents
- **Feature Spec:** `specs/001-retirement-cashflow/spec.md`
- **Architecture Plan:** `specs/001-retirement-cashflow/plan.md`
- **Task Breakdown:** `specs/001-retirement-cashflow/tasks.md`
- **Requirements Checklist:** `specs/001-retirement-cashflow/checklists/requirements.md`

### Key Functional Requirements
- **FR-001:** 35 rows (2025-2059)
- **FR-009:** Extend short series by repeating last value
- **FR-016/FR-017:** Balance via funding hierarchy
- **FR-026:** Failure = any negative balance
- **FR-032:** Monte Carlo ≥1000 paths
- **FR-033:** Bootstrap historical data

---

## Contributors & Contact
**Project Owner:** Partha Sarkar  
**Repository:** `retirement-cash-flow-simulator`  
**Branch:** `001-retirement-cashflow`

---

*Generated: 2025-12-19*
*Speckit Version: 1.0*
