import React from 'react';
import { Alert, Box, Typography } from '@mui/material';
import { GuardrailReview, GuardrailsSection as Section } from '../../types/simulation';
import { OUTCOME_LABELS } from '../../utils/resultColumns';
import KeyValueTable from './KeyValueTable';
import LineItemTable from './LineItemTable';
import { formatMoney } from './format';

const whole = (decimal: number) => `${Math.round(decimal * 100)}%`;
const oneDecimal = (decimal: number) => `${(decimal * 100).toFixed(1)}%`;

/** Why the review did what it did, in one sentence (SC006-006). */
function explanation(r: GuardrailReview, s: Section): string {
  const was = `The probability of success was ${oneDecimal(r.probabilityBefore)}`;
  const change = `from ${formatMoney(r.wantsBefore)} to ${formatMoney(r.wantsAfter)} a year`;
  switch (r.outcome) {
    case 'CUT':
      return `${was}, below the ${whole(s.lowerGuardrail)} lower guardrail, so Wants were cut ${change}, bringing it to ${oneDecimal(r.probabilityAfter)}.`;
    case 'TARGET_UNREACHABLE':
      return `${was}, below the ${whole(s.lowerGuardrail)} lower guardrail, and even no Wants can't reach the ${whole(s.targetSuccess)} target, ` +
        `so Wants were cut ${change}; it is now ${oneDecimal(r.probabilityAfter)}.`;
    case 'RAISE':
      return `${was}, above the ${whole(s.upperGuardrail)} upper guardrail, so Wants were raised ${change}, bringing it to ${oneDecimal(r.probabilityAfter)}.`;
    case 'RAISE_CAPPED':
      return `${was}, above the ${whole(s.upperGuardrail)} upper guardrail, so Wants were raised ${change}, the cap of 150% of the ` +
        `configured Wants; it is now ${oneDecimal(r.probabilityAfter)}.`;
    case 'NO_CHANGE':
      return r.probabilityBefore > s.upperGuardrail
        ? `${was}, above the ${whole(s.upperGuardrail)} upper guardrail, but Wants were already at the cap of 150% of the configured Wants, so they only grew with inflation.`
        : `${was}, between the guardrails, so Wants only grew with inflation.`;
  }
}

const dollarGuardrail = (portfolio: number | null, wants: number | null) =>
  portfolio != null && wants != null ? `${formatMoney(portfolio)} → Wants ${formatMoney(wants)}` : null;

/** The guardrails section of the yearly breakdown (spec 006, US006-4, FR006-020). */
const GuardrailsSection: React.FC<{ section: Section }> = ({ section: s }) => {
  const quarterly = s.reviewFrequency === 'QUARTERLY';
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <KeyValueTable label="Guardrail settings" rows={[
        ['Target probability of success', whole(s.targetSuccess)],
        ['Guardrails (lower – upper)', `${whole(s.lowerGuardrail)} – ${whole(s.upperGuardrail)}`],
        ['Review frequency', quarterly ? 'Quarterly' : 'Yearly']
      ]} />
      {s.reviews.map(r => {
        const label = quarterly ? `Review ${r.year} Q${r.quarter}` : `Review ${r.year}`;
        const lower = dollarGuardrail(r.lowerGuardrailPortfolio, r.wantsAtLowerGuardrail);
        const upper = dollarGuardrail(r.upperGuardrailPortfolio, r.wantsAtUpperGuardrail);
        const rows: [string, string][] = [
          ['Portfolio at the review', formatMoney(r.portfolioValue)],
          ['Probability of success', r.outcome === 'NO_CHANGE' ? oneDecimal(r.probabilityBefore)
            : `${oneDecimal(r.probabilityBefore)} → ${oneDecimal(r.probabilityAfter)}`],
          ['Outcome', OUTCOME_LABELS[r.outcome]],
          ['Wants (a year)', r.wantsAfter === r.wantsBefore ? formatMoney(r.wantsAfter)
            : `${formatMoney(r.wantsBefore)} → ${formatMoney(r.wantsAfter)}`],
          ...(lower ? [['If the portfolio falls to', lower] as [string, string]] : []),
          ...(upper ? [['If the portfolio rises to', upper] as [string, string]] : [])
        ];
        return (
          <Box key={label}>
            <Typography variant="subtitle2">{label}</Typography>
            <Typography variant="body2" sx={{ mb: 1 }}>{explanation(r, s)}</Typography>
            <KeyValueTable label={label} rows={rows} />
          </Box>
        );
      })}
      {s.noReviewNote && <Alert severity="info">{s.noReviewNote}</Alert>}
      <LineItemTable label="Wants" items={[s.configuredWants, s.adjustedWants]} />
    </Box>
  );
};

export default GuardrailsSection;
