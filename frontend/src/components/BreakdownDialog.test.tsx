import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BreakdownDialog from './BreakdownDialog';
import { getBreakdown } from '../services/api';
import { ExpenseSection, SimulationConfig, YearBreakdown } from '../types/simulation';

vi.mock('../services/api', () => ({ getBreakdown: vi.fn() }));
const mockGetBreakdown = vi.mocked(getBreakdown);

const config = { name: 'Test', oneTimeExpenses: [] } as unknown as SimulationConfig;

const expenses = (oneTimeExpenses: ExpenseSection['oneTimeExpenses']): ExpenseSection => ({
  items: [{ label: 'Needs', amount: 50000, rule: 'r', inputs: {}, todaysDollars: null }],
  wantsDerivation: [],
  oneTimeExpenses,
  total: 50000
});

const breakdown = (expenseSection: ExpenseSection | null = null): YearBreakdown => ({
  year: 2028,
  age: 63,
  spouseAge: 61,
  phase: 'RETIREMENT',
  isLastYear: false,
  startingPosition: {
    openingBalances: { sb: 100000, cbb: 400000, tba: 1000000, tda: 1500000, tfa: 300000 },
    openingTotal: 3300000,
    conditions: {
      inflationRate: 0.03, cumulativeInflation: 1.0609, equityReturn: 0.08, equityReturnSource: 'POST_RETIREMENT_RATE',
      presetName: null, presetSequenceYear: null, bondYield: 0.05, hysaRate: 0.04, incomeTaxRate: 0.12,
      marketVsAllTimeHigh: 1
    },
    household: { healthcareBracket: 'POST_RETIREMENT_PRE_MEDICARE', earners: [] }
  },
  expenses: expenseSection, income: null, incomeGap: null, moneyMovement: null, taxes: null, nextYear: null,
  apiMetadata: { version: '1.1.24', buildTime: 'now', serverStartTime: 'now' }
});

describe('BreakdownDialog', () => {
  beforeEach(() => {
    mockGetBreakdown.mockReset();
  });

  it('shows the sections the server sends, in spec order', async () => {
    mockGetBreakdown.mockResolvedValue(breakdown(expenses([])));

    render(<BreakdownDialog open onClose={() => {}} config={config} targetAge={63} />);

    expect(await screen.findByRole('heading', { name: 'Starting Position' })).toBeInTheDocument();
    expect(screen.getAllByRole('heading', { level: 3 }).map(h => h.textContent)).toEqual(['Starting Position', 'Expenses', 'Next Year']);
    // Sections the server hasn't filled are left out
    expect(screen.queryByRole('heading', { name: 'Money Movement' })).not.toBeInTheDocument();
  });

  it('shows all seven section headers on open, each one click from the top (SC004-006)', async () => {
    const line = (label: string) => ({ label, amount: 1000, rule: 'r', inputs: {}, todaysDollars: null });
    const ledger = (account: 'SB' | 'CBB' | 'TBA' | 'TDA' | 'TFA') =>
      ({ account, opening: [1, 1, 1, 1], rows: [], closing: [1, 1, 1, 1] });
    const tax = { basis: 'PRIOR_YEAR' as const, components: [], taxableIncome: 0, rate: 0.12, tax: 0 };
    mockGetBreakdown.mockResolvedValue({
      ...breakdown(expenses([])),
      income: {
        salary: null, interest: line('Interest'), dividends: line('Dividends'), socialSecurity: [],
        incomeTotal: 2000, withdrawals: [], totalIncome: 2000, transfers: []
      },
      incomeGap: {
        estimated: null, actual: null, differences: [], capAig: null, sbFloor: null, sbCap: null, cbbCap: null,
        cbbCapReductions: [], thresholds: [], takeHomeSalary: line('Take-home salary'),
        workingExpenses: line('Total expenses'), explanation: 'Working year'
      },
      moneyMovement: {
        ledgers: [ledger('SB'), ledger('CBB'), ledger('TBA'), ledger('TDA'), ledger('TFA')],
        flows: [], quarterStrategy: [], failure: null
      },
      taxes: { billThisYear: tax, generatedThisYear: { ...tax, basis: 'THIS_YEAR' }, simplifications: [] },
      nextYear: { year: 2029, age: 64, expenses: [], income: [], incomeGap: null, changes: [] }
    });

    render(<BreakdownDialog open onClose={() => {}} config={config} targetAge={63} />);

    await screen.findByRole('heading', { name: 'Starting Position' });
    const sections = ['Starting Position', 'Expenses', 'Income', 'Income Gap', 'Money Movement', 'Taxes', 'Next Year'];
    expect(screen.getAllByRole('heading', { level: 3 }).map(h => h.textContent)).toEqual(sections);
    // Each header is a button that opens its section: one click from the top
    sections.forEach(name => expect(screen.getByRole('button', { name })).toBeVisible());
  });

  it('opens Money Movement with the quarter highlighted when opened from a quarterly row (FR004-008)', async () => {
    const ledger = (account: 'SB' | 'CBB' | 'TBA' | 'TDA' | 'TFA') =>
      ({ account, opening: [1, 1, 1, 1], rows: [], closing: [1, 1, 1, 1] });
    mockGetBreakdown.mockResolvedValue({
      ...breakdown(),
      moneyMovement: {
        ledgers: [ledger('SB'), ledger('CBB'), ledger('TBA'), ledger('TDA'), ledger('TFA')],
        flows: [], quarterStrategy: [], failure: null
      }
    });

    render(<BreakdownDialog open onClose={() => {}} config={config} targetAge={63} initialQuarter={3} />);

    const heading = await screen.findByRole('button', { name: 'Money Movement' });
    expect(heading).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('button', { name: 'Starting Position' })).toHaveAttribute('aria-expanded', 'false');
    expect(within(screen.getByRole('table', { name: 'Spend Bucket ledger' })).getByRole('columnheader', { name: 'Q3' }))
      .toHaveAttribute('aria-current', 'true');
  });

  it('lists one-time expenses by name and type in the Expenses section (FR003-033-v2, FR004-022)', async () => {
    mockGetBreakdown.mockResolvedValue(breakdown(expenses([
      { name: 'Car Loan', type: 'LOAN_PAYMENT', amount: 9000, inflationAdjusted: false, todaysDollars: null },
      { name: 'Roof', type: 'CASH', amount: 20000, inflationAdjusted: true, todaysDollars: 18000 }
    ])));

    render(<BreakdownDialog open onClose={() => {}} config={config} targetAge={63} />);

    expect(await screen.findByText('Car Loan (loan payment)')).toBeInTheDocument();
    expect(screen.getByText('Roof (cash expense)')).toBeInTheDocument();
    expect(mockGetBreakdown).toHaveBeenCalledWith(config, 63);
  });

  it('leaves out the one-time expense list in a year without any', async () => {
    mockGetBreakdown.mockResolvedValue(breakdown(expenses([])));

    render(<BreakdownDialog open onClose={() => {}} config={config} targetAge={63} />);

    expect(await screen.findByRole('heading', { name: 'Expenses' })).toBeInTheDocument();
    expect(screen.queryByRole('table', { name: 'One-time expenses' })).not.toBeInTheDocument();
  });

  it('shows an error when the breakdown request fails', async () => {
    mockGetBreakdown.mockRejectedValue(new Error('Request failed with status code 422'));

    render(<BreakdownDialog open onClose={() => {}} config={config} targetAge={63} />);

    expect(await screen.findByText('Request failed with status code 422')).toBeInTheDocument();
  });

  it('closes via the Close button and by clicking outside (FR003-035)', async () => {
    mockGetBreakdown.mockResolvedValue(breakdown());
    const onClose = vi.fn();

    render(<BreakdownDialog open onClose={onClose} config={config} targetAge={63} />);
    await screen.findByRole('heading', { name: 'Starting Position' });

    await userEvent.click(screen.getByRole('button', { name: 'Close' }));
    expect(onClose).toHaveBeenCalledTimes(1);

    const backdrop = document.querySelector('.MuiBackdrop-root')!;
    await userEvent.click(backdrop);
    expect(onClose).toHaveBeenCalledTimes(2);
  });
});
