# Quarterly Processing Update - One-Time Expenses Specification

**Date**: 2026-01-21  
**Updated By**: GitHub Copilot  
**Feature**: 003-one-time-expenses

## Overview

Updated the feature specification to reflect quarterly processing instead of monthly processing, aligning with the overall system architecture where all income, expenses, and refilling strategies are executed quarterly.

## Key Changes Made

### 1. Clarifications Section
- Updated loan start timing clarification from "January" to "Q1"
- Changed payment calculation from "monthly payment" to "quarterly payment"
- Updated pro-rating description from "remaining months" to "remaining quarters"

### 2. User Stories & Acceptance Scenarios

#### US1 - Single Cash Expense
- Changed: "Spend Bucket balance decreases by $50,000 in **January**" → "in **Q1**"

#### US3 - Loan with Amortization
- Changed user story description: "understanding both the **monthly** payment burden" → "understanding both the **quarterly** payment burden"
- Changed independent test: "see **monthly** payments automatically calculated" → "see **quarterly** payments automatically calculated"
- Changed: "the **monthly** payment is automatically calculated" → "the **quarterly** payment is automatically calculated"
- Changed: "shows **monthly** payments from age 65 through 74" → "shows **quarterly** payments from age 65 through 74"
- Updated amortization formula description to specify **quarterly rate (APR/4)** instead of monthly rate
- Changed: "annual total reflects **12 monthly** payments" → "**4 quarterly** payments"

#### US4 - Edit and Remove Expenses
- Changed: "the **monthly** payment is recalculated" → "the **quarterly** payment is recalculated"

### 3. Functional Requirements

#### Cash Expense Requirements
- **FR-010**: Changed from "**January**" to "**Q1**"

#### Loan Expense Requirements
- **FR-016**: Updated to "calculate **quarterly** payment amount using standard amortization formula adapted for **quarterly compounding**: M = P[r(1+r)^n]/[(1+r)^n-1], where P=principal, r=**quarterly rate (APR/4)**, n=**total quarters**"
- **FR-016a**: Changed "**monthly** payment" to "**quarterly** payment"
- **FR-017**: Changed "**monthly** payment" to "**quarterly** payment" and "**months**" to "**quarters**"
- **FR-018**: Changed "calculated **monthly** payment" to "calculated **quarterly** payment"
- **FR-019**: Changed "deducted from the Spend Bucket **monthly**" to "**quarterly**"
- **FR-020a**: Changed "**January** of the start year" to "**Q1** of the start year" and "**monthly** loan payments" to "**quarterly** loan payments"

#### Calculation & Simulation
- **FR-023**: Changed "**January**" to "**Q1**"
- **FR-024**: Changed "**monthly** during the loan term starting in **January**" to "**quarterly** during the loan term starting in **Q1**" and "**monthly** payment amount" to "**quarterly** payment amount"

#### Results Display
- **FR-034**: Changed "annual payment total (**12 monthly** payments)" to "(**4 quarterly** payments)"

### 4. Key Entities

#### CashExpense
- Changed: "Paid entirely in **January** of specified year" → "in **Q1** of specified year"

#### LoanExpense
- Changed properties: "calculated **monthly** payment" → "calculated **quarterly** payment"
- Changed derived properties: "annual payment total (**monthly** payment × **12**)" → "(**quarterly** payment × **4**)"
- Changed behavior: "**Monthly** payments deducted" → "**Quarterly** payments deducted"

### 5. Success Criteria
- **SC-002**: Changed "calculated **monthly** payment displayed" → "calculated **quarterly** payment displayed"

### 6. Assumptions
- Changed: "**January** (year-level granularity only); '**January**' payment" → "**Q1** (year-level granularity only); '**Q1**' payment"
- Changed: "recurring **monthly** withdrawals" → "recurring **quarterly** withdrawals"
- Changed: "Loan interest is compounded **monthly** (standard for most consumer loans)" → "Loan interest is compounded **quarterly** (consistent with quarterly payment structure)"
- Changed: "Loan **monthly** payment amount stays constant (**12 monthly** payments per calendar year)" → "Loan **quarterly** payment amount stays constant (**4 quarterly** payments per calendar year)"

### 7. Edge Cases
- Updated 0% APR loan handling: "**monthly** payment = principal / number of **months**" → "**quarterly** payment = principal / number of **quarters**"
- Updated validation: "0 or negative **months**" → "0 or negative **quarters**"

### 8. Out of Scope
- **Payment timing flexibility**: Changed "Cash expenses always occur in **January**; loan payments always **monthly**. No **quarterly**, bi-weekly, or custom payment schedules" → "Cash expenses always occur in **Q1**; loan payments always **quarterly**. No **monthly**, bi-weekly, or custom payment schedules"
- **Principal paydown tracking**: Changed "only **monthly** payment amount is shown" → "only **quarterly** payment amount is shown"

## Important Notes

### Quarterly Compounding
The loan amortization formula now uses quarterly compounding:
- **Quarterly rate**: APR/4 (not APR/12)
- **Total periods**: term (years) × 4 quarters/year (not × 12 months/year)
- **Formula**: M = P[r(1+r)^n]/[(1+r)^n-1] where r = APR/4 and n = term × 4

### Consistency with System Architecture
This change ensures one-time expenses align with the quarterly processing model used throughout the retirement cash flow simulator:
- Income sources (salary, social security) are received quarterly
- Regular expenses (needs, wants, healthcare, property tax) are paid quarterly
- Spending strategy refilling happens quarterly
- Roth conversions happen quarterly
- HYSA interest accrues monthly but is credited quarterly
- Bond dividends accrue monthly but are credited quarterly

## Verification

All monthly/month/January references have been replaced with quarterly/quarter/Q1 equivalents. The specification now accurately reflects the quarterly processing model.

### Search Verification
- ✅ No remaining "monthly", "Monthly", "month", "Month", or "January" references
- ✅ 20+ "quarter/quarterly" references correctly placed
- ✅ 10+ "Q1" references correctly placed
- ✅ No errors in the specification file

