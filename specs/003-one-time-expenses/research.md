# Research: One-Time Expenses

**Feature**: 003-one-time-expenses  
**Date**: 2026-01-20  
**Status**: Complete

---

## Research Tasks

### 1. Age-to-Year Conversion Logic

**Decision**: Simple arithmetic conversion with validation for historical dates

**Rationale**: 
- The conversion formula is straightforward: `targetYear = currentYear + (targetAge - currentAge)`
- Reverse conversion: `targetAge = currentAge + (targetYear - currentYear)`
- Historical dates (targetAge < currentAge or targetYear < currentYear) should be rejected with validation errors
- This aligns with the simulation constraint that only looks forward 35 years from current year

**Implementation**:
```kotlin
// Kotlin utility
fun ageToYear(currentAge: Int, currentYear: Int, targetAge: Int): Int {
    require(targetAge >= currentAge) { "Target age cannot be in the past" }
    return currentYear + (targetAge - currentAge)
}

fun yearToAge(currentAge: Int, currentYear: Int, targetYear: Int): Int {
    require(targetYear >= currentYear) { "Target year cannot be in the past" }
    return currentAge + (targetYear - currentYear)
}
```

**Validation Rules**:
- Reject if targetAge < currentAge
- Reject if targetYear < currentYear
- Warn if targetYear > currentYear + 35 (beyond simulation period)

**Alternatives Considered**:
- Using actual calendar dates and Date objects: Rejected because the simulation only needs year granularity
- Allowing historical dates: Rejected because expenses in the past don't affect future retirement planning

---

### 2. Amortization Formula Implementation

**Decision**: Use standard loan amortization formula with special case for 0% APR

**Rationale**:
- Industry-standard formula ensures accurate calculations that match external calculators (SC-007)
- Special case for 0% APR avoids division by zero
- Kotlin's BigDecimal provides precision needed for financial calculations

**Standard Formula** (APR > 0):
```
M = P[r(1+r)^n] / [(1+r)^n - 1]

Where:
- M = Monthly payment
- P = Principal (loan amount)
- r = Monthly interest rate (APR / 12)
- n = Total number of months (term * 12)
```

**Special Case** (APR = 0):
```
M = P / n

Where:
- M = Monthly payment
- P = Principal
- n = Total number of months
```

**Implementation**:
```kotlin
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

fun calculateMonthlyPayment(principal: Double, aprPercent: Double, termYears: Int): Double {
    val p = BigDecimal.valueOf(principal)
    val n = termYears * 12
    
    if (aprPercent == 0.0) {
        // Simple division for 0% APR
        return p.divide(BigDecimal.valueOf(n.toLong()), 2, RoundingMode.HALF_UP).toDouble()
    }
    
    // Convert APR percentage to monthly decimal rate
    val r = BigDecimal.valueOf(aprPercent / 100.0 / 12.0)
    
    // Calculate (1 + r)^n
    val onePlusR = BigDecimal.ONE.add(r)
    val onePlusRPowN = onePlusR.pow(n)
    
    // Calculate M = P[r(1+r)^n] / [(1+r)^n - 1]
    val numerator = p.multiply(r).multiply(onePlusRPowN)
    val denominator = onePlusRPowN.subtract(BigDecimal.ONE)
    
    return numerator.divide(denominator, 2, RoundingMode.HALF_UP).toDouble()
}
```

**Validation**:
- Test against online calculators (e.g., bankrate.com, calculator.net)
- Accuracy requirement: within $0.01 of external calculators
- Unit tests with known values

**Alternatives Considered**:
- Using external library (Apache Commons Math): Rejected for simplicity; formula is straightforward
- JavaScript calculation in frontend: Rejected to maintain business logic in Kotlin API server

---

### 3. Breakdown Data Structure

**Decision**: Use structured list with expense details, not a simple map

**Rationale**:
- Supports duplicate expense names
- Allows future extension (e.g., adding expense type, ID)
- Type-safe serialization between Kotlin and TypeScript
- Clear structure for rendering in modal dialog

**Data Model**:
```kotlin
@Serializable
data class ExpenseDetail(
    val name: String,
    val amount: Double,
    val type: ExpenseType  // "CASH" or "LOAN_PAYMENT"
)

@Serializable
data class OneTimeExpensesBreakdown(
    val year: Int,
    val totalAmount: Double,
    val expenses: List<ExpenseDetail>
)
```

**TypeScript Interface**:
```typescript
export interface ExpenseDetail {
  name: string;
  amount: number;
  type: 'CASH' | 'LOAN_PAYMENT';
}

export interface OneTimeExpensesBreakdown {
  year: number;
  totalAmount: number;
  expenses: ExpenseDetail[];
}
```

**Usage in YearlyResult**:
- Add `oneTimeExpensesTotal: Double` to CashFlow
- Add `oneTimeExpensesBreakdown: List<ExpenseDetail>?` to YearlyResult (nullable, only populated if multiple expenses)

**Alternatives Considered**:
- Map<String, Double>: Rejected because it doesn't handle duplicate names
- Single string with concatenated details: Rejected for poor type safety and client-side parsing complexity

---

### 4. Error Handling Strategy

**Decision**: Multi-layered validation with clear user feedback

**Rationale**:
- Frontend validation provides immediate feedback
- Backend validation ensures data integrity
- Error messages use Material-UI Alert and Snackbar components
- HTTP status codes follow REST conventions

**Error Response Format**:
```typescript
export interface ValidationError {
  field: string;
  message: string;
}

export interface ApiError {
  error: string;
  validationErrors?: ValidationError[];
  statusCode: number;
}
```

**HTTP Status Codes**:
- 400 Bad Request: Invalid request format
- 422 Unprocessable Entity: Validation failures
- 500 Internal Server Error: Calculation or system errors

**Frontend Error Display**:
- Inline field errors: Use Material-UI TextField `error` and `helperText` props
- Form-level errors: Alert component above form
- Network errors: Snackbar with error message
- Modal for detailed validation errors if multiple fields affected

**Implementation Pattern**:
```typescript
// Frontend validation
const validateExpense = (expense: OneTimeExpense): ValidationError[] => {
  const errors: ValidationError[] = [];
  
  if (!expense.name || expense.name.trim() === '') {
    errors.push({ field: 'name', message: 'Expense name is required' });
  }
  
  if (expense.type === 'CASH') {
    if (expense.amount <= 0) {
      errors.push({ field: 'amount', message: 'Amount must be positive' });
    }
  }
  
  return errors;
};
```

**Alternatives Considered**:
- Generic error messages: Rejected for poor UX
- Only backend validation: Rejected because frontend validation improves responsiveness
- Custom error modal: Rejected in favor of Material-UI standard components

---

### 5. Persistence Strategy

**Decision**: Store expense list as part of SimulationConfig in SQLite database

**Rationale**:
- Expenses are configuration data, not transactional data
- Storing with SimulationConfig ensures atomicity
- Simplifies data model (no separate expenses table)
- Aligns with existing architecture where SimulationConfig is saved/loaded

**Data Model Changes**:
```kotlin
// Add to SimulationConfig.kt
@Serializable
data class SimulationConfig(
    // ...existing fields...
    val oneTimeExpenses: List<OneTimeExpense> = emptyList()
)

@Serializable
sealed interface OneTimeExpense {
    val id: String
    val name: String
    
    @Serializable
    @SerialName("CASH")
    data class Cash(
        override val id: String,
        override val name: String,
        val amount: Double,
        val yearOrAge: YearOrAge
    ) : OneTimeExpense
    
    @Serializable
    @SerialName("LOAN")
    data class Loan(
        override val id: String,
        override val name: String,
        val principal: Double,
        val aprPercent: Double,
        val termYears: Int,
        val startYearOrAge: YearOrAge,
        val monthlyPayment: Double  // Calculated and stored
    ) : OneTimeExpense
}

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

**Storage**:
- SQLite JSON column stores serialized oneTimeExpenses list
- Kotlin serialization handles sealed classes
- TypeScript mirrors the structure

**Alternatives Considered**:
- Separate expenses table: Rejected for added complexity and no query requirements
- Local storage only (no persistence): Rejected because users want to save configurations
- Session storage: Rejected because expenses should persist across browser sessions

---

### 6. Modal Dialog Pattern (Material-UI)

**Decision**: Use Material-UI Dialog component with custom content

**Rationale**:
- Material-UI Dialog provides built-in accessibility (ARIA, focus management)
- Responsive and follows Material Design guidelines
- Supports custom content while maintaining consistency
- Easy to test

**Implementation Pattern**:
```typescript
import { Dialog, DialogTitle, DialogContent, DialogActions, Button } from '@mui/material';

interface ExpenseBreakdownDialogProps {
  open: boolean;
  onClose: () => void;
  year: number;
  breakdown: ExpenseDetail[];
}

const ExpenseBreakdownDialog: React.FC<ExpenseBreakdownDialogProps> = ({
  open,
  onClose,
  year,
  breakdown
}) => {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>One-Time Expenses for Year {year}</DialogTitle>
      <DialogContent>
        <List>
          {breakdown.map((expense, index) => (
            <ListItem key={index}>
              <ListItemText
                primary={expense.name}
                secondary={`$${expense.amount.toLocaleString()}`}
              />
            </ListItem>
          ))}
        </List>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
};
```

**UX Considerations**:
- Only show info icon when multiple expenses exist in same year
- Click info icon opens modal
- Modal displays expense names and amounts in a list
- Close button and backdrop click dismiss modal
- Keyboard ESC key also dismisses modal (built-in Dialog behavior)

**Alternatives Considered**:
- Tooltip for breakdown: Rejected because may not fit multiple expenses
- Inline expansion: Rejected to maintain table layout consistency
- Custom modal component: Rejected in favor of Material-UI standard component

---

### 7. Execution Order for Expenses

**Decision**: Process one-time expenses AFTER regular expenses, sequentially within same period

**Rationale**:
- Spec explicitly states "One-time expenses paid AFTER regular living expenses"
- Sequential processing allows each expense to independently trigger spending strategy
- Maintains clarity in cashflow logic

**Processing Order (Monthly)**:
1. Process regular income (salary, social security, interest, dividends)
2. Process regular expenses (needs, wants, healthcare, taxes)
3. Process one-time expenses in order they were added:
   - Check SB balance
   - If insufficient, trigger spending strategy (draw from CBB, then equities)
   - Deduct expense from SB

**For Annual Calculations**:
- Cash expenses deducted in January
- Loan payments deducted monthly (12 full payments per year)
- Annual totals reflect cumulative one-time expense impact

**Implementation**:
```kotlin
// In simulation engine
fun processMonthlyExpenses(year: Int, month: Int) {
    // 1. Process regular income and expenses
    processRegularIncome()
    processRegularExpenses()
    
    // 2. Process one-time expenses sequentially
    getOneTimeExpensesForMonth(year, month).forEach { expense ->
        val withdrawalAmount = expense.getMonthlyAmount()
        
        // Check SB balance and trigger strategy if needed
        if (spendBucket < withdrawalAmount) {
            triggerSpendingStrategy(withdrawalAmount - spendBucket)
        }
        
        spendBucket -= withdrawalAmount
        totalOneTimeExpenses += withdrawalAmount
    }
}
```

**Alternatives Considered**:
- Process one-time expenses before regular: Rejected per spec clarification
- Aggregate all expenses then process: Rejected because spec says each expense independently triggers strategy

---

### 8. Mid-Year Loan Payment Handling

**Decision**: Always calculate full annual payment (12 months) regardless of start month

**Rationale**:
- Simplifies calculation logic
- Spec clarification states "Always full monthly payment amount regardless of start timing"
- Annual simulation granularity means monthly timing within year doesn't affect totals
- Conservative estimate (slightly overstates first-year payment if loan starts late in year)

**Implementation**:
- Loan starting at any point in year X results in 12 monthly payments in year X
- Subsequent years also have 12 monthly payments
- Final year (startYear + termYears - 1) has 12 monthly payments

**Example**:
- Loan: $100,000, 6% APR, 10 years, starting age 65 (year 2030)
- Monthly payment: $1,110.21 (calculated via amortization formula)
- Year 2030 (age 65): $13,322.52 (12 × $1,110.21)
- Years 2031-2038 (ages 66-73): $13,322.52 each
- Year 2039 (age 74): $13,322.52

**Alternatives Considered**:
- Pro-rate first year based on start month: Rejected per spec clarification and added complexity
- Track loan balance and recalculate: Rejected as out of scope (principal tracking not required)

---

### 9. Input Sanitization

**Decision**: HTML escaping for expense names, numeric validation for amounts

**Rationale**:
- Expense names displayed in UI (results table, modal dialog)
- Prevent XSS attacks via malicious expense names
- Kotlin JSON serialization handles SQL injection prevention
- TypeScript strict mode provides type safety

**Sanitization Rules**:
- Expense names: HTML escape special characters (<, >, &, ", ')
- Amounts: Validate numeric, reject negative, round to 2 decimal places
- APR: Validate numeric, reject negative, maximum 99.99%
- Term: Validate integer, reject negative, maximum 50 years

**Frontend Implementation**:
```typescript
const sanitizeExpenseName = (name: string): string => {
  return name
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;');
};

const validateAmount = (amount: number): boolean => {
  return amount > 0 && amount < 1e12 && Number.isFinite(amount);
};
```

**Backend Implementation**:
```kotlin
fun sanitizeExpenseName(name: String): String {
    // Trim whitespace, limit length
    return name.trim().take(100)
}

fun validateAmount(amount: Double): Boolean {
    return amount > 0 && amount.isFinite()
}
```

**Alternatives Considered**:
- No sanitization: Rejected for security risk
- Full HTML rendering of expense names: Rejected, not needed for simple text display
- Database-level sanitization only: Rejected, defense in depth requires multiple layers

---

### 10. Performance Considerations

**Decision**: No special optimization needed for baseline (<10 expenses)

**Rationale**:
- Spec states typical usage <10 expenses
- Linear processing (O(n) where n = number of expenses) is acceptable
- Kotlin simulation engine already handles 35 years × 12 months = 420 iterations efficiently
- Adding 10 expenses per year adds minimal overhead

**Performance Testing**:
- Success criterion SC-004: Simulation completes within 30 seconds for 10 concurrent expenses
- Current simulation baseline: <1 second for 35 years
- Expected overhead: Negligible (<100ms for 10 expenses × 35 years)

**Future Optimization (if needed)**:
- Index expenses by year for faster lookup
- Cache monthly payment calculations
- Use parallel processing for Monte Carlo simulations

**Alternatives Considered**:
- Database indexing for expenses: Rejected, expenses stored as JSON array
- Expense limit enforcement: Rejected per spec (no hard limit)
- Lazy calculation: Rejected, calculation is fast enough

---

## Technology-Specific Best Practices

### Kotlin (API Server)

**Sealed Interfaces for OneTimeExpense**:
- Use `sealed interface` for type-safe expense types
- Kotlinx.serialization with `@SerialName` for JSON polymorphism
- Exhaustive when expressions ensure all types handled

**Data Validation**:
- Use `require()` for precondition checks
- Custom validation functions return structured errors
- Ktor's `ContentNegotiation` for automatic JSON serialization

**Testing**:
- JUnit 5 for unit tests
- Test amortization formula against known values
- Test age/year conversion edge cases

### TypeScript (Frontend & Backend)

**Type Safety**:
- Use discriminated unions for OneTimeExpense types
- Strict null checks enabled
- No `any` types

**React Components**:
- Functional components with hooks
- Material-UI components for consistency
- Controlled form inputs

**State Management**:
- Local component state for expense list (array)
- Form validation on change and submit
- Optimistic UI updates

### Material-UI

**Component Choices**:
- `TextField` for text and numeric inputs
- `Select` for expense type dropdown
- `IconButton` with `InfoIcon` for breakdown trigger
- `Dialog` for breakdown modal
- `Alert` for validation errors

**Styling**:
- Use `sx` prop for inline styles
- Theme customization if needed
- Responsive design (mobile-first)

---

## Summary

All technical unknowns have been resolved:

1. ✅ Age-to-year conversion: Simple arithmetic with validation
2. ✅ Amortization formula: Standard formula with 0% APR special case
3. ✅ Breakdown structure: Typed list of ExpenseDetail objects
4. ✅ Error handling: Multi-layered with Material-UI components
5. ✅ Persistence: Part of SimulationConfig in SQLite
6. ✅ Modal pattern: Material-UI Dialog component
7. ✅ Execution order: After regular expenses, sequential processing
8. ✅ Mid-year loans: Full annual payment (12 months)
9. ✅ Sanitization: HTML escaping and numeric validation
10. ✅ Performance: No special optimization needed

Ready to proceed to Phase 1: Design & Contracts.

