# Implementation Plan: One-Time Expenses

**Branch**: `003-one-time-expenses` | **Date**: 2026-01-20 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/003-one-time-expenses/spec.md`

---

## Summary

This feature extends the retirement cash flow simulator to support one-time expenses, enabling users to model major purchases and loans during retirement. Users can add multiple expenses (cash payments or loans) with flexible timing specifications (by age or calendar year). The simulation engine processes these expenses after regular living expenses, triggering the spending strategy when necessary. Results display includes a new column showing one-time expense totals, with a breakdown modal for years with multiple concurrent expenses.

**Technical Approach**:
- Extend existing `SimulationConfig` data model with `oneTimeExpenses` list
- Use sealed interfaces/discriminated unions for type-safe expense types (Cash vs Loan)
- Implement standard loan amortization formula with special case for 0% APR
- Integrate expense processing into existing simulation engine after regular expenses
- Add breakdown display using Material-UI Dialog component
- No new API endpoints required; extends existing `/api/simulate` endpoint

---

## Technical Context

**Language/Version**:
- Kotlin 1.9+ (JVM 17) - API Server
- TypeScript 5.x - Frontend & Backend
- Deno 1.x - Backend (BFF)

**Primary Dependencies**:
- **Kotlin**: Ktor 2.x (web framework), Exposed ORM, kotlinx.serialization, SQLite JDBC
- **Frontend**: React 18, Vite 5, Material-UI (@mui/material), Recharts, Vitest
- **Backend**: Oak (Deno web framework)

**Storage**: SQLite database (accessed only by Kotlin API server)

**Testing**:
- **Kotlin**: JUnit 5 with MockK for unit tests
- **Frontend**: Vitest + React Testing Library
- **Backend**: `deno test` for integration tests

**Target Platform**: Web application (local/server deployment)

**Project Type**: N-tier web application (Frontend → Deno BFF → Kotlin API → SQLite)

**Performance Goals**:
- Simulation completion: <1 second for interactive mode (existing baseline)
- Monte Carlo (1000 runs): <10 seconds (existing baseline)
- One-time expenses overhead: <100ms for 10 expenses across 35 years
- Breakdown modal load time: <1 second

**Constraints**:
- Must maintain backward compatibility with existing saved SimulationConfigs
- Test-First Development required per constitution
- Strict N-tier boundaries (Frontend only talks to Deno BFF, not Kotlin directly)
- Financial calculation accuracy: within $0.01 of external calculators

**Scale/Scope**:
- Typical usage: <10 one-time expenses per simulation
- No hard limit enforced per spec
- 35-year simulation period (420 months)
- Single user (local persistence)

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

### Pre-Research Check (PASS)

✅ **Test-First Development**: Acknowledged and planned
- All phases include test tasks
- Unit tests for amortization formula, validation, data models
- Integration tests for simulation processing
- Component tests for UI elements

✅ **Strict N-Tier Boundaries**: Maintained
- Frontend communicates only with Deno Backend
- Deno Backend proxies to Kotlin API Server
- Kotlin API Server exclusively accesses SQLite
- No tier boundary violations

✅ **Explicit Contracts & Type Safety**: Ensured
- Sealed interfaces in Kotlin for OneTimeExpense types
- Discriminated unions in TypeScript
- Shared data model documented in data-model.md
- API contract defined in contracts/api-contract.md

✅ **Deterministic Simulation**: Preserved
- One-time expenses processed deterministically in sequence
- Same SimulationConfig produces same results
- No random elements in expense processing
- Monte Carlo seed handling unchanged

✅ **Standard Conventions**: Followed
- Kotlin: data classes, sealed interfaces, kotlinx.serialization
- TypeScript: strict mode, no `any`, interface-based types
- React: functional components with hooks
- Material-UI standard components

### Post-Design Check (PASS)

✅ **No New Technologies**: True
- Uses existing Kotlin, TypeScript, React, Material-UI stack
- No new frameworks or libraries introduced
- Leverages existing serialization and validation patterns

✅ **No Architecture Changes**: True
- Extends existing models (SimulationConfig, YearlyResult, CashFlow)
- Uses existing API endpoint (`/api/simulate`)
- No new API routes required
- No database schema changes (JSON column stores expenses)

**Result**: PASSED - No constitution violations. Feature aligns with all core principles and development standards.

---

## Project Structure

### Documentation (this feature)

```text
specs/003-one-time-expenses/
├── plan.md              # This file
├── research.md          # Phase 0 output (COMPLETE)
├── data-model.md        # Phase 1 output (COMPLETE)
├── quickstart.md        # Phase 1 output (COMPLETE)
├── contracts/           # Phase 1 output (COMPLETE)
│   └── api-contract.md
├── spec.md              # Feature specification (pre-existing)
├── ANALYSIS-REPORT.md   # Analysis from speckit.analyze (pre-existing)
└── tasks.md             # Phase 2 output (to be created by speckit.tasks)
```

### Source Code (repository root)

```text
api-server/                      # Kotlin API Server
├── src/
│   ├── main/
│   │   ├── kotlin/com/retirement/
│   │   │   ├── model/
│   │   │   │   ├── SimulationConfig.kt      # MODIFY: Add oneTimeExpenses field
│   │   │   │   ├── CashFlow.kt              # MODIFY: Add oneTimeExpenses field
│   │   │   │   ├── YearlyResult.kt          # MODIFY: Add oneTimeExpensesBreakdown field
│   │   │   │   └── OneTimeExpense.kt        # NEW: Expense data models
│   │   │   ├── logic/
│   │   │   │   ├── SimulationEngine.kt      # MODIFY: Integrate expense processing
│   │   │   │   ├── OneTimeExpenseProcessor.kt # NEW: Expense processing logic
│   │   │   │   └── expenseCalculations.kt   # NEW: Amortization, conversions
│   │   │   ├── validation/
│   │   │   │   └── ExpenseValidator.kt      # NEW: Validation rules
│   │   │   └── api/
│   │   │       └── SimulationRoutes.kt      # MODIFY: Add expense validation
│   │   └── resources/
│   └── test/
│       └── kotlin/com/retirement/
│           ├── model/
│           │   └── OneTimeExpenseTest.kt    # NEW: Data model tests
│           ├── logic/
│           │   ├── ExpenseCalculationsTest.kt # NEW: Formula tests
│           │   └── ExpenseProcessorTest.kt  # NEW: Processing tests
│           └── validation/
│               └── ExpenseValidatorTest.kt  # NEW: Validation tests
└── build.gradle.kts

frontend/                        # React Frontend
├── src/
│   ├── components/
│   │   ├── SimulationForm.tsx           # MODIFY: Add expense form section
│   │   ├── ExpenseForm.tsx              # NEW: Expense input form
│   │   ├── ExpenseList.tsx              # NEW: List of added expenses
│   │   └── ExpenseBreakdownDialog.tsx   # NEW: Breakdown modal
│   ├── pages/
│   │   └── ResultsPage.tsx              # MODIFY: Add expense column + modal
│   ├── types/
│   │   └── simulation.ts                # MODIFY: Add OneTimeExpense types
│   ├── utils/
│   │   └── expenseCalculations.ts       # NEW: Frontend calculations
│   └── services/
│       └── api.ts                       # No changes (passes through)
└── tests/
    └── components/
        ├── ExpenseForm.test.tsx         # NEW: Component tests
        ├── ExpenseList.test.tsx         # NEW: Component tests
        └── ExpenseBreakdownDialog.test.tsx # NEW: Component tests

backend/                         # Deno Backend (BFF)
├── src/
│   ├── types.ts                 # MODIFY: Add OneTimeExpense types
│   └── routes/
│       └── simulate.ts          # No logic changes (proxy only)
└── tests/
    └── simulate.test.ts         # MODIFY: Add expense test cases
```

**Structure Decision**: The feature integrates cleanly into the existing N-tier architecture:
- **Kotlin API Server** handles all business logic (calculation, validation, processing)
- **Frontend** provides UI components for expense input and result display
- **Deno Backend** acts as transparent proxy (no expense-specific logic)

This maintains separation of concerns and follows the established pattern from the base retirement cashflow feature (002-retirement-cashflow).

---

## Complexity Tracking

No complexity violations. This section intentionally empty per template guidance.

**Justification**: Feature extends existing architecture without introducing new technologies, patterns, or tier boundaries. All implementation follows established conventions.

---

## Architecture Decisions

### 1. Data Model: Sealed Interfaces vs Class Hierarchy

**Decision**: Use Kotlin sealed interfaces and TypeScript discriminated unions

**Rationale**:
- Type-safe expense type handling (Cash vs Loan)
- Exhaustive when/switch expressions ensure all types handled
- Clean serialization with kotlinx.serialization and JSON
- No inheritance complexity; composition preferred

**Implementation**:
```kotlin
@Serializable
sealed interface OneTimeExpense {
    val id: String
    val name: String
}

@Serializable
@SerialName("CASH")
data class CashExpense(...) : OneTimeExpense

@Serializable
@SerialName("LOAN")
data class LoanExpense(...) : OneTimeExpense
```

**Alternatives Considered**:
- Abstract base class: Rejected for less flexibility
- Single class with type discriminator: Rejected for weak type safety

---

### 2. Timing Specification: Flexible Year/Age Format

**Decision**: Support both calendar year and age-based timing via `YearOrAge` sealed interface

**Rationale**:
- Users think in different terms (e.g., "at age 67" vs "in 2030")
- Simple conversion logic: `year = currentYear + (age - currentAge)`
- Single source of truth (converted once during simulation setup)
- Explicit type makes intent clear

**Implementation**:
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

**Alternatives Considered**:
- Year only: Rejected for poor UX (users think in ages)
- Age only: Rejected (calendar year also valid mental model)
- Separate fields with flag: Rejected for type safety

---

### 3. Amortization Calculation: Pre-calculate and Store

**Decision**: Calculate `monthlyPayment` once when loan created, store in `LoanExpense`

**Rationale**:
- Calculation is deterministic (same inputs always yield same output)
- Avoids repeated calculation in simulation loop (420 months × N loans)
- Frontend can show monthly payment before simulation runs (UX benefit)
- Simplifies simulation logic (just use stored value)

**Implementation**:
```kotlin
// In frontend/backend when user creates loan
val monthlyPayment = calculateMonthlyPayment(principal, aprPercent, termYears)
val loan = LoanExpense(..., monthlyPayment = monthlyPayment)
```

**Alternatives Considered**:
- Calculate on-demand: Rejected for performance (repeated calculation)
- Derived property: Rejected (calculation complex, better as stored value)

---

### 4. Persistence: Part of SimulationConfig

**Decision**: Store `oneTimeExpenses` as list field in `SimulationConfig`

**Rationale**:
- Expenses are configuration data, not transactional
- Ensures atomicity (all config saved/loaded together)
- Simplifies data model (no joins, foreign keys)
- SQLite JSON column handles serialization
- Backward compatible (field defaults to empty list)

**Implementation**:
```kotlin
@Serializable
data class SimulationConfig(
    // ...existing fields...
    val oneTimeExpenses: List<OneTimeExpense> = emptyList()
)
```

**Alternatives Considered**:
- Separate expenses table: Rejected for added complexity, no query requirements
- Local storage only: Rejected (users want persistent configs)

---

### 5. Breakdown Display: Conditional Population

**Decision**: Only populate `oneTimeExpensesBreakdown` when 2+ expenses in year

**Rationale**:
- Reduces JSON payload size (breakdown null for 95% of years)
- Clear UX pattern: info icon only appears when needed
- Matches spec requirement (FR-031, FR-032)

**Implementation**:
```kotlin
val breakdown = buildBreakdownForYear(year)
yearlyResult.oneTimeExpensesBreakdown = if (breakdown.size > 1) breakdown else null
```

**Alternatives Considered**:
- Always include breakdown: Rejected for payload bloat
- Client-side breakdown building: Rejected (requires passing all expense data)

---

### 6. Processing Order: After Regular Expenses

**Decision**: Process one-time expenses AFTER all regular expenses in each period

**Rationale**:
- Explicit spec requirement (clarification: "One-time expenses paid AFTER regular living expenses")
- Each expense independently evaluates SB balance and triggers spending strategy
- Sequential processing maintains determinism
- Clear separation in code

**Implementation**:
```kotlin
fun processMonth(month: Int) {
    processRegularIncome()
    processRegularExpenses()
    // Now process one-time expenses
    processOneTimeExpenses(month)
}
```

**Alternatives Considered**:
- Before regular expenses: Rejected per spec
- Aggregate then process: Rejected (spec says each triggers strategy independently)

---

### 7. Validation: Multi-layered Defense

**Decision**: Validate in frontend (immediate feedback) AND backend (security)

**Rationale**:
- Frontend validation provides instant UX feedback
- Backend validation ensures data integrity (can't bypass client validation)
- Consistent error messages between tiers
- Defense in depth

**Implementation**:
- Frontend: TypeScript validation functions, Material-UI error display
- Backend: Kotlin validation before simulation, structured error responses

**Alternatives Considered**:
- Backend only: Rejected for poor UX (network round-trip for each error)
- Frontend only: Rejected for security (can't trust client)

---

### 8. Error Handling: Structured Validation Errors

**Decision**: Return structured validation errors with field-level details

**Rationale**:
- Enables client to highlight specific fields
- Clear error messages improve UX
- Consistent with REST API best practices
- Status code 422 indicates validation failure

**Implementation**:
```json
{
  "error": "Validation failed",
  "statusCode": 422,
  "validationErrors": [
    {
      "field": "oneTimeExpenses[0].amount",
      "message": "Amount must be positive"
    }
  ]
}
```

**Alternatives Considered**:
- Generic error message: Rejected for poor UX
- Exception-based: Rejected (validation errors are expected, not exceptions)

---

### 9. UI Components: Material-UI Standards

**Decision**: Use Material-UI Dialog, TextField, Select components

**Rationale**:
- Consistency with existing app UI
- Built-in accessibility (ARIA, focus management)
- Responsive design out of the box
- Well-tested component library
- Matches existing design system

**Components**:
- `Dialog` for breakdown modal
- `TextField` for text/numeric inputs
- `Select` for expense type dropdown
- `IconButton` + `InfoIcon` for breakdown trigger
- `Alert` for validation errors

**Alternatives Considered**:
- Custom components: Rejected for reinventing wheel
- Different UI library: Rejected for inconsistency

---

### 10. Testing: Test-First Development

**Decision**: Write tests BEFORE implementation for all core logic

**Rationale**:
- Constitution requirement (NON-NEGOTIABLE)
- Ensures financial calculation accuracy
- Prevents regressions
- Documents expected behavior
- Enables confident refactoring

**Test Coverage**:
- **Unit**: Amortization formula, validation, conversions
- **Integration**: Simulation with expenses, spending strategy triggered
- **Component**: Form rendering, dialog interaction
- **E2E**: Full user flow (add expense → simulate → view results)

**Alternatives Considered**: None (constitution mandate)

---

## Component Interactions

### Simulation Flow with One-Time Expenses

```
User Input (Frontend)
    ↓
    SimulationForm
    ├── ExpenseForm (add/edit expenses)
    └── ExpenseList (display, remove)
    ↓
    Submit SimulationConfig with oneTimeExpenses[]
    ↓
Deno Backend (BFF)
    ↓
    Proxy to Kotlin API
    ↓
Kotlin API Server
    ├── ExpenseValidator.validate()
    ├── SimulationEngine.run()
    │   ├── For each year:
    │   │   ├── Process regular income
    │   │   ├── Process regular expenses
    │   │   └── OneTimeExpenseProcessor.process()
    │   │       ├── Get expenses for this year
    │   │       ├── For each expense:
    │   │       │   ├── Calculate payment amount
    │   │       │   ├── Check SB balance
    │   │       │   ├── Trigger spending strategy if needed
    │   │       │   ├── Deduct from SB
    │   │       │   └── Add to breakdown
    │   │       └── If breakdown.size > 1, add to YearlyResult
    │   └── Return SimulationResult
    ↓
Deno Backend → Frontend
    ↓
ResultsPage
    ├── Results table with "One-Time Expenses" column
    └── ExpenseBreakdownDialog (opens on info icon click)
```

---

## Implementation Phases

Following the template workflow, Phase 0 (Research) and Phase 1 (Design & Contracts) are **COMPLETE**:

### Phase 0: Research ✅ COMPLETE

**Output**: research.md

**Covered**:
- Age-to-year conversion logic
- Amortization formula implementation (standard + 0% APR)
- Breakdown data structure
- Error handling strategy
- Persistence strategy
- Modal dialog pattern
- Execution order
- Mid-year loan handling
- Input sanitization
- Performance considerations

---

### Phase 1: Design & Contracts ✅ COMPLETE

**Outputs**:
- data-model.md ✅
- contracts/api-contract.md ✅
- quickstart.md ✅
- Agent context updated ✅

**Covered**:
- Complete data model for all expense types
- Sealed interfaces and discriminated unions
- Integration with existing models (SimulationConfig, CashFlow, YearlyResult)
- Validation rules
- API contracts (request/response schemas)
- Serialization examples
- Testing strategy

---

### Phase 2: Tasks (NOT YET STARTED)

**Note**: This phase is handled by the separate `/speckit.tasks` command, not this `/speckit.plan` command.

**Expected Output**: tasks.md with:
- Granular implementation tasks
- Test tasks (Test-First Development)
- Dependency ordering
- File paths for each task
- Parallel execution markers where safe

---

## Re-evaluation of Constitution Check (Post-Design)

### Test-First Development ✅

**Status**: CONFIRMED in design

- Research document specifies testing strategy
- Data model includes validation rules to test
- API contract defines test cases
- Quick start includes testing checklist
- Constitution requirement will be met during Phase 2 (tasks) and implementation

### N-Tier Boundaries ✅

**Status**: NO VIOLATIONS

- Frontend only talks to Deno Backend (no direct Kotlin calls)
- Deno Backend proxies to Kotlin API (no business logic in BFF)
- Kotlin API exclusively accesses SQLite
- Data model contracts ensure type safety across tiers

### Explicit Contracts ✅

**Status**: FULLY DEFINED

- API contract document specifies request/response schemas
- Data model document defines all types (Kotlin and TypeScript)
- Serialization examples provided
- Validation rules documented for both tiers

### Deterministic Simulation ✅

**Status**: PRESERVED

- Expense processing is sequential and deterministic
- No randomness in expense calculation or processing
- Same SimulationConfig always produces same result
- Monte Carlo random seed handling unchanged

**Final Result**: ✅ PASSED - All constitution requirements met. Ready to proceed to Phase 2 (tasks).

---

## Risk Assessment

### Technical Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Amortization calculation inaccuracy | Low | High | Unit tests against external calculators, Test-First approach |
| Breakdown modal performance with many expenses | Low | Medium | Lazy rendering, pagination if >20 expenses (unlikely) |
| Backward compatibility broken | Low | High | Default empty list, extensive testing with old configs |
| Spending strategy triggered incorrectly | Medium | High | Integration tests, clear processing order documentation |
| Age/year conversion edge cases | Medium | Medium | Comprehensive validation, reject historical dates |

### Implementation Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Forgetting Test-First | Medium | High | Constitution enforcement, code review checklist |
| Processing order wrong (before vs after regular) | Medium | High | Clear spec, integration tests, code comments |
| Not handling 0% APR | Low | Medium | Explicit test case, documented in formula |
| Breakdown shown for single expense | Low | Low | Conditional logic, component tests |

---

## Success Metrics

From spec.md Success Criteria, implementation must achieve:

1. ✅ **SC-001**: User can add cash expense and see impact within 30 seconds
   - **Measurement**: Manual testing, user observation
   
2. ✅ **SC-002**: Loan monthly payment auto-calculated and displayed immediately
   - **Measurement**: Frontend unit test, UX testing
   
3. ✅ **SC-003**: Support 10+ expenses without performance degradation
   - **Measurement**: Load test with 15 expenses, ensure <1s simulation time
   
4. ✅ **SC-004**: 95% of users understand impact without documentation
   - **Measurement**: User testing, usability study
   
5. ✅ **SC-005**: Breakdown dialog loads within 1 second
   - **Measurement**: Performance profiling, React DevTools
   
6. ✅ **SC-006**: Loan calculations accurate within $0.01
   - **Measurement**: Automated tests against bankrate.com calculator
   
7. ✅ **SC-007**: Complete "add expense → see results" in <2 minutes
   - **Measurement**: User testing, task completion time
   
8. ✅ **SC-008**: Zero calculation errors for full 35-year period
   - **Measurement**: Integration tests with edge cases
   
9. ✅ **SC-009**: Results table correct for 100% of test scenarios
   - **Measurement**: Test suite coverage, automated assertions
   
10. ✅ **SC-010**: Users distinguish single vs multiple expenses without instructions
    - **Measurement**: A/B testing, user observation (info icon only when needed)

---

## Next Steps

1. ✅ **COMPLETE**: Phase 0 Research (research.md)
2. ✅ **COMPLETE**: Phase 1 Design & Contracts (data-model.md, contracts/, quickstart.md)
3. ⏭️ **NEXT**: Run `/speckit.tasks` command to generate tasks.md
4. ⏭️ **THEN**: Begin implementation following Test-First Development
5. ⏭️ **ONGOING**: Refer to quickstart.md for implementation guidance

---

## Appendix: Key Formulas

### Amortization Formula (APR > 0)

```
M = P × [r(1+r)^n] / [(1+r)^n - 1]

Where:
- M = Monthly payment
- P = Principal (loan amount)
- r = Monthly interest rate (APR / 100 / 12)
- n = Total number of months (termYears × 12)
```

**Example**:
- Principal: $100,000
- APR: 6% (0.06)
- Term: 10 years
- Monthly rate r: 0.06 / 12 = 0.005
- Months n: 10 × 12 = 120
- Monthly payment M: $1,110.21

### Simple Division (APR = 0)

```
M = P / n

Where:
- M = Monthly payment
- P = Principal
- n = Total number of months
```

**Example**:
- Principal: $60,000
- APR: 0%
- Term: 5 years
- Months n: 60
- Monthly payment M: $1,000.00

### Age-to-Year Conversion

```
targetYear = currentYear + (targetAge - currentAge)
```

**Example**:
- Current year: 2026
- Current age: 60
- Target age: 67
- Target year: 2026 + (67 - 60) = 2033

### Year-to-Age Conversion

```
targetAge = currentAge + (targetYear - currentYear)
```

**Example**:
- Current year: 2026
- Current age: 60
- Target year: 2035
- Target age: 60 + (2035 - 2026) = 69

---

## Summary

This implementation plan provides:

✅ Complete technical context and architecture decisions  
✅ Resolved all technical unknowns through research  
✅ Defined comprehensive data models with validation rules  
✅ Specified API contracts between all tiers  
✅ Created quick start guide for developers  
✅ Confirmed constitution compliance (Test-First, N-tier, type safety)  
✅ Documented component interactions and data flow  
✅ Identified risks and mitigation strategies  
✅ Established success metrics  

**Ready for Phase 2**: Generate tasks.md with `/speckit.tasks` command and begin Test-First implementation.

**Feature Status**: Planning complete, implementation pending.

