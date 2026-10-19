import { describe, expect, it, vi } from 'vitest';
import axios, { AxiosError, AxiosHeaders, AxiosResponse } from 'axios';
import { describeRequestError, getSpendingStrategies } from './api';

const response = (status: number, data: unknown): AxiosResponse =>
  ({ status, data, statusText: '', headers: {}, config: { headers: new AxiosHeaders() } }) as AxiosResponse;

describe('describeRequestError (T003-071)', () => {
  it('explains an unreachable server', () => {
    const err = new AxiosError('Network Error', AxiosError.ERR_NETWORK);
    expect(describeRequestError(err)).toMatch(/Could not reach the simulation service/);
  });

  it('explains a timeout', () => {
    const err = new AxiosError('timeout of 30000ms exceeded', AxiosError.ECONNABORTED);
    expect(describeRequestError(err)).toMatch(/did not respond within 30 seconds/);
  });

  it('points at the API server when the backend reports 503', () => {
    const err = new AxiosError('Request failed', AxiosError.ERR_BAD_RESPONSE, undefined, undefined,
      response(503, { error: 'Simulation API server is unavailable' }));
    expect(describeRequestError(err)).toMatch(/simulation API server is not reachable/);
  });

  it('includes the server error for a 5xx', () => {
    const err = new AxiosError('Request failed', AxiosError.ERR_BAD_RESPONSE, undefined, undefined,
      response(500, { error: 'Internal Server Error' }));
    expect(describeRequestError(err)).toBe(
      'The simulation service failed (HTTP 500: Internal Server Error). If this persists, check that the API server is running.'
    );
  });

  it('includes a plain-text body for a 4xx', () => {
    const err = new AxiosError('Request failed', AxiosError.ERR_BAD_REQUEST, undefined, undefined,
      response(400, 'Age 99 not found in simulation results'));
    expect(describeRequestError(err)).toBe('The simulation request was rejected (HTTP 400): Age 99 not found in simulation results');
  });

  it('falls back to the message of a non-HTTP error', () => {
    expect(describeRequestError(new Error('boom'))).toBe('boom');
  });
});

describe('getSpendingStrategies (spec 006)', () => {
  it('fetches the strategy catalog from the backend', async () => {
    const strategies = [{ id: 'RISK_BASED_GUARDRAILS_V1', name: 'Risk-Based Guardrails', description: 'd', settings: 'GUARDRAILS' }];
    const get = vi.spyOn(axios, 'get').mockResolvedValue(response(200, strategies));

    expect(await getSpendingStrategies()).toEqual(strategies);
    expect(get).toHaveBeenCalledWith('http://localhost:8000/api/spending-strategies', expect.anything());
    get.mockRestore();
  });
});
