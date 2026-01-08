import axios from 'axios';
import { SimulationConfig, SimulationResult } from '../types/simulation';

const API_BASE_URL = 'http://localhost:8000';

export const runSimulation = async (config: SimulationConfig): Promise<SimulationResult> => {
  const response = await axios.post<SimulationResult>(`${API_BASE_URL}/api/simulate`, config);
  return response.data;
};
