/**
 * Utility functions for one-time expense calculations and conversions.
 */

import { YearOrAge } from '../types/simulation';

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
 * Calculate monthly payment using standard amortization formula.
 * M = P[r(1+r)^n] / [(1+r)^n - 1]
 *
 * For 0% APR, uses simple division: P / n
 */
export function calculateMonthlyPayment(principal: number, aprPercent: number, termYears: number): number {
  const n = termYears * 12;

  if (aprPercent === 0) {
    return principal / n;
  }

  const r = aprPercent / 100 / 12;
  const onePlusR = 1 + r;
  const onePlusRPowN = Math.pow(onePlusR, n);

  return (principal * r * onePlusRPowN) / (onePlusRPowN - 1);
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
  if (year < currentYear) {
    errors.push({ field: 'yearOrAge', message: 'Expense cannot occur in the past' });
  } else if (year > currentYear + 35) {
    errors.push({ field: 'yearOrAge', message: 'Expense must occur within simulation period' });
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
  currentYear: number
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

  const startYear = toYear(startYearOrAge, currentAge, currentYear);
  if (startYear < currentYear) {
    errors.push({ field: 'startYearOrAge', message: 'Loan cannot start in the past' });
  } else if (startYear > currentYear + 35) {
    errors.push({ field: 'startYearOrAge', message: 'Loan must start within simulation period' });
  }

  return errors;
}

