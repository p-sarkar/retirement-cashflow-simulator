import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SimulationForm from './SimulationForm';
import { SimulationConfig } from '../types/simulation';

// The form holds rates as whole-number percentages (3 = 3%); the API expects decimals (0.03),
// except loan aprPercent, which the API also takes as a whole number (5 = 5%).
// getByLabelText scans every label in the page, which is slow for this large form, so search one section
const field = (section: string, label: string) =>
  within(screen.getByText(section).parentElement!).getByLabelText(label);

const submit = async (edit?: () => void): Promise<SimulationConfig> => {
  const onSubmit = vi.fn();
  render(<SimulationForm onSubmit={onSubmit} />);
  edit?.();
  await userEvent.click(screen.getByRole('button', { name: 'Run Simulation' }));
  expect(onSubmit).toHaveBeenCalledTimes(1);
  return onSubmit.mock.lastCall![0];
};

describe('SimulationForm unit conversion on submit', () => {
  it('sends every rate as a decimal', async () => {
    const sent = await submit();

    expect(sent.rates).toEqual({
      inflation: 0.03,
      preRetirementGrowth: 0.08,
      postRetirementGrowth: 0.08,
      bondYield: 0.05,
      hysaRate: 0.03,
      incomeTax: 0.18
    });
    expect(sent.strategy.tdaWithdrawalPercentage).toBe(0.6);
  });

  it('converts values the user edits', async () => {
    const sent = await submit(() => {
      fireEvent.change(field('Economic Assumptions (%)', 'Inflation (%)'), { target: { value: '2.5' } });
      fireEvent.change(field('Withdrawal Strategy', 'TDA Withdrawal % of Needs'), { target: { value: '100' } });
    });

    expect(sent.rates.inflation).toBeCloseTo(0.025, 10);
    expect(sent.strategy.tdaWithdrawalPercentage).toBe(1);
  });

  it('keeps loan APR as a whole-number percentage', async () => {
    const sent = await submit();
    const loans = (sent.oneTimeExpenses ?? []).filter(e => e.type === 'LOAN');

    expect(loans.length).toBeGreaterThan(0);
    loans.forEach(loan => expect(loan.aprPercent).toBe(5));
  });

  it('leaves dollar amounts unchanged', async () => {
    const sent = await submit();

    expect(sent.rates.inflation).toBeLessThan(1);
    expect(sent.salary).toBeGreaterThan(1000);
    expect(sent.portfolio.sb).toBeGreaterThan(1000);
  });
});

describe('SimulationForm while a simulation is running (T071)', () => {
  it('disables the run button and shows progress', () => {
    const onSubmit = vi.fn();
    render(<SimulationForm onSubmit={onSubmit} running />);

    const button = screen.getByRole('button', { name: 'Running…' });
    expect(button).toBeDisabled();
    fireEvent.submit(button.closest('form')!);
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
