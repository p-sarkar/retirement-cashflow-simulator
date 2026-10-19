import React from 'react';
import { Alert, Box, Button, Table, TableBody, TableCell, TableHead, TableRow, ToggleButton, ToggleButtonGroup } from '@mui/material';
import { Summary } from '../types/simulation';

export type ResultsShown = 'saved' | 'rerun';

interface RerunNoticeProps {
  savedVersion: string | null; // the version that computed the saved results; null if unknown
  runningVersion: string;
  saved: Summary;
  rerun: Summary | null; // null until re-run
  showing: ResultsShown;
  running?: boolean;
  onRerun: () => void;
  onShow: (which: ResultsShown) => void;
}

const money = (amount: number, signed = false) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency', currency: 'USD', maximumFractionDigits: 0, signDisplay: signed ? 'exceptZero' : 'auto'
  }).format(amount);

const outcome = (summary: Summary) => (summary.isSuccess ? 'Success' : 'Failed');

/**
 * Saved results computed by another version (US005-1): says so, re-runs the saved inputs with the running version,
 * then compares both and switches between them (FR005-002 to FR005-004).
 */
const RerunNotice: React.FC<RerunNoticeProps> = ({
  savedVersion, runningVersion, saved, rerun, showing, running = false, onRerun, onShow
}) => {
  const savedLabel = savedVersion ? `v${savedVersion}` : 'unknown version';
  return (
    <Alert severity="warning" sx={{ mt: 2 }}>
      These results were computed by {savedVersion ? `v${savedVersion}` : 'an unknown version'}. This app is
      v{runningVersion}, which may give different results.
      <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap', mt: 1 }}>
        <Button size="small" variant="outlined" color="inherit" onClick={onRerun} disabled={running}>
          {running ? 'Re-running…' : `Re-run with v${runningVersion}`}
        </Button>
        {rerun && (
          <ToggleButtonGroup size="small" exclusive value={showing} onChange={(_e, value: ResultsShown | null) => value && onShow(value)}>
            <ToggleButton value="saved">Saved ({savedLabel})</ToggleButton>
            <ToggleButton value="rerun">Re-run (v{runningVersion})</ToggleButton>
          </ToggleButtonGroup>
        )}
      </Box>
      {rerun && (
        <Table size="small" aria-label="Saved and re-run results" sx={{ mt: 1, width: 'auto' }}>
          <TableHead>
            <TableRow>
              <TableCell />
              <TableCell>Saved</TableCell>
              <TableCell>Re-run</TableCell>
              <TableCell>Difference</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            <TableRow>
              <TableCell component="th" scope="row">Outcome</TableCell>
              <TableCell>{outcome(saved)}</TableCell>
              <TableCell>{outcome(rerun)}</TableCell>
              <TableCell />
            </TableRow>
            <TableRow>
              <TableCell component="th" scope="row">Failure year</TableCell>
              <TableCell>{saved.failureYear ?? '—'}</TableCell>
              <TableCell>{rerun.failureYear ?? '—'}</TableCell>
              <TableCell />
            </TableRow>
            <TableRow>
              <TableCell component="th" scope="row">Ending balance</TableCell>
              <TableCell>{money(saved.finalTotalBalance)}</TableCell>
              <TableCell>{money(rerun.finalTotalBalance)}</TableCell>
              <TableCell>{money(rerun.finalTotalBalance - saved.finalTotalBalance, true)}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      )}
    </Alert>
  );
};

export default RerunNotice;
