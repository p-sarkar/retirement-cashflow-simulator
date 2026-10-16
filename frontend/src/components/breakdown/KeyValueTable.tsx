import React from 'react';
import { Paper, Table, TableBody, TableCell, TableContainer, TableRow } from '@mui/material';

/** Label/value rows; numbers in a monospace font. */
const KeyValueTable: React.FC<{ rows: [string, string][]; label?: string }> = ({ rows, label }) => (
  <TableContainer component={Paper} variant="outlined">
    <Table size="small" aria-label={label}>
      <TableBody>
        {rows.map(([label, value]) => (
          <TableRow key={label}>
            <TableCell sx={{ width: '55%' }}>{label}</TableCell>
            <TableCell align="right" sx={/^[-$×\d]/.test(value) ? { fontFamily: 'monospace' } : undefined}>{value}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  </TableContainer>
);

export default KeyValueTable;
