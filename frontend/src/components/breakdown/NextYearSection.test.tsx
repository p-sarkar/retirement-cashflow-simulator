import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import NextYearSection from './NextYearSection';

describe('NextYearSection (US004-5)', () => {
  it('shows projected expenses and income, marks estimates and lists the changes', () => {
    render(<NextYearSection isLastYear={false} section={{
      year: 2031, age: 56,
      expenses: [{ label: 'Healthcare', amount: 27823, exact: true, change: 22027, rule: "Next year's healthcare rate × next year's inflation factor" }],
      income: [
        { label: 'Salary', amount: 0, exact: true, change: -258867, rule: 'No salary after retirement' },
        { label: 'Interest (estimate)', amount: 6000, exact: false, change: -5532, rule: 'Half of next year\'s expenses × HYSA rate' }
      ],
      incomeGap: { label: 'Income gap (estimate)', amount: 251000, exact: false, change: 251000, rule: 'Expenses minus estimated passive income' },
      changes: ['Retirement starts: salary and contributions end, and the spending strategy begins']
    }} />);

    expect(screen.getByText('Next year: 2031, age 56')).toBeInTheDocument();
    expect(screen.getByText('+$22,027')).toBeInTheDocument();
    expect(screen.getAllByText('Estimate')).toHaveLength(2);
    expect(screen.getByText(/Retirement starts/)).toBeInTheDocument();
    // FR004-003: each projection says how it was computed
    expect(screen.getByText('No salary after retirement')).toBeInTheDocument();
  });

  it('says the simulation ends with the last year (FR004-073)', () => {
    render(<NextYearSection isLastYear section={null} />);

    expect(screen.getByText('The simulation ends with this year.')).toBeInTheDocument();
  });
});
