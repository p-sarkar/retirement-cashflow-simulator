import React from 'react';
import { Box, Typography } from '@mui/material';
import { IncomeSection as Section } from '../../types/simulation';
import KeyValueTable from './KeyValueTable';
import LineItemTable from './LineItemTable';
import { formatMoney } from './format';

/** Section 3: income, kept apart from money moved out of investment accounts (FR004-030–FR004-032). */
const IncomeSection: React.FC<{ section: Section }> = ({ section }) => {
  const s = section.salary;
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {s && (
        <Box>
          <Typography variant="subtitle2" gutterBottom>Salary</Typography>
          <KeyValueTable label="Salary" rows={[
            ['Gross salary', formatMoney(s.gross)],
            ['401k contribution (to Tax Deferred)', formatMoney(-s.contribution401k)],
            ['Roth contribution (to Tax Free)', formatMoney(-s.contributionRoth)],
            ['Taxable contribution (to Taxable)', formatMoney(-s.contributionTba)],
            ['Take-home salary (to the Spend Bucket)', formatMoney(s.takeHome)]
          ]} />
        </Box>
      )}
      <Box>
        <Typography variant="subtitle2" gutterBottom>Investment income and Social Security</Typography>
        <LineItemTable label="Income" items={[section.interest, section.dividends]} />
        <KeyValueTable label="Social Security" rows={section.socialSecurity.map(e => [
          `${e.role === 'HIGHER' ? 'Higher' : 'Lower'} earner${e.spousalStepUp ? ' (spousal step-up)' : ''}` +
            (e.startYear ? `, claims from ${e.startYear}` : ''),
          formatMoney(e.amount)
        ] as [string, string])} />
        <Typography variant="body2" sx={{ mt: 1 }}>Income: <strong>{formatMoney(section.incomeTotal)}</strong></Typography>
      </Box>
      <Box>
        <Typography variant="subtitle2" gutterBottom>Withdrawals and transfers from investment accounts</Typography>
        <LineItemTable label="Withdrawals and transfers" items={section.withdrawals} />
      </Box>
      <Typography variant="subtitle1">Total income (as in the results table): <strong>{formatMoney(section.totalIncome)}</strong></Typography>
      {section.transfers.some(t => t.amount !== 0) && (
        <Box>
          <Typography variant="subtitle2" gutterBottom>Transfers between accounts (not income)</Typography>
          <LineItemTable label="Transfers between accounts" items={section.transfers} />
        </Box>
      )}
    </Box>
  );
};

export default IncomeSection;
