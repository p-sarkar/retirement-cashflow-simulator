import React from 'react';
import { Alert, Box, Paper, Typography } from '@mui/material';
import {
  CartesianGrid, Legend, Line, LineChart, ReferenceArea, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis
} from 'recharts';
import { SimulationResult } from '../types/simulation';
import { compactMoney } from '../utils/monteCarloChart';
import { OUTCOME_LABELS } from '../utils/resultColumns';
import { dollarGuardrailsSentence, money } from '../utils/guardrails';

interface GuardrailsSummaryProps {
  result: SimulationResult;
}

// Categorical slots 1-2 of the app's chart palette (validated: CVD and normal-vision separation pass);
// configured Wants is also dashed, so identity never rests on color alone
const COLORS = { wants: '#2a78d6', configured: '#eb6834', band: '#9e9e9e', grid: '#e0e0e0', text: '#5f5f5f' };
const CHART_HEIGHT = 220;

const percent = (decimal: number) => `${(decimal * 100).toFixed(1)}%`;
const wholePercent = (decimal: number) => `${Math.round(decimal * 100)}%`;

const StatTile: React.FC<{ label: string; value: string; note?: string }> = ({ label, value, note }) => (
  <Box sx={{ minWidth: 170 }}>
    <Typography variant="body2" color="text.secondary">{label}</Typography>
    <Typography variant="h5">{value}</Typography>
    {note && <Typography variant="caption" color="text.secondary">{note}</Typography>}
  </Box>
);

/** A chart that fills its container; jsdom tests stub ResponsiveContainer with a fixed size. */
const Chart: React.FC<{ title: string; children: React.ReactElement; caption?: React.ReactNode }> = ({ title, children, caption }) => (
  <Box sx={{ flex: '1 1 420px', minWidth: 320 }}>
    <Typography variant="subtitle2" fontWeight="bold">{title}</Typography>
    {caption && <Typography variant="caption" color="text.secondary" component="p">{caption}</Typography>}
    <ResponsiveContainer width="100%" height={CHART_HEIGHT}>{children}</ResponsiveContainer>
  </Box>
);

/**
 * Risk-Based Guardrails results (spec 006, FR006-018/FR006-019): the summary tiles, Wants against the configured
 * Wants, and each review's probability of success against the guardrails. The results table's guardrails columns
 * are the table view of both charts.
 */
const GuardrailsSummary: React.FC<GuardrailsSummaryProps> = ({ result }) => {
  const summary = result.summary.guardrails;
  if (!summary) return null;
  const settings = {
    targetSuccess: 0.9, upperGuardrail: 0.99, lowerGuardrail: 0.7, reviewFrequency: 'YEARLY',
    ...result.config.strategy.guardrails
  };

  const years = result.yearlyResults.filter(row => row.guardrails);
  const wantsData = years.map(row => ({ year: row.year, wants: row.cashFlow.wants, configured: row.guardrails!.configuredWants }));
  const reviews = years.flatMap(row => row.guardrails!.reviews);
  const quarterly = settings.reviewFrequency === 'QUARTERLY';
  const dollarSentence = dollarGuardrailsSentence(reviews[0]);
  const reviewData = reviews.map(r => ({
    label: quarterly ? `${r.year} Q${r.quarter}` : `${r.year}`,
    probability: r.probabilityBefore * 100,
    outcome: OUTCOME_LABELS[r.outcome],
    after: r.probabilityAfter * 100,
    dollars: dollarGuardrailsSentence(r)
  }));

  return (
    <Paper sx={{ p: 2, mt: 3 }}>
      <Typography variant="h5" sx={{ mb: 2 }}>Risk-Based Guardrails</Typography>
      {summary.configuredSpendingUnreachable && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          The configured spending could not be supported at the {wholePercent(settings.targetSuccess)} target:
          the first review, at the start of retirement, cut Wants.
        </Alert>
      )}
      {dollarSentence && (
        <Typography variant="body1" sx={{ mb: 2 }}>{dollarSentence}</Typography>
      )}
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 4, mb: 2 }}>
        <StatTile label="Starting probability of success" value={percent(summary.startingProbability)} note="At the first review" />
        <StatTile label="Adjustments" value={`${summary.cuts} cut${summary.cuts === 1 ? '' : 's'} · ${summary.raises} raise${summary.raises === 1 ? '' : 's'}`} />
        <StatTile label="Largest cut" value={summary.largestCutPercent == null ? 'None' : percent(summary.largestCutPercent)}
          note="Share of that year's spending" />
        <StatTile label="Annual spending (today's $)" value={`${money(summary.lowestAnnualSpending)} – ${money(summary.highestAnnualSpending)}`}
          note="Needs, Wants, healthcare, property tax" />
      </Box>

      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
        <Chart title="Wants per year">
          <LineChart data={wantsData} margin={{ top: 8, right: 16, bottom: 0, left: 8 }}>
            <CartesianGrid stroke={COLORS.grid} vertical={false} />
            <XAxis dataKey="year" tick={{ fill: COLORS.text, fontSize: 12 }} />
            <YAxis tickFormatter={compactMoney} tick={{ fill: COLORS.text, fontSize: 12 }} width={64} />
            <Tooltip formatter={value => money(Number(value))} />
            <Legend />
            <Line type="monotone" dataKey="wants" name="Wants" stroke={COLORS.wants} strokeWidth={2} dot={false} isAnimationActive={false} />
            <Line type="monotone" dataKey="configured" name="Configured Wants" stroke={COLORS.configured} strokeWidth={2}
              strokeDasharray="6 3" dot={false} isAnimationActive={false} />
          </LineChart>
        </Chart>

        <Chart
          title="Probability of success at each review"
          caption={<><span>Target {wholePercent(settings.targetSuccess)}</span>{' · '}
            <span>Guardrails {wholePercent(settings.lowerGuardrail)}–{wholePercent(settings.upperGuardrail)} (shaded)</span></>}
        >
          <LineChart data={reviewData} margin={{ top: 8, right: 16, bottom: 0, left: 8 }}>
            <CartesianGrid stroke={COLORS.grid} vertical={false} />
            <ReferenceArea y1={settings.lowerGuardrail * 100} y2={settings.upperGuardrail * 100} fill={COLORS.band} fillOpacity={0.15} />
            <ReferenceLine y={settings.targetSuccess * 100} stroke={COLORS.band} strokeDasharray="4 4" />
            <XAxis dataKey="label" tick={{ fill: COLORS.text, fontSize: 12 }} />
            <YAxis domain={[0, 100]} tickFormatter={(v: number) => `${v}%`} tick={{ fill: COLORS.text, fontSize: 12 }} width={48} />
            <Tooltip formatter={value => `${Number(value).toFixed(1)}%`}
              labelFormatter={(label, payload) => {
                const p = payload?.[0]?.payload as { outcome: string; after: number; dollars: string | null } | undefined;
                if (!p) return String(label);
                const change = p.outcome === 'No change' ? '' : `, then ${p.after.toFixed(1)}%`;
                return `${label}: ${p.outcome}${change}${p.dollars ? `. ${p.dollars}` : ''}`;
              }} />
            <Line type="monotone" dataKey="probability" name="Probability of success" stroke={COLORS.wants} strokeWidth={2}
              dot={{ r: 4 }} activeDot={{ r: 5 }} isAnimationActive={false} />
          </LineChart>
        </Chart>
      </Box>
    </Paper>
  );
};

export default GuardrailsSummary;
