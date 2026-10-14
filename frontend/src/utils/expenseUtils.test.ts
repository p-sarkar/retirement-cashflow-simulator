import { describe, expect, it } from 'vitest';
import {
  calculateQuarterlyPayment,
  simulationPeriod,
  formatValidationErrors,
  toAge,
  toYear,
  validateCashExpense,
  validateLoanExpense
} from './expenseUtils';
import { OneTimeExpense } from '../types/simulation';

const currentAge = 60;
const currentYear = 2025;

describe('toYear / toAge', () => {
  it('converts age to calendar year and back', () => {
    expect(toYear({ type: 'AGE', age: 67 }, currentAge, currentYear)).toBe(2032);
    expect(toAge({ type: 'YEAR', year: 2032 }, currentAge, currentYear)).toBe(67);
  });

  it('passes through values already in the requested form', () => {
    expect(toYear({ type: 'YEAR', year: 2035 }, currentAge, currentYear)).toBe(2035);
    expect(toAge({ type: 'AGE', age: 70 }, currentAge, currentYear)).toBe(70);
  });
});

describe('calculateQuarterlyPayment', () => {
  it('matches the standard amortization table (SC-006)', () => {
    expect(calculateQuarterlyPayment(100000, 6, 10)).toBeCloseTo(3342.71, 2);
  });

  it('uses simple division for 0% APR', () => {
    expect(calculateQuarterlyPayment(60000, 0, 5)).toBe(3000);
  });

  it('agrees with the API server for the same financed amount', () => {
    // Server: $40k inflated 2 years at 3%, 5% APR, 5 years -> $9,644.92/yr (4 x quarterly)
    expect(calculateQuarterlyPayment(40000 * 1.03 ** 2, 5, 5) * 4).toBeCloseTo(9644.92, 2);
  });
});

describe('validateCashExpense', () => {
  const valid = () => validateCashExpense('Roof', 1000, { type: 'AGE', age: 65 }, currentAge, currentYear);

  it('accepts a valid expense', () => {
    expect(valid()).toEqual([]);
  });

  it('rejects blank name, non-positive amount and out-of-range timing', () => {
    const fields = (errs: { field: string }[]) => errs.map(e => e.field);
    expect(fields(validateCashExpense(' ', 1000, { type: 'AGE', age: 65 }, currentAge, currentYear))).toEqual(['name']);
    expect(fields(validateCashExpense('Roof', 0, { type: 'AGE', age: 65 }, currentAge, currentYear))).toEqual(['amount']);
    expect(fields(validateCashExpense('Roof', 1000, { type: 'YEAR', year: 2024 }, currentAge, currentYear))).toEqual(['yearOrAge']);
    expect(fields(validateCashExpense('Roof', 1000, { type: 'YEAR', year: 2061 }, currentAge, currentYear))).toEqual(['yearOrAge']);
  });
});

describe('validateLoanExpense', () => {
  const loan = (overrides: Partial<{ principal: number; apr: number; term: number; down: number }> = {}) => {
    const { principal = 40000, apr = 5, term = 5, down = 0 } = overrides;
    return validateLoanExpense('Car', principal, apr, term, { type: 'YEAR', year: 2027 }, currentAge, currentYear, down)
      .map(e => e.field);
  };

  it('accepts a 1-year term and 0% APR', () => {
    expect(loan({ term: 1 })).toEqual([]);
    expect(loan({ apr: 0 })).toEqual([]);
  });

  it('rejects negative APR and non-positive term', () => {
    expect(loan({ apr: -1 })).toEqual(['aprPercent']);
    expect(loan({ term: 0 })).toEqual(['termYears']);
  });

  it('requires 0 <= down payment <= principal', () => {
    expect(loan({ down: 40000 })).toEqual([]);
    expect(loan({ down: 40001 })).toEqual(['downPayment']);
    expect(loan({ down: -1 })).toEqual(['downPayment']);
  });
});

describe('formatValidationErrors', () => {
  const expenses: OneTimeExpense[] = [
    { type: 'CASH', id: 'c', name: 'Roof', amount: -5, yearOrAge: { type: 'AGE', age: 65 } },
    { type: 'LOAN', id: 'l', name: 'Car Loan', principal: 1, aprPercent: -2, termYears: 5, startYearOrAge: { type: 'YEAR', year: 2027 } }
  ];

  it('names the expense each server error belongs to', () => {
    expect(formatValidationErrors([
      { field: 'oneTimeExpenses[0].amount', message: 'Amount must be greater than zero' },
      { field: 'oneTimeExpenses[1].aprPercent', message: 'APR cannot be negative' }
    ], expenses)).toEqual([
      'Expense 1 (Roof): Amount must be greater than zero',
      'Expense 2 (Car Loan): APR cannot be negative'
    ]);
  });

  it('falls back gracefully for unknown indexes and non-expense fields', () => {
    expect(formatValidationErrors([
      { field: 'oneTimeExpenses[5].name', message: 'Name required' },
      { field: 'currentAge', message: 'Invalid age' }
    ], expenses)).toEqual(['Expense 6: Name required', 'Invalid age']);
  });
});

describe('simulation period', () => {
  const fields = (errs: { field: string }[]) => errs.map(e => e.field);

  // currentYear 2025 / age 60: simulated years are 2026 (age 61) to 2050 (age 85)
  it('runs from the year after the current year through age 85', () => {
    expect(simulationPeriod(currentAge, currentYear)).toEqual({
      firstYear: 2026,
      lastYear: 2050,
      description: '2026 (age 61) to 2050 (age 85)'
    });
  });

  it('accepts timing only within the simulated years', () => {
    const timing = (yearOrAge: Parameters<typeof validateCashExpense>[2]) =>
      fields(validateCashExpense('Roof', 1000, yearOrAge, currentAge, currentYear));

    expect(timing({ type: 'YEAR', year: 2025 })).toEqual(['yearOrAge']);
    expect(timing({ type: 'YEAR', year: 2026 })).toEqual([]);
    expect(timing({ type: 'AGE', age: 85 })).toEqual([]);
    expect(timing({ type: 'AGE', age: 86 })).toEqual(['yearOrAge']);
  });

  it('lets a loan that starts in the last year run past the end', () => {
    expect(fields(validateLoanExpense('Car', 40000, 5, 10, { type: 'AGE', age: 85 }, currentAge, currentYear))).toEqual([]);
    expect(fields(validateLoanExpense('Car', 40000, 5, 10, { type: 'AGE', age: 86 }, currentAge, currentYear))).toEqual(['startYearOrAge']);
  });
});
