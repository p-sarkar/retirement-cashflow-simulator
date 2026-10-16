import React from 'react';
import {
  Box, Paper, Table, TableBody, TableCell, TableContainer, TableFooter, TableHead, TableRow, Typography
} from '@mui/material';
import { TaxComputation, TaxSection } from '../../types/simulation';
import { formatMoney, formatPercent } from './format';

const BASES = {
  PRIOR_YEAR: "Based on last year's taxable income",
  START_YEAR_ESTIMATE: "Based on an estimate of the start year's income (salary minus 401k, plus interest and dividends on the starting balances)",
  THIS_YEAR: 'Paid next year'
} as const;

const ComputationTable: React.FC<{ label: string; computation: TaxComputation }> = ({ label, computation: c }) => (
  <Box>
    <Typography variant="subtitle2">{label}</Typography>
    <Typography variant="caption" color="text.secondary" component="div" sx={{ mb: 1 }}>{BASES[c.basis]}</Typography>
    <TableContainer component={Paper} variant="outlined">
      <Table size="small" aria-label={label}>
        <TableHead>
          <TableRow>
            <TableCell />
            <TableCell align="right">Amount</TableCell>
            <TableCell align="right">Taxed</TableCell>
            <TableCell align="right">Taxable</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {c.components.map(item => (
            <TableRow key={item.name}>
              <TableCell component="th" scope="row">{item.name}</TableCell>
              <TableCell align="right" sx={{ fontFamily: 'monospace' }}>{formatMoney(item.amount)}</TableCell>
              <TableCell align="right">{formatPercent(item.taxedShare)}</TableCell>
              <TableCell align="right" sx={{ fontFamily: 'monospace' }}>{formatMoney(item.taxable)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
        <TableFooter>
          <TableRow>
            <TableCell colSpan={4} align="right" sx={{ fontWeight: 'bold', color: 'text.primary' }}>
              {`${formatMoney(c.taxableIncome)} × ${formatPercent(c.rate)} = ${formatMoney(c.tax)}`}
            </TableCell>
          </TableRow>
        </TableFooter>
      </Table>
    </TableContainer>
  </Box>
);

/** Section 6: this year's tax bill and the taxable income that sets next year's (FR004-060–FR004-062). */
const TaxesSection: React.FC<{ section: TaxSection }> = ({ section }) => (
  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
      <ComputationTable label="This year's tax bill" computation={section.billThisYear} />
      <ComputationTable label="Taxable income this year (next year's bill)" computation={section.generatedThisYear} />
    </Box>
    {section.simplifications.length > 0 && (
      <Box>
        <Typography variant="subtitle2">How the simulation simplifies taxes</Typography>
        <ul style={{ margin: 0 }}>
          {section.simplifications.map(s => <li key={s}><Typography variant="body2">{s}</Typography></li>)}
        </ul>
      </Box>
    )}
  </Box>
);

export default TaxesSection;
