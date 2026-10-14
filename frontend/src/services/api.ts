import axios from 'axios';
import { SimulationConfig, SimulationResult, ComputationBreakdown, BreakdownRequest } from '../types/simulation';

const API_BASE_URL = 'http://localhost:8000';
const REQUEST_TIMEOUT_MS = 30000;

export const runSimulation = async (config: SimulationConfig): Promise<SimulationResult> => {
  const response = await axios.post<SimulationResult>(`${API_BASE_URL}/api/simulate`, config, { timeout: REQUEST_TIMEOUT_MS });
  return response.data;
};

export const getBreakdown = async (config: SimulationConfig, targetAge: number): Promise<ComputationBreakdown> => {
  const request: BreakdownRequest = { config, targetAge };
  const response = await axios.post<ComputationBreakdown>(`${API_BASE_URL}/api/simulate/breakdown`, request, { timeout: REQUEST_TIMEOUT_MS });
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
  if (status >= 500) {
    return `The simulation service failed (HTTP ${status}${detail ? `: ${detail}` : ''}). If this persists, check that the API server is running.`;
  }
  return `The simulation request was rejected (HTTP ${status})${detail ? `: ${detail}` : '.'}`;
};
