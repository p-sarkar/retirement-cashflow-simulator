import { describe, expect, it, vi } from 'vitest';
import React from 'react';
import { render, screen } from '@testing-library/react';
import GuardrailsSummary from './GuardrailsSummary';
import { guardrailsResult } from '../test/fixtures';

// jsdom has no layout: give charts a fixed size
vi.mock('recharts', async importOriginal => ({
  ...(await importOriginal<typeof import('recharts')>()),
  ResponsiveContainer: ({ children }: { children: React.ReactElement<{ width?: number; height?: number }> }) =>
    React.cloneElement(children, { width: 600, height: 300 })
}));

describe('GuardrailsSummary (spec 006, FR006-018/FR006-019)', () => {
  it('shows the starting probability, cuts and raises, the largest cut and the spending range', () => {
    render(<GuardrailsSummary result={guardrailsResult()} />);
    expect(screen.getByText('Starting probability of success')).toBeInTheDocument();
    expect(screen.getByText('74.4%')).toBeInTheDocument();
    expect(screen.getByText('1 cut · 0 raises')).toBeInTheDocument();
    expect(screen.getByText('22.1%')).toBeInTheDocument();
    expect(screen.getByText('$82,500 – $105,000')).toBeInTheDocument();
  });

  it('charts Wants against configured Wants and the probability against the guardrails', () => {
    render(<GuardrailsSummary result={guardrailsResult()} />);
    expect(screen.getByText('Wants per year')).toBeInTheDocument();
    expect(screen.getByText('Probability of success at each review')).toBeInTheDocument();
    expect(screen.getByText('Target 85%')).toBeInTheDocument(); // the fixture's settings: 85 / 97 / 60
    expect(screen.getByText(/Guardrails 60%–97%/)).toBeInTheDocument();
  });

  it('warns when the configured spending was cut at the first review', () => {
    const result = guardrailsResult();
    result.summary.guardrails!.configuredSpendingUnreachable = true;
    render(<GuardrailsSummary result={result} />);
    expect(screen.getByText(/configured spending could not be supported/)).toBeInTheDocument();
  });

  it('renders nothing for the existing strategy', () => {
    const result = guardrailsResult();
    result.summary.guardrails = null;
    const { container } = render(<GuardrailsSummary result={result} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('states the starting guardrails in dollars (US006-3)', () => {
    const result = guardrailsResult();
    const first = result.yearlyResults[1].guardrails!.reviews[0];
    Object.assign(first, { lowerGuardrailPortfolio: 2800000, wantsAtLowerGuardrail: 21000, upperGuardrailPortfolio: 4600000, wantsAtUpperGuardrail: 52000 });
    Object.assign(result.summary.guardrails!, { startingLowerGuardrailPortfolio: 2800000, startingUpperGuardrailPortfolio: 4600000 });
    render(<GuardrailsSummary result={result} />);
    expect(screen.getByText(
      'If your portfolio falls below $2,800,000, Wants drop to $21,000 a year; above $4,600,000, they rise to $52,000 a year.'
    )).toBeInTheDocument();
  });

  it('leaves out a guardrail that no portfolio reaches', () => {
    const result = guardrailsResult();
    const first = result.yearlyResults[1].guardrails!.reviews[0];
    Object.assign(first, { lowerGuardrailPortfolio: 2800000, wantsAtLowerGuardrail: 21000 });
    Object.assign(result.summary.guardrails!, { startingLowerGuardrailPortfolio: 2800000 });
    render(<GuardrailsSummary result={result} />);
    expect(screen.getByText('If your portfolio falls below $2,800,000, Wants drop to $21,000 a year.')).toBeInTheDocument();
  });

  it('has no dollar sentence when the guardrails were not computed', () => {
    render(<GuardrailsSummary result={guardrailsResult()} />);
    expect(screen.queryByText(/If your portfolio/)).not.toBeInTheDocument();
  });
});

