import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BreakdownDialog from './BreakdownDialog';
import { getBreakdown } from '../services/api';
import { BreakdownSection, ComputationBreakdown, SimulationConfig } from '../types/simulation';

vi.mock('../services/api', () => ({ getBreakdown: vi.fn() }));
const mockGetBreakdown = vi.mocked(getBreakdown);

const config = { name: 'Test', oneTimeExpenses: [] } as unknown as SimulationConfig;

const section = (title: string, labels: string[]): BreakdownSection => ({
  title,
  steps: labels.map(label => ({ label, formula: 'f', values: {}, result: 1000, explanation: 'e' }))
});

const breakdown = (sections: BreakdownSection[]): ComputationBreakdown => ({
  year: 2028,
  age: 63,
  sections,
  apiMetadata: { version: '1.1.24', buildTime: 'now' }
} as ComputationBreakdown);

describe('BreakdownDialog', () => {
  beforeEach(() => {
    mockGetBreakdown.mockReset();
  });

  it('renders a One-Time Expenses section when the server includes one (FR-033-v2)', async () => {
    mockGetBreakdown.mockResolvedValue(breakdown([
      section('Income', ['Salary']),
      section('One-Time Expenses', ['Car Loan (Loan Payment)', 'Roof (Cash Expense)', 'Total One-Time Expenses'])
    ]));

    render(<BreakdownDialog open onClose={() => {}} config={config} targetAge={63} />);

    expect(await screen.findByText('One-Time Expenses')).toBeInTheDocument();
    expect(mockGetBreakdown).toHaveBeenCalledWith(config, 63);
  });

  it('omits the One-Time Expenses section when the server sends none', async () => {
    mockGetBreakdown.mockResolvedValue(breakdown([section('Income', ['Salary'])]));

    render(<BreakdownDialog open onClose={() => {}} config={config} targetAge={63} />);

    expect(await screen.findByText('Income')).toBeInTheDocument();
    expect(screen.queryByText('One-Time Expenses')).not.toBeInTheDocument();
  });

  it('shows an error when the breakdown request fails', async () => {
    mockGetBreakdown.mockRejectedValue(new Error('Request failed with status code 422'));

    render(<BreakdownDialog open onClose={() => {}} config={config} targetAge={63} />);

    expect(await screen.findByText('Request failed with status code 422')).toBeInTheDocument();
  });

  it('closes via the Close button and by clicking outside (FR-035)', async () => {
    mockGetBreakdown.mockResolvedValue(breakdown([section('Income', ['Salary'])]));
    const onClose = vi.fn();

    render(<BreakdownDialog open onClose={onClose} config={config} targetAge={63} />);
    await screen.findByText('Income');

    await userEvent.click(screen.getByRole('button', { name: 'Close' }));
    expect(onClose).toHaveBeenCalledTimes(1);

    const backdrop = document.querySelector('.MuiBackdrop-root')!;
    await userEvent.click(backdrop);
    expect(onClose).toHaveBeenCalledTimes(2);
  });
});
