# Data Model: One-Time Expenses

**Feature**: 003-one-time-expenses  
**Date**: 2026-01-20  
**Status**: Complete

---

## Overview

This document defines the data structures for the one-time expenses feature, including entity models, relationships, validation rules, and state transitions.

---

## Core Entities

### 1. OneTimeExpense (Sealed Interface)

Base interface for all one-time expenses. Uses sealed interface pattern in Kotlin for type-safe polymorphism.

**Properties**:
- `id: String` - Unique identifier (UUID)
- `name: String` - User-provided expense description (max 100 characters)

**Subtypes**:
- `CashExpense` - One-time lump sum payment
- `LoanExpense` - Fixed-term loan with amortized payments

**Kotlin Definition**:
```kotlin
@Serializable
sealed interface OneTimeExpense {
    val id: String
    val name: String
}
```

**TypeScript Definition**:
```typescript
export type OneTimeExpense = CashExpense | LoanExpense;

export interface BaseExpense {
  id: string;
  name: string;
}
```

---

### 2. CashExpense

Represents a one-time lump sum payment in a specific year.

**Properties**:
| Property | Type | Required | Validation | Description |
|----------|------|----------|------------|-------------|
| `id` | String | Yes | UUID format | Unique identifier |
| `name` | String | Yes | 1-100 chars, HTML escaped | Expense description |
| `amount` | Double | Yes | > 0, < 1e12 | Dollar amount |
| `yearOrAge` | YearOrAge | Yes | Valid year or age | When expense occurs |

**Kotlin Definition**:
```kotlin
@Serializable
@SerialName("CASH")
data class CashExpense(
    override val id: String,
    override val name: String,
    val amount: Double,
    val yearOrAge: YearOrAge
) : OneTimeExpense
```

**TypeScript Definition**:
```typescript
export interface CashExpense extends BaseExpense {
  type: 'CASH';
  amount: number;
  yearOrAge: YearOrAge;
}
```

**Validation Rules**:
- `amount > 0` (FR-037)
- `amount < 1e12` (reasonable upper bound)
- `name.trim().length >= 1` (FR-002)
- `name.length <= 100` (reasonable limit)
- `yearOrAge` must be within simulation period (FR-041)

**Behavior**:
- Paid as lump sum in January of specified year (FR-010)
- Deducted from Spend Bucket (FR-011)
- Triggers spending strategy if SB insufficient (FR-026)

---

### 3. LoanExpense

Represents a fixed-term loan with monthly amortized payments.

**Properties**:
| Property | Type | Required | Validation | Description |
|----------|------|----------|------------|-------------|
| `id` | String | Yes | UUID format | Unique identifier |
| `name` | String | Yes | 1-100 chars, HTML escaped | Loan description |
| `principal` | Double | Yes | > 0, < 1e12 | Loan amount |
| `aprPercent` | Double | Yes | >= 0, <= 99.99 | Annual percentage rate |
| `termYears` | Int | Yes | > 0, <= 50 | Loan duration in years |
| `startYearOrAge` | YearOrAge | Yes | Valid year or age | When loan starts |
| `monthlyPayment` | Double | Yes (calculated) | Auto-calculated | Monthly payment amount |

**Kotlin Definition**:
```kotlin
@Serializable
@SerialName("LOAN")
data class LoanExpense(
    override val id: String,
    override val name: String,
    val principal: Double,
    val aprPercent: Double,
    val termYears: Int,
    val startYearOrAge: YearOrAge,
    val monthlyPayment: Double
) : OneTimeExpense {
    // Derived property
    fun getEndYear(currentAge: Int, currentYear: Int): Int {
        val startYear = when (startYearOrAge) {
            is YearOrAge.Year -> startYearOrAge.year
            is YearOrAge.Age -> currentYear + (startYearOrAge.age - currentAge)
        }
        return startYear + termYears - 1
    }
    
    fun getAnnualPayment(): Double = monthlyPayment * 12
}
```

**TypeScript Definition**:
```typescript
export interface LoanExpense extends BaseExpense {
  type: 'LOAN';
  principal: number;
  aprPercent: number;
  termYears: number;
  startYearOrAge: YearOrAge;
  monthlyPayment: number;
}
```

**Validation Rules**:
- `principal > 0` (FR-037)
- `aprPercent >= 0` (FR-038)
- `aprPercent <= 99.99` (reasonable upper bound)
- `termYears > 0` (FR-039)
- `termYears <= 50` (reasonable upper bound)
- `name.trim().length >= 1` (FR-002)
- `startYearOrAge` must be within simulation period (FR-041)
- `endYear >= startYear` (FR-040)

**Derived Properties**:
- `endYear = startYear + termYears - 1` (FR-020)
- `annualPayment = monthlyPayment × 12` (FR-024)

**Behavior**:
- Monthly payments deducted from Spend Bucket (FR-019)
- Payments occur monthly throughout loan term (FR-020)
- Each payment independently triggers spending strategy if SB insufficient (FR-026)

**Calculation**:
```kotlin
fun calculateMonthlyPayment(principal: Double, aprPercent: Double, termYears: Int): Double {
    val n = termYears * 12
    
    if (aprPercent == 0.0) {
        return principal / n  // FR-017
    }
    
    val r = aprPercent / 100.0 / 12.0
    val onePlusR = 1.0 + r
    val onePlusRPowN = onePlusR.pow(n)
    
    // M = P[r(1+r)^n] / [(1+r)^n - 1]  (FR-016)
    return (principal * r * onePlusRPowN) / (onePlusRPowN - 1.0)
}
```

---

### 4. YearOrAge (Sealed Interface)

Represents timing specification in either calendar year or age format.

**Subtypes**:
- `Year` - Calendar year specification
- `Age` - Age-based specification

**Kotlin Definition**:
```kotlin
@Serializable
sealed interface YearOrAge {
    @Serializable
    @SerialName("YEAR")
    data class Year(val year: Int) : YearOrAge
    
    @Serializable
    @SerialName("AGE")
    data class Age(val age: Int) : YearOrAge
}
```

**TypeScript Definition**:
```typescript
export type YearOrAge = 
  | { type: 'YEAR'; year: number }
  | { type: 'AGE'; age: number };
```

**Conversion Logic**:
```kotlin
fun YearOrAge.toYear(currentAge: Int, currentYear: Int): Int {
    return when (this) {
        is YearOrAge.Year -> year
        is YearOrAge.Age -> currentYear + (age - currentAge)
    }
}

fun YearOrAge.toAge(currentAge: Int, currentYear: Int): Int {
    return when (this) {
        is YearOrAge.Year -> currentAge + (year - currentYear)
        is YearOrAge.Age -> age
    }
}
```

**Validation Rules**:
- For `Year`: `year >= currentYear` (no historical years)
- For `Age`: `age >= currentAge` (no historical ages)
- For both: must be within simulation period (currentYear to currentYear + 35)

---

### 5. ExpenseDetail

Detail record for breakdown display when multiple expenses occur in same year.

**Properties**:
| Property | Type | Description |
|----------|------|-------------|
| `name` | String | Expense name |
| `amount` | Double | Dollar amount for this expense in this year |
| `type` | ExpenseType | "CASH" or "LOAN_PAYMENT" |

**Kotlin Definition**:
```kotlin
@Serializable
data class ExpenseDetail(
    val name: String,
    val amount: Double,
    val type: ExpenseType
)

@Serializable
enum class ExpenseType {
    CASH,
    LOAN_PAYMENT
}
```

**TypeScript Definition**:
```typescript
export interface ExpenseDetail {
  name: string;
  amount: number;
  type: 'CASH' | 'LOAN_PAYMENT';
}
```

**Usage**:
- Created for each expense that occurs in a year
- Displayed in breakdown modal dialog (FR-033)
- For loans, amount is annual payment total (12 × monthlyPayment)

---

## Integration with Existing Models

### SimulationConfig Changes

Add `oneTimeExpenses` field to existing `SimulationConfig` data class.

**Kotlin**:
```kotlin
@Serializable
data class SimulationConfig(
    val id: String? = null,
    val name: String,
    val currentYear: Int,
    val currentAge: Int,
    val retirementAge: Int,
    val salary: Double,
    val portfolio: Portfolio,
    val spousal: SpousalDetails,
    val expenses: ExpenseConfig,
    val contributions: ContributionConfig,
    val rates: RateConfig,
    val strategy: StrategyConfig,
    val oneTimeExpenses: List<OneTimeExpense> = emptyList()  // NEW
)
```

**TypeScript**:
```typescript
export interface SimulationConfig {
  id?: string;
  name: string;
  currentYear: number;
  currentAge: number;
  retirementAge: number;
  salary: number;
  portfolio: Portfolio;
  spousal: SpousalDetails;
  expenses: ExpenseConfig;
  contributions: ContributionConfig;
  rates: RateConfig;
  strategy: StrategyConfig;
  oneTimeExpenses: OneTimeExpense[];  // NEW
}
```

**Migration**:
- Default value `emptyList()` ensures backward compatibility
- Existing saved configurations load without one-time expenses
- New configurations can include expenses

---

### CashFlow Changes

Add field to track one-time expenses in cash flow.

**Kotlin**:
```kotlin
@Serializable
data class CashFlow(
    // ...existing fields...
    val totalExpenses: Double,
    val oneTimeExpenses: Double = 0.0  // NEW
)
```

**TypeScript**:
```typescript
export interface CashFlow {
  // ...existing fields...
  totalExpenses: number;
  oneTimeExpenses: number;  // NEW
}
```

**Calculation**:
- Sum of all one-time expenses paid in this period (year or quarter)
- Includes cash expenses and loan payments
- Added to `totalExpenses` calculation (FR-027)

---

### YearlyResult Changes

Add field for expense breakdown when multiple expenses occur.

**Kotlin**:
```kotlin
@Serializable
data class YearlyResult(
    val year: Int,
    val age: Int,
    val balances: Portfolio,
    val cashFlow: CashFlow,
    val metrics: Metrics,
    val oneTimeExpensesBreakdown: List<ExpenseDetail>? = null  // NEW, nullable
)
```

**TypeScript**:
```typescript
export interface YearlyResult {
  year: number;
  age: number;
  balances: Portfolio;
  cashFlow: CashFlow;
  metrics: Metrics;
  oneTimeExpensesBreakdown?: ExpenseDetail[];  // NEW, optional
}
```

**Population Rules**:
- `null` or omitted when 0 or 1 expense in year (FR-030)
- Populated when 2+ expenses occur in same year (FR-031)
- Contains list of all expenses with names and amounts

---

## Validation Rules Summary

### Cash Expense Validation

| Rule | Requirement | Error Message |
|------|-------------|---------------|
| Name required | FR-002 | "Expense name is required" |
| Name max length | - | "Expense name must be 100 characters or less" |
| Amount positive | FR-037 | "Amount must be greater than zero" |
| Amount reasonable | - | "Amount must be less than $1 trillion" |
| Timing valid | FR-041 | "Expense must occur within simulation period (2026-2061)" |
| No historical timing | - | "Expense cannot occur in the past" |

### Loan Expense Validation

| Rule | Requirement | Error Message |
|------|-------------|---------------|
| Name required | FR-002 | "Loan name is required" |
| Name max length | - | "Loan name must be 100 characters or less" |
| Principal positive | FR-037 | "Principal must be greater than zero" |
| Principal reasonable | - | "Principal must be less than $1 trillion" |
| APR non-negative | FR-038 | "APR cannot be negative" |
| APR reasonable | - | "APR must be 99.99% or less" |
| Term positive | FR-039 | "Term must be greater than zero" |
| Term reasonable | - | "Term must be 50 years or less" |
| Start timing valid | FR-041 | "Loan must start within simulation period" |
| No historical start | - | "Loan cannot start in the past" |
| End after start | FR-040 | "Loan end year must be after or equal to start year" |

---

## State Transitions

### Expense Lifecycle

```
1. Created (Frontend)
   ↓
2. Validated (Frontend + Backend)
   ↓
3. Stored (SQLite via SimulationConfig)
   ↓
4. Retrieved (On simulation request)
   ↓
5. Processed (During simulation)
   ↓
6. Results Generated (YearlyResult with breakdown)
   ↓
7. Displayed (Results table + modal)
```

### Simulation Processing Flow

```
For each year in simulation:
    1. Process regular income
    2. Process regular expenses
    3. Get one-time expenses for this year
    4. For each one-time expense:
        a. Calculate payment amount
        b. Check Spend Bucket balance
        c. If insufficient, trigger spending strategy
        d. Deduct from Spend Bucket
        e. Record in cashFlow.oneTimeExpenses
        f. Add to breakdown list
    5. If breakdown.length > 1, add to yearlyResult.oneTimeExpensesBreakdown
```

### Cash Expense Processing

```
When year == expense.year:
    1. amount = expense.amount
    2. if (spendBucket < amount):
        triggerSpendingStrategy(amount - spendBucket)
    3. spendBucket -= amount
    4. cashFlow.oneTimeExpenses += amount
    5. breakdown.add(ExpenseDetail(expense.name, amount, CASH))
```

### Loan Expense Processing

```
For each month in year:
    if (year >= loan.startYear && year <= loan.endYear):
        1. amount = loan.monthlyPayment
        2. if (spendBucket < amount):
            triggerSpendingStrategy(amount - spendBucket)
        3. spendBucket -= amount
        4. monthlyTotal += amount

At year end:
    1. cashFlow.oneTimeExpenses += monthlyTotal
    2. breakdown.add(ExpenseDetail(loan.name, monthlyTotal, LOAN_PAYMENT))
```

---

## Relationships

```
SimulationConfig
    ├── oneTimeExpenses: List<OneTimeExpense>
    │   ├── CashExpense
    │   │   └── yearOrAge: YearOrAge
    │   └── LoanExpense
    │       └── startYearOrAge: YearOrAge
    │
    └── (existing fields)

YearlyResult
    ├── cashFlow: CashFlow
    │   └── oneTimeExpenses: Double
    │
    └── oneTimeExpensesBreakdown: List<ExpenseDetail>?
        └── ExpenseDetail
            ├── name: String
            ├── amount: Double
            └── type: ExpenseType
```

---

## Serialization Examples

### Cash Expense JSON (Kotlin ↔ TypeScript)

```json
{
  "type": "CASH",
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "New Car",
  "amount": 50000.0,
  "yearOrAge": {
    "type": "AGE",
    "age": 67
  }
}
```

### Loan Expense JSON (Kotlin ↔ TypeScript)

```json
{
  "type": "LOAN",
  "id": "123e4567-e89b-12d3-a456-426614174001",
  "name": "Home Equity Loan",
  "principal": 100000.0,
  "aprPercent": 6.0,
  "termYears": 10,
  "startYearOrAge": {
    "type": "YEAR",
    "year": 2030
  },
  "monthlyPayment": 1110.21
}
```

### YearlyResult with Breakdown JSON

```json
{
  "year": 2030,
  "age": 67,
  "balances": { /* ... */ },
  "cashFlow": {
    // ...other cashFlow fields...
    "oneTimeExpenses": 63322.52
  },
  "metrics": { /* ... */ },
  "oneTimeExpensesBreakdown": [
    {
      "name": "New Car",
      "amount": 50000.0,
      "type": "CASH"
    },
    {
      "name": "Home Equity Loan",
      "amount": 13322.52,
      "type": "LOAN_PAYMENT"
    }
  ]
}
```

---

## Index and Query Patterns

Since expenses are stored as JSON within SimulationConfig, no separate indexing is needed. However, for simulation processing efficiency:

**In-Memory Indexing During Simulation**:
```kotlin
// Build lookup map for fast access
val expensesByYear: Map<Int, List<OneTimeExpense>> = 
    config.oneTimeExpenses.groupBy { expense ->
        when (expense) {
            is CashExpense -> expense.yearOrAge.toYear(config.currentAge, config.currentYear)
            is LoanExpense -> expense.startYearOrAge.toYear(config.currentAge, config.currentYear)
        }
    }
```

**Performance**: O(n) to build map, O(1) lookup per year, where n = number of expenses (typically <10)

---

## Summary

This data model:
- ✅ Supports both cash and loan expenses with type safety
- ✅ Allows flexible timing specification (age or year)
- ✅ Provides structured breakdown data for UI display
- ✅ Integrates cleanly with existing SimulationConfig
- ✅ Maintains backward compatibility
- ✅ Enables efficient serialization between Kotlin and TypeScript
- ✅ Includes comprehensive validation rules
- ✅ Documents state transitions and processing flow

Ready for API contract definition (contracts/ directory).

