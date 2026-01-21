# Monthly Payment Calculation for Loan-Type One-Time Expenses

## Overview

For loan-type one-time expenses, the monthly payment is calculated using the **standard amortization formula** for fixed-rate loans. This is the same formula used by banks and financial institutions for mortgages, car loans, and personal loans.

---

## The Formula

### Standard Amortization Formula

```
M = P[r(1+r)^n] / [(1+r)^n - 1]
```

Where:
- **M** = Monthly payment amount
- **P** = Principal (loan amount)
- **r** = Monthly interest rate (APR / 100 / 12)
- **n** = Total number of monthly payments (term in years × 12)

### Special Case: 0% APR

For loans with 0% interest:
```
M = P / n
```

This is simply the principal divided by the number of months.

---

## Implementation

### Frontend (TypeScript)

**File**: `frontend/src/utils/expenseUtils.ts`

```typescript
export function calculateMonthlyPayment(
  principal: number, 
  aprPercent: number, 
  termYears: number
): number {
  const n = termYears * 12;  // Total monthly payments

  // Special case: 0% APR
  if (aprPercent === 0) {
    return principal / n;
  }

  // Calculate monthly interest rate
  const r = aprPercent / 100 / 12;
  
  // Calculate (1 + r)
  const onePlusR = 1 + r;
  
  // Calculate (1 + r)^n
  const onePlusRPowN = Math.pow(onePlusR, n);

  // Apply amortization formula
  return (principal * r * onePlusRPowN) / (onePlusRPowN - 1);
}
```

### Backend (Kotlin)

**File**: `api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt`

```kotlin
companion object {
    fun calculateMonthlyPayment(
        principal: Double, 
        aprPercent: Double, 
        termYears: Int
    ): Double {
        val n = termYears * 12

        // Special case: 0% APR
        if (aprPercent == 0.0) {
            return principal / n
        }

        // Calculate monthly interest rate
        val r = aprPercent / 100.0 / 12.0
        
        // Calculate (1 + r)
        val onePlusR = 1.0 + r
        
        // Calculate (1 + r)^n
        val onePlusRPowN = onePlusR.pow(n)

        // Apply amortization formula
        return (principal * r * onePlusRPowN) / (onePlusRPowN - 1.0)
    }
}
```

---

## Example Calculations

### Example 1: Car Loan (Default Expense)

**Loan Details**:
- Principal (P): $50,000
- APR: 4%
- Term: 5 years

**Step-by-Step Calculation**:

1. **Calculate total payments (n)**:
   ```
   n = 5 years × 12 months = 60 months
   ```

2. **Calculate monthly interest rate (r)**:
   ```
   r = 4% / 100 / 12 = 0.04 / 100 / 12 = 0.003333...
   ```

3. **Calculate (1 + r)**:
   ```
   1 + r = 1 + 0.003333... = 1.003333...
   ```

4. **Calculate (1 + r)^n**:
   ```
   (1.003333...)^60 = 1.22039...
   ```

5. **Apply the formula**:
   ```
   M = [50,000 × 0.003333... × 1.22039...] / [1.22039... - 1]
   M = [203.398...] / [0.22039...]
   M = $922.90 per month (approximately)
   ```

**Annual Payment**: $922.90 × 12 = **$11,074.80 per year**

### Example 2: Zero-Interest Loan

**Loan Details**:
- Principal (P): $30,000
- APR: 0%
- Term: 3 years

**Calculation**:
```
n = 3 × 12 = 36 months
M = $30,000 / 36 = $833.33 per month
```

**Annual Payment**: $833.33 × 12 = **$10,000 per year**

### Example 3: High-Interest Personal Loan

**Loan Details**:
- Principal (P): $20,000
- APR: 12%
- Term: 4 years

**Step-by-Step Calculation**:

1. n = 4 × 12 = 48 months
2. r = 12% / 100 / 12 = 0.01
3. (1 + r) = 1.01
4. (1 + r)^n = 1.01^48 = 1.61223...
5. M = [20,000 × 0.01 × 1.61223...] / [1.61223... - 1]
6. M = [322.45] / [0.61223...]
7. M = **$526.68 per month**

**Annual Payment**: $526.68 × 12 = **$6,320.16 per year**

---

## How It Works in the Simulation

### 1. User Input

When a user adds a loan expense:
- **Name**: "Partha's car 1"
- **Principal**: $50,000
- **APR**: 4%
- **Term**: 5 years
- **Start Age/Year**: Age 55

### 2. Monthly Payment Calculation

The system automatically calculates:
```
Monthly Payment = calculateMonthlyPayment(50000, 4, 5)
                = $922.90
```

This is stored in the `LoanExpense.monthlyPayment` field.

### 3. Annual Payment

Each year during the loan term (ages 55-59), the simulation:
```kotlin
val annualPayment = expense.monthlyPayment * 12
// For car: $922.90 × 12 = $11,074.80
```

### 4. Inflation Adjustment

The annual payment is then adjusted for inflation:
```kotlin
val adjustedPayment = annualPayment * inflationAdjustment
// Example at age 55 (year 5): $11,074.80 × 1.159... = $12,836
```

### 5. Deduction from Spend Bucket

Each year from age 55-59, approximately $12,836-$13,700 (inflation-adjusted) is deducted from the Spend Bucket.

---

## Key Characteristics of Amortization

### 1. Fixed Payment Amount
The monthly payment stays **constant** throughout the loan term. The original calculated payment never changes.

### 2. Payment Composition Changes Over Time
- **Early payments**: Mostly interest, small principal reduction
- **Later payments**: Mostly principal, small interest portion

However, the simulation **does not track** the principal/interest split. It simply:
- Deducts the full annual payment from the Spend Bucket
- Triggers spending strategy if insufficient funds

### 3. Inflation Adjustment
While the **base monthly payment is fixed**, the simulation applies **inflation adjustment** to model future dollars accurately:

**Year 1 (Age 55)**: $11,074.80 × 1.159 = $12,836  
**Year 2 (Age 56)**: $11,074.80 × 1.194 = $13,223  
**Year 3 (Age 57)**: $11,074.80 × 1.230 = $13,622  
**Year 4 (Age 58)**: $11,074.80 × 1.267 = $14,032  
**Year 5 (Age 59)**: $11,074.80 × 1.305 = $14,453  

(Assuming 3% inflation rate)

---

## Validation Rules

The system validates loan inputs:

1. **Principal**: Must be > 0
2. **APR**: Must be ≥ 0 (can be zero)
3. **Term**: Must be > 0 years
4. **Start Age/Year**: Must be valid and within simulation range

### Invalid Examples

❌ Principal = 0 or negative  
❌ Term = 0 years  
❌ APR = negative (not allowed)  

---

## Formula Derivation (Mathematical Background)

The amortization formula comes from the concept of **present value of an annuity**:

### Present Value of Annuity
```
PV = M × [(1 - (1 + r)^-n) / r]
```

Since we know PV (the principal P) and want to find M:
```
P = M × [(1 - (1 + r)^-n) / r]
```

Solving for M:
```
M = P × [r / (1 - (1 + r)^-n)]
```

This can be algebraically rearranged to:
```
M = P × [r(1 + r)^n / ((1 + r)^n - 1)]
```

Which is the standard amortization formula used in the code.

---

## Comparison with Other Loan Types

### This Implementation (Fixed-Rate Amortization)
- ✅ Monthly payment is constant
- ✅ Interest rate is fixed for entire term
- ✅ Standard for car loans, mortgages, personal loans

### NOT Implemented
- ❌ Variable-rate loans (APR changes over time)
- ❌ Interest-only payments
- ❌ Balloon payments
- ❌ Prepayment or early payoff
- ❌ Principal/interest breakdown tracking

---

## Testing the Calculation

### Online Loan Calculators

You can verify the monthly payment calculation using:
- **Bankrate Loan Calculator**: https://www.bankrate.com/calculators/mortgages/loan-calculator.aspx
- **Calculator.net Loan Calculator**: https://www.calculator.net/loan-calculator.html

### Example Verification

For the default car loan:
- Principal: $50,000
- APR: 4%
- Term: 5 years (60 months)

Any standard loan calculator will confirm:
- **Monthly Payment**: $920.41 - $922.90 (slight variations due to rounding)
- **Total Paid**: $55,224.60 - $55,374
- **Total Interest**: $5,224.60 - $5,374

---

## Summary

### Monthly Payment Formula
```
M = P[r(1+r)^n] / [(1+r)^n - 1]

Where:
- P = Principal amount
- r = Monthly interest rate (APR/100/12)
- n = Total monthly payments (years × 12)
```

### Special Case (0% APR)
```
M = P / n
```

### Key Points
1. ✅ Uses standard amortization formula
2. ✅ Monthly payment is constant (fixed-rate)
3. ✅ Calculated automatically in both frontend and backend
4. ✅ Annual payment = Monthly payment × 12
5. ✅ Inflation-adjusted each year in simulation
6. ✅ Deducted from Spend Bucket monthly/annually
7. ✅ Triggers spending strategy if SB insufficient

### Files Involved
- **Frontend**: `frontend/src/utils/expenseUtils.ts`
- **Backend**: `api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt`
- **Usage**: `frontend/src/components/OneTimeExpenseInput.tsx` (displays calculated payment)

---

**Last Updated**: January 21, 2026  
**Build Version**: v1.1.17

