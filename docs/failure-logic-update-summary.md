# Simulation Failure Logic Update - Summary

**Date**: January 21, 2026  
**Build**: v1.1.20  
**Status**: ✅ COMPLETE

---

## What Was Changed

Updated the simulation failure detection logic in the Retirement Cash Flow Simulator so that a simulation is marked as **failed** only when **BOTH TBA and TDA go below $0**.

### Previous Logic (Overly Conservative)

```kotlin
if (balances.sb < -(grossExpenses / 12.0) || 
    balances.cbb < 0 || 
    balances.tba < 0 ||    // ← Failed if TBA alone went negative
    balances.tda < 0 ||    // ← Failed if TDA alone went negative
    balances.tfa < 0) {
    isFailure = true
}
```

### New Logic (More Realistic)

```kotlin
val bothEquitiesNegative = balances.tba < 0 && balances.tda < 0
if (balances.sb < -(grossExpenses / 12.0) || 
    balances.cbb < 0 || 
    bothEquitiesNegative ||  // ← Now requires BOTH to be negative
    balances.tfa < 0) {
    isFailure = true
}
```

---

## Why This Change?

1. **More Realistic**: In retirement, it's common for one equity account to deplete first while the other still has funds
2. **Flexible**: The spending strategy can draw from either TBA or TDA - as long as one has funds, the plan continues
3. **Accurate**: Provides better success rate metrics that reflect actual plan viability

---

## Impact

### Success Rates Will Increase

Simulations that previously failed when one equity account went negative (but the other still had funds) will now succeed until BOTH accounts are depleted.

**Example**:
- Year 20: TDA = -$50k, TBA = $200k → **Continues** (TBA available)
- Year 25: TDA = -$50k, TBA = -$10k → **Fails** (both depleted)

### No Breaking Changes

- Same API structure
- Same data models
- Just more accurate failure detection

---

## Files Modified

1. **SimulationEngine.kt** - Updated failure detection logic (lines 560-567)
2. **version.properties** - Incremented build 19 → 20
3. **docs/simulation-failure-logic-update.md** - Detailed documentation created

---

## Testing

### Automatic Verification
✅ No TypeScript/Kotlin compilation errors  
✅ Build version incremented  
✅ Code clearly commented  

### Manual Testing Needed
- [ ] Run simulation with aggressive spending
- [ ] Verify continues when one equity account goes negative
- [ ] Confirm fails when both go negative
- [ ] Check success rates improve appropriately

---

## Next Steps

1. Test the updated logic with various scenarios
2. Verify Monte Carlo success rates improve
3. Deploy to production when validated

---

**Status**: Implementation complete, ready for testing  
**Risk Level**: Low (isolated logic change, well-documented)  
**Rollback**: Simple if needed (revert single condition)

