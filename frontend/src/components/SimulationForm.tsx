import React, { useState } from 'react';
import { 
  Button, 
  TextField, 
  Typography, 
  Grid, 
  Paper
} from '@mui/material';
import { SimulationConfig, OneTimeExpense } from '../types/simulation';
import OneTimeExpenseInput from './OneTimeExpenseInput';

interface SimulationFormProps {
  onSubmit: (config: SimulationConfig) => void;
}

const defaultConfig: SimulationConfig = {
  name: "My Retirement Plan",
  currentYear: new Date().getFullYear()-1,
  currentAge: 50,
  retirementAge: 55,
  salary: 230000,
  portfolio: {
    sb: 200000,
    cbb: 500000,
    tba: 1200000,
    tda: 1350000,
    tfa: 325000
  },
  spousal: {
    spouseAge: 50,
    lowerEarner: { claimAge: 67, annualBenefit: 20000 },
    higherEarner: { claimAge: 70, annualBenefit: 40000 }
  },
  expenses: {
    needs: 50000,
    wants: 100000,
    propertyTax: 12000,
    healthcarePreRetirement: 5000,
    healthcarePostRetirementPreMedicare: 24000,
    healthcareMedicare: 3000
  },
  contributions: {
    annual401k: 4000,
    annualRoth401k: 28000, // Roth 401k contribution (post-tax, goes to TFA)
    annualTba: 12000
  },
  rates: {
    inflation: 3, // Stored as whole number percentage
    preRetirementGrowth: 8,
    postRetirementGrowth: 8,
    bondYield: 5,
    hysaRate: 3,
    incomeTax: 18
  },
  strategy: {
    initialTdaWithdrawal: 40000, // Deprecated
    tdaWithdrawalPercentage: 60, // Stored as whole number in UI (100 = 100%), converted to 1.0 on submit
    rothConversionAmount: 0, // Deprecated
    rothConversionPreRetirement: 10000, // Pre-retirement Roth conversion
    rothConversionPostRetirement: 40000, // Post-retirement Roth conversion
    type: "PARTHA_V0_01_20250105"
  },
  oneTimeExpenses: [
    {
      id: 'default-cash-1',
      type: 'CASH',
      name: "Daughter's wedding",
      amount: 100000,
      yearOrAge: { type: 'AGE', age: 60 }
    },
    {
      id: 'partha-car-loan-1',
      type: 'LOAN',
      name: "Partha's car 1",
      principal: 50000,
      downPayment: 10000,
      aprPercent: 4,
      termYears: 5,
      startYearOrAge: { type: 'AGE', age: 55 },
      monthlyPayment: 737.93 // Pre-calculated: 40000 financed (50k - 10k down) at 4% for 5 years
    },
    {
      id: 'mou-car-loan-1',
      type: 'LOAN',
      name: "Mou's car 1",
      principal: 50000,
      aprPercent: 4,
      termYears: 5,
      startYearOrAge: { type: 'AGE', age: 58 },
      monthlyPayment: 920.41 // Pre-calculated: 50000 at 4% for 5 years
    },
    {
      id: 'partha-car-loan-2',
      type: 'LOAN',
      name: "Partha's car 2",
      principal: 50000,
      aprPercent: 4,
      termYears: 5,
      startYearOrAge: { type: 'AGE', age: 65 },
      monthlyPayment: 920.41 // Pre-calculated: 50000 at 4% for 5 years
    },
    {
      id: 'mou-car-loan-2',
      type: 'LOAN',
      name: "Mou's car 2",
      principal: 50000,
      aprPercent: 4,
      termYears: 5,
      startYearOrAge: { type: 'AGE', age: 68 },
      monthlyPayment: 920.41 // Pre-calculated: 50000 at 4% for 5 years
    },
  ] as OneTimeExpense[]
};

const SimulationForm: React.FC<SimulationFormProps> = ({ onSubmit }) => {
  const [config, setConfig] = useState<SimulationConfig>(defaultConfig);

  const handleChange = (section: keyof SimulationConfig, field: string, value: any) => {
    setConfig(prev => ({
      ...prev,
      [section]: typeof prev[section] === 'object' 
        ? { ...prev[section] as object, [field]: Number(value) }
        : value
    }));
  };

  const handleSpousalChange = (field: string, value: any) => {
      setConfig(prev => ({
          ...prev,
          spousal: {
              ...prev.spousal,
              [field]: Number(value)
          }
      }));
  };

  const handleSSChange = (earner: 'lowerEarner' | 'higherEarner', field: string, value: any) => {
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
  
  const handleRateChange = (field: string, value: any) => {
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

  const handleTopLevelChange = (field: keyof SimulationConfig, value: any) => {
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

    // Convert rates and tdaWithdrawalPercentage from whole number percentages to decimals for backend
    const configForBackend = {
      ...config,
      rates: {
        inflation: config.rates.inflation / 100,
        preRetirementGrowth: config.rates.preRetirementGrowth / 100,
        postRetirementGrowth: config.rates.postRetirementGrowth / 100,
        bondYield: config.rates.bondYield / 100,
        hysaRate: config.rates.hysaRate / 100,
        incomeTax: config.rates.incomeTax / 100
      },
      strategy: {
        ...config.strategy,
        tdaWithdrawalPercentage: config.strategy.tdaWithdrawalPercentage / 100
      }
    };

    onSubmit(configForBackend);
  };

  const formatPercent = (decimal: number) => {
    // Now rates are stored as whole numbers, so just return them
    return Number(decimal.toFixed(2));
  };

  return (
    <Paper sx={{ p: 2, width: '100%' }}>
      <form onSubmit={handleSubmit}>
        <Grid container spacing={2} alignItems="flex-start">
          {/* General Section */}
          <Grid size="auto">
            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>General</Typography>
            <Grid container direction="column" spacing={1.5}>
              <Grid><TextField size="small" label="Name" value={config.name} onChange={e => handleTopLevelChange('name', e.target.value)} sx={{ width: 200 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Current Age" value={config.currentAge} onChange={e => handleTopLevelChange('currentAge', e.target.value)} sx={{ width: 200 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Retirement Age" value={config.retirementAge} onChange={e => handleTopLevelChange('retirementAge', e.target.value)} sx={{ width: 200 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Current Year" value={config.currentYear} onChange={e => handleTopLevelChange('currentYear', e.target.value)} sx={{ width: 200 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Annual Salary" value={config.salary} onChange={e => handleTopLevelChange('salary', e.target.value)} sx={{ width: 200 }} /></Grid>
            </Grid>
          </Grid>

          {/* Social Security Section */}
          <Grid size="auto">
            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>Social Security</Typography>
            <Grid container direction="column" spacing={1.5}>
              <Grid><TextField size="small" type="number" label="Spouse Age" value={config.spousal.spouseAge} onChange={e => handleSpousalChange('spouseAge', e.target.value)} sx={{ width: 220 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Lower Earner Claim Age" value={config.spousal.lowerEarner.claimAge} onChange={e => handleSSChange('lowerEarner', 'claimAge', e.target.value)} sx={{ width: 220 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Lower Earner Benefit" value={config.spousal.lowerEarner.annualBenefit} onChange={e => handleSSChange('lowerEarner', 'annualBenefit', e.target.value)} sx={{ width: 220 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Higher Earner Claim Age" value={config.spousal.higherEarner.claimAge} onChange={e => handleSSChange('higherEarner', 'claimAge', e.target.value)} sx={{ width: 220 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Higher Earner Benefit" value={config.spousal.higherEarner.annualBenefit} onChange={e => handleSSChange('higherEarner', 'annualBenefit', e.target.value)} sx={{ width: 220 }} /></Grid>
            </Grid>
          </Grid>

          {/* Portfolio Section */}
          <Grid size="auto">
            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>Portfolio Balances</Typography>
            <Grid container direction="column" spacing={1.5}>
              <Grid><TextField size="small" type="number" label="Spend Bucket (HYSA)" value={config.portfolio.sb} onChange={e => handleChange('portfolio', 'sb', e.target.value)} sx={{ width: 200 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Crash Buffer (Bonds)" value={config.portfolio.cbb} onChange={e => handleChange('portfolio', 'cbb', e.target.value)} sx={{ width: 200 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Taxable (TBA)" value={config.portfolio.tba} onChange={e => handleChange('portfolio', 'tba', e.target.value)} sx={{ width: 200 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Tax Deferred (TDA)" value={config.portfolio.tda} onChange={e => handleChange('portfolio', 'tda', e.target.value)} sx={{ width: 200 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Tax Free (TFA)" value={config.portfolio.tfa} onChange={e => handleChange('portfolio', 'tfa', e.target.value)} sx={{ width: 200 }} /></Grid>
            </Grid>
          </Grid>

          {/* Expenses Section */}
          <Grid size="auto">
            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>Annual Expenses</Typography>
            <Grid container direction="column" spacing={1.5}>
              <Grid><TextField size="small" type="number" label="Needs" value={config.expenses.needs} onChange={e => handleChange('expenses', 'needs', e.target.value)} sx={{ width: 240 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Wants" value={config.expenses.wants} onChange={e => handleChange('expenses', 'wants', e.target.value)} sx={{ width: 240 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Property Tax" value={config.expenses.propertyTax} onChange={e => handleChange('expenses', 'propertyTax', e.target.value)} sx={{ width: 240 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Healthcare (Pre-Retirement)" value={config.expenses.healthcarePreRetirement} onChange={e => handleChange('expenses', 'healthcarePreRetirement', e.target.value)} sx={{ width: 240 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Healthcare (Retire to 65)" value={config.expenses.healthcarePostRetirementPreMedicare} onChange={e => handleChange('expenses', 'healthcarePostRetirementPreMedicare', e.target.value)} sx={{ width: 240 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Healthcare (Medicare)" value={config.expenses.healthcareMedicare} onChange={e => handleChange('expenses', 'healthcareMedicare', e.target.value)} sx={{ width: 240 }} /></Grid>
            </Grid>
          </Grid>

          {/* Contributions Section */}
          <Grid size="auto">
            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>Annual Contributions</Typography>
            <Grid container direction="column" spacing={1.5}>
              <Grid><TextField size="small" type="number" label="401k (Pre-Tax → TDA)" value={config.contributions.annual401k} onChange={e => handleChange('contributions', 'annual401k', e.target.value)} sx={{ width: 220 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Roth 401k (Post-Tax → TFA)" value={config.contributions.annualRoth401k} onChange={e => handleChange('contributions', 'annualRoth401k', e.target.value)} sx={{ width: 220 }} /></Grid>
              <Grid><TextField size="small" type="number" label="TBA Contribution" value={config.contributions.annualTba} onChange={e => handleChange('contributions', 'annualTba', e.target.value)} sx={{ width: 220 }} /></Grid>
            </Grid>
          </Grid>

          {/* Rates Section */}
          <Grid size="auto">
            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>Economic Assumptions (%)</Typography>
            <Grid container direction="column" spacing={1.5}>
              <Grid><TextField size="small" type="number" inputProps={{step: 0.01}} label="Inflation (%)" value={formatPercent(config.rates.inflation)} onChange={e => handleRateChange('inflation', e.target.value)} sx={{ width: 220 }} /></Grid>
              <Grid><TextField size="small" type="number" inputProps={{step: 0.01}} label="Pre-Retirement Growth (%)" value={formatPercent(config.rates.preRetirementGrowth)} onChange={e => handleRateChange('preRetirementGrowth', e.target.value)} sx={{ width: 220 }} /></Grid>
              <Grid><TextField size="small" type="number" inputProps={{step: 0.01}} label="Post-Retirement Growth (%)" value={formatPercent(config.rates.postRetirementGrowth)} onChange={e => handleRateChange('postRetirementGrowth', e.target.value)} sx={{ width: 220 }} /></Grid>
              <Grid><TextField size="small" type="number" inputProps={{step: 0.01}} label="Bond Yield (%)" value={formatPercent(config.rates.bondYield)} onChange={e => handleRateChange('bondYield', e.target.value)} sx={{ width: 220 }} /></Grid>
              <Grid><TextField size="small" type="number" inputProps={{step: 0.01}} label="HYSA Rate (%)" value={formatPercent(config.rates.hysaRate)} onChange={e => handleRateChange('hysaRate', e.target.value)} sx={{ width: 220 }} /></Grid>
              <Grid><TextField size="small" type="number" inputProps={{step: 0.01}} label="Effective Income Tax (%)" value={formatPercent(config.rates.incomeTax)} onChange={e => handleRateChange('incomeTax', e.target.value)} sx={{ width: 220 }} /></Grid>
            </Grid>
          </Grid>

          {/* Strategy Section */}
          <Grid size="auto">
            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>Withdrawal Strategy</Typography>
            <Grid container direction="column" spacing={1.5}>
              <Grid><TextField size="small" type="number" label="Initial TDA (Deprecated)" value={config.strategy.initialTdaWithdrawal} onChange={e => handleChange('strategy', 'initialTdaWithdrawal', e.target.value)} disabled sx={{ width: 240 }} /></Grid>
              <Grid><TextField size="small" type="number" inputProps={{step: 1, min: 0, max: 100}} label="TDA Withdrawal % of Needs" value={config.strategy.tdaWithdrawalPercentage} onChange={e => handleChange('strategy', 'tdaWithdrawalPercentage', e.target.value)} sx={{ width: 240 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Roth Convert (Pre-Retire)" value={config.strategy.rothConversionPreRetirement} onChange={e => handleChange('strategy', 'rothConversionPreRetirement', e.target.value)} sx={{ width: 240 }} /></Grid>
              <Grid><TextField size="small" type="number" label="Roth Convert (Post-Retire)" value={config.strategy.rothConversionPostRetirement} onChange={e => handleChange('strategy', 'rothConversionPostRetirement', e.target.value)} sx={{ width: 240 }} /></Grid>
            </Grid>
          </Grid>

          {/* One-Time Expenses Section */}
          <Grid size={12}>
            <OneTimeExpenseInput
              expenses={config.oneTimeExpenses}
              onExpensesChange={handleExpensesChange}
              currentAge={config.currentAge}
              currentYear={config.currentYear}
            />
          </Grid>

          {/* Submit Button */}
          <Grid size={12} sx={{ mt: 1 }}>
            <Button type="submit" variant="contained" color="primary">
              Run Simulation
            </Button>
          </Grid>
        </Grid>
      </form>
    </Paper>
  );
};

export default SimulationForm;