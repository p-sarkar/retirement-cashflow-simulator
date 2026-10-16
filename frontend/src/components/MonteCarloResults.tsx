import React from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography
} from '@mui/material';
import { MonteCarloResult, MonteCarloRunDetail } from '../types/simulation';
import MonteCarloChart, { CONFIDENCE_LINES } from './MonteCarloChart';
import ResultsTable from './ResultsTable';
import { compactMoney } from '../utils/monteCarloChart';

interface MonteCarloResultsProps {
  result: MonteCarloResult;
  selectedRunIndex: number | null;
  onSelectRun: (runIndex: number) => void;
  detail: MonteCarloRunDetail | null;
  detailLoading?: boolean;
  detailError?: string | null;
  onLoadDetails: (runIndex: number) => void;
  chartWidth?: number; // fixed chart size for tests
}

const percent = (decimal: number, digits = 1) => `${(decimal * 100).toFixed(digits)}%`;

const StatTile: React.FC<{ label: string; value: string; note?: string }> = ({ label, value, note }) => (
  <Box sx={{ minWidth: 170 }}>
    <Typography variant="body2" color="text.secondary">{label}</Typography>
    <Typography variant="h5">{value}</Typography>
    {note && <Typography variant="caption" color="text.secondary">{note}</Typography>}
  </Box>
);

/** Confidence-line values every 5 years and at the end: the chart's table view. */
function milestoneIndexes(yearCount: number): number[] {
  const indexes = [];
  for (let i = 0; i < yearCount; i += 5) indexes.push(i);
  if (indexes[indexes.length - 1] !== yearCount - 1) indexes.push(yearCount - 1);
  return indexes;
}

/** Monte Carlo analysis (US002-3) with the drill-down into one run (US002-4). */
const MonteCarloResults: React.FC<MonteCarloResultsProps> = ({
  result, selectedRunIndex, onSelectRun, detail, detailLoading = false, detailError, onLoadDetails, chartWidth
}) => {
  const { statistics, years, paths } = result;
  const last = years.length - 1;
  const selected = selectedRunIndex === null ? null : paths[selectedRunIndex] ?? null;
  const failures = paths.filter(p => !p.isSuccess).length;
  const lines = {
    median: statistics.medianPath,
    p75: statistics.percentile75Path,
    p90: statistics.percentile90Path
  };
  const showingDetail = detail && selected && detail.path.runIndex === selected.runIndex;

  return (
    <Paper sx={{ p: 2, mt: 3 }}>
      <Typography variant="h5" sx={{ mb: 2 }}>Monte Carlo Analysis</Typography>

      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 4, mb: 2 }}>
        <StatTile label="Success rate" value={percent(statistics.successRate)}
          note={`${paths.length - failures} of ${paths.length} runs reached End Age`} />
        <StatTile label={`Median balance in ${years[last]}`} value={compactMoney(statistics.medianPath[last])} />
        <StatTile label={`90% of runs end above`} value={compactMoney(statistics.percentile90Path[last])} note="The conservative outcome" />
      </Box>

      <MonteCarloChart result={result} selectedRunIndex={selectedRunIndex} onSelectRun={onSelectRun} width={chartWidth} />

      <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 1 }}>
        {result.method} Seed {result.seed}. A failed run counts as $0 after it fails.
      </Typography>

      <TableContainer sx={{ mt: 2 }}>
        <Table size="small" aria-label="Confidence lines by year">
          <TableHead>
            <TableRow>
              <TableCell>Total balance</TableCell>
              {milestoneIndexes(years.length).map(i => <TableCell key={i} align="right">{years[i]}</TableCell>)}
            </TableRow>
          </TableHead>
          <TableBody>
            {CONFIDENCE_LINES.map(line => (
              <TableRow key={line.key}>
                <TableCell>{line.label}</TableCell>
                {milestoneIndexes(years.length).map(i => (
                  <TableCell key={i} align="right">{compactMoney(lines[line.key][i])}</TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {selected && (
        <Paper variant="outlined" sx={{ p: 2, mt: 2 }} data-testid="selected-run-summary">
          <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 4 }}>
            <Typography variant="h6">Run {selected.runIndex + 1}</Typography>
            <StatTile label="Outcome" value={selected.isSuccess ? 'Success' : `Failed in ${selected.failureYear}`} />
            <StatTile label="Ending balance" value={compactMoney(selected.endingBalance)} />
            <StatTile label="Avg. market return" value={percent(selected.averageReturn)} />
            <StatTile label="Avg. inflation" value={percent(selected.averageInflation)} />
            <StatTile label="Avg. bond yield" value={percent(selected.averageBondYield)} />
            <Button
              variant="contained"
              onClick={() => onLoadDetails(selected.runIndex)}
              disabled={detailLoading || !!showingDetail}
              startIcon={detailLoading ? <CircularProgress size={16} color="inherit" /> : undefined}
            >
              {detailLoading ? 'Loading…' : 'Load Details'}
            </Button>
          </Box>
        </Paper>
      )}

      {detailError && <Alert severity="error" sx={{ mt: 2 }}>{detailError}</Alert>}

      {showingDetail && (
        <Box sx={{ mt: 2 }}>
          <Typography variant="h6">Run {detail.path.runIndex + 1}: market variables</Typography>
          <TableContainer sx={{ overflowX: 'auto' }}>
            <Table size="small" aria-label="Market variables by year">
              <TableHead>
                <TableRow>
                  <TableCell>Variable</TableCell>
                  {detail.scenario.years.slice(1).map(year => <TableCell key={year} align="right">{year}</TableCell>)}
                </TableRow>
              </TableHead>
              <TableBody>
                {([
                  ['Market return', detail.scenario.equityReturns],
                  ['Bond yield', detail.scenario.bondYields],
                  ['Inflation', detail.scenario.inflationRates]
                ] as const).map(([label, values]) => (
                  <TableRow key={label}>
                    <TableCell>{label}</TableCell>
                    {values.slice(1).map((value, i) => <TableCell key={i} align="right">{percent(value)}</TableCell>)}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          <ResultsTable
            result={detail.result}
            config={detail.result.config}
            title={`Run ${detail.path.runIndex + 1}: cash flow`}
            showBreakdown={false}
          />
        </Box>
      )}
    </Paper>
  );
};

export default MonteCarloResults;
