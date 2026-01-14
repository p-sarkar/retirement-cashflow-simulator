import axios from 'axios';
import { SimulationConfig, SimulationResult, ComputationBreakdown, BreakdownRequest } from '../types/simulation';

const API_BASE_URL = 'http://localhost:8000';

export const runSimulation = async (config: SimulationConfig): Promise<SimulationResult> => {
  const response = await axios.post<SimulationResult>(`${API_BASE_URL}/api/simulate`, config);
  return response.data;
};

export const getBreakdown = async (config: SimulationConfig, targetAge: number): Promise<ComputationBreakdown> => {
  const request: BreakdownRequest = { config, targetAge };
  const response = await axios.post<ComputationBreakdown>(`${API_BASE_URL}/api/simulate/breakdown`, request);
  return response.data;
};

