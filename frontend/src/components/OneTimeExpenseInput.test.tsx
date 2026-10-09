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

const setup = (expenses: OneTimeExpense[] = []) => {
  const onExpensesChange = vi.fn();
  const onValidityChange = vi.fn();
  render(
    <OneTimeExpenseInput
      expenses={expenses}
      onExpensesChange={onExpensesChange}
      onValidityChange={onValidityChange}
      currentAge={60}
      currentYear={2025}
    />
  );
  const lastExpenses = (): OneTimeExpense[] => onExpensesChange.mock.lastCall?.[0];
  const lastValidity = (): boolean => onValidityChange.mock.lastCall?.[0];
  return { onExpensesChange, onValidityChange, lastExpenses, lastValidity };
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

    change('APR %', '6');

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
});
