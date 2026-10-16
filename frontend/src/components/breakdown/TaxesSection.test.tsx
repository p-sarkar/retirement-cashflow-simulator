import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import TaxesSection from './TaxesSection';
import { TaxComputation } from '../../types/simulation';

const computation = (basis: TaxComputation['basis'], tax: number): TaxComputation => ({
  basis,
  components: [
    { name: 'Salary', amount: 200000, taxedShare: 1, taxable: 200000 },
    { name: 'TBA withdrawals', amount: 20000, taxedShare: 0.5, taxable: 10000 }
  ],
  taxableIncome: 210000,
  rate: 0.12,
  tax
});

describe('TaxesSection (US004-4)', () => {
  it('shows this year\'s bill from last year\'s income and the income generated for next year', () => {
    render(<TaxesSection section={{
      billThisYear: computation('PRIOR_YEAR', 25200),
      generatedThisYear: computation('THIS_YEAR', 25200),
      simplifications: ['A single effective tax rate applies to all taxable income.']
    }} />);

    const bill = within(screen.getByRole('table', { name: "This year's tax bill" }));
    expect(bill.getByText('TBA withdrawals')).toBeInTheDocument();
    expect(bill.getByText('50%')).toBeInTheDocument();
    expect(bill.getByText('$210,000 × 12% = $25,200')).toBeInTheDocument();
    expect(screen.getByText("Based on last year's taxable income")).toBeInTheDocument();
    expect(screen.getByRole('table', { name: "Taxable income this year (next year's bill)" })).toBeInTheDocument();
    expect(screen.getByText('A single effective tax rate applies to all taxable income.')).toBeInTheDocument();
  });

  it('says when the first year\'s bill is an estimate', () => {
    render(<TaxesSection section={{
      billThisYear: computation('START_YEAR_ESTIMATE', 25200),
      generatedThisYear: computation('THIS_YEAR', 25200),
      simplifications: []
    }} />);

    expect(screen.getByText(/estimate of the start year's income/)).toBeInTheDocument();
  });
});
