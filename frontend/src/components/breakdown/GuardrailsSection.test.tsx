import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import GuardrailsSection from './GuardrailsSection';
import { GuardrailReview, GuardrailsSection as Section } from '../../types/simulation';

const review = (overrides: Partial<GuardrailReview> = {}): GuardrailReview => ({
  year: 2032, quarter: 1, age: 57, portfolioValue: 3402171, probabilityBefore: 0.428, outcome: 'CUT',
  wantsBefore: 107464, wantsAfter: 26866, spendingBefore: 200000, probabilityAfter: 0.902,
  lowerGuardrailPortfolio: 4700000, upperGuardrailPortfolio: 7900000, wantsAtLowerGuardrail: 60000, wantsAtUpperGuardrail: 160000,
  ...overrides
});

const section = (overrides: Partial<Section> = {}): Section => ({
  targetSuccess: 0.9, upperGuardrail: 0.99, lowerGuardrail: 0.7, reviewFrequency: 'YEARLY',
  reviews: [review()],
  configuredWants: { label: 'Configured Wants', amount: 107464, rule: 'Configured amount × cumulative inflation', inputs: {}, todaysDollars: 90000 },
  adjustedWants: { label: 'Wants (guardrails)', amount: 26866, rule: "Today's-dollar Wants × cumulative inflation", inputs: {}, todaysDollars: 22500 },
  noReviewNote: null,
  ...overrides
});

describe('GuardrailsSection (spec 006, US006-4)', () => {
  it('shows the settings and explains a cut in words', () => {
    render(<GuardrailsSection section={section()} />);
    const settings = within(screen.getByRole('table', { name: 'Guardrail settings' }));
    expect(settings.getByText('90%')).toBeInTheDocument();
    expect(settings.getByText('70% – 99%')).toBeInTheDocument();
    expect(screen.getByText(
      'The probability of success was 42.8%, below the 70% lower guardrail, so Wants were cut from $107,464 to $26,866 a year, bringing it to 90.2%.'
    )).toBeInTheDocument();
  });

  it('lists each review with its dollar guardrails', () => {
    render(<GuardrailsSection section={section()} />);
    const table = within(screen.getByRole('table', { name: 'Review 2032' }));
    expect(table.getByText('$3,402,171')).toBeInTheDocument();
    expect(table.getByText('Cut')).toBeInTheDocument();
    expect(table.getByText('$4,700,000 → Wants $60,000')).toBeInTheDocument();
    expect(table.getByText('$7,900,000 → Wants $160,000')).toBeInTheDocument();
  });

  it('explains no change, and leaves out dollar guardrails that were not computed', () => {
    render(<GuardrailsSection section={section({ reviews: [review({
      outcome: 'NO_CHANGE', probabilityBefore: 0.82, probabilityAfter: 0.82, wantsAfter: 107464,
      lowerGuardrailPortfolio: null, upperGuardrailPortfolio: null, wantsAtLowerGuardrail: null, wantsAtUpperGuardrail: null
    })] })} />);
    expect(screen.getByText('The probability of success was 82.0%, between the guardrails, so Wants only grew with inflation.')).toBeInTheDocument();
    expect(screen.queryByText(/→ Wants/)).not.toBeInTheDocument();
  });

  it('shows the Wants line items and a note when quarterly reviews were cut short', () => {
    render(<GuardrailsSection section={section({ reviewFrequency: 'QUARTERLY',
      noReviewNote: "The plan failed during the year, so the reviews after Q2 didn't happen." })} />);
    expect(within(screen.getByRole('table', { name: 'Wants' })).getByText('Wants (guardrails)')).toBeInTheDocument();
    expect(screen.getByText("The plan failed during the year, so the reviews after Q2 didn't happen.")).toBeInTheDocument();
    expect(screen.getByRole('table', { name: 'Review 2032 Q1' })).toBeInTheDocument();
  });
});
