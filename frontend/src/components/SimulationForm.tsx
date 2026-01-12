import React, { useState } from 'react';
import { 
  Button, 
  TextField, 
  Typography, 
  Grid, 
  Paper,
  Divider
} from '@mui/material';
import { SimulationConfig } from '../types/simulation';

interface SimulationFormProps {
  onSubmit: (config: SimulationConfig) => void;
}

const defaultConfig: SimulationConfig = {
  name: "My Retirement Plan",
  currentYear: new Date().getFullYear()-1,
  currentAge: 50,
  retirementAge: 55,
  salary: 120000,
  portfolio: {
    sb: 45000,
    cbb: 450000,
    tba: 1200000,
    tda: 1350000,
    tfa: 475000
  },
  spousal: {
    spouseAge: 50,
    lowerEarner: { claimAge: 67, annualBenefit: 20000 },
    higherEarner: { claimAge: 70, annualBenefit: 40000 }
  },
  expenses: {
    needs: 50000,
    wants: 100000,
    propertyTax: 10000,
    healthcarePreMedicare: 5000,
    healthcareMedicare: 3000
  },
  contributions: {
    annual401k: 23000,
    annualTba: 12000
  },
  rates: {
    inflation: 0.03,
    preRetirementGrowth: 0.07,
    postRetirementGrowth: 0.05,
    bondYield: 0.04,
    hysaRate: 0.04,
    incomeTax: 0.20
  },
  strategy: {
    initialTdaWithdrawal: 40000,
    rothConversionAmount: 10000,
    type: "PARTHA_V0_01_20250105"
  }
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
  
  const handleTopLevelChange = (field: keyof SimulationConfig, value: any) => {
      setConfig(prev => ({
          ...prev,
          [field]: field === 'name' ? value : Number(value)
      }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(config);
  };

  return (
    <Paper sx={{ p: 3, mb: 3 }}>
      <Typography variant="h5" gutterBottom>Simulation Parameters</Typography>
      <form onSubmit={handleSubmit}>
        <Grid container spacing={3}>
          {/* General */}
          <Grid size={12}>
            <Typography variant="h6">General</Typography>
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField fullWidth label="Name" value={config.name} onChange={e => handleTopLevelChange('name', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 2 }}>
            <TextField fullWidth type="number" label="Current Age" value={config.currentAge} onChange={e => handleTopLevelChange('currentAge', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 2 }}>
            <TextField fullWidth type="number" label="Retirement Age" value={config.retirementAge} onChange={e => handleTopLevelChange('retirementAge', e.target.value)} />
          </Grid>
           <Grid size={{ xs: 6, sm: 2 }}>
            <TextField fullWidth type="number" label="Current Year" value={config.currentYear} onChange={e => handleTopLevelChange('currentYear', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 2 }}>
            <TextField fullWidth type="number" label="Annual Salary" value={config.salary} onChange={e => handleTopLevelChange('salary', e.target.value)} />
          </Grid>

          {/* Social Security */}
          <Grid size={12}><Divider /></Grid>
          <Grid size={12}>
            <Typography variant="h6">Social Security</Typography>
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" label="Spouse Age" value={config.spousal.spouseAge} onChange={e => handleSpousalChange('spouseAge', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
             <TextField fullWidth type="number" label="Lower Earner Claim Age" value={config.spousal.lowerEarner.claimAge} onChange={e => handleSSChange('lowerEarner', 'claimAge', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
             <TextField fullWidth type="number" label="Lower Earner Benefit" value={config.spousal.lowerEarner.annualBenefit} onChange={e => handleSSChange('lowerEarner', 'annualBenefit', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
             {/* Spacer to align next row if needed, or just let it wrap */}
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
             <TextField fullWidth type="number" label="Higher Earner Claim Age" value={config.spousal.higherEarner.claimAge} onChange={e => handleSSChange('higherEarner', 'claimAge', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
             <TextField fullWidth type="number" label="Higher Earner Benefit" value={config.spousal.higherEarner.annualBenefit} onChange={e => handleSSChange('higherEarner', 'annualBenefit', e.target.value)} />
          </Grid>

          {/* Portfolio */}
          <Grid size={12}><Divider /></Grid>
          <Grid size={12}>
            <Typography variant="h6">Portfolio Balances</Typography>
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" label="Spend Bucket (HYSA)" value={config.portfolio.sb} onChange={e => handleChange('portfolio', 'sb', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" label="Crash Buffer (Bonds)" value={config.portfolio.cbb} onChange={e => handleChange('portfolio', 'cbb', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
             <TextField fullWidth type="number" label="Taxable (TBA)" value={config.portfolio.tba} onChange={e => handleChange('portfolio', 'tba', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
             <TextField fullWidth type="number" label="Tax Deferred (TDA)" value={config.portfolio.tda} onChange={e => handleChange('portfolio', 'tda', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
             <TextField fullWidth type="number" label="Tax Free (TFA)" value={config.portfolio.tfa} onChange={e => handleChange('portfolio', 'tfa', e.target.value)} />
          </Grid>

          {/* Expenses */}
          <Grid size={12}><Divider /></Grid>
          <Grid size={12}>
            <Typography variant="h6">Annual Expenses (Today's Dollars)</Typography>
          </Grid>
           <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" label="Needs" value={config.expenses.needs} onChange={e => handleChange('expenses', 'needs', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" label="Wants" value={config.expenses.wants} onChange={e => handleChange('expenses', 'wants', e.target.value)} />
          </Grid>
           <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" label="Property Tax" value={config.expenses.propertyTax} onChange={e => handleChange('expenses', 'propertyTax', e.target.value)} />
          </Grid>
           <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" label="Healthcare (Pre-Medicare)" value={config.expenses.healthcarePreMedicare} onChange={e => handleChange('expenses', 'healthcarePreMedicare', e.target.value)} />
          </Grid>
           <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" label="Healthcare (Medicare)" value={config.expenses.healthcareMedicare} onChange={e => handleChange('expenses', 'healthcareMedicare', e.target.value)} />
          </Grid>

          {/* Contributions */}
          <Grid size={12}><Divider /></Grid>
          <Grid size={12}>
            <Typography variant="h6">Annual Contributions (Today's Dollars)</Typography>
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" label="401k Contribution" value={config.contributions.annual401k} onChange={e => handleChange('contributions', 'annual401k', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" label="TBA Contribution" value={config.contributions.annualTba} onChange={e => handleChange('contributions', 'annualTba', e.target.value)} />
          </Grid>

          {/* Rates */}
          <Grid size={12}><Divider /></Grid>
          <Grid size={12}>
            <Typography variant="h6">Economic Assumptions (Decimal, e.g. 0.03 = 3%)</Typography>
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" inputProps={{step: 0.01}} label="Inflation" value={config.rates.inflation} onChange={e => handleChange('rates', 'inflation', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" inputProps={{step: 0.01}} label="Pre-Retirement Growth" value={config.rates.preRetirementGrowth} onChange={e => handleChange('rates', 'preRetirementGrowth', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" inputProps={{step: 0.01}} label="Post-Retirement Growth" value={config.rates.postRetirementGrowth} onChange={e => handleChange('rates', 'postRetirementGrowth', e.target.value)} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" inputProps={{step: 0.01}} label="Bond Yield" value={config.rates.bondYield} onChange={e => handleChange('rates', 'bondYield', e.target.value)} />
          </Grid>
           <Grid size={{ xs: 6, sm: 4 }}>
            <TextField fullWidth type="number" inputProps={{step: 0.01}} label="HYSA Rate" value={config.rates.hysaRate} onChange={e => handleChange('rates', 'hysaRate', e.target.value)} />
          </Grid>

          <Grid size={12} sx={{ mt: 2 }}>
            <Button type="submit" variant="contained" color="primary" size="large">
              Run Simulation
            </Button>
          </Grid>
        </Grid>
      </form>
    </Paper>
  );
};

export default SimulationForm;