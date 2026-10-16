import React from 'react';
import { Box, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { IncomeGapSection as Section } from '../../types/simulation';
import LineItemTable from './LineItemTable';
import { formatMoney, formatPercent } from './format';

/** Section 4: the income gap and the caps the spending strategy derives from it (FR004-040–FR004-042, FR004-007). */
const IncomeGapSection: React.FC<{ section: Section }> = ({ section }) => {
  const { estimated, actual } = section;
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="body2">{section.explanation}</Typography>

      {section.takeHomeSalary && section.workingExpenses && (
        <LineItemTable label="Salary against expenses" items={[section.takeHomeSalary, section.workingExpenses]} />
      )}

      {estimated && actual && (
        <>
          <TableContainer component={Paper} variant="outlined">
            <Table size="small" aria-label="Estimated and actual income gap">
              <TableHead>
                <TableRow>
                  <TableCell />
                  <TableCell align="right" sx={{ fontWeight: 'bold' }}>Estimated (start of year)</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 'bold' }}>Actual (year end)</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {([['Expenses', 'expenses'], ['Passive income', 'passiveIncome'], ['Income gap', 'gap']] as const).map(([label, key]) => (
                  <TableRow key={key}>
                    <TableCell component="th" scope="row" sx={{ fontWeight: key === 'gap' ? 'bold' : undefined }}>{label}</TableCell>
                    <TableCell align="right" sx={{ fontFamily: 'monospace' }}>{formatMoney(estimated[key])}</TableCell>
                    <TableCell align="right" sx={{ fontFamily: 'monospace' }}>{formatMoney(actual[key])}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>

          <Box>
            <Typography variant="subtitle2" gutterBottom>Why the actual gap differs from the estimate</Typography>
            <LineItemTable label="Gap differences" items={section.differences} />
          </Box>

          <Box>
            <Typography variant="subtitle2" gutterBottom>Caps and floor</Typography>
            <LineItemTable label="Caps and floor" items={[section.capAig, section.sbFloor, section.sbCap, section.cbbCap].filter(i => i != null)} />
            {section.cbbCapReductions.length > 0 && (
              <Typography variant="caption" color="text.secondary">
                CBB cap reduced at age{section.cbbCapReductions.length > 1 ? 's' : ''} {section.cbbCapReductions.join(', ')}.
              </Typography>
            )}
          </Box>

          <Box>
            <Typography variant="subtitle2">Market thresholds for the spending strategy</Typography>
            {section.thresholds.map(t => (
              <Typography key={t.name} variant="body2" color={t.evaluated ? undefined : 'text.secondary'}>
                {t.name}: {formatPercent(t.value)}
              </Typography>
            ))}
          </Box>
        </>
      )}
    </Box>
  );
};

export default IncomeGapSection;
