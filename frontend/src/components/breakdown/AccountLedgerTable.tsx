import React from 'react';
import { Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { AccountLedger } from '../../types/simulation';
import { ENDPOINT_LABELS, MOVEMENT_LABELS, formatMoney } from './format';

interface Props {
  ledger: AccountLedger;
  highlightQuarter?: number; // 1–4
}

/** One account's quarter-by-quarter ledger: opening, each movement, closing (FR004-050, FR004-053). */
const AccountLedgerTable: React.FC<Props> = ({ ledger, highlightQuarter }) => {
  const name = ENDPOINT_LABELS[ledger.account];
  const shade = (q: number) => (q === highlightQuarter ? { backgroundColor: 'action.selected' } : {});
  const moneyRow = (label: string, values: number[], year: number, bold = false) => (
    <TableRow key={label}>
      <TableCell component="th" scope="row" sx={{ fontWeight: bold ? 'bold' : undefined }}>{label}</TableCell>
      {values.map((v, i) => (
        <TableCell key={i} align="right" sx={{ fontFamily: 'monospace', fontWeight: bold ? 'bold' : undefined, color: v < 0 ? 'error.main' : undefined, ...shade(i + 1) }}>
          {formatMoney(v)}
        </TableCell>
      ))}
      <TableCell align="right" sx={{ fontFamily: 'monospace', fontWeight: 'bold' }}>{formatMoney(year)}</TableCell>
    </TableRow>
  );

  return (
    <TableContainer component={Paper} variant="outlined">
      <Typography variant="subtitle2" sx={{ px: 2, pt: 1 }}>{name}</Typography>
      <Table size="small" aria-label={`${name} ledger`}>
        <TableHead>
          <TableRow>
            <TableCell />
            {[1, 2, 3, 4].map(q => (
              <TableCell key={q} align="right" aria-current={q === highlightQuarter ? 'true' : undefined} sx={{ fontWeight: 'bold', ...shade(q) }}>
                Q{q}
              </TableCell>
            ))}
            <TableCell align="right" sx={{ fontWeight: 'bold' }}>Year</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {moneyRow('Opening', ledger.opening, ledger.opening[0], true)}
          {ledger.rows.map(r =>
            moneyRow(
              `${MOVEMENT_LABELS[r.type]} ${r.yearTotal >= 0 ? 'from' : 'to'} ${ENDPOINT_LABELS[r.counterparty]}`,
              r.amounts,
              r.yearTotal
            )
          )}
          {moneyRow('Closing', ledger.closing, ledger.closing[3], true)}
        </TableBody>
      </Table>
    </TableContainer>
  );
};

export default AccountLedgerTable;
