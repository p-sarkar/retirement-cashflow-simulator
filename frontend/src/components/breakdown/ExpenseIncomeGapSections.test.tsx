import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import ExpensesSection from './ExpensesSection';
import IncomeSection from './IncomeSection';
import IncomeGapSection from './IncomeGapSection';

const item = (label: string, amount: number) => ({ label, amount, rule: 'r', inputs: {}, todaysDollars: null });

describe('ExpensesSection (FR004-020–FR004-022)', () => {
  it('lists categories, the working-year Wants derivation and one-time expenses by name and type', () => {
    render(<ExpensesSection section={{
      items: [item('Needs', 50000), item('Wants', 70000)],
      wantsDerivation: [item('Salary', 230000), item('Needs', -50000), item('Wants', 70000)],
      oneTimeExpenses: [
        { name: 'Car', type: 'DOWN_PAYMENT', amount: 11255, inflationAdjusted: true },
        { name: 'Car', type: 'LOAN_PAYMENT', amount: 8000, inflationAdjusted: false }
      ],
      total: 139255
    }} />);

    expect(screen.getByRole('table', { name: 'How Wants is computed' })).toBeInTheDocument();
    expect(screen.getByText('Car (down payment)')).toBeInTheDocument();
    expect(screen.getByText('Car (loan payment)')).toBeInTheDocument();
    expect(screen.getByText('Fixed for the loan term')).toBeInTheDocument();
    expect(screen.getByText('$139,255')).toBeInTheDocument();
  });
});

describe('IncomeSection (FR004-030–FR004-032)', () => {
  it('separates income from withdrawals and shows each earner', () => {
    render(<IncomeSection section={{
      salary: { gross: 230000, contribution401k: 0, contributionRoth: 5000, contributionTba: 5000, takeHome: 220000 },
      interest: item('Interest', 8000),
      dividends: item('Dividends', 20000),
      socialSecurity: [
        { role: 'HIGHER', amount: 0, spousalStepUp: false, startYear: 2045 },
        { role: 'LOWER', amount: 25000, spousalStepUp: true, startYear: 2039 }
      ],
      incomeTotal: 283000,
      withdrawals: [item('TBA withdrawals', 1000)],
      totalIncome: 284000
    }} />);

    expect(screen.getByText('$220,000')).toBeInTheDocument();
    expect(screen.getByText(/Lower earner.*spousal step-up/)).toBeInTheDocument();
    expect(screen.getByRole('table', { name: 'Withdrawals and transfers' })).toBeInTheDocument();
    expect(screen.getByText('$284,000')).toBeInTheDocument();
  });
});

describe('IncomeGapSection (FR004-040–FR004-042)', () => {
  it('explains a working year has no gap', () => {
    render(<IncomeGapSection section={{
      estimated: null, actual: null, differences: [], capAig: null, sbFloor: null, sbCap: null, cbbCap: null,
      cbbCapReductions: [], thresholds: [],
      takeHomeSalary: item('Take-home salary', 220000), workingExpenses: item('Total expenses', 220000),
      explanation: 'Working year: salary covers expenses, so there is no income gap.'
    }} />);

    expect(screen.getByText(/salary covers expenses/)).toBeInTheDocument();
  });

  it('shows the estimated and actual gap in a retirement year', () => {
    render(<IncomeGapSection section={{
      estimated: { expenses: 250000, passiveIncome: 30000, gap: 220000 },
      actual: { expenses: 270000, passiveIncome: 32000, gap: 238000 },
      differences: [item('One-time expenses', 20000), item('Interest above (−) or below (+) the estimate', -2000)],
      capAig: item('Cap AIG', 180000), sbFloor: item('SB floor', 110000), sbCap: item('SB cap', 360000), cbbCap: item('CBB cap', 721000),
      cbbCapReductions: [65],
      thresholds: [{ name: 'Crash Buffer vs 12 months earlier, at least (not modelled yet: always passes)', value: 0.9, evaluated: false }],
      takeHomeSalary: null, workingExpenses: null,
      explanation: 'Expenses minus passive income.'
    }} />);

    const gaps = screen.getByRole('table', { name: 'Estimated and actual income gap' });
    expect(gaps).toHaveTextContent('$220,000');
    expect(gaps).toHaveTextContent('$238,000');
    expect(screen.getByText(/not modelled yet/)).toBeInTheDocument();
  });
});
