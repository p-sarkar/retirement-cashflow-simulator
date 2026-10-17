import axios from 'axios';
import {
  SimulationConfig, SimulationResult, YearBreakdown, BreakdownRequest, MarketPreset,
  MonteCarloRequest, MonteCarloResult, MonteCarloRunBreakdownRequest, MonteCarloRunDetail, MonteCarloRunRequest,
  SaveSimulationRequest, SavedSimulation, SavedSimulationSummary
} from '../types/simulation';

const API_BASE_URL = 'http://localhost:8000';
const REQUEST_TIMEOUT_MS = 30000;
export const MONTE_CARLO_RUNS = 1000;

export const runSimulation = async (config: SimulationConfig): Promise<SimulationResult> => {
  const response = await axios.post<SimulationResult>(`${API_BASE_URL}/api/simulate`, config, { timeout: REQUEST_TIMEOUT_MS });
  return response.data;
};

export const getBreakdown = async (config: SimulationConfig, targetAge: number): Promise<YearBreakdown> => {
  const request: BreakdownRequest = { config, targetAge };
  const response = await axios.post<YearBreakdown>(`${API_BASE_URL}/api/simulate/breakdown`, request, { timeout: REQUEST_TIMEOUT_MS });
  return response.data;
};

export const getMarketPresets = async (): Promise<MarketPreset[]> => {
  const response = await axios.get<MarketPreset[]>(`${API_BASE_URL}/api/market-presets`, { timeout: REQUEST_TIMEOUT_MS });
  return response.data;
};

// --- Saved simulations (US002-2) ---

export const saveSimulation = async (name: string, result: SimulationResult): Promise<SavedSimulationSummary> => {
  const request: SaveSimulationRequest = { name, result };
  const response = await axios.post<SavedSimulationSummary>(`${API_BASE_URL}/api/simulations`, request, { timeout: REQUEST_TIMEOUT_MS });
  return response.data;
};

export const listSimulations = async (): Promise<SavedSimulationSummary[]> => {
  const response = await axios.get<SavedSimulationSummary[]>(`${API_BASE_URL}/api/simulations`, { timeout: REQUEST_TIMEOUT_MS });
  return response.data;
};

export const loadSimulation = async (id: string): Promise<SavedSimulation> => {
  const response = await axios.get<SavedSimulation>(`${API_BASE_URL}/api/simulations/${encodeURIComponent(id)}`, { timeout: REQUEST_TIMEOUT_MS });
  return response.data;
};

// --- Monte Carlo (US002-3, US002-4) ---

export const runMonteCarlo = async (config: SimulationConfig, runs = MONTE_CARLO_RUNS): Promise<MonteCarloResult> => {
  const request: MonteCarloRequest = { config, runs };
  const response = await axios.post<MonteCarloResult>(`${API_BASE_URL}/api/simulate/monte-carlo`, request, { timeout: REQUEST_TIMEOUT_MS });
  return response.data;
};

/** Full details of one Monte Carlo run, rebuilt on the server from the analysis seed. */
export const getMonteCarloRun = async (config: SimulationConfig, seed: number, runIndex: number): Promise<MonteCarloRunDetail> => {
  const request: MonteCarloRunRequest = { config, seed, runIndex };
  const response = await axios.post<MonteCarloRunDetail>(`${API_BASE_URL}/api/simulate/monte-carlo/run`, request, { timeout: REQUEST_TIMEOUT_MS });
  return response.data;
};

/** The computation breakdown for one year of one Monte Carlo run (spec 004 US004-6). */
export const getMonteCarloRunBreakdown = async (
  config: SimulationConfig, seed: number, runIndex: number, targetAge: number
): Promise<YearBreakdown> => {
  const request: MonteCarloRunBreakdownRequest = { config, seed, runIndex, targetAge };
  const response = await axios.post<YearBreakdown>(`${API_BASE_URL}/api/simulate/monte-carlo/run/breakdown`, request, { timeout: REQUEST_TIMEOUT_MS });
  return response.data;
};

/**
 * Turn a failed request into a message the user can act on. 422 validation errors are handled
 * separately by the caller, since they list per-field problems.
 */
export const describeRequestError = (err: unknown): string => {
  if (!axios.isAxiosError(err)) {
    return err instanceof Error ? err.message : 'Something went wrong running the simulation.';
  }
  if (err.code === 'ECONNABORTED' || err.code === 'ETIMEDOUT') {
    return `The simulation server did not respond within ${REQUEST_TIMEOUT_MS / 1000} seconds. Please try again.`;
  }
  if (!err.response) {
    return `Could not reach the simulation service at ${API_BASE_URL}. Check that the backend and API server are running.`;
  }
  const { status, data } = err.response;
  const detail = typeof data === 'string' ? data : (data as { error?: string } | undefined)?.error;
  if (status === 503) {
    // The backend is up but cannot reach the Kotlin API server
    return 'The simulation API server is not reachable. Check that it is running (cd api-server && ./gradlew run), then try again.';
  }
  if (status >= 500) {
    return `The simulation service failed (HTTP ${status}${detail ? `: ${detail}` : ''}). If this persists, check that the API server is running.`;
  }
  return `The simulation request was rejected (HTTP ${status})${detail ? `: ${detail}` : '.'}`;
};
