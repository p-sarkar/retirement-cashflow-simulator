import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import MoneyMovementSection from './MoneyMovementSection';
import { AccountLedger, MoneyMovementSection as Section } from '../../types/simulation';

const ledger = (account: AccountLedger['account'], opening: number): AccountLedger => ({
  account,
  opening: [opening, opening + 100, opening + 200, opening + 300],
  rows: [{ type: 'INTEREST', counterparty: 'BANK', amounts: [100, 100, 100, 100], yearTotal: 400 }],
  closing: [opening + 100, opening + 200, opening + 300, opening + 400]
});

const section: Section = {
  ledgers: [ledger('SB', 1000), ledger('CBB', 2000), ledger('TBA', 3000), ledger('TDA', 4000), ledger('TFA', 5000)],
  flows: [{ from: 'BANK', to: 'SB', amount: 400 }],
  quarterStrategy: [{
    quarter: 1, rule: 'MARKET_UP_CBB_FULL', marketVs12MonthsPrior: 1.08, marketVsAllTimeHigh: 1, refillToSb: 5000,
    refillToCbb: 0, limitedBy: 'SB_CAP', explanation: 'Market up and the CBB full: refill the SB from TDA/TBA.'
  }],
  failure: null
};

describe('MoneyMovementSection (US004-1)', () => {
  it('shows a ledger per account with Q1–Q4 and Year columns, opening and closing rows', () => {
    render(<MoneyMovementSection section={section} />);

    const tables = screen.getAllByRole('table', { name: /ledger/ });
    expect(tables).toHaveLength(5);
    const sb = within(screen.getByRole('table', { name: 'Spend Bucket ledger' }));
    ['Q1', 'Q2', 'Q3', 'Q4', 'Year'].forEach(h => expect(sb.getByRole('columnheader', { name: h })).toBeInTheDocument());
    expect(sb.getByRole('rowheader', { name: 'Opening' })).toBeInTheDocument();
    expect(sb.getByRole('rowheader', { name: 'Interest from HYSA interest' })).toBeInTheDocument();
    expect(sb.getByRole('rowheader', { name: 'Closing' })).toBeInTheDocument();
    expect(sb.getAllByText('$1,400').length).toBeGreaterThan(0); // Q4 closing
  });

  it('highlights the quarter it was opened on (FR004-008)', () => {
    render(<MoneyMovementSection section={section} highlightQuarter={3} />);

    const sb = within(screen.getByRole('table', { name: 'Spend Bucket ledger' }));
    expect(sb.getByRole('columnheader', { name: 'Q3' })).toHaveAttribute('aria-current', 'true');
    expect(sb.getByRole('columnheader', { name: 'Q2' })).not.toHaveAttribute('aria-current');
  });

  it('explains each quarter of the spending strategy', () => {
    render(<MoneyMovementSection section={section} />);

    expect(screen.getByText(/Market up and the CBB full/)).toBeInTheDocument();
  });

  it('marks the quarter a plan failed in', () => {
    render(<MoneyMovementSection section={{ ...section, failure: { quarter: 2, account: 'TDA', condition: 'Taxable and Tax-Deferred both below zero' } }} />);

    expect(screen.getByRole('alert')).toHaveTextContent('Q2: Taxable and Tax-Deferred both below zero');
  });
});
