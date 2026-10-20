import {
  CashFlow, GuardrailReview, Metrics, MonteCarloPath, MonteCarloResult, MonteCarloRunDetail, Portfolio, QuarterlyResult,
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
  strategy: { initialTdaWithdrawal: 40000, tdaWithdrawalPercentage: 0.7, rothConversionAmount: 0, rothConversionPreRetirement: 5000, rothConversionPostRetirement: 15000, type: 'PARTHA_V0_01_20250105',
    guardrails: { targetSuccess: 0.85, upperGuardrail: 0.97, lowerGuardrail: 0.6, reviewFrequency: 'QUARTERLY' } },
  oneTimeExpenses: [{ id: 'c', type: 'CASH', name: 'Roof', amount: 20000, yearOrAge: { type: 'YEAR', year: 2030 } }],
  marketPresetId: 'crash-2008'
};

const balances: Portfolio = { sb: 100000, cbb: 300000, tba: 500000, tda: 900000, tfa: 100000 };

const cashFlow = (overrides: Partial<CashFlow> = {}): CashFlow => ({
  salary: 150000, interest: 3500.5, dividends: 13500, socialSecurity: 0, tbaWithdrawal: 0, tdaWithdrawal: 5000,
  tdaWithdrawalSpend: 0, tdaWithdrawalRoth: 5000, tfaWithdrawal: 0, sbDeposit: 0, sbWithdrawal: 0, rothConversion: 5000,
  contribution401k: 20000, contributionRoth: 7000, contributionTba: 10000, totalIncome: 172000.5,
  needs: 60000, wants: 30000, healthcare: 5000, incomeTax: 20000, propertyTax: 10000, totalExpenses: 125000,
  oneTimeExpenses: 0, ...overrides
});

const metrics = (overrides: Partial<Metrics> = {}): Metrics => ({
  annualIncomeGap: null, incomeGapExpenses: 125000, incomeGapPassiveIncome: 17000, sbCap: null, cbbCap: 525000,
  isFailure: false, ...overrides
});

export const yearly = (year: number, age: number, overrides: Partial<YearlyResult> = {}): YearlyResult => ({
  year, age, balances, cashFlow: cashFlow(), metrics: metrics(), oneTimeExpensesBreakdown: null, market: null, guardrails: null, ...overrides
});

const market = { equityReturn: -0.1234, inflation: 0.031, bondYield: 0.045 };

export const quarterly = (year: number, quarter: number, age: number): QuarterlyResult => ({
  year, quarter, age, balances, cashFlow: cashFlow({ salary: 37500 }), metrics: metrics(), market
});

export const simulationResult = (config: SimulationConfig = apiConfig): SimulationResult => ({
  config,
  yearlyResults: [
    yearly(2026, 61),
    yearly(2031, 66, { cashFlow: cashFlow({ tfaWithdrawal: 12000 }), metrics: metrics({ annualIncomeGap: 45000.25, sbCap: 90000 }), market })
  ],
  quarterlyResults: [quarterly(2026, 0, 61), quarterly(2026, 1, 61)],
  summary: { finalTotalBalance: 1900000, isSuccess: true, failureYear: null, totalDividends: 1, totalInterest: 1, guardrails: null },
  apiMetadata: { version: '1.1.29', buildTime: 'b', serverStartTime: 's', build: '1.1.29+3.gabc1234' }
});

export const monteCarloPath = (runIndex: number, totalBalances: number[], isSuccess = true): MonteCarloPath => ({
  runIndex, totalBalances, isSuccess, failureYear: isSuccess ? null : 2025 + totalBalances.length - 1,
  endingBalance: totalBalances[totalBalances.length - 1], averageReturn: 0.071, averageInflation: 0.028, averageBondYield: 0.046,
  hadSpendingCut: null
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
    successRate: 2 / 3,
    spending: null
  },
  apiMetadata: { version: '1.1.29', buildTime: 'b', serverStartTime: 's', build: '1.1.29+3.gabc1234' }
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

const review = (year: number, overrides: Partial<GuardrailReview> = {}): GuardrailReview => ({
  year, quarter: 1, age: 66 + (year - 2031), portfolioValue: 3400000, probabilityBefore: 0.82, outcome: 'NO_CHANGE',
  wantsBefore: 30000, wantsAfter: 30000, spendingBefore: 105000, probabilityAfter: 0.82,
  lowerGuardrailPortfolio: null, upperGuardrailPortfolio: null, wantsAtLowerGuardrail: null, wantsAtUpperGuardrail: null,
  ...overrides
});

/** A guardrails result (spec 006): retires in 2031, a cut in 2032. */
export const guardrailsResult = (): SimulationResult => {
  const base = simulationResult({ ...apiConfig, strategy: { ...apiConfig.strategy, type: 'RISK_BASED_GUARDRAILS_V1' } });
  const reviews = [
    review(2031, { probabilityBefore: 0.744, probabilityAfter: 0.744 }),
    review(2032, { probabilityBefore: 0.428, outcome: 'CUT', wantsBefore: 30900, wantsAfter: 7700, probabilityAfter: 0.902 })
  ];
  return {
    ...base,
    yearlyResults: [
      base.yearlyResults[0],
      { ...base.yearlyResults[1], guardrails: { configuredWants: 30000, annualSpendingTodaysDollars: 105000, reviews: [reviews[0]] } },
      yearly(2032, 67, { cashFlow: { ...base.yearlyResults[1].cashFlow, wants: 7700 },
        guardrails: { configuredWants: 30900, annualSpendingTodaysDollars: 82500, reviews: [reviews[1]] } })
    ],
    summary: {
      ...base.summary,
      guardrails: {
        startingProbability: 0.744, startingLowerGuardrailPortfolio: null, startingUpperGuardrailPortfolio: null,
        cuts: 1, raises: 0, largestCutPercent: 0.2210, lowestAnnualSpending: 82500, highestAnnualSpending: 105000,
        configuredSpendingUnreachable: false
      }
    }
  };
};

/** monteCarloResult() as a guardrails analysis: spending lines and a cut in one of three runs. */
export const guardrailsMonteCarloResult = (): MonteCarloResult => {
  const base = monteCarloResult();
  return {
    ...base,
    paths: base.paths.map((p, i) => ({ ...p, hadSpendingCut: i === 2 })),
    statistics: {
      ...base.statistics,
      spending: {
        percentile10Path: [null, 70000, 65000, 60000],
        medianPath: [null, 100000, 98000, 97000],
        percentile90Path: [null, 120000, 125000, 130000],
        shareOfRunsWithCut: 1 / 3
      }
    }
  };
};
