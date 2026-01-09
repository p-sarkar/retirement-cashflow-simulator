import React from 'react';
import { 
  Table, 
  TableBody, 
  TableCell, 
  TableContainer, 
  TableHead, 
  TableRow, 
  Paper,
  Typography,
  Alert
} from '@mui/material';
import { SimulationResult } from '../types/simulation';

interface ResultsTableProps {
  result: SimulationResult | null;
}

const formatMoney = (amount: number) => {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(amount);
};

const ResultsTable: React.FC<ResultsTableProps> = ({ result }) => {
  if (!result) return null;

  const { yearlyResults, summary } = result;

  return (
    <Paper sx={{ p: 2, mt: 3 }}>
      <Typography variant="h5" gutterBottom>Simulation Results</Typography>
      
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
              <TableCell>Year</TableCell>
              <TableCell>Age</TableCell>
              <TableCell>Spend Bucket</TableCell>
              <TableCell>Crash Buffer</TableCell>
              <TableCell>Taxable</TableCell>
              <TableCell>Tax Deferred</TableCell>
              <TableCell>Tax Free</TableCell>
              <TableCell>Total Portfolio</TableCell>
              <TableCell>Needs</TableCell>
              <TableCell>Wants</TableCell>
              <TableCell>Healthcare</TableCell>
              <TableCell>Property Tax</TableCell>
              <TableCell>Income Tax</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Total Expenses</TableCell>
              <TableCell>Income Gap</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {yearlyResults.map((row) => {
                const totalBalance = row.balances.sb + row.balances.cbb + row.balances.tba + row.balances.tda + row.balances.tfa;
                const isFailure = row.metrics.isFailure;
                return (
                  <TableRow key={row.year} sx={{ backgroundColor: isFailure ? '#ffebee' : 'inherit' }}>
                    <TableCell>{row.year}</TableCell>
                    <TableCell>{row.age}</TableCell>
                    <TableCell>{formatMoney(row.balances.sb)}</TableCell>
                    <TableCell>{formatMoney(row.balances.cbb)}</TableCell>
                    <TableCell>{formatMoney(row.balances.tba)}</TableCell>
                    <TableCell>{formatMoney(row.balances.tda)}</TableCell>
                    <TableCell>{formatMoney(row.balances.tfa)}</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>{formatMoney(totalBalance)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.needs)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.wants)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.healthcare)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.propertyTax)}</TableCell>
                    <TableCell>{formatMoney(row.cashFlow.incomeTax)}</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>{formatMoney(row.cashFlow.totalExpenses)}</TableCell>
                    <TableCell>{formatMoney(row.metrics.annualIncomeGap)}</TableCell>
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
