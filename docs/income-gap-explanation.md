# Income Gap Explanation

## What is the Income Gap?

The **Income Gap** (also called **Annual Income Gap** or **AIG**) is the amount of money that must be withdrawn from retirement accounts each year to cover living expenses that are NOT covered by passive income sources.

## The Formula

```
Income Gap = Total Expenses - Passive Income
```

Where:
- **Total Expenses** = Needs + Wants + Healthcare + Property Tax + Income Tax
- **Passive Income** = Interest (from Spend Bucket) + Dividends (from Crash Buffer) + Social Security

## Why It Matters

The Income Gap represents the **shortfall** between what you need to spend and what you're earning passively. This gap must be filled by withdrawing from:

1. **TBA Withdrawal** - Selling stocks from Taxable Brokerage Account
2. **TDA Withdrawal** - Distributions from Tax-Deferred Account (Traditional 401k/IRA)
3. **TFA Withdrawal** - Roth IRA withdrawals (last resort)

**Note:** Roth conversions are handled separately as an investment strategy, not as part of the living expenses gap.

## Example Calculation

### Year 2025 Example:
```
Expenses:
  Needs:         $50,000
  Wants:         $100,000
  Healthcare:    $5,000
  Property Tax:  $12,000
  Income Tax:    $0 (no prior year income)
  ──────────────────────
  Total:         $167,000

Passive Income:
  Interest (SB): $12,000  (SB balance $300k × 4% HYSA rate)
  Dividends:     $40,000  (CBB balance $800k × 5% bond yield)
  Soc. Security: $0       (not yet claiming)
  ──────────────────────
  Total:         $52,000

Income Gap = $167,000 - $52,000 = $115,000
```

This means you need to withdraw **$115,000** from your retirement accounts to cover expenses.

## How It's Calculated in Code

### Base AIG (Year 0)
```kotlin
val baseAig = (needs + wants + propertyTax + healthcare) - 
              (SB × hysaRate) - 
              (CBB × bondYield)
```

This establishes the baseline gap at retirement, assuming:
- Healthcare uses post-retirement pre-Medicare rate as baseline
- No Social Security yet
- Current starting balances for SB and CBB

### Yearly AIG
```kotlin
// Calculated fresh each year from actual expenses and passive income
val totalExpenses = needsAdjusted + wantsAdjusted + healthcareAdjusted + propertyTaxAdjusted + annualTaxDue
val passiveIncome = annualInterest + annualDividends + annualSocialSecurity
val currentAig = totalExpenses - passiveIncome
```

Each year, the Income Gap is calculated from:
1. **Actual inflated expenses** for that year (needs, wants, healthcare, property tax, prior year income tax)
2. **Actual passive income** earned that year (interest on SB balance, dividends on CBB balance, Social Security)
3. The gap = expenses - passive income

This means the gap can vary based on:
- Changing healthcare costs (pre-retirement vs post-retirement vs Medicare)
- Social Security benefits starting
- Actual account balances (which affect interest/dividend income)
- Tax bills from prior year income

## Frontend Display Breakdown

The Income Gap is displayed with a hierarchical breakdown:

### Main Column: **Income Gap** (Bold)
The total shortfall amount

### Level 1 Breakdown:
- **└─ Gap Expenses** - Total expenses that create the gap
- **└─ Passive Income** - Income that reduces the gap

### Level 2 Breakdown (under Gap Expenses):
- **⤷ Needs** - Essential living expenses
- **⤷ Wants** - Discretionary spending
- **⤷ Healthcare** - Medical expenses (varies by age/Medicare)
- **⤷ Property Tax** - Real estate taxes (inflated yearly)
- **⤷ Income Tax** - Estimated taxes on taxable income

## Key Insights

### Pre-Retirement (Age < Retirement Age)
- Income Gap is typically **negative** or low
- Salary covers most/all expenses
- Accounts are growing through:
  - Contributions (401k, TBA)
  - Market returns
  - Compounding interest/dividends

### Post-Retirement (Age >= Retirement Age)
- Income Gap becomes **positive** and significant
- No more salary
- Must withdraw from retirement accounts to cover gap
- Gap increases yearly due to inflation
- May decrease when:
  - Social Security kicks in
  - Healthcare costs drop (Medicare at 65)
  - Lifestyle downsizing (lower wants)

## The Spending Strategy

The simulation uses the Income Gap to determine how much to withdraw from the Spend Bucket (SB) each month.

**Key principle:** The SB withdrawal equals the Income Gap, NOT the total expenses, because:
- Passive income (interest, dividends, Social Security) is deposited into the SB
- Therefore, we only need to withdraw the **gap** (what passive income doesn't cover)

**Monthly SB Withdrawal = Income Gap / 12**

Example:
```
Total Expenses: $167,000/year = $13,917/month
Passive Income: $52,000/year = $4,333/month (deposited to SB)
Income Gap:     $115,000/year = $9,583/month (withdrawn from SB)
```

The Spend Bucket acts as the central cash account:
- **Deposits:** Salary, interest, dividends, Social Security, withdrawals from investment accounts
- **Withdrawals:** Monthly income gap to cover living expenses

When the SB runs low, the spending strategy refills it by withdrawing from investment accounts in priority order:

1. **Taxable Brokerage** - First (lower tax rate on capital gains)
2. **Tax-Deferred** - Second (ordinary income tax rate)
3. **Tax-Free (Roth)** - Last resort (preserve tax-free growth)

Additionally, the **Crash Buffer** (CBB) is refilled quarterly when needed to maintain 4× the AIG as a safety net.

## Visual Hierarchy in Table

```
Income Gap: $115,000                    [BOLD - Main value]
  └─ Gap Expenses: $167,000            [Gray - Total expenses]
      ⤷ Needs: $50,000                 [Light gray - Component]
      ⤷ Wants: $100,000                [Light gray - Component]
      ⤷ Healthcare: $5,000             [Light gray - Component]
      ⤷ Property Tax: $12,000          [Light gray - Component]
      ⤷ Income Tax: $0                 [Light gray - Component]
  └─ Passive Income: $52,000           [Gray - Total passive]
```

## Important Notes

1. **Roth Conversions are separate** - Roth conversions are an investment/tax strategy and are NOT included in the Income Gap calculation. The gap only tracks living expenses.

2. **Income Tax creates a feedback loop** - Higher withdrawals → higher taxable income → higher taxes next year → higher gap

3. **Inflation compounds** - A $100k gap today becomes ~$180k in 20 years at 3% inflation

4. **CBB Cap tracks the Gap** - The Crash Buffer is maintained at 4× the current AIG to provide ~1 year of safety during market downturns

## Data Flow

```
API Server (Kotlin)                Frontend (React/TypeScript)
─────────────────────             ──────────────────────────

SimulationEngine.kt               ResultsTable.tsx
  ↓                                 ↓
Calculates:                       Displays:
  annualIncomeGap                   Income Gap (bold)
  incomeGapExpenses                 └─ Gap Expenses
  incomeGapPassiveIncome            └─ Passive Income
  ↓                                      ↓
Stores in Metrics                 Shows detailed breakdown
  ↓                                 with expense components
Returns via API
  ↓
frontend/services/api.ts
```

## Related Concepts

- **CBB Cap** - Set at 4× Income Gap for safety buffer
- **Total Expenses** - All spending for the year
- **Total Income** - All income sources (including withdrawals)
- **Salary** - Pre-retirement income (goes to $0 at retirement)
- **Social Security** - Passive income that kicks in at claiming age

