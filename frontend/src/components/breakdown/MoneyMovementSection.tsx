import React from 'react';
import { Alert, Box, List, ListItem, ListItemText, Typography } from '@mui/material';
import { MoneyMovementSection as Section } from '../../types/simulation';
import AccountLedgerTable from './AccountLedgerTable';
import MoneyFlowSankey from './MoneyFlowSankey';
import { formatMoney } from './format';

interface Props {
  section: Section;
  highlightQuarter?: number; // 1–4, when opened from a quarterly row
}

/** Section 5: the year's flows, each quarter's strategy decision and a ledger per account (FR004-050–FR004-055). */
const MoneyMovementSection: React.FC<Props> = ({ section, highlightQuarter }) => (
  <Box>
    {section.failure && (
      <Alert severity="error" sx={{ mb: 2 }}>
        Q{section.failure.quarter}: {section.failure.condition}
      </Alert>
    )}

    <Typography variant="subtitle2" gutterBottom>Where money came from and went this year</Typography>
    <MoneyFlowSankey flows={section.flows} />

    {section.quarterStrategy.length > 0 && (
      <Box sx={{ my: 2 }}>
        <Typography variant="subtitle2">Spending strategy each quarter</Typography>
        <List dense disablePadding>
          {section.quarterStrategy.map(q => (
            <ListItem key={q.quarter} disableGutters sx={q.quarter === highlightQuarter ? { backgroundColor: 'action.selected' } : undefined}>
              <ListItemText
                primary={`Q${q.quarter}: ${q.explanation}`}
                secondary={`Refill to SB ${formatMoney(q.refillToSb)}, to CBB ${formatMoney(q.refillToCbb)}`}
              />
            </ListItem>
          ))}
        </List>
      </Box>
    )}

    <Typography variant="subtitle2" gutterBottom>Ledgers (+ into the account, − out of it)</Typography>
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' }, gap: 2 }}>
      {section.ledgers.map(l => <AccountLedgerTable key={l.account} ledger={l} highlightQuarter={highlightQuarter} />)}
    </Box>
  </Box>
);

export default MoneyMovementSection;
