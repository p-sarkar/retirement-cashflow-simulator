import React, { useState } from 'react';
import { 
  Table, 
  TableBody, 
  TableCell, 
  TableContainer, 
  TableHead, 
  TableRow, 
  Paper,
  Typography,
  Alert,
  ToggleButton,
  ToggleButtonGroup,
  Box
} from '@mui/material';
import { SimulationResult, QuarterlyResult, YearlyResult } from '../types/simulation';

interface ResultsTableProps {
  result: SimulationResult | null;
}

const formatMoney = (amount: number) => {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(amount);
};

const ResultsTable: React.FC<ResultsTableProps> = ({ result }) => {
  const [viewMode, setViewMode] = useState<'yearly' | 'quarterly'>('yearly');

  if (!result) return null;

  const { yearlyResults, quarterlyResults, summary } = result;

  const handleViewChange = (
    _event: React.MouseEvent<HTMLElement>,
    newView: 'yearly' | 'quarterly' | null,
  ) => {
    if (newView !== null) {
      setViewMode(newView);
    }
  };

  const rows: (YearlyResult | QuarterlyResult)[] = viewMode === 'yearly' ? yearlyResults : (quarterlyResults || []);

  return (
    <Paper sx={{ p: 2, mt: 3, width: '100%', overflowX: 'auto' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5">Simulation Results</Typography>
        <ToggleButtonGroup
          value={viewMode}
          exclusive
          onChange={handleViewChange}
          aria-label="view mode"
          size="small"
        >
          <ToggleButton value="yearly" aria-label="yearly">
            Yearly
          </ToggleButton>
          <ToggleButton value="quarterly" aria-label="quarterly">
            Quarterly
          </ToggleButton>
        </ToggleButtonGroup>
      </Box>
      
      {summary.isSuccess ? (
        <Alert severity="success" sx={{ mb: 2 }}>
          Success! Plan survived to age 85 with ending balance of {formatMoney(summary.finalTotalBalance)}.
        </Alert>
      ) : (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failure! Plan failed at year {summary.failureYear}.
        </Alert>
      )}

      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Time</TableCell>
              <TableCell>Age</TableCell>
              <TableCell>Spend Bucket</TableCell>
              <TableCell>SB Deposit</TableCell>
              <TableCell>SB Withdrawal</TableCell>
              <TableCell>SB Cap</TableCell>
              <TableCell>Crash Buffer</TableCell>
              <TableCell>CBB Cap</TableCell>
              <TableCell>Taxable</TableCell>
              <TableCell>Tax Deferred</TableCell>
              <TableCell>Tax Free</TableCell>
              <TableCell>Total Portfolio</TableCell>
              <TableCell>Salary</TableCell>
              <TableCell>Interest</TableCell>
              <TableCell>Dividends</TableCell>
              <TableCell>Social Security</TableCell>
              <TableCell>TBA Withdrawal</TableCell>
              <TableCell>TDA Withdrawal</TableCell>
              <TableCell>Roth Conv.</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Total Income</TableCell>
              <TableCell>Needs</TableCell>
              <TableCell>Wants</TableCell>
              <TableCell>Healthcare</TableCell>
              <TableCell>Property Tax</TableCell>
              <TableCell>Income Tax</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Total Expenses</TableCell>
              <TableCell>401k Contrib</TableCell>
              <TableCell>TBA Contrib</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Income Gap</TableCell>
              <TableCell sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>└─ Gap Expenses</TableCell>
              <TableCell sx={{ fontSize: '0.65rem', color: 'text.disabled', paddingLeft: 3 }}>⤷ Needs</TableCell>
              <TableCell sx={{ fontSize: '0.65rem', color: 'text.disabled', paddingLeft: 3 }}>⤷ Wants</TableCell>
              <TableCell sx={{ fontSize: '0.65rem', color: 'text.disabled', paddingLeft: 3 }}>⤷ Healthcare</TableCell>
              <TableCell sx={{ fontSize: '0.65rem', color: 'text.disabled', paddingLeft: 3 }}>⤷ Property Tax</TableCell>
              <TableCell sx={{ fontSize: '0.65rem', color: 'text.disabled', paddingLeft: 3 }}>⤷ Income Tax</TableCell>
              <TableCell sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>└─ Passive Income</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((row, index) => {
                const totalBalance = row.balances.sb + row.balances.cbb + row.balances.tba + row.balances.tda + row.balances.tfa;
                const isFailure = row.metrics.isFailure;
                const timeLabel = 'quarter' in row 
                    ? `${row.year} Q${row.quarter + 1}` 
                    : row.year;

                return (
                  <TableRow key={index} sx={{ backgroundColor: isFailure ? '#ffebee' : 'inherit' }}>
                    <TableCell>{timeLabel}</TableCell>
                    <TableCell>{row.age}</TableCell>
                    <TableCell>{formatMoney(row.balances.sb)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.sbDeposit)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.sbWithdrawal)}</TableCell>
                    <TableCell>{formatMoney(row.metrics.sbCap)}</TableCell>
                    <TableCell>{formatMoney(row.balances.cbb)}</TableCell>
                    <TableCell>{formatMoney(row.metrics.cbbCap)}</TableCell>
                    <TableCell>{formatMoney(row.balances.tba)}</TableCell>
                    <TableCell>{formatMoney(row.balances.tda)}</TableCell>
                    <TableCell>{formatMoney(row.balances.tfa)}</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>{formatMoney(totalBalance)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.salary)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.interest)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.dividends)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.socialSecurity)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.tbaWithdrawal)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.tdaWithdrawal)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.rothConversion)}</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>{formatMoney(row.cashFlow.totalIncome)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.needs)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.wants)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.healthcare)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.propertyTax)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.incomeTax)}</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>{formatMoney(row.cashFlow.totalExpenses)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.contribution401k)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.contributionTba)}</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>{formatMoney(row.metrics.annualIncomeGap)}</TableCell>
                    <TableCell sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>{formatMoney(row.metrics.incomeGapExpenses)}</TableCell>
                    <TableCell sx={{ fontSize: '0.65rem', color: 'text.disabled' }}>{formatMoney(row.cashFlow.needs)}</TableCell>
                    <TableCell sx={{ fontSize: '0.65rem', color: 'text.disabled' }}>{formatMoney(row.cashFlow.wants)}</TableCell>
                    <TableCell sx={{ fontSize: '0.65rem', color: 'text.disabled' }}>{formatMoney(row.cashFlow.healthcare)}</TableCell>
                    <TableCell sx={{ fontSize: '0.65rem', color: 'text.disabled' }}>{formatMoney(row.cashFlow.propertyTax)}</TableCell>
                    <TableCell sx={{ fontSize: '0.65rem', color: 'text.disabled' }}>{formatMoney(row.cashFlow.incomeTax)}</TableCell>
                    <TableCell sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>{formatMoney(row.metrics.incomeGapPassiveIncome)}</TableCell>
                  </TableRow>
                );
            })}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  );
};

export default ResultsTable;
