import React, { useState } from 'react';
import { Button } from '@mui/material';
import axios from 'axios';
import { describeRequestError, exportInputs, importInputs } from '../services/api';
import { ErrorResponse, SimulationConfig, ValidationErrorResponse } from '../types/simulation';
import { formatValidationErrors } from '../utils/expenseUtils';
import { downloadJson, inputsFileName, readInputsFile } from '../utils/inputsFile';

export interface InputsFileProblem {
  message: string;
  details: string[];
}

interface InputsFileButtonsProps {
  getConfig: () => SimulationConfig; // the inputs to export: the form's, as they stand
  onImported: (config: SimulationConfig) => void;
  onError: (problem: InputsFileProblem) => void;
}

/** A failed export or import as a message, listing each invalid input for a 422 (FR005-015). */
function toProblem(err: unknown, action: 'exported' | 'imported', config?: SimulationConfig): InputsFileProblem {
  if (axios.isAxiosError<ValidationErrorResponse & ErrorResponse>(err)) {
    const data = err.response?.data;
    if (err.response?.status === 422 && data?.validationErrors) {
      return {
        message: `The ${action === 'imported' ? 'file was not imported' : 'inputs were not exported'} because some inputs are invalid:`,
        details: formatValidationErrors(data.validationErrors, config?.oneTimeExpenses ?? [])
      };
    }
    if (err.response?.status === 400 && action === 'imported') {
      return { message: `This file isn't a simulation inputs file: ${data?.error ?? ''}`.trim(), details: [] };
    }
  }
  return { message: describeRequestError(err), details: [] };
}

/** Export the form's inputs to a file and import a file into the form (US005-3). Nothing is run or saved. */
const InputsFileButtons: React.FC<InputsFileButtonsProps> = ({ getConfig, onImported, onError }) => {
  const [busy, setBusy] = useState(false);

  const handleExport = async () => {
    const config = getConfig();
    setBusy(true);
    try {
      downloadJson(inputsFileName(config.name), await exportInputs(config));
    } catch (err) {
      onError(toProblem(err, 'exported', config));
    } finally {
      setBusy(false);
    }
  };

  const handleFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = ''; // the same file can be chosen again
    if (!file) return;
    const read = await readInputsFile(file);
    if ('problem' in read) {
      onError({ message: read.problem, details: [] });
      return;
    }
    setBusy(true);
    try {
      onImported(await importInputs(read.content));
    } catch (err) {
      onError(toProblem(err, 'imported'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <Button variant="text" onClick={handleExport} disabled={busy}>Export inputs</Button>
      <Button variant="text" component="label" disabled={busy}>
        Import inputs
        <input type="file" accept="application/json,.json" hidden aria-label="Import inputs" onChange={handleFile} />
      </Button>
    </>
  );
};

export default InputsFileButtons;
