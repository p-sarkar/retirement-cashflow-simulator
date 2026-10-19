import { describe, expect, it, vi } from 'vitest';
import React from 'react';
import { render, screen } from '@testing-library/react';
import MonteCarloResults from './MonteCarloResults';
import { guardrailsMonteCarloResult, monteCarloResult } from '../test/fixtures';

vi.mock('recharts', async importOriginal => ({
  ...(await importOriginal<typeof import('recharts')>()),
  ResponsiveContainer: ({ children }: { children: React.ReactElement<{ width?: number; height?: number }> }) =>
    React.cloneElement(children, { width: 600, height: 300 })
}));

const props = { selectedRunIndex: null, onSelectRun: vi.fn(), detail: null, onLoadDetails: vi.fn(), chartWidth: 600 };

describe('MonteCarloResults with Risk-Based Guardrails (spec 006, FR006-021)', () => {
  it('shows the share of runs with a cut and spending percentiles in today\'s dollars', () => {
    render(<MonteCarloResults result={guardrailsMonteCarloResult()} {...props} />);
    expect(screen.getByText('Runs with a spending cut')).toBeInTheDocument();
    expect(screen.getByText('33.3%')).toBeInTheDocument();
    const table = screen.getByRole('table', { name: 'Annual spending by year' });
    expect(table).toHaveTextContent('10th percentile');
    expect(table).toHaveTextContent('$97K'); // median in 2028
    expect(table).toHaveTextContent('~'); // 2025, a working year
  });

  it('shows no spending statistics for the existing strategy', () => {
    render(<MonteCarloResults result={monteCarloResult()} {...props} />);
    expect(screen.queryByText('Runs with a spending cut')).not.toBeInTheDocument();
    expect(screen.queryByRole('table', { name: 'Annual spending by year' })).not.toBeInTheDocument();
  });
});
