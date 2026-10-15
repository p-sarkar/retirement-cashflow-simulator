// GENERATED FILE - DO NOT EDIT.
// Source: Kotlin @Serializable models in api-server (see TsContractGenerator).
// Regenerate: cd api-server && ./gradlew generateTsContract
// Optional (?) fields have server-side defaults and may be omitted in requests.

export interface SimulationConfig {
  id?: string | null;
  name: string;
  currentYear: number;
  currentAge: number;
  retirementAge: number;
  endAge?: number;
  salary: number;
  portfolio: Portfolio;
  spousal: SpousalDetails;
  expenses: ExpenseConfig;
  contributions: ContributionConfig;
  rates: RateConfig;
  strategy: StrategyConfig;
  oneTimeExpenses?: OneTimeExpense[];
  marketPresetId?: string | null;
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
  healthcarePreRetirement: number;
  healthcarePostRetirementPreMedicare: number;
  healthcareMedicare: number;
}

export interface ContributionConfig {
  annual401k: number;
  annualRoth?: number;
  annualTba: number;
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
  tdaWithdrawalPercentage?: number;
  rothConversionAmount?: number;
  rothConversionPreRetirement?: number;
  rothConversionPostRetirement?: number;
  type?: string;
}

export type OneTimeExpense = CashExpense | LoanExpense;

export interface CashExpense {
  type: 'CASH';
  id: string;
  name: string;
  amount: number;
  yearOrAge: YearOrAge;
}

export type YearOrAge = YearOrAgeAge | YearOrAgeYear;

export interface YearOrAgeAge {
  type: 'AGE';
  age: number;
}

export interface YearOrAgeYear {
  type: 'YEAR';
  year: number;
}

export interface LoanExpense {
  type: 'LOAN';
  id: string;
  name: string;
  principal: number;
  aprPercent: number;
  termYears: number;
  startYearOrAge: YearOrAge;
  quarterlyPayment?: number | null;
  downPayment?: number;
}

export interface BreakdownRequest {
  config: SimulationConfig;
  targetAge: number;
}

export interface MonteCarloRequest {
  config: SimulationConfig;
  runs?: number;
  seed?: number | null;
}

export interface MonteCarloRunRequest {
  config: SimulationConfig;
  seed: number;
  runIndex: number;
}

export interface SimulationResult {
  config: SimulationConfig;
  yearlyResults: YearlyResult[];
  quarterlyResults: QuarterlyResult[];
  summary: Summary;
  apiMetadata: ApiMetadata;
}

export interface YearlyResult {
  year: number;
  age: number;
  balances: Portfolio;
  cashFlow: CashFlow;
  metrics: Metrics;
  oneTimeExpensesBreakdown: ExpenseDetail[] | null;
}

export interface CashFlow {
  salary: number;
  interest: number;
  dividends: number;
  socialSecurity: number;
  tbaWithdrawal: number;
  tdaWithdrawal: number;
  tdaWithdrawalSpend: number;
  tdaWithdrawalRoth: number;
  sbDeposit: number;
  sbWithdrawal: number;
  rothConversion: number;
  contribution401k: number;
  contributionRoth: number;
  contributionTba: number;
  totalIncome: number;
  needs: number;
  wants: number;
  healthcare: number;
  incomeTax: number;
  propertyTax: number;
  totalExpenses: number;
  oneTimeExpenses: number;
}

export interface Metrics {
  annualIncomeGap: number | null;
  incomeGapExpenses: number;
  incomeGapPassiveIncome: number;
  sbCap: number | null;
  cbbCap: number;
  isFailure: boolean;
}

export interface ExpenseDetail {
  name: string;
  amount: number;
  type: ExpenseType;
}

export type ExpenseType = 'CASH' | 'LOAN_PAYMENT';

export interface QuarterlyResult {
  year: number;
  quarter: number;
  age: number;
  balances: Portfolio;
  cashFlow: CashFlow;
  metrics: Metrics;
}

export interface Summary {
  finalTotalBalance: number;
  isSuccess: boolean;
  failureYear: number | null;
  totalDividends: number;
  totalInterest: number;
}

export interface ApiMetadata {
  version: string;
  buildTime: string;
  serverStartTime: string;
}

export interface ComputationBreakdown {
  year: number;
  age: number;
  sections: BreakdownSection[];
  apiMetadata: ApiMetadata;
}

export interface BreakdownSection {
  title: string;
  steps: ComputationStep[];
}

export interface ComputationStep {
  label: string;
  formula: string;
  values: Record<string, number>;
  result: number;
  explanation: string;
}

export interface ValidationErrorResponse {
  error: string;
  statusCode: number;
  validationErrors: ValidationError[];
}

export interface ValidationError {
  field: string;
  message: string;
}

export interface ErrorResponse {
  error: string;
  statusCode: number;
}

export interface MarketPreset {
  id: string;
  name: string;
  description: string;
  inflation: number;
  preRetirementGrowth: number;
  postRetirementGrowth: number;
  bondYield: number;
  retirementReturnSequence: number[];
}

export interface MonteCarloResult {
  seed: number;
  runCount: number;
  method: string;
  years: number[];
  paths: MonteCarloPath[];
  statistics: Statistics;
  apiMetadata: ApiMetadata;
}

export interface MonteCarloPath {
  runIndex: number;
  totalBalances: number[];
  isSuccess: boolean;
  failureYear: number | null;
  endingBalance: number;
  averageReturn: number;
  averageInflation: number;
  averageBondYield: number;
}

export interface Statistics {
  medianPath: number[];
  percentile75Path: number[];
  percentile90Path: number[];
  successRate: number;
}

export interface MonteCarloRunDetail {
  seed: number;
  path: MonteCarloPath;
  scenario: MarketScenario;
  result: SimulationResult;
}

export interface MarketScenario {
  years: number[];
  equityReturns: number[];
  inflationRates: number[];
  bondYields: number[];
}

export interface SavedSimulationSummary {
  id: string;
  name: string;
  createdAt: string;
  isSuccess: boolean;
  failureYear: number | null;
  finalTotalBalance: number;
  endAge: number;
}

export interface SavedSimulation {
  summary: SavedSimulationSummary;
  result: SimulationResult;
}

export interface SaveSimulationRequest {
  name: string;
  result: SimulationResult;
}
