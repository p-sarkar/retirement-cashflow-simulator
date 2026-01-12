export interface SimulationConfig {
  id?: string;
  name: string;
  currentYear: number;
  currentAge: number;
  retirementAge: number;
  portfolio: Portfolio;
  spousal: SpousalDetails;
  expenses: ExpenseConfig;
  contributions: ContributionConfig;
  rates: RateConfig;
  strategy: StrategyConfig;
}

export interface ContributionConfig {
  annual401k: number;
  annualTba: number;
}

export interface Portfolio {
  sb: number;
  cbb: number;
  tba: number;
  tda: number;
  tfa: number;
}

export interface SpousalDetails {
  spouseAge: number;
  lowerEarner: SocialSecurityDetails;
  higherEarner: SocialSecurityDetails;
}

export interface SocialSecurityDetails {
  claimAge: number;
  annualBenefit: number;
}

export interface ExpenseConfig {
  needs: number;
  wants: number;
  propertyTax: number;
  healthcarePreMedicare: number;
  healthcareMedicare: number;
}

export interface RateConfig {
  inflation: number;
  preRetirementGrowth: number;
  postRetirementGrowth: number;
  bondYield: number;
  hysaRate: number;
  incomeTax: number;
}

export interface StrategyConfig {
  initialTdaWithdrawal: number;
  rothConversionAmount: number;
  type: string;
}

export interface SimulationResult {
  config: SimulationConfig;
  yearlyResults: YearlyResult[];
  summary: Summary;
}

export interface YearlyResult {
  year: number;
  age: number;
  balances: Portfolio;
  cashFlow: CashFlow;
  metrics: Metrics;
}

export interface CashFlow {
  salary: number;
  interest: number;
  dividends: number;
  socialSecurity: number;
  tbaWithdrawal: number;
  tdaWithdrawal: number;
  rothConversion: number;
  contribution401k: number;
  contributionTba: number;
  totalIncome: number;
  needs: number;
  wants: number;
  healthcare: number;
  incomeTax: number;
  propertyTax: number;
  totalExpenses: number;
}

export interface Metrics {
  annualIncomeGap: number;
  isFailure: boolean;
}

export interface Summary {
  finalTotalBalance: number;
  isSuccess: boolean;
  failureYear: number | null;
  totalDividends: number;
  totalInterest: number;
}

export interface SimulationRun {
  config: SimulationConfig;
  yearlyResults: YearlyResult[];
  isSuccess: boolean;
  failureYear: number | null;
  endingBalance: number;
}

export interface MonteCarloResult {
  runs: SimulationRun[];
  statistics: Statistics;
}

export interface Statistics {
  medianPath: number[];
  percentile75Path: number[];
  percentile90Path: number[];
  successRate: number;
}
