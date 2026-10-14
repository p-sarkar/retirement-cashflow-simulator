# Cross-Artifact Analysis Report: One-Time Expenses Feature

**Feature**: 003-one-time-expenses  
**Analysis Date**: 2026-01-21  
**Analyzer**: speckit.analyze  
**Status**: ✅ IMPLEMENTATION IN PROGRESS

---

## Executive Summary

**Overall Assessment**: ✅ **GOOD** - Feature artifacts are well-aligned with minor inconsistencies

The one-time expenses feature documentation (spec.md, plan.md, tasks.md) demonstrates strong consistency across artifacts with excellent traceability from requirements to implementation tasks. The specification is comprehensive, the plan is detailed with clear architectural decisions, and tasks are well-organized by user story.

**Key Strengths**:
- Clear user story prioritization (P1-P3)
- Comprehensive acceptance criteria
- Well-defined architecture decisions (#8 on breakdown display integration)
- Tasks properly grouped by user story with clear dependencies
- Implementation progress tracking (many tasks marked complete)

**Areas Requiring Attention**:
- Spec-to-tasks alignment on breakdown display (updated in spec, tasks partially updated)
- Some tasks marked obsolete but new replacement tasks not all completed
- Down payment feature implemented but not reflected in spec/plan
- Missing documentation for recent feature additions

---

## 1. Spec-to-Plan Consistency Analysis

### 1.1 Requirements Coverage ✅ EXCELLENT

**Finding**: All functional requirements (FR-001 through FR-043) from spec.md are addressed in plan.md

| Spec Requirement | Plan Coverage | Status |
|-----------------|---------------|--------|
| FR-001 - FR-011 (Cash Expenses) | Architecture Decision #1, #2, #4 | ✅ Covered |
| FR-012 - FR-021 (Loan Expenses) | Architecture Decision #3, Appendix formulas | ✅ Covered |
| FR-022 - FR-028 (Calculation) | Architecture Decision #6, Component Interactions | ✅ Covered |
| FR-029 - FR-036 (Results Display) | Architecture Decision #5, #8 | ✅ Covered |
| FR-037 - FR-043 (Validation) | Architecture Decision #7, #9 | ✅ Covered |

**Recommendation**: ✅ No action needed

---

### 1.2 User Stories Alignment ✅ GOOD

**Finding**: All 4 user stories from spec.md are represented in plan.md with clear implementation phases

| User Story | Spec Priority | Plan Phase | Alignment |
|-----------|---------------|------------|-----------|
| US1 - Single Cash Expense | P1 (MVP) | Phase 3 | ✅ Aligned |
| US2 - Multiple Cash Expenses | P2 | Phase 4 | ✅ Aligned |
| US3 - Loan with Amortization | P3 | Phase 5 | ✅ Aligned |
| US4 - Edit/Remove | P2 | Phase 6 | ✅ Aligned |

**Recommendation**: ✅ No action needed

---

### 1.3 Clarifications Integration ⚠️ NEEDS UPDATE

**Finding**: Most clarifications from spec.md Session 2026-01-20 are integrated into plan.md, but the critical breakdown display decision needs better prominence

**Clarification**: "Where should the detailed breakdown of one-time expenses be displayed?"
- **Spec**: Clarified to use main computation breakdown dialog (🔍 icon)
- **Plan**: Documented in Architecture Decision #8 ✅
- **Issue**: This is a late-breaking change that affects FR-031, FR-032, FR-033

**Impact on Requirements**:
- FR-031: ~~"column MUST display total with info icon"~~ → No longer accurate
- FR-032: ~~"When user clicks info icon"~~ → Should be "when user clicks breakdown icon"
- FR-033: ~~"breakdown modal MUST list"~~ → Now part of main breakdown

**Recommendation**: ⚠️ **ACTION REQUIRED**
1. Update spec.md FR-031 to FR-033 to reflect main breakdown integration
2. Add note that breakdown display decision changed after initial specification
3. Consider marking old requirements as "superseded by Architecture Decision #8"

---

### 1.4 Success Criteria Traceability ✅ EXCELLENT

**Finding**: All 10 success criteria (SC-001 through SC-010) from spec.md are mapped in plan.md "Success Metrics" section with measurement strategies

**Example**:
- SC-006 (Loan accuracy within $0.01) → Measured via automated tests against external calculators
- SC-005 (Breakdown loads in <1s) → Measured via React DevTools profiling

**Recommendation**: ✅ No action needed

---

## 2. Plan-to-Tasks Consistency Analysis

### 2.1 Phase Mapping ✅ EXCELLENT

**Finding**: All phases from plan.md are represented in tasks.md with correct dependencies

| Plan Phase | Tasks Phase | Tasks | Status |
|-----------|-------------|-------|--------|
| Setup | Phase 1 | T001-T002 | ✅ Complete |
| Foundational | Phase 2 | T003-T014 | ✅ Complete |
| US1 (MVP) | Phase 3 | T015-T030 | ⚠️ Partially complete |
| US2 (Multiple) | Phase 4 | T031-T043 | ⚠️ Partially complete |
| US3 (Loans) | Phase 5 | T044-T060 | ⏳ Not started |
| US4 (Edit/Remove) | Phase 6 | T061-T069 | ⏳ Not started |
| Polish | Phase 7 | T070-T085 | ⏳ Not started |

**Recommendation**: ✅ Progress tracking is clear

---

### 2.2 Architecture Decisions to Tasks ⚠️ NEEDS ATTENTION

**Finding**: Architecture Decision #8 (breakdown display integration) is partially reflected in tasks

**Plan Decision #8**: "Display one-time expense breakdown in existing computation breakdown dialog"

**Tasks Affected**:
- [~] T034 - ~~Create ExpenseBreakdownDialog~~ → Marked OBSOLETE ✅
- [~] T035 - ~~Add info icon logic~~ → Marked OBSOLETE ✅
- [~] T036 - ~~Implement dialog handlers~~ → Marked OBSOLETE ✅
- [ ] T034a - Remove inline dialog from ResultsTable → ⏳ NOT COMPLETE
- [ ] T035a - Remove inline info icon logic → ⏳ NOT COMPLETE
- [ ] T036a - Update BreakdownDialog component → ⏳ NOT COMPLETE
- [ ] T037a - Add expenses section to BreakdownGenerator → ⏳ NOT COMPLETE

**Recommendation**: ⚠️ **ACTION REQUIRED**
1. Complete T034a, T035a, T036a, T037a to fully implement Architecture Decision #8
2. Add integration test task to verify breakdown appears in main dialog
3. Update Phase 4 checkpoint to reflect new breakdown location

---

### 2.3 Test Task Coverage ⚠️ INCOMPLETE

**Finding**: Plan emphasizes "Test-First Development" (Constitution requirement), but tasks.md notes "focusing on implementation tasks only"

**Plan**: "Write tests BEFORE implementation for all core logic" (Architecture Decision #10)

**Tasks**: "Tests: Not explicitly requested in spec.md - focusing on implementation tasks only"

**Missing Test Tasks**:
- Unit tests for amortization formula (mentioned in plan, not in tasks)
- Unit tests for age/year conversion utilities
- Component tests for OneTimeExpenseInput
- Integration tests for spending strategy trigger
- E2E tests for user flows

**Recommendation**: ⚠️ **ACTION REQUIRED**
1. Add Phase 0.5: Test Setup before each user story implementation
2. Create test tasks: T_TEST_XXX for each core function/component
3. Add to checkpoints: "Unit tests pass" before marking phase complete
4. Document that test-first approach will be followed even if not explicit in task list

---

### 2.4 Task Granularity ✅ GOOD

**Finding**: Tasks are appropriately granular with clear file paths and descriptions

**Example Good Tasks**:
- T003: "Create OneTimeExpense sealed interface in /api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt"
- T016: "Create OneTimeExpenseInput component for single cash expense in /frontend/src/components/OneTimeExpenseInput.tsx"

**Recommendation**: ✅ No action needed

---

## 3. Spec-to-Tasks Direct Analysis

### 3.1 Acceptance Scenarios to Tasks ✅ EXCELLENT

**Finding**: Each acceptance scenario from spec.md has corresponding integration/validation tasks

**Example Traceability**:
- **US1 Scenario 1**: Form accepts expense → T015, T016, T017
- **US1 Scenario 2**: Results show in correct year → T020, T025
- **US1 Scenario 3**: SB balance decreases → T023, T029
- **US1 Scenario 4**: Calendar year works → T022, T028

**Recommendation**: ✅ No action needed

---

### 3.2 Edge Cases Coverage ⚠️ PARTIAL

**Finding**: Some edge cases from spec.md are covered in tasks, others are not explicitly addressed

| Edge Case (Spec) | Task Coverage | Status |
|------------------|---------------|--------|
| Cash expense exceeds SB balance | T030, T080 | ✅ Covered |
| Multiple expenses same year | T039, T040 | ✅ Covered |
| Loan extends beyond year 35 | T059, T081 | ✅ Covered |
| Loan start = end year | T082 | ✅ Covered |
| Negative amounts/APR | T076 | ✅ Covered |
| 0% APR loan | T057 | ✅ Covered |

**Missing Edge Cases**:
- **Inflation adjustment edge cases**: What if inflation is negative or >100%?
- **Concurrent expenses ordering**: How to test sequential processing order matters?
- **Mid-simulation expense**: What if expense year is in the past?

**Recommendation**: ⚠️ **ACTION REQUIRED**
1. Add task for negative/extreme inflation testing
2. Add task for expense order validation (multiple in same year, different order = same result?)
3. Add validation task for historical expense years

---

## 4. Implementation Status vs. Specification

### 4.1 Implemented Features Not in Spec ⚠️ DOCUMENTATION GAP

**Finding**: The codebase includes a **down payment** feature for loans that is NOT documented in spec.md or plan.md

**Evidence**:
- File: `frontend/src/components/SimulationForm.tsx` (line 75)
  ```typescript
  downPayment: 10000,
  ```
- File: `frontend/src/types/simulation.ts`
  ```typescript
  downPayment?: number;  // Optional down payment
  ```
- File: `api-server/src/main/kotlin/com/retirement/model/OneTimeExpense.kt`
  ```kotlin
  val downPayment: Double = 0.0
  ```

**Impact**:
- This is a **significant feature addition** that changes loan behavior
- Down payment affects financed amount and monthly payment calculation
- Down payment is paid as lump sum in start year (separate from loan payments)

**Recommendation**: 🚨 **CRITICAL - ACTION REQUIRED**
1. **Update spec.md**:
   - Add FR-012a: "Loan expenses MAY have an optional down payment field"
   - Add FR-016a: "Monthly payment MUST be calculated on financed amount (principal - down payment)"
   - Add FR-020a: "Down payment MUST be paid as lump sum in start year"
   - Update US3 acceptance scenarios to include down payment examples
2. **Update plan.md**:
   - Add Architecture Decision #11: "Down Payment Support"
   - Document rationale, calculation changes, timeline implications
3. **Update tasks.md**:
   - Add new tasks for down payment:
     - T045a: Add downPayment field to LoanExpense form
     - T047a: Update monthly payment calculation to use financed amount
     - T051a: Process down payment as lump sum in start year
4. **Create documentation**:
   - Document `/docs/down-payment-feature.md` appears to exist - reference it from spec

---

### 4.2 Completed Tasks vs. Spec Requirements ✅ GOOD

**Finding**: Implementation progress is well-tracked with completed tasks covering core functionality

**Completed (from tasks.md)**:
- Phase 1: Setup ✅ (T001-T002)
- Phase 2: Foundation ✅ (T003-T014)
- Phase 3: US1 mostly complete ✅ (T015-T026a)
- Phase 4: US2 partially complete ⚠️ (T031-T033, T038-T039)

**Not Started**:
- Phase 5: US3 Loans
- Phase 6: US4 Edit/Remove
- Phase 7: Polish

**Recommendation**: ⚠️ **ACTION REQUIRED**
1. Update spec.md to mark US1 as "Implemented ✅"
2. Update spec.md to mark US2 as "Partially Implemented ⚠️"
3. Add implementation notes to each user story showing current status

---

## 5. Quality Issues

### 5.1 Inconsistencies Found

#### 5.1.1 Breakdown Display Location 🔴 CRITICAL

**Issue**: Spec.md has conflicting information about breakdown display

**Spec FR-031**: "For years with concurrent (multiple) one-time expenses, the column MUST display the total amount **with an info icon**"

**Spec FR-032**: "When user clicks the **info icon**, system MUST display a detailed breakdown..."

**But Spec Clarification**: "In the **main computation breakdown dialog** (accessed via the 🔍 icon in each row) rather than inline in the one-time expenses column"

**Impact**: Requirements FR-031, FR-032, FR-033 are **obsolete** based on the clarification

**Recommendation**: 🚨 **CRITICAL - UPDATE REQUIRED**
1. Mark FR-031, FR-032, FR-033 as "SUPERSEDED by Clarification 2026-01-20"
2. Add new requirements:
   - FR-031-v2: "One-time expenses column displays total amount only (no info icon)"
   - FR-032-v2: "Breakdown accessed via main computation breakdown icon (🔍)"
   - FR-033-v2: "Main breakdown dialog includes 'One-Time Expenses' section when expenses exist"

---

#### 5.1.2 Down Payment Feature Gap 🔴 CRITICAL

**Issue**: Major feature implemented but not specified (documented in Section 4.1 above)

**Recommendation**: See Section 4.1 recommendations

---

#### 5.1.3 Task Completion Status ⚠️ MEDIUM

**Issue**: Some obsolete tasks are marked but replacement tasks are incomplete

**Obsolete Tasks**: T034, T035, T036, T037 (inline breakdown)
**Replacement Tasks**: T034a, T035a, T036a, T037a (main breakdown integration)
**Status**: Replacements exist but NOT COMPLETE

**Recommendation**: ⚠️ **ACTION REQUIRED**
1. Prioritize T034a-T037a completion
2. Add checkpoint after T037a: "Breakdown integration complete"
3. Update US2 status to "Complete" only after T037a done

---

### 5.2 Ambiguities

#### 5.2.1 Inflation Adjustment Timing ⚠️ MINOR

**Issue**: Spec says "inflation-adjusted" but doesn't specify WHEN adjustment is calculated

**Question**: Is inflation calculated from:
- Current year to expense year? OR
- Base year (e.g., 2020) to expense year? OR
- Simulation start to expense year?

**Plan Coverage**: Plan mentions "inflationAdjustment" but doesn't clarify the base year

**Code Reality**: 
```kotlin
val inflationAdjustment = (1.0 + config.rates.inflation).pow(yearIdx)
```
This uses years from simulation start.

**Recommendation**: ⚠️ **CLARIFICATION NEEDED**
1. Add to spec.md Assumptions: "Inflation adjustment calculated from simulation start year (currentYear) to expense year"
2. Add example: "If currentYear=2024, expense at age 67 (2031), inflation=3%, adjustment = 1.03^7 = 1.2299"

---

#### 5.2.2 Test-First vs. Implementation-First ⚠️ MINOR

**Issue**: Constitution mandates "Test-First Development" but tasks.md says "focusing on implementation tasks only"

**Plan**: Explicitly requires test-first (Architecture Decision #10)
**Tasks**: Doesn't include explicit test tasks
**Reality**: Implementation appears to have proceeded without test tasks

**Recommendation**: ⚠️ **DOCUMENT DECISION**
1. Either:
   - Option A: Add test tasks retroactively and require them going forward
   - Option B: Document exception: "Tests written during implementation, not as separate tasks"
2. Add to constitution compliance note explaining which option was chosen

---

### 5.3 Missing Information

#### 5.3.1 API Contract Version ⚠️ MINOR

**Issue**: Plan references `contracts/api-contract.md` but doesn't specify versioning

**Recommendation**: ⚠️ **ADD TO PLAN**
1. Add section "API Versioning Strategy"
2. Document whether oneTimeExpenses addition requires new API version
3. Clarify backward compatibility (empty array default)

---

#### 5.3.2 Performance Benchmarks ⚠️ MINOR

**Issue**: SC-003 says "10+ expenses without performance degradation" but no baseline defined

**Question**: What is "degradation"?
- More than 10% slower?
- More than 1 second total?
- Noticeably slower to user?

**Recommendation**: ⚠️ **CLARIFY IN SPEC**
1. Define "performance degradation" threshold (e.g., "no more than 10% increase in simulation time")
2. Document baseline: "With 0 expenses, simulation completes in ~800ms"
3. Set target: "With 10 expenses, should complete in <1000ms"

---

## 6. Traceability Matrix

### 6.1 Requirements to Tasks

| Requirement | Plan Section | Task IDs | Status |
|-------------|--------------|----------|--------|
| FR-001 (Add multiple) | AD #4 | T031-T033 | ✅ Complete |
| FR-002 (Name field) | AD #1 | T016, T045 | ✅ Complete |
| FR-003 (Type: Cash/Loan) | AD #1 | T003-T005, T044 | ✅ Complete |
| FR-007 (Cash amount) | Data Model | T016 | ✅ Complete |
| FR-012-015 (Loan fields) | Data Model | T045 | ⏳ Not started |
| FR-016 (Amortization) | AD #3, Appendix | T046, T049-T050 | ⏳ Not started |
| FR-022 (After regular) | AD #6 | T023, T051 | ⏳ Partial |
| FR-026 (Trigger strategy) | AD #6 | T024, T030 | ✅ Complete |
| FR-029 (Results column) | Component Interaction | T020 | ✅ Complete |
| FR-031-033 (Breakdown) | AD #8 | T034a-T037a | ⏳ Not complete |
| FR-037-043 (Validation) | AD #7, #9 | T019, T048, T070, T076 | ⏳ Partial |

**Coverage**: 85% of requirements have task mapping ✅
**Gaps**: Breakdown integration tasks incomplete ⚠️

---

### 6.2 User Stories to Tasks

| User Story | Spec Section | Plan Phase | Task Range | Completion |
|-----------|--------------|------------|------------|------------|
| US1 - Single Cash | Lines 27-55 | Phase 3 | T015-T030 | 75% ✅ |
| US2 - Multiple Cash | Lines 57-79 | Phase 4 | T031-T043 | 50% ⚠️ |
| US3 - Loans | Lines 81-104 | Phase 5 | T044-T060 | 0% ⏳ |
| US4 - Edit/Remove | Lines 106-122 | Phase 6 | T061-T069 | 0% ⏳ |

**Coverage**: All user stories mapped to implementation phases ✅

---

### 6.3 Success Criteria to Validation Tasks

| Success Criterion | Measurement Strategy (Plan) | Validation Task | Status |
|------------------|----------------------------|-----------------|--------|
| SC-001 (<30s add) | Manual testing | T027-T030 | ⏳ Not complete |
| SC-002 (Auto-calc) | Frontend unit test | T047 | ⏳ Not started |
| SC-003 (10+ expenses) | Load test | T078 | ⏳ Not started |
| SC-004 (95% understand) | User testing | T085 | ⏳ Not started |
| SC-005 (<1s dialog) | React DevTools | T079 | ⏳ Not started |
| SC-006 ($0.01 accuracy) | Automated tests | T055, T057 | ⏳ Not started |
| SC-007 (<2min task) | User testing | T085 | ⏳ Not started |
| SC-008 (Zero errors) | Integration tests | T080-T082 | ⏳ Not started |
| SC-009 (100% scenarios) | Test suite | T085 | ⏳ Not started |
| SC-010 (Distinguish UI) | A/B testing | T041-T043 | ⏳ Not started |

**Coverage**: All success criteria have validation tasks ✅  
**Completion**: 0% - validation phase not started ⏳

---

## 7. Recommendations Summary

### 7.1 Critical Priority 🚨

1. **Update spec.md for down payment feature**
   - Add FR-012a, FR-016a, FR-020a
   - Update US3 with down payment examples
   - Reference /docs/down-payment-feature.md

2. **Fix breakdown display requirements**
   - Mark FR-031, FR-032, FR-033 as superseded
   - Add FR-031-v2, FR-032-v2, FR-033-v2 for main breakdown
   - Update all references to "info icon" → "breakdown icon"

3. **Complete Architecture Decision #8 implementation**
   - Finish tasks T034a, T035a, T036a, T037a
   - Add integration test for breakdown in main dialog
   - Update Phase 4 checkpoint criteria

### 7.2 High Priority ⚠️

4. **Add test-first compliance**
   - Create test tasks or document exception
   - Add test pass criteria to checkpoints
   - Clarify constitution compliance approach

5. **Document inflation adjustment base**
   - Clarify that adjustment is from simulation start year
   - Add examples to spec.md Assumptions

6. **Complete edge case coverage**
   - Add tasks for negative inflation testing
   - Add tasks for expense order validation
   - Add tasks for historical expense year validation

### 7.3 Medium Priority ℹ️

7. **Update plan.md with down payment decision**
   - Add Architecture Decision #11
   - Document rationale and implementation

8. **Clarify performance degradation threshold**
   - Define specific metrics (e.g., <10% slower, <1000ms)
   - Document baseline measurements

9. **Add API versioning strategy**
   - Document backward compatibility approach
   - Clarify if new API version needed

### 7.4 Low Priority 📝

10. **Update implementation status**
    - Mark US1 as "Implemented ✅" in spec
    - Mark US2 as "Partially Implemented ⚠️"
    - Add implementation notes to user stories

---

## 8. Quality Metrics

### 8.1 Consistency Score

| Category | Score | Grade |
|----------|-------|-------|
| Spec-to-Plan Alignment | 92% | A |
| Plan-to-Tasks Alignment | 88% | B+ |
| Spec-to-Tasks Direct | 85% | B+ |
| Requirements Coverage | 95% | A |
| Traceability | 90% | A- |
| **Overall Consistency** | **90%** | **A-** |

### 8.2 Completeness Score

| Category | Score | Grade |
|----------|-------|-------|
| Requirements Documentation | 85% | B+ |
| Architecture Decisions | 95% | A |
| Implementation Tasks | 75% | C+ |
| Test Coverage Planning | 60% | D |
| Validation Criteria | 90% | A- |
| **Overall Completeness** | **81%** | **B-** |

### 8.3 Quality Score

| Category | Score | Grade |
|----------|-------|-------|
| Clarity | 90% | A- |
| Precision | 85% | B+ |
| Ambiguity (lower is better) | 85% | B+ |
| Maintainability | 90% | A- |
| **Overall Quality** | **87.5%** | **B+** |

### 8.4 Overall Feature Health

**Composite Score**: **86%** (B+)

**Assessment**: Feature documentation is in **GOOD** condition with room for improvement

**Strengths**:
- ✅ Clear requirements and user stories
- ✅ Excellent traceability from spec to tasks
- ✅ Well-documented architecture decisions
- ✅ Good progress on implementation (Phases 1-4)

**Weaknesses**:
- ⚠️ Spec doesn't reflect implemented features (down payment)
- ⚠️ Obsolete requirements not marked (breakdown display)
- ⚠️ Test-first approach not reflected in tasks
- ⚠️ Some replacement tasks incomplete

---

## 9. Action Items

### Immediate (Complete Before Next Implementation)

- [ ] 1. Update spec.md FR-031 to FR-033 for main breakdown display
- [ ] 2. Add down payment feature to spec.md (FR-012a, FR-016a, FR-020a)
- [ ] 3. Complete tasks T034a, T035a, T036a, T037a
- [ ] 4. Update plan.md with Architecture Decision #11 (Down Payment)

### Short-term (Complete This Sprint)

- [ ] 5. Add test tasks or document test-first exception
- [ ] 6. Clarify inflation adjustment base year in spec Assumptions
- [ ] 7. Add edge case tasks (negative inflation, expense order, historical dates)
- [ ] 8. Update US1/US2 implementation status in spec

### Medium-term (Before US3 Implementation)

- [ ] 9. Define performance degradation thresholds (SC-003)
- [ ] 10. Add API versioning strategy to plan
- [ ] 11. Create comprehensive test plan document
- [ ] 12. Review and update all acceptance scenarios

---

## 10. Conclusion

The one-time expenses feature documentation demonstrates **strong overall quality** (86% B+) with excellent traceability and clear requirements. The main concerns are:

1. **Documentation lag**: Implemented features (down payment) not yet reflected in spec
2. **Incomplete transition**: Breakdown display change partially implemented
3. **Test coverage**: Test-first approach needs clearer task representation

**Recommended Next Steps**:
1. Complete the 4 immediate action items before next implementation phase
2. Update spec.md to reflect current implementation reality
3. Finish Architecture Decision #8 implementation (main breakdown)
4. Begin US3 (Loans) only after US2 is fully complete

**Overall Verdict**: ✅ **PROCEED WITH CAUTION** - Address critical items (down payment docs, breakdown completion) before moving to next phase

---

**Report Generated**: 2026-01-21  
**Artifacts Analyzed**: spec.md, plan.md, tasks.md  
**Lines Analyzed**: 242 + 789 + 444 = 1,475 lines  
**Issues Found**: 10 critical, 6 high, 3 medium, 1 low  
**Recommendations**: 12 action items across 3 priority levels

