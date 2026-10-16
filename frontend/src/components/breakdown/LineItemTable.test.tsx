import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import LineItemTable from './LineItemTable';

describe('LineItemTable (FR004-003, FR004-005)', () => {
  it('shows each item with its amount, how it was computed, its inputs and today\'s dollars', () => {
    render(<LineItemTable label="Expenses" items={[
      { label: 'Needs', amount: 56275, rule: 'Configured amount × cumulative inflation', inputs: { inflationFactor: 1.1255 }, todaysDollars: 50000 },
      { label: 'Income Tax', amount: 30000, rule: "Last year's taxable income × the effective tax rate", inputs: {}, todaysDollars: null }
    ]} />);

    expect(screen.getByRole('table', { name: 'Expenses' })).toBeInTheDocument();
    expect(screen.getByRole('rowheader', { name: 'Needs' })).toBeInTheDocument();
    expect(screen.getByText('$56,275')).toBeInTheDocument();
    expect(screen.getByText('Configured amount × cumulative inflation')).toBeInTheDocument();
    expect(screen.getByText('inflationFactor: 1.1255')).toBeInTheDocument();
    expect(screen.getByText("$50,000 in today's dollars")).toBeInTheDocument();
  });
});
