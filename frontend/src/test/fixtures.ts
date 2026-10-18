import {
  CashFlow, Metrics, MonteCarloPath, MonteCarloResult, MonteCarloRunDetail, Portfolio, QuarterlyResult,
  SimulationConfig, SimulationResult, YearlyResult
} from '../types/simulation';

export const apiConfig: SimulationConfig = {
  name: 'Saved Plan',
  currentYear: 2025,
  currentAge: 60,
  retirementAge: 65,
  endAge: 90,
  salary: 150000,
  portfolio: { sb: 100000, cbb: 300000, tba: 500000, tda: 900000, tfa: 100000 },
  spousal: { spouseAge: 58, lowerEarner: { claimAge: 67, annualBenefit: 20000 }, higherEarner: { claimAge: 70, annualBenefit: 40000 } },
  expenses: { needs: 60000, wants: 30000, propertyTax: 10000, healthcarePreRetirement: 5000, healthcarePostRetirementPreMedicare: 20000, healthcareMedicare: 3000 },
  contributions: { annual401k: 20000, annualRoth: 7000, annualTba: 10000 },
  rates: { inflation: 0.025, preRetirementGrowth: 0.07, postRetirementGrowth: 0.06, bondYield: 0.045, hysaRate: 0.035, incomeTax: 0.18 },
  strategy: { initialTdaWithdrawal: 40000, tdaWithdrawalPercentage: 0.7, rothConversionAmount: 0, rothConversionPreRetirement: 5000, rothConversionPostRetirement: 15000, type: 'PARTHA_V0_01_20250105' },
  oneTimeExpenses: [{ id: 'c', type: 'CASH', name: 'Roof', amount: 20000, yearOrAge: { type: 'YEAR', year: 2030 } }],
  marketPresetId: 'crash-2008'
};

const balances: Portfolio = { sb: 100000, cbb: 300000, tba: 500000, tda: 900000, tfa: 100000 };

const cashFlow = (overrides: Partial<CashFlow> = {}): CashFlow => ({
  salary: 150000, interest: 3500.5, dividends: 13500, socialSecurity: 0, tbaWithdrawal: 0, tdaWithdrawal: 5000,
  tdaWithdrawalSpend: 0, tdaWithdrawalRoth: 5000, sbDeposit: 0, sbWithdrawal: 0, rothConversion: 5000,
  contribution401k: 20000, contributionRoth: 7000, contributionTba: 10000, totalIncome: 172000.5,
  needs: 60000, wants: 30000, healthcare: 5000, incomeTax: 20000, propertyTax: 10000, totalExpenses: 125000,
  oneTimeExpenses: 0, ...overrides
});

const metrics = (overrides: Partial<Metrics> = {}): Metrics => ({
  annualIncomeGap: null, incomeGapExpenses: 125000, incomeGapPassiveIncome: 17000, sbCap: null, cbbCap: 525000,
  isFailure: false, ...overrides
});

export const yearly = (year: number, age: number, overrides: Partial<YearlyResult> = {}): YearlyResult => ({
  year, age, balances, cashFlow: cashFlow(), metrics: metrics(), oneTimeExpensesBreakdown: null, market: null, ...overrides
});

const market = { equityReturn: -0.1234, inflation: 0.031, bondYield: 0.045 };

export const quarterly = (year: number, quarter: number, age: number): QuarterlyResult => ({
  year, quarter, age, balances, cashFlow: cashFlow({ salary: 37500 }), metrics: metrics(), market
});

export const simulationResult = (config: SimulationConfig = apiConfig): SimulationResult => ({
  config,
  yearlyResults: [
    yearly(2026, 61),
    yearly(2031, 66, { metrics: metrics({ annualIncomeGap: 45000.25, sbCap: 90000 }), market })
  ],
  quarterlyResults: [quarterly(2026, 0, 61), quarterly(2026, 1, 61)],
  summary: { finalTotalBalance: 1900000, isSuccess: true, failureYear: null, totalDividends: 1, totalInterest: 1 },
  apiMetadata: { version: '1.1.29', buildTime: 'b', serverStartTime: 's' }
});

export const monteCarloPath = (runIndex: number, totalBalances: number[], isSuccess = true): MonteCarloPath => ({
  runIndex, totalBalances, isSuccess, failureYear: isSuccess ? null : 2025 + totalBalances.length - 1,
  endingBalance: totalBalances[totalBalances.length - 1], averageReturn: 0.071, averageInflation: 0.028, averageBondYield: 0.046
});

/** Three runs over 2025-2028: rising, flat, and one that fails in 2027. */
export const monteCarloResult = (): MonteCarloResult => ({
  seed: 42,
  runCount: 3,
  method: 'Historical bootstrap.',
  years: [2025, 2026, 2027, 2028],
  paths: [
    monteCarloPath(0, [1000000, 1200000, 1400000, 1600000]),
    monteCarloPath(1, [1000000, 1000000, 1000000, 1000000]),
    monteCarloPath(2, [1000000, 600000, 200000], false)
  ],
  statistics: {
    medianPath: [1000000, 1000000, 1000000, 1000000],
    percentile75Path: [1000000, 800000, 600000, 500000],
    percentile90Path: [1000000, 680000, 360000, 200000],
    successRate: 2 / 3
  },
  apiMetadata: { version: '1.1.29', buildTime: 'b', serverStartTime: 's' }
});

export const monteCarloRunDetail = (runIndex = 1): MonteCarloRunDetail => ({
  seed: 42,
  path: monteCarloResult().paths[runIndex],
  scenario: {
    years: [2025, 2026, 2027, 2028],
    equityReturns: [0.07, -0.12, 0.215, 0.05],
    inflationRates: [0.025, 0.031, 0.02, 0.018],
    bondYields: [0.045, 0.05, 0.039, 0.041]
  },
  result: simulationResult()
});
