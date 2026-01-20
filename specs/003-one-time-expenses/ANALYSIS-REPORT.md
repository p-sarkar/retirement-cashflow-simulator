# Cross-Artifact Analysis Report: One-Time Expenses

**Feature**: 003-one-time-expenses  
**Date**: 2026-01-20  
**Status**: 🚨 **CRITICAL ISSUES FOUND** - Implementation blocked  
**Analyzer**: speckit.analyze agent

---

## Executive Summary

A comprehensive analysis of spec.md, plan.md, and tasks.md has been completed. While the feature specification is well-structured with excellent user story organization and 85 granular tasks, **7 CRITICAL issues** and **12 HIGH priority issues** must be resolved before implementation can proceed.

**Overall Coverage**: 95% of requirements have task coverage (41/43), but the 5% gap includes critical calculation requirements.

---

## 🚨 CRITICAL Issues (Must Resolve Before Implementation)

### 1. **plan.md is Completely Empty**
- **Severity**: CRITICAL
- **Impact**: Missing required architecture documentation
- **Description**: The plan.md file exists but contains only whitespace. This is a required artifact that should document:
  - Architecture decisions
  - Data model design
  - Component interaction patterns
  - Tech stack decisions specific to this feature
  - API contracts between frontend/backend
- **Recommendation**: Generate plan.md using the speckit.plan agent before proceeding

### 2. **Constitution Violation: Test-First Development Not Followed**
- **Severity**: CRITICAL
- **Impact**: Violates project development principles
- **Description**: tasks.md explicitly states "Tests: Not explicitly requested in spec.md - focusing on implementation tasks only"
- **Evidence**: No test definition tasks in any of the 7 phases
- **Recommendation**: Add test tasks for each user story following Test-First Development approach

### 3. **Missing Test Definition Tasks Throughout All Phases**
- **Severity**: CRITICAL
- **Impact**: No testing strategy defined
- **Details**: While validation checkpoints exist at end of tasks.md, there are no tasks for:
  - Unit test creation
  - Integration test creation
  - E2E test scenarios
  - Test data setup
- **Recommendation**: Add test tasks to each phase (US1-US4)

### 4. **Requirements with Zero Task Coverage**
- **Severity**: CRITICAL
- **Impact**: Key functionality will not be implemented
- **Missing Coverage**:
  - **FR-027**: "Monthly payment calculated using standard amortization formula: M = P[r(1+r)^n]/[(1+r)^n-1]"
  - **FR-028**: "For 0% APR loans, monthly payment = principal / (term in months)"
- **Current State**: Tasks mention "amortization calculation" but don't specify which formula or how to handle 0% APR edge case
- **Recommendation**: Add explicit tasks for implementing both calculation formulas with unit tests

### 5. **Incomplete Validation Coverage for Requirements**
- **Severity**: CRITICAL
- **Impact**: Validation requirements may not be implemented
- **Gaps**: Requirements FR-037 through FR-043 (validation rules) have task coverage, but implementation details are vague
- **Example**: T019 says "Add client-side validation" but doesn't specify which validation rules from spec
- **Recommendation**: Map each validation requirement (FR-037-FR-043) to specific validation task items

### 6. **Unmeasurable Success Criteria**
- **Severity**: CRITICAL
- **Impact**: Cannot determine if feature is successful
- **Issues**:
  - **SC-001**: "95% of users can add and configure expenses without documentation" - No measurement methodology defined
  - **SC-004**: "Simulation completes within 30 seconds for 10 concurrent expenses" - No performance testing tasks
  - **SC-007**: "Loan monthly payment accurate to $0.01 vs external calculator" - No validation against external calculator in tasks
- **Recommendation**: Add tasks for measuring/validating each success criterion

### 7. **Execution Ordering Ambiguity for One-Time vs Regular Expenses**
- **Severity**: CRITICAL
- **Impact**: Core business logic may be implemented incorrectly
- **Issue**: Spec states "One-time expenses paid AFTER regular living expenses" but T023 "Integrate cash expense withdrawal into SB processing" doesn't specify execution order
- **Risk**: Implementation may pay expenses in wrong order, breaking the spending strategy
- **Recommendation**: Add explicit task to implement expense ordering logic with test cases

---

## ⚠️ HIGH Priority Issues

### 8. **Ambiguous Specification: "Full Monthly Payment" for Mid-Year Loans**
- **Severity**: HIGH
- **Description**: Clarification states "Always full monthly payment amount regardless of start timing" but this could mean:
  - Option A: Full annual payment (12 months) even if loan starts in December
  - Option B: Partial year payment (pro-rated based on start month)
- **Recommendation**: Clarify in spec.md and update affected tasks (T051-T053)

### 9. **Underspecified Edge Case: Loan Extending Beyond Simulation Period**
- **Severity**: HIGH
- **Description**: Spec says "Only payments within the simulation timeframe are shown" but doesn't specify:
  - Should UI warn user that loan won't be fully paid off?
  - Should breakdown show remaining principal after year 35?
  - Should validation prevent this scenario?
- **Recommendation**: Add clarification to spec.md and corresponding task for handling this case

### 10. **Terminology Drift: "SB" vs "Spend Bucket"**
- **Severity**: HIGH (Quality)
- **Description**: Spec uses "Spend Bucket (SB)" but tasks use both "SB" and "Spend Bucket" inconsistently
- **Impact**: May cause confusion during implementation
- **Recommendation**: Standardize on one term throughout tasks.md

### 11. **Missing Task Coverage for Conversion Utilities**
- **Severity**: HIGH
- **Description**: T018 says "Implement age-to-year and year-to-age conversion utilities" but spec doesn't define:
  - Input: Current age + target age → output year?
  - Input: Current age + current year + target year → validate feasibility?
  - What if user is currently age 65 in 2026 and enters "age 60" (past)?
- **Recommendation**: Add validation task for historical age/year entries

### 12. **Incomplete Breakdown Data Model**
- **Severity**: HIGH
- **Description**: T008/T014 add "oneTimeExpensesBreakdown" but don't specify:
  - Data structure (Map<String, Double>? List<ExpenseDetail>?)
  - Key format (expense name? expense ID?)
  - How to handle duplicate expense names
- **Recommendation**: Add task to define breakdown data model in plan.md

### 13. **No Rollback/Undo Strategy for Edit Operations**
- **Severity**: HIGH (UX)
- **Description**: US4 covers edit/remove but doesn't specify:
  - Can user undo a removal?
  - Are changes applied immediately or on form submission?
  - Is there a "Reset to Original" option?
- **Recommendation**: Add UX decision to plan.md and corresponding tasks if needed

### 14. **Missing Validation for Concurrent Task Execution**
- **Severity**: HIGH
- **Description**: 15 tasks marked [P] for parallel execution, but no verification that file changes won't conflict
- **Example**: T009-T014 all modify /frontend/src/types/simulation.ts
- **Recommendation**: Review [P] markings; T009-T014 should be sequential, not parallel

### 15. **Annual Income Gap (AIG) Calculation Update Underspecified**
- **Severity**: HIGH
- **Description**: T026 says "Include oneTimeExpenses in Annual Income Gap (AIG) calculation" but spec doesn't define if:
  - One-time expenses increase the gap (need more money)
  - One-time expenses are separate from regular expenses in AIG calculation
  - AIG formula needs adjustment
- **Recommendation**: Add clarification to spec.md about AIG impact

### 16. **No Task for Expense List Persistence**
- **Severity**: HIGH
- **Description**: Tasks don't specify:
  - Where expense list is stored (form state? local storage? database?)
  - Does expense list persist across page refreshes?
  - Are expenses part of saved simulation configs?
- **Recommendation**: Add task for persistence strategy in plan.md

### 17. **Missing Input Sanitization Tasks**
- **Severity**: HIGH (Security)
- **Description**: T019 covers validation but not sanitization:
  - Are HTML/script tags in expense names sanitized?
  - Are special characters escaped?
  - SQL injection prevention (if stored in database)?
- **Recommendation**: Add sanitization tasks to both frontend and API server

### 18. **Incomplete Error Handling Strategy**
- **Severity**: HIGH
- **Description**: No tasks for:
  - Backend error responses (400 vs 422 vs 500)
  - Frontend error display patterns
  - Network failure handling
  - Partial failure scenarios (some expenses succeed, some fail)
- **Recommendation**: Add error handling tasks to each phase

### 19. **No Performance Testing Tasks for Success Criterion SC-004**
- **Severity**: HIGH
- **Description**: SC-004 requires "Simulation completes within 30 seconds for 10 concurrent expenses" but no task validates this
- **Recommendation**: Add performance test task with 10+ expense scenario

---

## 📊 Coverage Analysis

### Requirements Coverage
- **Total Requirements**: 43 functional requirements (FR-001 to FR-043)
- **Covered by Tasks**: 41 requirements (95%)
- **Missing Coverage**: 2 requirements (FR-027, FR-028) - both calculation formulas
- **Partial Coverage**: 7 requirements (FR-037-FR-043) - validation rules mentioned but not detailed

### User Story Coverage
- **US1 (Single Cash Expense)**: ✅ Excellent coverage (16 tasks)
- **US2 (Multiple Cash Expenses)**: ✅ Good coverage (13 tasks)
- **US3 (Loan with Amortization)**: ⚠️ Adequate coverage but missing formula details (17 tasks)
- **US4 (Edit & Remove)**: ⚠️ Basic coverage, missing undo/rollback (9 tasks)

### Edge Case Coverage
- **Covered**: 5/7 edge cases
- **Partially Covered**: 1 edge case (loan beyond simulation period)
- **Not Covered**: 1 edge case (historical age/year entries)

### Success Criteria Coverage
- **Measurable**: 7/10 success criteria
- **Unmeasurable**: 3 success criteria (SC-001, SC-004, SC-007)

---

## 📈 Quality Metrics

### Specification Quality
- ✅ User scenarios well-defined (4 stories, 19 acceptance scenarios)
- ✅ Priorities clearly assigned (P1, P2, P3)
- ✅ Edge cases identified
- ⚠️ Some ambiguities remain (mid-year loan payments, breakdown data model)

### Task Quality
- ✅ 85 granular, actionable tasks
- ✅ Dependency ordering generally correct
- ✅ File paths specified for each task
- ⚠️ Some tasks too vague (e.g., "Add client-side validation")
- ⚠️ Parallel execution markers may have conflicts
- ❌ No test tasks defined

### Phase Organization
- ✅ 7 well-structured phases
- ✅ MVP clearly identified (Phase 1+2+3)
- ✅ Checkpoints after each user story
- ⚠️ Foundation phase may be too large (12 blocking tasks)

---

## 🎯 Recommendations

### Immediate Actions (Before Implementation)

1. **Generate plan.md** using speckit.plan agent
   - Define data models (OneTimeExpense, breakdown structure)
   - Document API contracts
   - Specify component interactions
   - Make architecture decisions

2. **Add Test Tasks** to each phase
   - Phase 2: Unit tests for models and utilities
   - Phase 3-6: Test tasks for each user story
   - Phase 7: Integration and E2E tests
   - Add performance test for SC-004

3. **Add Missing Calculation Tasks**
   - T-NEW-1: Implement amortization formula (FR-027)
   - T-NEW-2: Implement 0% APR calculation (FR-028)
   - T-NEW-3: Unit test both formulas against external calculator (SC-007)

4. **Clarify Ambiguous Requirements**
   - Mid-year loan payment handling
   - Loan extending beyond simulation period
   - Age-to-year conversion for historical dates
   - Breakdown data model structure

5. **Map Validation Requirements to Tasks**
   - Update T019 to reference specific validation rules (FR-037-FR-043)
   - Add validation unit tests

6. **Add Measurement Tasks for Success Criteria**
   - SC-001: User testing task
   - SC-004: Performance benchmark task
   - SC-007: External calculator validation task

### Quality Improvements

7. **Standardize Terminology**
   - Use "Spend Bucket" consistently (avoid "SB" abbreviation)
   - Define all acronyms on first use

8. **Review Parallel Execution**
   - Mark T009-T014 as sequential (same file)
   - Verify other [P] tasks don't conflict

9. **Add Error Handling Strategy**
   - Define error response codes
   - Document error display patterns
   - Add error handling tasks

10. **Add Persistence Strategy**
    - Document where expense list is stored
    - Add persistence implementation tasks

---

## ✅ Strengths

Despite the issues, this feature has many strengths:

1. **Excellent User Story Organization**: 4 prioritized stories with clear acceptance scenarios
2. **Granular Task Breakdown**: 85 actionable tasks with specific file paths
3. **Clear MVP Definition**: Phase 1+2+3 delivers immediate value
4. **Good Dependency Ordering**: Foundation → US1 → US2 → US3 → US4 flow makes sense
5. **Comprehensive Edge Cases**: Spec identifies 7 edge cases upfront
6. **Technology-Agnostic Spec**: Spec.md properly focuses on WHAT, not HOW
7. **Detailed Acceptance Scenarios**: Given/When/Then format for all user stories

---

## 🚦 Go/No-Go Decision

**Recommendation**: 🛑 **NO-GO**

Do not proceed with implementation until:

1. ✅ plan.md is created with architecture and data models
2. ✅ Test tasks are added to tasks.md (Test-First Development)
3. ✅ Missing calculation tasks (FR-027, FR-028) are added
4. ✅ Ambiguous requirements are clarified
5. ✅ Success criteria measurement tasks are added

**Estimated Remediation Time**: 2-4 hours

**Next Steps**:
1. Run `speckit.plan` agent to generate plan.md
2. Manually add test tasks to tasks.md OR regenerate tasks.md with test requirements
3. Clarify ambiguous requirements in spec.md
4. Run `speckit.analyze` again to validate fixes

---

## Appendix: Task-to-Requirement Mapping

### Phase 2 (Foundation)
- T003-T005: Cover FR-001, FR-002, FR-003, FR-004, FR-005
- T006: Covers FR-010
- T007-T008: Cover FR-034, FR-035
- T009-T014: Cover frontend type definitions

### Phase 3 (US1)
- T015-T020: Cover FR-011, FR-012, FR-013, FR-014, FR-033
- T021-T026: Cover FR-015, FR-016, FR-017, FR-018, FR-019, FR-020, FR-037-FR-043
- T027-T030: Validation tasks

### Phase 4 (US2)
- T031-T036: Cover FR-021, FR-022, FR-023
- T037-T039: Cover FR-034, FR-035
- T040-T043: Validation tasks

### Phase 5 (US3)
- T044-T060: Cover FR-006, FR-007, FR-008, FR-009, FR-024, FR-025, FR-026
- **Missing**: FR-027, FR-028 (calculation formulas)
- T061-T067: Validation tasks

### Phase 6 (US4)
- T068-T076: Cover FR-029, FR-030, FR-031, FR-032

### Phase 7 (Polish)
- T077-T085: General quality and edge cases

---

**End of Analysis Report**

