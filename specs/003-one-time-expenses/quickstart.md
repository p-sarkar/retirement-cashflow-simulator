# Quick Start: One-Time Expenses Implementation

**Feature**: 003-one-time-expenses  
**Date**: 2026-01-20  
**Status**: Complete

---

## Overview

This guide provides a quick reference for developers implementing the one-time expenses feature. It covers the essential files to modify, key concepts, and implementation checklist.

---

## Key Concepts

### 1. Two Expense Types
- **Cash Expense**: One-time lump sum payment in a specific year
- **Loan Expense**: Fixed-term loan with monthly amortized payments

### 2. Timing Specification
- Expenses can be specified by **calendar year** (e.g., 2030) or **age** (e.g., 67)
- Age converted to year during simulation: `year = currentYear + (age - currentAge)`

### 3. Processing Order
1. Regular income (salary, social security, interest, dividends)
2. Regular expenses (needs, wants, healthcare, taxes)
3. **One-time expenses** ← NEW (processed sequentially, each triggers spending strategy if SB insufficient)

### 4. Breakdown Display
- Years with 0-1 expense: Show total only
- Years with 2+ expenses: Show total + info icon → modal with breakdown

---

## Files to Modify

### Kotlin (API Server)

#### New Files (Create)
```
api-server/src/main/kotlin/com/retirement/
├── model/
│   └── OneTimeExpense.kt          # Data models (sealed interface, Cash, Loan)
├── logic/
│   └── OneTimeExpenseProcessor.kt # Calculation and processing logic
└── validation/
    └── ExpenseValidator.kt        # Validation rules
```

#### Existing Files (Modify)
```
api-server/src/main/kotlin/com/retirement/
├── model/
│   ├── SimulationConfig.kt        # Add oneTimeExpenses: List<OneTimeExpense>
│   ├── CashFlow.kt                # Add oneTimeExpenses: Double
│   └── YearlyResult.kt            # Add oneTimeExpensesBreakdown: List<ExpenseDetail>?
└── logic/
    └── SimulationEngine.kt        # Integrate expense processing
```

### TypeScript (Frontend)

#### New Files (Create)
```
frontend/src/
├── components/
│   ├── ExpenseForm.tsx                # Form for adding/editing expenses
│   ├── ExpenseList.tsx                # List of added expenses
│   └── ExpenseBreakdownDialog.tsx    # Modal for breakdown display
└── utils/
    └── expenseCalculations.ts         # Amortization formula, validation
```

#### Existing Files (Modify)
```
frontend/src/
├── types/
│   └── simulation.ts              # Add OneTimeExpense types
├── components/
│   └── SimulationForm.tsx         # Integrate ExpenseForm
└── pages/
    └── ResultsPage.tsx            # Add one-time expenses column + breakdown
```

### TypeScript (Deno Backend)

#### Existing Files (Modify)
```
backend/src/
├── types.ts                       # Add OneTimeExpense types (mirror frontend)
└── routes/
    └── simulate.ts                # Pass through oneTimeExpenses (no processing)
```

---

## Implementation Checklist

### Phase 1: Data Models (Kotlin)

- [ ] Create `OneTimeExpense.kt` with sealed interface
- [ ] Define `CashExpense` data class
- [ ] Define `LoanExpense` data class
- [ ] Define `YearOrAge` sealed interface
- [ ] Define `ExpenseDetail` data class
- [ ] Add `oneTimeExpenses` to `SimulationConfig`
- [ ] Add `oneTimeExpenses` to `CashFlow`
- [ ] Add `oneTimeExpensesBreakdown` to `YearlyResult`
- [ ] Write unit tests for data model serialization

### Phase 2: Calculation Logic (Kotlin)

- [ ] Create `expenseCalculations.kt` utility
- [ ] Implement amortization formula (APR > 0)
- [ ] Implement simple division formula (APR = 0)
- [ ] Implement age-to-year conversion
- [ ] Implement year-to-age conversion
- [ ] Write unit tests for calculations (compare to external calculators)

### Phase 3: Expense Processing (Kotlin)

- [ ] Create `OneTimeExpenseProcessor` class
- [ ] Implement cash expense processing
- [ ] Implement loan payment processing
- [ ] Integrate with spending strategy (trigger if SB insufficient)
- [ ] Build expense breakdown data
- [ ] Modify `SimulationEngine` to call processor
- [ ] Write integration tests for processing

### Phase 4: Validation (Kotlin + Frontend)

- [ ] Create `ExpenseValidator` (Kotlin)
- [ ] Implement cash expense validation rules
- [ ] Implement loan expense validation rules
- [ ] Add validation to API request handler
- [ ] Mirror validation in frontend TypeScript
- [ ] Write unit tests for validation

### Phase 5: Frontend - Types & Utils

- [ ] Add `OneTimeExpense` types to `simulation.ts`
- [ ] Add `ExpenseDetail` type
- [ ] Create `expenseCalculations.ts` utility
- [ ] Implement frontend amortization calculator
- [ ] Implement frontend validation functions
- [ ] Write unit tests for utilities

### Phase 6: Frontend - Expense Form

- [ ] Create `ExpenseForm.tsx` component
- [ ] Add expense type selector (Cash/Loan)
- [ ] Add name input field
- [ ] Add amount/principal input
- [ ] Add year/age selector
- [ ] Add loan-specific fields (APR, term)
- [ ] Display calculated monthly payment (loans)
- [ ] Add validation error display
- [ ] Write component tests

### Phase 7: Frontend - Expense List

- [ ] Create `ExpenseList.tsx` component
- [ ] Display list of added expenses
- [ ] Add edit button per expense
- [ ] Add remove button per expense
- [ ] Handle empty state
- [ ] Write component tests

### Phase 8: Frontend - Integration

- [ ] Integrate `ExpenseForm` into `SimulationForm`
- [ ] Manage expense list state (array)
- [ ] Pass expenses to simulation API
- [ ] Write integration tests

### Phase 9: Frontend - Results Display

- [ ] Add "One-Time Expenses" column to results table
- [ ] Display total for each year
- [ ] Add info icon for years with 2+ expenses
- [ ] Create `ExpenseBreakdownDialog` component
- [ ] Wire up dialog open/close
- [ ] Display breakdown list in dialog
- [ ] Write component tests

### Phase 10: Backend (Deno)

- [ ] Add `OneTimeExpense` types to `types.ts`
- [ ] Update request validation (if any)
- [ ] Pass through `oneTimeExpenses` to Kotlin API
- [ ] Write integration tests

---

## Key Implementation Details

### Amortization Formula (Kotlin)

```kotlin
fun calculateMonthlyPayment(principal: Double, aprPercent: Double, termYears: Int): Double {
    val n = termYears * 12
    
    if (aprPercent == 0.0) {
        return principal / n
    }
    
    val r = aprPercent / 100.0 / 12.0
    val onePlusR = 1.0 + r
    val onePlusRPowN = onePlusR.pow(n)
    
    return (principal * r * onePlusRPowN) / (onePlusRPowN - 1.0)
}
```

### Processing in Simulation Engine (Kotlin)

```kotlin
// In simulation loop, after regular expenses
val expensesThisYear = getOneTimeExpensesForYear(year, currentAge, currentYear)

expensesThisYear.forEach { expense ->
    val amount = when (expense) {
        is CashExpense -> {
            if (isJanuary) expense.amount else 0.0
        }
        is LoanExpense -> {
            expense.monthlyPayment  // Paid monthly
        }
    }
    
    if (spendBucket < amount) {
        triggerSpendingStrategy(amount - spendBucket)
    }
    
    spendBucket -= amount
    totalOneTimeExpenses += amount
    breakdown.add(ExpenseDetail(expense.name, amount, /* type */))
}
```

### React Form State (Frontend)

```typescript
const [expenses, setExpenses] = useState<OneTimeExpense[]>([]);

const handleAddExpense = (expense: OneTimeExpense) => {
  setExpenses([...expenses, { ...expense, id: uuidv4() }]);
};

const handleRemoveExpense = (id: string) => {
  setExpenses(expenses.filter(e => e.id !== id));
};

const handleEditExpense = (id: string, updated: OneTimeExpense) => {
  setExpenses(expenses.map(e => e.id === id ? updated : e));
};
```

### Breakdown Dialog (Frontend)

```typescript
<Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
  <DialogTitle>One-Time Expenses for Year {year}</DialogTitle>
  <DialogContent>
    <List>
      {breakdown.map((expense, index) => (
        <ListItem key={index}>
          <ListItemText
            primary={expense.name}
            secondary={`$${expense.amount.toLocaleString()} (${expense.type})`}
          />
        </ListItem>
      ))}
    </List>
  </DialogContent>
  <DialogActions>
    <Button onClick={onClose}>Close</Button>
  </DialogActions>
</Dialog>
```

---

## Testing Strategy

### Unit Tests

**Kotlin**:
- Amortization formula accuracy (within $0.01 of external calculators)
- Age-to-year conversion edge cases
- Validation rules for all fields
- Serialization round-trip (JSON → object → JSON)

**TypeScript**:
- Component rendering (Vitest + React Testing Library)
- Validation functions
- Frontend calculation accuracy
- State management

### Integration Tests

**Kotlin**:
- Full simulation with one-time expenses
- Multiple expenses in same year
- Spending strategy triggered by expenses
- Breakdown generation

**Deno**:
- Request pass-through
- Error handling

### End-to-End

- User adds cash expense, runs simulation, sees impact
- User adds loan, sees monthly payment calculation
- User sees breakdown for concurrent expenses
- User edits/removes expenses

---

## Common Pitfalls & Solutions

### Pitfall 1: Forgetting to Process Expenses After Regular Expenses

**Problem**: One-time expenses processed before regular expenses  
**Solution**: Ensure processing order in simulation engine: income → regular expenses → **one-time expenses**

### Pitfall 2: Not Triggering Spending Strategy for Each Expense

**Problem**: Only checking SB balance once for all expenses  
**Solution**: Loop through expenses sequentially, trigger strategy for each if needed

### Pitfall 3: Showing Breakdown for Single Expense

**Problem**: Info icon appears even when only one expense  
**Solution**: Only populate `oneTimeExpensesBreakdown` if `expenses.length > 1`

### Pitfall 4: Incorrect Loan End Year Calculation

**Problem**: Off-by-one error in end year  
**Solution**: `endYear = startYear + termYears - 1` (e.g., 10-year loan starting 2030 ends 2039)

### Pitfall 5: Not Handling 0% APR

**Problem**: Division by zero or NaN in amortization formula  
**Solution**: Special case: if `aprPercent == 0.0`, use `monthlyPayment = principal / (termYears * 12)`

---

## Performance Considerations

### Expected Load
- Typical usage: <10 expenses per simulation
- Simulation: 35 years = 420 months
- Worst case: 10 expenses × 420 months = 4,200 checks

### Optimization
- **Build lookup map**: Group expenses by year for O(1) access
- **Cache calculations**: Store monthly payment in `LoanExpense` (don't recalculate each month)
- **Lazy evaluation**: Only build breakdown if multiple expenses

### Code Example
```kotlin
// Build once at simulation start
val expensesByYear: Map<Int, List<OneTimeExpense>> = 
    config.oneTimeExpenses.groupBy { expense ->
        expense.getYear(currentAge, currentYear)
    }

// O(1) lookup per year
fun getExpensesForYear(year: Int): List<OneTimeExpense> {
    return expensesByYear[year] ?: emptyList()
}
```

---

## Debugging Tips

### Backend (Kotlin)

**Enable detailed logging**:
```kotlin
logger.debug("Processing ${expense.name}: amount=$amount, SB balance=$spendBucket")
```

**Verify serialization**:
```kotlin
println(Json.encodeToString(expense))  // Should match frontend JSON
```

**Check calculation accuracy**:
- Use external calculator (e.g., bankrate.com)
- Compare monthly payment values
- Should be within $0.01

### Frontend

**Log state changes**:
```typescript
useEffect(() => {
  console.log('Expenses updated:', expenses);
}, [expenses]);
```

**Verify API payload**:
```typescript
console.log('Sending config:', JSON.stringify(config, null, 2));
```

**Test validation**:
```typescript
const errors = validateExpense(expense);
console.log('Validation errors:', errors);
```

---

## Resources

### External References
- [Loan amortization formula](https://en.wikipedia.org/wiki/Amortization_calculator)
- [Material-UI Dialog component](https://mui.com/material-ui/react-dialog/)
- [Kotlin sealed interfaces](https://kotlinlang.org/docs/sealed-classes.html)
- [TypeScript discriminated unions](https://www.typescriptlang.org/docs/handbook/2/narrowing.html#discriminated-unions)

### Internal References
- [Feature spec](../spec.md)
- [Data model](../data-model.md)
- [API contract](../contracts/api-contract.md)
- [Research decisions](../research.md)

### Testing Tools
- Kotlin: JUnit 5, MockK
- TypeScript: Vitest, React Testing Library
- Integration: `deno test`
- External calculator: https://www.calculator.net/loan-calculator.html

---

## Quick Reference Commands

### Run Tests

```bash
# Kotlin API server
cd api-server
./gradlew test

# Frontend
cd frontend
npm test

# Deno backend
cd backend
deno test
```

### Build

```bash
# Kotlin API server
cd api-server
./gradlew build

# Frontend
cd frontend
npm run build
```

### Run Dev Servers

```bash
# All services (from repo root)
./start-all.sh

# Or individually:
# Kotlin API: cd api-server && ./gradlew run
# Deno backend: cd backend && deno run --allow-all src/main.ts
# Frontend: cd frontend && npm run dev
```

---

## Next Steps

After reviewing this quick start:

1. Read [research.md](../research.md) for detailed design decisions
2. Review [data-model.md](../data-model.md) for complete data structures
3. Check [api-contract.md](../contracts/api-contract.md) for API details
4. Start implementation with Phase 1 (Data Models)
5. Follow Test-First Development (write tests before implementation)
6. Use the checklist to track progress

---

## Summary

This quick start covers:
- ✅ Key concepts and architecture
- ✅ Files to create and modify
- ✅ Implementation checklist (10 phases)
- ✅ Code snippets for critical sections
- ✅ Testing strategy
- ✅ Common pitfalls and solutions
- ✅ Performance considerations
- ✅ Debugging tips

Ready to begin implementation following Test-First Development principles.

