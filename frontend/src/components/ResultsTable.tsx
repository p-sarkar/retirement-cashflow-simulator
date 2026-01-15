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
  Box,
  IconButton,
  Tooltip
} from '@mui/material';
import InfoIcon from '@mui/icons-material/Info';
import { SimulationResult, SimulationConfig, QuarterlyResult, YearlyResult } from '../types/simulation';
import BreakdownDialog from './BreakdownDialog';

interface ResultsTableProps {
  result: SimulationResult | null;
  config: SimulationConfig | null;
}

const formatMoney = (amount: number) => {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(amount);
};

const ResultsTable: React.FC<ResultsTableProps> = ({ result, config }) => {
  const [viewMode, setViewMode] = useState<'yearly' | 'quarterly'>('yearly');
  const [breakdownOpen, setBreakdownOpen] = useState(false);
  const [selectedAge, setSelectedAge] = useState<number>(0);

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

  const handleBreakdownClick = (age: number) => {
    setSelectedAge(age);
    setBreakdownOpen(true);
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
              <TableCell>Details</TableCell>
              <TableCell>Time</TableCell>
              <TableCell>Age</TableCell>
              <TableCell>Spend Bucket</TableCell>
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
              <TableCell sx={{ fontWeight: 'bold' }}>TDA Withdrawal</TableCell>
              <TableCell sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>└─ TDA for Spend</TableCell>
              <TableCell sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>└─ TDA for Roth</TableCell>
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
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((row, index) => {
                const totalBalance = row.balances.sb + row.balances.cbb + row.balances.tba + row.balances.tda + row.balances.tfa;
                const isFailure = row.metrics.isFailure;
                const isAge75 = row.age === 75;
                const timeLabel = 'quarter' in row
                    ? `${row.year} Q${row.quarter + 1}` 
                    : row.year;

                // Determine background color: failure (red) > age 75 (yellow) > default
                const backgroundColor = isFailure ? '#ffebee' : (isAge75 ? '#fff9c4' : 'inherit');

                return (
                  <TableRow key={index} sx={{ backgroundColor }}>
                    <TableCell>
                      <Tooltip title="View detailed computation breakdown">
                        <IconButton
                          size="small"
                          onClick={() => handleBreakdownClick(row.age)}
                          color="primary"
                        >
                          <InfoIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                    <TableCell>{timeLabel}</TableCell>
                    <TableCell>{row.age}</TableCell>
                    <TableCell>{formatMoney(row.balances.sb)}</TableCell>
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
                    <TableCell sx={{ fontWeight: 'bold' }}>{formatMoney(row.cashFlow.tdaWithdrawal)}</TableCell>
                    <TableCell sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>{formatMoney(row.cashFlow.tdaWithdrawalSpend)}</TableCell>
                    <TableCell sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>{formatMoney(row.cashFlow.tdaWithdrawalRoth)}</TableCell>
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
                  </TableRow>
                );
            })}
          </TableBody>
        </Table>
      </TableContainer>

      <BreakdownDialog
        open={breakdownOpen}
        onClose={() => setBreakdownOpen(false)}
        config={config}
        targetAge={selectedAge}
      />
    </Paper>
  );
};

export default ResultsTable;
