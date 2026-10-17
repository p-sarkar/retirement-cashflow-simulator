import React from 'react';
import { Box, Typography } from '@mui/material';
import { StartingPosition } from '../../types/simulation';
import { formatMoney, formatPercent } from './format';
import KeyValueTable from './KeyValueTable';

const BRACKETS = {
  PRE_RETIREMENT: 'Pre-retirement',
  POST_RETIREMENT_PRE_MEDICARE: 'Post-retirement, before Medicare',
  MEDICARE: 'Medicare'
} as const;

const RETURN_SOURCES = {
  PRE_RETIREMENT_RATE: 'pre-retirement growth rate',
  POST_RETIREMENT_RATE: 'post-retirement growth rate',
  PRESET_SEQUENCE: 'preset return sequence',
  MONTE_CARLO_SCENARIO: 'Monte Carlo scenario'
} as const;

/** Section 1: prior year-end balances, the year's economic conditions and the household (FR004-010–FR004-013). */
const StartingPositionSection: React.FC<{ position: StartingPosition }> = ({ position }) => {
  const { openingBalances: b, conditions: c, household } = position;
  const returnSource =
    c.equityReturnSource === 'MONTE_CARLO_SCENARIO' ? `sampled for Monte Carlo run ${c.monteCarloRun}`
      : c.equityReturnSource === 'PRESET_SEQUENCE' ? `from the ${c.presetName} preset, year ${c.presetSequenceYear} of its sequence`
        : `from the configured ${RETURN_SOURCES[c.equityReturnSource]}`;

  return (
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr 1fr' }, gap: 2 }}>
      <Box>
        <Typography variant="subtitle2" gutterBottom>Opening balances (prior year end)</Typography>
        <KeyValueTable rows={[
          ['Spend Bucket', formatMoney(b.sb)],
          ['Crash Buffer', formatMoney(b.cbb)],
          ['Taxable', formatMoney(b.tba)],
          ['Tax Deferred', formatMoney(b.tda)],
          ['Tax Free', formatMoney(b.tfa)],
          ['Total', formatMoney(position.openingTotal)]
        ]} />
      </Box>
      <Box>
        <Typography variant="subtitle2" gutterBottom>Economic conditions this year</Typography>
        <KeyValueTable rows={[
          ['Inflation', formatPercent(c.inflationRate)],
          ['Cumulative inflation since start', `× ${c.cumulativeInflation.toFixed(4)}`],
          ['Equity return', formatPercent(c.equityReturn)],
          ['Bond yield', formatPercent(c.bondYield)],
          ['HYSA rate', formatPercent(c.hysaRate)],
          ['Income tax rate', formatPercent(c.incomeTaxRate)],
          ['Market vs all-time high (year start)', formatPercent(c.marketVsAllTimeHigh)]
        ]} />
        <Typography variant="caption" color="text.secondary">Equity return {returnSource}.</Typography>
      </Box>
      <Box>
        <Typography variant="subtitle2" gutterBottom>Household</Typography>
        <KeyValueTable rows={[
          ['Healthcare', BRACKETS[household.healthcareBracket]],
          ...household.earners.map(e => [
            `${e.role === 'HIGHER' ? 'Higher' : 'Lower'} earner (age ${e.age})`,
            e.claiming ? 'Claiming Social Security' : `Claims at ${e.claimAge}`
          ] as [string, string])
        ]} />
      </Box>
    </Box>
  );
};

export default StartingPositionSection;
