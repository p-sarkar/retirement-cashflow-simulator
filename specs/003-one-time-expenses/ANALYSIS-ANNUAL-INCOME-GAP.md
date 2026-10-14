# Analysis: Annual Income Gap DOES Include One-Time Expenses

**Date**: January 23, 2026  
**Issue**: User reports Annual Income Gap doesn't appear to account for one-time expenses  
**Status**: ✅ CODE IS CORRECT - Investigation shows calculation is accurate

---

## Summary

The Annual Income Gap calculation **DOES correctly include one-time expenses**. This has been verified by code inspection of both backend calculation and frontend display logic.

---

## Code Verification

### Backend Calculation (SimulationEngine.kt)

**Lines 651-653**:
```kotlin
// Calculate actual AIG for this year from real expenses and passive income
// AIG = Total Expenses - Passive Income (including one-time expenses)
val totalExpenses = needsAdjusted + wantsAdjusted + healthcareAdjusted + 
                    propertyTaxAdjusted + annualTaxDue + annualOneTimeExpenses
val passiveIncome = annualInterest + annualDividends + annualSocialSecurity
val currentAig = totalExpenses - passiveIncome
```

✅ **Line 651**: `totalExpenses` includes `annualOneTimeExpenses`  
✅ **Line 653**: `currentAig` (Annual Income Gap) = totalExpenses - passiveIncome  
✅ **Therefore**: Annual Income Gap INCLUDES one-time expenses

### Stored in YearlyResult (Line 689)

```kotlin
metrics = Metrics(
    annualIncomeGap = currentAig,  // ← Includes one-time expenses
    incomeGapExpenses = totalExpenses,  // ← Also includes one-time expenses
    incomeGapPassiveIncome = passiveIncome,
    sbCap = currentCapAig * 2.0,
    cbbCap = calculateCbbCap(age),
    isFailure = isFailure
)
```

✅ `annualIncomeGap` is set to `currentAig` which includes one-time expenses  
✅ `incomeGapExpenses` is set to `totalExpenses` which includes one-time expenses

### Frontend Display (ResultsTable.tsx, Line 177)

```typescript
<TableCell sx={{ fontWeight: 'bold' }}>
    {formatMoney(row.metrics.annualIncomeGap)}
</TableCell>
```

✅ Frontend displays `row.metrics.annualIncomeGap` which includes one-time expenses

---

## Verification Example

### Scenario: Year with $50,000 One-Time Expense

**Inputs**:
- Needs: $40,000 (adjusted for inflation)
- Wants: $20,000 (adjusted for inflation)
- Healthcare: $5,000 (adjusted for inflation)
- Property Tax: $10,000 (adjusted for inflation)
- Income Tax: $15,000
- **One-Time Expenses**: $50,000 ← Cash expense or loan payment
- Interest: $2,000
- Dividends: $3,000
- Social Security: $25,000

**Calculation**:
```
totalExpenses = $40,000 + $20,000 + $5,000 + $10,000 + $15,000 + $50,000
              = $140,000

passiveIncome = $2,000 + $3,000 + $25,000
              = $30,000

annualIncomeGap = $140,000 - $30,000
                = $110,000  ← INCLUDES the $50,000 one-time expense
```

**Without One-Time Expense**:
```
totalExpenses = $90,000
passiveIncome = $30,000
annualIncomeGap = $60,000
```

**Difference**: $110,000 - $60,000 = $50,000 ← Exactly the one-time expense amount!

---

## Why the User Might Think It's Not Included

### Possible Reasons for Confusion

1. **Looking at the Wrong Metric**:
   - User might be looking at SB Cap instead of Income Gap
   - SB Cap calculation DOES include one-time expenses in currentCapAig (line 656)
   - But the displayed SB Cap might look different due to the 2× multiplier

2. **Quarterly vs Annual View**:
   - Quarterly Income Gap also includes one-time expenses (we just fixed this in the previous bug)
   - User might have been testing before the fix was deployed

3. **Expecting a Separate Column**:
   - User might expect to see "Income Gap (excluding one-time)" vs "Income Gap (including one-time)"
   - The system only shows one Income Gap that includes everything

4. **Testing Before Fix Deployed**:
   - If testing an older version, quarterly income gap was broken (just fixed)
   - Annual was always correct

5. **Visual Inspection Error**:
   - Large numbers can be hard to mentally verify
   - User might be looking at the wrong year or row

---

## How to Verify in the UI

### Manual Verification Steps

1. **Run simulation with one-time expense**
   - Add a $50,000 cash expense in a specific year
   - Note the year (e.g., 2038)

2. **Switch to Annual view**

3. **Find the year with the expense** (2038)

4. **Note the values**:
   - Needs: $______
   - Wants: $______
   - Healthcare: $______
   - Property Tax: $______
   - Income Tax: $______
   - **One-Time Exp**: $50,000 ✅
   - **Total Expenses**: Sum of all above (should include $50,000) ✅

5. **Note passive income**:
   - Interest: $______
   - Dividends: $______
   - Social Security: $______
   - **Total Passive**: Sum of above

6. **Verify Income Gap**:
   - **Expected**: Total Expenses - Total Passive Income
   - **Displayed**: Income Gap column value
   - **Should match!** ✅

### Example Verification

If you see:
```
Year 2038:
- Needs: $45,000
- Wants: $22,000
- Healthcare: $6,000
- Property Tax: $11,000
- Income Tax: $16,000
- One-Time Exp: $50,000
- Total Expenses: $150,000 ← Should include all above

- Interest: $2,500
- Dividends: $3,500
- Social Security: $28,000
- Total Passive: $34,000

Expected Income Gap: $150,000 - $34,000 = $116,000
Displayed Income Gap: $116,000 ← Should match!
```

If the displayed Income Gap is $116,000, then one-time expenses ARE included! ✅

If it shows $66,000 instead, that would indicate a bug (but our code review shows this shouldn't happen).

---

## Comparison: What DOES and DOESN'T Include One-Time Expenses

### DOES Include One-Time Expenses ✅

1. **Total Expenses** (cashFlow.totalExpenses)
   - Annual: ✅ Yes
   - Quarterly: ✅ Yes (fixed 2026-01-23)

2. **Annual Income Gap** (metrics.annualIncomeGap)
   - Annual: ✅ Yes (always worked)
   - Quarterly: ✅ Yes (fixed 2026-01-23)

3. **Income Gap Expenses** (metrics.incomeGapExpenses)
   - Annual: ✅ Yes
   - Quarterly: ✅ Yes (fixed 2026-01-23)

4. **Cap AIG** (for SB cap calculation)
   - Annual: ✅ Yes (line 656)
   - Uses formula: (needs + wants*0.5 + healthcare + tax + property tax + **oneTimeExpenses**) - passiveIncome

### Does NOT Include One-Time Expenses ❌

Currently, nothing explicitly excludes one-time expenses from calculations. All major calculations include them.

**Note**: This is correct behavior because:
- One-time expenses are real expenses that need funding
- They should affect the income gap
- They impact how much needs to be withdrawn from accounts

---

## Recommendation

### If User Still Sees Issue

1. **Provide Specific Example**:
   - Which year?
   - What are the actual displayed values?
   - What calculation did you perform manually?
   - What did you expect to see vs what you actually see?

2. **Check Server Version**:
   - Ensure api-server has been restarted with the latest fix
   - Build timestamp should be: 2026-01-23 09:38:42 or later

3. **Clear Browser Cache**:
   - Hard refresh (Cmd+Shift+R on Mac, Ctrl+Shift+R on Windows)
   - Clear application cache
   - Restart browser

4. **Verify Data**:
   - Open browser developer console
   - Run simulation
   - Check the API response for the YearlyResult
   - Verify `metrics.annualIncomeGap` value matches expectation

### If User Wants Different Behavior

If the user wants to see income gap **excluding** one-time expenses (for comparison), we would need to:
- Add a new metric: `baseIncomeGap` (without one-time expenses)
- Display both in the UI
- This would be a **new feature request**, not a bug

---

## Conclusion

**The code is correct**: Annual Income Gap DOES include one-time expenses as designed.

**Evidence**:
- ✅ Backend calculation includes `annualOneTimeExpenses` in totalExpenses
- ✅ Income gap calculated as totalExpenses - passiveIncome
- ✅ Stored correctly in Metrics
- ✅ Frontend displays the correct value
- ✅ Code comment explicitly states: "AIG = Total Expenses - Passive Income (including one-time expenses)"

**Next Steps**:
1. User should verify in UI using the manual verification steps above
2. If still seeing incorrect values, provide specific example with numbers
3. Ensure running latest build (2026-01-23 09:38:42 or later)

---

**Analyzed By**: GitHub Copilot  
**Date**: January 23, 2026  
**Conclusion**: ✅ CODE IS CORRECT - Annual Income Gap includes one-time expenses as designed  
**Status**: Awaiting user verification with specific example if issue persists

