/**
 * Utility functions for one-time expense calculations and conversions.
 */

import { YearOrAge, OneTimeExpense, ApiValidationError } from '../types/simulation';

/**
 * Age at which the simulation ends. Mirrors SimulationPeriod.END_AGE on the API server; fixed until
 * it becomes a field on the configuration form.
 */
export const SIMULATION_END_AGE = 85;

/**
 * The simulated years: the current year holds the starting balances, so simulation runs from the
 * following year through the year the user reaches SIMULATION_END_AGE.
 */
export function simulationPeriod(currentAge: number, currentYear: number) {
  const firstYear = currentYear + 1;
  const lastYear = currentYear + (SIMULATION_END_AGE - currentAge);
  return {
    firstYear,
    lastYear,
    description: `${firstYear} (age ${currentAge + 1}) to ${lastYear} (age ${SIMULATION_END_AGE})`
  };
}

/**
 * Convert YearOrAge to a calendar year.
 */
export function toYear(yearOrAge: YearOrAge, currentAge: number, currentYear: number): number {
  if (yearOrAge.type === 'YEAR') {
    return yearOrAge.year;
  }
  return currentYear + (yearOrAge.age - currentAge);
}

/**
 * Convert YearOrAge to an age.
 */
export function toAge(yearOrAge: YearOrAge, currentAge: number, currentYear: number): number {
  if (yearOrAge.type === 'AGE') {
    return yearOrAge.age;
  }
  return currentAge + (yearOrAge.year - currentYear);
}

/**
 * Calculate quarterly payment using standard amortization formula.
 * Q = P[r(1+r)^n] / [(1+r)^n - 1]
 *
 * r is the quarterly rate (APR / 4) and n the number of quarters; Q is the level payment that
 * pays off P exactly after n quarters. Mirrors LoanExpense.calculateQuarterlyPayment on the API
 * server, which computes the payment actually simulated; this copy is only the form's estimate.
 *
 * For 0% APR the formula divides by zero, so it uses simple division: P / n
 *
 * @param financedAmount The amount to be financed (principal - down payment)
 * @param aprPercent Annual percentage rate
 * @param termYears Loan term in years
 */
export function calculateQuarterlyPayment(financedAmount: number, aprPercent: number, termYears: number): number {
  const n = termYears * 4;

  if (aprPercent === 0) {
    return financedAmount / n;
  }

  const r = aprPercent / 100 / 4; // quarterly interest rate
  const onePlusR = 1 + r;
  const onePlusRPowN = Math.pow(onePlusR, n); // growth of $1 over the term

  return (financedAmount * r * onePlusRPowN) / (onePlusRPowN - 1);
}

/**
 * Format a number as currency.
 */
export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount);
}

/**
 * Generate a unique ID for an expense.
 */
export function generateExpenseId(): string {
  return crypto.randomUUID();
}

/**
 * Validate a cash expense.
 */
export interface ValidationError {
  field: string;
  message: string;
}

export function validateCashExpense(
  name: string,
  amount: number,
  yearOrAge: YearOrAge,
  currentAge: number,
  currentYear: number
): ValidationError[] {
  const errors: ValidationError[] = [];

  if (!name || name.trim().length === 0) {
    errors.push({ field: 'name', message: 'Expense name is required' });
  } else if (name.length > 100) {
    errors.push({ field: 'name', message: 'Expense name must be 100 characters or less' });
  }

  if (amount <= 0) {
    errors.push({ field: 'amount', message: 'Amount must be greater than zero' });
  } else if (amount >= 1e12) {
    errors.push({ field: 'amount', message: 'Amount must be less than $1 trillion' });
  }

  const year = toYear(yearOrAge, currentAge, currentYear);
  const period = simulationPeriod(currentAge, currentYear);
  if (year < period.firstYear || year > period.lastYear) {
    errors.push({ field: 'yearOrAge', message: `Expense must fall within the simulation: ${period.description}` });
  }

  return errors;
}

/**
 * Validate a loan expense.
 */
export function validateLoanExpense(
  name: string,
  principal: number,
  aprPercent: number,
  termYears: number,
  startYearOrAge: YearOrAge,
  currentAge: number,
  currentYear: number,
  downPayment: number = 0
): ValidationError[] {
  const errors: ValidationError[] = [];

  if (!name || name.trim().length === 0) {
    errors.push({ field: 'name', message: 'Loan name is required' });
  } else if (name.length > 100) {
    errors.push({ field: 'name', message: 'Loan name must be 100 characters or less' });
  }

  if (principal <= 0) {
    errors.push({ field: 'principal', message: 'Principal must be greater than zero' });
  } else if (principal >= 1e12) {
    errors.push({ field: 'principal', message: 'Principal must be less than $1 trillion' });
  }

  if (aprPercent < 0) {
    errors.push({ field: 'aprPercent', message: 'APR cannot be negative' });
  } else if (aprPercent > 99.99) {
    errors.push({ field: 'aprPercent', message: 'APR must be 99.99% or less' });
  }

  if (termYears <= 0) {
    errors.push({ field: 'termYears', message: 'Term must be greater than zero' });
  } else if (termYears > 50) {
    errors.push({ field: 'termYears', message: 'Term must be 50 years or less' });
  }

  if (downPayment < 0) {
    errors.push({ field: 'downPayment', message: 'Down payment cannot be negative' });
  } else if (downPayment > principal) {
    errors.push({ field: 'downPayment', message: 'Down payment cannot exceed principal' });
  }

  // A loan may run past the end of the simulation; only payments up to then are simulated
  const startYear = toYear(startYearOrAge, currentAge, currentYear);
  const period = simulationPeriod(currentAge, currentYear);
  if (startYear < period.firstYear || startYear > period.lastYear) {
    errors.push({ field: 'startYearOrAge', message: `Loan must start within the simulation: ${period.description}` });
  }

  return errors;
}

/**
 * Format server validation errors (422) as readable lines, naming the expense each error belongs to.
 * e.g. "oneTimeExpenses[1].aprPercent" -> "Expense 2 (Car Loan): APR cannot be negative"
 */
export function formatValidationErrors(errors: ApiValidationError[], expenses: OneTimeExpense[]): string[] {
  return errors.map(({ field, message }) => {
    const match = /^oneTimeExpenses\[(\d+)\]/.exec(field);
    if (!match) return message;
    const index = Number(match[1]);
    const name = expenses[index]?.name;
    return `Expense ${index + 1}${name ? ` (${name})` : ''}: ${message}`;
  });
}
