# Testing One-Time Expenses Breakdown

## Current Status
✅ All services are running:
- Frontend: http://localhost:5173
- Backend: http://localhost:8000  
- API Server: http://localhost:8090

## How to See the Breakdown

The one-time expenses breakdown **only appears when you have 2 or more expenses in the SAME year**.

### Test Steps:

1. **Open the app**: http://localhost:5173

2. **Add TWO expenses for the SAME year**:
   - Click "Add Expense" (now on the left side)
   - First expense:
     - Type: Cash
     - Name: "Car"
     - Amount: 30000
     - When: Age
     - Age: 67
   
   - Click "Add Expense" again
   - Second expense:
     - Type: Cash
     - Name: "Trip"
     - Amount: 20000
     - When: Age
     - Age: 67 (SAME as first expense)

3. **Fill in the rest of the simulation form** with basic values

4. **Click "Run Simulation"**

5. **Look at the results table** at age 67:
   - You should see the total amount in the "One-Time Exp" column (~$65,000 inflation-adjusted)
   - **There should be an info icon (ℹ️) next to the amount**
   - Click the info icon to see the breakdown dialog

## Why You Might Not See It

### Reason 1: Only One Expense
If you only have ONE expense in a year, there's no breakdown shown (no info icon). This is by design because there's nothing to break down.

### Reason 2: Expenses in Different Years
If your expenses are in DIFFERENT years (e.g., one at age 65, another at age 70), each year will show the amount but NO info icon.

### Reason 3: No Expenses Added
If you haven't added any expenses, the column will just show $0.

## What the Breakdown Shows

When you click the info icon (ℹ️), you'll see a dialog titled:
**"One-Time Expense Breakdown"**

It will list:
- **Car**: Cash Expense: $XX,XXX (inflation-adjusted)
- **Trip**: Cash Expense: $XX,XXX (inflation-adjusted)

## UI Changes Made

✅ **"Add Expense" button** is now on the LEFT side (with outlined variant)
✅ **Delete button** (trash icon) is now on the LEFT side of each expense (in red)
✅ Both buttons moved per your request

## Technical Details

The breakdown feature works as follows:

1. **Backend** (SimulationEngine.kt):
   - Tracks all expenses for each year in `yearOneTimeExpenseBreakdown` list
   - Only includes breakdown in result if `size >= 2`
   - Returns `null` for single expenses (no breakdown needed)

2. **Frontend** (ResultsTable.tsx):
   - Checks `yearlyRow.oneTimeExpensesBreakdown?.length >= 2`
   - Shows info icon only when condition is true
   - Dialog displays all expenses with names and amounts

## Example Scenario

**Setup**:
- Age 67: Car ($30k) + Trip ($20k)
- Age 70: Home Repair ($50k)

**Results Table**:
- Age 67: Shows ~$65k with ℹ️ icon (click to see Car + Trip breakdown)
- Age 70: Shows ~$65k with NO icon (only one expense, no breakdown needed)

## Troubleshooting

If you still don't see the breakdown:

1. **Check browser console** (F12) for any JavaScript errors
2. **Verify you added 2+ expenses for the SAME year**
3. **Refresh the page** after the services restarted
4. **Check the API response** in Network tab to see if breakdown data is present
5. **Make sure you're in "Yearly" view** (not Quarterly) - breakdown only shows in yearly view

---

**Services Running**: ✅  
**UI Updated**: ✅  
**Ready to Test**: ✅

Try adding 2 expenses at age 67 and you should see the info icon!

