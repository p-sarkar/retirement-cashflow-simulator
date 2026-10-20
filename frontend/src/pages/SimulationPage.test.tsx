import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AxiosError, AxiosHeaders } from 'axios';
import SimulationPage from './SimulationPage';
import * as api from '../services/api';
import * as inputsFile from '../utils/inputsFile';
import { apiConfig, monteCarloResult, monteCarloRunDetail, simulationResult } from '../test/fixtures';
import { SavedSimulationSummary } from '../types/simulation';

vi.mock('../services/api', async importOriginal => ({
  ...(await importOriginal<typeof import('../services/api')>()),
  getMarketPresets: vi.fn(),
  getSpendingStrategies: vi.fn(),
  listSimulations: vi.fn(),
  loadSimulation: vi.fn(),
  saveSimulation: vi.fn(),
  updateSimulation: vi.fn(),
  renameSimulation: vi.fn(),
  deleteSimulation: vi.fn(),
  exportInputs: vi.fn(),
  importInputs: vi.fn(),
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
  finalTotalBalance: 1900000, endAge: 90, updatedAt: null, version: '1.1.29', isCurrentVersion: true
};
const running = { version: '1.1.29', buildTime: 'b', serverStartTime: 's', build: '1.1.29+3.gabc1234' };

beforeEach(() => {
  vi.resetAllMocks();
  mocked.getMarketPresets.mockResolvedValue([]);
  mocked.getSpendingStrategies.mockResolvedValue([]);
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
    mocked.loadSimulation.mockResolvedValue({ summary, result: simulationResult(), apiMetadata: running });
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

describe('SimulationPage results from an older version (US005-1)', () => {
  const older = { ...summary, version: '1.1.28', isCurrentVersion: false };
  const savedResult = { ...simulationResult(), apiMetadata: { ...running, version: '1.1.28' } };
  const rerunResult = { ...simulationResult(), summary: { ...simulationResult().summary, finalTotalBalance: 1500000 } };

  const loadOlder = async () => {
    mocked.listSimulations.mockResolvedValue([older]);
    mocked.loadSimulation.mockResolvedValue({ summary: older, result: savedResult, apiMetadata: running });
    render(<SimulationPage />);
    await userEvent.click(await screen.findByRole('tab', { name: 'Saved Simulations (1)' }));
    await userEvent.click(screen.getByRole('button', { name: 'Load Saved Plan' }));
    await screen.findByText(/Loaded "Saved Plan"/);
  };

  it('flags the saved results, re-runs the saved inputs and switches between both (FR005-002 to FR005-005)', async () => {
    await loadOlder();
    expect(screen.getByText(/computed by v1\.1\.28/)).toBeInTheDocument();
    expect(screen.getByText('API v1.1.28')).toBeInTheDocument(); // the saved results are shown

    mocked.runSimulation.mockResolvedValue(rerunResult);
    await userEvent.click(screen.getByRole('button', { name: 'Re-run with v1.1.29' }));
    expect(mocked.runSimulation).toHaveBeenCalledWith(savedResult.config);
    expect(await screen.findByRole('table', { name: 'Saved and re-run results' })).toHaveTextContent('$1,500,000');
    expect(screen.getByText('API v1.1.29')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Saved (v1.1.28)' }));
    expect(screen.getByText('API v1.1.28')).toBeInTheDocument();
    expect(mocked.saveSimulation).not.toHaveBeenCalled(); // nothing stored until the user saves
  });

  it('drops the comparison when other inputs are run (FR005-005)', async () => {
    await loadOlder();
    expect(screen.getByText(/computed by v1\.1\.28/)).toBeInTheDocument();
    mocked.runSimulation.mockResolvedValue(simulationResult());
    await userEvent.click(screen.getByText('Simulation Parameters'));
    await userEvent.click(runButton());

    await screen.findByText('API v1.1.29');
    expect(screen.queryByText(/computed by v1\.1\.28/)).not.toBeInTheDocument();
  });

  it('shows no notice for results computed by the running version (FR005-006)', async () => {
    mocked.listSimulations.mockResolvedValue([summary]);
    mocked.loadSimulation.mockResolvedValue({ summary, result: simulationResult(), apiMetadata: running });
    render(<SimulationPage />);
    await userEvent.click(await screen.findByRole('tab', { name: 'Saved Simulations (1)' }));
    await userEvent.click(screen.getByRole('button', { name: 'Load Saved Plan' }));
    await screen.findByText(/Loaded "Saved Plan"/);

    expect(screen.queryByText(/computed by/)).not.toBeInTheDocument();
  });
});

describe('SimulationPage update, rename and delete (US005-2)', () => {
  const notFound = () => new AxiosError('Request failed', 'ERR_BAD_REQUEST', undefined, undefined, {
    status: 404, statusText: '', headers: {}, config: { headers: new AxiosHeaders() },
    data: { error: 'Saved simulation abc not found', statusCode: 404 }
  });

  const loadSaved = async () => {
    mocked.listSimulations.mockResolvedValue([summary]);
    mocked.loadSimulation.mockResolvedValue({ summary, result: simulationResult(), apiMetadata: running });
    render(<SimulationPage />);
    await userEvent.click(await screen.findByRole('tab', { name: 'Saved Simulations (1)' }));
    await userEvent.click(screen.getByRole('button', { name: 'Load Saved Plan' }));
    await screen.findByText(/Loaded "Saved Plan"/);
  };

  it('updates the loaded simulation with the results shown (FR005-007)', async () => {
    await loadSaved();
    mocked.updateSimulation.mockResolvedValue({ ...summary, updatedAt: '2026-10-18T10:00:00-07:00' });
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: "Update 'Saved Plan'" }));

    expect(mocked.updateSimulation).toHaveBeenCalledWith('abc', simulationResult());
    expect(await screen.findByText('Updated "Saved Plan".')).toBeInTheDocument();
    expect(mocked.listSimulations).toHaveBeenCalledTimes(2);
  });

  it('keeps the results of a deleted loaded simulation, which can then only be saved as new (FR005-010)', async () => {
    await loadSaved();
    mocked.deleteSimulation.mockResolvedValue(undefined);
    mocked.listSimulations.mockResolvedValue([]);
    await userEvent.click(screen.getByRole('tab', { name: 'Saved Simulations (1)' }));
    await userEvent.click(screen.getByRole('button', { name: 'Delete Saved Plan' }));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete' }));
    expect(mocked.deleteSimulation).toHaveBeenCalledWith('abc');
    expect(await screen.findByText('Deleted "Saved Plan".')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('tab', { name: 'Cash Flow' }));
    expect(screen.getByText('Simulation Results')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(within(screen.getByRole('dialog')).queryByRole('button', { name: /Update/ })).not.toBeInTheDocument();
  });

  it('offers to save as new when the loaded simulation no longer exists (FR005-012)', async () => {
    await loadSaved();
    mocked.updateSimulation.mockRejectedValue(notFound());
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: "Update 'Saved Plan'" }));

    expect(await screen.findByText(/"Saved Plan" no longer exists/)).toBeInTheDocument();
    expect(mocked.listSimulations).toHaveBeenCalledTimes(2); // refreshed
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(within(screen.getByRole('dialog')).queryByRole('button', { name: /Update/ })).not.toBeInTheDocument();
  });

  it('renames from the list and refreshes it (FR005-008)', async () => {
    mocked.listSimulations.mockResolvedValue([summary]);
    mocked.renameSimulation.mockResolvedValue({ ...summary, name: 'Retire at 66' });
    render(<SimulationPage />);
    await userEvent.click(await screen.findByRole('tab', { name: 'Saved Simulations (1)' }));
    await userEvent.click(screen.getByRole('button', { name: 'Rename Saved Plan' }));
    const name = within(screen.getByRole('dialog')).getByLabelText('Name');
    await userEvent.clear(name);
    await userEvent.type(name, 'Retire at 66');
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Rename' }));

    expect(mocked.renameSimulation).toHaveBeenCalledWith('abc', 'Retire at 66');
    expect(mocked.listSimulations).toHaveBeenCalledTimes(2);
  });
});

describe('SimulationPage export and import inputs (US005-3)', () => {
  const file = { format: 'retirement-simulator-inputs', formatVersion: 1, appVersion: '1.1.29', config: apiConfig };

  it('fills the form from an imported file without running, and forgets the loaded simulation (FR005-014)', async () => {
    mocked.listSimulations.mockResolvedValue([summary]);
    mocked.loadSimulation.mockResolvedValue({ summary, result: simulationResult(), apiMetadata: running });
    mocked.importInputs.mockResolvedValue({ ...apiConfig, name: 'Imported plan' });
    render(<SimulationPage />);
    await userEvent.click(await screen.findByRole('tab', { name: 'Saved Simulations (1)' }));
    await userEvent.click(screen.getByRole('button', { name: 'Load Saved Plan' }));
    await screen.findByText(/Loaded "Saved Plan"/);

    await userEvent.click(screen.getByText('Simulation Parameters'));
    await userEvent.upload(screen.getByLabelText('Import inputs'), new File([JSON.stringify(file)], 'plan-inputs.json'));

    expect(await screen.findByText(/Imported the inputs of "Imported plan"/)).toBeInTheDocument();
    expect(screen.getAllByLabelText('Name')[0]).toHaveValue('Imported plan'); // the plan's name (expenses have names too)
    expect(mocked.runSimulation).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole('button', { name: 'Save' })); // the loaded results are still shown
    expect(within(screen.getByRole('dialog')).queryByRole('button', { name: /Update/ })).not.toBeInTheDocument();
  });

  it("exports a saved simulation's inputs from the list, named after the simulation (FR005-013)", async () => {
    const download = vi.spyOn(inputsFile, 'downloadJson').mockImplementation(() => {});
    const renamed = { ...summary, name: 'Retire at 65' }; // renamed after saving: its inputs keep the old name
    mocked.listSimulations.mockResolvedValue([renamed]);
    mocked.loadSimulation.mockResolvedValue({ summary: renamed, result: simulationResult(), apiMetadata: running });
    mocked.exportInputs.mockResolvedValue(file);
    render(<SimulationPage />);
    await userEvent.click(await screen.findByRole('tab', { name: 'Saved Simulations (1)' }));
    await userEvent.click(screen.getByRole('button', { name: 'Export Retire at 65' }));

    await vi.waitFor(() => expect(download).toHaveBeenCalledWith('retire-at-65-inputs.json', file));
    expect(mocked.exportInputs).toHaveBeenCalledWith(apiConfig);
    download.mockRestore();
  });
});

describe('SimulationPage version and build', () => {
  it('shows the version and the build that computed the results', async () => {
    mocked.runSimulation.mockResolvedValue(simulationResult());
    render(<SimulationPage />);
    await userEvent.click(runButton());

    expect(await screen.findByText('API v1.1.29')).toBeInTheDocument();
    expect(screen.getByText('Build: 1.1.29+3.gabc1234, b')).toBeInTheDocument();
  });

  it('shows only the build time for results saved before builds were recorded', async () => {
    const older = simulationResult();
    mocked.runSimulation.mockResolvedValue({ ...older, apiMetadata: { ...older.apiMetadata, build: '' } });
    render(<SimulationPage />);
    await userEvent.click(runButton());

    expect(await screen.findByText('Build: b')).toBeInTheDocument();
  });
});
