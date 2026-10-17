import React from 'react';
import { Box, Paper, Table, TableBody, TableCell, TableContainer, TableRow, Typography } from '@mui/material';
import { ExpenseSection } from '../../types/simulation';
import LineItemTable from './LineItemTable';
import { formatMoney } from './format';

const KINDS = { CASH: 'cash expense', DOWN_PAYMENT: 'down payment', LOAN_PAYMENT: 'loan payment' } as const;

/** Section 2: the year's expenses (FR004-020–FR004-022). */
const ExpensesSection: React.FC<{ section: ExpenseSection }> = ({ section }) => (
  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
    <LineItemTable label="Expenses" items={section.items} />
    {section.wantsDerivation.length > 0 && (
      <Box>
        <Typography variant="subtitle2" gutterBottom>How Wants is computed in a working year</Typography>
        <LineItemTable label="How Wants is computed" items={section.wantsDerivation} />
      </Box>
    )}
    {section.oneTimeExpenses.length > 0 && (
      <Box>
        <Typography variant="subtitle2" gutterBottom>One-time expenses</Typography>
        <TableContainer component={Paper} variant="outlined">
          <Table size="small" aria-label="One-time expenses">
            <TableBody>
              {section.oneTimeExpenses.map((e, i) => (
                <TableRow key={i}>
                  <TableCell component="th" scope="row">{`${e.name} (${KINDS[e.type]})`}</TableCell>
                  <TableCell align="right" sx={{ fontFamily: 'monospace' }}>{formatMoney(e.amount)}</TableCell>
                  <TableCell>
                    <Typography variant="caption" color="text.secondary">
                      {!e.inflationAdjusted
                        ? 'Fixed for the loan term'
                        : e.todaysDollars != null
                          ? `Inflation-adjusted from ${formatMoney(e.todaysDollars)} in today's dollars`
                          : 'Inflation-adjusted'}
                    </Typography>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Box>
    )}
    <Typography variant="subtitle1">Total expenses: <strong>{formatMoney(section.total)}</strong></Typography>
  </Box>
);

export default ExpensesSection;
