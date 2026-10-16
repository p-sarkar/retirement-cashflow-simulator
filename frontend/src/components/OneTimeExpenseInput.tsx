import React from 'react';
import {
  TextField,
  Typography,
  Grid,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  ListItemText,
  Button,
  Box,
  IconButton,
  Paper,
  InputAdornment,
  Tooltip,
  Collapse,
  Chip,
  SelectChangeEvent
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import { OneTimeExpense, CashExpense, LoanExpense, YearOrAge } from '../types/simulation';
import {
  generateExpenseId,
  calculateQuarterlyPayment,
  formatCurrency,
  validateCashExpense,
  validateLoanExpense,
  ValidationError
} from '../utils/expenseUtils';

interface OneTimeExpenseInputProps {
  expenses: OneTimeExpense[];
  onExpensesChange: (expenses: OneTimeExpense[]) => void;
  onValidityChange?: (allValid: boolean) => void;  // false while any expense entry is incomplete or invalid
  currentAge: number;
  currentYear: number;
  endAge: number;  // expense timing must fall within the simulation (FR003-041)
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
  downPayment: string;
  // Timing fields
  timingType: TimingType;
  timingValue: string;
  // UI state
  expanded: boolean;
  touched: string[];  // fields the user has edited; errors show for these (or any field with a value)
}

type FieldName = 'name' | 'amount' | 'principal' | 'aprPercent' | 'termYears' | 'downPayment' | 'timingValue';

const EXPENSE_TYPE_HELP: Record<ExpenseType, { label: string; help: string }> = {
  CASH: {
    label: 'Cash',
    help: 'Lump sum paid from the Spend Bucket in Q1 of the chosen year, adjusted for inflation.'
  },
  LOAN: {
    label: 'Loan',
    help: 'Optional down payment in Q1 of the start year, then fixed quarterly payments for the term, amortized at the APR.'
  }
};

const createEmptyExpenseState = (): ExpenseFormState => ({
  id: generateExpenseId(),
  type: 'CASH',
  name: '',
  amount: '',
  principal: '',
  aprPercent: '',
  termYears: '',
  downPayment: '',
  timingType: 'AGE',
  timingValue: '',
  expanded: true,
  touched: []
});

const toNumber = (raw: string): number => parseFloat(raw) || 0;
const isBlank = (raw: string): boolean => raw.trim() === '';
const isWholeNumber = (raw: string): boolean => /^-?\d+$/.test(raw.trim());

// Display a raw numeric string with thousands separators ("50000" -> "50,000"); leave anything else as typed
const groupDigits = (raw: string): string => {
  if (!/^-?\d+(\.\d*)?$/.test(raw)) return raw;
  const [whole, fraction] = raw.split('.');
  const grouped = Number(whole).toLocaleString('en-US');
  return fraction === undefined ? grouped : `${grouped}.${fraction}`;
};

const formatWholeDollars = (amount: number): string =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(amount);

function timingOf(state: ExpenseFormState): YearOrAge {
  return state.timingType === 'AGE'
    ? { type: 'AGE', age: parseInt(state.timingValue) || 0 }
    : { type: 'YEAR', year: parseInt(state.timingValue) || 0 };
}

/**
 * Validate a form entry. Blank and malformed fields get their own messages ("Amount is required")
 * so they are not reported as out-of-range values ("Amount must be greater than zero").
 */
function validateState(state: ExpenseFormState, currentAge: number, currentYear: number, endAge: number): ValidationError[] {
  const timingField = state.type === 'CASH' ? 'yearOrAge' : 'startYearOrAge';
  const timingLabel = state.timingType === 'AGE' ? 'Age' : 'Year';
  const inputErrors: ValidationError[] = [];

  const required = (field: string, raw: string, label: string) => {
    if (isBlank(raw)) inputErrors.push({ field, message: `${label} is required` });
  };

  if (state.type === 'CASH') {
    required('amount', state.amount, 'Amount');
  } else {
    required('principal', state.principal, 'Principal');
    required('aprPercent', state.aprPercent, 'APR');
    required('termYears', state.termYears, 'Term');
    if (!isBlank(state.termYears) && !isWholeNumber(state.termYears)) {
      inputErrors.push({ field: 'termYears', message: 'Term must be a whole number of years' });
    }
  }
  required(timingField, state.timingValue, timingLabel);
  if (!isBlank(state.timingValue) && !isWholeNumber(state.timingValue)) {
    inputErrors.push({ field: timingField, message: `${timingLabel} must be a whole number` });
  }

  const rangeErrors = state.type === 'CASH'
    ? validateCashExpense(state.name, toNumber(state.amount), timingOf(state), currentAge, currentYear, endAge)
    : validateLoanExpense(
        state.name,
        toNumber(state.principal),
        toNumber(state.aprPercent),
        parseInt(state.termYears) || 0,
        timingOf(state),
        currentAge,
        currentYear,
        endAge,
        toNumber(state.downPayment)
      );

  const fieldsWithInputErrors = new Set(inputErrors.map(e => e.field));
  return [...inputErrors, ...rangeErrors.filter(e => !fieldsWithInputErrors.has(e.field))];
}

// Map a validation error field to the form field that displays it
const errorFieldToFormField = (field: string): FieldName =>
  field === 'yearOrAge' || field === 'startYearOrAge' ? 'timingValue' : field as FieldName;

const FIELD_LABELS: Record<FieldName, string> = {
  name: 'Name',
  amount: 'Amount',
  principal: 'Principal',
  aprPercent: 'APR',
  termYears: 'Term',
  downPayment: 'Down Payment',
  timingValue: 'Age'
};

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
      downPayment: '',
      timingType: cashExp.yearOrAge.type,
      timingValue: cashExp.yearOrAge.type === 'AGE'
        ? cashExp.yearOrAge.age.toString()
        : cashExp.yearOrAge.year.toString(),
      expanded: false,
      touched: []
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
      downPayment: (loanExp.downPayment || 0).toString(),
      timingType: loanExp.startYearOrAge.type,
      timingValue: loanExp.startYearOrAge.type === 'AGE'
        ? loanExp.startYearOrAge.age.toString()
        : loanExp.startYearOrAge.year.toString(),
      expanded: false,
      touched: []
    };
  }
}

const getQuarterlyPayment = (state: ExpenseFormState): number | null => {
  if (state.type !== 'LOAN') return null;
  const financedAmount = toNumber(state.principal) - toNumber(state.downPayment);
  const termYears = parseInt(state.termYears) || 0;
  if (financedAmount <= 0 || termYears <= 0) return null;
  return calculateQuarterlyPayment(financedAmount, toNumber(state.aprPercent), termYears);
};

// One-line description shown in the card header, e.g. "Loan · $50,000 ($10,000 down) · 5% · 5 yrs · from age 55"
function summarize(state: ExpenseFormState): string {
  const timing = isBlank(state.timingValue)
    ? null
    : state.timingType === 'AGE' ? `age ${state.timingValue}` : state.timingValue;

  if (state.type === 'CASH') {
    return [
      'Cash',
      !isBlank(state.amount) && formatWholeDollars(toNumber(state.amount)),
      timing && `at ${timing}`
    ].filter(Boolean).join(' · ');
  }

  const down = toNumber(state.downPayment);
  const payment = getQuarterlyPayment(state);
  return [
    'Loan',
    !isBlank(state.principal) &&
      `${formatWholeDollars(toNumber(state.principal))}${down > 0 ? ` (${formatWholeDollars(down)} down)` : ''}`,
    !isBlank(state.aprPercent) && `${state.aprPercent}%`,
    !isBlank(state.termYears) && `${state.termYears} yrs`,
    timing && `from ${timing}`,
    payment !== null && `est. ${formatCurrency(payment)}/qtr`
  ].filter(Boolean).join(' · ');
}

interface MoneyFieldProps {
  label: string;
  value: string;
  onChange: (raw: string) => void;
  error?: string;
  width: number;
}

/** Dollar input: shows "50,000" when idle and the raw number while editing. */
const MoneyField: React.FC<MoneyFieldProps> = ({ label, value, onChange, error, width }) => {
  const [focused, setFocused] = React.useState(false);
  return (
    <TextField
      size="small"
      label={label}
      value={focused ? value : groupDigits(value)}
      onFocus={() => setFocused(true)}
      onBlur={() => setFocused(false)}
      onChange={(e) => onChange(e.target.value.replace(/[,\s$]/g, ''))}
      error={!!error}
      helperText={error}
      inputProps={{ inputMode: 'decimal' }}
      InputProps={{
        startAdornment: <InputAdornment position="start">$</InputAdornment>
      }}
      sx={{ width }}
    />
  );
};

const OneTimeExpenseInput: React.FC<OneTimeExpenseInputProps> = ({
  expenses,
  onExpensesChange,
  onValidityChange,
  currentAge,
  currentYear,
  endAge
}) => {
  const [formStates, setFormStates] = React.useState<ExpenseFormState[]>(() =>
    expenses.map(expenseToFormState)
  );

  function formStateToExpense(state: ExpenseFormState): OneTimeExpense | null {
    if (validateState(state, currentAge, currentYear, endAge).length > 0) return null;
    const yearOrAge = timingOf(state);

    if (state.type === 'CASH') {
      return {
        type: 'CASH',
        id: state.id,
        name: state.name.trim(),
        amount: toNumber(state.amount),
        yearOrAge
      };
    }
    // quarterlyPayment is not sent: the API server computes the actual payment (FR003-016b)
    return {
      type: 'LOAN',
      id: state.id,
      name: state.name.trim(),
      principal: toNumber(state.principal),
      aprPercent: toNumber(state.aprPercent),
      termYears: parseInt(state.termYears) || 0,
      startYearOrAge: yearOrAge,
      downPayment: toNumber(state.downPayment)
    };
  }

  const updateExpenses = (newFormStates: ExpenseFormState[]) => {
    setFormStates(newFormStates);

    // Convert valid form states to expenses
    const validExpenses = newFormStates
      .map(formStateToExpense)
      .filter((exp): exp is OneTimeExpense => exp !== null);

    onExpensesChange(validExpenses);
    // Invalid entries are not sent, so the parent must block the run rather than silently drop them (FR003-043)
    onValidityChange?.(validExpenses.length === newFormStates.length);
  };

  // Timing validity depends on current age/year and End Age, so re-check every entry when they change
  const isFirstRender = React.useRef(true);
  React.useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    updateExpenses(formStates);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentAge, currentYear, endAge]);

  const handleAddExpense = () => {
    updateExpenses([...formStates, createEmptyExpenseState()]);
  };

  const handleRemoveExpense = (id: string) => {
    updateExpenses(formStates.filter(s => s.id !== id));
  };

  const handleFieldChange = (id: string, field: keyof ExpenseFormState, value: string) => {
    updateExpenses(formStates.map(state =>
      state.id === id
        ? { ...state, [field]: value, touched: state.touched.includes(field) ? state.touched : [...state.touched, field] }
        : state
    ));
  };

  const handleTypeChange = (id: string, event: SelectChangeEvent<ExpenseType>) => {
    handleFieldChange(id, 'type', event.target.value as ExpenseType);
  };

  const handleTimingTypeChange = (id: string, event: SelectChangeEvent<TimingType>) => {
    handleFieldChange(id, 'timingType', event.target.value as TimingType);
  };

  const setExpanded = (id: string | null, expanded: boolean) => {
    // UI-only: expenses and validity are unchanged
    setFormStates(states => states.map(s => (id === null || s.id === id ? { ...s, expanded } : s)));
  };

  const allExpanded = formStates.every(s => s.expanded);

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 1 }}>
        <Typography variant="subtitle2" fontWeight="bold">One-Time Expenses</Typography>
        <Tooltip
          title={
            <>
              <b>Cash:</b> {EXPENSE_TYPE_HELP.CASH.help}
              <br />
              <b>Loan:</b> {EXPENSE_TYPE_HELP.LOAN.help}
              <br />
              Amounts are in today's dollars.
            </>
          }
        >
          <InfoOutlinedIcon fontSize="small" color="action" aria-label="About one-time expense types" />
        </Tooltip>
      </Box>

      <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
        <Button
          size="small"
          startIcon={<AddIcon />}
          onClick={handleAddExpense}
          variant="outlined"
        >
          Add Expense
        </Button>
        {formStates.length > 1 && (
          <Button size="small" onClick={() => setExpanded(null, !allExpanded)}>
            {allExpanded ? 'Collapse all' : 'Expand all'}
          </Button>
        )}
      </Box>

      {formStates.length === 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          No one-time expenses added. Click "Add Expense" to model major purchases or loans.
        </Typography>
      )}

      {formStates.map((state) => {
        const errors = validateState(state, currentAge, currentYear, endAge);
        const fieldError = (field: FieldName): string | undefined => {
          const shown = state.touched.includes(field) || !isBlank(String(state[field]));
          if (!shown) return undefined;
          return errors.find(e => errorFieldToFormField(e.field) === field)?.message;
        };
        // Errors on fields the user hasn't reached yet are summarized instead of shown in red
        const errorFields = new Set(errors.map(e => errorFieldToFormField(e.field)));
        const pendingFields = (Object.keys(FIELD_LABELS) as FieldName[])  // in form order
          .filter(field => errorFields.has(field) && !fieldError(field));
        const invalid = errors.length > 0;
        // An invalid entry stays open so its errors are visible
        const open = state.expanded || invalid;
        const quarterlyPayment = getQuarterlyPayment(state);
        const displayName = state.name.trim() || 'New expense';

        return (
          <Paper key={state.id} sx={{ p: 1.5, mb: 1.5 }} variant="outlined">
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <IconButton
                size="small"
                onClick={() => setExpanded(state.id, !state.expanded)}
                disabled={invalid}
                aria-label={`${open ? 'Collapse' : 'Expand'} ${displayName}`}
                aria-expanded={open}
              >
                <ExpandMoreIcon
                  fontSize="small"
                  sx={{ transform: open ? 'rotate(180deg)' : 'none', transition: 'transform 150ms' }}
                />
              </IconButton>
              <Typography variant="body2" fontWeight="medium">{displayName}</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ flex: 1 }} noWrap>
                {summarize(state)}
              </Typography>
              {invalid && <Chip size="small" color="error" variant="outlined" label="Needs attention" />}
              <IconButton
                size="small"
                onClick={() => handleRemoveExpense(state.id)}
                color="error"
                aria-label={`Remove ${displayName}`}
              >
                <DeleteIcon fontSize="small" />
              </IconButton>
            </Box>

            <Collapse in={open}>
              <Grid container spacing={1} sx={{ mt: 1.5, pl: 5 }}>
                {/* Row 1: Type, Name */}
                <Grid>
                  <Tooltip title={EXPENSE_TYPE_HELP[state.type].help} placement="top">
                    <FormControl size="small" sx={{ width: 100 }}>
                      <InputLabel id={`${state.id}-type-label`}>Type</InputLabel>
                      <Select
                        labelId={`${state.id}-type-label`}
                        value={state.type}
                        label="Type"
                        onChange={(e) => handleTypeChange(state.id, e)}
                        renderValue={(value) => EXPENSE_TYPE_HELP[value].label}
                      >
                        {(['CASH', 'LOAN'] as ExpenseType[]).map(type => (
                          <MenuItem key={type} value={type} sx={{ maxWidth: 360, whiteSpace: 'normal' }}>
                            <ListItemText primary={EXPENSE_TYPE_HELP[type].label} secondary={EXPENSE_TYPE_HELP[type].help} />
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  </Tooltip>
                </Grid>
                <Grid>
                  <TextField
                    size="small"
                    label="Name"
                    value={state.name}
                    onChange={(e) => handleFieldChange(state.id, 'name', e.target.value)}
                    error={!!fieldError('name')}
                    helperText={fieldError('name')}
                    sx={{ width: 180 }}
                  />
                </Grid>

                {/* Cash-specific fields */}
                {state.type === 'CASH' && (
                  <Grid>
                    <MoneyField
                      label="Amount"
                      value={state.amount}
                      onChange={(raw) => handleFieldChange(state.id, 'amount', raw)}
                      error={fieldError('amount')}
                      width={150}
                    />
                  </Grid>
                )}

                {/* Loan-specific fields */}
                {state.type === 'LOAN' && (
                  <>
                    <Grid>
                      <MoneyField
                        label="Principal"
                        value={state.principal}
                        onChange={(raw) => handleFieldChange(state.id, 'principal', raw)}
                        error={fieldError('principal')}
                        width={140}
                      />
                    </Grid>
                    <Grid>
                      <MoneyField
                        label="Down Payment"
                        value={state.downPayment}
                        onChange={(raw) => handleFieldChange(state.id, 'downPayment', raw)}
                        error={fieldError('downPayment')}
                        width={140}
                      />
                    </Grid>
                    <Grid>
                      <TextField
                        size="small"
                        type="number"
                        label="APR"
                        value={state.aprPercent}
                        onChange={(e) => handleFieldChange(state.id, 'aprPercent', e.target.value)}
                        error={!!fieldError('aprPercent')}
                        helperText={fieldError('aprPercent')}
                        inputProps={{ step: 0.01, min: 0, max: 99.99 }}
                        InputProps={{
                          endAdornment: <InputAdornment position="end">%</InputAdornment>
                        }}
                        sx={{ width: 110 }}
                      />
                    </Grid>
                    <Grid>
                      <TextField
                        size="small"
                        type="number"
                        label="Term"
                        value={state.termYears}
                        onChange={(e) => handleFieldChange(state.id, 'termYears', e.target.value)}
                        error={!!fieldError('termYears')}
                        helperText={fieldError('termYears')}
                        inputProps={{ step: 1, min: 1, max: 50 }}
                        InputProps={{
                          endAdornment: <InputAdornment position="end">yrs</InputAdornment>
                        }}
                        sx={{ width: 110 }}
                      />
                    </Grid>
                    {quarterlyPayment !== null && (
                      <Grid>
                        <TextField
                          size="small"
                          label="Est. Quarterly Payment"
                          value={formatCurrency(quarterlyPayment)}
                          InputProps={{ readOnly: true }}
                          helperText="Today's dollars. Simulated payment is fixed in start-year dollars (see results)."
                          FormHelperTextProps={{ sx: { mx: 0 } }}
                          sx={{ width: 220 }}
                        />
                      </Grid>
                    )}
                  </>
                )}

                {/* Timing fields */}
                <Grid>
                  <FormControl size="small" sx={{ width: 90 }}>
                    <InputLabel id={`${state.id}-when-label`}>When</InputLabel>
                    <Select
                      labelId={`${state.id}-when-label`}
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
                    error={!!fieldError('timingValue')}
                    helperText={fieldError('timingValue')}
                    inputProps={{ step: 1 }}
                    sx={{ width: 110 }}
                  />
                </Grid>
              </Grid>

              {pendingFields.length > 0 && (
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1, pl: 5 }}>
                  To include this expense, fill in: {pendingFields
                    .map(f => (f === 'timingValue' ? (state.timingType === 'AGE' ? 'Age' : 'Year') : FIELD_LABELS[f]))
                    .join(', ')}
                </Typography>
              )}
            </Collapse>
          </Paper>
        );
      })}
    </Box>
  );
};

export default OneTimeExpenseInput;
