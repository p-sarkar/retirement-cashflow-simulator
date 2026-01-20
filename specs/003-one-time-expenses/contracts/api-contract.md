# API Contract: One-Time Expenses

**Feature**: 003-one-time-expenses  
**Date**: 2026-01-20  
**Status**: Complete  
**Version**: 1.0

---

## Overview

This document defines the API contracts between the frontend, Deno backend (BFF), and Kotlin API server for the one-time expenses feature. The contracts ensure type-safe communication and consistent data structures across all tiers.

---

## Architecture Flow

```
Frontend (React/TypeScript)
    ↓ HTTP/JSON
Deno Backend (BFF)
    ↓ HTTP/JSON
Kotlin API Server
    ↓ SQLite
Database
```

**Note**: The one-time expenses feature does NOT introduce new API endpoints. Instead, it extends the existing `/api/simulate` endpoint by adding the `oneTimeExpenses` field to `SimulationConfig`.

---

## Existing Endpoint Extension

### POST /api/simulate

**Purpose**: Run retirement cash flow simulation (interactive or Monte Carlo)

**Existing Behavior**: Accepts `SimulationConfig`, returns `SimulationResult` or `MonteCarloResult`

**Change**: Add `oneTimeExpenses` field to request body

#### Request

**HTTP Method**: POST  
**Path**: `/api/simulate`  
**Content-Type**: `application/json`

**Request Body** (SimulationConfig):
```json
{
  "id": "optional-uuid",
  "name": "My Retirement Plan",
  "currentYear": 2026,
  "currentAge": 60,
  "retirementAge": 65,
  "salary": 150000,
  "portfolio": {
    "sb": 50000,
    "cbb": 100000,
    "tba": 200000,
    "tda": 500000,
    "tfa": 100000
  },
  "spousal": { /* ... */ },
  "expenses": { /* ... */ },
  "contributions": { /* ... */ },
  "rates": { /* ... */ },
  "strategy": { /* ... */ },
  
  // NEW FIELD
  "oneTimeExpenses": [
    {
      "type": "CASH",
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "New Car",
      "amount": 50000,
      "yearOrAge": {
        "type": "AGE",
        "age": 67
      }
    },
    {
      "type": "LOAN",
      "id": "123e4567-e89b-12d3-a456-426614174001",
      "name": "Home Equity Loan",
      "principal": 100000,
      "aprPercent": 6.0,
      "termYears": 10,
      "startYearOrAge": {
        "type": "YEAR",
        "year": 2030
      },
      "monthlyPayment": 1110.21
    }
  ]
}
```

**Request Schema**:
```typescript
interface SimulationRequest {
  config: SimulationConfig;
  type?: 'interactive' | 'montecarlo';  // Existing field
  runs?: number;  // Existing field (for Monte Carlo)
}

interface SimulationConfig {
  // ...existing fields...
  oneTimeExpenses: OneTimeExpense[];  // NEW
}

type OneTimeExpense = CashExpense | LoanExpense;

interface CashExpense {
  type: 'CASH';
  id: string;
  name: string;
  amount: number;
  yearOrAge: YearOrAge;
}

interface LoanExpense {
  type: 'LOAN';
  id: string;
  name: string;
  principal: number;
  aprPercent: number;
  termYears: number;
  startYearOrAge: YearOrAge;
  monthlyPayment: number;
}

type YearOrAge = 
  | { type: 'YEAR'; year: number }
  | { type: 'AGE'; age: number };
```

#### Response

**HTTP Status**: 200 OK  
**Content-Type**: `application/json`

**Response Body** (SimulationResult):
```json
{
  "config": { /* echoes back request config */ },
  "yearlyResults": [
    {
      "year": 2026,
      "age": 60,
      "balances": { /* ... */ },
      "cashFlow": {
        // ...existing fields...
        "oneTimeExpenses": 0  // NEW: total one-time expenses this year
      },
      "metrics": { /* ... */ }
      // oneTimeExpensesBreakdown omitted (0 or 1 expense)
    },
    {
      "year": 2030,
      "age": 67,
      "balances": { /* ... */ },
      "cashFlow": {
        // ...existing fields...
        "oneTimeExpenses": 63322.52  // NEW: car + loan payment
      },
      "metrics": { /* ... */ },
      
      // NEW: breakdown for years with 2+ expenses
      "oneTimeExpensesBreakdown": [
        {
          "name": "New Car",
          "amount": 50000,
          "type": "CASH"
        },
        {
          "name": "Home Equity Loan",
          "amount": 13322.52,
          "type": "LOAN_PAYMENT"
        }
      ]
    }
  ],
  "quarterlyResults": [ /* ... */ ],
  "summary": {
    "finalTotalBalance": 1250000,
    "isSuccess": true,
    "failureYear": null,
    "totalDividends": 85000,
    "totalInterest": 12000
  },
  "apiMetadata": { /* ... */ }
}
```

**Response Schema**:
```typescript
interface SimulationResult {
  config: SimulationConfig;
  yearlyResults: YearlyResult[];
  quarterlyResults: QuarterlyResult[];
  summary: Summary;
  apiMetadata: ApiMetadata;
}

interface YearlyResult {
  year: number;
  age: number;
  balances: Portfolio;
  cashFlow: CashFlow;
  metrics: Metrics;
  oneTimeExpensesBreakdown?: ExpenseDetail[];  // NEW: optional
}

interface CashFlow {
  // ...existing fields...
  oneTimeExpenses: number;  // NEW
}

interface ExpenseDetail {
  name: string;
  amount: number;
  type: 'CASH' | 'LOAN_PAYMENT';
}
```

#### Error Responses

**422 Unprocessable Entity** (Validation Errors):
```json
{
  "error": "Validation failed",
  "statusCode": 422,
  "validationErrors": [
    {
      "field": "oneTimeExpenses[0].amount",
      "message": "Amount must be greater than zero"
    },
    {
      "field": "oneTimeExpenses[1].aprPercent",
      "message": "APR cannot be negative"
    }
  ]
}
```

**400 Bad Request** (Malformed JSON):
```json
{
  "error": "Invalid request format",
  "statusCode": 400
}
```

**500 Internal Server Error** (Calculation Error):
```json
{
  "error": "Simulation calculation failed",
  "statusCode": 500
}
```

---

## Data Type Contracts

### OneTimeExpense Types

#### CashExpense

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `type` | String | Yes | Must be "CASH" | Discriminator for expense type |
| `id` | String | Yes | UUID format | Unique identifier |
| `name` | String | Yes | 1-100 chars | Expense description |
| `amount` | Number | Yes | > 0, < 1e12 | Dollar amount |
| `yearOrAge` | YearOrAge | Yes | See below | When expense occurs |

**Kotlin**:
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

**TypeScript**:
```typescript
export interface CashExpense extends BaseExpense {
  type: 'CASH';
  amount: number;
  yearOrAge: YearOrAge;
}
```

#### LoanExpense

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `type` | String | Yes | Must be "LOAN" | Discriminator for expense type |
| `id` | String | Yes | UUID format | Unique identifier |
| `name` | String | Yes | 1-100 chars | Loan description |
| `principal` | Number | Yes | > 0, < 1e12 | Loan amount |
| `aprPercent` | Number | Yes | >= 0, <= 99.99 | Annual percentage rate |
| `termYears` | Number | Yes | > 0, <= 50 | Loan duration |
| `startYearOrAge` | YearOrAge | Yes | See below | When loan starts |
| `monthlyPayment` | Number | Yes | Calculated | Monthly payment |

**Kotlin**:
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
) : OneTimeExpense
```

**TypeScript**:
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

#### YearOrAge

Discriminated union for timing specification.

**Year Variant**:
```typescript
{
  type: 'YEAR',
  year: number  // Calendar year (e.g., 2030)
}
```

**Age Variant**:
```typescript
{
  type: 'AGE',
  age: number  // Age in years (e.g., 67)
}
```

**Kotlin**:
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

**TypeScript**:
```typescript
export type YearOrAge = 
  | { type: 'YEAR'; year: number }
  | { type: 'AGE'; age: number };
```

**Validation**:
- Year: `year >= currentYear` and `year <= currentYear + 35`
- Age: `age >= currentAge` and corresponding year <= `currentYear + 35`

### ExpenseDetail

Used in breakdown display.

| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Expense name |
| `amount` | Number | Dollar amount for this year |
| `type` | String | "CASH" or "LOAN_PAYMENT" |

**Kotlin**:
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

**TypeScript**:
```typescript
export interface ExpenseDetail {
  name: string;
  amount: number;
  type: 'CASH' | 'LOAN_PAYMENT';
}
```

---

## Validation Rules Contract

Both frontend and backend MUST enforce these validation rules.

### Cash Expense Validation

| Field | Rule | Error Message |
|-------|------|---------------|
| `name` | Required, 1-100 chars | "Expense name is required" / "Name too long (max 100 chars)" |
| `amount` | > 0, < 1e12 | "Amount must be positive" / "Amount too large" |
| `yearOrAge.year` | >= currentYear, <= currentYear+35 | "Year must be within simulation period" |
| `yearOrAge.age` | >= currentAge, corresponding year <= currentYear+35 | "Age must be within simulation period" |

### Loan Expense Validation

| Field | Rule | Error Message |
|-------|------|---------------|
| `name` | Required, 1-100 chars | "Loan name is required" / "Name too long (max 100 chars)" |
| `principal` | > 0, < 1e12 | "Principal must be positive" / "Principal too large" |
| `aprPercent` | >= 0, <= 99.99 | "APR cannot be negative" / "APR too high (max 99.99%)" |
| `termYears` | > 0, <= 50 | "Term must be positive" / "Term too long (max 50 years)" |
| `startYearOrAge` | Same as YearOrAge rules above | See above |
| `endYear` | >= startYear, <= currentYear+35 | "Loan extends beyond simulation period" |
| `monthlyPayment` | Automatically calculated, must be > 0 | N/A (calculated field) |

---

## Serialization Examples

### Request Example: Mixed Expenses

```json
{
  "config": {
    "name": "Retirement with Car and Loan",
    "currentYear": 2026,
    "currentAge": 60,
    "retirementAge": 65,
    // ...other config fields...
    "oneTimeExpenses": [
      {
        "type": "CASH",
        "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "name": "New Car Purchase",
        "amount": 50000,
        "yearOrAge": {
          "type": "AGE",
          "age": 67
        }
      },
      {
        "type": "LOAN",
        "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
        "name": "Home Renovation Loan",
        "principal": 75000,
        "aprPercent": 5.5,
        "termYears": 5,
        "startYearOrAge": {
          "type": "YEAR",
          "year": 2028
        },
        "monthlyPayment": 1433.99
      },
      {
        "type": "CASH",
        "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
        "name": "International Vacation",
        "amount": 15000,
        "yearOrAge": {
          "type": "AGE",
          "age": 70
        }
      }
    ]
  },
  "type": "interactive"
}
```

### Response Example: Year with Multiple Expenses

```json
{
  "year": 2033,
  "age": 67,
  "balances": {
    "sb": 45000,
    "cbb": 98000,
    "tba": 220000,
    "tda": 480000,
    "tfa": 115000
  },
  "cashFlow": {
    "salary": 0,
    "interest": 2250,
    "dividends": 8800,
    "socialSecurity": 35000,
    "tbaWithdrawal": 0,
    "tdaWithdrawal": 25000,
    "tdaWithdrawalSpend": 25000,
    "tdaWithdrawalRoth": 0,
    "sbDeposit": 71050,
    "sbWithdrawal": 0,
    "rothConversion": 0,
    "contribution401k": 0,
    "contributionRoth401k": 0,
    "contributionTba": 0,
    "totalIncome": 71050,
    "needs": 40000,
    "wants": 20000,
    "healthcare": 8000,
    "incomeTax": 5000,
    "propertyTax": 6000,
    "oneTimeExpenses": 67207.88,
    "totalExpenses": 146207.88
  },
  "metrics": {
    "annualIncomeGap": 75157.88,
    "incomeGapExpenses": 146207.88,
    "incomeGapPassiveIncome": 71050,
    "sbCap": 95280,
    "cbbCap": 146207.88,
    "isFailure": false
  },
  "oneTimeExpensesBreakdown": [
    {
      "name": "New Car Purchase",
      "amount": 50000,
      "type": "CASH"
    },
    {
      "name": "Home Renovation Loan",
      "amount": 17207.88,
      "type": "LOAN_PAYMENT"
    }
  ]
}
```

### Error Response Example: Validation Failure

```json
{
  "error": "Validation failed",
  "statusCode": 422,
  "validationErrors": [
    {
      "field": "oneTimeExpenses[1].aprPercent",
      "message": "APR cannot be negative"
    },
    {
      "field": "oneTimeExpenses[2].yearOrAge.year",
      "message": "Year must be within simulation period (2026-2061)"
    }
  ]
}
```

---

## Frontend-Backend Contract (Deno BFF)

The Deno backend acts as a proxy and orchestration layer. For one-time expenses:

### Request Flow

1. **Frontend → Deno BFF**: POST `/api/simulate` with `SimulationConfig` including `oneTimeExpenses`
2. **Deno BFF validates**:
   - Request format
   - Required fields present
   - Types match expected schema
3. **Deno BFF → Kotlin API**: Forward request to Kotlin API server
4. **Kotlin API**: Perform simulation including one-time expenses
5. **Kotlin API → Deno BFF**: Return `SimulationResult`
6. **Deno BFF → Frontend**: Forward response

### Error Handling

Deno BFF should:
- Return 400 for malformed JSON
- Return 422 for validation errors (with details from Kotlin API)
- Return 500 for server errors
- Return 503 if Kotlin API is unavailable

---

## Backward Compatibility

### Request Compatibility

Old clients that don't send `oneTimeExpenses`:
- ✅ Still work (field defaults to empty list)
- ✅ Simulation runs without one-time expenses
- ✅ Response has `cashFlow.oneTimeExpenses = 0` for all years

### Response Compatibility

Old clients receiving new responses:
- ✅ Can ignore `cashFlow.oneTimeExpenses` field
- ✅ Can ignore `oneTimeExpensesBreakdown` field
- ✅ All other fields unchanged

New clients receiving old responses (from cached or old server):
- ✅ TypeScript optional fields handle missing data
- ✅ Default values prevent errors

---

## Testing Contracts

### Contract Test Cases

1. **Round-trip serialization**: JSON → Kotlin → JSON matches
2. **Type discrimination**: `type` field correctly deserializes to CashExpense or LoanExpense
3. **Validation enforcement**: Both frontend and backend reject invalid data with same error messages
4. **Null/undefined handling**: Optional fields work correctly
5. **Backward compatibility**: Old requests still work, new fields optional

### Sample Test Data

**Valid Cash Expense**:
```json
{
  "type": "CASH",
  "id": "test-id-1",
  "name": "Test Expense",
  "amount": 10000,
  "yearOrAge": { "type": "AGE", "age": 65 }
}
```

**Valid Loan Expense**:
```json
{
  "type": "LOAN",
  "id": "test-id-2",
  "name": "Test Loan",
  "principal": 50000,
  "aprPercent": 5.0,
  "termYears": 10,
  "startYearOrAge": { "type": "YEAR", "year": 2030 },
  "monthlyPayment": 530.33
}
```

**Invalid Expense** (should fail validation):
```json
{
  "type": "CASH",
  "id": "test-id-3",
  "name": "",
  "amount": -1000,
  "yearOrAge": { "type": "AGE", "age": 50 }
}
```

---

## Summary

This contract:
- ✅ Extends existing `/api/simulate` endpoint without breaking changes
- ✅ Defines type-safe data structures for Kotlin and TypeScript
- ✅ Specifies validation rules enforced by both frontend and backend
- ✅ Provides concrete serialization examples
- ✅ Ensures backward compatibility
- ✅ Documents error responses
- ✅ Covers all expense types and edge cases

No new endpoints needed. All communication flows through existing simulation infrastructure.

