# Specification Update: Breakdown Display Redesign

**Date**: January 21, 2026  
**Change Type**: Design Decision Update  
**Affected Documents**: spec.md, plan.md, tasks.md  
**Status**: Specification Updated - Implementation Pending

---

## Summary

Updated the one-time expenses feature specification to move the expense breakdown display from an inline info icon in the one-time expenses column to the main computation breakdown dialog (accessed via the 🔍 icon).

---

## What Changed

### 1. Specification (spec.md)

**Updated Acceptance Criteria**:

**User Story 2 (Multiple Cash Expenses)**:
- **OLD**: "When user clicks info icon next to one-time expenses amount, breakdown shows expense names"
- **NEW**: "When user clicks main computation breakdown icon (🔍), breakdown includes 'One-Time Expenses' section"

**User Story 3 (Loan with Amortization)**:
- **OLD**: "When user clicks info icon, breakdown shows loan and cash expenses"
- **NEW**: "When user clicks main computation breakdown icon (🔍), 'One-Time Expenses' section shows loans and cash"

**Added Clarification**:
- Q: Where should the detailed breakdown be displayed?
- A: In the main computation breakdown dialog rather than inline, providing unified location for all year-level details and avoiding UI clutter

### 2. Plan (plan.md)

**Added Design Decision #8**:

**Decision**: Display breakdown in main computation breakdown instead of inline

**Rationale**:
- ✅ Unified UX - all year details in one place
- ✅ Reduced clutter - no multiple info icons
- ✅ Consistency - follows existing pattern
- ✅ Discoverability - users know to click 🔍
- ✅ Extensibility - easy to add more expense details
- ✅ Mobile-friendly - single modal works better

**Implementation Approach**:
- Remove inline expense breakdown dialog and info icon
- Add "One-Time Expenses" section to BreakdownGenerator
- Show expense names, types (Cash/Loan), inflation-adjusted amounts
- Section hidden when no expenses

### 3. Tasks (tasks.md)

**Marked Obsolete** (3 tasks):
- [~] T034: Create ExpenseBreakdownDialog component
- [~] T035: Add info icon rendering logic
- [~] T036: Implement breakdown dialog handlers
- [~] T037: Add expense breakdown generation logic

**Added New Tasks** (4 tasks):
- [ ] T034a: Remove inline expense breakdown dialog from ResultsTable
- [ ] T035a: Remove inline info icon logic from one-time expenses column
- [ ] T036a: Update BreakdownDialog to display one-time expenses section
- [ ] T037a: Add one-time expenses section to BreakdownGenerator

**Updated Integration Tests**:
- T041: Test expenses visible in main breakdown (not inline dialog)
- T042: Verify section shows when expenses exist (not "no icon for single expense")
- T043: Verify names, types, and amounts display correctly

---

## Rationale for Change

### Problems with Inline Approach

1. **UI Clutter**: Multiple info icons in table (breakdown icon + expense icon)
2. **Inconsistency**: Other computation details in main breakdown, expenses separate
3. **Discoverability**: Users might not know to look for expense-specific icon
4. **Mobile UX**: Multiple modals harder to navigate on small screens
5. **Extensibility**: Hard to add expense type badges, principal/interest splits, etc.

### Benefits of Main Breakdown Approach

1. **Single Source of Truth**: All year-level computation in one dialog
2. **Better Information Architecture**: Expenses are part of computation, show them there
3. **Familiar Pattern**: Users already click 🔍 to understand a year
4. **Scalability**: Easy to add subsections for cash vs loans, payment schedules, etc.
5. **Cleaner Table**: One-time expenses column shows just the total (like other expense columns)

---

## Implementation Impact

### Backend Changes

**File**: `api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt`

**Change**: Add new section to `ComputationBreakdown`:

```kotlin
// Section: One-Time Expenses
if (yearlyResult.oneTimeExpensesBreakdown != null && 
    yearlyResult.oneTimeExpensesBreakdown.isNotEmpty()) {
    sections.add(BreakdownSection(
        title = "One-Time Expenses",
        items = yearlyResult.oneTimeExpensesBreakdown.map { expense ->
            BreakdownItem(
                label = "${expense.name} (${expense.type})",
                value = expense.amount,
                formula = "Inflation-adjusted from input"
            )
        },
        total = yearlyResult.cashFlow.oneTimeExpenses,
        explanation = "One-time expenses paid from Spend Bucket this year"
    ))
}
```

**No Changes Needed**:
- SimulationEngine.kt (already tracks breakdown in `oneTimeExpensesBreakdown`)
- SimulationResult.kt (already includes breakdown field)
- OneTimeExpense.kt (data models unchanged)

### Frontend Changes

**File**: `frontend/src/components/ResultsTable.tsx`

**Removals**:
- Delete `expenseBreakdownOpen` state
- Delete `selectedExpenseBreakdown` state
- Delete `handleExpenseBreakdownClick` function
- Remove inline info icon logic from one-time expenses cell
- Remove `<Dialog>` component for expense breakdown

**Changes**:
```typescript
// Before:
<TableCell>
  {formatMoney(row.cashFlow.oneTimeExpenses || 0)}
  {yearlyRow.oneTimeExpensesBreakdown?.length >= 2 && (
    <IconButton onClick={handleExpenseBreakdownClick(...)}>
      <InfoIcon />
    </IconButton>
  )}
</TableCell>

// After:
<TableCell>
  {formatMoney(row.cashFlow.oneTimeExpenses || 0)}
</TableCell>
```

**File**: `frontend/src/components/BreakdownDialog.tsx`

**Addition**: Display one-time expenses section when data exists

```typescript
{breakdown.sections.find(s => s.title === "One-Time Expenses") && (
  <Box>
    <Typography variant="h6">One-Time Expenses</Typography>
    <List>
      {section.items.map(item => (
        <ListItem>
          <ListItemText
            primary={item.label}
            secondary={formatMoney(item.value)}
          />
        </ListItem>
      ))}
    </List>
  </Box>
)}
```

---

## Migration Notes

### Current Implementation Status

**Already Implemented (Inline Approach)**:
- ✅ YearlyResult.oneTimeExpensesBreakdown field exists
- ✅ SimulationEngine populates breakdown data
- ✅ ResultsTable has inline dialog and info icon logic
- ✅ Breakdown shown when 2+ expenses in same year

**What Needs to Change**:
- Remove inline dialog from ResultsTable
- Remove info icon conditional rendering
- Add section to BreakdownGenerator
- Update BreakdownDialog to display section
- Update integration tests

### Testing Strategy

**Unit Tests**:
- BreakdownGenerator: Verify "One-Time Expenses" section generation
- BreakdownDialog: Verify section rendering

**Integration Tests**:
- Add 2+ expenses in same year
- Click main breakdown icon (🔍)
- Verify "One-Time Expenses" section appears
- Verify expense names, types, amounts are correct

**Manual Testing**:
1. Add multiple expenses at age 67
2. Run simulation
3. Click 🔍 icon in age 67 row
4. Scroll to "One-Time Expenses" section
5. Verify all expenses listed with correct data

---

## Next Steps

### Immediate Actions

1. ✅ **Update Specification** - COMPLETE
2. ✅ **Update Plan** - COMPLETE
3. ✅ **Update Tasks** - COMPLETE
4. ⏸️ **Implement Backend Changes** - Next step
5. ⏸️ **Implement Frontend Changes** - Next step
6. ⏸️ **Update Tests** - Next step
7. ⏸️ **Manual Validation** - Next step

### Implementation Order

**Phase 1: Backend** (Estimated: 30 minutes)
- Add one-time expenses section to BreakdownGenerator
- Update section generation logic
- Test with multiple expenses

**Phase 2: Frontend** (Estimated: 45 minutes)
- Remove inline breakdown dialog and state
- Remove info icon logic
- Update BreakdownDialog to display new section
- Test rendering

**Phase 3: Integration** (Estimated: 15 minutes)
- Update integration test expectations
- Manual testing across scenarios
- Documentation update

**Total Estimated Time**: ~90 minutes

---

## Files Modified

### Specification Documents
- ✅ `specs/003-one-time-expenses/spec.md`
- ✅ `specs/003-one-time-expenses/plan.md`
- ✅ `specs/003-one-time-expenses/tasks.md`

### Code Files (Pending)
- ⏸️ `api-server/src/main/kotlin/com/retirement/logic/BreakdownGenerator.kt`
- ⏸️ `frontend/src/components/ResultsTable.tsx`
- ⏸️ `frontend/src/components/BreakdownDialog.tsx`

### Test Files (Pending)
- ⏸️ Update integration test expectations
- ⏸️ Add new BreakdownGenerator tests

---

## Risk Assessment

**Low Risk** ✅:
- Backend change is additive (just new section)
- Frontend change is removal + reuse of existing dialog
- No data model changes
- No API contract changes

**Medium Risk** ⚠️:
- Need to verify BreakdownDialog can handle new section type
- Need to ensure proper null handling when no expenses

**High Risk** 🔴:
- None identified

---

## Conclusion

This specification update improves the user experience by consolidating all year-level computation details in one location (the main breakdown dialog) rather than scattering them across multiple UI elements. The change is well-scoped, low-risk, and aligns with existing UI patterns.

**Status**: ✅ Specification complete, ready for implementation

---

**Updated**: January 21, 2026  
**Next Review**: After implementation complete  
**Document Version**: 1.0

