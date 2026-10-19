import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AxiosError, AxiosHeaders } from 'axios';
import InputsFileButtons from './InputsFileButtons';
import * as api from '../services/api';
import * as inputsFile from '../utils/inputsFile';
import { apiConfig } from '../test/fixtures';

vi.mock('../services/api', async importOriginal => ({
  ...(await importOriginal<typeof import('../services/api')>()),
  exportInputs: vi.fn(),
  importInputs: vi.fn()
}));
const mocked = vi.mocked(api);

const file = { format: 'retirement-simulator-inputs', formatVersion: 1, appVersion: '1.1.29', config: apiConfig };
const httpError = (status: number, data: unknown) => new AxiosError('Request failed', 'ERR_BAD_REQUEST', undefined, undefined, {
  status, statusText: '', headers: {}, config: { headers: new AxiosHeaders() }, data
});
const upload = (content: string, name = 'plan-inputs.json') => new File([content], name, { type: 'application/json' });

let onImported: ReturnType<typeof vi.fn>;
let onError: ReturnType<typeof vi.fn>;
beforeEach(() => {
  vi.resetAllMocks();
  onImported = vi.fn();
  onError = vi.fn();
});
afterEach(() => vi.restoreAllMocks());

const renderButtons = () =>
  render(<InputsFileButtons getConfig={() => apiConfig} onImported={onImported} onError={onError} />);

describe('InputsFileButtons (US005-3)', () => {
  it('exports the inputs as <name>-inputs.json with the file the server wrote (FR005-013)', async () => {
    const download = vi.spyOn(inputsFile, 'downloadJson').mockImplementation(() => {});
    mocked.exportInputs.mockResolvedValue(file);
    renderButtons();

    await userEvent.click(screen.getByRole('button', { name: 'Export inputs' }));
    expect(mocked.exportInputs).toHaveBeenCalledWith(apiConfig);
    expect(download).toHaveBeenCalledWith('saved-plan-inputs.json', file);
  });

  it('imports a file and hands over its inputs without running anything (FR005-014)', async () => {
    mocked.importInputs.mockResolvedValue(apiConfig);
    renderButtons();

    await userEvent.upload(screen.getByLabelText('Import inputs'), upload(JSON.stringify(file)));
    expect(mocked.importInputs).toHaveBeenCalledWith(file);
    expect(onImported).toHaveBeenCalledWith(apiConfig);
  });

  it('refuses a file over 1 MB or that is not JSON before any request (FR005-016)', async () => {
    renderButtons();
    const input = screen.getByLabelText('Import inputs');

    await userEvent.upload(input, upload('x'.repeat(1024 * 1024 + 1)));
    expect(onError).toHaveBeenLastCalledWith({ message: "This file is too large to be a simulation inputs file (over 1 MB).", details: [] });
    await userEvent.upload(input, upload('Year,Age\n2026,51', 'plan.csv'));
    expect(onError).toHaveBeenLastCalledWith({ message: "This file isn't a simulation inputs file.", details: [] });
    expect(mocked.importInputs).not.toHaveBeenCalled();
    expect(onImported).not.toHaveBeenCalled();
  });

  it("says when the server doesn't recognise the file (FR005-016)", async () => {
    mocked.importInputs.mockRejectedValue(httpError(400, { error: 'The file was written by a newer version of the app (format 2)', statusCode: 400 }));
    renderButtons();

    await userEvent.upload(screen.getByLabelText('Import inputs'), upload(JSON.stringify(file)));
    expect(onError).toHaveBeenCalledWith({
      message: "This file isn't a simulation inputs file: The file was written by a newer version of the app (format 2)", details: []
    });
  });

  it('lists every invalid input of an imported file and leaves the form alone (FR005-015)', async () => {
    mocked.importInputs.mockRejectedValue(httpError(422, {
      error: 'Validation failed', statusCode: 422, validationErrors: [
        { field: 'retirementAge', message: 'Retirement age must be a whole number from 18 to 110' },
        { field: 'currentYear', message: 'Current year must be a whole number from 1900 to 2200' }
      ]
    }));
    renderButtons();

    await userEvent.upload(screen.getByLabelText('Import inputs'), upload(JSON.stringify(file)));
    expect(onError).toHaveBeenCalledWith({
      message: 'The file was not imported because some inputs are invalid:',
      details: ['Retirement age must be a whole number from 18 to 110', 'Current year must be a whole number from 1900 to 2200']
    });
    expect(onImported).not.toHaveBeenCalled();
  });
});
