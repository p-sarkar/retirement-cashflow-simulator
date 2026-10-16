import { QuarterlyResult, YearlyResult } from '../types/simulation';

export type ResultRow = YearlyResult | QuarterlyResult;

/**
 * The results table columns, shared by the table and the CSV export so both always have the same
 * columns (FR002-010a). `money` values are dollars; null means not applicable (shown as ~).
 */
export interface ResultColumn {
  header: string;
  csvHeader?: string; // when the table header has decoration (e.g. tree lines)
  value: (row: ResultRow) => number | string | null;
  money?: boolean;
  emphasis?: 'header' | 'cell' | 'both'; // bold header and/or cells
  sub?: boolean; // breakdown of the previous column, shown smaller
  minWidth?: number;
}

export const timeLabel = (row: ResultRow) => ('quarter' in row ? `${row.year} Q${row.quarter + 1}` : `${row.year}`);

export const totalBalance = (row: ResultRow) =>
  row.balances.sb + row.balances.cbb + row.balances.tba + row.balances.tda + row.balances.tfa;

export const RESULT_COLUMNS: ResultColumn[] = [
  { header: 'Time', value: timeLabel, minWidth: 90 },
  { header: 'Age', value: row => row.age },
  { header: 'Spend Bucket', value: row => row.balances.sb, money: true },
  { header: 'SB Cap', value: row => row.metrics.sbCap, money: true },
  { header: 'Crash Buffer', value: row => row.balances.cbb, money: true },
  { header: 'CBB Cap', value: row => row.metrics.cbbCap, money: true },
  { header: 'Taxable', value: row => row.balances.tba, money: true },
  { header: 'Tax Deferred', value: row => row.balances.tda, money: true },
  { header: 'Tax Free', value: row => row.balances.tfa, money: true },
  { header: 'Total Portfolio', value: totalBalance, money: true, emphasis: 'cell' },
  { header: 'Salary', value: row => row.cashFlow.salary, money: true },
  { header: 'Interest', value: row => row.cashFlow.interest, money: true },
  { header: 'Dividends', value: row => row.cashFlow.dividends, money: true },
  { header: 'Social Security', value: row => row.cashFlow.socialSecurity, money: true },
  { header: 'TBA Withdrawal', value: row => row.cashFlow.tbaWithdrawal, money: true },
  { header: 'TDA Withdrawal', value: row => row.cashFlow.tdaWithdrawal, money: true, emphasis: 'both' },
  { header: '└─ TDA for Spend', csvHeader: 'TDA for Spend', value: row => row.cashFlow.tdaWithdrawalSpend, money: true, sub: true },
  { header: '└─ TDA for Roth', csvHeader: 'TDA for Roth', value: row => row.cashFlow.tdaWithdrawalRoth, money: true, sub: true },
  { header: 'Roth Conv.', value: row => row.cashFlow.rothConversion, money: true },
  { header: 'Total Income', value: row => row.cashFlow.totalIncome, money: true, emphasis: 'both' },
  { header: 'Needs', value: row => row.cashFlow.needs, money: true },
  { header: 'Wants', value: row => row.cashFlow.wants, money: true },
  { header: 'Healthcare', value: row => row.cashFlow.healthcare, money: true },
  { header: 'Property Tax', value: row => row.cashFlow.propertyTax, money: true },
  { header: 'Income Tax', value: row => row.cashFlow.incomeTax, money: true },
  { header: 'One-Time Exp', value: row => row.cashFlow.oneTimeExpenses || 0, money: true },
  { header: 'Total Expenses', value: row => row.cashFlow.totalExpenses, money: true, emphasis: 'both' },
  { header: '401k Contrib', value: row => row.cashFlow.contribution401k, money: true },
  { header: 'TBA Contrib', value: row => row.cashFlow.contributionTba, money: true },
  // Null in working years (salary covers expenses)
  { header: 'Income Gap', value: row => row.metrics.annualIncomeGap, money: true, emphasis: 'both' }
];
