import React from 'react';
import { Box, Paper, Table, TableBody, TableCell, TableContainer, TableRow, Typography } from '@mui/material';
import { LineItem } from '../../types/simulation';
import { formatMoney } from './format';

const formatInput = (v: number) =>
  Math.abs(v) >= 1000 ? formatMoney(v) : new Intl.NumberFormat('en-US', { maximumFractionDigits: 4 }).format(v);

/** Line items with their amount, how each was computed, its inputs and today's-dollar amount (FR004-003, FR004-005). */
const LineItemTable: React.FC<{ label: string; items: LineItem[] }> = ({ label, items }) => (
  <TableContainer component={Paper} variant="outlined">
    <Table size="small" aria-label={label}>
      <TableBody>
        {items.map((item, i) => (
          <TableRow key={`${item.label}-${i}`}>
            <TableCell component="th" scope="row" sx={{ width: '28%', fontWeight: 'medium' }}>{item.label}</TableCell>
            <TableCell align="right" sx={{ width: '16%', fontFamily: 'monospace', color: item.amount < 0 ? 'error.main' : undefined }}>
              {formatMoney(item.amount)}
            </TableCell>
            <TableCell>
              <Typography variant="caption" color="text.secondary" component="div">{item.rule}</Typography>
              {(Object.keys(item.inputs).length > 0 || item.todaysDollars != null) && (
                <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                  {Object.entries(item.inputs).map(([key, value]) => (
                    <Typography key={key} variant="caption" sx={{ fontFamily: 'monospace' }}>{key}: {formatInput(value)}</Typography>
                  ))}
                  {item.todaysDollars != null && (
                    <Typography variant="caption" sx={{ fontFamily: 'monospace' }}>{formatMoney(item.todaysDollars)} in today's dollars</Typography>
                  )}
                </Box>
              )}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  </TableContainer>
);

export default LineItemTable;
