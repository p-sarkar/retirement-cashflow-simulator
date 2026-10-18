import { describe, expect, it } from 'vitest';
import { csvFileName, toCsv } from './csvExport';
import { RESULT_COLUMNS } from './resultColumns';
import { simulationResult } from '../test/fixtures';
import { YearlyResult } from '../types/simulation';

const parse = (csv: string) => csv.trim().split('\r\n').map(line => line.split(','));

describe('CSV export (FR002-010a)', () => {
  it('has the same columns as the results table', () => {
    const [header] = parse(toCsv(simulationResult().yearlyResults));
    expect(header).toHaveLength(RESULT_COLUMNS.length);
    expect(header.slice(0, 4)).toEqual(['Time', 'Age', 'Spend Bucket', 'SB Cap']);
    expect(header).toContain('TDA for Spend'); // table header without the tree lines
    expect(header.slice(-4)).toEqual(['Income Gap', 'Equity Return (%)', 'Inflation (%)', 'Bond Yield (%)']);
  });

  it('writes one row per year with plain numbers, empty for not-applicable values', () => {
    const [header, working, retired] = parse(toCsv(simulationResult().yearlyResults));
    const cell = (row: string[], name: string) => row[header.indexOf(name)];

    expect(cell(working, 'Time')).toBe('2026');
    expect(cell(working, 'Age')).toBe('61');
    expect(cell(working, 'Interest')).toBe('3500.50');
    expect(cell(working, 'Total Portfolio')).toBe('1900000.00');
    expect(cell(working, 'Income Gap')).toBe(''); // ~ in the table
    expect(cell(retired, 'Income Gap')).toBe('45000.25');
  });

  it("writes each year's market rates as percentages, empty where none apply (FR002-009)", () => {
    const [header, start, retired] = parse(toCsv(simulationResult().yearlyResults));
    const rates = (row: string[]) => row.slice(header.indexOf('Equity Return (%)'));

    expect(rates(start)).toEqual(['', '', '']); // the start-year snapshot
    expect(rates(retired)).toEqual(['-12.34', '3.10', '4.50']);
  });

  it('leaves the market rates empty for a result saved before rows had them', () => {
    const saved: Partial<YearlyResult> = { ...simulationResult().yearlyResults[1] };
    delete saved.market;
    const [, row] = parse(toCsv([saved as YearlyResult]));
    expect(row.slice(-3)).toEqual(['', '', '']);
  });

  it('labels quarterly rows by quarter', () => {
    const rows = parse(toCsv(simulationResult().quarterlyResults));
    expect(rows.slice(1).map(row => row[0])).toEqual(['2026 Q1', '2026 Q2']);
  });

  it('names the file after the simulation and view', () => {
    expect(csvFileName('My Retirement Plan', 'yearly')).toBe('my-retirement-plan-yearly.csv');
    expect(csvFileName("Mou's plan (v2)", 'quarterly')).toBe('mou-s-plan-v2-quarterly.csv');
    expect(csvFileName('', 'yearly')).toBe('simulation-yearly.csv');
  });
});
