import React from 'react';
import {
  TextField,
  Typography,
  Grid,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Box,
  IconButton,
  Paper,
  InputAdornment,
  SelectChangeEvent
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import { OneTimeExpense, CashExpense, LoanExpense, YearOrAge } from '../types/simulation';
import {
  generateExpenseId,
  calculateMonthlyPayment,
  formatCurrency,
  validateCashExpense,
  validateLoanExpense,
  ValidationError
} from '../utils/expenseUtils';

interface OneTimeExpenseInputProps {
  expenses: OneTimeExpense[];
  onExpensesChange: (expenses: OneTimeExpense[]) => void;
  currentAge: number;
  currentYear: number;
}

type TimingType = 'AGE' | 'YEAR';
type ExpenseType = 'CASH' | 'LOAN';

interface ExpenseFormState {
  id: string;
  type: ExpenseType;
  name: string;
  // Cash expense fields
  amount: string;
  // Loan expense fields
  principal: string;
  aprPercent: string;
  termYears: string;
  // Timing fields
  timingType: TimingType;
  timingValue: string;
  // Validation
  errors: ValidationError[];
}

const createEmptyExpenseState = (): ExpenseFormState => ({
  id: generateExpenseId(),
  type: 'CASH',
  name: '',
  amount: '',
  principal: '',
  aprPercent: '',
  termYears: '',
  timingType: 'AGE',
  timingValue: '',
  errors: []
});

const OneTimeExpenseInput: React.FC<OneTimeExpenseInputProps> = ({
  expenses,
  onExpensesChange,
  currentAge,
  currentYear
}) => {
  const [formStates, setFormStates] = React.useState<ExpenseFormState[]>(() => {
    // Initialize form states from existing expenses
    if (expenses.length === 0) {
      return [];
    }
    return expenses.map(exp => expenseToFormState(exp));
  });

  function expenseToFormState(expense: OneTimeExpense): ExpenseFormState {
    if (expense.type === 'CASH') {
      const cashExp = expense as CashExpense;
      return {
        id: expense.id,
        type: 'CASH',
        name: expense.name,
        amount: cashExp.amount.toString(),
        principal: '',
        aprPercent: '',
        termYears: '',
        timingType: cashExp.yearOrAge.type,
        timingValue: cashExp.yearOrAge.type === 'AGE'
          ? cashExp.yearOrAge.age.toString()
          : cashExp.yearOrAge.year.toString(),
        errors: []
      };
    } else {
      const loanExp = expense as LoanExpense;
      return {
        id: expense.id,
        type: 'LOAN',
        name: expense.name,
        amount: '',
        principal: loanExp.principal.toString(),
        aprPercent: loanExp.aprPercent.toString(),
        termYears: loanExp.termYears.toString(),
        timingType: loanExp.startYearOrAge.type,
        timingValue: loanExp.startYearOrAge.type === 'AGE'
          ? loanExp.startYearOrAge.age.toString()
          : loanExp.startYearOrAge.year.toString(),
        errors: []
      };
    }
  }

  function formStateToExpense(state: ExpenseFormState): OneTimeExpense | null {
    const yearOrAge: YearOrAge = state.timingType === 'AGE'
      ? { type: 'AGE', age: parseInt(state.timingValue) || 0 }
      : { type: 'YEAR', year: parseInt(state.timingValue) || 0 };

    if (state.type === 'CASH') {
      const amount = parseFloat(state.amount) || 0;
      const errors = validateCashExpense(state.name, amount, yearOrAge, currentAge, currentYear);
      if (errors.length > 0) return null;

      return {
        type: 'CASH',
        id: state.id,
        name: state.name.trim(),
        amount,
        yearOrAge
      };
    } else {
      const principal = parseFloat(state.principal) || 0;
      const aprPercent = parseFloat(state.aprPercent) || 0;
      const termYears = parseInt(state.termYears) || 0;
      const errors = validateLoanExpense(state.name, principal, aprPercent, termYears, yearOrAge, currentAge, currentYear);
      if (errors.length > 0) return null;

      const monthlyPayment = calculateMonthlyPayment(principal, aprPercent, termYears);

      return {
        type: 'LOAN',
        id: state.id,
        name: state.name.trim(),
        principal,
        aprPercent,
        termYears,
        startYearOrAge: yearOrAge,
        monthlyPayment
      };
    }
  }

  const updateExpenses = (newFormStates: ExpenseFormState[]) => {
    setFormStates(newFormStates);

    // Convert valid form states to expenses
    const validExpenses = newFormStates
      .map(formStateToExpense)
      .filter((exp): exp is OneTimeExpense => exp !== null);

    onExpensesChange(validExpenses);
  };

  const handleAddExpense = () => {
    const newState = createEmptyExpenseState();
    updateExpenses([...formStates, newState]);
  };

  const handleRemoveExpense = (id: string) => {
    updateExpenses(formStates.filter(s => s.id !== id));
  };

  const handleFieldChange = (id: string, field: keyof ExpenseFormState, value: string) => {
    const newStates = formStates.map(state => {
      if (state.id === id) {
        const updatedState = { ...state, [field]: value };

        // Validate on change
        if (updatedState.type === 'CASH') {
          const yearOrAge: YearOrAge = updatedState.timingType === 'AGE'
            ? { type: 'AGE', age: parseInt(updatedState.timingValue) || 0 }
            : { type: 'YEAR', year: parseInt(updatedState.timingValue) || 0 };
          updatedState.errors = validateCashExpense(
            updatedState.name,
            parseFloat(updatedState.amount) || 0,
            yearOrAge,
            currentAge,
            currentYear
          );
        } else {
          const yearOrAge: YearOrAge = updatedState.timingType === 'AGE'
            ? { type: 'AGE', age: parseInt(updatedState.timingValue) || 0 }
            : { type: 'YEAR', year: parseInt(updatedState.timingValue) || 0 };
          updatedState.errors = validateLoanExpense(
            updatedState.name,
            parseFloat(updatedState.principal) || 0,
            parseFloat(updatedState.aprPercent) || 0,
            parseInt(updatedState.termYears) || 0,
            yearOrAge,
            currentAge,
            currentYear
          );
        }

        return updatedState;
      }
      return state;
    });
    updateExpenses(newStates);
  };

  const handleTypeChange = (id: string, event: SelectChangeEvent<ExpenseType>) => {
    const newType = event.target.value as ExpenseType;
    handleFieldChange(id, 'type', newType);
  };

  const handleTimingTypeChange = (id: string, event: SelectChangeEvent<TimingType>) => {
    const newTimingType = event.target.value as TimingType;
    handleFieldChange(id, 'timingType', newTimingType);
  };

  const getFieldError = (state: ExpenseFormState, field: string): string | undefined => {
    const error = state.errors.find(e => e.field === field);
    return error?.message;
  };

  const getMonthlyPaymentDisplay = (state: ExpenseFormState): string => {
    if (state.type !== 'LOAN') return '';
    const principal = parseFloat(state.principal) || 0;
    const aprPercent = parseFloat(state.aprPercent) || 0;
    const termYears = parseInt(state.termYears) || 0;
    if (principal <= 0 || termYears <= 0) return '';
    const payment = calculateMonthlyPayment(principal, aprPercent, termYears);
    return formatCurrency(payment);
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
        <Typography variant="subtitle2" fontWeight="bold">One-Time Expenses</Typography>
        <Button
          size="small"
          startIcon={<AddIcon />}
          onClick={handleAddExpense}
        >
          Add Expense
        </Button>
      </Box>

      {formStates.length === 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          No one-time expenses added. Click "Add Expense" to model major purchases or loans.
        </Typography>
      )}

      {formStates.map((state, _index) => (
        <Paper key={state.id} sx={{ p: 1.5, mb: 1.5 }} variant="outlined">
          <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
            <Grid container spacing={1} sx={{ flex: 1 }}>
              {/* Row 1: Type, Name */}
              <Grid>
                <FormControl size="small" sx={{ width: 100 }}>
                  <InputLabel>Type</InputLabel>
                  <Select
                    value={state.type}
                    label="Type"
                    onChange={(e) => handleTypeChange(state.id, e)}
                  >
                    <MenuItem value="CASH">Cash</MenuItem>
                    <MenuItem value="LOAN">Loan</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid>
                <TextField
                  size="small"
                  label="Name"
                  value={state.name}
                  onChange={(e) => handleFieldChange(state.id, 'name', e.target.value)}
                  error={!!getFieldError(state, 'name')}
                  helperText={getFieldError(state, 'name')}
                  sx={{ width: 180 }}
                />
              </Grid>

              {/* Cash-specific fields */}
              {state.type === 'CASH' && (
                <Grid>
                  <TextField
                    size="small"
                    type="number"
                    label="Amount"
                    value={state.amount}
                    onChange={(e) => handleFieldChange(state.id, 'amount', e.target.value)}
                    error={!!getFieldError(state, 'amount')}
                    helperText={getFieldError(state, 'amount')}
                    InputProps={{
                      startAdornment: <InputAdornment position="start">$</InputAdornment>
                    }}
                    sx={{ width: 150 }}
                  />
                </Grid>
              )}

              {/* Loan-specific fields */}
              {state.type === 'LOAN' && (
                <>
                  <Grid>
                    <TextField
                      size="small"
                      type="number"
                      label="Principal"
                      value={state.principal}
                      onChange={(e) => handleFieldChange(state.id, 'principal', e.target.value)}
                      error={!!getFieldError(state, 'principal')}
                      helperText={getFieldError(state, 'principal')}
                      InputProps={{
                        startAdornment: <InputAdornment position="start">$</InputAdornment>
                      }}
                      sx={{ width: 140 }}
                    />
                  </Grid>
                  <Grid>
                    <TextField
                      size="small"
                      type="number"
                      label="APR %"
                      value={state.aprPercent}
                      onChange={(e) => handleFieldChange(state.id, 'aprPercent', e.target.value)}
                      error={!!getFieldError(state, 'aprPercent')}
                      helperText={getFieldError(state, 'aprPercent')}
                      inputProps={{ step: 0.01 }}
                      sx={{ width: 90 }}
                    />
                  </Grid>
                  <Grid>
                    <TextField
                      size="small"
                      type="number"
                      label="Term (yrs)"
                      value={state.termYears}
                      onChange={(e) => handleFieldChange(state.id, 'termYears', e.target.value)}
                      error={!!getFieldError(state, 'termYears')}
                      helperText={getFieldError(state, 'termYears')}
                      sx={{ width: 100 }}
                    />
                  </Grid>
                  {getMonthlyPaymentDisplay(state) && (
                    <Grid>
                      <TextField
                        size="small"
                        label="Monthly Payment"
                        value={getMonthlyPaymentDisplay(state)}
                        InputProps={{ readOnly: true }}
                        sx={{ width: 130 }}
                      />
                    </Grid>
                  )}
                </>
              )}

              {/* Timing fields */}
              <Grid>
                <FormControl size="small" sx={{ width: 90 }}>
                  <InputLabel>When</InputLabel>
                  <Select
                    value={state.timingType}
                    label="When"
                    onChange={(e) => handleTimingTypeChange(state.id, e)}
                  >
                    <MenuItem value="AGE">Age</MenuItem>
                    <MenuItem value="YEAR">Year</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid>
                <TextField
                  size="small"
                  type="number"
                  label={state.timingType === 'AGE' ? 'Age' : 'Year'}
                  value={state.timingValue}
                  onChange={(e) => handleFieldChange(state.id, 'timingValue', e.target.value)}
                  error={!!getFieldError(state, 'yearOrAge') || !!getFieldError(state, 'startYearOrAge')}
                  helperText={getFieldError(state, 'yearOrAge') || getFieldError(state, 'startYearOrAge')}
                  sx={{ width: 90 }}
                />
              </Grid>
            </Grid>

            {/* Delete button */}
            <IconButton
              size="small"
              onClick={() => handleRemoveExpense(state.id)}
              sx={{ mt: 0.5 }}
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Box>
        </Paper>
      ))}
    </Box>
  );
};

export default OneTimeExpenseInput;

