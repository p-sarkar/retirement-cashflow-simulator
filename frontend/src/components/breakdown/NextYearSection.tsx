import React from 'react';
import {
  Box, Chip, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography
} from '@mui/material';
import { NextYearSection as Section, ProjectedItem } from '../../types/simulation';
import { formatMoney } from './format';

const signed = (amount: number) => (amount > 0 ? `+${formatMoney(amount)}` : formatMoney(amount));

const ProjectionTable: React.FC<{ label: string; items: ProjectedItem[] }> = ({ label, items }) => (
  <TableContainer component={Paper} variant="outlined">
    <Table size="small" aria-label={label}>
      <TableHead>
        <TableRow>
          <TableCell>{label}</TableCell>
          <TableCell align="right">Next year</TableCell>
          <TableCell align="right">Change</TableCell>
          <TableCell />
        </TableRow>
      </TableHead>
      <TableBody>
        {items.map(item => (
          <TableRow key={item.label}>
            <TableCell component="th" scope="row">{item.label}</TableCell>
            <TableCell align="right" sx={{ fontFamily: 'monospace' }}>{formatMoney(item.amount)}</TableCell>
            <TableCell align="right" sx={{ fontFamily: 'monospace', color: 'text.secondary' }}>{signed(item.change)}</TableCell>
            <TableCell>{!item.exact && <Chip label="Estimate" size="small" variant="outlined" />}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  </TableContainer>
);

interface Props {
  section: Section | null;
  isLastYear: boolean;
}

/** Section 7: next year's expenses, income and gap, from what is known at the end of this year (FR004-070–FR004-073). */
const NextYearSection: React.FC<Props> = ({ section, isLastYear }) => {
  if (!section) {
    return <Typography variant="body2">{isLastYear ? 'The simulation ends with this year.' : 'No projection for this year.'}</Typography>;
  }
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="subtitle2">Next year: {section.year}, age {section.age}</Typography>
      {section.changes.length > 0 && (
        <Box>
          <Typography variant="body2" fontWeight="medium">What changes</Typography>
          <ul style={{ margin: 0 }}>
            {section.changes.map(c => <li key={c}><Typography variant="body2">{c}</Typography></li>)}
          </ul>
        </Box>
      )}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
        <ProjectionTable label="Expenses" items={section.expenses} />
        <ProjectionTable label="Income" items={section.income} />
      </Box>
      {section.incomeGap && <ProjectionTable label="Income gap" items={[section.incomeGap]} />}
      <Typography variant="caption" color="text.secondary">
        Amounts that don't depend on the markets are exact; interest, dividends and the income gap are estimates.
      </Typography>
    </Box>
  );
};

export default NextYearSection;
