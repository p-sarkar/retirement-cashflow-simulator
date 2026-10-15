import { afterEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import ResultsTable from './ResultsTable';
import * as csv from '../utils/csvExport';
import { RESULT_COLUMNS } from '../utils/resultColumns';
import { simulationResult } from '../test/fixtures';

afterEach(() => vi.restoreAllMocks());

describe('ResultsTable', () => {
  it('shows the shared columns, with ~ for values that do not apply', () => {
    render(<ResultsTable result={simulationResult()} config={null} />);
    const headers = screen.getAllByRole('columnheader').map(th => th.textContent);

    expect(headers).toEqual(['Details', ...RESULT_COLUMNS.map(c => c.header)]);
    expect(screen.getAllByRole('row')[1]).toHaveTextContent('~');
  });

  it('exports the current view as CSV (FR-010a)', () => {
    const download = vi.spyOn(csv, 'downloadCsv').mockImplementation(() => {});
    render(<ResultsTable result={simulationResult()} config={null} />);

    fireEvent.click(screen.getByRole('button', { name: 'quarterly' }));
    fireEvent.click(screen.getByRole('button', { name: 'Export CSV' }));

    expect(download).toHaveBeenCalledTimes(1);
    const [fileName, content] = download.mock.lastCall!;
    expect(fileName).toBe('saved-plan-quarterly.csv');
    expect(content.split('\r\n')[1]).toMatch(/^2026 Q1,61,/);
  });

  it('can hide the breakdown column and add header actions', () => {
    render(<ResultsTable result={simulationResult()} config={null} showBreakdown={false} headerActions={<button>Save</button>} />);
    expect(screen.queryByRole('columnheader', { name: 'Details' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
  });
});
