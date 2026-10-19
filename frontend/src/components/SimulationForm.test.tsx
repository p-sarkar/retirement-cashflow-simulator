import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SimulationForm from './SimulationForm';
import { SimulationConfig } from '../types/simulation';
import { apiConfig } from '../test/fixtures';

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
      hysaRate: 0.04,
      incomeTax: 0.12
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

describe('SimulationForm while a simulation is running (T003-071)', () => {
  it('disables the run button and shows progress', () => {
    const onSubmit = vi.fn();
    render(<SimulationForm onSubmit={onSubmit} running />);

    const button = screen.getByRole('button', { name: 'Running…' });
    expect(button).toBeDisabled();
    fireEvent.submit(button.closest('form')!);
    expect(onSubmit).not.toHaveBeenCalled();
  });
});

describe('SimulationForm End Age (FR002-001a)', () => {
  const endAgeField = () => field('General', 'End Age') as HTMLInputElement;
  const runButton = () => screen.getByRole('button', { name: 'Run Simulation' });

  it('defaults to 85 and is sent with the config', async () => {
    const sent = await submit();
    expect(sent.endAge).toBe(85);
  });

  it('sends an edited End Age', async () => {
    const sent = await submit(() => fireEvent.change(endAgeField(), { target: { value: '95' } }));
    expect(sent.endAge).toBe(95);
  });

  it('shows an inline error and blocks Run when End Age is not after Current Age', () => {
    const onSubmit = vi.fn();
    render(<SimulationForm onSubmit={onSubmit} />);

    fireEvent.change(field('General', 'Current Age'), { target: { value: '50' } });
    fireEvent.change(endAgeField(), { target: { value: '50' } });

    expect(screen.getByText('End age must be greater than current age (50) and no more than 110')).toBeInTheDocument();
    expect(runButton()).toBeDisabled();
    fireEvent.submit(runButton().closest('form')!);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('does not flag one-time expenses while End Age is mid-edit', () => {
    render(<SimulationForm onSubmit={vi.fn()} />);

    fireEvent.change(endAgeField(), { target: { value: '' } });

    expect(screen.queryByText('Needs attention')).not.toBeInTheDocument();
  });
});

describe('SimulationForm market presets (FR002-003)', () => {
  const presets = [
    { id: 'crash-2008', name: '2008 Crash', description: 'Replays 2008-2012 at retirement', inflation: 0.03,
      preRetirementGrowth: 0.10, postRetirementGrowth: 0.10, bondYield: 0.05, retirementReturnSequence: [-0.37] },
    { id: 'bull-market', name: 'Bull Market', description: 'Strong markets', inflation: 0.025,
      preRetirementGrowth: 0.12, postRetirementGrowth: 0.12, bondYield: 0.04, retirementReturnSequence: [] }
  ];
  const marketField = () => field('Economic Assumptions (%)', 'Market Conditions');

  const choose = async (name: string) => {
    await userEvent.click(marketField());
    await userEvent.click(screen.getByRole('option', { name }));
  };

  const submitWith = async (edit: () => Promise<void>) => {
    const onSubmit = vi.fn();
    render(<SimulationForm onSubmit={onSubmit} presets={presets} />);
    await edit();
    await userEvent.click(screen.getByRole('button', { name: 'Run Simulation' }));
    return onSubmit.mock.lastCall![0] as SimulationConfig;
  };

  it('starts with custom market conditions', async () => {
    const sent = await submitWith(async () => {});
    expect(marketField()).toHaveTextContent('Custom');
    expect(sent.marketPresetId).toBeNull();
  });

  it('fills the market rates from the chosen preset and sends its id', async () => {
    const sent = await submitWith(() => choose('Bull Market'));

    expect(screen.getByText('Strong markets')).toBeInTheDocument();
    expect(sent.marketPresetId).toBe('bull-market');
    expect(sent.rates).toMatchObject({ inflation: 0.025, preRetirementGrowth: 0.12, postRetirementGrowth: 0.12, bondYield: 0.04 });
    expect(sent.rates.hysaRate).toBe(0.04); // not a market rate: unchanged
  });

  const MARKET_RATE_LABELS = ['Inflation (%)', 'Pre-Retirement Growth (%)', 'Post-Retirement Growth (%)', 'Bond Yield (%)'];

  it('greys out the rates a preset sets, and says how to edit them', async () => {
    render(<SimulationForm onSubmit={vi.fn()} presets={presets} />);
    await choose('2008 Crash');

    MARKET_RATE_LABELS.forEach(label => expect(field('Economic Assumptions (%)', label)).toBeDisabled());
    expect(field('Economic Assumptions (%)', 'Inflation (%)')).toHaveValue(3); // the preset's value is still shown
    expect(field('Economic Assumptions (%)', 'HYSA Rate (%)')).toBeEnabled();
    expect(field('Economic Assumptions (%)', 'Effective Income Tax (%)')).toBeEnabled();
    expect(screen.getByText('Set by the 2008 Crash preset. Choose Custom to edit them.')).toBeInTheDocument();
  });

  it('makes the rates editable again under Custom, starting from the preset values', async () => {
    const sent = await submitWith(async () => {
      await choose('Bull Market');
      await choose('Custom');
      MARKET_RATE_LABELS.forEach(label => expect(field('Economic Assumptions (%)', label)).toBeEnabled());
      expect(field('Economic Assumptions (%)', 'Inflation (%)')).toHaveValue(2.5);
      fireEvent.change(field('Economic Assumptions (%)', 'Bond Yield (%)'), { target: { value: '6' } });
    });

    expect(sent.marketPresetId).toBeNull();
    expect(sent.rates).toMatchObject({ inflation: 0.025, preRetirementGrowth: 0.12, bondYield: 0.06 });
    expect(screen.queryByText(/Choose Custom to edit them/)).not.toBeInTheDocument();
  });

  it('keeps the preset when a non-market rate is edited', async () => {
    const sent = await submitWith(async () => {
      await choose('2008 Crash');
      fireEvent.change(field('Economic Assumptions (%)', 'HYSA Rate (%)'), { target: { value: '3' } });
    });
    expect(sent.marketPresetId).toBe('crash-2008');
  });

  it('hides the selector when no presets are available', () => {
    render(<SimulationForm onSubmit={vi.fn()} />);
    expect(within(screen.getByText('Economic Assumptions (%)').parentElement!).queryByLabelText('Market Conditions')).not.toBeInTheDocument();
  });
});

describe('SimulationForm loading a saved config (US002-2)', () => {
  it('starts from the saved inputs and sends them back unchanged', async () => {
    const onSubmit = vi.fn();
    render(<SimulationForm onSubmit={onSubmit} initialConfig={apiConfig} />);

    expect(field('General', 'Name')).toHaveValue('Saved Plan');
    expect(field('Economic Assumptions (%)', 'Inflation (%)')).toHaveValue(2.5);
    await userEvent.click(screen.getByRole('button', { name: 'Run Simulation' }));
    expect(onSubmit.mock.lastCall![0]).toEqual(apiConfig);
  });
});

describe('SimulationForm Monte Carlo (US002-3)', () => {
  it('runs Monte Carlo with the current inputs', async () => {
    const onRunMonteCarlo = vi.fn();
    render(<SimulationForm onSubmit={vi.fn()} onRunMonteCarlo={onRunMonteCarlo} />);

    await userEvent.click(screen.getByRole('button', { name: 'Run Monte Carlo (1,000 runs)' }));
    expect(onRunMonteCarlo).toHaveBeenCalledTimes(1);
    expect(onRunMonteCarlo.mock.lastCall![0].rates.inflation).toBe(0.03);
  });

  it('disables both runs while Monte Carlo is running', () => {
    render(<SimulationForm onSubmit={vi.fn()} onRunMonteCarlo={vi.fn()} monteCarloRunning />);
    expect(screen.getByRole('button', { name: 'Running Monte Carlo…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Run Simulation' })).toBeDisabled();
  });

  it('has no Monte Carlo button without a handler', () => {
    render(<SimulationForm onSubmit={vi.fn()} />);
    expect(screen.queryByRole('button', { name: /Monte Carlo/ })).not.toBeInTheDocument();
  });
});

describe('SimulationForm spending strategy (spec 006, US006-1)', () => {
  const strategies = [
    { id: 'PARTHA_V0_01_20250105', name: "Partha's Spending Strategy-v0.01-20260105", description: 'Fixed spending with buckets', settings: 'NONE' },
    { id: 'RISK_BASED_GUARDRAILS_V1', name: 'Risk-Based Guardrails', description: 'Adjusts Wants at guardrails', settings: 'GUARDRAILS' }
  ];
  const section = 'Withdrawal Strategy';
  const strategyField = () => field(section, 'Spending Strategy');
  const guardrailLabels = ['Target Success (%)', 'Upper Guardrail (%)', 'Lower Guardrail (%)', 'Review Frequency'];
  const queryGuardrail = (label: string) => within(screen.getByText(section).parentElement!).queryByLabelText(label);

  const choose = async (name: string) => {
    await userEvent.click(strategyField());
    await userEvent.click(screen.getByRole('option', { name }));
  };

  it('offers the catalog with the existing strategy selected and described (scenario 2)', async () => {
    render(<SimulationForm onSubmit={vi.fn()} strategies={strategies} />);
    expect(strategyField()).toHaveTextContent("Partha's Spending Strategy-v0.01-20260105");
    expect(screen.getByText('Fixed spending with buckets')).toBeInTheDocument();
    await userEvent.click(strategyField());
    expect(screen.getByRole('option', { name: 'Risk-Based Guardrails' })).toBeInTheDocument();
  });

  it('shows the guardrail settings with their defaults only for guardrails (scenario 3, FR006-005)', async () => {
    render(<SimulationForm onSubmit={vi.fn()} strategies={strategies} />);
    guardrailLabels.forEach(label => expect(queryGuardrail(label)).not.toBeInTheDocument());

    await choose('Risk-Based Guardrails');

    expect(screen.getByText('Adjusts Wants at guardrails')).toBeInTheDocument();
    expect(field(section, 'Target Success (%)')).toHaveValue(90);
    expect(field(section, 'Upper Guardrail (%)')).toHaveValue(99);
    expect(field(section, 'Lower Guardrail (%)')).toHaveValue(70);
    expect(field(section, 'Review Frequency')).toHaveTextContent('Yearly');
    expect(field(section, 'TDA Withdrawal % of Needs')).toBeInTheDocument();
    expect(field(section, 'Roth Convert (Post-Retire)')).toBeInTheDocument();
  });

  it('sends the strategy and its settings as decimals', async () => {
    const onSubmit = vi.fn();
    render(<SimulationForm onSubmit={onSubmit} strategies={strategies} />);
    await choose('Risk-Based Guardrails');
    fireEvent.change(field(section, 'Target Success (%)'), { target: { value: '85' } });
    await userEvent.click(field(section, 'Review Frequency'));
    await userEvent.click(screen.getByRole('option', { name: 'Quarterly' }));
    await userEvent.click(screen.getByRole('button', { name: 'Run Simulation' }));

    const sent: SimulationConfig = onSubmit.mock.lastCall![0];
    expect(sent.strategy.type).toBe('RISK_BASED_GUARDRAILS_V1');
    expect(sent.strategy.guardrails).toEqual({ targetSuccess: 0.85, upperGuardrail: 0.99, lowerGuardrail: 0.7, reviewFrequency: 'QUARTERLY' });
  });

  it('keeps edited guardrail settings when switching away and back (scenario 5)', async () => {
    render(<SimulationForm onSubmit={vi.fn()} strategies={strategies} />);
    await choose('Risk-Based Guardrails');
    fireEvent.change(field(section, 'Lower Guardrail (%)'), { target: { value: '60' } });
    await choose("Partha's Spending Strategy-v0.01-20260105");
    expect(queryGuardrail('Lower Guardrail (%)')).not.toBeInTheDocument();
    await choose('Risk-Based Guardrails');
    expect(field(section, 'Lower Guardrail (%)')).toHaveValue(60);
  });

  it('shows a 422 message on the guardrail field it names (FR006-010)', () => {
    const initial = { ...apiConfig, strategy: { ...apiConfig.strategy, type: 'RISK_BASED_GUARDRAILS_V1' } };
    render(<SimulationForm onSubmit={vi.fn()} strategies={strategies} initialConfig={initial}
      fieldErrors={[{ field: 'strategy.guardrails.lowerGuardrail', message: 'Lower guardrail (90%) must be below the target (90%)' }]} />);
    expect(screen.getByText('Lower guardrail (90%) must be below the target (90%)')).toBeInTheDocument();
    expect(field(section, 'Lower Guardrail (%)')).toHaveAttribute('aria-invalid', 'true');
  });

  it('offers only the existing strategy when the catalog could not be loaded (R006-7)', async () => {
    render(<SimulationForm onSubmit={vi.fn()} />);
    expect(strategyField()).toHaveTextContent("Partha's Spending Strategy-v0.01-20260105");
    await userEvent.click(strategyField());
    expect(screen.getAllByRole('option')).toHaveLength(1);
  });
});
