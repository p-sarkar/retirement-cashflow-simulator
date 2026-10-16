import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AxiosError, AxiosHeaders } from 'axios';
import SimulationPage from './SimulationPage';
import * as api from '../services/api';
import { apiConfig, monteCarloResult, monteCarloRunDetail, simulationResult } from '../test/fixtures';
import { SavedSimulationSummary } from '../types/simulation';

vi.mock('../services/api', async importOriginal => ({
  ...(await importOriginal<typeof import('../services/api')>()),
  getMarketPresets: vi.fn(),
  listSimulations: vi.fn(),
  loadSimulation: vi.fn(),
  saveSimulation: vi.fn(),
  runSimulation: vi.fn(),
  runMonteCarlo: vi.fn(),
  getMonteCarloRun: vi.fn()
}));
const mocked = vi.mocked(api);

// jsdom has no layout, so a ResponsiveContainer would be 0px wide: give the chart a fixed size
vi.mock('recharts', async importOriginal => ({
  ...(await importOriginal<typeof import('recharts')>()),
  ResponsiveContainer: ({ children }: { children: React.ReactElement<{ width?: number; height?: number }> }) =>
    React.cloneElement(children, { width: 600, height: 300 })
}));

const summary: SavedSimulationSummary = {
  id: 'abc', name: 'Saved Plan', createdAt: '2026-10-15T09:30:00-07:00', isSuccess: true, failureYear: null,
  finalTotalBalance: 1900000, endAge: 90
};

beforeEach(() => {
  vi.resetAllMocks();
  mocked.getMarketPresets.mockResolvedValue([]);
  mocked.listSimulations.mockResolvedValue([]);
});

const runButton = () => screen.getByRole('button', { name: 'Run Simulation' });

describe('SimulationPage save and load (US002-2)', () => {
  it('saves the displayed simulation under the name entered', async () => {
    mocked.runSimulation.mockResolvedValue(simulationResult());
    mocked.saveSimulation.mockResolvedValue(summary);
    render(<SimulationPage />);

    await userEvent.click(runButton());
    await userEvent.click(await screen.findByRole('button', { name: 'Save' }));
    const dialog = screen.getByRole('dialog');
    const name = within(dialog).getByLabelText('Name');
    expect(name).toHaveValue('Saved Plan'); // the config's name
    await userEvent.clear(name);
    await userEvent.type(name, 'Retire at 65');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    expect(mocked.saveSimulation).toHaveBeenCalledWith('Retire at 65', simulationResult());
    expect(await screen.findByText('Saved "Saved Plan".')).toBeInTheDocument();
    expect(mocked.listSimulations).toHaveBeenCalledTimes(2); // on mount, and after saving
  });

  it('loads a saved simulation into the form and shows its results', async () => {
    mocked.listSimulations.mockResolvedValue([summary]);
    mocked.loadSimulation.mockResolvedValue({ summary, result: simulationResult() });
    render(<SimulationPage />);

    await userEvent.click(await screen.findByRole('tab', { name: 'Saved Simulations (1)' }));
    await userEvent.click(screen.getByRole('button', { name: 'Load Saved Plan' }));

    expect(await screen.findByText(/Loaded "Saved Plan"/)).toBeInTheDocument();
    expect(screen.getByText('Simulation Results')).toBeInTheDocument();
    // The form (collapsed after loading) holds the saved inputs: running again sends them unchanged
    mocked.runSimulation.mockResolvedValue(simulationResult());
    await userEvent.click(screen.getByText('Simulation Parameters'));
    await userEvent.click(runButton());
    expect(mocked.runSimulation).toHaveBeenCalledWith(apiConfig);
  });
});

describe('SimulationPage Monte Carlo (US002-3, US002-4)', () => {
  it('runs the analysis with the form inputs, then loads a run in full from its seed', async () => {
    mocked.runMonteCarlo.mockResolvedValue(monteCarloResult());
    mocked.getMonteCarloRun.mockResolvedValue(monteCarloRunDetail(1));
    render(<SimulationPage />);

    await userEvent.click(screen.getByRole('button', { name: 'Run Monte Carlo (1,000 runs)' }));
    expect(await screen.findByText('Monte Carlo Analysis')).toBeInTheDocument();
    const config = mocked.runMonteCarlo.mock.lastCall![0];
    expect(config.rates.inflation).toBe(0.03); // the form's defaults, as decimals

    // Click the flat run (index 1, $1M every year) halfway along the chart
    const hitArea = screen.getByTestId('run-hit-area');
    (hitArea as unknown as SVGRectElement).ownerSVGElement!.getBoundingClientRect = () =>
      ({ left: 0, top: 0, right: 600, bottom: 300, width: 600, height: 300, x: 0, y: 0, toJSON: () => ({}) });
    const [x, y, width, height] = ['x', 'y', 'width', 'height'].map(a => Number(hitArea.getAttribute(a)));
    fireEvent.click(hitArea, { clientX: x + width / 2, clientY: y + height / 2 }); // $1M of a $2M axis

    expect(screen.getByTestId('selected-run-summary')).toHaveTextContent('Run 2');
    await userEvent.click(screen.getByRole('button', { name: 'Load Details' }));

    expect(mocked.getMonteCarloRun).toHaveBeenCalledWith(config, 42, 1);
    expect(await screen.findByRole('table', { name: 'Market variables by year' })).toBeInTheDocument();
    expect(screen.getByText('Run 2: cash flow')).toBeInTheDocument();
  });
});

describe('SimulationPage invalid inputs (T002-043.4)', () => {
  it('lists the field errors the server returns', async () => {
    const response = {
      status: 422, statusText: '', headers: {}, config: { headers: new AxiosHeaders() },
      data: {
        error: 'Validation failed', statusCode: 422,
        validationErrors: [{ field: 'currentAge', message: 'Current age must be a whole number from 18 to 109' }]
      }
    };
    mocked.runSimulation.mockRejectedValue(new AxiosError('Request failed', 'ERR_BAD_REQUEST', undefined, undefined, response));
    render(<SimulationPage />);

    await userEvent.click(runButton());

    expect(await screen.findByText('The simulation was not run because some inputs are invalid:')).toBeInTheDocument();
    expect(screen.getByText('Current age must be a whole number from 18 to 109')).toBeInTheDocument();
  });
});
