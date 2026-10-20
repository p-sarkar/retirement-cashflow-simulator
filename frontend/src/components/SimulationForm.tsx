import React, { useRef, useState } from 'react';
import { 
  Button, 
  TextField, 
  Typography, 
  Grid, 
  Paper,
  CircularProgress,
  MenuItem,
  Box,
  Stack
} from '@mui/material';
import { SimulationConfig, OneTimeExpense, MarketPreset, SpendingStrategyInfo, ApiValidationError } from '../types/simulation';
import OneTimeExpenseInput from './OneTimeExpenseInput';
import { validateEndAge } from '../utils/expenseUtils';
import { applyPreset, FormConfig, MARKET_RATE_FIELDS, toApiConfig, toFormConfig } from '../utils/formConfig';
import InputsFileButtons, { InputsFileProblem } from './InputsFileButtons';

interface SimulationFormProps {
  onSubmit: (config: SimulationConfig) => void;
  running?: boolean;  // a simulation request is in flight
  initialConfig?: SimulationConfig;  // e.g. a loaded saved simulation; the form starts from its values
  presets?: MarketPreset[];  // market condition presets (FR002-003); none hides the selector
  onRunMonteCarlo?: (config: SimulationConfig) => void;  // shows the Run Monte Carlo button
  monteCarloRunning?: boolean;
  strategies?: SpendingStrategyInfo[];  // spending strategy catalog (spec 006); none offers the existing strategy only
  fieldErrors?: ApiValidationError[];  // the last 422's errors, shown on the fields they name (FR006-010)
  onImportInputs?: (config: SimulationConfig) => void;  // shows Export / Import inputs (US005-3)
  onInputsFileError?: (problem: InputsFileProblem) => void;
}

const CUSTOM_MARKET = 'custom';

const EXISTING_STRATEGY: SpendingStrategyInfo = {
  id: 'PARTHA_V0_01_20250105',
  name: "Partha's Spending Strategy-v0.01-20260105",
  description: 'Fixed spending; a Spend Bucket and Crash Buffer decide which accounts pay.',
  settings: 'NONE'
};

/** The guardrail settings fields (spec 006, FR006-009), as whole-number percentages in the form. */
const GUARDRAIL_PERCENT_FIELDS = [
  { field: 'targetSuccess', label: 'Target Success (%)' },
  { field: 'upperGuardrail', label: 'Upper Guardrail (%)' },
  { field: 'lowerGuardrail', label: 'Lower Guardrail (%)' }
] as const;

const defaultConfig: FormConfig = {
  name: "My Retirement Plan",
  currentYear: new Date().getFullYear(),
  currentAge: 51,
  retirementAge: 55,
  endAge: 85,
  salary: 230000,
  portfolio: {
    sb:   200000,
    cbb:  400000,
    tba: 1215507,
    tda: 1735185,
    tfa:  428470
  },
  spousal: {
    spouseAge: 49,
    lowerEarner: { claimAge: 62, annualBenefit: 20000 },
    higherEarner: { claimAge: 70, annualBenefit: 40000 }
  },
  expenses: {
    needs: 50000,
    wants: 90000,
    propertyTax: 12000,
    healthcarePreRetirement: 5000,
    healthcarePostRetirementPreMedicare: 24000,
    healthcareMedicare: 3000
  },
  contributions: {
    annual401k: 0,
    annualRoth: 5000, // Roth contribution: Roth 401k and/or Roth IRA (post-tax, goes to TFA)
    annualTba: 5000
  },
  rates: {
    inflation: 3, // Stored as whole number percentage
    preRetirementGrowth: 8,
    postRetirementGrowth: 8,
    bondYield: 5,
    hysaRate: 4,
    incomeTax: 12
  },
  strategy: {
    initialTdaWithdrawal: 40000, // Deprecated
    tdaWithdrawalPercentage: 60, // Stored as whole number in UI (100 = 100%), converted to 1.0 on submit
    rothConversionAmount: 0, // Deprecated
    rothConversionPreRetirement: 10000, // Pre-retirement Roth conversion
    rothConversionPostRetirement: 20000, // Post-retirement Roth conversion
    type: "PARTHA_V0_01_20250105",
    // Risk-Based Guardrails (spec 006), kept while another strategy is selected (FR006-004)
    guardrails: { targetSuccess: 90, upperGuardrail: 99, lowerGuardrail: 70, reviewFrequency: 'YEARLY' }
  },
  marketPresetId: null, // custom market conditions
  oneTimeExpenses: [
    {
      id: 'partha-car-loan-1',
      type: 'LOAN',
      name: "Partha's car 1",
      principal: 50000,
      downPayment: 10000,
      aprPercent: 5,
      termYears: 5,
      startYearOrAge: { type: 'AGE', age: 55 }
    },
    {
      id: 'mou-car-loan-1',
      type: 'LOAN',
      name: "Mou's car 1",
      principal: 50000,
      downPayment: 10000,
      aprPercent: 5,
      termYears: 5,
      startYearOrAge: { type: 'AGE', age: 56 }
    },
    {
      id: 'home-maintenance-loan-2',
      type: 'LOAN',
      name: 'New HVAC',
      principal: 20000,
      aprPercent: 5,
      termYears: 5,
      startYearOrAge: { type: 'YEAR', year: 2032 }
    },
    {
      id: 'default-cash-1',
      type: 'CASH',
      name: "Daughter's wedding",
      amount: 100000,
      yearOrAge: { type: 'AGE', age: 60 }
    },
    {
      id: 'home-maintenance-loan-1',
      type: 'LOAN',
      name: 'New roof',
      principal: 25000,
      aprPercent: 5,
      termYears: 5,
      startYearOrAge: { type: 'YEAR', year: 2035 }
    },
    {
      id: 'partha-car-loan-2',
      type: 'LOAN',
      name: "Partha's car 2",
      principal: 50000,
      downPayment: 10000,
      aprPercent: 5,
      termYears: 5,
      startYearOrAge: { type: 'AGE', age: 65 }
    },
    {
      id: 'mou-car-loan-2',
      type: 'LOAN',
      name: "Mou's car 2",
      principal: 50000,
      downPayment: 10000,
      aprPercent: 5,
      termYears: 5,
      startYearOrAge: { type: 'AGE', age: 68 }
    },
  ] as OneTimeExpense[]
};

const SimulationForm: React.FC<SimulationFormProps> = ({
  onSubmit, running = false, initialConfig, presets = [], onRunMonteCarlo, monteCarloRunning = false,
  strategies = [], fieldErrors = [], onImportInputs, onInputsFileError
}) => {
  const [config, setConfig] = useState<FormConfig>(() => (initialConfig ? toFormConfig(initialConfig) : defaultConfig));
  const [expensesValid, setExpensesValid] = useState(true);
  const endAgeError = validateEndAge(config.endAge, config.currentAge);
  const canRun = expensesValid && !endAgeError && !running && !monteCarloRunning;
  const selectedPreset = presets.find(p => p.id === config.marketPresetId);
  const strategyOptions = strategies.length > 0 ? strategies : [EXISTING_STRATEGY];
  const selectedStrategy = strategyOptions.find(s => s.id === config.strategy.type);
  const fieldError = (path: string) => fieldErrors.find(e => e.field === path)?.message;
  // While End Age is mid-edit (blank or invalid), check expenses against the last valid one rather than
  // flagging them all as outside the simulation; Run stays blocked by the End Age error meanwhile
  const lastValidEndAge = useRef(config.endAge);
  if (!endAgeError) lastValidEndAge.current = config.endAge;

  const handleChange = (section: keyof FormConfig, field: string, value: string) => {
    setConfig(prev => ({
      ...prev,
      [section]: typeof prev[section] === 'object' 
        ? { ...prev[section] as object, [field]: Number(value) }
        : value
    }));
  };

  const handleSpousalChange = (field: string, value: string) => {
      setConfig(prev => ({
          ...prev,
          spousal: {
              ...prev.spousal,
              [field]: Number(value)
          }
      }));
  };

  const handleSSChange = (earner: 'lowerEarner' | 'higherEarner', field: string, value: string) => {
      setConfig(prev => ({
          ...prev,
          spousal: {
              ...prev.spousal,
              [earner]: {
                  ...prev.spousal[earner],
                  [field]: Number(value)
              }
          }
      }));
  };
  
  const handleRateChange = (field: string, value: string) => {
    const numValue = parseFloat(value);
    if (isNaN(numValue)) return;
    
    // Store as whole number percentage (e.g., 3.5 for 3.5%)
    const roundedPercentage = Math.round(numValue * 100) / 100;
    
    setConfig(prev => ({
      ...prev,
      rates: {
        ...prev.rates,
        [field]: roundedPercentage
      }
    }));
  };

  const handleStrategyChange = (type: string) => {
    setConfig(prev => ({ ...prev, strategy: { ...prev.strategy, type } }));
  };

  const handleGuardrailChange = (field: string, value: string | number) => {
    setConfig(prev => ({
      ...prev,
      strategy: { ...prev.strategy, guardrails: { ...prev.strategy.guardrails, [field]: value } }
    }));
  };

  const handlePresetChange = (presetId: string) => {
    const preset = presets.find(p => p.id === presetId);
    setConfig(prev => (preset ? applyPreset(prev, preset) : { ...prev, marketPresetId: null }));
  };

  const handleTopLevelChange = (field: keyof FormConfig, value: string) => {
      setConfig(prev => ({
          ...prev,
          [field]: field === 'name' ? value : Number(value)
      }));
  };

  const handleExpensesChange = (newExpenses: OneTimeExpense[]) => {
    setConfig(prev => ({
      ...prev,
      oneTimeExpenses: newExpenses
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!canRun) return;

    // Rates and tdaWithdrawalPercentage go to the backend as decimals
    onSubmit(toApiConfig(config));
  };

  const handleRunMonteCarlo = () => {
    if (!canRun || !onRunMonteCarlo) return;
    onRunMonteCarlo(toApiConfig(config));
  };

  // A preset sets the market rates, so they are disabled (greyed out, values still shown) until Custom is chosen (FR002-003)
  const presetSetsRate = (field: string) =>
    !!selectedPreset && (MARKET_RATE_FIELDS as readonly string[]).includes(field);
  const rateField = (field: keyof FormConfig['rates'], label: string) => (
    <Box>
      <TextField
        size="small" type="number" label={label}
        slotProps={{ htmlInput: { step: 0.01 } }}
        disabled={presetSetsRate(field)}
        value={formatPercent(config.rates[field])}
        onChange={e => handleRateChange(field, e.target.value)}
        sx={{ width: 220 }}
      />
    </Box>
  );

  const formatPercent = (decimal: number) => {
    // Now rates are stored as whole numbers, so just return them
    return Number(decimal.toFixed(2));
  };

  return (
    <Paper sx={{ p: 2, width: '100%' }}>
      <form onSubmit={handleSubmit}>
        <Grid container spacing={2} sx={{ alignItems: 'flex-start' }}>
          {/* General Section */}
          <Grid size="auto">
            <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>General</Typography>
            <Stack spacing={1.5}>
              <Box><TextField size="small" label="Name" value={config.name} onChange={e => handleTopLevelChange('name', e.target.value)} sx={{ width: 200 }} /></Box>
              <Box><TextField size="small" type="number" label="Current Age" value={config.currentAge} onChange={e => handleTopLevelChange('currentAge', e.target.value)} sx={{ width: 200 }} /></Box>
              <Box><TextField size="small" type="number" label="Retirement Age" value={config.retirementAge} onChange={e => handleTopLevelChange('retirementAge', e.target.value)} sx={{ width: 200 }} /></Box>
              <Box><TextField size="small" type="number" label="End Age" value={config.endAge} onChange={e => handleTopLevelChange('endAge', e.target.value)} error={!!endAgeError} helperText={endAgeError ?? 'Simulate through this age'} slotProps={{ htmlInput: { step: 1 } }} sx={{ width: 200 }} /></Box>
              <Box><TextField size="small" type="number" label="Current Year" value={config.currentYear} onChange={e => handleTopLevelChange('currentYear', e.target.value)} sx={{ width: 200 }} /></Box>
              <Box><TextField size="small" type="number" label="Annual Salary" value={config.salary} onChange={e => handleTopLevelChange('salary', e.target.value)} sx={{ width: 200 }} /></Box>
            </Stack>
          </Grid>

          {/* Social Security Section */}
          <Grid size="auto">
            <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>Social Security</Typography>
            <Stack spacing={1.5}>
              <Box><TextField size="small" type="number" label="Spouse Age" value={config.spousal.spouseAge} onChange={e => handleSpousalChange('spouseAge', e.target.value)} sx={{ width: 220 }} /></Box>
              <Box><TextField size="small" type="number" label="Lower Earner Claim Age" value={config.spousal.lowerEarner.claimAge} onChange={e => handleSSChange('lowerEarner', 'claimAge', e.target.value)} sx={{ width: 220 }} /></Box>
              <Box><TextField size="small" type="number" label="Lower Earner Benefit" value={config.spousal.lowerEarner.annualBenefit} onChange={e => handleSSChange('lowerEarner', 'annualBenefit', e.target.value)} sx={{ width: 220 }} /></Box>
              <Box><TextField size="small" type="number" label="Higher Earner Claim Age" value={config.spousal.higherEarner.claimAge} onChange={e => handleSSChange('higherEarner', 'claimAge', e.target.value)} sx={{ width: 220 }} /></Box>
              <Box><TextField size="small" type="number" label="Higher Earner Benefit" value={config.spousal.higherEarner.annualBenefit} onChange={e => handleSSChange('higherEarner', 'annualBenefit', e.target.value)} sx={{ width: 220 }} /></Box>
            </Stack>
          </Grid>

          {/* Portfolio Section */}
          <Grid size="auto">
            <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>Portfolio Balances</Typography>
            <Stack spacing={1.5}>
              <Box><TextField size="small" type="number" label="Spend Bucket (HYSA)" value={config.portfolio.sb} onChange={e => handleChange('portfolio', 'sb', e.target.value)} sx={{ width: 200 }} /></Box>
              <Box><TextField size="small" type="number" label="Crash Buffer (Bonds)" value={config.portfolio.cbb} onChange={e => handleChange('portfolio', 'cbb', e.target.value)} sx={{ width: 200 }} /></Box>
              <Box><TextField size="small" type="number" label="Taxable (TBA)" value={config.portfolio.tba} onChange={e => handleChange('portfolio', 'tba', e.target.value)} sx={{ width: 200 }} /></Box>
              <Box><TextField size="small" type="number" label="Tax Deferred (TDA)" value={config.portfolio.tda} onChange={e => handleChange('portfolio', 'tda', e.target.value)} sx={{ width: 200 }} /></Box>
              <Box><TextField size="small" type="number" label="Tax Free (TFA)" value={config.portfolio.tfa} onChange={e => handleChange('portfolio', 'tfa', e.target.value)} sx={{ width: 200 }} /></Box>
            </Stack>
          </Grid>

          {/* Expenses Section */}
          <Grid size="auto">
            <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>Annual Expenses</Typography>
            <Stack spacing={1.5}>
              <Box><TextField size="small" type="number" label="Needs" value={config.expenses.needs} onChange={e => handleChange('expenses', 'needs', e.target.value)} sx={{ width: 240 }} /></Box>
              <Box><TextField size="small" type="number" label="Wants" value={config.expenses.wants} onChange={e => handleChange('expenses', 'wants', e.target.value)} sx={{ width: 240 }} /></Box>
              <Box><TextField size="small" type="number" label="Property Tax" value={config.expenses.propertyTax} onChange={e => handleChange('expenses', 'propertyTax', e.target.value)} sx={{ width: 240 }} /></Box>
              <Box><TextField size="small" type="number" label="Healthcare (Pre-Retirement)" value={config.expenses.healthcarePreRetirement} onChange={e => handleChange('expenses', 'healthcarePreRetirement', e.target.value)} sx={{ width: 240 }} /></Box>
              <Box><TextField size="small" type="number" label="Healthcare (Retire to 65)" value={config.expenses.healthcarePostRetirementPreMedicare} onChange={e => handleChange('expenses', 'healthcarePostRetirementPreMedicare', e.target.value)} sx={{ width: 240 }} /></Box>
              <Box><TextField size="small" type="number" label="Healthcare (Medicare)" value={config.expenses.healthcareMedicare} onChange={e => handleChange('expenses', 'healthcareMedicare', e.target.value)} sx={{ width: 240 }} /></Box>
            </Stack>
          </Grid>

          {/* Contributions Section */}
          <Grid size="auto">
            <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>Annual Contributions</Typography>
            <Stack spacing={1.5}>
              <Box><TextField size="small" type="number" label="401k (Pre-Tax → TDA)" value={config.contributions.annual401k} onChange={e => handleChange('contributions', 'annual401k', e.target.value)} sx={{ width: 220 }} /></Box>
              <Box><TextField size="small" type="number" label="Roth (Post-Tax → TFA)" value={config.contributions.annualRoth} onChange={e => handleChange('contributions', 'annualRoth', e.target.value)} sx={{ width: 220 }} /></Box>
              <Box><TextField size="small" type="number" label="TBA Contribution" value={config.contributions.annualTba} onChange={e => handleChange('contributions', 'annualTba', e.target.value)} sx={{ width: 220 }} /></Box>
            </Stack>
          </Grid>

          {/* Rates Section */}
          <Grid size="auto">
            <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>Economic Assumptions (%)</Typography>
            <Stack spacing={1.5}>
              {presets.length > 0 && (
                <Box>
                  <TextField
                    select
                    size="small"
                    label="Market Conditions"
                    value={config.marketPresetId ?? CUSTOM_MARKET}
                    onChange={e => handlePresetChange(e.target.value)}
                    helperText={selectedPreset?.description ?? 'Your own rates below'}
                    sx={{ width: 220 }}
                  >
                    <MenuItem value={CUSTOM_MARKET}>Custom</MenuItem>
                    {presets.map(preset => <MenuItem key={preset.id} value={preset.id}>{preset.name}</MenuItem>)}
                  </TextField>
                </Box>
              )}
              {rateField('inflation', 'Inflation (%)')}
              {rateField('preRetirementGrowth', 'Pre-Retirement Growth (%)')}
              {rateField('postRetirementGrowth', 'Post-Retirement Growth (%)')}
              {rateField('bondYield', 'Bond Yield (%)')}
              {selectedPreset && (
                <Box sx={{ maxWidth: 220 }}>
                  <Typography variant="caption" color="text.secondary">
                    Set by the {selectedPreset.name} preset. Choose Custom to edit them.
                  </Typography>
                </Box>
              )}
              {rateField('hysaRate', 'HYSA Rate (%)')}
              {rateField('incomeTax', 'Effective Income Tax (%)')}
            </Stack>
          </Grid>

          {/* Strategy Section */}
          <Grid size="auto">
            <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>Withdrawal Strategy</Typography>
            <Stack spacing={1.5}>
              <Box>
                <TextField
                  select
                  size="small"
                  label="Spending Strategy"
                  value={selectedStrategy ? config.strategy.type : ''}
                  onChange={e => handleStrategyChange(e.target.value)}
                  error={!!fieldError('strategy.type')}
                  helperText={fieldError('strategy.type') ?? selectedStrategy?.description}
                  sx={{ width: 240 }}
                >
                  {strategyOptions.map(s => <MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>)}
                </TextField>
              </Box>
              {selectedStrategy?.settings === 'GUARDRAILS' && (
                <>
                  {GUARDRAIL_PERCENT_FIELDS.map(({ field, label }) => (
                    <Box key={field}>
                      <TextField
                        size="small" type="number" slotProps={{ htmlInput: { step: 1, min: 1, max: 99 } }} label={label}
                        value={config.strategy.guardrails[field]}
                        onChange={e => handleGuardrailChange(field, Number(e.target.value))}
                        error={!!fieldError(`strategy.guardrails.${field}`)}
                        helperText={fieldError(`strategy.guardrails.${field}`)}
                        sx={{ width: 240 }}
                      />
                    </Box>
                  ))}
                  <Box>
                    <TextField
                      select size="small" label="Review Frequency"
                      value={config.strategy.guardrails.reviewFrequency}
                      onChange={e => handleGuardrailChange('reviewFrequency', e.target.value)}
                      error={!!fieldError('strategy.guardrails.reviewFrequency')}
                      helperText={fieldError('strategy.guardrails.reviewFrequency')}
                      sx={{ width: 240 }}
                    >
                      <MenuItem value="YEARLY">Yearly</MenuItem>
                      <MenuItem value="QUARTERLY">Quarterly</MenuItem>
                    </TextField>
                  </Box>
                </>
              )}
              <Box><TextField size="small" type="number" label="Initial TDA (Deprecated)" value={config.strategy.initialTdaWithdrawal} onChange={e => handleChange('strategy', 'initialTdaWithdrawal', e.target.value)} disabled sx={{ width: 240 }} /></Box>
              <Box><TextField size="small" type="number" slotProps={{ htmlInput: { step: 1, min: 0, max: 100 } }} label="TDA Withdrawal % of Needs" value={config.strategy.tdaWithdrawalPercentage} onChange={e => handleChange('strategy', 'tdaWithdrawalPercentage', e.target.value)} sx={{ width: 240 }} /></Box>
              <Box><TextField size="small" type="number" label="Roth Convert (Pre-Retire)" value={config.strategy.rothConversionPreRetirement} onChange={e => handleChange('strategy', 'rothConversionPreRetirement', e.target.value)} sx={{ width: 240 }} /></Box>
              <Box><TextField size="small" type="number" label="Roth Convert (Post-Retire)" value={config.strategy.rothConversionPostRetirement} onChange={e => handleChange('strategy', 'rothConversionPostRetirement', e.target.value)} sx={{ width: 240 }} /></Box>
            </Stack>
          </Grid>

          {/* One-Time Expenses Section */}
          <Grid size={12}>
            <OneTimeExpenseInput
              expenses={config.oneTimeExpenses}
              onExpensesChange={handleExpensesChange}
              onValidityChange={setExpensesValid}
              currentAge={config.currentAge}
              currentYear={config.currentYear}
              endAge={lastValidEndAge.current}
            />
          </Grid>

          {/* Submit Button */}
          <Grid size={12} sx={{ mt: 1, display: 'flex', alignItems: 'center', gap: 2 }}>
            <Button
              type="submit"
              variant="contained"
              color="primary"
              disabled={!canRun}
              startIcon={running ? <CircularProgress size={16} color="inherit" /> : undefined}
            >
              {running ? 'Running…' : 'Run Simulation'}
            </Button>
            {onRunMonteCarlo && (
              <Button
                variant="outlined"
                color="primary"
                disabled={!canRun}
                onClick={handleRunMonteCarlo}
                startIcon={monteCarloRunning ? <CircularProgress size={16} color="inherit" /> : undefined}
              >
                {monteCarloRunning ? 'Running Monte Carlo…' : 'Run Monte Carlo (1,000 runs)'}
              </Button>
            )}
            {onImportInputs && onInputsFileError && (
              <InputsFileButtons getConfig={() => toApiConfig(config)} onImported={onImportInputs} onError={onInputsFileError} />
            )}
            {!expensesValid && (
              <Typography variant="body2" color="error">
                Complete or remove the invalid one-time expenses to run the simulation.
              </Typography>
            )}
          </Grid>
        </Grid>
      </form>
    </Paper>
  );
};

export default SimulationForm;