import { Endpoint, MovementType } from '../../types/simulation';

export const formatMoney = (amount: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(amount);

export const formatNumber = (num: number) =>
  Math.abs(num) < 1 ? num.toFixed(4) : new Intl.NumberFormat('en-US', { maximumFractionDigits: 2 }).format(num);

/** 0.0325 → "3.25%" */
export const formatPercent = (rate: number) => `${(rate * 100).toFixed(2).replace(/\.?0+$/, '')}%`;

export const ENDPOINT_LABELS: Record<Endpoint, string> = {
  SB: 'Spend Bucket',
  CBB: 'Crash Buffer',
  TBA: 'Taxable',
  TDA: 'Tax Deferred',
  TFA: 'Tax Free',
  SALARY: 'Salary',
  SOCIAL_SECURITY: 'Social Security',
  BANK: 'HYSA interest',
  BONDS: 'Bond dividends',
  EXPENSES: 'Expenses',
  MARKET: 'Market'
};

export const MOVEMENT_LABELS: Record<MovementType, string> = {
  INTEREST: 'Interest',
  DIVIDENDS: 'Dividends',
  TAKE_HOME_SALARY: 'Take-home salary',
  CONTRIBUTION_401K: '401k contribution',
  CONTRIBUTION_ROTH: 'Roth contribution',
  CONTRIBUTION_TBA: 'Taxable contribution',
  SOCIAL_SECURITY: 'Social Security',
  REGULAR_EXPENSES: 'Regular expenses',
  ONE_TIME_EXPENSE: 'One-time expenses',
  STRATEGY_REFILL: 'Spending-strategy refill',
  SHORTFALL_COVER: 'Shortfall cover',
  ROTH_CONVERSION: 'Roth conversion',
  MARKET_GROWTH: 'Market growth'
};
