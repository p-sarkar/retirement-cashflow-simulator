import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import OneTimeExpenseInput from './OneTimeExpenseInput';
import { LoanExpense, OneTimeExpense } from '../types/simulation';
import { calculateQuarterlyPayment, formatCurrency } from '../utils/expenseUtils';

const loan: LoanExpense = {
  type: 'LOAN',
  id: 'loan-1',
  name: 'Car Loan',
  principal: 50000,
  downPayment: 10000,
  aprPercent: 5,
  termYears: 5,
  startYearOrAge: { type: 'YEAR', year: 2027 }
};

const cash: OneTimeExpense = {
  type: 'CASH',
  id: 'cash-1',
  name: 'Roof',
  amount: 50000,
  yearOrAge: { type: 'AGE', age: 65 }
};

const setup = (expenses: OneTimeExpense[] = [], currentAge = 60) => {
  const onExpensesChange = vi.fn();
  const onValidityChange = vi.fn();
  const ui = (age: number) => (
    <OneTimeExpenseInput
      expenses={expenses}
      onExpensesChange={onExpensesChange}
      onValidityChange={onValidityChange}
      currentAge={age}
      currentYear={2025}
    />
  );
  const { rerender } = render(ui(currentAge));
  const lastExpenses = (): OneTimeExpense[] => onExpensesChange.mock.lastCall?.[0];
  const lastValidity = (): boolean => onValidityChange.mock.lastCall?.[0];
  const setCurrentAge = (age: number) => rerender(ui(age));
  return { onExpensesChange, onValidityChange, lastExpenses, lastValidity, setCurrentAge };
};

const change = (label: string, value: string) =>
  fireEvent.change(screen.getByLabelText(label), { target: { value } });

describe('OneTimeExpenseInput', () => {
  it('shows an empty-state message when there are no expenses', () => {
    setup();
    expect(screen.getByText(/No one-time expenses added/)).toBeInTheDocument();
  });

  it('reports invalid while a newly added expense is incomplete (FR-043)', async () => {
    const { lastExpenses, lastValidity } = setup();
    await userEvent.click(screen.getByRole('button', { name: /Add Expense/ }));

    expect(lastValidity()).toBe(false);
    expect(lastExpenses()).toEqual([]);
  });

  it('emits a valid cash expense once all fields are filled', async () => {
    const { lastExpenses, lastValidity } = setup();
    await userEvent.click(screen.getByRole('button', { name: /Add Expense/ }));

    change('Name', 'Roof');
    change('Amount', '20000');
    change('Age', '65');

    expect(lastValidity()).toBe(true);
    expect(lastExpenses()).toEqual([
      expect.objectContaining({ type: 'CASH', name: 'Roof', amount: 20000, yearOrAge: { type: 'AGE', age: 65 } })
    ]);
  });

  it('shows field validation errors', async () => {
    setup();
    await userEvent.click(screen.getByRole('button', { name: /Add Expense/ }));

    change('Amount', '-5');

    expect(screen.getByText('Amount must be greater than zero')).toBeInTheDocument();
  });

  it('shows the quarterly payment as an estimate in today\'s dollars (FR-018)', () => {
    setup([loan]);

    const estimate = screen.getByLabelText('Est. Quarterly Payment') as HTMLInputElement;
    expect(estimate.value).toBe(formatCurrency(calculateQuarterlyPayment(40000, 5, 5)));
    expect(screen.getByText(/Today's dollars/)).toBeInTheDocument();
  });

  it('does not send quarterlyPayment when a loan is edited (FR-016b)', () => {
    const { lastExpenses, lastValidity } = setup([loan]);

    change('APR', '6');

    expect(lastValidity()).toBe(true);
    const [sent] = lastExpenses() as LoanExpense[];
    expect(sent).toMatchObject({ type: 'LOAN', aprPercent: 6, principal: 50000, downPayment: 10000 });
    expect(sent).not.toHaveProperty('quarterlyPayment');
  });

  it('removes an expense', async () => {
    const { lastExpenses, lastValidity } = setup([loan]);

    await userEvent.click(screen.getByTestId('DeleteIcon').closest('button')!);

    expect(lastExpenses()).toEqual([]);
    expect(lastValidity()).toBe(true);
  });

  describe('validation messages (T070)', () => {
    it('lists the missing fields of a new entry instead of marking them all red', async () => {
      setup();
      await userEvent.click(screen.getByRole('button', { name: /Add Expense/ }));

      expect(screen.getByText('To include this expense, fill in: Name, Amount, Age')).toBeInTheDocument();
      expect(screen.queryByText('Amount is required')).not.toBeInTheDocument();
    });

    it('reports a cleared required field as required, not out of range', async () => {
      setup();
      await userEvent.click(screen.getByRole('button', { name: /Add Expense/ }));

      change('Amount', '100');
      change('Amount', '');

      expect(screen.getByText('Amount is required')).toBeInTheDocument();
      expect(screen.queryByText('Amount must be greater than zero')).not.toBeInTheDocument();
    });

    it('rejects a fractional loan term', () => {
      const { lastValidity } = setup([loan]);

      change('Term', '2.5');

      expect(screen.getByText('Term must be a whole number of years')).toBeInTheDocument();
      expect(lastValidity()).toBe(false);
    });

    it('re-validates timing when the current age changes', () => {
      // Roof at age 65 is valid at 60, but in the past once current age is 70
      const { lastValidity, lastExpenses, setCurrentAge } = setup([cash]);

      setCurrentAge(70);

      expect(lastValidity()).toBe(false);
      expect(lastExpenses()).toEqual([]);
      expect(screen.getByText('Expense cannot occur in the past')).toBeInTheDocument();
    });
  });

  describe('collapsible entries (T072)', () => {
    it('starts saved expenses collapsed with a one-line summary', () => {
      setup([loan]);

      const toggle = screen.getByRole('button', { name: 'Expand Car Loan' });
      expect(toggle).toHaveAttribute('aria-expanded', 'false');
      expect(screen.getByText(/Loan · \$50,000 \(\$10,000 down\) · 5% · 5 yrs · from 2027 · est\./)).toBeInTheDocument();
    });

    it('expands and collapses an entry', async () => {
      setup([loan]);

      await userEvent.click(screen.getByRole('button', { name: 'Expand Car Loan' }));
      expect(screen.getByRole('button', { name: 'Collapse Car Loan' })).toHaveAttribute('aria-expanded', 'true');
    });

    it('keeps an invalid entry open', async () => {
      setup();
      await userEvent.click(screen.getByRole('button', { name: /Add Expense/ }));

      const toggle = screen.getByRole('button', { name: 'Collapse New expense' });
      expect(toggle).toBeDisabled();
      expect(screen.getByText('Needs attention')).toBeInTheDocument();
    });

    it('expands all entries at once', async () => {
      setup([loan, cash]);

      await userEvent.click(screen.getByRole('button', { name: 'Expand all' }));

      expect(screen.getByRole('button', { name: 'Collapse Car Loan' })).toHaveAttribute('aria-expanded', 'true');
      expect(screen.getByRole('button', { name: 'Collapse Roof' })).toHaveAttribute('aria-expanded', 'true');
    });
  });

  it('explains cash vs loan in the type menu (T073)', async () => {
    setup();
    await userEvent.click(screen.getByRole('button', { name: /Add Expense/ }));

    await userEvent.click(screen.getByRole('combobox', { name: 'Type' }));

    expect(screen.getByText(/Lump sum paid from the Spend Bucket in Q1/)).toBeInTheDocument();
    expect(screen.getByText(/fixed quarterly payments for the term/)).toBeInTheDocument();
  });

  describe('field formatting (T074)', () => {
    it('shows dollar amounts with thousands separators and accepts pasted commas', () => {
      const { lastExpenses } = setup([cash]);

      expect((screen.getByLabelText('Amount') as HTMLInputElement).value).toBe('50,000');

      change('Amount', '$1,250,000.50');

      expect((screen.getByLabelText('Amount') as HTMLInputElement).value).toBe('1,250,000.50');
      expect(lastExpenses()).toEqual([expect.objectContaining({ amount: 1250000.5 })]);
    });

    it('shows the raw number while editing', async () => {
      setup([cash]);
      const amount = screen.getByLabelText('Amount') as HTMLInputElement;

      await userEvent.click(amount);

      expect(amount.value).toBe('50000');
    });
  });

  describe('editing and removing (US4, T066–T069)', () => {
    it('T066 sends the updated cash amount', () => {
      const { lastExpenses } = setup([cash]);

      change('Amount', '60000');

      expect(lastExpenses()).toEqual([expect.objectContaining({ id: 'cash-1', amount: 60000 })]);
    });

    it('T067 recalculates the estimated quarterly payment when the APR changes', () => {
      setup([{ ...loan, aprPercent: 6 }]);
      const estimate = () => (screen.getByLabelText('Est. Quarterly Payment') as HTMLInputElement).value;
      expect(estimate()).toBe(formatCurrency(calculateQuarterlyPayment(40000, 6, 5)));

      change('APR', '5');

      expect(estimate()).toBe(formatCurrency(calculateQuarterlyPayment(40000, 5, 5)));
    });

    it('T068 removes only the chosen expense', async () => {
      const { lastExpenses } = setup([loan, cash]);

      await userEvent.click(screen.getByRole('button', { name: 'Remove Car Loan' }));

      expect(lastExpenses()).toEqual([expect.objectContaining({ id: 'cash-1' })]);
      expect(screen.queryByText('Car Loan')).not.toBeInTheDocument();
    });

    it('T069 removing every expense sends an empty list', async () => {
      const { lastExpenses, lastValidity } = setup([loan, cash]);

      await userEvent.click(screen.getByRole('button', { name: 'Remove Car Loan' }));
      await userEvent.click(screen.getByRole('button', { name: 'Remove Roof' }));

      expect(lastExpenses()).toEqual([]);
      expect(lastValidity()).toBe(true);
      expect(screen.getByText(/No one-time expenses added/)).toBeInTheDocument();
    });
  });
});
