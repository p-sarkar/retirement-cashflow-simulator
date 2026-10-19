import { afterEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import ResultsTable from './ResultsTable';
import * as csv from '../utils/csvExport';
import { RESULT_COLUMNS } from '../utils/resultColumns';
import { guardrailsResult, simulationResult } from '../test/fixtures';

afterEach(() => vi.restoreAllMocks());

describe('ResultsTable', () => {
  it('shows the shared columns, with ~ for values that do not apply', () => {
    render(<ResultsTable result={simulationResult()} config={null} />);
    const headers = screen.getAllByRole('columnheader').map(th => th.textContent);

    expect(headers).toEqual(['Details', ...RESULT_COLUMNS.map(c => c.header)]);
    expect(screen.getAllByRole('row')[1]).toHaveTextContent('~');
  });

  it("shows each year's market rates as percentages (FR002-009)", () => {
    render(<ResultsTable result={simulationResult()} config={null} />);
    const cells = screen.getAllByRole('row')[2].querySelectorAll('td');

    expect([...cells].slice(-3).map(td => td.textContent)).toEqual(['-12.3%', '3.1%', '4.5%']);
  });

  it('exports the current view as CSV (FR002-010a)', () => {
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

  it('adds configured Wants, the review probability and its outcome for guardrails (spec 006)', () => {
    render(<ResultsTable result={guardrailsResult()} config={null} />);
    const headers = screen.getAllByRole('columnheader').map(th => th.textContent);
    const wants = headers.indexOf('Wants');
    expect(headers.slice(wants, wants + 4)).toEqual(['Wants', 'Wants (configured)', 'Success Probability', 'Guardrail']);

    const cutRow = screen.getAllByRole('row')[3]; // 2032
    expect(cutRow).toHaveTextContent('$30,900');
    expect(cutRow).toHaveTextContent('42.8%');
    expect(cutRow).toHaveTextContent('Cut');
  });

  it('exports the guardrails columns too', () => {
    const download = vi.spyOn(csv, 'downloadCsv').mockImplementation(() => {});
    render(<ResultsTable result={guardrailsResult()} config={null} />);
    fireEvent.click(screen.getByRole('button', { name: 'Export CSV' }));
    const [, content] = download.mock.lastCall!;
    expect(content.split('\r\n')[0]).toContain('Wants,Wants (configured),Success Probability (%),Guardrail');
  });

  it('has no guardrails columns for the existing strategy', () => {
    render(<ResultsTable result={simulationResult()} config={null} />);
    expect(screen.queryByRole('columnheader', { name: 'Guardrail' })).not.toBeInTheDocument();
  });
});
